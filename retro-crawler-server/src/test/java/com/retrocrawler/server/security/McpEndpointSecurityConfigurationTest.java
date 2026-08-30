package com.retrocrawler.server.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

class McpEndpointSecurityConfigurationTest {

	private static final String TOKEN = "mcp-token-with-at-least-thirty-two-characters";
	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withUserConfiguration(McpEndpointSecurityConfiguration.class);

	@Test
	void protectsOnlyTheMcpRouteWhenATokenIsConfigured() {
		contextRunner.withPropertyValues("retro-crawler.server.mcp.bearer-token=" + TOKEN).run(context -> {
			assertThat(context).hasNotFailed().hasSingleBean(McpEndpointSecurityProperties.class)
					.hasSingleBean(FilterRegistrationBean.class);
			final FilterRegistrationBean<?> registration = context.getBean(FilterRegistrationBean.class);
			assertThat(registration.getUrlPatterns()).containsExactlyInAnyOrder("/mcp", "/mcp/*");
		});
	}

	@Test
	void failsClosedWhenNoTokenIsConfigured() {
		contextRunner.run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure())
					.hasRootCauseMessage("retro-crawler.server.mcp.bearer-token must be configured");
		});
	}

	@Test
	void rejectsAWeakToken() {
		contextRunner.withPropertyValues("retro-crawler.server.mcp.bearer-token=too-short").run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure())
					.hasRootCauseMessage("retro-crawler.server.mcp.bearer-token must contain at least 32 characters");
		});
	}
}
