package com.retrocrawler.model.measurement;

import javax.measure.Unit;

/** A positive linear density of recorded tracks. */
public final class TrackDensity extends PositiveQuantity<TrackDensity> {

	private static final long serialVersionUID = 1L;

	public TrackDensity(final int tracksPerInch) {
		this(tracksPerInch, MeasurementUnits.TRACK_PER_INCH);
	}

	public TrackDensity(final Number amount, final Unit<TrackDensity> unit) {
		super(amount, unit);
	}

	public int tracksPerInch() {
		return amountIn(MeasurementUnits.TRACK_PER_INCH).intValueExact();
	}

	@Override
	public String toString() {
		return amount().toPlainString() + (MeasurementUnits.TRACK_PER_INCH.equals(unit()) ? "TPI" : " " + unit());
	}
}
