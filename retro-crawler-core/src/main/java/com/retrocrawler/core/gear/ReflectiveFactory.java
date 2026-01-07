package com.retrocrawler.core.gear;

import java.util.Set;

public interface ReflectiveFactory<T> {

	T reflectOn(Set<Class<?>> types);

	default T reflectOn(final TypeSource source) {
		return reflectOn(source.getTypes());
	}

}
