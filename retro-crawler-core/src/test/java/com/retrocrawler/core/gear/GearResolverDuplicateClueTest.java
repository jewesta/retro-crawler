package com.retrocrawler.core.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.retrocrawler.core.gear.parser.FactParser;
import com.retrocrawler.core.gear.parser.ParseContext;

class GearResolverDuplicateClueTest {

	private static final ARI SOURCE = ARI.of("test", ArchiveId.of("archive"), Path.of("gear"));

	@Test
	void keepsAnAnonymousObservationFromCompetingWithAnExplicitSemanticKey() {
		final GearResolver resolver = new GearResolverFactory().reflectOn(Set.of(BusGear.class));
		final Artifact artifact = new Artifact(Clues.of(Clue.of("bus", "PCI"), Clue.of("AGP")));
		final ParseContext context = new ParseContext(Configuration.builder().build(), SOURCE);

		final GearResolution resolution = resolver.resolveWithIdentity(artifact, context).orElseThrow();

		assertEquals(Set.of(Bus.PCI), ((BusGear) resolution.gear()).bus);
		assertTrue(resolution.trace().resolved().unresolvedClues().stream()
				.anyMatch(clue -> clue.isAnonymous() && clue.value().equals(Set.of("AGP"))));
	}

	@Test
	void combinesSeveralAnonymousObservationsIntoOneCollectionFact() {
		final GearResolver resolver = new GearResolverFactory().reflectOn(Set.of(BusGear.class));
		final Artifact artifact = new Artifact(Clues.of(Clue.of("ISA"), Clue.of("PCI"), Clue.of("AGP")));
		final ParseContext context = new ParseContext(Configuration.builder().build(), SOURCE);

		final BusGear gear = (BusGear) resolver.resolve(artifact, context).orElseThrow();

		assertEquals(Set.of(Bus.ISA, Bus.PCI, Bus.AGP), gear.bus);
	}

	private enum Bus {
		ISA,
		PCI,
		AGP
	}

	public static final class BusParser implements FactParser<Bus> {

		@Override
		public RatedFact<Bus> parse(final String rawValue, final ParseContext context) {
			try {
				return RatedFact.exact(Bus.valueOf(rawValue));
			} catch (final IllegalArgumentException e) {
				return RatedFact.none("Not a bus.");
			}
		}
	}

	@RetroGear(AnyGearMatcher.class)
	public static final class BusGear {

		@RetroFact(parser = BusParser.class, strict = false)
		private Set<Bus> bus = Set.of();
	}
}
