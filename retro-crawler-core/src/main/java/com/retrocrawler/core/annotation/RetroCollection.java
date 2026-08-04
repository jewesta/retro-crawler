package com.retrocrawler.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.retrocrawler.core.archive.filter.ArchivePathFilter;

/**
 * Declares the identity and default archive configuration of one collector's
 * collection. Exactly one type in a RetroCrawler model carries this annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RetroCollection {

	String id();

	/**
	 * An optional display name for the collection. If none is given,
	 * {@link #id()} is used.
	 */
	String name() default "";

	/**
	 * A collection can consist of several hierarchical roots. Locations may be
	 * omitted when they are supplied explicitly while constructing the model.
	 */
	String[] locations() default {};

	/**
	 * Optional BCP 47 language tag used as the collection's interpretation
	 * locale. The host's format locale is used when omitted.
	 */
	String locale() default "";

	/**
	 * Optional {@link java.time.ZoneId} identifier used as the collection's
	 * time zone. The host's system time zone is used when omitted.
	 */
	String timeZone() default "";

	/**
	 * Filters deciding which source entries belong to the archive. An entry
	 * must be accepted by every configured filter.
	 */
	Class<? extends ArchivePathFilter>[] pathFilters() default {};

	/**
	 * Optional collection-wide directory for crawler-owned files such as
	 * external catalogs. Runtime model configuration can override this value.
	 */
	String workingDirectory() default "";
}
