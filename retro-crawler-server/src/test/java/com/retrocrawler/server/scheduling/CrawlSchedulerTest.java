package com.retrocrawler.server.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.crawl.CrawlAlreadyRunningException;
import com.retrocrawler.core.crawl.CrawlOperation;
import com.retrocrawler.core.crawl.CrawlOperationId;
import com.retrocrawler.core.crawl.CrawlOperationService;
import com.retrocrawler.server.RetroCrawlerServerProperties.CrawlSchedule;

class CrawlSchedulerTest {

	@Test
	void registersTheConfiguredCronTriggerAndStartsAFullCrawl() {
		final CrawlOperationService operations = mock(CrawlOperationService.class);
		final CrawlOperation operation = mock(CrawlOperation.class);
		when(operation.id()).thenReturn(new CrawlOperationId("scheduled"));
		when(operations.start(ReindexScope.all())).thenReturn(operation);
		final CrawlScheduler scheduler = new CrawlScheduler(operations,
				new CrawlSchedule(true, "0 30 2 * * *", ZoneId.of("Europe/Berlin")));
		final ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

		scheduler.configureTasks(registrar);

		assertThat(registrar.getTriggerTaskList()).singleElement().satisfies(task -> {
			assertThat(task.getTrigger()).isEqualTo(new CronTrigger("0 30 2 * * *", ZoneId.of("Europe/Berlin")));
			task.getRunnable().run();
		});
		verify(operations).start(ReindexScope.all());
	}

	@Test
	void skipsATriggerWhileAnotherCrawlIsActive() {
		final CrawlOperationService operations = mock(CrawlOperationService.class);
		when(operations.start(ReindexScope.all()))
				.thenThrow(new CrawlAlreadyRunningException(new CrawlOperationId("active")));
		final CrawlScheduler scheduler = new CrawlScheduler(operations,
				new CrawlSchedule(true, "0 0 3 * * *", ZoneId.of("UTC")));

		assertThatCode(scheduler::startCrawl).doesNotThrowAnyException();
		verify(operations).start(ReindexScope.all());
	}
}
