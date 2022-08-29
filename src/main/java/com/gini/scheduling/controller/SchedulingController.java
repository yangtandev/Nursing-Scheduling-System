package com.gini.scheduling.controller;


import com.gini.scheduling.model.*;
import com.gini.scheduling.service.SchedulingService;
import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.api.solver.SolverStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
public class SchedulingController {
    @Autowired
    private SchedulingService schedulingService;
    @Autowired(required = false)
    private SolverManager<Scheduling, String> solverManager;
    @Autowired(required = false)
    private ScoreManager<Scheduling> scoreManager;

    @PostMapping("/solve")
    public String solve() {
        solverManager.solveAndListen(SchedulingService.SINGLETON_TIME_TABLE_ID,
                schedulingService::findById,
                schedulingService::save);
        return "Success";
    }

    public SolverStatus getSolverStatus() {
        return solverManager.getSolverStatus(SchedulingService.SINGLETON_TIME_TABLE_ID);
    }

    @PostMapping("/stopSolving")
    public String stopSolving() {
        solverManager.terminateEarly(SchedulingService.SINGLETON_TIME_TABLE_ID);
        return "Success";
    }
}
