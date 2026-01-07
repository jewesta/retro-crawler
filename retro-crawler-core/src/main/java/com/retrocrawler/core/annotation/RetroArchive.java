package com.retrocrawler.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.retrocrawler.core.archive.clues.BlindPathNameClueFinder;
import com.retrocrawler.core.archive.clues.FileContentClueFinder;
import com.retrocrawler.core.archive.clues.FileNameClueFinder;
import com.retrocrawler.core.archive.clues.PathNameClueFinder;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RetroArchive {

	String id();

	/**
	 * An optional display name for the archive. If none given id is used.
	 */
	String name() default "";

	/**
	 * An archive can consist of various folders. All folders are scanned for clues.
	 */
	String[] locations();

	/**
	 * Clue-finder configuration.
	 */
	LookAt findClues() default @LookAt;

	@Retention(RetentionPolicy.RUNTIME)
	@Target({})
	@interface LookAt {

		Class<? extends PathNameClueFinder> pathName() default BlindPathNameClueFinder.class;

		Class<? extends FileNameClueFinder>[] fileNames() default {};

		Class<? extends FileContentClueFinder>[] fileContents() default {};
	}

}