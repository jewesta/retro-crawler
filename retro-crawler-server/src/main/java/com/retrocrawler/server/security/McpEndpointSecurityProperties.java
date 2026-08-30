package com.retrocrawler.server.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Authentication configuration for the MCP HTTP endpoint. */
@ConfigurationProperties("retro-crawler.server.mcp")
public record McpEndpointSecurityProperties(String bearerToken) {

	private static final int MINIMUM_TOKEN_LENGTH = 32;

	public McpEndpointSecurityProperties {
		if (bearerToken == null || bearerToken.isBlank()) {
			throw new IllegalArgumentException("retro-crawler.server.mcp.bearer-token must be configured");
		}
		if (!bearerToken.equals(bearerToken.strip())) {
			throw new IllegalArgumentException(
					"retro-crawler.server.mcp.bearer-token must not contain surrounding whitespace");
		}
		if (bearerToken.length() < MINIMUM_TOKEN_LENGTH) {
			throw new IllegalArgumentException("retro-crawler.server.mcp.bearer-token must contain at least "
					+ MINIMUM_TOKEN_LENGTH + " characters");
		}
	}

	@Override
	public String toString() {
		return "McpEndpointSecurityProperties[bearerToken=<redacted>]";
	}
}
