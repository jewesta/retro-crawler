package com.retrocrawler.demo;

public enum DemoModels {

	RETRO_PC("com.retrocrawler.demo.gear");

	private final String basePackage;

	private DemoModels(final String basePackage) {
		this.basePackage = basePackage;
	}

	public String getBasePackage() {
		return basePackage;
	}

}
