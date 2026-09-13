/**
 * Objective quantities and canonical parsers for measured archive facts.
 * <p>
 * Quantitative model values use the Units of Measurement API's
 * {@link javax.measure.Quantity} and {@link javax.measure.Unit} contracts, with
 * Indriya as their implementation. Prefer a standard quantity type from
 * {@code javax.measure.quantity} when one expresses the observed property.
 * Stable non-SI collection vocabulary may introduce a dedicated
 * {@code Quantity} subtype and unit instead.
 * <p>
 * Do not collapse semantically different counts into one dimensionless value.
 * For example, a parallel bus width, a serial lane count, and a package
 * terminal count require distinct quantity types even though all three use
 * positive integers.
 */
package com.retrocrawler.model.measurement;
