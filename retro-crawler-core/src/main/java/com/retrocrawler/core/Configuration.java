package com.retrocrawler.core;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable collection-wide settings available while clues are interpreted.
 */
public record Configuration(Locale locale, ZoneId timeZone, Clock clock) {

	public Configuration {
		locale = Objects.requireNonNull(locale, "locale");
		timeZone = Objects.requireNonNull(timeZone, "timeZone");
		clock = Objects.requireNonNull(clock, "clock").withZone(timeZone);
	}

	/**
	 * Starts with the host's current format locale, time zone, and clock.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Starts with this configuration as its base values.
	 */
	public Builder toBuilder() {
		return new Builder(this);
	}

	public static final class Builder {

		private Locale locale;
		private ZoneId timeZone;
		private Clock clock;
		private boolean timeZoneOverridden;

		private Builder() {
			locale = Locale.getDefault(Locale.Category.FORMAT);
			timeZone = ZoneId.systemDefault();
			clock = Clock.system(timeZone);
		}

		private Builder(final Configuration base) {
			locale = base.locale();
			timeZone = base.timeZone();
			clock = base.clock();
		}

		public Builder locale(final Locale value) {
			locale = Objects.requireNonNull(value, "value");
			return this;
		}

		public Builder timeZone(final ZoneId value) {
			timeZone = Objects.requireNonNull(value, "value");
			timeZoneOverridden = true;
			clock = clock.withZone(timeZone);
			return this;
		}

		/**
		 * Sets the clock and, unless this builder also overrides the time zone,
		 * uses the clock's zone as the effective time zone.
		 */
		public Builder clock(final Clock value) {
			clock = Objects.requireNonNull(value, "value");
			if (timeZoneOverridden) {
				clock = clock.withZone(timeZone);
			} else {
				timeZone = clock.getZone();
			}
			return this;
		}

		public Configuration build() {
			return new Configuration(locale, timeZone, clock);
		}
	}
}
