package com.retrocrawler.model.hardware;

import java.util.Locale;

import com.retrocrawler.core.gear.RatedFact;
import com.retrocrawler.core.gear.parser.EnumFactParser;
import com.retrocrawler.core.gear.parser.ParseContext;

public final class VideoConnectorParser implements EnumFactParser<VideoConnector> {

	@Override
	public Class<VideoConnector> enumType() {
		return VideoConnector.class;
	}

	@Override
	public RatedFact<VideoConnector> parse(final String rawValue, final ParseContext context) {
		if (rawValue == null) {
			return RatedFact.none("Expected a recognized video connector.");
		}

		final String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
		return switch (normalized) {
		case "DISPLAYPORT", "DISPLAY PORT", "DP" -> RatedFact.exact(VideoConnector.DISPLAY_PORT);
		case "DVI" -> RatedFact.exact(VideoConnector.DVI);
		case "HDMI" -> RatedFact.exact(VideoConnector.HDMI);
		case "S-VIDEO", "S VIDEO", "SVIDEO" -> RatedFact.exact(VideoConnector.S_VIDEO);
		case "VGA" -> RatedFact.exact(VideoConnector.VGA);
		default -> RatedFact.none("Expected a recognized video connector.");
		};
	}
}
