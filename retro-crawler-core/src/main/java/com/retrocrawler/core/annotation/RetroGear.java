package com.retrocrawler.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.gear.matcher.AnyGearMatcher;
import com.retrocrawler.core.gear.matcher.GearMatcher;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RetroGear {

	/**
	 * You can use {@link AnyGearMatcher} as a "catch all". Every {@link Artifact}
	 * that cannot be converted to any other type will end up being converted to the
	 * type using {@link AnyGearMatcher}. It can only be added to one type.
	 */
	Class<? extends GearMatcher> value();

}
