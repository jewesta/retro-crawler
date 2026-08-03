package com.retrocrawler.mycollection.gear;

import java.util.Optional;
import java.util.Set;

import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.model.measurement.TrackDensity;
import com.retrocrawler.model.measurement.TrackDensityParser;
import com.retrocrawler.model.storage.FloppyDiskFormat;
import com.retrocrawler.model.storage.FloppyDiskFormatParser;
import com.retrocrawler.model.storage.FloppyDiskFormFactor;
import com.retrocrawler.model.storage.FloppyDiskFormFactorParser;
import com.retrocrawler.mycollection.AttributeNames;
import com.retrocrawler.mycollection.matchers.DisketteMatcher;

@RetroGear(DisketteMatcher.class)
public final class Diskette extends MyGear {

	@RetroFact(key = AttributeNames.FLOPPY_DISK_FORM_FACTOR, parser = FloppyDiskFormFactorParser.class,
			strict = false, contextual = true)
	private FloppyDiskFormFactor formFactor;

	@RetroFact(key = AttributeNames.TRACK_DENSITY, parser = TrackDensityParser.class, strict = false,
			optional = true)
	private Set<TrackDensity> trackDensities = Set.of();

	@RetroFact(key = AttributeNames.FLOPPY_DISK_FORMAT, parser = FloppyDiskFormatParser.class, strict = false,
			optional = true)
	private Set<FloppyDiskFormat> floppyDiskFormats = Set.of();

	public Diskette() {
	}

	public Set<TrackDensity> getTrackDensities() {
		return Set.copyOf(trackDensities);
	}

	public Set<FloppyDiskFormat> getFloppyDiskFormats() {
		return Set.copyOf(floppyDiskFormats);
	}

	public Optional<FloppyDiskFormFactor> getFormFactor() {
		return Optional.ofNullable(formFactor);
	}
}
