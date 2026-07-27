package com.retrocrawler.mycollection.facts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.model.identifier.TheRetroWebId;
import com.retrocrawler.model.identifier.TheRetroWebIdParser;
import com.retrocrawler.model.measurement.DataCapacity;
import com.retrocrawler.model.measurement.DataCapacityParser;
import com.retrocrawler.mycollection.memory.RamSet;

class CollectionFactParsersTest {

	@Test
	void parsesTheRetroWebIdsAsExternalReferences() {
		final var fact = new TheRetroWebIdParser().parse("10510");

		assertEquals(Confidence.EXACT, fact.getConfidence());
		assertEquals(new TheRetroWebId(10510), fact.getValue().orElseThrow());
		assertEquals(Confidence.NONE, new TheRetroWebIdParser().parse("motherboard-10510").getConfidence());
	}

	@Test
	void parsesCapacitiesWithDecimalCommaAndCanonicalUnits() {
		final DataCapacityParser parser = new DataCapacityParser();

		assertEquals(new DataCapacity(new BigDecimal("1.125"), DataCapacity.Unit.MB),
				parser.parse("1,125MB").getValue().orElseThrow());
		assertEquals(new DataCapacity(BigDecimal.valueOf(32), DataCapacity.Unit.KB),
				parser.parse("32kb").getValue().orElseThrow());
		assertEquals(new DataCapacity(BigDecimal.valueOf(32), DataCapacity.Unit.KB),
				parser.parse("32KB").getValue().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("3,5").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("3,3V").getConfidence());
	}

	@Test
	void parsesOnlyCanonicalCountFirstRamSets() {
		final RamSetParser parser = new RamSetParser();

		final RamSet set = (RamSet) parser.parse("2 x 16MB").getValue().orElseThrow();
		assertEquals(2, set.memberCount());
		assertEquals(new DataCapacity(BigDecimal.valueOf(16), DataCapacity.Unit.MB), set.capacityPerMember());
		assertEquals(new DataCapacity(BigDecimal.valueOf(32), DataCapacity.Unit.MB), set.totalCapacity());

		assertEquals(Confidence.NONE, parser.parse("32kb x 4").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("2 x 4 x 256kb plus Parity").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("? x 100MB").getConfidence());
	}

	@Test
	void comparesEquivalentCapacitiesAcrossUnits() {
		final DataCapacity oneGigabyte = new DataCapacity(BigDecimal.ONE, DataCapacity.Unit.GB);
		final DataCapacity twoTimes512Megabytes = new DataCapacity(BigDecimal.valueOf(512),
				DataCapacity.Unit.MB).multiply(2);

		assertTrue(oneGigabyte.sameSizeAs(twoTimes512Megabytes));
	}
}
