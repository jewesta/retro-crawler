package com.retrocrawler.app;

import java.util.Objects;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.demo.gear.MyKnownGear;

record VaadinGearNode(MyKnownGear gear, ARI source) {

	VaadinGearNode {
		Objects.requireNonNull(gear, "gear");
		Objects.requireNonNull(source, "source");
	}
}
