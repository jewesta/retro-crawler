package com.retrocrawler.model.hardware;

public record MemoryAccessTime(int nanoseconds) {

	public MemoryAccessTime {
		if (nanoseconds <= 0) {
			throw new IllegalArgumentException("Memory access time must be positive: " + nanoseconds);
		}
	}

	@Override
	public String toString() {
		return nanoseconds + "ns";
	}
}
