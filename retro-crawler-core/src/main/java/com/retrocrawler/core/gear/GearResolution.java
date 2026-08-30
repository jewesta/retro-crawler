package com.retrocrawler.core.gear;

import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.gear.trace.ResolutionTrace;

/**
 * A resolved gear instance together with the identity needed for archive-wide
 * validation and its model-dependent resolution trace.
 */
public record GearResolution(GearType type, Object gear, Optional<Object> retroId, ResolutionTrace trace) {

	public GearResolution(final Object gear, final Optional<Object> retroId, final ResolutionTrace trace) {
		this(GearType.from(Objects.requireNonNull(gear, "gear").getClass()), gear, retroId, trace);
	}

	public GearResolution {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(gear, "gear");
		Objects.requireNonNull(retroId, "retroId");
		Objects.requireNonNull(trace, "trace");
	}
}
