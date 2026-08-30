package com.retrocrawler.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
		contextRunner.withBean(RetroCrawler.class, () -> mock(RetroCrawler.class))
				.run(context -> assertThat(context).hasNotFailed());
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
