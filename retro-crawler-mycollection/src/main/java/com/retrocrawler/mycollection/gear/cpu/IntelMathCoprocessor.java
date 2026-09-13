package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.mycollection.matchers.ProcessorFamilyMatchers.IntelMathCoprocessorMatcher;

@RetroGear(value = IntelMathCoprocessorMatcher.class, key = "intel-math-coprocessor", name = "Intel math coprocessor")
public final class IntelMathCoprocessor extends IntelProcessor {

	public IntelMathCoprocessor() {
	}
}
