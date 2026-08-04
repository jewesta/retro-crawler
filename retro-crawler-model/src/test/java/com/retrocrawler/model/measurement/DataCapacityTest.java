package com.retrocrawler.model.measurement;

import static com.retrocrawler.model.ParserTestContext.CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class DataCapacityTest {

	@Test
	void parsesDecimalCommaAndEstablishedCapacityUnits() {
		final DataCapacityParser parser = new DataCapacityParser();

		assertEquals(new DataCapacity(new BigDecimal("1.125"), DataCapacity.Unit.MB),
				parser.parse("1,125MB", CONTEXT).value().orElseThrow());
		assertEquals(new DataCapacity(BigDecimal.valueOf(32), DataCapacity.Unit.KB),
				parser.parse("32kb", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("3,3V", CONTEXT).confidence());
	}

	@Test
	void comparesEquivalentCapacitiesAcrossUnits() {
		final DataCapacity oneGigabyte = new DataCapacity(BigDecimal.ONE, DataCapacity.Unit.GB);
		final DataCapacity twoTimes512Megabytes = new DataCapacity(BigDecimal.valueOf(512), DataCapacity.Unit.MB)
				.multiply(2);

		assertTrue(oneGigabyte.sameSizeAs(twoTimes512Megabytes));
	}
}
