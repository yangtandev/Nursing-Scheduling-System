package com.gini.scheduling.solver;

import com.gini.scheduling.domain.Scheduling;
import com.gini.scheduling.persistence.SchedulingRepository;
import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.api.solver.SolverStatus;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.SolverManagerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class SchedulingController {
    @Autowired
    private SchedulingRepository schedulingRepository;
    @Autowired(required = false)
    private SolverManager<Scheduling, String> solverManager;
    @Autowired(required = false)
    private ScoreManager<Scheduling> scoreManager;

    @GetMapping()
    public Scheduling getScheduling() {
        // Get the solver status before loading the solution
        // to avoid the race condition that the solver terminates between them
        SolverStatus solverStatus = getSolverStatus();
        Scheduling solution = schedulingRepository.findById(SchedulingRepository.SINGLETON_TIME_TABLE_ID);
        scoreManager.updateScore(solution); // Sets the score
        solution.setSolverStatus(solverStatus);
        return solution;
    }

    @PostMapping("/solve")
    public void solve() {
        solverManager.solveAndListen(SchedulingRepository.SINGLETON_TIME_TABLE_ID,
                schedulingRepository::findById,
                schedulingRepository::save);
    }

    public SolverStatus getSolverStatus() {
        return solverManager.getSolverStatus(SchedulingRepository.SINGLETON_TIME_TABLE_ID);
    }

    @PostMapping("/stopSolving")
    public void stopSolving() {
        solverManager.terminateEarly(SchedulingRepository.SINGLETON_TIME_TABLE_ID);
    }
}
