package com.retrocrawler.mycollection.gear;

import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.model.hardware.PrinterType;
import com.retrocrawler.mycollection.AttributeNames;
import com.retrocrawler.mycollection.facts.PrinterTypeParser;
import com.retrocrawler.mycollection.matchers.PrinterMatcher;

@RetroGear(PrinterMatcher.class)
public final class Printer extends MyGear {

	@RetroFact(key = AttributeNames.PRINTER_TYPE, parser = PrinterTypeParser.class, strict = false, optional = false)
	private PrinterType printerType;

	public Printer() {
	}

	public PrinterType printerType() {
		return printerType;
	}
}
