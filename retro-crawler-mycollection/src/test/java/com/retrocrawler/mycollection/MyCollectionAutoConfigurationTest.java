package com.retrocrawler.mycollection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.retrocrawler.core.Model;
import com.retrocrawler.core.RetroCrawler;

class MyCollectionAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MyCollectionAutoConfiguration.class));

	@Test
	void suppliesTheCollectionModelWithoutDeploymentConfiguration() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed().hasSingleBean(Model.class).doesNotHaveBean(RetroCrawler.class);
			assertThat(context.getBean(Model.class).collectionId()).isEqualTo("my_collection");
		});
	}

	@Test
	void preservesAnApplicationProvidedModel() {
		final Model model = mock(Model.class);

		contextRunner.withBean(Model.class, () -> model)
				.run(context -> assertThat(context.getBean(Model.class)).isSameAs(model));
	}
}
