package com.retrocrawler.server.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import com.retrocrawler.core.crawl.CrawlOperationService;
import com.retrocrawler.server.RetroCrawlerServerProperties;

class CrawlSchedulingConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(PropertiesConfiguration.class, CrawlSchedulingConfiguration.class)
			.withBean(CrawlOperationService.class, () -> mock(CrawlOperationService.class));

	@Test
	void schedulingIsDisabledByDefault() {
		contextRunner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(CrawlScheduler.class));
	}

	@Test
	void enablesSchedulingFromServerConfiguration() {
		contextRunner
				.withPropertyValues("retro-crawler.server.crawl.enabled=true",
						"retro-crawler.server.crawl.cron=0 30 2 * * *", "retro-crawler.server.crawl.zone=Europe/Berlin")
				.run(context -> assertThat(context).hasNotFailed().hasSingleBean(CrawlScheduler.class));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(RetroCrawlerServerProperties.class)
	static class PropertiesConfiguration {
	}
}
