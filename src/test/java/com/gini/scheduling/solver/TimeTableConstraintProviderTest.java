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

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.optaplanner.test.api.score.stream.ConstraintVerifier;

import com.gini.scheduling.domain.Staff;
import com.gini.scheduling.domain.Shift;
import com.gini.scheduling.domain.TimeTable;
import com.gini.scheduling.domain.Timeslot;

class TimeTableConstraintProviderTest {

    private static final Shift SHIFT1 = new Shift(1, "Shift1");
    private static final Shift SHIFT2 = new Shift(2, "Shift2");
    private static final Timeslot TIMESLOT1 = new Timeslot(1, DayOfWeek.MONDAY, LocalTime.NOON);
    private static final Timeslot TIMESLOT2 = new Timeslot(2, DayOfWeek.TUESDAY, LocalTime.NOON);
    private static final Timeslot TIMESLOT3 = new Timeslot(3, DayOfWeek.TUESDAY, LocalTime.NOON.plusHours(1));
    private static final Timeslot TIMESLOT4 = new Timeslot(4, DayOfWeek.TUESDAY, LocalTime.NOON.plusHours(3));

    private final ConstraintVerifier<TimeTableConstraintProvider, TimeTable> constraintVerifier =
            ConstraintVerifier.build(new TimeTableConstraintProvider(), TimeTable.class, Staff.class);

    @Test
    void shiftConflict() {
        Staff firstStaff = new Staff(1, "CardID1", "Name1", "staffGroup1", SHIFT1, TIMESLOT1);
        Staff conflictingStaff = new Staff(2, "CardID2", "Name2", "staffGroup2", SHIFT1, TIMESLOT1);
        Staff nonConflictingStaff = new Staff(3, "CardID3", "Name3", "staffGroup3", SHIFT1, TIMESLOT2);
        constraintVerifier.verifyThat(TimeTableConstraintProvider::shiftConflict)
                .given(firstStaff, conflictingStaff, nonConflictingStaff)
                .penalizesBy(1);
    }

    @Test
    void nameConflict() {
        String conflictingName = "Name1";
        Staff firstStaff = new Staff(1, "CardID1", conflictingName, "staffGroup1", SHIFT1, TIMESLOT1);
        Staff conflictingStaff = new Staff(2, "CardID2", conflictingName, "staffGroup2", SHIFT2, TIMESLOT1);
        Staff nonConflictingStaff = new Staff(3, "CardID3", "Name2", "staffGroup3", SHIFT1, TIMESLOT2);
        constraintVerifier.verifyThat(TimeTableConstraintProvider::nameConflict)
                .given(firstStaff, conflictingStaff, nonConflictingStaff)
                .penalizesBy(1);
    }

    @Test
    void staffGroupConflict() {
        String conflictingstaffGroup = "staffGroup1";
        Staff firstStaff = new Staff(1, "CardID1", "Name1", conflictingstaffGroup, SHIFT1, TIMESLOT1);
        Staff conflictingStaff = new Staff(2, "CardID2", "Name2", conflictingstaffGroup, SHIFT2, TIMESLOT1);
        Staff nonConflictingStaff = new Staff(3, "CardID3", "Name3", "staffGroup3", SHIFT1, TIMESLOT2);
        constraintVerifier.verifyThat(TimeTableConstraintProvider::staffGroupConflict)
                .given(firstStaff, conflictingStaff, nonConflictingStaff)
                .penalizesBy(1);
    }

    @Test
    void nameShiftStability() {
        String name = "Name1";
        Staff staffInFirstShift = new Staff(1, "CardID1", name, "staffGroup1", SHIFT1, TIMESLOT1);
        Staff staffInSameShift = new Staff(2, "CardID2", name, "staffGroup2", SHIFT1, TIMESLOT1);
        Staff staffInDifferentShift = new Staff(3, "CardID3", name, "staffGroup3", SHIFT2, TIMESLOT1);
        constraintVerifier.verifyThat(TimeTableConstraintProvider::nameShiftStability)
                .given(staffInFirstShift, staffInDifferentShift, staffInSameShift)
                .penalizesBy(2);
    }

    @Test
    void nameTimeEfficiency() {
        String name = "Name1";
        Staff singleStaffOnMonday = new Staff(1, "CardID1", name, "staffGroup1", SHIFT1, TIMESLOT1);
        Staff firstTuesdayStaff = new Staff(2, "CardID2", name, "staffGroup2", SHIFT1, TIMESLOT2);
        Staff secondTuesdayStaff = new Staff(3, "CardID3", name, "staffGroup3", SHIFT1, TIMESLOT3);
        Staff thirdTuesdayStaffWithGap = new Staff(4, "CardID4", name, "staffGroup4", SHIFT1, TIMESLOT4);
        constraintVerifier.verifyThat(TimeTableConstraintProvider::nameTimeEfficiency)
                .given(singleStaffOnMonday, firstTuesdayStaff, secondTuesdayStaff, thirdTuesdayStaffWithGap)
                .rewardsWith(1); // Second tuesday staff immediately follows the first.
    }

    @Test
    void staffGroupCardIDVariety() {
        String staffGroup = "staffGroup1";
        String repeatedCardID = "CardID1";
        Staff mondayStaff = new Staff(1, repeatedCardID, "Name1", staffGroup, SHIFT1, TIMESLOT1);
        Staff firstTuesdayStaff = new Staff(2, repeatedCardID, "Name2", staffGroup, SHIFT1, TIMESLOT2);
        Staff secondTuesdayStaff = new Staff(3, repeatedCardID, "Name3", staffGroup, SHIFT1, TIMESLOT3);
        Staff thirdTuesdayStaffWithDifferentCardID = new Staff(4, "CardID2", "Name4", staffGroup, SHIFT1, TIMESLOT4);
        Staff staffInAnotherstaffGroup = new Staff(5, repeatedCardID, "Name5", "staffGroup2", SHIFT1, TIMESLOT1);
        constraintVerifier.verifyThat(TimeTableConstraintProvider::staffGroupCardIDVariety)
                .given(mondayStaff, firstTuesdayStaff, secondTuesdayStaff, thirdTuesdayStaffWithDifferentCardID,
                        staffInAnotherstaffGroup)
                .penalizesBy(1); // Second tuesday staff immediately follows the first.
    }

}
