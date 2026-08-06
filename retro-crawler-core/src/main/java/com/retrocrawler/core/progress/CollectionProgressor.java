package com.retrocrawler.core.progress;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Sequentially follows a collection through one progress controller. */
public final class CollectionProgressor<T> {

	private final ProgressController controller;

	private final Collection<T> collection;

	private final String stepLabel;

	CollectionProgressor(final ProgressController controller, final Collection<T> collection, final String stepLabel) {
		this.controller = Objects.requireNonNull(controller, "controller");
		this.collection = Objects.requireNonNull(collection, "collection");
		this.stepLabel = stepLabel;
	}

	public void forEach(final Consumer<T> consumer) {
		Objects.requireNonNull(consumer, "consumer");
		forEach((element, ignored) -> consumer.accept(element));
	}

	public void forEach(final BiConsumer<T, ProgressController> consumer) {
		Objects.requireNonNull(consumer, "consumer");
		if (collection.isEmpty()) {
			return;
		}
		if (stepLabel != null) {
			controller.setStepLabel(stepLabel);
		}
		controller.reset(collection.size());
		final Progressor[] progressors = controller.splitIntoEqualParts(collection.size());
		int index = 0;
		for (final T element : collection) {
			final Progressor elementProgressor = progressors[index++];
			consumer.accept(element, elementProgressor);
			elementProgressor.advanceToEnd();
		}
		controller.advanceToEnd();
	}
}
