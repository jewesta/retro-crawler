package com.retrocrawler.app;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
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
	public void beginArchive(final ArchiveDescriptor archive) {
		// no-op
	}

	@Override
	public void endArchive(final ArchiveDescriptor archive) {
		// no-op
	}

	@Override
	public VaadinGearNode addNode(final VaadinGearNode parent, final MyKnownGear gear) {
		throw new IllegalStateException("The Vaadin tree requires the Gear source ARI.");
	}

	@Override
	public VaadinGearNode addNode(final VaadinGearNode parent, final MyKnownGear gear, final ARI source) {
		final VaadinGearNode node = new VaadinGearNode(gear, source);
		data.addItem(parent, node);
		return node;
	}

	@Override
	public TreeData<VaadinGearNode> build() {
		return data;
	}
}
