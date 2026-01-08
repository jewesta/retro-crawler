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

	private final GearDescriptor descriptor;

	private final RetroAttributeAdapter adapter = new RetroAttributeAdapter();

	private final List<Injector> injectors;

	public GearSpecialist(final GearDescriptor descriptor) {
		this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
		/*
		 * Order is important! The AnyAttributeInjector must come last because it
		 * (potentially) injects what the other injectors skipped.
		 */
		this.injectors = List.of(new DeclaredFactsInjector(adapter), new StandaloneIdInjector(adapter),
				new AnyAttributeInjector());
	}

	public GearDescriptor getGearDefinition() {
		return descriptor;
	}

	@Override
	public Confidence matches(final GearContext context) {
		return descriptor.getMatcher().matches(context);
	}

	@Override
	public Object create(final GearContext context) {
		Objects.requireNonNull(context, "context");
		final RetroAttributes attributes = Objects.requireNonNull(context.attributes(), "attributes");

		// The birth of a new gear
		final Object gear = Reflection.newInstance(descriptor.getType());
		final GearInjectionSession session = new GearInjectionSession(descriptor, gear, attributes);
		for (final Injector injector : injectors) {
			injector.inject(session);
		}

		return gear;
	}
}