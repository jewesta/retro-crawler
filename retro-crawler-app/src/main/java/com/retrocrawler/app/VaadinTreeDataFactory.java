package com.retrocrawler.app;

import com.retrocrawler.core.stash.Batch;
import com.retrocrawler.core.stash.GearNode;
import com.retrocrawler.demo.gear.MyKnownGear;
import com.vaadin.flow.data.provider.hierarchy.TreeData;

final class VaadinTreeDataFactory {

	private VaadinTreeDataFactory() {
		// static utility class
	}

	static TreeData<VaadinGearNode> from(final Batch<MyKnownGear> batch) {
		final TreeData<VaadinGearNode> data = new TreeData<>();
		for (final GearNode<MyKnownGear> root : batch.roots()) {
			add(data, null, root);
		}
		return data;
	}

	private static void add(final TreeData<VaadinGearNode> data, final VaadinGearNode parent,
			final GearNode<MyKnownGear> source) {
		final VaadinGearNode node = new VaadinGearNode(source.gear(), source.source());
		data.addItem(parent, node);
		for (final GearNode<MyKnownGear> child : source.children()) {
			add(data, node, child);
		}
	}
}
