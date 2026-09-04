package com.retrocrawler.model.hardware;

/**
 * A manufacturer-defined production marking observed on a processor package.
 */
public sealed interface ProcessorMarking permits AmdProcessorMarking, IntelProcessorMarking {
}
