package com.retrocrawler.mycollection.matchers;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.GearContext;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.model.hardware.MemoryFormFactor;
import com.retrocrawler.model.hardware.MemoryStandard;
import com.retrocrawler.model.measurement.DataCapacity;
import com.retrocrawler.mycollection.AttributeNames;

public final class MemoryModuleMatcher implements GearMatcher {

	@Override
	public Confidence matches(final GearContext context) {
		final boolean hasCapacity = context.getFact(AttributeNames.CAPACITY, DataCapacity.class).isPresent();
		final boolean hasModuleForm = context.getFacts(AttributeNames.MEMORY_FORM_FACTOR, MemoryFormFactor.class)
				.isPresent();
		final boolean hasMemoryStandard = context.getFacts(AttributeNames.MEMORY_STANDARD, MemoryStandard.class)
				.isPresent();
		return hasCapacity && (hasModuleForm || hasMemoryStandard) ? Confidence.STRONG : Confidence.NONE;
	}
}
