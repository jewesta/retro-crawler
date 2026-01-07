package com.retrocrawler.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import com.retrocrawler.core.util.RetroAttribute;

/**
 * Only works on {@link Map}s with {@link String} as key and
 * {@link RetroAttribute} as value;
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RetroAnyAttribute {

}
