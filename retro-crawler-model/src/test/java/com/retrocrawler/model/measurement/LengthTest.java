package com.retrocrawler.model.measurement;

import static com.retrocrawler.model.ParserTestContext.CONTEXT;
import static com.retrocrawler.model.measurement.MeasurementUnits.CENTIMETRE;
import static com.retrocrawler.model.measurement.MeasurementUnits.INCH;
import static com.retrocrawler.model.measurement.MeasurementUnits.METRE;
import static com.retrocrawler.model.measurement.MeasurementUnits.MILLIMETRE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.gear.Confidence;

class LengthTest {

	@Test
	void parsesMetricAndImperialMeasurementsWithoutAssigningARole() {
		final LengthParser parser = new LengthParser();

		assertEquals(new Length(new BigDecimal("1.25"), MILLIMETRE),
				parser.parse("1,25mm", CONTEXT).value().orElseThrow());
		assertEquals(new Length(new BigDecimal("50"), CENTIMETRE),
				parser.parse("50 cm", CONTEXT).value().orElseThrow());
		assertEquals(new Length(new BigDecimal("2"), METRE), parser.parse("2m", CONTEXT).value().orElseThrow());
		assertEquals(new Length(new BigDecimal("2.5"), INCH), parser.parse("2,5\"", CONTEXT).value().orElseThrow());
		assertEquals(new Length(new BigDecimal("2.5"), INCH),
				parser.parse("2.5 inches", CONTEXT).value().orElseThrow());
	}

	@Test
	void comparesEquivalentLengthsAcrossUnits() {
		final Length fiftyCentimeters = new Length(new BigDecimal("50"), CENTIMETRE);
		final Length fiveHundredMillimeters = new Length(new BigDecimal("500"), MILLIMETRE);
		final Length twoAndAHalfInches = new Length(new BigDecimal("2.5"), INCH);

		assertTrue(fiftyCentimeters.sameLengthAs(fiveHundredMillimeters));
		assertEquals(new BigDecimal("63.5"), twoAndAHalfInches.inMillimeters());
		assertEquals(new BigDecimal("2.5"), twoAndAHalfInches.amountIn(INCH));
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
		assertThrows(IllegalArgumentException.class, () -> new Length(BigDecimal.ZERO, MILLIMETRE));
	}
}
