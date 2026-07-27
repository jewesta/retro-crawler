package com.retrocrawler.mycollection.model;

import java.util.Objects;

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
