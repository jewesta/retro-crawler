package com.retrocrawler.core.archive.clues;

import java.util.Objects;
import java.util.Random;
import java.util.Set;

import com.retrocrawler.core.util.JacksonSerializable;
import com.retrocrawler.core.util.RetroAttribute;

public class Clue implements RetroAttribute {

	public static final String PREFIX_ANONYMOUS = "_";

	public static final String KEY_INTERNAL_ID = JacksonSerializable.PREFIX_INTERNAL + "id";

	private static final Random RANDOM = new Random();

	private final String key;

	private final Set<String> value;

	Clue(final String key, final Set<String> values) {
		this.key = key;
		this.value = Objects.requireNonNull(values, "values");
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public Set<String> getValue() {
		return value;
	}

	public int size() {
		return value.size();
	}

	public boolean isAnonymous() {
		return key.startsWith(PREFIX_ANONYMOUS);
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
		if (!key.startsWith(JacksonSerializable.PREFIX_INTERNAL)) {
			throw new IllegalArgumentException("Expected internal key starting with '"
					+ JacksonSerializable.PREFIX_INTERNAL + "' but got: '" + key + "'.");
		}
		if (key.equals(JacksonSerializable.TYPE)) {
			throw new IllegalArgumentException(
					JacksonSerializable.TYPE + " is reserved for serialization and cannot be used.");
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
		if (key.startsWith(JacksonSerializable.PREFIX_INTERNAL)) {
			throw new IllegalArgumentException("Expected key that doesn't start '" + JacksonSerializable.PREFIX_INTERNAL
					+ "'. This prefix is reserved for internal keys and cannot be used. Offending key: '" + key + "'.");
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[key=" + key + ", value=" + value + "]";
	}

}
