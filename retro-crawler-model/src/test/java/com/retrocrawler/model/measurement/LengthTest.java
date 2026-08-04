package com.retrocrawler.model.measurement;

import static com.retrocrawler.model.ParserTestContext.CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.model.measurement.Length.Unit;

class LengthTest {

	@Test
	void parsesMetricAndImperialMeasurementsWithoutAssigningARole() {
		final LengthParser parser = new LengthParser();

		assertEquals(new Length(new BigDecimal("1.25"), Unit.MILLIMETER),
				parser.parse("1,25mm", CONTEXT).value().orElseThrow());
		assertEquals(new Length(new BigDecimal("50"), Unit.CENTIMETER),
				parser.parse("50 cm", CONTEXT).value().orElseThrow());
		assertEquals(new Length(new BigDecimal("2"), Unit.METER), parser.parse("2m", CONTEXT).value().orElseThrow());
		assertEquals(new Length(new BigDecimal("2.5"), Unit.INCH),
				parser.parse("2,5\"", CONTEXT).value().orElseThrow());
		assertEquals(new Length(new BigDecimal("2.5"), Unit.INCH),
				parser.parse("2.5 inches", CONTEXT).value().orElseThrow());
	}

	@Test
	void comparesEquivalentLengthsAcrossUnits() {
		final Length fiftyCentimeters = new Length(new BigDecimal("50"), Unit.CENTIMETER);
		final Length fiveHundredMillimeters = new Length(new BigDecimal("500"), Unit.MILLIMETER);
		final Length twoAndAHalfInches = new Length(new BigDecimal("2.5"), Unit.INCH);

		assertTrue(fiftyCentimeters.sameLengthAs(fiveHundredMillimeters));
		assertEquals(new BigDecimal("63.5"), twoAndAHalfInches.inMillimeters());
		assertEquals(new BigDecimal("2.5"), twoAndAHalfInches.amountIn(Unit.INCH));
		assertEquals("2.5\"", twoAndAHalfInches.toString());
	}

	@Test
	void rejectsMissingUnitsAndNonPositiveOrUnrelatedQuantities() {
		final LengthParser parser = new LengthParser();

		for (final String invalid : new String[] {
				"2.5", "0mm", "-2m", "50MB"
		}) {
			assertEquals(Confidence.NONE, parser.parse(invalid, CONTEXT).confidence(), invalid);
		}
		assertFalse(LengthParser.parseValue(null).isPresent());
		assertThrows(IllegalArgumentException.class, () -> new Length(BigDecimal.ZERO, Unit.MILLIMETER));
	}
}
