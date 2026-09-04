package com.retrocrawler.mycollection.facts;

import com.retrocrawler.core.gear.parser.EnumParser;
import com.retrocrawler.model.hardware.PrinterType;

public final class PrinterTypeParser extends EnumParser<PrinterType> {

	public PrinterTypeParser() {
		super(PrinterType.class, PrinterTypeParser::matches);
	}

	private static boolean matches(final PrinterType printerType, final String rawValue) {
		return switch (printerType) {
		case DOT_MATRIX_9_PIN -> "9-Nadel".equals(rawValue);
		case DAISY_WHEEL, TYPEBALL, LINE_MATRIX, CHAIN, BAND, DRUM, INKJET, LED, LASER, DIRECT_THERMAL, THERMAL_TRANSFER, DYE_SUBLIMATION, SOLID_INK -> false;
		};
	}
}
