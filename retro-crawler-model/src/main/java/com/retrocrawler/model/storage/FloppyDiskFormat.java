package com.retrocrawler.model.storage;

import java.util.Objects;
import java.util.Optional;

/**
 * The recorded side count and density class printed on or specified for a
 * floppy disk. Either component may be absent when source material states only
 * one half of the conventional SS/DS and SD/DD/QD/HD/ED notation.
 */
public record FloppyDiskFormat(Optional<Sides> sides, Optional<Density> density) {

	public FloppyDiskFormat {
		sides = Objects.requireNonNull(sides, "sides");
		density = Objects.requireNonNull(density, "density");
		if (sides.isEmpty() && density.isEmpty()) {
			throw new IllegalArgumentException("A floppy-disk format needs a side count, a density, or both.");
		}
	}

	public static FloppyDiskFormat of(final Sides sides, final Density density) {
		return new FloppyDiskFormat(Optional.of(Objects.requireNonNull(sides, "sides")),
				Optional.of(Objects.requireNonNull(density, "density")));
	}

	public static FloppyDiskFormat sides(final Sides sides) {
		return new FloppyDiskFormat(Optional.of(Objects.requireNonNull(sides, "sides")), Optional.empty());
	}

	public static FloppyDiskFormat density(final Density density) {
		return new FloppyDiskFormat(Optional.empty(), Optional.of(Objects.requireNonNull(density, "density")));
	}

	public enum Sides {
		SINGLE,
		DOUBLE
	}

	public enum Density {
		SINGLE,
		DOUBLE,
		QUAD,
		HIGH,
		EXTENDED
	}
}
