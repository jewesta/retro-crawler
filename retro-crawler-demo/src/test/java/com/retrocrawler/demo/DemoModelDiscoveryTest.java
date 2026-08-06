package com.retrocrawler.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.Model;
import com.retrocrawler.core.archive.filter.IgnoreDotPaths;
import com.retrocrawler.core.archive.filter.IgnoreLinuxSystemPaths;
import com.retrocrawler.core.archive.filter.IgnoreMacSystemPaths;
import com.retrocrawler.core.archive.filter.IgnoreWindowsSystemPaths;

class DemoModelDiscoveryTest {

	@Test
	void discoversDemoModelFromBasePackage() {
		final Model model = Model.from(DemoModels.RETRO_PC.getBasePackage());

		assertEquals("retro_pc_demo", model.collectionId());
		assertEquals(List.of(IgnoreDotPaths.class, IgnoreWindowsSystemPaths.class, IgnoreMacSystemPaths.class,
				IgnoreLinuxSystemPaths.class), model.pathFilters().stream().map(Object::getClass).toList());
	}
}
