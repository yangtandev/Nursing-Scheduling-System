package com.gini.scheduling.controller;


import com.gini.scheduling.dao.*;
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
    private SgresultRepository sgresultRepository;
    @Autowired
    private SgschRepository sgschRepository;
    @Autowired
    private SgbackupRepository sgbackupRepository;
    @Autowired
    private SgsysRepository sgsysRepository;
    @Autowired
    private SgruserRepository sgruserRepository;
    @Autowired(required = false)
    private SolverManager<Scheduling, String> solverManager;
    @Autowired(required = false)
    private ScoreManager<Scheduling> scoreManager;
    public static final Logger logger = LoggerFactory.getLogger(SgrroomController.class);

    public void backupSgsch(LocalDate startSchdate, LocalDate endSchdate){
        int sgbackupNumber = sgbackupRepository.findCountByDate(startSchdate, endSchdate);
        if(sgbackupNumber==0){
            List<Sgsch> sgschList = sgschRepository.findAllByDate(startSchdate, endSchdate);
            for(Sgsch sgsch:sgschList){
                String uno = sgsch.getUno();
                LocalDate schdate = sgsch.getSchdate();
                String clsno = sgsch.getClsno();
                sgbackupRepository.save(new Sgbackup(uno, schdate, clsno));
            }
        }
    }
    public void syncSgsch(LocalDate startSchdate, LocalDate endSchdate){
        int sgschNumber = sgschRepository.findCountByDate(startSchdate, endSchdate);
        if (sgschNumber > 0) {
            sgschRepository.deleteALLByDate(startSchdate, endSchdate);
        }
        List<Sgresult> sgresultList = sgresultRepository.findAllByDate(startSchdate, endSchdate);
        for(Sgresult sgresult:sgresultList){
            String uno = sgresult.getUno();
            LocalDate schdate = sgresult.getSchdate();
            String clsno = sgresult.getClsno();
            int clspr = sgresult.getClspr();
            int overtime = sgresult.getOvertime();
            sgschRepository.save(new Sgsch(uno, schdate, clsno, clspr, overtime));
        }
    }

    public Map<String, Integer> getRequiredCLS(){
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
        Map<String, Integer> sgresultMap = new HashMap<>();
        sgresultMap.put("55", r55RoomOpen * r55NeedManpower + 4);
        sgresultMap.put("D6", 2);
        sgresultMap.put("A0", 2);
        sgresultMap.put("A8", 2);
        return sgresultMap;
    }
    public void init(LocalDate startSchdate, LocalDate endSchdate) {
        List<Sgbackup> sgbackupList = sgbackupRepository.findAllByDate(startSchdate, endSchdate);

            long monthsBetween = ChronoUnit.MONTHS.between(
                    LocalDate.parse(startSchdate.toString()),
                    LocalDate.parse(endSchdate.toString()).plusDays(1));
            for (int i = 0; i < monthsBetween; i++) {
                LocalDate currentMonth = startSchdate.plusMonths(i);
                YearMonth month = YearMonth.from(currentMonth);
                LocalDate monthStart = month.atDay(1);
                LocalDate monthEnd = month.atEndOfMonth();
                int sgresultNumber = sgresultRepository.findCountByDate(monthStart, monthEnd);
                if (sgresultNumber > 0) {
                    sgresultRepository.deleteALLByDate(monthStart, monthEnd);
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
                        for (Map.Entry<String, Integer> entry : this.getRequiredCLS().entrySet()) {
                            for (int count = 0; count < entry.getValue(); count++) {
                                if (!sgrusers.hasNext()) {
                                    sgrusers = sgruserRepository.findAll().iterator();
                                }
                                Sgruser sgruser = sgrusers.next();
                                String clsno = entry.getKey();
                                sgresultRepository.save(new Sgresult(sgruser, currentDate, currentWeek, clsno));
                            }
                        }

                    }
                }
            }

    }

    @GetMapping("/solve")
    public String solve(LocalDate startSchdate, LocalDate endSchdate) {
        this.stopSolving();
        this.backupSgsch(startSchdate, endSchdate);
        this.init(startSchdate, endSchdate);
        solverManager.solveAndListen(SchedulingService.SINGLETON_TIME_TABLE_ID,
                schedulingService::findById,
                schedulingService::save);
        this.syncSgsch(startSchdate, endSchdate);
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
