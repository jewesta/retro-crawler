package com.retrocrawler.model.hardware;

import static com.retrocrawler.model.ParserTestContext.CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class HardwareParsersTest {

	@Test
	void preservesExplicitChipDesignationsWithoutAssumingAPartNumberSyntax() {
		final ChipDesignationParser parser = new ChipDesignationParser();

		assertEquals(new ChipDesignation("RC42-A"), parser.parse(" RC42-A ", CONTEXT).value().orElseThrow());
		assertEquals(new ChipDesignation("Example Semiconductor 42"),
				parser.parse("Example Semiconductor 42", CONTEXT).value().orElseThrow());
		assertEquals(new ChipDesignation("mixed Case 7"), parser.parse("mixed Case 7", CONTEXT).value().orElseThrow());
		assertEquals("mixed Case 7", new ChipDesignation(" mixed Case 7 ").toString());
		assertEquals(Confidence.NONE, parser.parse("  ", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse(null, CONTEXT).confidence());
		assertThrows(IllegalArgumentException.class, () -> new ChipDesignation(""));
	}

	@Test
	void parsesExpansionBusNamesAndEstablishedAliases() {
		final ExpansionBusParser parser = new ExpansionBusParser();

		assertEquals(ExpansionBus.ISA, parser.parse("ISA", CONTEXT).value().orElseThrow());
		assertEquals(ExpansionBus.MCA, parser.parse("Micro Channel Architecture", CONTEXT).value().orElseThrow());
		assertEquals(ExpansionBus.PCI_EXPRESS, parser.parse("PCIe", CONTEXT).value().orElseThrow());
		assertEquals(ExpansionBus.VLB, parser.parse("VESA Local Bus", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("USB", CONTEXT).confidence());
	}

	@Test
	void keepsMemoryAccessTimeSeparateFromMemoryStandard() {
		final MemoryAccessTimeParser accessTimeParser = new MemoryAccessTimeParser();
		final MemoryStandardParser standardParser = new MemoryStandardParser();

		assertEquals(new MemoryAccessTime(70), accessTimeParser.parse("70ns", CONTEXT).value().orElseThrow());
		assertEquals(new MemoryAccessTime(60), accessTimeParser.parse("ns60", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, accessTimeParser.parse("70", CONTEXT).confidence());
		assertEquals(MemoryStandard.PC_100, standardParser.parse("PC100", CONTEXT).value().orElseThrow());
		assertEquals(MemoryStandard.PC_133, standardParser.parse("pc-133", CONTEXT).value().orElseThrow());
		assertEquals(MemoryStandard.PC_3200, standardParser.parse("PC3200", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, standardParser.parse("70ns", CONTEXT).confidence());
	}

	@Test
	void parsesPortableMemoryAndBoardVocabulary() {
		final MemoryFormFactorParser memoryForm = new MemoryFormFactorParser();
		final MemoryFeatureParser memoryFeature = new MemoryFeatureParser();
		final ComputerFormFactorParser computerForm = new ComputerFormFactorParser();
		final VideoConnectorParser videoConnector = new VideoConnectorParser();

		assertEquals(MemoryFormFactor.SIMM_30_PIN, memoryForm.parse("SIMM30", CONTEXT).value().orElseThrow());
		assertEquals(MemoryFormFactor.SIMM_72_PIN, memoryForm.parse("72-pin SIMM", CONTEXT).value().orElseThrow());
		assertEquals(MemoryFormFactor.SO_DIMM, memoryForm.parse("SO-DIMM", CONTEXT).value().orElseThrow());
		assertEquals(MemoryFeature.EXTENDED_DATA_OUT, memoryFeature.parse("EDO", CONTEXT).value().orElseThrow());
		assertEquals(MemoryFeature.FAST_PAGE_MODE, memoryFeature.parse("FPM", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, memoryFeature.parse("EDOFPM", CONTEXT).confidence());
		assertEquals(ComputerFormFactor.MICRO_ATX, computerForm.parse("microATX", CONTEXT).value().orElseThrow());
		assertEquals(VideoConnector.S_VIDEO, videoConnector.parse("S-Video", CONTEXT).value().orElseThrow());
		assertEquals(VideoConnector.DISPLAY_PORT, videoConnector.parse("DP", CONTEXT).value().orElseThrow());
	}
}
