package com.retrocrawler.app;

import com.retrocrawler.core.archive.clues.Bucket;
import com.retrocrawler.core.gear.GearTreeFactory;
import com.retrocrawler.demo.gear.MyKnownGear;
import com.vaadin.flow.data.provider.hierarchy.TreeData;

final class VaadinTreeDataFactory implements GearTreeFactory<TreeData<VaadinGearNode>, VaadinGearNode, MyKnownGear> {

	private final TreeData<VaadinGearNode> data = new TreeData<>();

	@Override
	public Class<MyKnownGear> gearType() {
		return MyKnownGear.class;
	}

	@Override
	public void beginBucket(final Bucket bucket) {
		// no-op
	}

	@Override
	public void endBucket(final Bucket bucket) {
		// no-op
	}

	@Override
	public VaadinGearNode addNode(final VaadinGearNode parent, final MyKnownGear gear) {
		throw new IllegalStateException("The Vaadin tree requires the Gear source path.");
	}

	@Override
	public VaadinGearNode addNode(final VaadinGearNode parent, final MyKnownGear gear,
			final java.nio.file.Path sourcePath) {
		final VaadinGearNode node = new VaadinGearNode(gear, sourcePath);
		data.addItem(parent, node);
		return node;
	}

	@Override
	public TreeData<VaadinGearNode> build() {
		return data;
	}
}
