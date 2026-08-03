package com.retrocrawler.model.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class HardwareParsersTest {

	@Test
	void preservesExplicitChipDesignationsWithoutAssumingAPartNumberSyntax() {
		final ChipDesignationParser parser = new ChipDesignationParser();

		assertEquals(new ChipDesignation("RC42-A"), parser.parse(" RC42-A ").value().orElseThrow());
		assertEquals(new ChipDesignation("Example Semiconductor 42"),
				parser.parse("Example Semiconductor 42").value().orElseThrow());
		assertEquals(new ChipDesignation("mixed Case 7"), parser.parse("mixed Case 7").value().orElseThrow());
		assertEquals("mixed Case 7", new ChipDesignation(" mixed Case 7 ").toString());
		assertEquals(Confidence.NONE, parser.parse("  ").confidence());
		assertEquals(Confidence.NONE, parser.parse(null).confidence());
		assertThrows(IllegalArgumentException.class, () -> new ChipDesignation(""));
	}

	@Test
	void parsesExpansionBusNamesAndEstablishedAliases() {
		final ExpansionBusParser parser = new ExpansionBusParser();

		assertEquals(ExpansionBus.ISA, parser.parse("ISA").value().orElseThrow());
		assertEquals(ExpansionBus.MCA, parser.parse("Micro Channel Architecture").value().orElseThrow());
		assertEquals(ExpansionBus.PCI_EXPRESS, parser.parse("PCIe").value().orElseThrow());
		assertEquals(ExpansionBus.VLB, parser.parse("VESA Local Bus").value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("USB").confidence());
	}

	@Test
	void keepsMemoryAccessTimeSeparateFromMemoryStandard() {
		final MemoryAccessTimeParser accessTimeParser = new MemoryAccessTimeParser();
		final MemoryStandardParser standardParser = new MemoryStandardParser();

		assertEquals(new MemoryAccessTime(70), accessTimeParser.parse("70ns").value().orElseThrow());
		assertEquals(new MemoryAccessTime(60), accessTimeParser.parse("ns60").value().orElseThrow());
		assertEquals(Confidence.NONE, accessTimeParser.parse("70").confidence());
		assertEquals(MemoryStandard.PC_100, standardParser.parse("PC100").value().orElseThrow());
		assertEquals(MemoryStandard.PC_133, standardParser.parse("pc-133").value().orElseThrow());
		assertEquals(MemoryStandard.PC_3200, standardParser.parse("PC3200").value().orElseThrow());
		assertEquals(Confidence.NONE, standardParser.parse("70ns").confidence());
	}

	@Test
	void parsesPortableMemoryAndBoardVocabulary() {
		final MemoryFormFactorParser memoryForm = new MemoryFormFactorParser();
		final MemoryFeatureParser memoryFeature = new MemoryFeatureParser();
		final ComputerFormFactorParser computerForm = new ComputerFormFactorParser();
		final VideoConnectorParser videoConnector = new VideoConnectorParser();

		assertEquals(MemoryFormFactor.SIMM_30_PIN, memoryForm.parse("SIMM30").value().orElseThrow());
		assertEquals(MemoryFormFactor.SIMM_72_PIN, memoryForm.parse("72-pin SIMM").value().orElseThrow());
		assertEquals(MemoryFormFactor.SO_DIMM, memoryForm.parse("SO-DIMM").value().orElseThrow());
		assertEquals(MemoryFeature.EXTENDED_DATA_OUT, memoryFeature.parse("EDO").value().orElseThrow());
		assertEquals(MemoryFeature.FAST_PAGE_MODE, memoryFeature.parse("FPM").value().orElseThrow());
		assertEquals(Confidence.NONE, memoryFeature.parse("EDOFPM").confidence());
		assertEquals(ComputerFormFactor.MICRO_ATX, computerForm.parse("microATX").value().orElseThrow());
		assertEquals(VideoConnector.S_VIDEO, videoConnector.parse("S-Video").value().orElseThrow());
		assertEquals(VideoConnector.DISPLAY_PORT, videoConnector.parse("DP").value().orElseThrow());
	}
}
