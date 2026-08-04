package com.retrocrawler.app;

import java.nio.file.Path;
import java.util.Objects;

import com.retrocrawler.demo.gear.MyKnownGear;

record VaadinGearNode(MyKnownGear gear, Path sourcePath) {

	VaadinGearNode {
		Objects.requireNonNull(gear, "gear");
		Objects.requireNonNull(sourcePath, "sourcePath");
	}
}
