package com.retrocrawler.mycollection.facts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.model.appearance.Color;
import com.retrocrawler.model.commerce.Money;
import com.retrocrawler.model.condition.DamageKind;
import com.retrocrawler.model.condition.FunctionalCondition;
import com.retrocrawler.model.condition.ItemCondition;
import com.retrocrawler.model.identifier.TheRetroWebId;
import com.retrocrawler.model.identifier.TheRetroWebIdParser;
import com.retrocrawler.model.locale.LanguageCode;
import com.retrocrawler.model.locale.RegionCode;
import com.retrocrawler.model.measurement.DataCapacity;
import com.retrocrawler.model.measurement.DataCapacityParser;
import com.retrocrawler.model.measurement.ScreenSize;
import com.retrocrawler.model.packaging.PackagingOrigin;
import com.retrocrawler.model.packaging.SealState;
import com.retrocrawler.model.temporal.DateMarking;
import com.retrocrawler.model.temporal.YearWeek;
import com.retrocrawler.mycollection.catalog.Destiny;
import com.retrocrawler.mycollection.catalog.Tested;
import com.retrocrawler.mycollection.memory.RamSet;

class CollectionFactParsersTest {

	@Test
	void adaptsGermanCalendarWeeksWithoutMakingMonthsAmbiguous() {
		final CollectionDateMarkingParser parser = new CollectionDateMarkingParser();

		assertEquals(DateMarking.of(YearMonth.of(1994, 5)),
				parser.parse("1994-05").getValue().orElseThrow());
		assertEquals(DateMarking.of(new YearWeek(1994, 31)),
				parser.parse("1994-KW31").getValue().orElseThrow());
		assertEquals(DateMarking.of(LocalDate.of(1994, 2, 22)),
				parser.parse("1994-02-22").getValue().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("KW31 1994").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("02-22-1994").getConfidence());
	}

	@Test
	void parsesTheRetroWebIdsAsExternalReferences() {
		final var fact = new TheRetroWebIdParser().parse("10510");

		assertEquals(Confidence.EXACT, fact.getConfidence());
		assertEquals(new TheRetroWebId(10510), fact.getValue().orElseThrow());
		assertEquals(Confidence.NONE, new TheRetroWebIdParser().parse("motherboard-10510").getConfidence());
	}

	@Test
	void parsesCapacitiesWithDecimalCommaAndCanonicalUnits() {
		final DataCapacityParser parser = new DataCapacityParser();

		assertEquals(new DataCapacity(new BigDecimal("1.125"), DataCapacity.Unit.MB),
				parser.parse("1,125MB").getValue().orElseThrow());
		assertEquals(new DataCapacity(BigDecimal.valueOf(32), DataCapacity.Unit.KB),
				parser.parse("32kb").getValue().orElseThrow());
		assertEquals(new DataCapacity(BigDecimal.valueOf(32), DataCapacity.Unit.KB),
				parser.parse("32KB").getValue().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("3,5").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("3,3V").getConfidence());
	}

	@Test
	void parsesOnlyCanonicalCountFirstRamSets() {
		final RamSetParser parser = new RamSetParser();

		final RamSet set = (RamSet) parser.parse("2 x 16MB").getValue().orElseThrow();
		assertEquals(2, set.memberCount());
		assertEquals(new DataCapacity(BigDecimal.valueOf(16), DataCapacity.Unit.MB), set.capacityPerMember());
		assertEquals(new DataCapacity(BigDecimal.valueOf(32), DataCapacity.Unit.MB), set.totalCapacity());

		assertEquals(Confidence.NONE, parser.parse("32kb x 4").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("2 x 4 x 256kb plus Parity").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("? x 100MB").getConfidence());
	}

	@Test
	void comparesEquivalentCapacitiesAcrossUnits() {
		final DataCapacity oneGigabyte = new DataCapacity(BigDecimal.ONE, DataCapacity.Unit.GB);
		final DataCapacity twoTimes512Megabytes = new DataCapacity(BigDecimal.valueOf(512),
				DataCapacity.Unit.MB).multiply(2);

		assertTrue(oneGigabyte.sameSizeAs(twoTimes512Megabytes));
	}

	@Test
	void parsesTheCollectionLifecycleVocabulary() {
		assertEquals(Destiny.VERSCHENKT,
				new DestinyParser().parse("verschenkt").getValue().orElseThrow());
		assertEquals(Destiny.ENTSORGT,
				new DestinyParser().parse("ENTSORGT").getValue().orElseThrow());
		assertEquals(Confidence.NONE, new DestinyParser().parse("weitergegeben").getConfidence());

		assertEquals(Tested.POST, new TestedParser().parse("post").getValue().orElseThrow());
		assertEquals(Tested.BOOT, new TestedParser().parse("boot").getValue().orElseThrow());
		assertEquals(Tested.FULL, new TestedParser().parse("full").getValue().orElseThrow());
		assertEquals(Confidence.NONE, new TestedParser().parse("bios").getConfidence());
	}

