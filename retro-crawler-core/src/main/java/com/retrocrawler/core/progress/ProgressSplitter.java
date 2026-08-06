package com.retrocrawler.core.progress;

import java.math.BigDecimal;
import java.util.Collection;

/** Divides a progress range into nested progressors. */
public interface ProgressSplitter {

	Progressor[] splitByPercentages(BigDecimal... percentages);

	Progressor[] splitByPercentages(Collection<BigDecimal> percentages);

	Progressor[] splitIntoEqualParts(int parts);

	Progressor[] splitInRelationTo(Collection<Double> segments);

	Progressor[] splitInRelationTo(double... segments);

	ProgressSplitter setStepLabel(String label);
}
