package com.retrocrawler.core.gear;

import java.util.HashSet;
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
		String resolvedKey = null;
		for (final String raw : raws) {
			final BestAnonymousMatch best = findBestAnonymousMatch(raw);
			if (best == null || best.confidence() == Confidence.NONE) {
				putAnonymousClueIfUseful(resolved, clue);
				return;
			}
			final String candidateKey = best.key();
			if (resolvedKey != null && !resolvedKey.equals(candidateKey)) {
				putAnonymousClueIfUseful(resolved, clue);
				return;
			}
			resolvedKey = candidateKey;
		}

		if (resolvedKey == null) {
			putAnonymousClueIfUseful(resolved, clue);
			return;
		}

		final FactFinder finder = factFinders.get(resolvedKey);
		if (finder == null) {
			putAnonymousClueIfUseful(resolved, clue);
			return;
		}

		final RetroAttribute existing = resolved.get(resolvedKey);
		if (existing == null) {
			final Optional<Fact> fact = finder.find(clue);
			if (fact.isPresent()) {
				resolved.put(fact.get());
			} else {
				putAnonymousClueIfUseful(resolved, clue);
			}
			return;
		}

		reconcile(resolved, resolvedKey, existing, clue, finder);
	}

	private static void reconcile(final RetroAttributes resolved, final String resolvedKey,
			final RetroAttribute existing, final Clue incoming, final FactFinder finder) {

		final Set<String> combinedValues = new HashSet<>(incoming.getValue());
		if (existing instanceof final Fact fact) {
			combinedValues.addAll(fact.source().getValue());
		} else if (existing instanceof final Clue clue) {
			combinedValues.addAll(clue.getValue());
		}

		final Clue combined = Clue.of(resolvedKey, Set.copyOf(combinedValues));
		final Optional<Fact> combinedFact = finder.find(combined);
		if (existing instanceof final Fact fact && combinedFact.isPresent()
				&& fact.getValue().equals(combinedFact.get().getValue())) {
			// The new observation corroborates the already resolved fact.
			return;
		}

		resolved.replace(combinedFact.<RetroAttribute>map(Function.identity()).orElse(combined));
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
		return resolveWithIdentity(artifact).map(GearResolution::gear);
	}

	@SuppressWarnings(Sonar.JAVA_REDUCE_NUMBER_OF_BREAK_AND_CONTINUE)
	public Optional<GearResolution> resolveWithIdentity(final Artifact artifact) {
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
		final Optional<Object> retroId = retroId(best.getGearDefinition(), attributes);
		return Optional.of(new GearResolution(newGear, retroId));
	}

	private static Optional<Object> retroId(final GearDescriptor descriptor, final RetroAttributes attributes) {
		final Optional<String> idKey = descriptor.getIdAttributeKey();
		if (idKey.isEmpty()) {
			return Optional.empty();
		}

		final RetroAttribute attribute = attributes.get(idKey.get());
		if (!(attribute instanceof Fact)) {
			return Optional.empty();
		}

		final Set<? extends Object> values = attribute.getValue();
		if (values.size() != 1) {
			throw new IllegalStateException("Expected exactly one @RetroId value for key '" + idKey.get() + "' on "
					+ descriptor.getType().getName() + " but got " + values.size() + ".");
		}
		return Optional.of(values.iterator().next());
	}

}
