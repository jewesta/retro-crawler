package com.retrocrawler.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.retrocrawler.core.RetroCrawler;

class RetroCrawlerServerConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(RetroCrawlerServerConfiguration.class);

	@Test
	void startsWithExactlyOneCrawler() {
		contextRunner.withBean(RetroCrawler.class, () -> mock(RetroCrawler.class)).run(context -> {
			assertThat(context).hasNotFailed().hasSingleBean(RetroCrawlerServerProperties.class);
			final RetroCrawlerServerProperties properties = context.getBean(RetroCrawlerServerProperties.class);
			assertThat(properties.crawl().enabled()).isFalse();
			assertThat(properties.crawl().cron()).isEqualTo("0 0 3 * * *");
			assertThat(properties.crawl().zone()).isEqualTo(ZoneId.of("UTC"));
		});
	}

	@Test
	void bindsServerConfigurationIndependentlyFromCollectionLocations() {
		contextRunner.withBean(RetroCrawler.class, () -> mock(RetroCrawler.class))
				.withPropertyValues("retro-crawler.server.crawl.enabled=true",
						"retro-crawler.server.crawl.cron=0 30 2 * * *", "retro-crawler.server.crawl.zone=Europe/Berlin",
						"retro-crawler.repository.root=/repository")
				.run(context -> {
					final RetroCrawlerServerProperties properties = context.getBean(RetroCrawlerServerProperties.class);
					assertThat(properties.crawl().enabled()).isTrue();
					assertThat(properties.crawl().cron()).isEqualTo("0 30 2 * * *");
					assertThat(properties.crawl().zone()).isEqualTo(ZoneId.of("Europe/Berlin"));
				});
	}

	@Test
	void failsWithoutACrawler() {
		contextRunner.run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class);
		});
	}

	@Test
	void failsWithSeveralCrawlers() {
		contextRunner.withBean("firstCrawler", RetroCrawler.class, () -> mock(RetroCrawler.class))
				.withBean("secondCrawler", RetroCrawler.class, () -> mock(RetroCrawler.class)).run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.hasRootCauseInstanceOf(NoUniqueBeanDefinitionException.class);
				});
	}
}
