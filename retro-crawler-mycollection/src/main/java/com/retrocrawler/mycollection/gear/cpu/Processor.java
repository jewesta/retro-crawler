package com.retrocrawler.mycollection.gear.cpu;

import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.model.hardware.ProcessorMarking;
import com.retrocrawler.model.hardware.ProcessorMarkingParser;
import com.retrocrawler.mycollection.AttributeNames;
import com.retrocrawler.mycollection.gear.MyGear;
import com.retrocrawler.mycollection.matchers.ProcessorMatcher;

@RetroGear(value = ProcessorMatcher.class, name = "Processor")
public class Processor extends MyGear {

	@RetroFact(key = AttributeNames.PROCESSOR_MARKING, parser = ProcessorMarkingParser.class, strict = false,
			optional = false)
	private ProcessorMarking processorMarking;

	public Processor() {
	}

	public ProcessorMarking processorMarking() {
		return processorMarking;
	}
}
