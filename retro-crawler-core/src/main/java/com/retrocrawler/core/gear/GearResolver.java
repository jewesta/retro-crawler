package com.retrocrawler.core.gear;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.clues.DuplicateClueException;
import com.retrocrawler.core.gear.filter.FilterDefinition;
import com.retrocrawler.core.gear.injector.GearSpecialist;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.core.gear.parser.ParseContext;
import com.retrocrawler.core.gear.trace.ResolutionTrace.Phase;
import com.retrocrawler.core.util.RetroAttribute;
import com.retrocrawler.core.util.Sonar;

/**
 * Interprets a model-independent {@link Artifact} as model-dependent facts and
 * gear without modifying the artifact or its cached clues.
 */
public class GearResolver {

	private final Map<Class<?>, GearSpecialist> gearSpecialists;

	private final Map<String, FactFinder> factFinders;

	private final ClueClassifier clueClassifier;

	private final Map<Class<?>, Set<String>> contextualFactKeys;

	private final List<FilterDefinition<?>> filters;

	// package-private: only factories construct this
	GearResolver(final Map<Class<?>, GearSpecialist> specialists, final Map<String, FactFinder> factFinders,
			final Map<Class<?>, Set<String>> contextualFactKeys, final List<FilterDefinition<?>> filters) {
		this.gearSpecialists = Objects.requireNonNull(specialists, "specialists");
		this.factFinders = Objects.requireNonNull(factFinders, "factFinders");
		this.contextualFactKeys = Objects.requireNonNull(contextualFactKeys, "contextualFactKeys");
		this.filters = List.copyOf(Objects.requireNonNull(filters, "filters"));
		this.clueClassifier = new ClueClassifier(factFinders.keySet());
	}

	/** Every structured filter derived from this resolver's Fact model. */
	public List<FilterDefinition<?>> filters() {
		return filters;
	}

	private record BestAnonymousMatch(String key, Confidence confidence, boolean contextual, boolean ambiguous) {
	}

	private void handleKnownKeyClue(final RetroAttributes resolved, final Clue clue, final ParseContext parseContext) {
		final String key = clue.key();

		if (resolved.containsKey(key)) {
			rejectDuplicateSemanticKey(key, resolved.get(key), clue);
		}

		final FactFinder finder = factFinders.get(key);
		final RetroAttribute attribute;
		if (finder == null) {
			attribute = clue;
		} else {
			attribute = finder.find(clue, parseContext).<RetroAttribute> map(Function.identity()).orElse(clue);
		}

		resolved.put(attribute);
	}

