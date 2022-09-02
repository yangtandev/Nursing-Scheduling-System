package com.gini.scheduling.constraint;

import com.gini.scheduling.model.Sgresult;
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
                sgresultConflict(constraintFactory),
//                nameConflict(constraintFactory),
//                uteamConflict(constraintFactory),
//                Soft constraints
//                nameSgresultStability(constraintFactory),
        };
    }

    Constraint sgresultConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .fromUniquePair(Sgresult.class,
                        Joiners.equal(Sgresult::getUno),
                        Joiners.equal(Sgresult::getSchdate)
                ).filter((Sgresult1, Sgresult2) -> {
                    return true;
                })
                .penalize("UNO conflict", HardSoftScore.ONE_HARD);
    }

//    Constraint nameConflict(ConstraintFactory constraintFactory) {
//        return constraintFactory
//                .fromUniquePair(Sgresult.class,
//                        Joiners.equal(Sgresult::getName),
//                        Joiners.equal(Sgresult::getDate)
//                ).filter((Sgresult1, Sgresult2) -> {
//                    if (Sgresult1.getSgruser().getId() == Sgresult2.getSgruser().getId()) {
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
//                .fromUniquePair(Sgresult.class,
//                        Joiners.equal(Sgresult::getSgruser),
//                        Joiners.equal(Sgresult::getSgruser))
//                .penalize("Team conflict", HardSoftScore.ONE_HARD);
//    }
//
//    Constraint nameSgresultStability(ConstraintFactory constraintFactory) {
//        // A name prefers to teach in a single sgresult.
//        return constraintFactory
//                .fromUniquePair(Sgresult.class,
//                        Joiners.equal(Sgresult::getSgruser))
//                .filter((Sgresult1, Sgresult2) -> {
//                    if (Sgresult1.getDate() == Sgresult2.getDate() && Sgresult1.getName() == Sgresult2.getName()) {
//                        return Sgresult1.getSgruser().getId() != Sgresult2.getSgruser().getId();
//                    }
//                    return false;
//                })
//                .penalize("Name sgresult stability", HardSoftScore.ONE_HARD);
//    }


}
