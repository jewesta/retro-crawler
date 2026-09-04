package com.retrocrawler.mycollection.gear;

import com.retrocrawler.core.annotation.RetroFact;
import com.retrocrawler.model.hardware.ProcessorMarking;
import com.retrocrawler.model.hardware.ProcessorMarkingParser;
import com.retrocrawler.mycollection.AttributeNames;

public abstract class Processor extends MyGear {

	@RetroFact(key = AttributeNames.PROCESSOR_MARKING, parser = ProcessorMarkingParser.class, strict = false,
			optional = false)
	private ProcessorMarking processorMarking;

	protected Processor() {
	}

	public ProcessorMarking processorMarking() {
		return processorMarking;
	}
}
