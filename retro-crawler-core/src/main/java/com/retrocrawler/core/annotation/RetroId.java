package com.retrocrawler.core.annotation;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.retrocrawler.core.archive.clues.Archive;
import com.retrocrawler.core.gear.parser.FactParser;

/**
 * Works in tandem with {@link RetroFact} or {@link RetroClue} or stand alone.
 * When used in tandem guarantees that the id produced by the {@link FactParser}
 * is unique among all elements in the same {@link Archive}. In this case the
 * value can also not be optional. When used alone the type must be String and
 * an artificial Id is injected.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(FIELD)
public @interface RetroId {

}
