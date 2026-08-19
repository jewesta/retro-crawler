package com.retrocrawler.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.retrocrawler.core.archive.clues.ClueFinder;

/**
 * Declares which observations produce clues while crawling a collection.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RetroClues {

	/**
	 * The finders that inspect each candidate artifact through its transient,
	 * pruned archive view.
	 */
	Class<? extends ClueFinder>[] value();
}
