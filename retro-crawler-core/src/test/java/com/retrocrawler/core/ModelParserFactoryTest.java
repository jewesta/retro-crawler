package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.retrocrawler.core.annotation.RetroClues;
import com.retrocrawler.core.annotation.RetroCollection;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroFactDefaultParser;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.FolderNameClueFinder;
import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.gear.parser.AutoDetectParser;
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.IntParser;
import com.retrocrawler.core.gear.parser.PathParser;
import com.retrocrawler.core.gear.parser.StringParser;

class ModelParserFactoryTest {

	@BeforeEach
	void resetParserObservations() {
		AnnotationStringParser.instances = 0;
		AnnotationIntegerParser.instances = 0;
		AnnotationPathParser.instances = 0;
	}

	@Test
	void annotationSelectsDefaultParserClassesForEveryMatchingFactKey() {
		Model.from(Set.of(AnnotatedDefaultsCollection.class, DefaultFactGear.class));

		assertEquals(2, AnnotationStringParser.instances);
		assertEquals(2, AnnotationIntegerParser.instances);
		assertEquals(2, AnnotationPathParser.instances);
	}

	@Test
	void builderFactoriesConstructDefaultAndExplicitParsersPerEffectiveFactKey() {
		final Map<String, FactParser<String>> strings = new HashMap<>();
		final Map<String, FactParser<Integer>> integers = new HashMap<>();
		final Map<String, FactParser<Path>> paths = new HashMap<>();
		final List<String> explicitKeys = new ArrayList<>();

		Model.builder().typesFrom(Set.of(BuiltInDefaultsCollection.class, FactoryFactGear.class))
				.parserFactory(StringParser.class, key -> strings.computeIfAbsent(key, KeyedStringParser::new))
				.parserFactory(IntParser.class, key -> integers.computeIfAbsent(key, KeyedIntegerParser::new))
				.parserFactory(PathParser.class, key -> paths.computeIfAbsent(key, KeyedPathParser::new))
				.parserFactory(ExplicitStringParser.class, key -> {
					explicitKeys.add(key);
					return new ExplicitStringParser(key);
				}).build();

		assertEquals(Set.of("first", "renamed", "explicitBuiltIn"), strings.keySet());
		assertNotSame(strings.get("first"), strings.get("renamed"));
		assertEquals(Set.of("count", "primitiveCount"), integers.keySet());
		assertNotSame(integers.get("count"), integers.get("primitiveCount"));
		assertEquals(Set.of("photo", "attachments"), paths.keySet());
		assertNotSame(paths.get("photo"), paths.get("attachments"));
		assertEquals(List.of("explicit"), explicitKeys);
	}

	@Test
	void builderFactoryOverridesAnAnnotationSelectedDefaultParserClass() {
		final List<String> keys = new ArrayList<>();

		Model.builder().typesFrom(Set.of(AnnotatedDefaultsCollection.class, StringFactGear.class))
				.parserFactory(AnnotationStringParser.class, key -> {
					keys.add(key);
					return new KeyedStringParser(key);
				}).build();

		assertEquals(List.of("value"), keys);
		assertEquals(0, AnnotationStringParser.instances);
	}

	@Test
	void rejectsDuplicateFactoriesForTheSameSelectedParserClass() {
		final Model.Builder builder = Model.builder().parserFactory(StringParser.class, KeyedStringParser::new);

		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> builder.parserFactory(StringParser.class, KeyedStringParser::new));

