package com.gini.scheduling.persistence;

import com.gini.scheduling.domain.Shift;
import com.gini.scheduling.domain.TimeTable;
import com.gini.scheduling.utils.UUIDGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TimeTableRepository {
    // There is only one timetable, so there is only timeTableId (= problemId).
    public static final String SINGLETON_TIME_TABLE_ID = UUIDGenerator.generateUUID22();

    @Autowired
    private ShiftRepository shiftRepository;
    @Autowired
    private StaffRepository staffRepository;

    public TimeTable findById(String id) {
        if (!SINGLETON_TIME_TABLE_ID.equals(id)) {
            throw new IllegalStateException("There is no timeTable with id (" + id + ").");
        }
        // Occurs in a single transaction, so each initialized schedule references the same
        // staff/shift instance
        // that is contained by the timeTable's staffList/shiftList.
        return new TimeTable(shiftRepository.findAll(), staffRepository.findAll());
    }

    public void save(TimeTable timeTable) {
        for (Shift shift : timeTable.getShiftList()) {
            // TODO this is awfully naive: optimistic locking causes issues if called by the SolverManager
            shiftRepository.save(shift);
        }
    }
}
