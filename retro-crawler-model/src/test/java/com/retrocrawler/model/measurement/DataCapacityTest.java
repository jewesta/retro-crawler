package com.retrocrawler.model.measurement;

import static com.retrocrawler.model.ParserTestContext.CONTEXT;
import static com.retrocrawler.model.measurement.MeasurementUnits.GIBIBYTE;
import static com.retrocrawler.model.measurement.MeasurementUnits.KIBIBYTE;
import static com.retrocrawler.model.measurement.MeasurementUnits.MEBIBYTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.gear.Confidence;

class DataCapacityTest {

	@Test
	void parsesDecimalCommaAndEstablishedCapacityUnits() {
		final DataCapacityParser parser = new DataCapacityParser();

		assertEquals(new DataCapacity(new BigDecimal("1.125"), MEBIBYTE),
				parser.parse("1,125MB", CONTEXT).value().orElseThrow());
		assertEquals(new DataCapacity(BigDecimal.valueOf(32), KIBIBYTE),
				parser.parse("32kb", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("3,3V", CONTEXT).confidence());
	}

	@Test
	void comparesEquivalentCapacitiesAcrossUnits() {
		final DataCapacity oneGigabyte = new DataCapacity(BigDecimal.ONE, GIBIBYTE);
		final DataCapacity twoTimes512Megabytes = new DataCapacity(BigDecimal.valueOf(512), MEBIBYTE).multiply(2);

		assertTrue(oneGigabyte.sameSizeAs(twoTimes512Megabytes));
		assertEquals(new BigDecimal("1048576"), oneGigabyte.inKibibytes());
		assertEquals(oneGigabyte.inKibibytes(), oneGigabyte.inKilobytes());
	}
}
