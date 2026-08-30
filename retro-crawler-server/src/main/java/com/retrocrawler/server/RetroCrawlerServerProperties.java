package com.retrocrawler.server;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Operational configuration owned by the generic server. */
@ConfigurationProperties("retro-crawler.server")
public record RetroCrawlerServerProperties(CrawlSchedule crawl) {

	private static final String DEFAULT_CRON = "0 0 3 * * *";
	private static final ZoneId DEFAULT_ZONE = ZoneId.of("UTC");

	public RetroCrawlerServerProperties {
		crawl = crawl == null ? new CrawlSchedule(false, null, null) : crawl;
	}

	/** Configuration for scheduled full-collection crawls. */
	public record CrawlSchedule(boolean enabled, String cron, ZoneId zone) {

		public CrawlSchedule {
			cron = cron == null || cron.isBlank() ? DEFAULT_CRON : cron.trim();
			zone = zone == null ? DEFAULT_ZONE : zone;
		}
	}
}
