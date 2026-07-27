package com.retrocrawler.mycollection.memory;

import java.util.Objects;

import com.retrocrawler.model.measurement.DataCapacity;

public record RamSet(int memberCount, DataCapacity capacityPerMember) {

	public RamSet {
		if (memberCount < 2) {
			throw new IllegalArgumentException("A RAM set must contain at least two members: " + memberCount);
		}
		Objects.requireNonNull(capacityPerMember, "capacityPerMember");
	}

	public DataCapacity totalCapacity() {
		return capacityPerMember.multiply(memberCount);
	}
}
