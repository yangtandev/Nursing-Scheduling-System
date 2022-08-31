package com.gini.scheduling.controller;


import com.gini.scheduling.dao.SgruserRepository;
import com.gini.scheduling.dao.SgschRepository;
import com.gini.scheduling.dao.SgsysRepository;
import com.gini.scheduling.model.*;
import com.gini.scheduling.service.SchedulingService;
import com.gini.scheduling.utils.DateGenerator;
import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.api.solver.SolverStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping
public class SchedulingController {
    @Autowired
    private SchedulingService schedulingService;
    @Autowired
    private SgschRepository sgschRepository;
    @Autowired
    private SgsysRepository sgsysRepository;
    @Autowired
    private SgruserRepository sgruserRepository;
    @Autowired(required = false)
    private SolverManager<Scheduling, String> solverManager;
    @Autowired(required = false)
    private ScoreManager<Scheduling> scoreManager;
    public static final Logger logger = LoggerFactory.getLogger(SgrroomController.class);

    public void init(LocalDate startSchdate, LocalDate endSchdate) {
        int r55RoomOpen = 12;
        int r55NeedManpower = 2;
        List<Sgsys> sgsysList = sgsysRepository.findAll();
        for (Sgsys sgsys : sgsysList) {
            if (sgsys.getSkey().equals("r55RoomOpen")) {
                r55RoomOpen = Integer.parseInt(sgsys.getVal());
            } else if (sgsys.getSkey().equals("r55NeedManpower")) {
                r55NeedManpower = Integer.parseInt(sgsys.getVal());
            }
        }
        Map<String, Integer> sgschMap = new HashMap<>();
        sgschMap.put("55", r55RoomOpen * r55NeedManpower + 4);
        sgschMap.put("D6", 2);
        sgschMap.put("A0", 2);
        sgschMap.put("A8", 2);

        long monthsBetween = ChronoUnit.MONTHS.between(
                LocalDate.parse(startSchdate.toString()),
                LocalDate.parse(endSchdate.toString()).plusDays(1));
        for (int i = 0; i < monthsBetween; i++) {
            LocalDate currentMonth = startSchdate.plusMonths(i);
            YearMonth month = YearMonth.from(currentMonth);
            LocalDate monthStart = month.atDay(1);
            LocalDate monthEnd = month.atEndOfMonth();
            int sgschNumber = sgschRepository.findCountByDate(monthStart, monthEnd);
            if (sgschNumber > 0) {
                sgschRepository.deleteALLByDate(monthStart, monthEnd);
            }
            DateGenerator newDateUtil = new DateGenerator();
            List<DateGenerator.WeekInfo> weekList = newDateUtil.getScope(String.valueOf(currentMonth.getYear()), String.valueOf(currentMonth.getMonth().getValue()));

            for (int week = 0; week < weekList.size(); week++) {
                int currentWeek = week + 1;
                Iterator<Sgruser> sgrusers = sgruserRepository.findAll().iterator();
                LocalDate startDate = LocalDate.parse(weekList.get(week).getStart());
                LocalDate endDate = LocalDate.parse(weekList.get(week).getEnd());
                int totalDates = endDate.getDayOfMonth() - startDate.getDayOfMonth() + 1;
                for (int dateIndex = 0; dateIndex < totalDates; dateIndex++) {
                    LocalDate currentDate
                            = startDate.plusDays(dateIndex);
                    for (Map.Entry<String, Integer> entry : sgschMap.entrySet()) {
                        for (int count = 0; count < entry.getValue(); count++) {
                            if (!sgrusers.hasNext()) {
                                sgrusers = sgruserRepository.findAll().iterator();
                            }
                            Sgruser sgruser = sgrusers.next();
                            String clsno = entry.getKey();
                            sgschRepository.save(new Sgsch(sgruser, currentDate, currentWeek, clsno));
                        }
                    }

                }
            }
        }
    }

    @PostMapping("/solve")
    public String solve(LocalDate startSchdate, LocalDate endSchdate) {
        this.stopSolving();
        this.init(startSchdate, endSchdate);
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