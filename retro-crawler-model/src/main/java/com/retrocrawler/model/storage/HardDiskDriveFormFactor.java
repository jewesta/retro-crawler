package com.retrocrawler.model.storage;

import java.math.BigDecimal;

/** Nominal platter diameter used to classify hard-disk drives. */
public enum HardDiskDriveFormFactor {

	INCH_1_8("1.8"),
	INCH_2_5("2.5"),
	INCH_3_5("3.5"),
	INCH_5_25("5.25");

	private final BigDecimal nominalInches;

	HardDiskDriveFormFactor(final String nominalInches) {
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
