package com.gini.scheduling.solver;

import java.time.Duration;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;

import com.gini.scheduling.domain.Schedule;

public class TimeTableConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // Hard constraints
                shiftConflict(constraintFactory),
                // Soft constraints
                nameShiftStability(constraintFactory),
        };
    }

    Constraint shiftConflict(ConstraintFactory constraintFactory) {
        // A shift can accommodate at most one staff at the same time.
        return constraintFactory
                // Select each pair of 2 different staffs ...
                .fromUniquePair(Schedule.class,
                        // ... in the same dates ...
                        Joiners.equal(Schedule::getDates),
                        // ... in the same shift ...
                        Joiners.equal(Schedule::getShift))
                // ... and penalize each pair with a hard weight.
                .penalize("Shift conflict", HardSoftScore.ONE_HARD);
    }

    Constraint nameShiftStability(ConstraintFactory constraintFactory) {
        // A name prefers to teach in a single shift.
        return constraintFactory
                .fromUniquePair(Schedule.class,
                        Joiners.equal(Schedule::getStaff))
                .filter((staff1, staff2) -> staff1.getShift() != staff2.getShift())
                .penalize("Name shift stability", HardSoftScore.ONE_SOFT);
    }

//    Constraint nameTimeEfficiency(ConstraintFactory constraintFactory) {
//        // A name prefers to teach sequential staffs and dislikes gaps between staffs.
//        return constraintFactory
//                .from(Schedule.class)
//                .join(Schedule.class, Joiners.equal(Schedule::getStaff),
//                        Joiners.equal((staff) -> staff.getDates().getdate()))
//                .filter((staff1, staff2) -> {
//                    Duration between = Duration.between(staff1.getDates().getEndTime(),
//                            staff2.getDates().getStartTime());
//                    return !between.isNegative() && between.compareTo(Duration.ofMinutes(30)) <= 0;
//                })
//                .reward("Name time efficiency", HardSoftScore.ONE_SOFT);
//    }

}
