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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping
public class SchedulingController {
    @Autowired
    private SchedulingService schedulingService;
    @Autowired
    private SgresultRepository sgresultRepository;
    @Autowired
    private SgshiftRepository sgshiftRepository;
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

    public void backupSgsch(LocalDate startSchdate, LocalDate endSchdate) {
        List<Sgbackup> sgbackupList = new ArrayList<>();
        List<Sgsch> sgschList = sgschRepository.findAllByDate(startSchdate, endSchdate);
        for (Sgsch sgsch : sgschList) {
            String uno = sgsch.getUno();
            LocalDate schdate = sgsch.getSchdate();
            String clsno = sgsch.getClsno();
            sgbackupList.add(new Sgbackup(uno, schdate, clsno));
        }
        sgbackupRepository.saveAll(sgbackupList);
    }

    public void syncSgsch(LocalDate startSchdate, LocalDate endSchdate) {
        List<Sgsch> sgschList = new ArrayList<>();
        List<Sgresult> sgresultList = sgresultRepository.findAllByDate(startSchdate, endSchdate);
        for (Sgresult sgresult : sgresultList) {
            String uno = sgresult.getUno();
            LocalDate schdate = sgresult.getSchdate();
            String clsno = sgresult.getSgshift().getClsno();
            int clspr = sgresult.getClspr();
            int overtime = sgresult.getOvertime();
            sgschList.add(new Sgsch(uno, schdate, clsno, clspr, overtime));
        }
        sgschRepository.saveAll(sgschList);
    }

