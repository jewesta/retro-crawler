package com.retrocrawler.mycollection.gear;

import java.util.Optional;

import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.model.storage.HardDiskDriveFormFactor;
import com.retrocrawler.model.storage.HardDiskDriveFormFactorParser;
import com.retrocrawler.mycollection.AttributeNames;
import com.retrocrawler.mycollection.matchers.HardDiskDriveMatcher;

@RetroGear(HardDiskDriveMatcher.class)
public final class HardDiskDrive extends MyGear {

	@RetroFact(key = AttributeNames.HARD_DISK_DRIVE_FORM_FACTOR, parser = HardDiskDriveFormFactorParser.class,
			strict = false, contextual = true)
	private HardDiskDriveFormFactor formFactor;

	public HardDiskDrive() {
	}

	public Optional<HardDiskDriveFormFactor> getFormFactor() {
		return Optional.ofNullable(formFactor);
	}
}
