package com.retrocrawler.core.gear;

import java.util.Objects;
import java.util.Set;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.util.RetroAttribute;

public final class Fact implements RetroAttribute {

	private final String key;
	private final Set<Object> value;
	private final Confidence confidence;
	private final Clue source;

	public Fact(final String key, final Set<Object> value, final Confidence confidence, final Clue source) {
		this.key = Objects.requireNonNull(key, "key");
		this.value = Objects.requireNonNull(value, "value");
		this.confidence = Objects.requireNonNull(confidence, "confidence");
		this.source = Objects.requireNonNull(source, "source");
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public Set<Object> getValue() {
		return value;
	}

	public Confidence getConfidence() {
		return confidence;
	}

	public Clue source() {
		return source;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[key=" + key + ", value=" + value + "]";
	}

}