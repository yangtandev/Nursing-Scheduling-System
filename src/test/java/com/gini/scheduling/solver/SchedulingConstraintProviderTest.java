///*
// * Copyright 2020 Red Hat, Inc. and/or its affiliates.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.gini.scheduling.solver;
//
//import java.time.DayOfWeek;
//import java.time.LocalTime;
//
//import org.junit.jupiter.api.Test;
//import org.optaplanner.test.api.score.stream.ConstraintVerifier;
//
//import com.gini.scheduling.domain.Staff;
//import com.gini.scheduling.domain.Shift;
//import com.gini.scheduling.domain.Scheduling;
//import com.gini.scheduling.domain.Schedule;
//
//class SchedulingConstraintProviderTest {
//
//    private static final Shift SHIFT1 = new Shift(1, "Shift1");
//    private static final Shift SHIFT2 = new Shift(2, "Shift2");
//    private static final Schedule TIMESLOT1 = new Schedule(1, DayOfWeek.MONDAY, LocalTime.NOON);
//    private static final Schedule TIMESLOT2 = new Schedule(2, DayOfWeek.TUESDAY, LocalTime.NOON);
//    private static final Schedule TIMESLOT3 = new Schedule(3, DayOfWeek.TUESDAY, LocalTime.NOON.plusHours(1));
//    private static final Schedule TIMESLOT4 = new Schedule(4, DayOfWeek.TUESDAY, LocalTime.NOON.plusHours(3));
//
//    private final ConstraintVerifier<SchedulingConstraintProvider, Scheduling> constraintVerifier =
//            ConstraintVerifier.build(new SchedulingConstraintProvider(), Scheduling.class, Staff.class);
//
//    @Test
//    void shiftConflict() {
//        Staff firstStaff = new Staff(1, "CardID1", "Name1", "team1", SHIFT1, TIMESLOT1);
//        Staff conflictingStaff = new Staff(2, "CardID2", "Name2", "team2", SHIFT1, TIMESLOT1);
//        Staff nonConflictingStaff = new Staff(3, "CardID3", "Name3", "team3", SHIFT1, TIMESLOT2);
//        constraintVerifier.verifyThat(SchedulingConstraintProvider::shiftConflict)
//                .given(firstStaff, conflictingStaff, nonConflictingStaff)
//                .penalizesBy(1);
//    }
//
//    @Test
//    void nameConflict() {
//        String conflictingName = "Name1";
//        Staff firstStaff = new Staff(1, "CardID1", conflictingName, "team1", SHIFT1, TIMESLOT1);
//        Staff conflictingStaff = new Staff(2, "CardID2", conflictingName, "team2", SHIFT2, TIMESLOT1);
//        Staff nonConflictingStaff = new Staff(3, "CardID3", "Name2", "team3", SHIFT1, TIMESLOT2);
//        constraintVerifier.verifyThat(SchedulingConstraintProvider::nameConflict)
//                .given(firstStaff, conflictingStaff, nonConflictingStaff)
//                .penalizesBy(1);
//    }
//
//    @Test
//    void teamConflict() {
//        String conflictingteam = "team1";
//        Staff firstStaff = new Staff(1, "CardID1", "Name1", conflictingteam, SHIFT1, TIMESLOT1);
//        Staff conflictingStaff = new Staff(2, "CardID2", "Name2", conflictingteam, SHIFT2, TIMESLOT1);
//        Staff nonConflictingStaff = new Staff(3, "CardID3", "Name3", "team3", SHIFT1, TIMESLOT2);
//        constraintVerifier.verifyThat(SchedulingConstraintProvider::teamConflict)
//                .given(firstStaff, conflictingStaff, nonConflictingStaff)
//                .penalizesBy(1);
//    }
//
//    @Test
//    void nameShiftStability() {
//        String name = "Name1";
//        Staff staffInFirstShift = new Staff(1, "CardID1", name, "team1", SHIFT1, TIMESLOT1);
//        Staff staffInSameShift = new Staff(2, "CardID2", name, "team2", SHIFT1, TIMESLOT1);
//        Staff staffInDifferentShift = new Staff(3, "CardID3", name, "team3", SHIFT2, TIMESLOT1);
//        constraintVerifier.verifyThat(SchedulingConstraintProvider::nameShiftStability)
//                .given(staffInFirstShift, staffInDifferentShift, staffInSameShift)
//                .penalizesBy(2);
//    }
//
//    @Test
//    void nameTimeEfficiency() {
//        String name = "Name1";
//        Staff singleStaffOnMonday = new Staff(1, "CardID1", name, "team1", SHIFT1, TIMESLOT1);
//        Staff firstTuesdayStaff = new Staff(2, "CardID2", name, "team2", SHIFT1, TIMESLOT2);
//        Staff secondTuesdayStaff = new Staff(3, "CardID3", name, "team3", SHIFT1, TIMESLOT3);
//        Staff thirdTuesdayStaffWithGap = new Staff(4, "CardID4", name, "team4", SHIFT1, TIMESLOT4);
//        constraintVerifier.verifyThat(SchedulingConstraintProvider::nameTimeEfficiency)
//                .given(singleStaffOnMonday, firstTuesdayStaff, secondTuesdayStaff, thirdTuesdayStaffWithGap)
//                .rewardsWith(1); // Second tuesday staff immediately follows the first.
//    }
//
//    @Test
//    void teamCardIDVariety() {
//        String team = "team1";
//        String repeatedCardID = "CardID1";
//        Staff mondayStaff = new Staff(1, repeatedCardID, "Name1", team, SHIFT1, TIMESLOT1);
//        Staff firstTuesdayStaff = new Staff(2, repeatedCardID, "Name2", team, SHIFT1, TIMESLOT2);
//        Staff secondTuesdayStaff = new Staff(3, repeatedCardID, "Name3", team, SHIFT1, TIMESLOT3);
//        Staff thirdTuesdayStaffWithDifferentCardID = new Staff(4, "CardID2", "Name4", team, SHIFT1, TIMESLOT4);
//        Staff staffInAnotherteam = new Staff(5, repeatedCardID, "Name5", "team2", SHIFT1, TIMESLOT1);
//        constraintVerifier.verifyThat(SchedulingConstraintProvider::teamCardIDVariety)
//                .given(mondayStaff, firstTuesdayStaff, secondTuesdayStaff, thirdTuesdayStaffWithDifferentCardID,
//                        staffInAnotherteam)
//                .penalizesBy(1); // Second tuesday staff immediately follows the first.
//    }
//
//}
