package com.retrocrawler.core.archive.clues;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.util.RetroAttribute;

/**
 * Model-independent evidence observed while crawling an archive.
 * <p>
 * A clue retains the raw string values and any key explicitly observed by its
 * finder. It may be cached as part of an {@link Artifact}; only later gear
 * resolution may interpret it as a typed {@link com.retrocrawler.core.gear.Fact
 * Fact}. The crawler attaches the producing finder's unique simple class name.
 * A finder may additionally declare exact contributing resources by ARI; an
 * empty source list deliberately makes no resource-level provenance claim.
 */
public class Clue implements RetroAttribute {

	public static final String PREFIX_ANONYMOUS = "_";

	private static final Random RANDOM = new Random();

	private final String key;

	private final Set<String> value;

	private final String finder;

	private final List<ARI> sources;

	Clue(final String key, final Set<String> values) {
		this(key, values, null, List.of());
	}

	Clue(final String key, final Set<String> values, final String finder, final Iterable<ARI> sources) {
		this.key = key;
		this.value = Set.copyOf(Objects.requireNonNull(values, "values"));
		if (finder != null && finder.isBlank()) {
			throw new IllegalArgumentException("finder must not be blank.");
		}
		this.finder = finder;
		final LinkedHashSet<ARI> distinctSources = new LinkedHashSet<>();
		for (final ARI source : Objects.requireNonNull(sources, "sources")) {
			if (!distinctSources.add(Objects.requireNonNull(source, "sources must not contain null"))) {
				throw new IllegalArgumentException("Clue sources must not contain duplicates: " + source);
			}
		}
		this.sources = List.copyOf(distinctSources);
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

	/** The finder that produced this clue, once the crawler has attached it. */
	public Optional<String> finder() {
		return Optional.ofNullable(finder);
	}

	/**
	 * Exact resources the finder explicitly declared as contributing evidence.
	 */
	public List<ARI> sources() {
		return sources;
	}

	/**
	 * Returns this clue with one additional explicitly declared source.
	 * <p>
	 * Source declaration is deliberate: the crawler never infers provenance
	 * merely because a finder accessed a resource.
	 */
	public Clue from(final ARI source) {
		Objects.requireNonNull(source, "source");
		if (sources.contains(source)) {
			return this;
		}
		final List<ARI> combined = new ArrayList<>(sources);
		combined.add(source);
		return new Clue(key, value, finder, combined);
	}

	/** Returns this clue with the additional explicitly declared sources. */
	public Clue from(final Iterable<ARI> additionalSources) {
		Clue sourced = this;
		for (final ARI source : Objects.requireNonNull(additionalSources, "additionalSources")) {
			sourced = sourced.from(source);
		}
		return sourced;
	}

	public boolean isAnonymous() {
		return key.startsWith(PREFIX_ANONYMOUS);
	}

	/**
	 * A missing-value clue records that a known key was deliberately observed
	 * even though no value was supplied. It can establish an artifact and
	 * retain source intent, but cannot be resolved into a fact.
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
			throw new IllegalArgumentException(
					"Expected internal key starting with '" + InternalClueKeys.PREFIX + "' but got: '" + key + "'.");
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

	Clue foundBy(final String finderName) {
		Objects.requireNonNull(finderName, "finderName");
		return finderName.equals(finder) ? this : new Clue(key, value, finderName, sources);
	}

	Clue rekeyAnonymous() {
		if (!isAnonymous()) {
			throw new IllegalStateException("Only an anonymous clue may be re-keyed.");
		}
		return new Clue(createAnonymousKey(), value, finder, sources);
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
		return getClass().getSimpleName() + "[key=" + key + ", value=" + value + ", finder=" + finder + ", sources="
				+ sources + "]";
	}

}
