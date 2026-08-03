package com.retrocrawler.app.cli;

import java.util.Objects;

import com.retrocrawler.core.stash.GearBucket;
import com.retrocrawler.core.stash.GearNode;
import com.retrocrawler.core.stash.Stash;

final class AsciiTreePrinter {

	private AsciiTreePrinter() {
		// static utility class
	}

	static <G> void printStash(final Stash<G> stash) {
		Objects.requireNonNull(stash, "stash");
		for (final GearBucket<G> bucket : stash.buckets()) {
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
