package com.retrocrawler.core.gear;

import java.lang.reflect.Field;
import java.util.Objects;

import com.retrocrawler.core.annotation.RetroClue;

public class ClueDescriptor extends AttributeDescriptor {

	public ClueDescriptor(final RetroClue annotation, final Field field) {
		super(field, annotation.optional());
		Objects.requireNonNull(annotation, "annotation");
		this.key = effectiveKey(field, annotation.key());
	}

}