    public void init(LocalDate startSchdate, LocalDate endSchdate) {
        // clean SGRESULT
        sgresultRepository.deleteALLByDate(startSchdate, endSchdate);

        List<Sgruser> sgruserList = sgruserRepository.findAll();
        List<Sgshift> sgshiftList = sgshiftRepository.findAll();
        List<Sgsys> sgsysList = sgsysRepository.findAll();
        List<Sgresult> sgresultList = new ArrayList<>();
        long monthsBetween = ChronoUnit.MONTHS.between(
            LocalDate.parse(startSchdate.toString()),
            LocalDate.parse(endSchdate.toString()).plusDays(1));

        for (int i = 0; i < monthsBetween; i++) {
            LocalDate currentMonth = startSchdate.plusMonths(i);
            YearMonth month = YearMonth.from(currentMonth);
            LocalDate monthStart = month.atDay(1);
            LocalDate monthEnd = month.atEndOfMonth();
            List<Sgbackup> unavaliableList = sgbackupRepository.findAllByDate(monthStart, monthEnd);
            List<DateGenerator.WeekInfo> weekList = new DateGenerator().getScope(String.valueOf(currentMonth.getYear()), String.valueOf(currentMonth.getMonth().getValue()));

            for (int week = 0; week < weekList.size(); week++) {
                LocalDate startDate = LocalDate.parse(weekList.get(week).getStart());
                LocalDate endDate = LocalDate.parse(weekList.get(week).getEnd());
                int totalDates = endDate.getDayOfMonth() - startDate.getDayOfMonth() + 1;
                int currentWeek = week + 1;

                for (int dateIndex = 0; dateIndex < totalDates; dateIndex++) {
                    LocalDate currentDate = startDate.plusDays(dateIndex);
                    DayOfWeek day = DayOfWeek.of(currentDate.get(ChronoField.DAY_OF_WEEK));
                    Boolean isWeekend = day == DayOfWeek.SUNDAY || day == DayOfWeek.SATURDAY;
                    Iterator<Sgruser> avaliableIterator;
                    // 判斷有無休假紀錄
                    if (unavaliableList.size() > 0) {
                        // 預設每日所有人為 OFF，如有排公休則予以公休。
                        for (Sgruser sgruser : sgruserList) {
                            Boolean isVacation = unavaliableList
                                .stream()
                                .filter(sgbackup -> sgbackup.getUno().equals(sgruser.getUno()) && sgbackup.getClsno().equals("公休"))
                                .collect(Collectors.toList())
                                .size() > 0;
                            if (isVacation) {
                                sgresultList.add(new Sgresult(sgruser.getUno(), currentDate, currentWeek, new Sgshift("公休")));
                            } else {
                                sgresultList.add(new Sgresult(sgruser.getUno(), currentDate, currentWeek, new Sgshift("OFF")));
                            }
                        }
                        // 參考預約休假名單，過濾休假人員，以獲取可出勤人員名單
                        avaliableIterator = sgruserRepository
                            .findAll()
                            .stream()
                            .filter(sgruser ->
                                unavaliableList
                                    .stream()
                                    .filter(sgbackup -> sgbackup.getUno().equals(sgruser.getUno()) && (sgbackup.getClsno().equals("OFF") || sgbackup.getClsno().equals("公休")))
                                    .collect(Collectors.toList())
                                    .size() == 0
                            )
                            .collect(Collectors.toList())
                            .iterator();
                    } else {
                        // 預設每日所有人為 OFF
                        for (Sgruser sgruser : sgruserList) {
                            sgresultList.add(new Sgresult(sgruser.getUno(), currentDate, currentWeek, new Sgshift("OFF")));
                        }
                        avaliableIterator = sgruserRepository.findAll().iterator();
                    }
                    // 獲取排班設定
                    int r55RoomOpen = 0;
                    int r55NeedManpower = 0;
                    int rd6Manpower = 0;
                    int ra0Manpower = 0;
                    int ra8Manpower = 0;
                    int rdailyManpower = 0;
                    for (Sgsys sgsys : sgsysList) {
                        switch (sgsys.getSkey()) {
                            case "r55RoomOpen":
                                r55RoomOpen = Integer.parseInt(sgsys.getVal());
                                break;
                            case "r55NeedManpower":
                                r55NeedManpower = Integer.parseInt(sgsys.getVal());
                                break;
                            case "rd6Manpower":
                                rd6Manpower = Integer.parseInt(sgsys.getVal());
                                break;
                            case "ra0Manpower":
                                ra0Manpower = Integer.parseInt(sgsys.getVal());
                                break;
                            case "ra8Manpower":
                                ra8Manpower = Integer.parseInt(sgsys.getVal());
                                break;
                            case "rdailyManpower":
                                rdailyManpower = Integer.parseInt(sgsys.getVal());
                                break;
                        }
                    }
                    // 計算每日各班別出勤人數
                    for (Sgshift sgshift : sgshiftList) {
                        int requireManpower = 0;
                        // 判斷是否為假日
                        if (isWeekend) {
                            switch (sgshift.getClsno()) {
                                case "55":
                                    requireManpower = 2;
                                    break;
                                case "D6":
                                    requireManpower = rd6Manpower;
                                    break;
                                case "A0":
                                    requireManpower = ra0Manpower;
                                    break;
                            }
                        } else {
                            switch (sgshift.getClsno()) {
                                case "55":
                                    requireManpower = r55RoomOpen * r55NeedManpower;
                                    break;
                                case "D6":
                                    requireManpower = rd6Manpower;
                                    break;
                                case "A0":
                                    requireManpower = ra0Manpower;
                                    break;
                                case "A8":
                                    requireManpower = ra8Manpower;
                                    break;
                                case "常日":
                                    requireManpower = rdailyManpower;
                                    break;
                            }
                        }
                        // 指派人員出勤到各班別
                        for (int manpower = 1; manpower <= requireManpower; manpower++) {
                            String currentUno = avaliableIterator.next().getUno();
                            // 當非休假人員額度用完，則指派 OFF 班別人員出勤
                            if (!avaliableIterator.hasNext()) {
                                avaliableIterator = sgruserRepository
                                    .findAll()
                                    .stream()
                                    .filter(sgruser ->
                                        sgresultList
                                            .stream()
                                            .filter(sgresult -> sgresult.getUno().equals(sgruser.getUno()) && sgresult.getSgshift().getClsno().equals("OFF"))
                                            .collect(Collectors.toList())
                                            .size() > 0
                                    )
                                    .collect(Collectors.toList())
                                    .iterator();
                            }
                            // 更新排班結果表
                            for (int sgresultIndex = 0; sgresultIndex < sgresultList.size(); sgresultIndex++) {
                                if (sgresultList.get(sgresultIndex).getUno().equals(currentUno) && sgresultList.get(sgresultIndex).getSchdate().equals(currentDate)) {
                                    sgresultList.get(sgresultIndex).setSgshift(sgshift);
                                }
                            }
                        }
                    }
                }
            }
        }
        sgresultRepository.saveAll(sgresultList);
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
