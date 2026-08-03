package com.retrocrawler.model.packaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class PackagingParsersTest {

	@Test
	void parsesCanonicalEnglishPackagingOrigins() {
		final PackagingOriginParser parser = new PackagingOriginParser();

		assertEquals(PackagingOrigin.ORIGINAL,
				parser.parse(" Original Packaging ").value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("OVP").confidence());
		assertEquals(Confidence.NONE, parser.parse("boxed").confidence());
		assertEquals(Confidence.NONE, parser.parse(null).confidence());
	}

	@Test
	void parsesCanonicalEnglishSealStates() {
		final SealStateParser parser = new SealStateParser();

		assertEquals(SealState.SEALED, parser.parse(" Sealed ").value().orElseThrow());
		assertEquals(SealState.OPENED, parser.parse("OPENED").value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("CIB").confidence());
		assertEquals(Confidence.NONE, parser.parse("NIB").confidence());
		assertEquals(Confidence.NONE, parser.parse(null).confidence());
	}

	@Test
	void keepsPackagingOriginAndSealStateIndependent() {
		assertEquals(Confidence.NONE, new PackagingOriginParser().parse("sealed").confidence());
		assertEquals(Confidence.NONE, new SealStateParser().parse("original packaging").confidence());
	}
}
