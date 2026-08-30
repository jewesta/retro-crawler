package com.retrocrawler.core.gear;

import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.gear.trace.ResolutionTrace;

/**
 * A resolved gear instance together with the identity needed for archive-wide
 * validation and its model-dependent resolution trace.
 */
public record GearResolution(Object gear, Optional<Object> retroId, ResolutionTrace trace) {

	public GearResolution {
		Objects.requireNonNull(gear, "gear");
		Objects.requireNonNull(retroId, "retroId");
		Objects.requireNonNull(trace, "trace");
	}
}
