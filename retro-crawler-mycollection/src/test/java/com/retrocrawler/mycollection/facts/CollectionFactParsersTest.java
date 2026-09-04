package com.retrocrawler.mycollection.facts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.Configuration;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.gear.Confidence;
import com.retrocrawler.core.gear.parser.ParseContext;
import com.retrocrawler.model.appearance.Color;
import com.retrocrawler.model.commerce.Money;
import com.retrocrawler.model.condition.DamageKind;
import com.retrocrawler.model.condition.FunctionalCondition;
import com.retrocrawler.model.condition.ItemCondition;
import com.retrocrawler.model.hardware.PrinterType;
import com.retrocrawler.model.identifier.TheRetroWebId;
import com.retrocrawler.model.identifier.TheRetroWebIdParser;
import com.retrocrawler.model.locale.LanguageCode;
import com.retrocrawler.model.locale.RegionCode;
import com.retrocrawler.model.measurement.DataCapacity;
import com.retrocrawler.model.measurement.DataCapacityParser;
import com.retrocrawler.model.measurement.Length;
import com.retrocrawler.model.measurement.Length.Unit;
import com.retrocrawler.model.packaging.PackagingOrigin;
import com.retrocrawler.model.packaging.SealState;
import com.retrocrawler.model.temporal.DateMarking;
import com.retrocrawler.model.temporal.YearWeek;
import com.retrocrawler.mycollection.catalog.Destiny;
import com.retrocrawler.mycollection.catalog.Tested;

class CollectionFactParsersTest {

	private static final ParseContext CONTEXT = new ParseContext(Configuration.builder().build(),
			ARI.of("test", ArchiveId.of("archive"), Path.of("gear")));

