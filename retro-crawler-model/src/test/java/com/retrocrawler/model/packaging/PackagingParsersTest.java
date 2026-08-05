package com.retrocrawler.model.packaging;

import static com.retrocrawler.model.ParserTestContext.CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.gear.Confidence;

class PackagingParsersTest {

	@Test
	void parsesCanonicalEnglishPackagingOrigins() {
		final PackagingOriginParser parser = new PackagingOriginParser();

		assertEquals(PackagingOrigin.ORIGINAL, parser.parse(" Original Packaging ", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("OVP", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("boxed", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse(null, CONTEXT).confidence());
	}

	@Test
	void parsesCanonicalEnglishSealStates() {
		final SealStateParser parser = new SealStateParser();

		assertEquals(SealState.SEALED, parser.parse(" Sealed ", CONTEXT).value().orElseThrow());
		assertEquals(SealState.OPENED, parser.parse("OPENED", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("CIB", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("NIB", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse(null, CONTEXT).confidence());
	}

	@Test
	void keepsPackagingOriginAndSealStateIndependent() {
		assertEquals(Confidence.NONE, new PackagingOriginParser().parse("sealed", CONTEXT).confidence());
		assertEquals(Confidence.NONE, new SealStateParser().parse("original packaging", CONTEXT).confidence());
	}
}
