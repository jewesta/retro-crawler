package com.retrocrawler.model.measurement;

public record TrackDensity(int tracksPerInch) {

	public TrackDensity {
		if (tracksPerInch <= 0) {
			throw new IllegalArgumentException("Track density must be positive: " + tracksPerInch);
		}
	}

	@Override
	public String toString() {
		return tracksPerInch + "TPI";
	}
}
