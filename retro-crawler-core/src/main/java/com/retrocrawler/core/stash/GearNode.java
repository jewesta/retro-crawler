package com.retrocrawler.core.stash;

import java.util.List;
import java.util.Objects;

import com.retrocrawler.core.archive.ARI;

public final class GearNode<G> {

	private final G gear;

	private final ARI source;

	private final List<GearNode<G>> children;

	public GearNode(final G gear, final ARI source, final List<GearNode<G>> children) {
		this.gear = Objects.requireNonNull(gear, "gear");
		this.source = Objects.requireNonNull(source, "source");
		this.children = List.copyOf(Objects.requireNonNull(children, "children"));
	}

	public G gear() {
		return gear;
	}

	public ARI source() {
		return source;
	}

	public List<GearNode<G>> children() {
		return children;
	}
}
