package com.retrocrawler.model.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.model.identifier.TheRetroWebCategory;
import com.retrocrawler.model.identifier.TheRetroWebId;
import com.retrocrawler.model.identifier.TheRetroWebReference;

class ManufacturerCatalogTest {

	private final ManufacturerCatalog catalog = ManufacturerCatalog.bundled();

	@Test
	void providesOneIndependentlySourcedDefaultManufacturer() {
		final Manufacturer asus = new Manufacturer("ASUS", "ASUSTeK Computer Inc.");

		assertEquals(List.of(asus), catalog.entries());
		assertEquals(List.of(asus), catalog.findByName(" asus "));
		assertEquals(List.of(asus), catalog.findByName("ASUSTEK COMPUTER INC."));
		assertTrue(asus.theRetroWebReference().isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> catalog.entries().add(asus));
	}

	@Test
	void parserReturnsExactFactsOnlyForUnambiguousCatalogNames() {
		final ManufacturerParser parser = new ManufacturerParser();

		assertEquals(new Manufacturer("ASUS", "ASUSTeK Computer Inc."), parser.parse("Asus").value().orElseThrow());
		assertEquals(Confidence.EXACT, parser.parse("ASUSTeK Computer Inc.").confidence());
		assertEquals(Confidence.NONE, parser.parse("Unknown Industries").confidence());
		assertEquals(Confidence.NONE, parser.parse(" ").confidence());
		assertEquals(Confidence.NONE, parser.parse(null).confidence());
	}

	@Test
	void preservesAmbiguityInsteadOfChoosingOneManufacturer() {
		final ManufacturerCatalog external = ManufacturerCatalog
				.read(new StringReader("name\tfull_name\ttrw_id\n" + "Umax\tUmax Technologies, Inc.\t2573\n"
						+ "Umax\tConQuest Entertainment a. s.\t2574\n" + "Solo\t\t\n"));
		final ManufacturerParser parser = new ManufacturerParser(external);

		assertEquals(2, external.findByName("Umax").size());
		assertEquals(Confidence.NONE, parser.parse("Umax").confidence());
		assertTrue(parser.parse("Umax").explanation().orElseThrow().contains("ambiguous"));
		final Manufacturer conQuest = (Manufacturer) parser.parse("ConQuest Entertainment a. s.").value().orElseThrow();
		assertEquals("ConQuest Entertainment a. s.", conQuest.fullName().orElseThrow());
		assertEquals(new Manufacturer("Solo"), parser.parse("Solo").value().orElseThrow());
		final Manufacturer linked = (Manufacturer) parser.parse("Umax Technologies, Inc.").value().orElseThrow();
		assertEquals("https://theretroweb.com/manufacturers/2573",
				linked.theRetroWebReference().orElseThrow().lookupUri().toString());
	}

	@Test
	void rejectsUnknownOrIncompleteSchemasAndInvalidProviderIds() {
		assertThrows(IllegalArgumentException.class, () -> ManufacturerCatalog.read(new StringReader("""
			name	full_name	trw_id	logo_file
			ASUS	ASUSTeK Computer Inc.	53	53.svg
			""")));
		assertThrows(IllegalArgumentException.class, () -> ManufacturerCatalog.read(new StringReader("name\nASUS\n")));
		assertThrows(IllegalArgumentException.class, () -> ManufacturerCatalog.read(new StringReader("""
			name	full_name	trw_id
			ASUS	ASUSTeK Computer Inc.	not-an-id
			""")));
	}

	@Test
	void validatesAndNormalizesManufacturerValues() {
		assertEquals(new Manufacturer("ASUS", Optional.empty(), Optional.empty()), new Manufacturer(" ASUS "));
		assertEquals(new Manufacturer("ASUS", "ASUSTeK Computer Inc."),
				new Manufacturer(" ASUS ", " ASUSTeK Computer Inc. "));
		assertThrows(IllegalArgumentException.class, () -> new Manufacturer(" "));
		assertThrows(IllegalArgumentException.class, () -> new Manufacturer("ASUS", " "));
		final TheRetroWebReference motherboard = TheRetroWebCategory.MOTHERBOARD.reference(new TheRetroWebId(53));
		assertThrows(IllegalArgumentException.class, () -> new Manufacturer("ASUS", null, motherboard));
	}
}
