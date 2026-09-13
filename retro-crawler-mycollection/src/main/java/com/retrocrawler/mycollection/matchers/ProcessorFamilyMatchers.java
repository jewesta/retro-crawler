package com.retrocrawler.mycollection.matchers;

import java.util.Objects;
import java.util.regex.Pattern;

import com.retrocrawler.core.gear.Confidence;
import com.retrocrawler.core.gear.GearContext;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.model.hardware.AmdProcessorMarking;
import com.retrocrawler.model.hardware.IntelProcessorMarking;
import com.retrocrawler.model.hardware.ProcessorMarking;
import com.retrocrawler.mycollection.AttributeNames;

/**
 * Matchers for processor identities stated by the title and package marking.
 */
public final class ProcessorFamilyMatchers {

	private static final String TOKEN_START = "(?<![A-Z0-9])";

	private static final String TOKEN_END = "(?![A-Z0-9])";

	private ProcessorFamilyMatchers() {
		// matcher namespace
	}

	private abstract static class TitleMatcher implements GearMatcher {

		private final Class<? extends ProcessorMarking> markingType;

		private final Pattern titlePattern;

		protected TitleMatcher(final Class<? extends ProcessorMarking> markingType, final String titlePattern) {
			this.markingType = Objects.requireNonNull(markingType, "markingType");
			this.titlePattern = Pattern.compile(Objects.requireNonNull(titlePattern, "titlePattern"),
					Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		}

		@Override
		public final Confidence matches(final GearContext context) {
			final boolean hasExpectedMarking = context.fact(AttributeNames.PROCESSOR_MARKING, markingType).isPresent();
			final boolean titleNamesFamily = context.fact(AttributeNames.TITLE, String.class)
					.filter(title -> titlePattern.matcher(title).find()).isPresent();
			return hasExpectedMarking && titleNamesFamily ? Confidence.EXACT : Confidence.NONE;
		}
	}

	public static final class Amd8086Matcher extends TitleMatcher {

		public Amd8086Matcher() {
			super(AmdProcessorMarking.class, "8086");
		}
	}

	public static final class Amd80286Matcher extends TitleMatcher {

		public Amd80286Matcher() {
			super(AmdProcessorMarking.class, TOKEN_START + "(?:AM286|N80L286|80C286|80286)" + TOKEN_END);
		}
	}

	public static final class Amd80386Matcher extends TitleMatcher {

		public Amd80386Matcher() {
			super(AmdProcessorMarking.class, TOKEN_START + "(?:AM386|80C386|80386)" + TOKEN_END);
		}
	}

	public static final class Amd80486Matcher extends TitleMatcher {

		public Amd80486Matcher() {
			super(AmdProcessorMarking.class, TOKEN_START + "(?:AM486|80C486|80486)" + TOKEN_END);
		}
	}

	public static final class Amd5x86Matcher extends TitleMatcher {

		public Amd5x86Matcher() {
			super(AmdProcessorMarking.class, TOKEN_START + "(?:AM5X86|AMD[- ]?X5|5X86)" + TOKEN_END);
		}
	}

	public static final class AmdK5Matcher extends TitleMatcher {

		public AmdK5Matcher() {
			super(AmdProcessorMarking.class, TOKEN_START + "K5" + TOKEN_END);
		}
	}

	public static final class AmdK6Matcher extends TitleMatcher {

		public AmdK6Matcher() {
			super(AmdProcessorMarking.class, TOKEN_START + "K6" + TOKEN_END);
		}
	}

	public static final class AmdK62Matcher extends TitleMatcher {

		public AmdK62Matcher() {
			super(AmdProcessorMarking.class, "K6[- ]2");
		}
	}

	public static final class AmdK6IIIMatcher extends TitleMatcher {

		public AmdK6IIIMatcher() {
			super(AmdProcessorMarking.class, TOKEN_START + "K6\\s*[- ]\\s*(?:III|3)" + TOKEN_END);
		}
	}

	public static final class Intel80286Matcher extends TitleMatcher {

		public Intel80286Matcher() {
			super(IntelProcessorMarking.class, "(?:I?80286|I286)");
		}
	}

	public static final class Intel80386Matcher extends TitleMatcher {

		public Intel80386Matcher() {
			super(IntelProcessorMarking.class, TOKEN_START + "(?:I?80386|I386)" + TOKEN_END);
		}
	}

	public static final class Intel80486Matcher extends TitleMatcher {

		public Intel80486Matcher() {
			super(IntelProcessorMarking.class, TOKEN_START + "(?:I?80486|I486)" + TOKEN_END);
		}
	}

	public static final class IntelPentiumMatcher extends TitleMatcher {

		public IntelPentiumMatcher() {
			super(IntelProcessorMarking.class, TOKEN_START + "PENTIUM" + TOKEN_END);
		}
	}

	public static final class IntelPentiumMmxMatcher extends TitleMatcher {

		public IntelPentiumMmxMatcher() {
			super(IntelProcessorMarking.class, TOKEN_START + "PENTIUM.*" + TOKEN_START + "MMX" + TOKEN_END);
		}
	}

	public static final class IntelPentium4Matcher extends TitleMatcher {

		public IntelPentium4Matcher() {
			super(IntelProcessorMarking.class, TOKEN_START + "PENTIUM\\s+(?:4|IV)" + TOKEN_END);
		}
	}

	public static final class IntelCoreMatcher extends TitleMatcher {

		public IntelCoreMatcher() {
			super(IntelProcessorMarking.class, "CORE");
		}
	}

	public static final class IntelMathCoprocessorMatcher extends TitleMatcher {

		public IntelMathCoprocessorMatcher() {
			super(IntelProcessorMarking.class, "(?:I?8087|I?80287|I?80387|I?80487|[234]87)");
		}
	}
}
