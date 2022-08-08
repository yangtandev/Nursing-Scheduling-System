/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gini.scheduling.solver;

import java.time.Duration;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;

import com.gini.scheduling.domain.Staff;

public class TimeTableConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // Hard constraints
                shiftConflict(constraintFactory),
                nameConflict(constraintFactory),
                staffGroupConflict(constraintFactory),
                // Soft constraints
                nameShiftStability(constraintFactory),
                nameTimeEfficiency(constraintFactory),
                staffGroupCardIDVariety(constraintFactory)
        };
    }

    Constraint shiftConflict(ConstraintFactory constraintFactory) {
        // A shift can accommodate at most one staff at the same time.
        return constraintFactory
                // Select each pair of 2 different staffs ...
                .fromUniquePair(Staff.class,
                        // ... in the same timeslot ...
                        Joiners.equal(Staff::getTimeslot),
                        // ... in the same shift ...
                        Joiners.equal(Staff::getShift))
                // ... and penalize each pair with a hard weight.
                .penalize("Shift conflict", HardSoftScore.ONE_HARD);
    }

    Constraint nameConflict(ConstraintFactory constraintFactory) {
        // A name can teach at most one staff at the same time.
        return constraintFactory
                .fromUniquePair(Staff.class,
                        Joiners.equal(Staff::getTimeslot),
                        Joiners.equal(Staff::getName))
                .penalize("Name conflict", HardSoftScore.ONE_HARD);
    }

    Constraint staffGroupConflict(ConstraintFactory constraintFactory) {
        // A student can attend at most one staff at the same time.
        return constraintFactory
                .fromUniquePair(Staff.class,
                        Joiners.equal(Staff::getTimeslot),
                        Joiners.equal(Staff::getStaffGroup))
                .penalize("StaffGroup conflict", HardSoftScore.ONE_HARD);
    }

    Constraint nameShiftStability(ConstraintFactory constraintFactory) {
        // A name prefers to teach in a single shift.
        return constraintFactory
                .fromUniquePair(Staff.class,
                        Joiners.equal(Staff::getName))
                .filter((staff1, staff2) -> staff1.getShift() != staff2.getShift())
                .penalize("Name shift stability", HardSoftScore.ONE_SOFT);
    }

    Constraint nameTimeEfficiency(ConstraintFactory constraintFactory) {
        // A name prefers to teach sequential staffs and dislikes gaps between staffs.
        return constraintFactory
                .from(Staff.class)
                .join(Staff.class, Joiners.equal(Staff::getName),
                        Joiners.equal((staff) -> staff.getTimeslot().getDayOfWeek()))
                .filter((staff1, staff2) -> {
                    Duration between = Duration.between(staff1.getTimeslot().getEndTime(),
                            staff2.getTimeslot().getStartTime());
                    return !between.isNegative() && between.compareTo(Duration.ofMinutes(30)) <= 0;
                })
                .reward("Name time efficiency", HardSoftScore.ONE_SOFT);
    }

    Constraint staffGroupCardIDVariety(ConstraintFactory constraintFactory) {
        // A staffGroup dislikes sequential staffs on the same cardID.
        return constraintFactory
                .from(Staff.class)
                .join(Staff.class,
                        Joiners.equal(Staff::getCardID),
                        Joiners.equal(Staff::getStaffGroup),
                        Joiners.equal((staff) -> staff.getTimeslot().getDayOfWeek()))
                .filter((staff1, staff2) -> {
                    Duration between = Duration.between(staff1.getTimeslot().getEndTime(),
                            staff2.getTimeslot().getStartTime());
                    return !between.isNegative() && between.compareTo(Duration.ofMinutes(30)) <= 0;
                })
                .penalize("StaffGroup cardID variety", HardSoftScore.ONE_SOFT);
    }

}
