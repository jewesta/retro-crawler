package com.retrocrawler.model.identifier;

import java.util.Optional;

/**
 * Nintendo platform prefixes used on Game Boy-family cartridge labels.
 */
public enum NintendoGameBoyPlatform {

	GAME_BOY("DMG"),
	GAME_BOY_COLOR("CGB"),
	GAME_BOY_ADVANCE("AGB");

	private final String code;

	NintendoGameBoyPlatform(final String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}

	public boolean supportsCartridgeGameCodeLength(final int length) {
		return this == GAME_BOY ? length >= 2 && length <= 4 : length == 4;
	}

	public boolean supportsRomGameCodeLength(final int length) {
		return this == GAME_BOY ? length >= 3 && length <= 4 : length == 4;
	}

	public static Optional<NintendoGameBoyPlatform> fromCode(final String code) {
		if (code == null) {
			return Optional.empty();
		}
		for (final NintendoGameBoyPlatform platform : values()) {
			if (platform.code.equals(code)) {
				return Optional.of(platform);
			}
		}
		return Optional.empty();
	}
}
