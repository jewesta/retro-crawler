package com.retrocrawler.core;

import java.util.Objects;
import java.util.Set;

import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.gear.ReflectiveFactory;

/**
 * @deprecated Use {@link Model#from(Set)} and {@link RetroCrawler#builder()}.
 */
@Deprecated(forRemoval = true)
public class RetroCrawlerFactory implements ReflectiveFactory<RetroCrawler> {

	private final Repository repository;

	public RetroCrawlerFactory(final Repository repository) {
		this.repository = Objects.requireNonNull(repository, "repository");
	}

	@Override
	public RetroCrawler reflectOn(final Set<Class<?>> types) {
		Objects.requireNonNull(types, "types");
		return RetroCrawler.builder().model(Model.from(types)).repository(repository).build();
	}
}
