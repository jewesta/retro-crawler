package com.retrocrawler.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.retrocrawler.core.archive.clues.BlindFolderNameClueFinder;
import com.retrocrawler.core.archive.clues.FileContentClueFinder;
import com.retrocrawler.core.archive.clues.FileNameClueFinder;
import com.retrocrawler.core.archive.clues.FolderNameClueFinder;
import com.retrocrawler.core.archive.clues.TreeClueFinder;

/**
 * Declares which observations produce clues while crawling a collection.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RetroClues {

	Class<? extends FolderNameClueFinder> fromFolderName() default BlindFolderNameClueFinder.class;

	Class<? extends FileNameClueFinder>[] fromFileNames() default {};

	Class<? extends FileContentClueFinder>[] fromFileContents() default {};

	/**
	 * Post-order finders that may inspect non-artifact folder subtrees through a
	 * transient archive view.
	 */
	Class<? extends TreeClueFinder>[] fromFolderTrees() default {};
}
