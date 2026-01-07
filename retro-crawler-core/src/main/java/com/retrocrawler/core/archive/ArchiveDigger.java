package com.retrocrawler.core.archive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.ArchivePathClueFinder;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.util.Hashes;
import com.retrocrawler.core.util.Monitor;
import com.retrocrawler.core.util.PathNames;

public class ArchiveDigger {

	private static final Logger logger = LoggerFactory.getLogger(ArchiveDigger.class);

	private final ArchiveDescriptor descriptor;

	private final ArchivePathClueFinder clueFinder;

	public ArchiveDigger(final ArchiveDescriptor descriptor, final ArchivePathClueFinder clueFinder) {
		this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
		this.clueFinder = Objects.requireNonNull(clueFinder, "clueFinder");
	}

	public ArchiveNode dig(final Path path, final Monitor monitor) throws IOException {
		if (!Files.isDirectory(path)) {
			throw new IllegalArgumentException("Expected a folder but got: " + path.toString());
		}
		return dig(path, path, monitor);
	}

	private Clue createInternalArtifactId(final Path root, final Path path) {
		final String archiveId = descriptor.getId().get();
		final String relative = root.relativize(path).toString().replace('\\', '/');
		final String basis = archiveId + "::" + relative;
		final byte[] hash = Hashes.sha256(basis);
		// 16 bytes -> 32 hex chars. Usually plenty, much smaller than full paths.
		final String id = Hashes.toHex(hash, 16);
		return Clue.internal(Clue.KEY_INTERNAL_ID, id);
	}

	/**
	 * @param root    Guaranteed to be a folder (not a file)
	 * @param path    Guaranteed to be a folder (not a file)
	 * @param monitor
	 * @return
	 * @throws IOException
	 */
	private ArchiveNode dig(final Path root, final Path path, final Monitor monitor) throws IOException {
		final String pathName;
		if (path.equals(root)) {
			/**
			 * In order to get unique root node ids among various buckets we need to use the
			 * folder's name including all parents (the full path) for the root node. All
			 * other nodes below it just use the folder name itself.
			 */
			pathName = path.toString();
		} else {
			pathName = path.getFileName().toString();
		}
		if (monitor.isCancelled()) {
			return new ArchiveNode(pathName, null, null);
		}

		final String displayPath = PathNames.abbreviatePathName(path.toString());
		monitor.postUpdate(displayPath);

		final List<Path> paths;
		/*
		 * Need a try-with to close the stream or else the JVM will sooner or later
		 * crash with a java.io.IOException: Too many open files.
		 */
		try (final Stream<Path> files = Files.list(path)) {
			paths = files.toList();
		}

		final ArchivePath node = new ArchivePath(path, paths);
		final Set<Clue> clues = clueFinder.find(node, monitor);

		final Artifact artifact;
		if (clues.isEmpty()) {
			artifact = null;
		} else {
			final Set<Clue> effectiveClues = new HashSet<>(clues);
			final Clue id = createInternalArtifactId(root, path);
			effectiveClues.add(id);
			artifact = new Artifact(effectiveClues);
			logger.info("Found artifact at: " + root.relativize(path).toString());
		}

		final List<ArchiveNode> children = new ArrayList<>();
		for (final Path child : paths) {
			if (Files.isDirectory(child)) {
				children.add(dig(root, child, monitor));
			}
		}

		final List<ArchiveNode> effectiveChildren = children.isEmpty() ? null : children;
		return new ArchiveNode(pathName, artifact, effectiveChildren);
	}
}