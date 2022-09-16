package com.gini.scheduling.constraint;

import com.gini.scheduling.model.Sgresult;
import org.apache.commons.lang3.builder.CompareToBuilder;
import java.util.Comparator;

public class SchedulingDifficultyComparator implements Comparator<Sgresult> {
	public int compare(Sgresult a, Sgresult b) {
		return new CompareToBuilder().append(a.getSchdate(), b.getSchdate())
				.append(a.isShiftWork(), b.isShiftWork()).toComparison();
	}
}
