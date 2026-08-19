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
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.gear.parser.ParseContext;

class GearResolverFactoryARITest {

	private static final ARI SOURCE = ARI.of("test", ArchiveId.of("archive"), Path.of("gear"));

	@Test
	void autoDetectsAriParserForScalarAndCollectionFacts() {
		final GearResolver resolver = new GearResolverFactory().reflectOn(Set.of(AriGear.class));
		final Artifact artifact = new Artifact(
				Clues.of(Clue.of("picture", "front.jpeg"), Clue.of("images", Set.of("disk-one.img", "disk-two.img"))));

		final ParseContext context = new ParseContext(Configuration.builder().build(), SOURCE);
		final AriGear gear = (AriGear) resolver.resolve(artifact, context).orElseThrow();

		assertEquals(SOURCE.resolve(Path.of("front.jpeg")), gear.picture);
		assertEquals(Set.of(SOURCE.resolve(Path.of("disk-one.img")), SOURCE.resolve(Path.of("disk-two.img"))),
				gear.images);
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class AriGear {

		@RetroFact
		private ARI picture;

		@RetroFact
		private Set<ARI> images = Set.of();
	}
}
