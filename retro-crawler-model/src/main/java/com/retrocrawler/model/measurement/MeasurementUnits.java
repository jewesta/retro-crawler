package com.retrocrawler.model.measurement;

import static javax.measure.BinaryPrefix.GIBI;
import static javax.measure.BinaryPrefix.KIBI;
import static javax.measure.BinaryPrefix.MEBI;
import static javax.measure.BinaryPrefix.TEBI;
import static javax.measure.MetricPrefix.CENTI;
import static javax.measure.MetricPrefix.MILLI;
import static javax.measure.MetricPrefix.NANO;
import static tech.units.indriya.unit.Units.SECOND;

import java.math.BigDecimal;

import javax.measure.Quantity;
import javax.measure.Unit;

import tech.units.indriya.unit.BaseUnit;

/** Units used by the shared archive measurement vocabulary. */
public final class MeasurementUnits {

	public static final Unit<javax.measure.quantity.Length> MILLIMETRE = MILLI(tech.units.indriya.unit.Units.METRE);
	public static final Unit<javax.measure.quantity.Length> CENTIMETRE = CENTI(tech.units.indriya.unit.Units.METRE);
	public static final Unit<javax.measure.quantity.Length> METRE = tech.units.indriya.unit.Units.METRE;
	public static final Unit<javax.measure.quantity.Length> INCH = METRE.multiply(new BigDecimal("0.0254"));

	public static final Unit<javax.measure.quantity.Power> WATT = tech.units.indriya.unit.Units.WATT;
	public static final Unit<javax.measure.quantity.Time> NANOSECOND = NANO(SECOND);

	public static final Unit<DataCapacity> BYTE = new BaseUnit<>("B");
	public static final Unit<DataCapacity> KIBIBYTE = KIBI(BYTE);
	public static final Unit<DataCapacity> MEBIBYTE = MEBI(BYTE);
	public static final Unit<DataCapacity> GIBIBYTE = GIBI(BYTE);
	public static final Unit<DataCapacity> TEBIBYTE = TEBI(BYTE);

	public static final Unit<TrackDensity> TRACK_PER_METRE = typed(METRE.inverse());
	public static final Unit<TrackDensity> TRACK_PER_INCH = typed(INCH.inverse());

	private MeasurementUnits() {
	}

	/*
	 * A Unit's quantity parameter is erased. These expressions already carry
	 * the correct physical dimensions; this cast assigns their custom semantic
	 * kind.
	 */
	@SuppressWarnings("unchecked")
	private static <Q extends Quantity<Q>> Unit<Q> typed(final Unit<?> unit) {
		return (Unit<Q>) unit;
	}
}
