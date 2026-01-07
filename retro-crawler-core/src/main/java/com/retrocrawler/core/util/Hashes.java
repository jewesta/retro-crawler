package com.retrocrawler.core.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hashes {

	private Hashes() {
		// static utility class
	}

	public static byte[] sha256(final String s) {
		try {
			final MessageDigest md = MessageDigest.getInstance("SHA-256");
			return md.digest(s.getBytes(StandardCharsets.UTF_8));
		} catch (final NoSuchAlgorithmException e) {
			throw new IllegalStateException("Missing SHA-256 MessageDigest.", e);
		}
	}

	public static String toHex(final byte[] bytes, final int maxBytes) {
		final int len = Math.min(bytes.length, maxBytes);
		final StringBuilder sb = new StringBuilder(len * 2);
		for (int i = 0; i < len; i++) {
			sb.append(Character.forDigit((bytes[i] >> 4) & 0xF, 16));
			sb.append(Character.forDigit(bytes[i] & 0xF, 16));
		}
		return sb.toString();
	}

}
