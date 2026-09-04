package com.retrocrawler.model.hardware;

import java.util.Objects;
import java.util.Optional;

/**
 * An Intel processor marking containing a Finished Process Order (FPO) batch
 * number and, where printed with it, a partial Assembly Test Process Order
 * (ATPO) serial number.
 */
public record IntelProcessorMarking(String finishedProcessOrder, Optional<String> partialAtpo)
		implements ProcessorMarking {

	public IntelProcessorMarking {
		finishedProcessOrder = Objects.requireNonNull(finishedProcessOrder, "finishedProcessOrder");
		if (!finishedProcessOrder.matches("[A-Z]\\d(?:0[1-9]|[1-4]\\d|5[0-3])[A-Z0-9]{4}")) {
			throw new IllegalArgumentException("Invalid Intel Finished Process Order: " + finishedProcessOrder);
		}

		partialAtpo = Objects.requireNonNull(partialAtpo, "partialAtpo");
		if (partialAtpo.isPresent() && !partialAtpo.orElseThrow().matches("\\d{4}")) {
			throw new IllegalArgumentException("Invalid partial Intel ATPO: " + partialAtpo.orElseThrow());
		}
	}

	@Override
	public String toString() {
		return finishedProcessOrder + partialAtpo.map(value -> "-" + value).orElse("");
	}
}
