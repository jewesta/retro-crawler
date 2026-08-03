package com.retrocrawler.model.hardware;

import java.util.Objects;

/**
 * An identifying expression used for an integrated circuit.
 * <p>
 * A designation may be a complete manufacturer part number, a family or core
 * name, a package marking, or incomplete identifying text. It deliberately
 * does not claim which of those forms the observed text represents.
 */
public record ChipDesignation(String designation) {

	public ChipDesignation {
		Objects.requireNonNull(designation, "designation");
		designation = designation.trim();
		if (designation.isEmpty()) {
			throw new IllegalArgumentException("Chip designation must not be blank.");
		}
	}

	@Override
	public String toString() {
		return designation;
	}
}
