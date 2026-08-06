package com.retrocrawler.core;

/** Decides whether a {@link Journal} stops at its first recorded exception. */
public enum FailureMode {

	FAIL_EARLY,

	FAIL_LATE

}
