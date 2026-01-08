package com.retrocrawler.app.collection.gear;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.retrocrawler.app.collection.AttributeNames;
import com.retrocrawler.core.annotation.RetroAnyAttribute;
import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.util.RetroAttribute;

public abstract class MyRetroGear {

	@RetroFact(key = "@folder", optional = false)
	private String folderName;

	@RetroAnyAttribute
	private final Map<String, RetroAttribute> properties = new HashMap<>();

	public String getFolderName() {
		return folderName;
	}

	public Map<String, RetroAttribute> getAttributes() {
		return properties;
	}

	@Override
	public String toString() {
		return getAttributes().values().stream().map(Object::toString)
				.collect(Collectors.joining(", ", getClass().getSimpleName() + "[", "]"));
	}

}
