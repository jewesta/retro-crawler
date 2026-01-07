package com.retrocrawler.core.util;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Implement this an Jackson will automatically include type information should
 * the implementing type get serialized. This is useful in all situations where
 * the type cannot be inferred by Jackson due to type erasure. For example if a
 * field type is an interface or Object.
 * 
 * Retro Crawler reserves the key "@type" even though it is currently not used
 * for serialization. It might be in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = JacksonSerializable.TYPE)
public interface JacksonSerializable {

	String PREFIX_INTERNAL = "@";

	String TYPE = PREFIX_INTERNAL + "type";

}
