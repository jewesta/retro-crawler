package com.retrocrawler.server.scheduling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.retrocrawler.core.crawl.CrawlOperationService;
import com.retrocrawler.server.RetroCrawlerServerProperties;

/** Enables the configured nightly crawl without owning crawl concurrency. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "retro-crawler.server.crawl", name = "enabled", havingValue = "true")
class CrawlSchedulingConfiguration {

	@Bean
	CrawlScheduler crawlScheduler(final CrawlOperationService operations,
			final RetroCrawlerServerProperties properties) {
		return new CrawlScheduler(operations, properties.crawl());
	}
}
