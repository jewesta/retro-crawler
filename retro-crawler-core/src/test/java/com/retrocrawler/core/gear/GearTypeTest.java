package com.retrocrawler.core.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.annotation.RetroAnyGear;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.gear.matcher.GearMatcher;

class GearTypeTest {

	@Test
	void derivesKeyAndNameFromTheGearClass() {
		assertEquals(new GearType("graphics-card", "graphics card"), GearType.from(GraphicsCard.class));
	}

	@Test
	void honorsIndependentKeyAndNameOverrides() {
		assertEquals(new GearType("video-card", "Graphics adapter"), GearType.from(CustomizedCard.class));
	}

	@Test
	void derivesAndOverridesRetroAnyGearMetadataTheSameWay() {
		assertEquals(new GearType("mystery-card", "Unidentified card"), GearType.from(MysteryCard.class));
	}

	@Test
	void rejectsATypeDeclaredAsBothMatchedAndRetroAnyGear() {
		assertThrows(IllegalArgumentException.class, () -> GearType.from(ContradictingCard.class));
	}

	@Test
	void rejectsKeysOutsideTheGearTypeKeyVocabulary() {
		assertThrows(IllegalArgumentException.class, () -> new GearType("GraphicsCard", "graphics card"));
	}

	@RetroGear(TestMatcher.class)
	private static final class GraphicsCard {
	}

	@RetroGear(value = TestMatcher.class, key = "video-card", name = "Graphics adapter")
	private static final class CustomizedCard {
	}

	@RetroAnyGear(name = "Unidentified card")
	private static final class MysteryCard {
	}

	@RetroAnyGear
	@RetroGear(TestMatcher.class)
	private static final class ContradictingCard {
	}

	private static final class TestMatcher implements GearMatcher {

		@Override
		public Confidence matches(final GearContext context) {
			return Confidence.NONE;
		}
	}
}
