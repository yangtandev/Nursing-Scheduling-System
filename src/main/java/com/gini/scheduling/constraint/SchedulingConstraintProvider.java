package com.gini.scheduling.constraint;

import com.gini.scheduling.model.Sgsch;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;

public class SchedulingConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
//                 Hard constraints
                sgschConflict(constraintFactory),
//                nameConflict(constraintFactory),
//                uteamConflict(constraintFactory),
//                Soft constraints
//                nameSgschStability(constraintFactory),
        };
    }

    Constraint sgschConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .fromUniquePair(Sgsch.class,
                        Joiners.equal(Sgsch::getUno),
                        Joiners.equal(Sgsch::getSchdate)
                ).filter((Sgsch1, Sgsch2) -> {
                    return true;
                })
                .penalize("Name conflict", HardSoftScore.ONE_HARD);
    }

//    Constraint nameConflict(ConstraintFactory constraintFactory) {
//        return constraintFactory
//                .fromUniquePair(Sgsch.class,
//                        Joiners.equal(Sgsch::getName),
//                        Joiners.equal(Sgsch::getDate)
//                ).filter((Sgsch1, Sgsch2) -> {
//                    if (Sgsch1.getSgruser().getId() == Sgsch2.getSgruser().getId()) {
//                        return false;
//                    }
//                    return true;
//                })
//                .penalize("Name conflict", HardSoftScore.ONE_HARD);
//    }
//
//    Constraint uteamConflict(ConstraintFactory constraintFactory) {
//        // A student can attend at most one sgruser at the same time.
//        return constraintFactory
//                .fromUniquePair(Sgsch.class,
//                        Joiners.equal(Sgsch::getSgruser),
//                        Joiners.equal(Sgsch::getSgruser))
//                .penalize("Team conflict", HardSoftScore.ONE_HARD);
//    }
//
//    Constraint nameSgschStability(ConstraintFactory constraintFactory) {
//        // A name prefers to teach in a single sgsch.
//        return constraintFactory
//                .fromUniquePair(Sgsch.class,
//                        Joiners.equal(Sgsch::getSgruser))
//                .filter((Sgsch1, Sgsch2) -> {
//                    if (Sgsch1.getDate() == Sgsch2.getDate() && Sgsch1.getName() == Sgsch2.getName()) {
//                        return Sgsch1.getSgruser().getId() != Sgsch2.getSgruser().getId();
//                    }
//                    return false;
//                })
//                .penalize("Name sgsch stability", HardSoftScore.ONE_HARD);
//    }


}
