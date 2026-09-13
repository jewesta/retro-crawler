package com.retrocrawler.mycollection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.Journal;
import com.retrocrawler.core.Model;
import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.InMemoryRepository;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.mycollection.gear.MyGear;
import com.retrocrawler.mycollection.gear.cpu.Amd5x86;
import com.retrocrawler.mycollection.gear.cpu.Amd80286;
import com.retrocrawler.mycollection.gear.cpu.Amd80386;
import com.retrocrawler.mycollection.gear.cpu.Amd80486;
import com.retrocrawler.mycollection.gear.cpu.Amd8086;
import com.retrocrawler.mycollection.gear.cpu.AmdK5;
import com.retrocrawler.mycollection.gear.cpu.AmdK62;
import com.retrocrawler.mycollection.gear.cpu.AmdK6III;
import com.retrocrawler.mycollection.gear.cpu.AmdProcessor;
import com.retrocrawler.mycollection.gear.cpu.Intel80286;
import com.retrocrawler.mycollection.gear.cpu.Intel80386;
import com.retrocrawler.mycollection.gear.cpu.Intel80486;
import com.retrocrawler.mycollection.gear.cpu.IntelCore;
import com.retrocrawler.mycollection.gear.cpu.IntelMathCoprocessor;
import com.retrocrawler.mycollection.gear.cpu.IntelPentium;
import com.retrocrawler.mycollection.gear.cpu.IntelPentium4;
import com.retrocrawler.mycollection.gear.cpu.IntelPentiumMmx;
import com.retrocrawler.mycollection.gear.cpu.IntelProcessor;
import com.retrocrawler.mycollection.gear.cpu.Processor;

class ProcessorHierarchyTest {

	private static final ArchiveId ARCHIVE_ID = ArchiveId.of("processor_hierarchy");

	@TempDir
	private Path archiveRoot;

	@Test
	void selectsTheMostSpecificProcessorIdentityFromMarkingAndTitle() throws IOException {
		final List<ProcessorCase> cases = List.of(new ProcessorCase("AMD Am8086 8MHz", "A-985211PM", Amd8086.class),
				new ProcessorCase("AMD N80L286 12MHz", "A-985211PM", Amd80286.class),
				new ProcessorCase("AMD Am386 DX-40", "A-985211PM", Amd80386.class),
				new ProcessorCase("AMD Am486 DX2-66", "A-985211PM", Amd80486.class),
				new ProcessorCase("AMD Am5x86-P75", "A-985211PM", Amd5x86.class),
				new ProcessorCase("AMD K5 PR166", "A-985211PM", AmdK5.class),
				new ProcessorCase("AMD K6 2 500", "A-985211PM", AmdK62.class),
				new ProcessorCase("AMD K6-III 450", "A-985211PM", AmdK6III.class),
				new ProcessorCase("Unclassified AMD processor", "A-985211PM", AmdProcessor.class),
				new ProcessorCase("Intel N80286 12MHz", "L5170697", Intel80286.class),
				new ProcessorCase("Intel i386 DX-33", "L5170697", Intel80386.class),
				new ProcessorCase("Intel i486 DX2-66", "L5170697", Intel80486.class),
				new ProcessorCase("Intel Pentium 100", "L5170697", IntelPentium.class),
				new ProcessorCase("Intel Pentium MMX 233", "L5170697", IntelPentiumMmx.class),
				new ProcessorCase("Intel Pentium 4 2.8GHz", "L5170697", IntelPentium4.class),
				new ProcessorCase("IntelCore2 Duo", "V123A456", IntelCore.class),
				new ProcessorCase("Intel A80387 DX", "L5170697", IntelMathCoprocessor.class),
				new ProcessorCase("Unclassified Intel processor", "L5170697", IntelProcessor.class),
				new ProcessorCase("AMD K6-2 title on an Intel package", "L5170697", IntelProcessor.class));

		for (int index = 0; index < cases.size(); index++) {
			final ProcessorCase processorCase = cases.get(index);
			Files.createDirectories(archiveRoot
					.resolve(processorCase.title() + " [" + processorCase.marking() + "] [" + (200100 + index) + "]"));
		}

		final List<MyGear> gear = crawler().crawl(new Journal(), ReindexScope.all()).query(MyGear.class).pull().gear();

		assertEquals(cases.size(), gear.size());
		for (final ProcessorCase processorCase : cases) {
			final Processor processor = (Processor) gear.stream()
					.filter(candidate -> candidate.getTitle().filter(processorCase.title()::equals).isPresent())
					.findFirst().orElseThrow();
			assertEquals(processorCase.type(), processor.getClass(), processorCase.title());
			assertNotNull(processor.processorMarking());
		}
	}

	private RetroCrawler crawler() {
		return RetroCrawler.builder().model(Model.from("com.retrocrawler.mycollection"))
				.repository(new InMemoryRepository()).archive(ArchiveDescriptor.of(ARCHIVE_ID, archiveRoot)).build();
	}

	private record ProcessorCase(String title, String marking, Class<? extends Processor> type) {
	}
}
