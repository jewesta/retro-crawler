package com.retrocrawler.core.archive.clues;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class InternalClueKeysTest {

	@Test
	void exposesStableReservedKeys() {
		assertAll(() -> assertEquals("@id", InternalClueKeys.ID),
				() -> assertEquals("@folder", InternalClueKeys.FOLDER),
				() -> assertEquals("@type", InternalClueKeys.TYPE));
	}

	@Test
	void preventsFindersFromCreatingReservedClues() {
		assertAll(() -> assertThrows(IllegalArgumentException.class, () -> Clue.of(InternalClueKeys.ID, "artifact-id")),
				() -> assertThrows(IllegalArgumentException.class,
						() -> Clue.of(InternalClueKeys.FOLDER, "artifact-folder")),
				() -> assertThrows(IllegalArgumentException.class, () -> Clue.of(InternalClueKeys.TYPE, "clue-type")));
	}

	@Test
	void permitsSyntheticCluesButKeepsTypeForSerialization() {
		assertAll(() -> assertEquals(InternalClueKeys.ID, Clue.internal(InternalClueKeys.ID, "artifact-id").key()),
				() -> assertEquals(InternalClueKeys.FOLDER,
						Clue.internal(InternalClueKeys.FOLDER, "artifact-folder").key()),
				() -> assertThrows(IllegalArgumentException.class,
						() -> Clue.internal(InternalClueKeys.TYPE, "clue-type")));
	}

}
