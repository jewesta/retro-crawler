package com.retrocrawler.mycollection.matchers;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.GearContext;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.model.measurement.TrackDensity;
import com.retrocrawler.model.storage.FloppyDiskFormat;
import com.retrocrawler.mycollection.AttributeNames;

public final class DisketteMatcher implements GearMatcher {

	@Override
	public Confidence matches(final GearContext context) {
		final boolean hasTrackDensity = context.facts(AttributeNames.TRACK_DENSITY, TrackDensity.class)
				.isPresent();
		final boolean hasFloppyDiskFormat = context
				.facts(AttributeNames.FLOPPY_DISK_FORMAT, FloppyDiskFormat.class).isPresent();
		return hasTrackDensity && hasFloppyDiskFormat ? Confidence.STRONG : Confidence.NONE;
	}
}
