package com.retrocrawler.app.cli;

import java.util.Objects;

import com.retrocrawler.core.GearArchive;
import com.retrocrawler.core.GearBucket;
import com.retrocrawler.core.GearNode;

final class AsciiTreePrinter {

	private AsciiTreePrinter() {
		// static utility class
	}

	static <G> void printArchive(final GearArchive<G> archive) {
		Objects.requireNonNull(archive, "archive");
		for (final GearBucket<G> bucket : archive.buckets()) {
			System.out.println(String.valueOf(bucket.bucket()));
			final var roots = bucket.roots();
			for (int i = 0; i < roots.size(); i++) {
				final boolean last = i == roots.size() - 1;
				printNode(roots.get(i), "", last);
			}
			System.out.println();
		}
	}

	private static <G> void printNode(final GearNode<G> node, final String prefix, final boolean last) {
		final String connector = last ? "└── " : "├── ";
		System.out.println(prefix + connector + String.valueOf(node.gear()));
		final String childPrefix = prefix + (last ? "    " : "│   ");
		final var children = node.children();
		for (int i = 0; i < children.size(); i++) {
			final boolean childLast = i == children.size() - 1;
			printNode(children.get(i), childPrefix, childLast);
		}
	}
}