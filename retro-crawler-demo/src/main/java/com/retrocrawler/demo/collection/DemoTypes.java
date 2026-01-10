package com.retrocrawler.demo.collection;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.retrocrawler.demo.collection.gear.MyKnownGear;
import com.retrocrawler.demo.collection.gear.MyMysteryGear;
import com.retrocrawler.demo.collection.gear.MyRetroGear;
import com.retrocrawler.demo.collection.gear.RetroPCDemoArchive;

public enum DemoTypes {

	RETRO_PC(RetroPCDemoArchive.class, MyRetroGear.class, MyKnownGear.class, MyMysteryGear.class);

	private final Set<Class<?>> types;

	private DemoTypes(final Class<?>... types) {
		this.types = Arrays.stream(types).collect(Collectors.toSet());
	}

	public Set<Class<?>> getTypes() {
		return types;
	}

	public static List<Set<Class<?>>> allTypeSets() {
		return Arrays.stream(values()).map(DemoTypes::getTypes).toList();
	}

}
