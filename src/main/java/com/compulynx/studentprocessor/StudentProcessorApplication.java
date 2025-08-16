package com.compulynx.studentprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.apache.poi.util.IOUtils;

@SpringBootApplication
public class StudentProcessorApplication {

	public static void main(String[] args) {
		// Set a higher limit for Apache POI to handle large files
		// The default is 100_000_000 (100MB)
		IOUtils.setByteArrayMaxOverride(200_000_000); // set mine to 200MB
	
		SpringApplication.run(StudentProcessorApplication.class, args);
	}

}
