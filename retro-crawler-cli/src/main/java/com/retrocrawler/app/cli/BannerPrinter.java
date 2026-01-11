package com.retrocrawler.app.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class BannerPrinter {

	private static final String BANNER_RESOURCE = "/banner.txt";

	private BannerPrinter() {
		// no instances
	}

	public static void print(final String subtitle) {
		try (InputStream in = BannerPrinter.class.getResourceAsStream(BANNER_RESOURCE)) {
			if (in == null) {
				System.err.println("Warning: banner resource not found: " + BANNER_RESOURCE);
				return;
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
				reader.lines().forEach(System.out::println);
			}
		} catch (final IOException e) {
			System.err.println("Warning: failed to read banner resource: " + e.getMessage());
		}
		if (subtitle != null) {
			System.out.println(subtitle);
		}
		System.out.println();
	}
}