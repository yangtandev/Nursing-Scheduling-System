package com.gini.scheduling.model;

import java.util.List;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.solver.SolverStatus;
import org.springframework.stereotype.Service;

@Service
@PlanningSolution
public class Scheduling {
    @ValueRangeProvider(id = "sgruserRange")
	@ProblemFactCollectionProperty
    private List<Sgruser> sgruserList;

    @ProblemFactCollectionProperty
    private List<String> sgshiftList;
    
    @ProblemFactCollectionProperty
    private List<Sgbackup> sgbackupList;
    
    @PlanningEntityCollectionProperty
    private List<Sgresult> sgresultList;

    @PlanningScore
    private HardSoftScore score;

    // Ignored by OptaPlanner, used by the UI to display solve or stop solving button
    private SolverStatus solverStatus;

    public Scheduling() {
    }

    public Scheduling(List<Sgruser> sgruserList, List<String> sgshiftList, List<Sgbackup> sgbackupList, List<Sgresult> sgresultList) {
        this.sgruserList = sgruserList;
        this.sgshiftList = sgshiftList;
        this.sgbackupList = sgbackupList;
        this.sgresultList = sgresultList;
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public List<Sgruser> getSgruserList() {
        return sgruserList;
    }

    public List<String> getSgshiftList() {
        return sgshiftList;
    }
    
    public List<Sgbackup> getSgbackupList() {
        return sgbackupList;
    }

    public List<Sgresult> getSgresultList() {
    	return sgresultList;
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
