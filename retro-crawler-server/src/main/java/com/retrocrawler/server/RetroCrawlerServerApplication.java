package com.retrocrawler.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** The generic executable host for RetroCrawler protocol adapters. */
@SpringBootApplication
public class RetroCrawlerServerApplication {

	public static void main(final String[] args) {
		SpringApplication.run(RetroCrawlerServerApplication.class, args);
	}
}
