package com.retrocrawler.core.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.Configuration;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.Node;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.gear.parser.ParseContext;

class GearResolverFactoryPathTest {

	private static final ARI SOURCE = ARI.of("test", ArchiveId.of("archive"), Path.of("gear"));

	@Test
	void autoDetectsPathParserForScalarAndCollectionFacts() {
		final GearResolver resolver = new GearResolverFactory().reflectOn(Set.of(PathGear.class));
		final Artifact artifact = new Artifact(
				Clues.of(Clue.of("picture", "front.jpeg"), Clue.of("images", Set.of("disk-one.img", "disk-two.img"))));
		final Path root = Path.of("/mounted/archive");

		final ParseContext context = new ParseContext(Configuration.builder().build(),
				new Node(root, root.resolve("gear")));
		final PathGear gear = (PathGear) resolver.resolve(SOURCE, artifact, context).orElseThrow();

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
