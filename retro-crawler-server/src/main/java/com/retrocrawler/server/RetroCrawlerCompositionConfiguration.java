package com.retrocrawler.server;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.retrocrawler.core.Model;
import com.retrocrawler.core.RetroCrawler;

/**
 * Composes the conventional crawler from an application's model and locations.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(RetroCrawler.class)
@EnableConfigurationProperties(LocationsProperties.class)
class RetroCrawlerCompositionConfiguration {

	@Bean
	RetroCrawler retroCrawler(final Model model, final LocationsProperties properties) {
		return RetroCrawler.builder().model(model).locations(properties.toLocations()).build();
	}
}