	@Test
	void keepsItemConditionFunctionalHealthAndSpecificDamageIndependent() {
		final CollectionItemConditionParser itemCondition = new CollectionItemConditionParser();
		final CollectionFunctionalConditionParser functionalCondition =
				new CollectionFunctionalConditionParser();
		final CollectionDamageKindParser damage = new CollectionDamageKindParser();

		assertEquals(ItemCondition.NEW, itemCondition.parse("Neu").getValue().orElseThrow());
		assertEquals(ItemCondition.USED, itemCondition.parse("gebraucht").getValue().orElseThrow());
		assertEquals(ItemCondition.REFURBISHED,
				itemCondition.parse("refurbished").getValue().orElseThrow());
		assertEquals(ItemCondition.DAMAGED,
				itemCondition.parse("beschädigt").getValue().orElseThrow());

		assertEquals(FunctionalCondition.WORKING,
				functionalCondition.parse("working").getValue().orElseThrow());
		assertEquals(FunctionalCondition.PARTIALLY_DEFECTIVE,
				functionalCondition.parse("teildefekt").getValue().orElseThrow());
		assertEquals(FunctionalCondition.DEFECTIVE,
				functionalCondition.parse("defekt").getValue().orElseThrow());

		assertEquals(DamageKind.BATTERY_DAMAGE,
				damage.parse("Akkuschaden").getValue().orElseThrow());
		assertEquals(DamageKind.BREAKAGE, damage.parse("Bruch").getValue().orElseThrow());

		assertEquals(Confidence.NONE, itemCondition.parse("defekt").getConfidence());
		assertEquals(Confidence.NONE, functionalCondition.parse("beschädigt").getConfidence());
		assertEquals(Confidence.NONE, damage.parse("beschädigt").getConfidence());
	}

	@Test
	void adaptsCollectionPackagingLanguageWithoutInferringRelatedFacts() {
		final CollectionPackagingOriginParser packagingOrigin = new CollectionPackagingOriginParser();
		final CollectionSealStateParser sealState = new CollectionSealStateParser();

		assertEquals(PackagingOrigin.ORIGINAL,
				packagingOrigin.parse("OVP").getValue().orElseThrow());
		assertEquals(PackagingOrigin.ORIGINAL,
				packagingOrigin.parse("original packaging").getValue().orElseThrow());
		assertEquals(SealState.SEALED, sealState.parse("sealed").getValue().orElseThrow());
		assertEquals(SealState.SEALED, sealState.parse("versiegelt").getValue().orElseThrow());
		assertEquals(SealState.OPENED, sealState.parse("geöffnet").getValue().orElseThrow());
		assertEquals(SealState.OPENED, sealState.parse("opened").getValue().orElseThrow());

		for (final String unrelated : new String[] { "CIB", "NIB", "NOS", "lose" }) {
			assertEquals(Confidence.NONE, packagingOrigin.parse(unrelated).getConfidence());
			assertEquals(Confidence.NONE, sealState.parse(unrelated).getConfidence());
		}
		assertEquals(Confidence.NONE, packagingOrigin.parse("sealed").getConfidence());
		assertEquals(Confidence.NONE, sealState.parse("OVP").getConfidence());
	}

	@Test
	void defaultsBareMonetaryAmountsToEuros() {
		final MoneyParser parser = new MoneyParser();

		assertEquals(new Money(new BigDecimal("12.34"), Currency.getInstance("EUR")),
				parser.parse("12,34").getValue().orElseThrow());
		assertEquals(new Money(new BigDecimal("120"), Currency.getInstance("EUR")),
				parser.parse("120 EUR").getValue().orElseThrow());
		assertEquals(new Money(new BigDecimal("20"), Currency.getInstance("USD")),
				parser.parse("20 usd").getValue().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("20 EURO").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("-20 EUR").getConfidence());
	}

	@Test
	void limitsAnonymousLocaleParsingToEstablishedCollectionMarkers() {
		final CollectionLanguageCodeParser language = new CollectionLanguageCodeParser();
		final CollectionRegionCodeParser region = new CollectionRegionCodeParser();

		assertEquals(new LanguageCode("en"), language.parse("EN").getValue().orElseThrow());
		assertEquals(new RegionCode("US"), region.parse("US").getValue().orElseThrow());
		assertEquals(new RegionCode("EUR"), region.parse("EU").getValue().orElseThrow());
		assertEquals(new RegionCode("EUR"), region.parse("EUR").getValue().orElseThrow());
		assertEquals(Confidence.NONE, language.parse("SD").getConfidence());
		assertEquals(Confidence.NONE, language.parse("EU").getConfidence());
		assertEquals(Confidence.NONE, region.parse("AT").getConfidence());

		for (final String code : new String[] { "DE", "ES", "FR", "IT" }) {
			assertEquals(Confidence.EXACT, language.parse(code).getConfidence());
			assertEquals(Confidence.EXACT, region.parse(code).getConfidence());
		}
	}

	@Test
	void limitsAnonymousScreenSizesToEstablishedCollectionEvidence() {
		final CollectionScreenSizeParser parser = new CollectionScreenSizeParser();

		assertEquals(new ScreenSize(BigDecimal.valueOf(19)), parser.parse("19″").getValue().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("1,8″").getConfidence());
		assertEquals(Confidence.NONE, parser.parse("24″").getConfidence());
	}

	@Test
	void adaptsCollectionColorLanguageWithoutTreatingTransparencyAsAColor() {
		final CollectionColorParser parser = new CollectionColorParser();

		assertEquals(Color.BLACK, parser.parse("schwarz").getValue().orElseThrow());
		assertEquals(Color.GREEN, parser.parse("grün").getValue().orElseThrow());
		assertEquals(Color.PURPLE, parser.parse("lila").getValue().orElseThrow());
		assertEquals(Set.of(Color.WHITE, Color.PINK), parser.parse("weiß-pink").getValue().orElseThrow());
		assertEquals(Set.of(Color.WHITE, Color.PINK), parser.parse("weiß/pink").getValue().orElseThrow());
		assertEquals(Color.WHITE, parser.parse("white").getValue().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("transparent").getConfidence());
	}
}
