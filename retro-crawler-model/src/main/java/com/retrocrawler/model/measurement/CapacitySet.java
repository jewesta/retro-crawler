package com.retrocrawler.model.measurement;

import java.util.Objects;

/** A set of equally sized members with a data capacity stated per member. */
public record CapacitySet(int memberCount, DataCapacity capacityPerMember) {

	public CapacitySet {
		if (memberCount < 2) {
			throw new IllegalArgumentException("A capacity set must contain at least two members: " + memberCount);
		}
		Objects.requireNonNull(capacityPerMember, "capacityPerMember");
	}

	public DataCapacity totalCapacity() {
		return capacityPerMember.multiply(memberCount);
	}

	@Override
	public String toString() {
		return memberCount + " x " + capacityPerMember;
	}
}
