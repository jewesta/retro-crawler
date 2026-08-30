package com.retrocrawler.server;

import java.util.Objects;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.retrocrawler.core.RetroCrawler;

/** Enforces the collection composition contract of the generic server. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RetroCrawlerServerProperties.class)
class RetroCrawlerServerConfiguration {

	RetroCrawlerServerConfiguration(final RetroCrawler retroCrawler) {
		Objects.requireNonNull(retroCrawler, "retroCrawler");
	}
}
