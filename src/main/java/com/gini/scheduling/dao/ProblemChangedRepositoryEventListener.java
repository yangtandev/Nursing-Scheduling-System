package com.gini.scheduling.dao;

import com.gini.scheduling.controller.SchedulingController;
import com.gini.scheduling.model.Sgruser;
import com.gini.scheduling.model.Sgshift;
import com.gini.scheduling.model.Sgresult;
import org.optaplanner.core.api.solver.SolverStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

/**
 * This class ensures that during solving, no CRUD operations are allowed.
 */
@Component
@RepositoryEventHandler
public class ProblemChangedRepositoryEventListener {
    @Autowired
    private SchedulingController schedulingController;

    // TODO Future work: Give the CRUD operations "right of way", by calling something like this:
    // before: solverManager.freeze(TIME_TABLE_ID);
    // after: reloadProblem(TIME_TABLE_ID, timeTableRepository::findById);

    @HandleBeforeCreate
    @HandleBeforeSave
    @HandleBeforeDelete
    private void sgruserCreateSaveDelete(Sgruser sgruser) {
        assertNotSolving();
    }
    
    @HandleBeforeCreate
    @HandleBeforeSave
    @HandleBeforeDelete
    private void sgresultCreateSaveDelete(Sgresult sgresult) {
        assertNotSolving();
    }

    public void assertNotSolving() {
        // TODO Race condition: if a timeTableSolverService.solve() call arrives concurrently,
        // the solver might start before the CRUD transaction completes. That's not very harmful, though.
        if (schedulingController.getSolverStatus() != SolverStatus.NOT_SOLVING) {
            throw new IllegalStateException("The solver is solving. Please stop solving first.");
        }
    }
}
