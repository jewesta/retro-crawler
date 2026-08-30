package com.retrocrawler.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.crawl.CrawlOperationService;
import com.retrocrawler.mcp.crawl.RetroCrawlerCrawlMcpTools;
import com.retrocrawler.mcp.filter.FilterCatalog;

class RetroCrawlerMcpAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(RetroCrawlerMcpAutoConfiguration.class));

	@Test
	void providesToolsForASingleCrawler() {
		final RetroCrawler crawler = crawler();

		contextRunner.withBean(RetroCrawler.class, () -> crawler).run(context -> {
			assertThat(context).hasSingleBean(CrawlOperationService.class).hasSingleBean(RetroCrawlerMcpTools.class)
					.hasSingleBean(RetroCrawlerCrawlMcpTools.class);
			assertThat(context.getBean(RetroCrawlerMcpTools.class).listArchives()).isEqualTo(
					new ArchiveCatalog("test_collection", List.of(new ArchiveSummary("main", "Main archive"))));
			assertThat(context.getBean(RetroCrawlerMcpTools.class).listFilters())
					.isEqualTo(new FilterCatalog("test_collection", List.of()));
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
	void failsForAnAmbiguousCrawler() {
		contextRunner.withBean("firstCrawler", RetroCrawler.class, RetroCrawlerMcpAutoConfigurationTest::crawler)
				.withBean("secondCrawler", RetroCrawler.class, RetroCrawlerMcpAutoConfigurationTest::crawler)
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.hasRootCauseInstanceOf(NoUniqueBeanDefinitionException.class);
				});
	}

	@Test
	void preservesApplicationProvidedTools() {
		final RetroCrawler crawler = crawler();
		final RetroCrawlerMcpTools tools = new RetroCrawlerMcpTools(crawler);

		contextRunner.withBean(RetroCrawler.class, () -> crawler).withBean(RetroCrawlerMcpTools.class, () -> tools)
				.run(context -> assertThat(context.getBean(RetroCrawlerMcpTools.class)).isSameAs(tools));
	}

	@Test
	void preservesAnApplicationProvidedCrawlOperationService() {
		final RetroCrawler crawler = crawler();
		final CrawlOperationService service = mock(CrawlOperationService.class);

		contextRunner.withBean(RetroCrawler.class, () -> crawler).withBean(CrawlOperationService.class, () -> service)
				.run(context -> assertThat(context.getBean(CrawlOperationService.class)).isSameAs(service));
	}

	private static RetroCrawler crawler() {
		final RetroCrawler crawler = mock(RetroCrawler.class);
		when(crawler.collectionId()).thenReturn("test_collection");
		when(crawler.archives()).thenReturn(
				List.of(new ArchiveDescriptor(ArchiveId.of("main"), "Main archive", Path.of("archive-root"))));
		return crawler;
	}
}
