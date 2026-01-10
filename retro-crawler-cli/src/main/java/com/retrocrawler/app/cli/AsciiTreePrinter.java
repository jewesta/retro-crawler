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
		for (final GearBucket<G> bucket : archive.getBuckets()) {
			System.out.println(String.valueOf(bucket.getBucket()));
			final var roots = bucket.getRoots();
			for (int i = 0; i < roots.size(); i++) {
				final boolean last = i == roots.size() - 1;
				printNode(roots.get(i), "", last);
			}
			System.out.println();
		}
	}

	private static <G> void printNode(final GearNode<G> node, final String prefix, final boolean last) {
		final String connector = last ? "└── " : "├── ";
		System.out.println(prefix + connector + String.valueOf(node.getGear()));
		final String childPrefix = prefix + (last ? "    " : "│   ");
		final var children = node.getChildren();
		for (int i = 0; i < children.size(); i++) {
			final boolean childLast = i == children.size() - 1;
			printNode(children.get(i), childPrefix, childLast);
		}
	}
}