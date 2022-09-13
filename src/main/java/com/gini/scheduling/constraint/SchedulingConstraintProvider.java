package com.gini.scheduling.constraint;

import com.gini.scheduling.controller.SgrroomController;
import com.gini.scheduling.dao.SgruserRepository;
import com.gini.scheduling.dao.SgsysRepository;
import com.gini.scheduling.model.Sgresult;
import com.gini.scheduling.model.Sgruser;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.stream.Collectors;

import static org.optaplanner.core.api.score.stream.Joiners.equal;

public class SchedulingConstraintProvider implements ConstraintProvider {
    @Autowired
    private SgruserRepository sgruserRepository;
    @Autowired
    private SgsysRepository sgsysRepository;

    public static final Logger logger = LoggerFactory.getLogger(SgrroomController.class);

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
//                 Hard constraints
//                sgresultConflict(constraintFactory),
//                OFFConflict(constraintFactory),
//                uteamConflict(constraintFactory),

//                Soft constraints
//                nameSgresultStability(constraintFactory),
        };
    }

//    Constraint OffConstraint(ConstraintFactory constraintFactory) {
//        return constraintFactory
//                .from(Shift.class)
//                .filter(shift -> shift.getEmployee().getName().equals("Ann"))
//                .penalize("Don't assign Ann", HardSoftScore.ONE_SOFT);
//    }

    //    Constraint sgresultConflict(ConstraintFactory constraintFactory) {
//        return constraintFactory
//                .from(Sgresult.class)
//                .groupBy(Sgresult::getSgshift, countDistinct())
//                .filter((clsno, count) -> {
//                    int r55RoomOpen = 12;
//                    int r55NeedManpower = 2;
//                    List<Sgsys> sgsysList = sgsysRepository.findAll();
//                    for (Sgsys sgsys : sgsysList) {
//                        if (sgsys.getSkey().equals("r55RoomOpen")) {
//                            r55RoomOpen = Integer.parseInt(sgsys.getVal());
//                        } else if (sgsys.getSkey().equals("r55NeedManpower")) {
//                            r55NeedManpower = Integer.parseInt(sgsys.getVal());
//                        }
//                    }
//
//                    if (
//                            clsno.equals("55") && count != r55RoomOpen * r55NeedManpower ||
//                                    clsno.equals("D6") || clsno.equals("A0") && count != 2
//                    ) {
//                        return true;
//                    }
//                    return false;
//                })
//                .penalize("UNO conflict", HardSoftScore.ONE_HARD);
//    }
//    Constraint breakBetweenNonConsecutiveShiftsIsAtLeastTenHours(ConstraintFactory constraintFactory) {
//        return constraintFactory
//                .from(Shift.class)
//                .join(Shift.class,
//                        equal(Shift::getEmployee),
//                        lessThan(Shift::getEndDateTime, Shift::getStartDateTime))
//                .filter((s1, s2) -> !Objects.equals(s1, s2))
//                .filter((s1, s2) -> s1.getEndDateTime().until(s2.getStartDateTime(), ChronoUnit.HOURS) < 10)
//                .penalizeConfigurableLong(CONSTRAINT_BREAK_BETWEEN_NON_CONSECUTIVE_SHIFTS, (s1, s2) -> {
//                    long breakLength = s1.getEndDateTime().until(s2.getStartDateTime(), ChronoUnit.MINUTES);
//                    return (10 * 60) - breakLength;
//                });
//    }
//    Constraint OFFConflict(ConstraintFactory constraintFactory) {
//        return constraintFactory
//                .from(Sgresult.class)
//                .join(Sgresult.class,
//                        equal(Sgresult::getSchweek),
//                        equal(Sgresult::getUno))
//                .filter((Sgresult1, Sgresult2) -> {
//                    if (Sgresult1.getSgruser().getId() == Sgresult2.getSgruser().getId()) {
//                        return false;
//                    }
//                    return true;
//                })
//                .penalize("Name conflict", HardSoftScore.ONE_HARD);
//    }
//
//    Constraint uteamConflict(ConstraintFactory constraintFactory) {
//        return constraintFactory
//                .fromUniquePair(Sgresult.class,
//                        equal(Sgresult::getSgshift),
//                        equal(Sgresult::getSchdate))
//                .filter((Sgresult1, Sgresult2) -> {
//                    List<Sgruser> sgruserList = sgruserRepository.findAll();
//                    String clsno = Sgresult1.getSgshift().getClsno();
//                    String clsno2 = Sgresult1.getSgshift().getClsno();
//                    logger.info(clsno+" "+clsno2);
//                    LocalDate schdate = Sgresult1.getSchdate();
//                    DayOfWeek day = DayOfWeek.of(schdate.get(ChronoField.DAY_OF_WEEK));
//                    Boolean isWeekend = day == DayOfWeek.SUNDAY || day == DayOfWeek.SATURDAY;
//                    if (
//                            clsno.equals("D6") ||
//                                    clsno.equals("A0") ||
//                                    (clsno.equals("55") && isWeekend)
//                    ) {
//                        String team1 = sgruserList
//                                .stream()
//                                .filter(sgruser -> sgruser.getUno().equals(Sgresult1.getUno()))
//                                .map(sgruser -> sgruser.getUteam())
//                                .collect(Collectors.toList())
//                                .get(0);
//                        String team2 = sgruserList
//                                .stream()
//                                .filter(sgruser -> sgruser.getUno().equals(Sgresult2.getUno()))
//                                .map(sgruser -> sgruser.getUteam())
//                                .collect(Collectors.toList())
//                                .get(0);
//                        if (team1.equals(team2)) {
//                            return true;
//                        }
//                    }
//                    return false;
//                })
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
