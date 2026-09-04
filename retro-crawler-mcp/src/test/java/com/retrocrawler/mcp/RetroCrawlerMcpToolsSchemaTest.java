package com.retrocrawler.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.retrocrawler.mcp.crawl.RetroCrawlerCrawlMcpTools;

class RetroCrawlerMcpToolsSchemaTest {

	@Test
	void retainsThePublicSearchParameterNamesForTheMcpInputSchema() throws NoSuchMethodException {
		final Method searchGear = RetroCrawlerMcpTools.class.getMethod("searchGear", List.class, List.class,
				Integer.class, Integer.class);

		assertThat(searchGear.getParameters()).allMatch(java.lang.reflect.Parameter::isNamePresent)
				.extracting(java.lang.reflect.Parameter::getName)
				.containsExactly("archiveIds", "criteria", "offset", "limit");
	}

	@Test
	void retainsThePublicBrowseParameterNamesForTheMcpInputSchema() throws NoSuchMethodException {
		final Method browseArchive = RetroCrawlerMcpTools.class.getMethod("browseArchive", String.class, Integer.class,
				Integer.class);

		assertThat(browseArchive.getParameters()).allMatch(java.lang.reflect.Parameter::isNamePresent)
				.extracting(java.lang.reflect.Parameter::getName).containsExactly("ari", "offset", "limit");
	}

	@Test
	void retainsThePublicCrawlParameterNamesForTheMcpInputSchema() throws NoSuchMethodException {
		final Method startCrawl = RetroCrawlerCrawlMcpTools.class.getMethod("startCrawl", List.class, List.class);
		final Method getCrawl = RetroCrawlerCrawlMcpTools.class.getMethod("getCrawl", String.class);
		final Method cancelCrawl = RetroCrawlerCrawlMcpTools.class.getMethod("cancelCrawl", String.class);

		assertThat(startCrawl.getParameters()).extracting(java.lang.reflect.Parameter::getName)
				.containsExactly("archiveIds", "subtreeAris");
		assertThat(getCrawl.getParameters()).extracting(java.lang.reflect.Parameter::getName)
				.containsExactly("operationId");
		assertThat(cancelCrawl.getParameters()).extracting(java.lang.reflect.Parameter::getName)
				.containsExactly("operationId");
	}
}
