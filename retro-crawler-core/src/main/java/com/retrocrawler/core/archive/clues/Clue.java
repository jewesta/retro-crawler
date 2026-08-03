package com.retrocrawler.core.archive.clues;

import java.util.Objects;
import java.util.Random;
import java.util.Set;

import com.retrocrawler.core.util.RetroAttribute;

/**
 * Model-independent evidence observed while crawling an archive.
 * <p>
 * A clue retains the raw string values and any key explicitly observed by its
 * finder. It may be cached as part of an {@link Artifact}; only later gear
 * resolution may interpret it as a typed
 * {@link com.retrocrawler.core.gear.Fact Fact}.
 */
public class Clue implements RetroAttribute {

	public static final String PREFIX_ANONYMOUS = "_";

	private static final Random RANDOM = new Random();

	private final String key;

	private final Set<String> value;

	Clue(final String key, final Set<String> values) {
		this.key = key;
		this.value = Set.copyOf(Objects.requireNonNull(values, "values"));
	}

	@Override
	public String key() {
		return key;
	}

	@Override
	public Set<String> value() {
		return value;
	}

	public int size() {
		return value.size();
	}

	public boolean isAnonymous() {
		return key.startsWith(PREFIX_ANONYMOUS);
	}

	/**
	 * A missing-value clue records that a known key was deliberately observed even
	 * though no value was supplied. It can establish an artifact and retain source
	 * intent, but cannot be resolved into a fact.
	 */
	public boolean isMissingValue() {
		return !isAnonymous() && value.isEmpty();
	}

	public static Clue of(final String value) {
		return new Clue(createAnonymousKey(), Set.of(value));
	}

	public static Clue of(final String key, final String value) {
		assertUnreserved(key);
		return new Clue(key, Set.of(value));
	}

	public static Clue of(final Set<String> values) {
		return new Clue(createAnonymousKey(), values);
	}

	public static Clue of(final String key, final Set<String> values) {
		assertUnreserved(key);
		return new Clue(key, values);
	}

	public static Clue missingValue(final String key) {
		assertUnreserved(key);
		return new Clue(key, Set.of());
	}

	private static String random(final int length) {
		final String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			final int index = RANDOM.nextInt(chars.length());
			sb.append(chars.charAt(index));
		}
		return sb.toString();
	}

	public static Clue internal(final String key, final String value) {
		if (!key.startsWith(InternalClueKeys.PREFIX)) {
			throw new IllegalArgumentException("Expected internal key starting with '"
					+ InternalClueKeys.PREFIX + "' but got: '" + key + "'.");
		}
		if (key.equals(InternalClueKeys.TYPE)) {
			throw new IllegalArgumentException(
					InternalClueKeys.TYPE + " is reserved for serialization and cannot be used.");
		}
		return new Clue(key, Set.of(value));
	}

	private static String createAnonymousKey() {
		return PREFIX_ANONYMOUS + random(8);
	}

	private static void assertUnreserved(final String key) {
		if (key.startsWith(PREFIX_ANONYMOUS)) {
			throw new IllegalArgumentException("Expected key that doesn't start '" + PREFIX_ANONYMOUS
					+ "'. This prefix is reserved for anonymous keys and cannot be used. Offending key: '" + key
					+ "'.");
		}
		if (key.startsWith(InternalClueKeys.PREFIX)) {
			throw new IllegalArgumentException("Expected key that doesn't start '" + InternalClueKeys.PREFIX
					+ "'. This prefix is reserved for internal keys and cannot be used. Offending key: '" + key + "'.");
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[key=" + key + ", value=" + value + "]";
	}

}
