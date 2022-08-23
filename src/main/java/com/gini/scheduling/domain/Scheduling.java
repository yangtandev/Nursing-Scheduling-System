package com.gini.scheduling.domain;

import java.util.List;

import com.gini.scheduling.persistence.StaffRepository;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.solver.SolverStatus;

@PlanningSolution
public class Scheduling {
    @ValueRangeProvider(id = "staffRange")
    @ProblemFactCollectionProperty
    private List<Staff> staffList;

    @PlanningEntityCollectionProperty
    private List<Shift> shiftList;

    @PlanningScore
    private HardSoftScore score;

    // Ignored by OptaPlanner, used by the UI to display solve or stop solving button
    private SolverStatus solverStatus;

    private Scheduling() {
    }

    public Scheduling(List<Shift> shiftList,
                     List<Staff> staffList) {
        this.shiftList = shiftList;
        this.staffList = staffList;

    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public List<Shift> getShiftList() {
        return shiftList;
    }

    public List<Staff> getStaffList() {
        return staffList;
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
