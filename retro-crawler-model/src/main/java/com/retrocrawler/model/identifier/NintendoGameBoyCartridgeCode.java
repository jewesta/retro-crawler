package com.retrocrawler.model.identifier;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;

/**
 * A code printed on a Nintendo Game Boy-family cartridge label.
 *
 * <p>
 * Most labels use a structured {@code DMG}, {@code CGB}, or {@code AGB} prefix,
 * but observed genuine cartridges also contain legacy forms, distributor
 * prefixes, and printing mistakes. The complete canonical text is therefore the
 * identity of this value. Structured accessors are best-effort views and
 * catalogue membership is deliberately separate.
 *
 * <p>
 * This is distinct from the related {@link NintendoGameBoyRomId} printed on the
 * mask ROM. Some legacy label codes are structurally indistinguishable from ROM
 * IDs, so callers must retain the observation's context.
 */
public record NintendoGameBoyCartridgeCode(String value) {

	private static final Pattern CODE_SHAPE = Pattern.compile("[A-Z0-9]{3,4}(?:-[A-Z0-9]{1,8}){1,4}");

	public NintendoGameBoyCartridgeCode {
		value = normalize(value, "value");
		if (!CODE_SHAPE.matcher(value).matches()) {
			throw new IllegalArgumentException("Unsupported Game Boy cartridge code shape: " + value);
		}
	}

	public NintendoGameBoyCartridgeCode(final NintendoGameBoyPlatform platform, final String gameCode,
			final String distributionCode, final OptionalInt revision) {
		this(standardCode(platform, gameCode, distributionCode, revision));
	}

	public NintendoGameBoyCartridgeCode(final NintendoGameBoyPlatform platform, final String gameCode,
			final String distributionCode) {
		this(platform, gameCode, distributionCode, OptionalInt.empty());
	}

	public List<String> segments() {
		return List.of(value.split("-"));
	}

	/**
	 * Returns the platform explicitly present in the first or second segment. A
	 * catalogue entry can still establish the platform for a label whose
	 * printed platform prefix is misspelled.
	 */
	public Optional<NintendoGameBoyPlatform> platform() {
		final List<String> segments = segments();
		for (int index = 0; index < Math.min(2, segments.size()); index++) {
			final Optional<NintendoGameBoyPlatform> platform = NintendoGameBoyPlatform.fromCode(segments.get(index));
			if (platform.isPresent()) {
				return platform;
			}
		}
		return Optional.empty();
	}

	public Optional<String> gameCode() {
		final int platformIndex = platformIndex();
		final List<String> segments = segments();
		return platformIndex >= 0 && platformIndex + 1 < segments.size() ? Optional.of(segments.get(platformIndex + 1))
				: Optional.empty();
	}

	public Optional<String> distributionCode() {
		final int platformIndex = platformIndex();
		final List<String> segments = segments();
		final int candidateIndex = platformIndex + 2;
		if (platformIndex < 0 || candidateIndex >= segments.size()) {
			return Optional.empty();
		}
		final String candidate = segments.get(candidateIndex);
		return candidate.matches("[A-Z0-9]{3,4}") ? Optional.of(candidate) : Optional.empty();
	}

	public OptionalInt revision() {
		final List<String> segments = segments();
		final String candidate = segments.getLast();
		return candidate.matches("[1-9]") ? OptionalInt.of(Integer.parseInt(candidate)) : OptionalInt.empty();
	}

	@Override
	public String toString() {
		return value;
	}

	private int platformIndex() {
		final List<String> segments = segments();
		for (int index = 0; index < Math.min(2, segments.size()); index++) {
			if (NintendoGameBoyPlatform.fromCode(segments.get(index)).isPresent()) {
				return index;
			}
		}
		return -1;
	}

	private static String standardCode(final NintendoGameBoyPlatform platform, final String gameCode,
			final String distributionCode, final OptionalInt revision) {
		Objects.requireNonNull(platform, "platform");
		final String normalizedGameCode = normalize(gameCode, "gameCode");
		final String normalizedDistributionCode = normalize(distributionCode, "distributionCode");
		Objects.requireNonNull(revision, "revision");

		if (!normalizedGameCode.matches("[A-Z0-9]+")
				|| !platform.supportsCartridgeGameCodeLength(normalizedGameCode.length())) {
			throw new IllegalArgumentException("Unsupported Game Boy game code: " + normalizedGameCode);
		}
		if (!normalizedDistributionCode.matches("[A-Z0-9]{3,4}")) {
			throw new IllegalArgumentException("Unsupported Game Boy distribution code: " + normalizedDistributionCode);
		}
		if (revision.isPresent() && (revision.getAsInt() < 1 || revision.getAsInt() > 9)) {
			throw new IllegalArgumentException("Game Boy label revision must be between 1 and 9.");
		}

		final String base = platform.code() + "-" + normalizedGameCode + "-" + normalizedDistributionCode;
		return revision.isPresent() ? base + "-" + revision.getAsInt() : base;
	}

	private static String normalize(final String value, final String name) {
		return Objects.requireNonNull(value, name).trim().toUpperCase(Locale.ROOT);
	}
}
