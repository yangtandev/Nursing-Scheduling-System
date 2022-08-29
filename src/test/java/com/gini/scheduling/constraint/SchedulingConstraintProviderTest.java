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
//import com.gini.scheduling.domain.Sgruser;
//import com.gini.scheduling.domain.Sgsch;
//import com.gini.scheduling.domain.Scheduling;
//import com.gini.scheduling.domain.Schedule;
//
//class SchedulingConstraintProviderTest {
//
//    private static final Sgsch SHIFT1 = new Sgsch(1, "Sgsch1");
//    private static final Sgsch SHIFT2 = new Sgsch(2, "Sgsch2");
//    private static final Schedule TIMESLOT1 = new Schedule(1, DayOfWeek.MONDAY, LocalTime.NOON);
//    private static final Schedule TIMESLOT2 = new Schedule(2, DayOfWeek.TUESDAY, LocalTime.NOON);
//    private static final Schedule TIMESLOT3 = new Schedule(3, DayOfWeek.TUESDAY, LocalTime.NOON.plusHours(1));
//    private static final Schedule TIMESLOT4 = new Schedule(4, DayOfWeek.TUESDAY, LocalTime.NOON.plusHours(3));
//
//    private final ConstraintVerifier<SchedulingConstraintProvider, Scheduling> constraintVerifier =
//            ConstraintVerifier.build(new SchedulingConstraintProvider(), Scheduling.class, Sgruser.class);
//
//    @Test
//    void sgschConflict() {
//        Sgruser firstSgruser = new Sgruser(1, "CardID1", "Name1", "uteam1", SHIFT1, TIMESLOT1);
//        Sgruser conflictingSgruser = new Sgruser(2, "CardID2", "Name2", "uteam2", SHIFT1, TIMESLOT1);
//        Sgruser nonConflictingSgruser = new Sgruser(3, "CardID3", "Name3", "uteam3", SHIFT1, TIMESLOT2);
//        constraintVerifier.verifyThat(SchedulingConstraintProvider::sgschConflict)
//                .given(firstSgruser, conflictingSgruser, nonConflictingSgruser)
//                .penalizesBy(1);
//    }
//
//    @Test
//    void nameConflict() {
//        String conflictingName = "Name1";
//        Sgruser firstSgruser = new Sgruser(1, "CardID1", conflictingName, "uteam1", SHIFT1, TIMESLOT1);
//        Sgruser conflictingSgruser = new Sgruser(2, "CardID2", conflictingName, "uteam2", SHIFT2, TIMESLOT1);
//        Sgruser nonConflictingSgruser = new Sgruser(3, "CardID3", "Name2", "uteam3", SHIFT1, TIMESLOT2);
//        constraintVerifier.verifyThat(SchedulingConstraintProvider::nameConflict)
//                .given(firstSgruser, conflictingSgruser, nonConflictingSgruser)
//                .penalizesBy(1);
//    }
//
//    @Test
//    void uteamConflict() {
//        String conflictinguteam = "uteam1";
//        Sgruser firstSgruser = new Sgruser(1, "CardID1", "Name1", conflictinguteam, SHIFT1, TIMESLOT1);
//        Sgruser conflictingSgruser = new Sgruser(2, "CardID2", "Name2", conflictinguteam, SHIFT2, TIMESLOT1);
//        Sgruser nonConflictingSgruser = new Sgruser(3, "CardID3", "Name3", "uteam3", SHIFT1, TIMESLOT2);
//        constraintVerifier.verifyThat(SchedulingConstraintProvider::uteamConflict)
//                .given(firstSgruser, conflictingSgruser, nonConflictingSgruser)
//                .penalizesBy(1);
//    }
//
//    @Test
//    void nameSgschStability() {
//        String name = "Name1";
//        Sgruser sgruserInFirstSgsch = new Sgruser(1, "CardID1", name, "uteam1", SHIFT1, TIMESLOT1);
//        Sgruser sgruserInSameSgsch = new Sgruser(2, "CardID2", name, "uteam2", SHIFT1, TIMESLOT1);
//        Sgruser sgruserInDifferentSgsch = new Sgruser(3, "CardID3", name, "uteam3", SHIFT2, TIMESLOT1);
//        constraintVerifier.verifyThat(SchedulingConstraintProvider::nameSgschStability)
//                .given(sgruserInFirstSgsch, sgruserInDifferentSgsch, sgruserInSameSgsch)
//                .penalizesBy(2);
//    }
//
//    @Test
//    void nameTimeEfficiency() {
//        String name = "Name1";
//        Sgruser singleSgruserOnMonday = new Sgruser(1, "CardID1", name, "uteam1", SHIFT1, TIMESLOT1);
//        Sgruser firstTuesdaySgruser = new Sgruser(2, "CardID2", name, "uteam2", SHIFT1, TIMESLOT2);
//        Sgruser secondTuesdaySgruser = new Sgruser(3, "CardID3", name, "uteam3", SHIFT1, TIMESLOT3);
//        Sgruser thirdTuesdaySgruserWithGap = new Sgruser(4, "CardID4", name, "uteam4", SHIFT1, TIMESLOT4);
//        constraintVerifier.verifyThat(SchedulingConstraintProvider::nameTimeEfficiency)
//                .given(singleSgruserOnMonday, firstTuesdaySgruser, secondTuesdaySgruser, thirdTuesdaySgruserWithGap)
//                .rewardsWith(1); // Second tuesday sgruser immediately follows the first.
//    }
//
//    @Test
//    void uteamCardIDVariety() {
//        String uteam = "uteam1";
//        String repeatedCardID = "CardID1";
//        Sgruser mondaySgruser = new Sgruser(1, repeatedCardID, "Name1", uteam, SHIFT1, TIMESLOT1);
//        Sgruser firstTuesdaySgruser = new Sgruser(2, repeatedCardID, "Name2", uteam, SHIFT1, TIMESLOT2);
//        Sgruser secondTuesdaySgruser = new Sgruser(3, repeatedCardID, "Name3", uteam, SHIFT1, TIMESLOT3);
//        Sgruser thirdTuesdaySgruserWithDifferentCardID = new Sgruser(4, "CardID2", "Name4", uteam, SHIFT1, TIMESLOT4);
//        Sgruser sgruserInAnotheruteam = new Sgruser(5, repeatedCardID, "Name5", "uteam2", SHIFT1, TIMESLOT1);
//        constraintVerifier.verifyThat(SchedulingConstraintProvider::uteamCardIDVariety)
//                .given(mondaySgruser, firstTuesdaySgruser, secondTuesdaySgruser, thirdTuesdaySgruserWithDifferentCardID,
//                        sgruserInAnotheruteam)
//                .penalizesBy(1); // Second tuesday sgruser immediately follows the first.
//    }
//
//}
