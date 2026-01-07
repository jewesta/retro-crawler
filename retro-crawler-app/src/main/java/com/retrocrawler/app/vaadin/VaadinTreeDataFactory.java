package com.retrocrawler.app.vaadin;

import com.retrocrawler.app.collection.gear.MyKnownGear;
import com.retrocrawler.core.GearTreeFactory;
import com.retrocrawler.core.archive.clues.Bucket;
import com.vaadin.flow.data.provider.hierarchy.TreeData;

public final class VaadinTreeDataFactory implements GearTreeFactory<TreeData<MyKnownGear>, MyKnownGear, MyKnownGear> {

	private final TreeData<MyKnownGear> data = new TreeData<>();

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
	public MyKnownGear addNode(final MyKnownGear parent, final MyKnownGear gear) {
		data.addItem(parent, gear);
		return gear;
	}

	@Override
	public TreeData<MyKnownGear> build() {
		return data;
	}
}