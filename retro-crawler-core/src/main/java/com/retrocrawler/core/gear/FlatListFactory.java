package com.retrocrawler.core.gear;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.retrocrawler.core.archive.ArchiveDescriptor;

public final class FlatListFactory<G> implements GearTreeFactory<List<G>, G, G> {

	private final Class<G> gearType;

	private final List<G> out = new ArrayList<>();

	public FlatListFactory(final Class<G> gearType) {
		this.gearType = Objects.requireNonNull(gearType, "gearType");
	}

	@Override
	public Class<G> gearType() {
		return gearType;
	}

	@Override
	public void beginArchive(final ArchiveDescriptor archive) {
		// nothing
	}

	@Override
	public void endArchive(final ArchiveDescriptor archive) {
		// nothing
	}

	@Override
	public G addNode(final G parent, final G gear) {
		out.add(gear);
		return gear; // handle is the gear itself
	}

	@Override
	public List<G> build() {
		return List.copyOf(out);
	}
}
