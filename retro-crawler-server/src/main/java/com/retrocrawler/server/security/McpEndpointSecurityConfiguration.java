package com.retrocrawler.server.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Requires bearer authentication for every MCP HTTP exchange. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(McpEndpointSecurityProperties.class)
class McpEndpointSecurityConfiguration {

	@Bean
	FilterRegistrationBean<McpBearerTokenFilter> mcpBearerTokenFilter(final McpEndpointSecurityProperties properties) {
		final FilterRegistrationBean<McpBearerTokenFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(new McpBearerTokenFilter(properties.bearerToken()));
		registration.setName("mcpBearerTokenFilter");
		registration.addUrlPatterns("/mcp", "/mcp/*");
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}
}
