package com.retrocrawler.mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.crawl.CrawlOperationService;

/** Auto-configures RetroCrawler's MCP tools around an application crawler. */
@AutoConfiguration
@ConditionalOnClass(McpTool.class)
public class RetroCrawlerMcpAutoConfiguration {

	@Bean(destroyMethod = "close")
	@ConditionalOnMissingBean
	CrawlOperationService crawlOperationService(final RetroCrawler retroCrawler) {
		return new CrawlOperationService(retroCrawler);
	}

	@Bean
	@ConditionalOnMissingBean
	RetroCrawlerMcpTools retroCrawlerMcpTools(final RetroCrawler retroCrawler) {
		return new RetroCrawlerMcpTools(retroCrawler);
	}
}
