package com.retrocrawler.model.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class HardwareParsersTest {

	@Test
	void parsesExpansionBusNamesAndEstablishedAliases() {
		final ExpansionBusParser parser = new ExpansionBusParser();

		assertEquals(ExpansionBus.ISA, parser.parse("ISA").getValue().orElseThrow());
		assertEquals(ExpansionBus.MCA, parser.parse("Micro Channel Architecture").getValue().orElseThrow());
		assertEquals(ExpansionBus.PCI_EXPRESS, parser.parse("PCIe").getValue().orElseThrow());
		assertEquals(ExpansionBus.VLB, parser.parse("VESA Local Bus").getValue().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("USB").getConfidence());
	}

	@Test
	void keepsMemoryAccessTimeSeparateFromMemoryStandard() {
		final MemoryAccessTimeParser accessTimeParser = new MemoryAccessTimeParser();
		final MemoryStandardParser standardParser = new MemoryStandardParser();

		assertEquals(new MemoryAccessTime(70), accessTimeParser.parse("70ns").getValue().orElseThrow());
		assertEquals(new MemoryAccessTime(60), accessTimeParser.parse("ns60").getValue().orElseThrow());
		assertEquals(Confidence.NONE, accessTimeParser.parse("70").getConfidence());
		assertEquals(MemoryStandard.PC_100, standardParser.parse("PC100").getValue().orElseThrow());
		assertEquals(MemoryStandard.PC_133, standardParser.parse("pc-133").getValue().orElseThrow());
		assertEquals(MemoryStandard.PC_3200, standardParser.parse("PC3200").getValue().orElseThrow());
		assertEquals(Confidence.NONE, standardParser.parse("70ns").getConfidence());
	}

	@Test
	void parsesPortableMemoryAndBoardVocabulary() {
		final MemoryFormFactorParser memoryForm = new MemoryFormFactorParser();
		final MemoryFeatureParser memoryFeature = new MemoryFeatureParser();
		final ComputerFormFactorParser computerForm = new ComputerFormFactorParser();
		final VideoConnectorParser videoConnector = new VideoConnectorParser();

		assertEquals(MemoryFormFactor.SIMM_30_PIN, memoryForm.parse("SIMM30").getValue().orElseThrow());
		assertEquals(MemoryFormFactor.SIMM_72_PIN, memoryForm.parse("72-pin SIMM").getValue().orElseThrow());
		assertEquals(MemoryFormFactor.SO_DIMM, memoryForm.parse("SO-DIMM").getValue().orElseThrow());
		assertEquals(MemoryFeature.EXTENDED_DATA_OUT, memoryFeature.parse("EDO").getValue().orElseThrow());
		assertEquals(MemoryFeature.FAST_PAGE_MODE, memoryFeature.parse("FPM").getValue().orElseThrow());
		assertEquals(Confidence.NONE, memoryFeature.parse("EDOFPM").getConfidence());
		assertEquals(ComputerFormFactor.MICRO_ATX,
				computerForm.parse("microATX").getValue().orElseThrow());
		assertEquals(VideoConnector.S_VIDEO, videoConnector.parse("S-Video").getValue().orElseThrow());
		assertEquals(VideoConnector.DISPLAY_PORT, videoConnector.parse("DP").getValue().orElseThrow());
	}
}
