package com.retrocrawler.model.condition;

import static com.retrocrawler.model.ParserTestContext.CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.gear.Confidence;

class ConditionParsersTest {

	@Test
	void parsesThePortableItemConditionVocabularyWithoutInventingAnOrder() {
		final ItemConditionParser parser = new ItemConditionParser();

		assertEquals(ItemCondition.NEW, parser.parse("new", CONTEXT).value().orElseThrow());
		assertEquals(ItemCondition.USED, parser.parse(" USED ", CONTEXT).value().orElseThrow());
		assertEquals(ItemCondition.REFURBISHED, parser.parse("refurbished", CONTEXT).value().orElseThrow());
		assertEquals(ItemCondition.DAMAGED, parser.parse("damaged", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("mint", CONTEXT).confidence());
	}

	@Test
	void keepsFunctionalConditionSeparateFromBroadItemCondition() {
		final FunctionalConditionParser parser = new FunctionalConditionParser();

		assertEquals(FunctionalCondition.WORKING, parser.parse("working", CONTEXT).value().orElseThrow());
		assertEquals(FunctionalCondition.PARTIALLY_DEFECTIVE,
				parser.parse("partially defective", CONTEXT).value().orElseThrow());
		assertEquals(FunctionalCondition.DEFECTIVE, parser.parse("defective", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("damaged", CONTEXT).confidence());
	}

	@Test
	void parsesSpecificDamageWithoutClaimingGeneralConditionWords() {
		final DamageKindParser parser = new DamageKindParser();

		assertEquals(DamageKind.BATTERY_DAMAGE, parser.parse("battery damage", CONTEXT).value().orElseThrow());
		assertEquals(DamageKind.BREAKAGE, parser.parse("BREAKAGE", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("damaged", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("defective", CONTEXT).confidence());
	}
}
