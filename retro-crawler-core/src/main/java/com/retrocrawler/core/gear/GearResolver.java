package com.retrocrawler.core.gear;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.injector.GearSpecialist;
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
			if (resolved.isFact(resolvedKey)) {
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
			if (resolved.isFact(candidateKey)) {
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

		/*
		 * We are now looking at the given artifact and we want to turn it into a new
		 * retro gear (if possible). To do this we first try to turn as many clues as
		 * possible into facts. Attributes that cannot be turned into facts remain as
		 * clues.
		 */
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

		/*
		 * Now that we have identified as many facts as possible, we try to find out
		 * what kind of retro gear best fits the artifact. To do this we ask all gear
		 * specialists how confident they are that the artifact could represent their
		 * gear type. The "winner" (highest confidence) will later be built.
		 */
		GearSpecialist best = null;
		Confidence bestConfidence = Confidence.NONE;
		for (final GearSpecialist specialist : gearSpecialists.values()) {
			final Class<?> gearType = specialist.getGearDefinition().getType();
			final GearContext context = new GearContext(gearType, artifact, attributes);
			final Confidence confidence = specialist.matches(context);

			// The user might try to be clever and return null instead of a confidence
			Objects.requireNonNull(confidence, "The " + GearMatcher.class.getSimpleName() + " of type "
					+ specialist.getGearDefinition().getType() + " must not return null.");

			if (confidence == Confidence.NONE) {
				continue;
			}

			/*
			 * Note: Should there be more than one experts with the same (best) confidence
			 * we currently let the first expert win. This is because throwing would be
			 * overly harsh: It would mean that a single ambiguous artifact could stop the
			 * whole pipeline.
			 * 
			 * TODO: Find a way to inject the confidence level into gear and/or give the
			 * user more control in draw situations
			 * https://github.com/jewesta/retro-crawler/issues/12
			 */
			if (best == null || confidence.isHigherThan(bestConfidence)) {
				best = specialist;
				bestConfidence = confidence;
			}
		}

		if (best == null) {
			// No expert was confident
			return Optional.empty();
		}

		final Class<?> bestType = best.getGearDefinition().getType();
		final GearContext context = new GearContext(bestType, artifact, attributes);
		/*
		 * The gear specialist is asked to build a gear. Since it was confident it could
		 * do that we expect it to return a non-null value. Building might still throw,
		 * though. For example in case of problems with type matching / annotations /
		 * missing no-arg constructor etc. -- but that would be a fundamental problem
		 * with the gear declaration the user would have to fix.
		 */
		final Object newGear = best.create(context);
		// Intentionally not nullable!
		return Optional.of(newGear);
	}

}