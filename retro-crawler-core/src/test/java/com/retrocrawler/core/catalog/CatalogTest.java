package com.retrocrawler.core.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.api.Test;

class CatalogTest {

	private enum Key {
		first,
		second,
		third
	}

	@Test
	void readsCommentsArbitraryHeaderOrderAndTrailingEmptyCells() {
		final Catalog<Key> catalog = Catalog.read(Key.class, new StringReader("""
				# source

				third\tfirst\tsecond
				last\tone\t
				  # another comment
				three\ttwo\tmiddle
				"""));

		assertEquals(2, catalog.rows().size());
		assertEquals("one", catalog.rows().getFirst().get(Key.first));
		assertEquals("", catalog.rows().getFirst().get(Key.second));
		assertEquals("last", catalog.rows().getFirst().get(Key.third));
		assertEquals(List.of("one", "", "last"),
				List.of(Key.values()).stream().map(catalog.rows().getFirst()::get).toList());
		assertThrows(UnsupportedOperationException.class, () -> catalog.rows().clear());
		assertThrows(UnsupportedOperationException.class,
				() -> catalog.rows().getFirst().values().put(Key.first, "changed"));
	}

	@Test
	void rejectsMissingUnknownAndDuplicateHeaderKeys() {
		assertThrows(IllegalArgumentException.class,
				() -> Catalog.read(Key.class, new StringReader("first\tsecond\n")));
		assertThrows(IllegalArgumentException.class,
				() -> Catalog.read(Key.class, new StringReader("first\tsecond\tunknown\n")));
		assertThrows(IllegalArgumentException.class,
				() -> Catalog.read(Key.class, new StringReader("first\tsecond\tsecond\tthird\n")));
	}

	@Test
	void rejectsRowsWhoseWidthDoesNotMatchTheHeader() {
		assertThrows(IllegalArgumentException.class,
				() -> Catalog.read(Key.class, new StringReader("first\tsecond\tthird\none\ttwo\n")));
		assertThrows(IllegalArgumentException.class,
				() -> Catalog.read(Key.class,
						new StringReader("first\tsecond\tthird\none\ttwo\tthree\tfour\n")));
	}

	@Test
	void combinesOnlyCatalogsWithTheSameKeyType() {
		final Catalog<Key> first = Catalog.read(Key.class,
				new StringReader("first\tsecond\tthird\n1\t2\t3\n"));
		final Catalog<Key> second = Catalog.read(Key.class,
				new StringReader("third\tsecond\tfirst\n6\t5\t4\n"));

		assertEquals(2, first.plus(second).rows().size());
		assertEquals("4", first.plus(second).rows().get(1).get(Key.first));
	}
}
