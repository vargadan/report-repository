package net.vargadaniel.re.reportrepository;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
		String payLoad = message.getPayload();
		
		Report report = new Report();
		report.setOrderId(orderId.toString());	
		report.setContent(payLoad.getBytes());
		
		reportRepository.save(report);
	}

	
	@RequestMapping(path="/files/{id}", method=RequestMethod.GET, produces="text/plain")
	ResponseEntity<String> getFile(@PathVariable("id") Long id, @AuthenticationPrincipal Principal principal) {
		logger.info("calling /files/{}.pdf with princiapl:{}", id, principal.getName());
		Report report = reportRepository.findOne(id);
		if (report == null) {
			return new ResponseEntity<>("No report found with ID " + id, HttpStatus.NOT_FOUND);
		} 
		String content = new String(report.getContent());
		return new ResponseEntity<>(content, HttpStatus.OK);
	}

}
