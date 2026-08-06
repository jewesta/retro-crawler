package com.retrocrawler.core.progress;

/** Advances a progressor automatically over a configured duration. */
public interface AutoProgressor extends Runnable {

	void done();

	AutoProgressor updateInterval(long millis);
}
