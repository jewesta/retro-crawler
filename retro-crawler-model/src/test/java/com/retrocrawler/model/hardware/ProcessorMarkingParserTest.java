package com.retrocrawler.model.hardware;

import static com.retrocrawler.model.ParserTestContext.CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.gear.Confidence;

class ProcessorMarkingParserTest {

	private final ProcessorMarkingParser parser = new ProcessorMarkingParser();

	@Test
	void parsesObservedCanonicalAmdProductionMarkings() {
		assertEquals(new AmdProcessorMarking("A", "9852", "11PM"),
				parser.parse("A-985211PM", CONTEXT).value().orElseThrow());
		assertEquals(new AmdProcessorMarking("B1", "0038", "E5D"),
				parser.parse("B1-0038E5D", CONTEXT).value().orElseThrow());
		assertEquals(new AmdProcessorMarking("A", "9452", "FPG-T"),
				parser.parse("A-9452FPG-T", CONTEXT).value().orElseThrow());
	}

	@Test
	void parsesObservedIntelFinishedProcessOrdersWithOptionalPartialAtpo() {
		assertEquals(new IntelProcessorMarking("L7470234", Optional.empty()),
				parser.parse("L7470234", CONTEXT).value().orElseThrow());
		assertEquals(new IntelProcessorMarking("L5170697", Optional.of("0141")),
				parser.parse("L5170697-0141", CONTEXT).value().orElseThrow());
		assertEquals(new IntelProcessorMarking("Q742A494", Optional.empty()),
				parser.parse("Q742A494", CONTEXT).value().orElseThrow());
	}

	@Test
	void rejectsNoncanonicalOrInsufficientlySpecificShapes() {
		assertEquals(Confidence.NONE, parser.parse("A 985211PM", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("ET4000AX", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("L7540234", CONTEXT).confidence());
		assertThrows(IllegalArgumentException.class, () -> new AmdProcessorMarking("A", "9852", "unknown"));
		assertThrows(IllegalArgumentException.class, () -> new IntelProcessorMarking("L7540234", Optional.empty()));
	}
}
