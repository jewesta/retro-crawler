package com.retrocrawler.core.util;

public class Sonar {

	/*
	 * Reduce the total number of break and continue statements in this loop to use
	 * at most one.
	 */
	public static final String JAVA_REDUCE_NUMBER_OF_BREAK_AND_CONTINUE = "java:S135";

	/* Replace this "Map.containsKey()" with a call to "Map.computeIfAbsent()". */
	public static final String JAVA_REPLACE_WITH_COMPUTE_IF_ABSENT = "java:S3824";

	private Sonar() {
		// static utility class
	}

}
