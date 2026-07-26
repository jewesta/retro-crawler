package com.retrocrawler.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.retrocrawler.core.annotation.RetroId;

public final class DuplicateRetroIdException extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	private final Map<Object, List<String>> duplicates;

	DuplicateRetroIdException(final Map<Object, List<String>> duplicates) {
		super(message(duplicates));
		final Map<Object, List<String>> copy = new LinkedHashMap<>();
		duplicates.forEach((id, paths) -> copy.put(id, List.copyOf(paths)));
		this.duplicates = Collections.unmodifiableMap(copy);
	}

	public Map<Object, List<String>> getDuplicates() {
		return duplicates;
	}

	private static String message(final Map<Object, List<String>> duplicates) {
		Objects.requireNonNull(duplicates, "duplicates");
		final String details = duplicates.entrySet().stream()
				.map(entry -> entry.getKey() + " at [" + String.join(", ", entry.getValue()) + "]")
				.collect(Collectors.joining("; "));
		return "Duplicate " + RetroId.class.getSimpleName() + " value(s) in archive: " + details;
	}
}
