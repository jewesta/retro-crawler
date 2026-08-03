package com.retrocrawler.model.identifier;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Nintendo Game Boy-family ROM ID, distinct from the code printed on the
 * cartridge label.
 */
public record NintendoGameBoyRomId(NintendoGameBoyPlatform platform, String gameCode, int revision) {

	private static final Pattern ROM_ID = Pattern.compile("^(DMG|CGB|AGB)-([A-Z0-9]{3,4})-([0-9]+)$");

	public NintendoGameBoyRomId {
		Objects.requireNonNull(platform, "platform");
		gameCode = Objects.requireNonNull(gameCode, "gameCode").trim().toUpperCase(Locale.ROOT);
		if (!gameCode.matches("[A-Z0-9]+") || !platform.supportsRomGameCodeLength(gameCode.length())) {
			throw new IllegalArgumentException("Unsupported Game Boy ROM game code: " + gameCode);
		}
		if (revision < 0) {
			throw new IllegalArgumentException("Game Boy ROM revision must not be negative.");
		}
	}

	public static Optional<NintendoGameBoyRomId> parse(final String rawValue) {
		if (rawValue == null) {
			return Optional.empty();
		}
		final Matcher matcher = ROM_ID.matcher(rawValue.trim().toUpperCase(Locale.ROOT));
		if (!matcher.matches()) {
			return Optional.empty();
		}
		try {
			final NintendoGameBoyPlatform platform = NintendoGameBoyPlatform.fromCode(matcher.group(1)).orElseThrow();
			return Optional
					.of(new NintendoGameBoyRomId(platform, matcher.group(2), Integer.parseInt(matcher.group(3))));
		} catch (final IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	@Override
	public String toString() {
		return platform.code() + "-" + gameCode + "-" + revision;
	}
}
