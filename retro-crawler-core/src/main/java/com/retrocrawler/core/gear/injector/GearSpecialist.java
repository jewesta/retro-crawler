package com.retrocrawler.core.gear.injector;

import java.util.List;
import java.util.Objects;

import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.GearContext;
import com.retrocrawler.core.gear.GearDescriptor;
import com.retrocrawler.core.gear.GearFactory;
import com.retrocrawler.core.gear.RetroAttributes;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.core.util.Reflection;

public class GearSpecialist implements GearMatcher, GearFactory {

	private final GearDescriptor definition;

	private final UnassignedPolicy unassignedPolicy = new UnassignedPolicy();

	private final RetroAttributeAdapter adapter = new RetroAttributeAdapter();

	private final List<Injector> injectors;

	public GearSpecialist(final GearDescriptor descriptor) {
		this.definition = Objects.requireNonNull(descriptor, "descriptor");
		/*
		 * Order is important! The AnyAttributeInjector must come last because it
		 * (potentially) injects what the other injectors skipped.
		 */
		this.injectors = List.of(new DeclaredFactsInjector(adapter), new StandaloneIdInjector(adapter),
				new AnyAttributeInjector());
	}

	public GearDescriptor getGearDefinition() {
		return definition;
	}

	@Override
	public Confidence matches(final GearContext context) {
		return definition.getMatcher().matches(context);
	}

	@Override
	public Object create(final GearContext context) {
		Objects.requireNonNull(context, "context");

		final Object gear = Reflection.newInstance(definition.getType());
		final RetroAttributes attributes = Objects.requireNonNull(context.attributes(), "attributes");

		final GearInjectionSession session = unassignedPolicy.createSession(definition, gear, attributes);

		for (final Injector injector : injectors) {
			injector.inject(session);
		}

		return gear;
	}
}