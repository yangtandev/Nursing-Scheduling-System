package com.gini.scheduling.constraint;

import com.gini.scheduling.controller.SgrroomController;
import com.gini.scheduling.model.Sgbackup;
import com.gini.scheduling.model.Sgresult;
import com.gini.scheduling.utils.VacationDayCalculate;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoField;
import java.time.temporal.WeekFields;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import static org.optaplanner.core.api.score.stream.ConstraintCollectors.count;
import static org.optaplanner.core.api.score.stream.ConstraintCollectors.countBi;
import static org.optaplanner.core.api.score.stream.Joiners.equal;
import static org.optaplanner.core.api.score.stream.Joiners.filtering;

public class SchedulingConstraintProvider implements ConstraintProvider {
    public static final Logger logger = LoggerFactory.getLogger(SgrroomController.class);

//	private int level1 = 10;
//	private int level2 = level1 * level1;
//	private int level3 = level1 * level1 * level1;

//    private final int level5 = 100;
//    private final int level4 = 50;
//    private final int level3 = 20;
//    private final int level2 = 10;
//    private final int level1 = 1;

    private final int level5 = 1;
    private final int level4 = 1;
    private final int level3 = 1;
    private final int level2 = 1;
    private final int level1 = 1;

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
//            UnoAndUteamConstraint(constraintFactory),
////            daysOffInTheWeekConstraint(constraintFactory),
//            daysOffInTheMonthConstraint(constraintFactory),
//            atLeastOneFullWeekendConstraint(constraintFactory),
//            atLeast15daysPerMonthConstraint(constraintFactory),
//            clsnoConstraint(constraintFactory),
////            fourDaysPerCourseConstraint(constraintFactory),
        };
    }

    Constraint UnoAndUteamConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
            .from(Sgresult.class)
            .join(Sgresult.class, equal(Sgresult::getSchdate),
                filtering((Sgresult1, Sgresult2) -> {
                    Boolean bothUnoAreTheSame = Sgresult1.getUno().equals(Sgresult2.getUno());
                    if (bothUnoAreTheSame) {
                        Boolean bothSchuuidAreDifferent = !Sgresult1.getSchuuid().equals(Sgresult2.getSchuuid());
                        return bothSchuuidAreDifferent;
                    } else {
                        Boolean bothClsnoAreTheSame = Sgresult1.getClsno().equals(Sgresult2.getClsno());
                        Boolean bothUteamAreTheSame = Sgresult1.getUteam().equals(Sgresult2.getUteam());
                        if (bothClsnoAreTheSame && bothUteamAreTheSame) {
                            Boolean bothAreShiftWork = Sgresult1.isShiftWork();
                            if (bothAreShiftWork) return true;
                            Boolean bothClsnoAreA8 = Sgresult1.getClsno().equals("A8");
                            Boolean bothUteamAreTeamC = Sgresult1.getUteam().equals("C");
                            if (bothClsnoAreA8 && bothUteamAreTeamC) return true;
                        }
                        return false;
                    }
                }))
            .penalize("Uno and uteam constraint", HardSoftScore.ofHard(level3));
    }

    Constraint daysOffInTheWeekConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
            .from(Sgresult.class)
            .filter(Sgresult -> Sgresult.getClsno().equals("55")
                ||Sgresult.getClsno().equals("D6")
                ||Sgresult.getClsno().equals("A0")
                ||Sgresult.getClsno().equals("A8")
            )
            .groupBy(Sgresult->Sgresult.getSchweek()+"-"+Sgresult.getSchdate().getMonthValue(), Sgresult::getUno, count())
            .filter((schweek, uno, workdays) -> {
                String[] parts = schweek.split("-");
                int year = Integer.valueOf("20"+String.valueOf(parts[0]).substring(0,2));
                int week = Integer.valueOf(String.valueOf(parts[0]).substring(2));
                int month = Integer.valueOf(parts[1]);
                WeekFields weekFields= WeekFields.ISO;
                LocalDate now = LocalDate.now();
                LocalDate localDate = now.withYear(year).with(weekFields.weekOfYear(),week);
                LocalDate monday  = localDate.with(weekFields.dayOfWeek(), 1L);
                LocalDate sunday  = localDate.with(weekFields.dayOfWeek(), 7L);
                int totalDays = 0;
                for (LocalDate currentSchdate = monday; currentSchdate.isBefore(sunday.plusDays(1)); currentSchdate = currentSchdate.plusDays(1)) {
                    if(currentSchdate.getMonthValue() == month) totalDays++;
                }
                int totalDayOff = totalDays - workdays;
                return totalDayOff > 2 || totalDayOff == 0;
            })
            .penalize("Days off in the week constraint", HardSoftScore.ofHard(level3));
    }

    Constraint daysOffInTheMonthConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
            .from(Sgresult.class)
            .filter(Sgresult -> Sgresult.getClsno().equals("55")
                ||Sgresult.getClsno().equals("D6")
                ||Sgresult.getClsno().equals("A0")
                ||Sgresult.getClsno().equals("A8")
                ||Sgresult.getClsno().equals("公休")
            )
            .groupBy(Sgresult -> String.format("%d%d", Sgresult.getSchdate().getYear(),
                Sgresult.getSchdate().getMonthValue()), Sgresult::getUno, count())
            .filter((yearMonth, uno, workdays) -> {
                Integer year = Integer.valueOf(yearMonth.substring(0, 4));
                String month = Integer.valueOf(yearMonth.substring(4)) >= 10 ? yearMonth.substring(4)
                    : "0" + yearMonth.substring(4);
                HashMap<String, Boolean> map = new VacationDayCalculate().yearVacationDay(year);
                Set<String> keySet = map.keySet();
                int estimateDaysOff = keySet
                    .stream()
                    .filter(key -> map.get(key) && key.startsWith(month))
                    .collect(Collectors.toList())
                    .size();
                YearMonth yearMonthObject = YearMonth.of(year, Integer.valueOf(month));
                int totalDays = yearMonthObject.lengthOfMonth();
                int actualDaysOff = totalDays - workdays;
                return actualDaysOff != estimateDaysOff;
            })
            .penalize("Days off in the month constraint", HardSoftScore.ofHard(level3));
    }

    Constraint atLeastOneFullWeekendConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
            .from(Sgresult.class)
            .join(Sgresult.class, equal(Sgresult::getUno), equal(Sgresult::getSchweek), filtering((Sgresult1, Sgresult2) -> {
                boolean bothSchdateAreCorrect =
                    (DayOfWeek.of(Sgresult1.getSchdate().get(ChronoField.DAY_OF_WEEK)) == DayOfWeek.SATURDAY && DayOfWeek.of(Sgresult2.getSchdate().get(ChronoField.DAY_OF_WEEK)) == DayOfWeek.SUNDAY)
                || (DayOfWeek.of(Sgresult1.getSchdate().get(ChronoField.DAY_OF_WEEK)) == DayOfWeek.SUNDAY && DayOfWeek.of(Sgresult2.getSchdate().get(ChronoField.DAY_OF_WEEK)) == DayOfWeek.SATURDAY);
                boolean BothClsnoAreNotDayOff = !(Sgresult1.getClsno().equals("OFF") && Sgresult2.getClsno().equals("OFF"));
                return bothSchdateAreCorrect && BothClsnoAreNotDayOff;
            }))
            .groupBy(
                (Sgresult1, Sgresult2) -> String.format("%d%d", Sgresult1.getSchdate().getYear(),
                    Sgresult1.getSchdate().getMonthValue()),
                (Sgresult1, Sgresult2) -> Sgresult1.getUno(), countBi())
            .filter((yearMonth, uno, numOfIncompleteWeekends) -> {
                int year = Integer.valueOf(yearMonth.substring(0, 4));
                int month = Integer.valueOf(yearMonth.substring(4));
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month - 1, 1);
                int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                int numOfWeekends = 0;
                for (int i = 1; i <= daysInMonth; i++) {
                    calendar.set(year, month - 1, i);
                    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                    if (dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY) {
                        numOfWeekends++;
                    }
                }
                int numOfCompleteWeekends = (numOfWeekends - numOfWeekends % 2) / 2;
                return numOfCompleteWeekends == numOfIncompleteWeekends;
            })
            .penalize("At least one full Weekend constraint", HardSoftScore.ofHard(level4));
    }

    Constraint atLeast15daysPerMonthConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
            .from(Sgresult.class)
            .join(Sgbackup.class,
                equal(Sgresult -> Sgresult.getSchdate().getYear(), Sgbackup -> Sgbackup.getSchdate().getYear()),
                equal(Sgresult -> Sgresult.getSchdate().getMonthValue(), Sgbackup -> Sgbackup.getSchdate().getMonthValue()),
                equal(Sgresult::getUno, Sgbackup::getUno))
            .filter((Sgresult, Sgbackup) -> {
                Boolean BothClsnoAreA0 = (Sgresult.getClsno().equals("A0") && Sgbackup.getClsno().equals("A0"))
               || (Sgresult.getClsno().equals("D6") && Sgbackup.getClsno().equals("D6"));
                return BothClsnoAreA0;
            })
            .groupBy(
                (Sgresult, Sgbackup) -> Sgresult.getSchdate().getYear() + Sgresult.getSchdate().getMonthValue(),
                (Sgresult, Sgbackup) -> Sgresult.getClsno()+Sgresult.getUno(),
                countBi()
            )
            .filter((yearMonth, clsnoUno, count) -> {
                    return count < 15;
            })
            .penalize("At least 15 days per month constraint", HardSoftScore.ofHard(level3));
    }

    Constraint clsnoConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
            .from(Sgresult.class)
            .join(Sgresult.class)
            .join(Sgbackup.class)
            .filter((Sgresult1, Sgresult2, Sgbackup) -> {
                Boolean allYearAreTheSame = Sgresult1.getSchdate().getYear() == Sgresult2.getSchdate().getYear()
                    && Sgresult2.getSchdate().getYear() == Sgbackup.getSchdate().getYear();
                Boolean allMonthAreTheSame = Sgresult1.getSchdate().getMonthValue() == Sgresult2.getSchdate().getMonthValue()
                    && Sgresult2.getSchdate().getMonthValue() == Sgbackup.getSchdate().getMonthValue();
                Boolean allUnoAreTheSame = Sgresult1.getUno().equals(Sgresult2.getUno())
                    && Sgresult2.getUno().equals(Sgbackup.getUno());
                if (allYearAreTheSame && allMonthAreTheSame && allUnoAreTheSame) {
                    DayOfWeek weekendDay = DayOfWeek.of(Sgresult1.getSchdate().get(ChronoField.DAY_OF_WEEK));
                    Boolean weekendIsSaturday = weekendDay == DayOfWeek.SATURDAY;
                    Boolean weekendIsSunday = weekendDay == DayOfWeek.SUNDAY;
                    Boolean weekendIs55 = Sgresult1.getClsno().equals("55");
                    Boolean weekdayIsMonday = Sgresult2.getSchdate().equals(Sgresult1.getSchdate().plusDays(2));
                    Boolean weekdayIsFriday = Sgresult2.getSchdate().equals(Sgresult1.getSchdate().minusDays(2));
                    Boolean weekdayIsNotDayOff = !Sgresult2.getClsno().equals("OFF");
                    if (weekendIsSaturday && weekendIs55) return weekdayIsMonday && weekdayIsNotDayOff;
                    if (weekendIsSunday && weekendIs55) return weekdayIsFriday && weekdayIsNotDayOff;
                    Boolean isDaily = Sgbackup.getClsno().equals("daily");
                    if (isDaily) {
                        return Sgresult1.getClsno().equals("D6")
                            || Sgresult1.getClsno().equals("A0")
                            || Sgresult1.getClsno().equals("A8")
                            || Sgresult2.getClsno().equals("D6")
                            || Sgresult2.getClsno().equals("A0")
                            || Sgresult2.getClsno().equals("A8");
                    }
                    Boolean bothSchdateAreCorrect = Sgresult1.getSchdate().equals(Sgresult2.getSchdate().minusDays(1));
                    if (bothSchdateAreCorrect) {
                        Boolean bothClsnoAreDifferent = Sgbackup.getClsno().equals("公休")
                            && (!Sgresult1.getClsno().equals("公休") && Sgresult1.getSchdate().equals(Sgbackup.getSchdate()))
                            || (!Sgresult2.getClsno().equals("公休") && Sgresult2.getSchdate().equals(Sgbackup.getSchdate()));
                        if (bothClsnoAreDifferent) return true;
                        Boolean is55 = Sgresult1.getClsno().equals("55");
                        if (is55) {
                            return Sgresult2.getClsno().equals("A0");
                        }
                        Boolean isD6 = Sgresult1.getClsno().equals("D6");
                        if (isD6) {
                            return Sgresult2.getClsno().equals("55")
                                || Sgresult2.getClsno().equals("A0")
                                || Sgresult2.getClsno().equals("A8");
                        }
                        Boolean isA8 = Sgresult1.getClsno().equals("A8");
                        if (isA8) {
                            return Sgresult2.getClsno().equals("55")
                                || Sgresult2.getClsno().equals("A0");
                        }
                    }
                }
                return false;
            })
            .penalize("Clsno constraint", HardSoftScore.ofHard(level5));
    }

    Constraint fourDaysPerCourseConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
            .from(Sgresult.class)
            .join(Sgresult.class)
            .join(Sgresult.class)
            .join(Sgresult.class)
            .filter((Sgresult1, Sgresult2, Sgresult3, Sgresult4) -> {
                logger.info("Sgresult1 {} {} {}", Sgresult1.getSchdate(), Sgresult1.getUno(), Sgresult1.getClsno());
                logger.info("Sgresult2 {} {} {}", Sgresult2.getSchdate(), Sgresult2.getUno(), Sgresult2.getClsno());
                logger.info("Sgresult3 {} {} {}", Sgresult3.getSchdate(), Sgresult3.getUno(), Sgresult3.getClsno());
                logger.info("Sgresult4 {} {} {}", Sgresult4.getSchdate(), Sgresult4.getUno(), Sgresult4.getClsno());
                return false;
//                LocalDate currentDate = Sgresult1.getSchdate();
//                int currentMonth = currentDate.getMonthValue();
//                String currentUno = Sgresult1.getUno();
//                Boolean allInTheSameMonth = Sgresult2.getSchdate().getMonthValue() == currentMonth
//                    && Sgresult3.getSchdate().getMonthValue() == currentMonth
//                    && Sgresult4.getSchdate().getMonthValue() == currentMonth;
//                Boolean allUnoAreTheSame = Sgresult2.getUno().equals(currentUno)
//                    && Sgresult3.getUno().equals(currentUno)
//                    && Sgresult4.getUno().equals(currentUno);
//                Boolean allSchdateAreCorrect = Sgresult2.getSchdate().minusDays(1).equals(currentDate)
//                    && Sgresult3.getSchdate().minusDays(2).equals(currentDate)
//                    && Sgresult4.getSchdate().minusDays(3).equals(currentDate);
//                Boolean allClsnoAreA0 = Sgresult1.getClsno().equals("A0")
//                    && Sgresult2.getClsno().equals("A0")
//                    && Sgresult3.getClsno().equals("A0")
//                    && Sgresult4.getClsno().equals("A0");
//                if(allInTheSameMonth && allUnoAreTheSame && allSchdateAreCorrect && allClsnoAreA0){
//                    logger.info("Sgresult1 {} {} {}",Sgresult1.getSchdate(),Sgresult1.getUno(),Sgresult1.getClsno());
//                    logger.info("Sgresult2 {} {} {}",Sgresult2.getSchdate(),Sgresult2.getUno(),Sgresult2.getClsno());
//                    logger.info("Sgresult3 {} {} {}",Sgresult3.getSchdate(),Sgresult3.getUno(),Sgresult3.getClsno());
//                    logger.info("Sgresult4 {} {} {}",Sgresult4.getSchdate(),Sgresult4.getUno(),Sgresult4.getClsno());
//                }
//                return !(allInTheSameMonth && allUnoAreTheSame && allSchdateAreCorrect && allClsnoAreA0);
            })
            .penalize("Four days per course constraint", HardSoftScore.ofHard(level1));
    }
}
