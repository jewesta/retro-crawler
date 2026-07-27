package com.retrocrawler.mycollection.references;

import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.model.identifier.TheRetroWebCategory;
import com.retrocrawler.model.identifier.TheRetroWebReference;
import com.retrocrawler.mycollection.gear.GraphicsCard;
import com.retrocrawler.mycollection.gear.MyGear;

/**
 * Maps recognized collection gear onto The Retro Web's content taxonomy.
 *
 * <p>
 * This adapter deliberately owns knowledge of both models. Collection gear
 * remains independent of The Retro Web, while the shared reference model
 * remains independent of this collection's concrete gear classes.
 */
public final class TheRetroWebReferences {

	public Optional<TheRetroWebReference> referenceFor(final MyGear gear) {
		Objects.requireNonNull(gear, "gear");

		if (gear instanceof GraphicsCard) {
			return gear.getTheRetroWebId().map(TheRetroWebCategory.EXPANSION_CARD::reference);
		}

		return Optional.empty();
	}
}