	@SuppressWarnings({
			Sonar.JAVA_REDUCE_NUMBER_OF_BREAK_AND_CONTINUE
	})
	private void handleAnonymousClue(final RetroAttributes resolved, final Clue clue, final ParseContext parseContext,
			final Set<String> allowedContextualKeys, final ResolutionTraceRecorder trace, final Phase phase) {
		final Set<String> raws = clue.value();
		String resolvedKey = null;
		for (final String raw : raws) {
			final BestAnonymousMatch best = findBestAnonymousMatch(raw, parseContext, allowedContextualKeys);
			if (best == null || best.confidence() == Confidence.NONE) {
				putAnonymousClueIfUseful(resolved, clue);
				return;
			}
			if (best.ambiguous()) {
				trace.ambiguousFact(phase,
						"Anonymous value '" + raw + "' matched more than one Fact key at " + best.confidence() + ".");
				putAnonymousClueIfUseful(resolved, clue);
				return;
			}
			final String candidateKey = best.key();
			if (resolvedKey != null && !resolvedKey.equals(candidateKey)) {
				trace.ambiguousFact(phase, "Values of one anonymous clue resolved to different Fact keys: '"
						+ resolvedKey + "' and '" + candidateKey + "'.");
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
			final Optional<Fact> fact = finder.find(clue, parseContext);
			if (fact.isPresent()) {
				resolved.put(fact.get());
			} else {
				putAnonymousClueIfUseful(resolved, clue);
			}
			return;
		}

		if (existing instanceof final Fact fact && fact.source().isAnonymous()) {
			reconcileAnonymousClues(resolved, resolvedKey, fact, clue, finder, parseContext, trace, phase);
			return;
		}

		/*
		 * Explicitly keyed evidence is authoritative. Preserve the anonymous
		 * observation under its generated key, but do not let its lenient
		 * interpretation compete with the explicit semantic claim.
		 */
		putAnonymousClueIfUseful(resolved, clue);
	}

	private static void reconcileAnonymousClues(final RetroAttributes resolved, final String resolvedKey,
			final Fact existing, final Clue incoming, final FactFinder finder, final ParseContext parseContext,
			final ResolutionTraceRecorder trace, final Phase phase) {
		final Set<String> combinedValues = new HashSet<>(existing.source().value());
		combinedValues.addAll(incoming.value());

		final Clue combinedAnonymous = Clue.of(Set.copyOf(combinedValues));
		final Optional<Fact> combinedFact = finder.find(combinedAnonymous, parseContext);
		if (combinedFact.isPresent() && existing.value().equals(combinedFact.get().value())) {
			return;
		}
		trace.ambiguousFact(phase,
				"Anonymous clues competing for Fact key '" + resolvedKey + "' could not be reconciled.");

		final RetroAttribute combined = combinedFact.<RetroAttribute> map(Function.identity())
				.orElseGet(() -> Clue.of(resolvedKey, Set.copyOf(combinedValues)));
		resolved.replace(combined);
	}

	private static void rejectDuplicateSemanticKey(final String key, final RetroAttribute existing,
			final Clue incoming) {
		final Clue previous = existing instanceof final Fact fact ? fact.source() : (Clue) existing;
		throw new DuplicateClueException("More than one clue resolves to semantic key '" + key + "'. First clue: "
				+ previous + ", duplicate clue: " + incoming + ".");
	}

	private void putAnonymousClueIfUseful(final RetroAttributes resolved, final Clue clue) {
		if (!resolved.containsClue(clue)) {
			resolved.put(clue);
		}
	}

	private BestAnonymousMatch findBestAnonymousMatch(final String raw, final ParseContext parseContext,
			final Set<String> allowedContextualKeys) {
		BestAnonymousMatch best = null;
		for (final FactFinder finder : factFinders.values()) {
			if (finder.isStrict() || finder.isContextual() && !allowedContextualKeys.contains(finder.key())) {
				continue;
			}
			best = considerAnonymousCandidate(best, finder, raw, parseContext);
		}
		return best;
	}

	private BestAnonymousMatch considerAnonymousCandidate(final BestAnonymousMatch bestSoFar, final FactFinder finder,
			final String raw, final ParseContext parseContext) {

		final RatedFact<?> rated = finder.parse(raw, parseContext);
		final Confidence confidence = rated.confidence();
		if (confidence == Confidence.NONE) {
			return bestSoFar;
		}

		final BestAnonymousMatch candidate = new BestAnonymousMatch(finder.key(), confidence, finder.isContextual(),
				false);

		if (bestSoFar == null) {
			return candidate;
		}

		if (confidence.isHigherThan(bestSoFar.confidence())) {
			return candidate;
		}

		if (confidence == bestSoFar.confidence()) {
			if (candidate.contextual() != bestSoFar.contextual()) {
				return candidate.contextual() ? candidate : bestSoFar;
			}
			/*
			 * Equal candidates mean that the model cannot identify what the
			 * anonymous observation says. Keep that evidence as a clue instead
			 * of choosing by map iteration order or failing resolution. An
			 * explicitly keyed clue remains unambiguous and is handled by
			 * handleKnownKeyClue(...).
			 */
			return new BestAnonymousMatch(bestSoFar.key(), confidence, bestSoFar.contextual(), true);
		}

		return bestSoFar;
	}

	public Optional<Object> resolve(final Artifact artifact, final ParseContext parseContext) {
		return resolveWithIdentity(artifact, parseContext).map(GearResolution::gear);
	}

	@SuppressWarnings(Sonar.JAVA_REDUCE_NUMBER_OF_BREAK_AND_CONTINUE)
	public Optional<GearResolution> resolveWithIdentity(final Artifact artifact, final ParseContext parseContext) {
		Objects.requireNonNull(artifact, "artifact");
		Objects.requireNonNull(parseContext, "parseContext");
		final ARI source = parseContext.source();
		final ResolutionTraceRecorder trace = new ResolutionTraceRecorder(artifact);

		/*
		 * We are now looking at the given artifact and we want to turn it into
		 * a new retro gear (if possible). To do this we first try to turn as
		 * many clues as possible into facts. Attributes that cannot be turned
		 * into facts remain as clues.
		 */
		final Clues clues = clueClassifier.classify(artifact.clues());
		final RetroAttributes detectionAttributes = resolveAttributes(clues, parseContext, Set.of(), trace,
				Phase.DETECTION);
		trace.attributes(Phase.DETECTION, detectionAttributes);

		/*
		 * Now that we have identified as many facts as possible, we try to find
		 * out what kind of retro gear best fits the artifact. To do this we ask
		 * all gear specialists how confident they are that the artifact could
		 * represent their gear type. The "winner" (highest confidence) will
		 * later be built.
		 */
		GearSpecialist best = null;
		Confidence bestConfidence = Confidence.NONE;
		for (final GearSpecialist specialist : gearSpecialists.values()) {
			final Class<?> gearType = specialist.gearDefinition().type();
			final GearContext context = new GearContext(gearType, source, artifact, detectionAttributes);
			final Confidence confidence = specialist.matches(context);

			// The user might try to be clever and return null instead of a confidence
			Objects.requireNonNull(confidence, "The " + GearMatcher.class.getSimpleName() + " of type "
					+ specialist.gearDefinition().type() + " must not return null.");
			trace.match(specialist, confidence);

			if (confidence == Confidence.NONE) {
				continue;
			}

			/*
			 * Should more than one specialist have the same best confidence,
			 * the first still wins rather than stopping the pipeline. The trace
			 * retains every match and explicitly records that ambiguity.
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

		final Class<?> bestType = best.gearDefinition().type();
		trace.selected(bestType);
		final Set<String> selectedContextualKeys = contextualFactKeys.getOrDefault(bestType, Set.of());
		final RetroAttributes attributes = resolveAttributes(clues, parseContext, selectedContextualKeys, trace,
				Phase.RESOLUTION);
		trace.attributes(Phase.RESOLUTION, attributes);
		final GearContext context = new GearContext(bestType, source, artifact, attributes);
		/*
		 * The gear specialist is asked to build a gear. Since it was confident
		 * it could do that we expect it to return a non-null value. Building
		 * might still throw, though. For example in case of problems with type
		 * matching / annotations / missing no-arg constructor etc. -- but that
		 * would be a fundamental problem with the gear declaration the user
		 * would have to fix.
		 */
		final Object newGear = best.create(context);
		final Optional<Object> retroId = retroId(best.gearDefinition(), attributes);
		return Optional.of(new GearResolution(newGear, retroId, trace.trace()));
	}

	private RetroAttributes resolveAttributes(final Clues clues, final ParseContext parseContext,
			final Set<String> allowedContextualKeys, final ResolutionTraceRecorder trace, final Phase phase) {
		final RetroAttributes attributes = new RetroAttributes();
		for (final Clue clue : clues) {
			if (!clue.isAnonymous()) {
				handleKnownKeyClue(attributes, clue, parseContext);
			}
		}
		for (final Clue clue : clues) {
			if (clue.isAnonymous()) {
				handleAnonymousClue(attributes, clue, parseContext, allowedContextualKeys, trace, phase);
			}
		}
		return attributes;
	}

	private static Optional<Object> retroId(final GearDescriptor descriptor, final RetroAttributes attributes) {
		final Optional<String> idKey = descriptor.idAttributeKey();
		if (idKey.isEmpty()) {
			return Optional.empty();
		}

		final RetroAttribute attribute = attributes.get(idKey.get());
		if (!(attribute instanceof Fact)) {
			return Optional.empty();
		}

		final Set<? extends Object> values = attribute.value();
		if (values.size() != 1) {
			throw new IllegalStateException("Expected exactly one @RetroId value for key '" + idKey.get() + "' on "
					+ descriptor.type().getName() + " but got " + values.size() + ".");
		}
		return Optional.of(values.iterator().next());
	}

}