	@Test
	void adaptsGermanCalendarWeeksWithoutMakingMonthsAmbiguous() {
		final CollectionDateMarkingParser parser = new CollectionDateMarkingParser();

		assertEquals(DateMarking.of(YearMonth.of(1994, 5)), parser.parse("1994-05", CONTEXT).value().orElseThrow());
		assertEquals(DateMarking.of(new YearWeek(1994, 31)), parser.parse("1994-KW31", CONTEXT).value().orElseThrow());
		assertEquals(DateMarking.of(LocalDate.of(1994, 2, 22)),
				parser.parse("1994-02-22", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("KW31 1994", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("02-22-1994", CONTEXT).confidence());
	}

	@Test
	void parsesTheRetroWebIdsAsExternalReferences() {
		final var fact = new TheRetroWebIdParser().parse("10510", CONTEXT);

		assertEquals(Confidence.EXACT, fact.confidence());
		assertEquals(new TheRetroWebId(10510), fact.value().orElseThrow());
		assertEquals(Confidence.NONE, new TheRetroWebIdParser().parse("motherboard-10510", CONTEXT).confidence());
	}

	@Test
	void parsesCapacitiesWithDecimalCommaAndCanonicalUnits() {
		final DataCapacityParser parser = new DataCapacityParser();

		assertEquals(new DataCapacity(new BigDecimal("1.125"), DataCapacity.Unit.MB),
				parser.parse("1,125MB", CONTEXT).value().orElseThrow());
		assertEquals(new DataCapacity(BigDecimal.valueOf(32), DataCapacity.Unit.KB),
				parser.parse("32kb", CONTEXT).value().orElseThrow());
		assertEquals(new DataCapacity(BigDecimal.valueOf(32), DataCapacity.Unit.KB),
				parser.parse("32KB", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("3,5", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("3,3V", CONTEXT).confidence());
	}

	@Test
	void comparesEquivalentCapacitiesAcrossUnits() {
		final DataCapacity oneGigabyte = new DataCapacity(BigDecimal.ONE, DataCapacity.Unit.GB);
		final DataCapacity twoTimes512Megabytes = new DataCapacity(BigDecimal.valueOf(512), DataCapacity.Unit.MB)
				.multiply(2);

		assertTrue(oneGigabyte.sameSizeAs(twoTimes512Megabytes));
	}

	@Test
	void parsesTheCollectionLifecycleVocabulary() {
		assertEquals(Destiny.VERSCHENKT, new DestinyParser().parse("verschenkt", CONTEXT).value().orElseThrow());
		assertEquals(Destiny.ENTSORGT, new DestinyParser().parse("ENTSORGT", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, new DestinyParser().parse("weitergegeben", CONTEXT).confidence());

		assertEquals(Tested.POST, new TestedParser().parse("post", CONTEXT).value().orElseThrow());
		assertEquals(Tested.BOOT, new TestedParser().parse("boot", CONTEXT).value().orElseThrow());
		assertEquals(Tested.FULL, new TestedParser().parse("full", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, new TestedParser().parse("bios", CONTEXT).confidence());
	}

	@Test
	void mapsExistingCollectionPrinterVocabularyToPortablePrinterTypes() {
		final PrinterTypeParser parser = new PrinterTypeParser();

		assertEquals(PrinterType.DOT_MATRIX_9_PIN, parser.parse("9-Nadel", CONTEXT).value().orElseThrow());
	}

	@Test
	void keepsItemConditionFunctionalHealthAndSpecificDamageIndependent() {
		final CollectionItemConditionParser itemCondition = new CollectionItemConditionParser();
		final CollectionFunctionalConditionParser functionalCondition = new CollectionFunctionalConditionParser();
		final CollectionDamageKindParser damage = new CollectionDamageKindParser();

		assertEquals(ItemCondition.NEW, itemCondition.parse("Neu", CONTEXT).value().orElseThrow());
		assertEquals(ItemCondition.USED, itemCondition.parse("gebraucht", CONTEXT).value().orElseThrow());
		assertEquals(ItemCondition.REFURBISHED, itemCondition.parse("refurbished", CONTEXT).value().orElseThrow());
		assertEquals(ItemCondition.DAMAGED, itemCondition.parse("beschädigt", CONTEXT).value().orElseThrow());

		assertEquals(FunctionalCondition.WORKING, functionalCondition.parse("working", CONTEXT).value().orElseThrow());
		assertEquals(FunctionalCondition.PARTIALLY_DEFECTIVE,
				functionalCondition.parse("teildefekt", CONTEXT).value().orElseThrow());
		assertEquals(FunctionalCondition.DEFECTIVE, functionalCondition.parse("defekt", CONTEXT).value().orElseThrow());

		assertEquals(DamageKind.BATTERY_DAMAGE, damage.parse("Akkuschaden", CONTEXT).value().orElseThrow());
		assertEquals(DamageKind.BREAKAGE, damage.parse("Bruch", CONTEXT).value().orElseThrow());

		assertEquals(Confidence.NONE, itemCondition.parse("defekt", CONTEXT).confidence());
		assertEquals(Confidence.NONE, functionalCondition.parse("beschädigt", CONTEXT).confidence());
		assertEquals(Confidence.NONE, damage.parse("beschädigt", CONTEXT).confidence());
	}

	@Test
	void adaptsCollectionPackagingLanguageWithoutInferringRelatedFacts() {
		final CollectionPackagingOriginParser packagingOrigin = new CollectionPackagingOriginParser();
		final CollectionSealStateParser sealState = new CollectionSealStateParser();

		assertEquals(PackagingOrigin.ORIGINAL, packagingOrigin.parse("OVP", CONTEXT).value().orElseThrow());
		assertEquals(PackagingOrigin.ORIGINAL,
				packagingOrigin.parse("original packaging", CONTEXT).value().orElseThrow());
		assertEquals(SealState.SEALED, sealState.parse("sealed", CONTEXT).value().orElseThrow());
		assertEquals(SealState.SEALED, sealState.parse("versiegelt", CONTEXT).value().orElseThrow());
		assertEquals(SealState.OPENED, sealState.parse("geöffnet", CONTEXT).value().orElseThrow());
		assertEquals(SealState.OPENED, sealState.parse("opened", CONTEXT).value().orElseThrow());

		for (final String unrelated : new String[] {
				"CIB", "NIB", "NOS", "lose"
		}) {
			assertEquals(Confidence.NONE, packagingOrigin.parse(unrelated, CONTEXT).confidence());
			assertEquals(Confidence.NONE, sealState.parse(unrelated, CONTEXT).confidence());
		}
		assertEquals(Confidence.NONE, packagingOrigin.parse("sealed", CONTEXT).confidence());
		assertEquals(Confidence.NONE, sealState.parse("OVP", CONTEXT).confidence());
	}

	@Test
	void defaultsBareMonetaryAmountsToEuros() {
		final MoneyParser parser = new MoneyParser();

		assertEquals(new Money(new BigDecimal("12.34"), Currency.getInstance("EUR")),
				parser.parse("12,34", CONTEXT).value().orElseThrow());
		assertEquals(new Money(new BigDecimal("120"), Currency.getInstance("EUR")),
				parser.parse("120 EUR", CONTEXT).value().orElseThrow());
		assertEquals(new Money(new BigDecimal("20"), Currency.getInstance("USD")),
				parser.parse("20 usd", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("20 EURO", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("-20 EUR", CONTEXT).confidence());
	}

	@Test
	void limitsAnonymousLocaleParsingToEstablishedCollectionMarkers() {
		final CollectionLanguageCodeParser language = new CollectionLanguageCodeParser();
		final CollectionRegionCodeParser region = new CollectionRegionCodeParser();

		assertEquals(new LanguageCode("en"), language.parse("EN", CONTEXT).value().orElseThrow());
		assertEquals(new RegionCode("US"), region.parse("US", CONTEXT).value().orElseThrow());
		assertEquals(new RegionCode("EUR"), region.parse("EU", CONTEXT).value().orElseThrow());
		assertEquals(new RegionCode("EUR"), region.parse("EUR", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, language.parse("SD", CONTEXT).confidence());
		assertEquals(Confidence.NONE, language.parse("EU", CONTEXT).confidence());
		assertEquals(Confidence.NONE, region.parse("AT", CONTEXT).confidence());

		for (final String code : new String[] {
				"DE", "ES", "FR", "IT"
		}) {
			assertEquals(Confidence.EXACT, language.parse(code, CONTEXT).confidence());
			assertEquals(Confidence.EXACT, region.parse(code, CONTEXT).confidence());
		}
	}

	@Test
	void adaptsTheLegacyInchGlyphWithoutChangingTheSharedLengthMeaning() {
		final CollectionLengthParser parser = new CollectionLengthParser();

		assertEquals(new Length(new BigDecimal("2.5"), Unit.INCH),
				parser.parse("2,5\uF020", CONTEXT).value().orElseThrow());
		assertEquals(new Length(BigDecimal.valueOf(50), Unit.CENTIMETER),
				parser.parse("50cm", CONTEXT).value().orElseThrow());
	}

	@Test
	void adaptsCollectionColorLanguageWithoutTreatingTransparencyAsAColor() {
		final CollectionColorParser parser = new CollectionColorParser();

		assertEquals(Color.BLACK, parser.parse("schwarz", CONTEXT).value().orElseThrow());
		assertEquals(Color.GREEN, parser.parse("grün", CONTEXT).value().orElseThrow());
		assertEquals(Color.PURPLE, parser.parse("lila", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("weiß-pink", CONTEXT).confidence());
		assertEquals(Confidence.NONE, parser.parse("weiß/pink", CONTEXT).confidence());
		assertEquals(Color.WHITE, parser.parse("white", CONTEXT).value().orElseThrow());
		assertEquals(Confidence.NONE, parser.parse("transparent", CONTEXT).confidence());
	}
}
