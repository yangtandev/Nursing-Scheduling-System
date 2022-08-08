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

package com.gini.scheduling.persistence;

import com.gini.scheduling.domain.Staff;
import com.gini.scheduling.domain.TimeTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TimeTableRepository {

    // There is only one time table, so there is only timeTableId (= problemId).
    public static final Long SINGLETON_TIME_TABLE_ID = 1L;

    @Autowired
    private TimeslotRepository timeslotRepository;
    @Autowired
    private ShiftRepository shiftRepository;
    @Autowired
    private StaffRepository staffRepository;

    public TimeTable findById(Long id) {
        if (!SINGLETON_TIME_TABLE_ID.equals(id)) {
            throw new IllegalStateException("There is no timeTable with id (" + id + ").");
        }
        // Occurs in a single transaction, so each initialized staff references the same timeslot/shift instance
        // that is contained by the timeTable's timeslotList/shiftList.
        return new TimeTable(
                timeslotRepository.findAll(),
                shiftRepository.findAll(),
                staffRepository.findAll());
    }

    public void save(TimeTable timeTable) {
        for (Staff staff : timeTable.getStaffList()) {
            // TODO this is awfully naive: optimistic locking causes issues if called by the SolverManager
            staffRepository.save(staff);
        }
    }

}
