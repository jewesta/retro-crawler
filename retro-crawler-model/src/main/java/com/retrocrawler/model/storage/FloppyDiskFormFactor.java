package com.retrocrawler.model.storage;

import java.math.BigDecimal;

/** Nominal physical width used to name a floppy-disk format. */
public enum FloppyDiskFormFactor {

	INCH_3("3"),
	INCH_3_5("3.5"),
	INCH_5_25("5.25"),
	INCH_8("8");

	private final BigDecimal nominalInches;

	FloppyDiskFormFactor(final String nominalInches) {
		this.nominalInches = new BigDecimal(nominalInches);
	}

	public BigDecimal nominalInches() {
		return nominalInches;
	}

	@Override
	public String toString() {
		return nominalInches.toPlainString() + '"';
	}
}
