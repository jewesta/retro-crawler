package com.retrocrawler.model.identifier;

import java.util.Objects;

/**
 * Four-digit catalogue number printed for a Sega Game Gear cartridge release.
 */
public record SegaGameGearCartridgeCode(String value) {

	public SegaGameGearCartridgeCode {
		Objects.requireNonNull(value, "value");
		if (!value.matches("\\d{4}")) {
			throw new IllegalArgumentException("Game Gear cartridge code must contain four digits: " + value);
		}
	}

	@Override
	public String toString() {
		return value;
	}
}
