package com.gini.scheduling.model;

import java.util.List;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.solver.SolverStatus;

@PlanningSolution
public class Scheduling {
    @ValueRangeProvider(id = "shiftRange")
    @ProblemFactCollectionProperty
    private List<Sgshift> sgshiftList;

    @PlanningEntityCollectionProperty
    private List<Sgresult> sgresultList;

    @PlanningScore
    private HardSoftScore score;

    // Ignored by OptaPlanner, used by the UI to display solve or stop solving button
    private SolverStatus solverStatus;

    public Scheduling() {
    }

    public Scheduling(List<Sgresult> sgresultList, List<Sgshift> sgshiftList) {
        this.sgresultList = sgresultList;
        this.sgshiftList = sgshiftList;
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public List<Sgresult> getSgresultList() {
        return sgresultList;
    }

    public List<Sgshift> getSgshiftList() {
        return sgshiftList;
    }


    public void setScore(HardSoftScore score) {
        this.score = score;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public SolverStatus getSolverStatus() {
        return solverStatus;
    }

    public void setSolverStatus(SolverStatus solverStatus) {
        this.solverStatus = solverStatus;
    }


}
