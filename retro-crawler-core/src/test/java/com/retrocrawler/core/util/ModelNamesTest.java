package com.retrocrawler.core.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ModelNamesTest {

	@Test
	void derivesTypeKeysAndDisplayNamesFromJavaWords() throws NoSuchFieldException {
		assertAll(() -> assertEquals("graphics-card", ModelNames.typeKey(GraphicsCard.class)),
				() -> assertEquals("graphics card", ModelNames.displayName(GraphicsCard.class)),
				() -> assertEquals("cpu-card", ModelNames.typeKey(CPUCard.class)),
				() -> assertEquals("cpu card", ModelNames.displayName(CPUCard.class)),
				() -> assertEquals("the retro web id",
						ModelNames.displayName(Facts.class.getDeclaredField("theRetroWebId"))),
				() -> assertEquals("lot price", ModelNames.displayName("lot-price")));
	}

	private static final class GraphicsCard {
	}

	private static final class CPUCard {
	}

	private static final class Facts {

		@SuppressWarnings("unused")
		private String theRetroWebId;
	}
}
