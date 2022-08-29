package com.gini.scheduling.service;

import com.gini.scheduling.dao.SgrroomRepository;
import com.gini.scheduling.dao.SgruserRepository;
import com.gini.scheduling.dao.SgschRepository;
import com.gini.scheduling.dao.SgsysRepository;
import com.gini.scheduling.model.Scheduling;
import com.gini.scheduling.model.Sgsch;
import com.gini.scheduling.utils.UUIDGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SchedulingService {
    //     There is only one timetable, so there is only timeTableId (= problemId).
//    public static final String SINGLETON_TIME_TABLE_ID = new Sgruser().toString();
    public static final String SINGLETON_TIME_TABLE_ID = UUIDGenerator.generateUUID22();
    @Autowired
    private SgschRepository sgschRepository;
    @Autowired
    private SgruserRepository sgruserRepository;

    @Autowired
    private SgrroomRepository sgrroomRepository;

    @Autowired
    private SgsysRepository sgsysRepository;

    public Scheduling findById(String id) {
        if (!SINGLETON_TIME_TABLE_ID.equals(id)) {
            throw new IllegalStateException("There is no timeTable with id (" + id + ").");
        }
        // Occurs in a single transaction, so each initialized schedule references the same
        // sgruser/sgsch instance
        // that is contained by the timeTable's sgruserList/sgschList.
        return new Scheduling(sgschRepository.findAll(), sgruserRepository.findAll());
    }

    public void save(Scheduling scheduling) {
        for (Sgsch sgsch : scheduling.getSgschList()) {
            // TODO this is awfully naive: optimistic locking causes issues if called by the SolverManager
            sgschRepository.save(sgsch);
        }
    }
}
