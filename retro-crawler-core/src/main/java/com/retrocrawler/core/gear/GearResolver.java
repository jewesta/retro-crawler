package com.retrocrawler.core.gear;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.core.util.RetroAttribute;
import com.retrocrawler.core.util.Sonar;

public class GearResolver {

	private final Map<Class<?>, GearSpecialist> gearSpecialists;

	private final Map<String, FactFinder> factFinders;

	// package-private: only factories construct this
	GearResolver(final Map<Class<?>, GearSpecialist> specialists, final Map<String, FactFinder> factFinders) {
		this.gearSpecialists = Objects.requireNonNull(specialists, "specialists");
		this.factFinders = Objects.requireNonNull(factFinders, "factFinders");
	}

	private record BestAnonymousMatch(String key, Confidence confidence) {
	}

	private static boolean hasFact(final RetroAttributes resolved, final String key) {
		return resolved.get(key) instanceof Fact;
	}

	private void handleKnownKeyClue(final RetroAttributes resolved, final Clue clue) {
		final String key = clue.getKey();

		if (resolved.containsKey(key)) {
			return;
		}

		final FactFinder finder = factFinders.get(key);
		final RetroAttribute attribute;
		if (finder == null) {
			attribute = clue;
		} else {
			attribute = finder.find(clue).<RetroAttribute>map(Function.identity()).orElse(clue);
		}

		resolved.put(attribute);
	}

	@SuppressWarnings({ Sonar.JAVA_REDUCE_NUMBER_OF_BREAK_AND_CONTINUE })
	private void handleAnonymousClue(final RetroAttributes resolved, final Clue clue) {
		final Set<String> raws = clue.getValue();

		if (raws.size() == 1) {
			final String raw = raws.iterator().next();

			final BestAnonymousMatch best = findBestAnonymousMatch(raw);
			if (best == null || best.confidence() == Confidence.NONE) {
				putAnonymousClueIfUseful(resolved, clue);
				return;
			}

			final String resolvedKey = best.key();

			if (hasFact(resolved, resolvedKey)) {
				return;
			}

			if (resolved.containsKey(resolvedKey)) {
				putAnonymousClueIfUseful(resolved, clue);
				return;
			}

			final FactFinder finder = factFinders.get(resolvedKey);
			if (finder == null) {
				putAnonymousClueIfUseful(resolved, clue);
				return;
			}

			final Optional<Fact> fact = finder.find(clue);
			if (fact.isEmpty()) {
				putAnonymousClueIfUseful(resolved, clue);
				return;
			}

			resolved.put(fact.get());
			return;
		}

		boolean allResolved = true;
		String resolvedKey = null;

		for (final String raw : raws) {
			final BestAnonymousMatch best = findBestAnonymousMatch(raw);
			if (best == null || best.confidence() == Confidence.NONE) {
				allResolved = false;
				break;
			}

			final String candidateKey = best.key();

			if (hasFact(resolved, candidateKey)) {
				return;
			}

			if (resolved.containsKey(candidateKey)) {
				allResolved = false;
				break;
			}

			if (resolvedKey != null && !resolvedKey.equals(candidateKey)) {
				allResolved = false;
				break;
			}

			resolvedKey = candidateKey;
		}

		if (!allResolved || resolvedKey == null) {
			putAnonymousClueIfUseful(resolved, clue);
			return;
		}

		final FactFinder finder = factFinders.get(resolvedKey);
		if (finder == null) {
			putAnonymousClueIfUseful(resolved, clue);
			return;
		}

		final Optional<Fact> fact = finder.find(clue);
		if (fact.isEmpty()) {
			putAnonymousClueIfUseful(resolved, clue);
			return;
		}

		resolved.put(fact.get());
	}

	private void putAnonymousClueIfUseful(final RetroAttributes resolved, final Clue clue) {
		if (!resolved.containsClue(clue)) {
			resolved.put(clue);
		}
	}

	private BestAnonymousMatch findBestAnonymousMatch(final String raw) {
		BestAnonymousMatch best = null;
		for (final FactFinder finder : factFinders.values()) {
			if (finder.isStrict()) {
				continue;
			}
			best = considerAnonymousCandidate(best, finder, raw);
		}
		return best;
	}

	private BestAnonymousMatch considerAnonymousCandidate(final BestAnonymousMatch bestSoFar, final FactFinder finder,
			final String raw) {

		final RatedFact rated = finder.parse(raw);
		final Confidence confidence = rated.getConfidence();
		if (confidence == Confidence.NONE) {
			return bestSoFar;
		}

		final BestAnonymousMatch candidate = new BestAnonymousMatch(finder.getKey(), confidence);

		if (bestSoFar == null) {
			return candidate;
		}

		if (confidence.isHigherThan(bestSoFar.confidence())) {
			return candidate;
		}

		if (confidence == bestSoFar.confidence()) {
			throw new IllegalArgumentException("Ambiguous anonymous clue '" + raw + "': multiple parsers returned "
					+ confidence + " (keys '" + bestSoFar.key() + "' and '" + finder.getKey() + "').");
		}

		return bestSoFar;
	}

	@SuppressWarnings(Sonar.JAVA_REDUCE_NUMBER_OF_BREAK_AND_CONTINUE)
	public Optional<Object> resolve(final Artifact artifact) {
		Objects.requireNonNull(artifact, "artifact");

		final RetroAttributes attributes = new RetroAttributes();
		final Set<Clue> clues = artifact.getClues();

		for (final Clue clue : clues) {
			if (!clue.isAnonymous()) {
				handleKnownKeyClue(attributes, clue);
			}
		}

		for (final Clue clue : clues) {
			if (clue.isAnonymous()) {
				handleAnonymousClue(attributes, clue);
			}
		}

		GearSpecialist best = null;
		Confidence bestConfidence = Confidence.NONE;

		for (final GearSpecialist specialist : gearSpecialists.values()) {
			final Class<?> gearType = specialist.getGearDefinition().getType();

			final Confidence confidence = specialist.matches(new GearContext(gearType, artifact, attributes));

			Objects.requireNonNull(confidence, "The " + GearMatcher.class.getSimpleName() + " of type "
					+ specialist.getGearDefinition().getType() + " must not return null.");

			if (confidence == Confidence.NONE) {
				continue;
			}

			if (best == null || confidence.isHigherThan(bestConfidence)) {
				best = specialist;
				bestConfidence = confidence;
			}
		}

		if (best == null) {
			return Optional.empty();
		}

		final Class<?> bestType = best.getGearDefinition().getType();
		final GearContext context = new GearContext(bestType, artifact, attributes);
		return Optional.of(best.create(context));
	}

}