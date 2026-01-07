package com.retrocrawler.core.gear;

import java.util.Set;

@FunctionalInterface
public interface TypeSource {

	Set<Class<?>> getTypes();

}