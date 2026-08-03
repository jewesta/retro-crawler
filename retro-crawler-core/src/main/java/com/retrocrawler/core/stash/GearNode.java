package com.retrocrawler.core.stash;

import java.util.List;
import java.util.Objects;

public final class GearNode<G> {

	private final G gear;

	private final List<GearNode<G>> children;

	public GearNode(final G gear, final List<GearNode<G>> children) {
		this.gear = Objects.requireNonNull(gear, "gear");
		this.children = List.copyOf(Objects.requireNonNull(children, "children"));
	}

	public G gear() {
		return gear;
	}

	public List<GearNode<G>> children() {
		return children;
	}
}
