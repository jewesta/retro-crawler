package com.retrocrawler.core.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.gear.parser.FactParseContext;

class GearResolverFactoryPathTest {

	@Test
	void autoDetectsPathParserForScalarAndCollectionFacts() {
		final GearResolver resolver = new GearResolverFactory().reflectOn(Set.of(PathGear.class));
		final Artifact artifact = new Artifact(Set.of(Clue.of("picture", "gear/front.jpeg"),
				Clue.of("images", Set.of("gear/disk-one.img", "gear/disk-two.img"))));
		final Path root = Path.of("/mounted/archive");

		final PathGear gear = (PathGear) resolver
				.resolve(artifact, FactParseContext.located(root, root.resolve("gear"))).orElseThrow();

		assertEquals(root.resolve("gear/front.jpeg"), gear.picture);
		assertEquals(Set.of(root.resolve("gear/disk-one.img"), root.resolve("gear/disk-two.img")), gear.images);
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class PathGear {

		@RetroFact
		private Path picture;

		@RetroFact
		private Set<Path> images = Set.of();
	}
}
