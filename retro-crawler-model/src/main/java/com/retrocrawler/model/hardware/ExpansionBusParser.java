package com.retrocrawler.model.hardware;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.EnumFactParser;
import com.retrocrawler.core.gear.parser.ParseContext;

public final class ExpansionBusParser implements EnumFactParser<ExpansionBus> {

	@Override
	public Class<ExpansionBus> enumType() {
		return ExpansionBus.class;
	}

	@Override
	public RatedFact<ExpansionBus> parse(final String rawValue, final ParseContext context) {
		if (rawValue == null) {
			return RatedFact.none("Expected a recognized expansion bus.");
		}

		final String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
		return switch (normalized) {
		case "AGP" -> RatedFact.exact(ExpansionBus.AGP);
		case "EISA" -> RatedFact.exact(ExpansionBus.EISA);
		case "ISA" -> RatedFact.exact(ExpansionBus.ISA);
		case "MCA", "MICRO CHANNEL", "MICRO CHANNEL ARCHITECTURE" -> RatedFact.exact(ExpansionBus.MCA);
		case "PCI" -> RatedFact.exact(ExpansionBus.PCI);
		case "PCIE", "PCI-E", "PCI EXPRESS", "PCI-EXPRESS" -> RatedFact.exact(ExpansionBus.PCI_EXPRESS);
		case "VLB", "VESA LOCAL BUS" -> RatedFact.exact(ExpansionBus.VLB);
		default -> RatedFact.none("Expected a recognized expansion bus.");
		};
	}
}
