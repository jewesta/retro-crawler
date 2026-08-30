package com.retrocrawler.server.scheduling;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.crawl.CrawlAlreadyRunningException;
import com.retrocrawler.core.crawl.CrawlOperation;
import com.retrocrawler.core.crawl.CrawlOperationService;
import com.retrocrawler.server.RetroCrawlerServerProperties.CrawlSchedule;

/** Starts configured full crawls through the shared operation service. */
final class CrawlScheduler implements SchedulingConfigurer {

	private static final Logger LOGGER = LoggerFactory.getLogger(CrawlScheduler.class);

	private final CrawlOperationService operations;

	private final CrawlSchedule schedule;

	CrawlScheduler(final CrawlOperationService operations, final CrawlSchedule schedule) {
		this.operations = Objects.requireNonNull(operations, "operations");
		this.schedule = Objects.requireNonNull(schedule, "schedule");
	}

	@Override
	public void configureTasks(final ScheduledTaskRegistrar taskRegistrar) {
		Objects.requireNonNull(taskRegistrar, "taskRegistrar").addTriggerTask(this::startCrawl,
				new CronTrigger(schedule.cron(), schedule.zone()));
	}

	void startCrawl() {
		try {
			final CrawlOperation operation = operations.start(ReindexScope.all());
			LOGGER.info("Started scheduled crawl operation '{}'.", operation.id());
		} catch (final CrawlAlreadyRunningException running) {
			LOGGER.info("Skipped scheduled crawl because operation '{}' is still active.", running.activeOperationId());
		}
	}
}
