package com.gini.scheduling.solver;

import com.gini.scheduling.domain.Shift;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;

public class TimeTableConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                // Hard constraints
//                shiftConflict(constraintFactory),
//                nameConflict(constraintFactory),
//                teamConflict(constraintFactory),
//                // Soft constraints
                nameShiftStability(constraintFactory),
//                nameTimeEfficiency(constraintFactory),
//                teamCardIDVariety(constraintFactory)
        };
    }

//    Constraint shiftConflict(ConstraintFactory constraintFactory) {
//        // A shift can accommodate at most one staff at the same time.
//        return constraintFactory
//                .fromUniquePair(Shift.class,
//                        Joiners.equal(Shift::getStaff),
//                        Joiners.equal(Shift::getDate))
//                .penalize("Shift conflict", HardSoftScore.ONE_HARD);
//    }

//    Constraint nameConflict(ConstraintFactory constraintFactory) {
//        // A name can teach at most one staff at the same time.
//        return constraintFactory
//                .fromUniquePair(Shift.class,
//                        Joiners.equal(Shift::getShift),
//                        Joiners.equal(Shift::getStaff))
//                .penalize("Name conflict", HardSoftScore.ONE_HARD);
//    }
//
//    Constraint teamConflict(ConstraintFactory constraintFactory) {
//        // A student can attend at most one staff at the same time.
//        return constraintFactory
//                .fromUniquePair(Shift.class,
//                        Joiners.equal(Shift::getShift),
//                        Joiners.equal(Shift::getStaff))
//                .penalize("Team conflict", HardSoftScore.ONE_HARD);
//    }
//
    Constraint nameShiftStability(ConstraintFactory constraintFactory) {
        // A name prefers to teach in a single shift.
        return constraintFactory
                .fromUniquePair(Shift.class,
                        Joiners.equal(Shift::getStaff))
                .filter((Shift1, Shift2) -> Shift1.getDate() != Shift2.getDate())
                .penalize("Name shift stability", HardSoftScore.ONE_HARD);
    }
//
//    Constraint nameTimeEfficiency(ConstraintFactory constraintFactory) {
//        // A name prefers to teach sequential staffs and dislikes gaps between staffs.
//        return constraintFactory
//                .from(Shift.class)
//                .join(Shift.class, Joiners.equal(Shift::getStaff),
//                        Joiners.equal((Shift) -> Shift.getShift().getDate()))
//                .filter((Shift1, Shift2) -> {
//                    Duration between = Duration.between(Shift1.getShift().getDate(),
//                            Shift2.getShift().getDate());
//                    return !between.isNegative() && between.compareTo(Duration.ofMinutes(30)) <= 0;
//                })
//                .reward("Name time efficiency", HardSoftScore.ONE_SOFT);
//    }
//
//    Constraint teamCardIDVariety(ConstraintFactory constraintFactory) {
//        // A team dislikes sequential staffs on the same id.
//        return constraintFactory
//                .from(Shift.class)
//                .join(Shift.class,
//                        Joiners.equal(Shift::getStaff),
//                        Joiners.equal(Shift::getShift),
//                        Joiners.equal((Shift) -> Shift.getShift().getDate()))
//                .filter((Shift1, Shift2) -> {
//                    Duration between = Duration.between(Shift1.getShift().getDate(),
//                            Shift2.getShift().getDate());
//                    return !between.isNegative() && between.compareTo(Duration.ofMinutes(30)) <= 0;
//                })
//                .penalize("Team id variety", HardSoftScore.ONE_SOFT);
//    }
}
