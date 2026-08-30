package com.retrocrawler.mycollection;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.retrocrawler.core.Model;

/** Supplies MyCollection's interpretation model to a consuming application. */
@AutoConfiguration
@ConditionalOnMissingBean(Model.class)
public class MyCollectionAutoConfiguration {

	@Bean
	Model model() {
		return Model.from(MyCollectionAutoConfiguration.class.getPackageName());
	}
}
