package com.retrocrawler.model.packaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class PackagingParsersTest {

	@Test
	void parsesCanonicalEnglishPackagingOrigins() {
		final PackagingOriginParser parser = new PackagingOriginParser();

		assertEquals(PackagingOrigin.ORIGINAL,
				parser.parse(" Original Packaging ").getValue().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("OVP").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("boxed").getConfidence());
		assertEquals(Confidence.NONE, parser.parse(null).getConfidence());
	}

	@Test
	void parsesCanonicalEnglishSealStates() {
		final SealStateParser parser = new SealStateParser();

		assertEquals(SealState.SEALED, parser.parse(" Sealed ").getValue().orElseThrow());
		assertEquals(SealState.OPENED, parser.parse("OPENED").getValue().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("CIB").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("NIB").getConfidence());
		assertEquals(Confidence.NONE, parser.parse(null).getConfidence());
	}

	@Test
	void keepsPackagingOriginAndSealStateIndependent() {
		assertEquals(Confidence.NONE, new PackagingOriginParser().parse("sealed").getConfidence());
		assertEquals(Confidence.NONE, new SealStateParser().parse("original packaging").getConfidence());
	}
}
