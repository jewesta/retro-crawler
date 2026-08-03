package com.retrocrawler.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.retrocrawler.core.archive.CrawlEverything;
import com.retrocrawler.core.archive.CrawlPolicy;

/**
 * Declares the identity and default filesystem configuration of one collector's
 * collection. Exactly one type in a RetroCrawler model carries this annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RetroCollection {

	String id();

	/**
	 * An optional display name for the collection. If none is given, {@link #id()}
	 * is used.
	 */
	String name() default "";

	/**
	 * A collection can consist of several folders. Locations may be omitted when
	 * they are supplied explicitly while constructing the model.
	 */
	String[] locations() default {};

	/**
	 * Policy deciding which filesystem entries belong to the archive crawl.
	 */
	Class<? extends CrawlPolicy> crawlPolicy() default CrawlEverything.class;

	/**
	 * Optional collection-wide directory for crawler-owned files such as external
	 * catalogs. Runtime model configuration can override this value.
	 */
	String workingDirectory() default "";
}