		assertTrue(failure.getMessage().contains(StringParser.class.getName()));
	}

	@Test
	void rejectsAFactoryThatReturnsNoParserForASelectedFactKey() {
		final NullPointerException failure = assertThrows(NullPointerException.class,
				() -> Model.builder().typesFrom(Set.of(BuiltInDefaultsCollection.class, StringFactGear.class))
						.parserFactory(StringParser.class, key -> null).build());

		assertTrue(failure.getMessage().contains(StringParser.class.getName()));
		assertTrue(failure.getMessage().contains("value"));
	}

	@Test
	void rejectsAFactoryForTheAutoDetectionMarkerItself() {
		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> Model.builder().parserFactory(AutoDetectParser.class, key -> RatedFact::exact));

		assertTrue(failure.getMessage().contains(AutoDetectParser.class.getSimpleName()));
	}

	@RetroCollection(id = "annotation_default_parsers", locations = "/not/read")
	@RetroClues(fromFolderName = EmptyClueFinder.class)
	@RetroFactDefaultParser(string = AnnotationStringParser.class, integer = AnnotationIntegerParser.class,
			path = AnnotationPathParser.class)
	public static final class AnnotatedDefaultsCollection {
	}

	@RetroCollection(id = "built_in_default_parsers", locations = "/not/read")
	@RetroClues(fromFolderName = EmptyClueFinder.class)
	public static final class BuiltInDefaultsCollection {
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class DefaultFactGear {

		@RetroFact
		private String first;

		@RetroFact(key = "renamed")
		private String second;

		@RetroFact
		private Integer count;

		@RetroFact(optional = false)
		private int primitiveCount;

		@RetroFact
		private Path photo;

		@RetroFact
		private Set<Path> attachments;

		public DefaultFactGear() {
		}
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class FactoryFactGear {

		@RetroFact
		private String first;

		@RetroFact(key = "renamed")
		private String second;

		@RetroFact
		private Integer count;

		@RetroFact(optional = false)
		private int primitiveCount;

		@RetroFact
		private Path photo;

		@RetroFact
		private Set<Path> attachments;

		@RetroFact(key = "explicitBuiltIn", parser = StringParser.class)
		private String explicitBuiltIn;

		@RetroFact(key = "explicit", parser = ExplicitStringParser.class)
		private String explicit;

		public FactoryFactGear() {
		}
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class StringFactGear {

		@RetroFact
		private String value;

		public StringFactGear() {
		}
	}

	public static final class AnnotationStringParser implements FactParser<String> {

		private static int instances;

		public AnnotationStringParser() {
			instances++;
		}

		@Override
		public RatedFact<String> parse(final String rawValue) {
			return RatedFact.exact(rawValue);
		}
	}

	public static final class AnnotationIntegerParser implements FactParser<Integer> {

		private static int instances;

		public AnnotationIntegerParser() {
			instances++;
		}

		@Override
		public RatedFact<Integer> parse(final String rawValue) {
			return RatedFact.exact(Integer.valueOf(rawValue));
		}
	}

	public static final class AnnotationPathParser implements FactParser<Path> {

		private static int instances;

		public AnnotationPathParser() {
			instances++;
		}

		@Override
		public RatedFact<Path> parse(final String rawValue) {
			return RatedFact.exact(Path.of(rawValue));
		}
	}

	private record KeyedStringParser(String key) implements FactParser<String> {

		@Override
		public RatedFact<String> parse(final String rawValue) {
			return RatedFact.exact(rawValue);
		}
	}

	private record KeyedIntegerParser(String key) implements FactParser<Integer> {

		@Override
		public RatedFact<Integer> parse(final String rawValue) {
			return RatedFact.exact(Integer.valueOf(rawValue));
		}
	}

	private record KeyedPathParser(String key) implements FactParser<Path> {

		@Override
		public RatedFact<Path> parse(final String rawValue) {
			return RatedFact.exact(Path.of(rawValue));
		}
	}

	public static final class ExplicitStringParser implements FactParser<String> {

		private final String key;

		private ExplicitStringParser(final String key) {
			this.key = key;
		}

		@Override
		public RatedFact<String> parse(final String rawValue) {
			return RatedFact.exact(key + ":" + rawValue);
		}
	}

	public static final class EmptyClueFinder implements FolderNameClueFinder {

		@Override
		public Set<Clue> find(final String folderName) {
			return Set.of();
		}
	}
}
