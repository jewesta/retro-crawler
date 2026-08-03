package com.retrocrawler.model.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class ConditionParsersTest {

	@Test
	void parsesThePortableItemConditionVocabularyWithoutInventingAnOrder() {
		final ItemConditionParser parser = new ItemConditionParser();

		assertEquals(ItemCondition.NEW, parser.parse("new").getValue().orElseThrow());
		assertEquals(ItemCondition.USED, parser.parse(" USED ").getValue().orElseThrow());
		assertEquals(ItemCondition.REFURBISHED,
				parser.parse("refurbished").getValue().orElseThrow());
		assertEquals(ItemCondition.DAMAGED, parser.parse("damaged").getValue().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("mint").getConfidence());
	}

	@Test
	void keepsFunctionalConditionSeparateFromBroadItemCondition() {
		final FunctionalConditionParser parser = new FunctionalConditionParser();

		assertEquals(FunctionalCondition.WORKING,
				parser.parse("working").getValue().orElseThrow());
		assertEquals(FunctionalCondition.PARTIALLY_DEFECTIVE,
				parser.parse("partially defective").getValue().orElseThrow());
		assertEquals(FunctionalCondition.DEFECTIVE,
				parser.parse("defective").getValue().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("damaged").getConfidence());
	}

	@Test
	void parsesSpecificDamageWithoutClaimingGeneralConditionWords() {
		final DamageKindParser parser = new DamageKindParser();

		assertEquals(DamageKind.BATTERY_DAMAGE,
				parser.parse("battery damage").getValue().orElseThrow());
		assertEquals(DamageKind.BREAKAGE,
				parser.parse("BREAKAGE").getValue().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("damaged").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("defective").getConfidence());
	}
}
