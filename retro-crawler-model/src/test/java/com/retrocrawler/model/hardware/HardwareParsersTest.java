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
		assertEquals(Confidence.NONE, standardParser.parse("70ns").getConfidence());
	}
}
