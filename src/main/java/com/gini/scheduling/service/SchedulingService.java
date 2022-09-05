package com.gini.scheduling.service;

import com.gini.scheduling.dao.*;
import com.gini.scheduling.model.Scheduling;
import com.gini.scheduling.model.Sgresult;
import com.gini.scheduling.utils.UUIDGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SchedulingService {
    public static final String SINGLETON_TIME_TABLE_ID = UUIDGenerator.generateUUID22();
    @Autowired
    private SgresultRepository sgresultRepository;
    @Autowired
    private SgshiftRepository sgshiftRepository;
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
        // sgruser/sgresult instance
        // that is contained by the timeTable's sgruserList/sgresultList
        return new Scheduling(sgresultRepository.findAll(), sgshiftRepository.findAll());
    }

    public void save(Scheduling scheduling) {
        for (Sgresult sgresult : scheduling.getSgresultList()) {
            // TODO this is awfully naive: optimistic locking causes issues if called by the SolverManager
            sgresultRepository.save(sgresult);
        }
    }
}
