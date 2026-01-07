package com.retrocrawler.core.gear;

import java.lang.reflect.Field;
import java.util.Objects;

import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.gear.parser.FactParser;

public class FactDescriptor extends AttributeDescriptor {

	private final boolean strict;

	private final Class<? extends FactParser> parser;

	public FactDescriptor(final RetroFact annotation, final Field field) {
		super(field, annotation.optional());
		Objects.requireNonNull(annotation, "annotation");
		this.key = effectiveKey(field, annotation.key());
		this.strict = annotation.strict();
		@SuppressWarnings("unchecked")
		final Class<? extends FactParser> p = (Class<? extends FactParser>) (Class<?>) annotation.parser();
		this.parser = Objects.requireNonNull(p, "parser");
	}

	public boolean isStrict() {
		return strict;
	}

	public Class<? extends FactParser> getParser() {
		return parser;
	}

}
