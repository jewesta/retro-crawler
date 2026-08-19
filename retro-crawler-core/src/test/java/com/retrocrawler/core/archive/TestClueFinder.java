package com.retrocrawler.core.archive;

import java.util.Objects;
import java.util.function.Function;

import com.retrocrawler.core.archive.clues.ArchiveFolderView;
import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.clues.Clues;

/**
 * Named test adapter for the production contract that finder names are durable.
 */
class TestClueFinder implements ClueFinder {

	private final Function<ArchiveFolderView, Clues> finding;

	TestClueFinder(final Function<ArchiveFolderView, Clues> finding) {
		this.finding = Objects.requireNonNull(finding, "finding");
	}

	@Override
	public Clues find(final ArchiveFolderView folder) {
		return finding.apply(folder);
	}
}

final class FirstTestClueFinder extends TestClueFinder {

	FirstTestClueFinder(final Function<ArchiveFolderView, Clues> finding) {
		super(finding);
	}
}

final class SecondTestClueFinder extends TestClueFinder {

	SecondTestClueFinder(final Function<ArchiveFolderView, Clues> finding) {
		super(finding);
	}
}
