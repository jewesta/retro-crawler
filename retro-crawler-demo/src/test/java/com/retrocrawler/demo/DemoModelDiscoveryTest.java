package com.retrocrawler.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.Model;

class DemoModelDiscoveryTest {

	@Test
	void discoversDemoModelFromBasePackage() {
		final Model model = Model.from(DemoModels.RETRO_PC.getBasePackage());

		assertEquals("retro_pc_demo", model.archiveDescriptor().id().value());
	}
}
