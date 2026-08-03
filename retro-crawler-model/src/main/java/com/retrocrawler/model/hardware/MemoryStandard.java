package com.retrocrawler.model.hardware;

public enum MemoryStandard {

	PC_66("PC66"),
	PC_100("PC100"),
	PC_133("PC133"),
	PC_2100("PC2100"),
	PC_2700("PC2700"),
	PC_3200("PC3200");

	private final String displayName;

	MemoryStandard(final String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
}
