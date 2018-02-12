package net.vargadaniel.re.reportrepository;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface ReportEngine {
	
	final String REPORT_FILES = "reportFiles";
	final String STATUS_UPDATES = "statusUpdates"; 
	
	@Input(REPORT_FILES)
	SubscribableChannel reportFileTopic();
	
	@Output(STATUS_UPDATES)
	MessageChannel statusUpdates();
}
