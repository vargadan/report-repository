package net.vargadaniel.re.reportrepository;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.vargadaniel.re.reportrepository.model.Report;

@SpringBootApplication
@RestController
@EnableBinding(ReportEngine.class)
@EnableResourceServer
public class ReportRepoApp {
	
	final static Logger logger = LoggerFactory.getLogger(ReportRepoApp.class);
	
	@Autowired
	ReportRepository reportRepository;
	
	@Autowired
	ReportEngine reportEngine;
	
	public static void main(String[] args) {
		String k8sNamespace = System.getenv("KUBERNETES_NAMESPACE");
		if (k8sNamespace != null) {
			String profile = k8sNamespace.substring(k8sNamespace.lastIndexOf("-") + 1);
			System.setProperty("spring.profiles.active", profile);
		}
		SpringApplication.run(ReportRepoApp.class, args);
	}
	
	@StreamListener(ReportEngine.REPORT_FILES)
	void saveReport(Message<String> message) {
		Object orderId = message.getHeaders().get("orderId");
		try {
			String payLoad = message.getPayload();
			
			Report report = new Report();
			report.setOrderId(orderId.toString());	
			report.setContent(payLoad.getBytes());
			
			reportRepository.save(report);
			statusUpdate(orderId.toString(), "report saved"); 
		} catch (Exception e) {
			statusUpdate(orderId.toString(), "Could not save : " + e.getMessage());
			throw e;
		}
		
	}
	
	void statusUpdate(String orderId, String status) {
		logger.info("statusupdate for orderId " + orderId + " status : " + status);
		reportEngine.statusUpdates()
				.send(MessageBuilder.withPayload(new StatusUpdate(orderId, status)).build());
	}

	
	@RequestMapping(path="/files/{id}", method=RequestMethod.GET, produces="text/plain")
	ResponseEntity<String> getFile(@PathVariable("id") String orderId, @AuthenticationPrincipal Principal principal) {
		logger.info("calling /files/{}.pdf with princiapl:{}", orderId, principal.getName());
		List<Report> reports = reportRepository.findByOrderId(orderId);
		if (reports == null || reports.isEmpty()) {
			return new ResponseEntity<>("No report found with ID " + orderId, HttpStatus.NOT_FOUND);
		} else if (reports.size() == 1) {
			String content = new String(reports.get(0).getContent());
			return new ResponseEntity<>(content, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(reports.size() + " reports found for orderId " + orderId, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	static class StatusUpdate {
		
		public String getOrderId() {
			return orderId;
		}

		public String getStatus() {
			return status;
		}

		public StatusUpdate(String orderId, String status) {
			super();
			this.orderId = orderId;
			this.status = status;
		}

		final String orderId;
		
		final String status;
		
	}

}
