package com.retrocrawler.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.retrocrawler.core.Model;
import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.mcp.RetroCrawlerMcpAutoConfiguration;
import com.retrocrawler.mcp.RetroCrawlerMcpTools;
import com.retrocrawler.mycollection.MyCollectionAutoConfiguration;

class RetroCrawlerServerCompositionTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(RetroCrawlerMcpAutoConfiguration.class, MyCollectionAutoConfiguration.class))
			.withUserConfiguration(RetroCrawlerCompositionConfiguration.class, RetroCrawlerServerConfiguration.class);

	@Test
	void composesServerMcpAdapterAndAnExternalCollection() {
		contextRunner.withPropertyValues("retro-crawler.repository.root=repository",
				"retro-crawler.archives[0].id=hardware", "retro-crawler.archives[0].name=Hardware",
				"retro-crawler.archives[0].root=archives/hardware", "retro-crawler.archives[1].id=software",
				"retro-crawler.archives[1].name=Software", "retro-crawler.archives[1].root=archives/software")
				.run(context -> {
					assertThat(context).hasNotFailed().hasSingleBean(Model.class)
							.hasSingleBean(LocationsProperties.class).hasSingleBean(RetroCrawler.class)
							.hasSingleBean(RetroCrawlerMcpTools.class)
							.hasSingleBean(RetroCrawlerServerProperties.class);

					assertThat(context.getBean(RetroCrawler.class).archives()).containsExactly(
							new ArchiveDescriptor(ArchiveId.of("hardware"), "Hardware", Path.of("archives/hardware")),
							new ArchiveDescriptor(ArchiveId.of("software"), "Software", Path.of("archives/software")));
				});
	}

	@Test
	void failsWhenTheRepositoryLocationIsNotConfigured() {
		contextRunner.withPropertyValues("retro-crawler.archives[0].id=hardware",
				"retro-crawler.archives[0].name=Hardware", "retro-crawler.archives[0].root=archives/hardware")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.hasRootCauseMessage("retro-crawler.repository must be configured");
				});
	}

	@Test
	void failsWhenNoArchiveLocationsAreConfigured() {
		contextRunner.withPropertyValues("retro-crawler.repository.root=repository").run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure())
					.hasRootCauseMessage("retro-crawler.archives must contain at least one archive");
		});
	}

	@Test
	void preservesAnApplicationProvidedCrawlerWithoutRequiringLocations() {
		final RetroCrawler crawler = mock(RetroCrawler.class);

		contextRunner.withBean(RetroCrawler.class, () -> crawler).run(context -> {
			assertThat(context).hasNotFailed().doesNotHaveBean(LocationsProperties.class);
			assertThat(context.getBean(RetroCrawler.class)).isSameAs(crawler);
		});
	}

	@Test
	void requiresACollectionModelForDefaultComposition() {
		new ApplicationContextRunner().withUserConfiguration(RetroCrawlerCompositionConfiguration.class)
				.withPropertyValues("retro-crawler.repository.root=repository", "retro-crawler.archives[0].id=hardware",
						"retro-crawler.archives[0].name=Hardware", "retro-crawler.archives[0].root=archives/hardware")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class);
				});
	}
}
