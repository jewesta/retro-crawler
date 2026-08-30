package com.retrocrawler.core.annotation;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.retrocrawler.core.archive.ARI;

/**
 * Requests injection of the source {@link ARI} that identifies the resolved
 * Gear occurrence.
 * <p>
 * The annotation is optional. When present, it must occur on exactly one
 * {@link ARI} field. The source is framework context rather than a clue or
 * fact.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(FIELD)
public @interface RetroSource {

}
