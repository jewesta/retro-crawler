package com.retrocrawler.model.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;

class ConditionParsersTest {

	@Test
	void parsesThePortableItemConditionVocabularyWithoutInventingAnOrder() {
		final ItemConditionParser parser = new ItemConditionParser();

		assertEquals(ItemCondition.NEW, parser.parse("new").value().orElseThrow());
		assertEquals(ItemCondition.USED, parser.parse(" USED ").value().orElseThrow());
		assertEquals(ItemCondition.REFURBISHED, parser.parse("refurbished").value().orElseThrow());
		assertEquals(ItemCondition.DAMAGED, parser.parse("damaged").value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("mint").confidence());
	}

	@Test
	void keepsFunctionalConditionSeparateFromBroadItemCondition() {
		final FunctionalConditionParser parser = new FunctionalConditionParser();

		assertEquals(FunctionalCondition.WORKING, parser.parse("working").value().orElseThrow());
		assertEquals(FunctionalCondition.PARTIALLY_DEFECTIVE,
				parser.parse("partially defective").value().orElseThrow());
		assertEquals(FunctionalCondition.DEFECTIVE, parser.parse("defective").value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("damaged").confidence());
	}

	@Test
	void parsesSpecificDamageWithoutClaimingGeneralConditionWords() {
		final DamageKindParser parser = new DamageKindParser();

		assertEquals(DamageKind.BATTERY_DAMAGE, parser.parse("battery damage").value().orElseThrow());
		assertEquals(DamageKind.BREAKAGE, parser.parse("BREAKAGE").value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("damaged").confidence());
		assertEquals(Confidence.NONE, parser.parse("defective").confidence());
	}
}
