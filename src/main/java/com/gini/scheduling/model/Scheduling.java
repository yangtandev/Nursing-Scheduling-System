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
    @ValueRangeProvider(id = "sgruserRange")
    @ProblemFactCollectionProperty
    private List<Sgruser> sgruserList;

    @PlanningEntityCollectionProperty
    private List<Sgresult> sgresultList;

    @PlanningScore
    private HardSoftScore score;

    // Ignored by OptaPlanner, used by the UI to display solve or stop solving button
    private SolverStatus solverStatus;

    private Scheduling() {
    }

    public Scheduling(List<Sgresult> sgresultList,
                     List<Sgruser> sgruserList) {
        this.sgresultList = sgresultList;
        this.sgruserList = sgruserList;

    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public List<Sgresult> getSgresultList() {
        return sgresultList;
    }

    public List<Sgruser> getSgruserList() {
        return sgruserList;
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
