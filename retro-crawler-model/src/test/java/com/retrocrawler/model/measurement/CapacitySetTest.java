package com.retrocrawler.model.measurement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.model.measurement.DataCapacity.Unit;

class CapacitySetTest {

	@Test
	void parsesCountAndCapacityInEitherOrder() {
		final CapacitySetParser parser = new CapacitySetParser();
		final CapacitySet expected = new CapacitySet(4, new DataCapacity(BigDecimal.valueOf(32), Unit.KB));

		assertEquals(expected, parser.parse("4 x 32kb").getValue().orElseThrow());
		assertEquals(expected, parser.parse("32kb x 4").getValue().orElseThrow());
		assertEquals(new DataCapacity(BigDecimal.valueOf(128), Unit.KB), expected.totalCapacity());
		assertEquals("4 x 32KB", expected.toString());
	}

	@Test
	void rejectsValuesWhichDoNotDescribeAUniformCapacitySet() {
		final CapacitySetParser parser = new CapacitySetParser();

		for (final String invalid : new String[] {
				"5 x", "Set A", "2 x 4 x 256kb plus Parity", "? x 100MB", "1 x 16MB" }) {
			assertEquals(Confidence.NONE, parser.parse(invalid).getConfidence(), invalid);
		}
		assertThrows(IllegalArgumentException.class,
				() -> new CapacitySet(1, new DataCapacity(BigDecimal.ONE, Unit.MB)));
	}
}
