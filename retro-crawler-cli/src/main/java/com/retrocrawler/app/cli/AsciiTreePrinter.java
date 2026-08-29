package com.retrocrawler.app.cli;

import java.util.Objects;

import com.retrocrawler.core.stash.ArchiveGear;
import com.retrocrawler.core.stash.Batch;
import com.retrocrawler.core.stash.GearNode;

final class AsciiTreePrinter {

	private AsciiTreePrinter() {
		// static utility class
	}

	static <G> void print(final Batch<G> batch) {
		Objects.requireNonNull(batch, "batch");
		for (final ArchiveGear<G> archive : batch.archives()) {
			System.out.println(archive.archive().name() + " [" + archive.archive().root() + "]");
			final var roots = archive.roots();
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
