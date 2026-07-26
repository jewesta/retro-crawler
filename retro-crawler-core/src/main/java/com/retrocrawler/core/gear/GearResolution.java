package com.retrocrawler.core.gear;

import java.util.Objects;
import java.util.Optional;

/**
 * A resolved gear instance together with the identity needed for archive-wide
 * validation.
 */
public record GearResolution(Object gear, Optional<Object> retroId) {

	public GearResolution {
		Objects.requireNonNull(gear, "gear");
		Objects.requireNonNull(retroId, "retroId");
	}
}
