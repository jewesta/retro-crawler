package com.retrocrawler.model.identifier;

import static com.retrocrawler.model.identifier.PlayStationPortableMarket.AMERICAS;
import static com.retrocrawler.model.identifier.PlayStationPortableMarket.ASIA;
import static com.retrocrawler.model.identifier.PlayStationPortableMarket.EUROPE;
import static com.retrocrawler.model.identifier.PlayStationPortableMarket.JAPAN;
import static com.retrocrawler.model.identifier.PlayStationPortableMarket.KOREA;
import static com.retrocrawler.model.identifier.PlayStationPortablePublishingClass.LICENSED;
import static com.retrocrawler.model.identifier.PlayStationPortablePublishingClass.SONY_COMPUTER_ENTERTAINMENT;

import java.util.Arrays;
import java.util.Optional;

/**
 * The known four-letter prefixes for physical PlayStation Portable software.
 */
public enum PlayStationPortableDiscPrefix {

	UCJS(SONY_COMPUTER_ENTERTAINMENT, JAPAN),
	ULJS(LICENSED, JAPAN),
	UCUS(SONY_COMPUTER_ENTERTAINMENT, AMERICAS),
	ULUS(LICENSED, AMERICAS),
	UCES(SONY_COMPUTER_ENTERTAINMENT, EUROPE),
	ULES(LICENSED, EUROPE),
	UCKS(SONY_COMPUTER_ENTERTAINMENT, KOREA),
	ULKS(LICENSED, KOREA),
	UCAS(SONY_COMPUTER_ENTERTAINMENT, ASIA),
	ULAS(LICENSED, ASIA);

	private final PlayStationPortablePublishingClass publishingClass;
	private final PlayStationPortableMarket market;

	PlayStationPortableDiscPrefix(final PlayStationPortablePublishingClass publishingClass,
			final PlayStationPortableMarket market) {
		this.publishingClass = publishingClass;
		this.market = market;
	}

	public PlayStationPortablePublishingClass publishingClass() {
		return publishingClass;
	}

	public PlayStationPortableMarket market() {
		return market;
	}

	static Optional<PlayStationPortableDiscPrefix> fromCode(final String code) {
		return Arrays.stream(values()).filter(value -> value.name().equals(code)).findFirst();
	}
}
