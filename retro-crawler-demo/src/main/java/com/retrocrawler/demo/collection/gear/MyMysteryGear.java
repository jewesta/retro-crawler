package com.retrocrawler.demo.collection.gear;

import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator.TypeMatcher;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;

/**
 * A {@link MyMysteryGear} is created from an {@link Artifact} in your retro
 * collection that could not be matched to any known {@link RetroGear} by the
 * {@link TypeMatcher}s. Only Artifacts with at least one {@link Clue} are
 * considered. So the Map returned by {@link #getAttributes()} will contain at
 * least one attribute.
 * 
 * The class is final because it is a reserved fallback type that can only be
 * used by RetroCrawler.
 */
@RetroGear(AnyGearMatcher.class)
public final class MyMysteryGear extends MyRetroGear {

}
