package com.retrocrawler.model.hardware;

/**
 * The physical printing mechanism used by a printer.
 */
public enum PrinterType {

	/**
	 * An impact dot-matrix mechanism whose print head contains nine pins.
	 */
	DOT_MATRIX_9_PIN,

	/**
	 * An impact mechanism that strikes preformed characters carried on the
	 * spokes of a daisy wheel.
	 */
	DAISY_WHEEL,

	/**
	 * An impact mechanism that selects preformed characters on a rotating,
	 * ball-shaped type element.
	 */
	TYPEBALL,

	/**
	 * An impact line-printer mechanism that forms characters from dots using a
	 * moving print shuttle and a bank of hammers.
	 */
	LINE_MATRIX,

	/**
	 * An impact line-printer mechanism that carries preformed characters on a
	 * continuously moving chain.
	 */
	CHAIN,

	/**
	 * An impact line-printer mechanism that carries preformed characters on a
	 * continuously moving metal band.
	 */
	BAND,

	/**
	 * An impact line-printer mechanism whose preformed characters are arranged
	 * on a rotating drum.
	 */
	DRUM,

	/**
	 * A non-impact mechanism that deposits microscopic droplets of liquid ink.
	 */
	INKJET,

	/**
	 * An electrophotographic mechanism that exposes its image with a stationary
	 * array of light-emitting diodes.
	 */
	LED,

	/**
	 * An electrophotographic mechanism that exposes its image with a scanned
	 * laser beam.
	 */
	LASER,

	/**
	 * A thermal mechanism that forms an image directly on heat-sensitive media.
	 */
	DIRECT_THERMAL,

	/**
	 * A thermal mechanism that transfers pigment from a ribbon onto the print
	 * medium.
	 */
	THERMAL_TRANSFER,

	/**
	 * A thermal mechanism that diffuses vaporized dye into the print medium.
	 */
	DYE_SUBLIMATION,

	/**
	 * A mechanism that melts solid ink and transfers it onto the print medium.
	 */
	SOLID_INK

}
