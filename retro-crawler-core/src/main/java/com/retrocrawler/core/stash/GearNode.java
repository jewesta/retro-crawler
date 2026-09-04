package com.retrocrawler.core.stash;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.gear.GearType;
import com.retrocrawler.core.gear.trace.ResolutionTrace;

public final class GearNode<G> {

	private final G gear;

	private final GearType type;

	private final ARI source;

	private final Optional<ResolutionTrace> trace;

	private final List<GearNode<G>> children;

	public GearNode(final G gear, final ARI source, final List<GearNode<G>> children) {
		this(GearType.from(Objects.requireNonNull(gear, "gear").getClass()), gear, source, Optional.empty(), children);
	}

	public GearNode(final G gear, final ARI source, final ResolutionTrace trace, final List<GearNode<G>> children) {
		this(GearType.from(Objects.requireNonNull(gear, "gear").getClass()), gear, source,
				Optional.of(Objects.requireNonNull(trace, "trace")), children);
	}

	public GearNode(final GearType type, final G gear, final ARI source, final List<GearNode<G>> children) {
		this(type, gear, source, Optional.empty(), children);
	}

	public GearNode(final GearType type, final G gear, final ARI source, final ResolutionTrace trace,
			final List<GearNode<G>> children) {
		this(type, gear, source, Optional.of(Objects.requireNonNull(trace, "trace")), children);
	}

	GearNode(final GearType type, final G gear, final ARI source, final Optional<ResolutionTrace> trace,
			final List<GearNode<G>> children) {
		this.type = Objects.requireNonNull(type, "type");
		this.gear = Objects.requireNonNull(gear, "gear");
		this.source = Objects.requireNonNull(source, "source");
		this.trace = Objects.requireNonNull(trace, "trace");
		this.children = List.copyOf(Objects.requireNonNull(children, "children"));
	}

	public G gear() {
		return gear;
	}

	public GearType type() {
		return type;
	}

	public ARI source() {
		return source;
	}

	/**
	 * The resolution explanation, present on nodes produced by RetroCrawler.
	 */
	public Optional<ResolutionTrace> trace() {
		return trace;
	}

	public List<GearNode<G>> children() {
		return children;
	}
}
