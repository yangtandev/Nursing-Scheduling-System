package com.gini.scheduling.controller;

import com.gini.scheduling.dao.*;
import com.gini.scheduling.model.*;
import com.gini.scheduling.service.SchedulingService;
import com.gini.scheduling.utils.DateGenerator;
import com.gini.scheduling.utils.VacationDayCalculate;
import org.apache.commons.lang3.StringUtils;
import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.api.solver.SolverStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping
public class SchedulingController {
    public static final Logger logger = LoggerFactory.getLogger(SgrroomController.class);
    @Autowired
    private SchedulingService schedulingService;
    @Autowired
    private Scheduling scheduling;
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
    @Value("${optaplanner.solver.termination.spent-limit}")
    private String spentLimit;

    public static <T> List<T> getRandomList(List<T> sourceList, int total) {
        List<T> tempList = new ArrayList<T>();
        List<T> result = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            int random = new Random().nextInt(sourceList.size());
            if (!tempList.contains(sourceList.get(random))) {
                tempList.add(sourceList.get(random));
                result.add(sourceList.remove(random));
            } else {
                i--;
            }
        }
        return result;
    }

    public void cleanSgbackupAndSgresult(LocalDate startSchdate, LocalDate endSchdate) {
        sgbackupRepository.deleteALLByDate(startSchdate, endSchdate);
        sgresultRepository.deleteALLByDate(startSchdate, endSchdate);
    }

    public void backupSgsch(LocalDate startSchdate, LocalDate endSchdate) {
        List<Sgbackup> sgbackupList = new ArrayList<>();
        List<Sgsch> sgschList = sgschRepository.findAllByDate(startSchdate, endSchdate);
        for (Sgsch sgsch : sgschList) {
            String uno = sgsch.getUno();
            LocalDate schdate = sgsch.getSchdate();
            String clsno = sgsch.getClsno();
            if (clsno.equals("OFF") || clsno.equals("公休")) {
                sgbackupList.add(new Sgbackup(uno, schdate, clsno));
            }
        }
        sgbackupRepository.saveAll(sgbackupList);
    }

    public void syncSgsch(LocalDate startSchdate, LocalDate endSchdate) {
        List<Sgsch> sgschList = new ArrayList<>();
        List<Sgresult> sgresultList = sgresultRepository.findAllByDate(startSchdate, endSchdate);
        for (Sgresult sgresult : sgresultList) {
            String uno = sgresult.getUno();
            LocalDate schdate = sgresult.getSchdate();
            String clsno = sgresult.getClsno();
            int clspr = sgresult.getClspr();
            int overtime = sgresult.getOvertime();
            sgschList.add(new Sgsch(uno, schdate, clsno, clspr, overtime));
        }
        sgschRepository.saveAll(sgschList);
    }

    public void init(LocalDate startSchdate, LocalDate endSchdate) {
        List<Sgruser> sgruserList = sgruserRepository.findAll();
        List<Sgruser> sgruserRegularList = new ArrayList<>();
        for (int index = 0; index < 2; index++) {
            int random = new Random().nextInt(sgruserList.size());
            sgruserRegularList.add(sgruserList.remove(random));
        }
        List<Sgruser> sgruserBreastFeedList = new ArrayList<>();
        for (int index = 0; index < sgruserList.size(); index++) {
            Boolean uteamIsBreastFeed = sgruserList.get(index).getUteam().equals("哺乳");
            if (uteamIsBreastFeed) sgruserBreastFeedList.add(sgruserList.remove(index));
        }
        List<Sgshift> sgshiftList = sgshiftRepository.findAll();
        List<Sgsys> sgsysList = sgsysRepository.findAll();
        List<Sgresult> sgresultList = new ArrayList<>();
        List<Sgbackup> sgbackupList = new ArrayList<>();
        long monthsBetween = ChronoUnit.MONTHS.between(LocalDate.parse(startSchdate.toString()),
            LocalDate.parse(endSchdate.toString()).plusDays(1));
        for (int i = 0; i < monthsBetween; i++) {
            // 本月日期區間
            LocalDate firstDateOfTheMonth = startSchdate.plusMonths(i);
            YearMonth month = YearMonth.from(firstDateOfTheMonth);
            LocalDate monthStart = month.atDay(1);
            LocalDate monthEnd = month.atEndOfMonth();
            // 上月日期區間
            LocalDate firstDateOfTheLastMonth = startSchdate.minusMonths(1);
            YearMonth lastMonth = YearMonth.from(firstDateOfTheLastMonth);
            LocalDate lastMonthStart = lastMonth.atDay(1);
            LocalDate lastMonthEnd = lastMonth.atEndOfMonth();
            List<Sgbackup> unavailableList = sgbackupRepository.findAllByDate(monthStart, monthEnd)
                .stream()
                .filter(sgbackup -> sgbackup.getClsno().equals("OFF") || sgbackup.getClsno().equals("公休"))
                .collect(Collectors.toList());
            List<DateGenerator.WeekInfo> weekList = new DateGenerator().getScope(
                String.valueOf(firstDateOfTheMonth.getYear()),
                String.valueOf(firstDateOfTheMonth.getMonth().getValue()));
            // 獲取排班設定
            int r55RoomOpen = 0;
            int r55NeedManpower = 0;
            int r55Holiday = 0;
            int r55HolidayA = 0;
            int r55HolidayB = 0;
            int r55HolidayC = 0;
            int r55Wait = 0;
            int r55Nurse = 0;
            int r55WorkStat = 0;
            int r55OPDOR = 0;
            int rd6Manpower = 0;
            int rd6Holiday = 0;
            int rd6ManpowerA = 0;
            int rd6ManpowerB = 0;
            int rd6ManpowerC = 0;
            int ra0Manpower = 0;
            int ra0Holiday = 0;
            int ra0ManpowerA = 0;
            int ra0ManpowerB = 0;
            int ra0ManpowerC = 0;
            int ra8Manpower = 0;
            int ra8ManpowerA = 0;
            int ra8ManpowerB = 0;
            int ra8ManpowerC = 0;
            for (Sgsys sgsys : sgsysList) {
                switch (sgsys.getSkey()) {
                    case "r55RoomOpen":
                        r55RoomOpen = Integer.parseInt(sgsys.getVal());
                        break;
                    case "r55NeedManpower":
                        r55NeedManpower = Integer.parseInt(sgsys.getVal());
                        break;
                    case "r55Holiday":
                        r55Holiday = Integer.parseInt(sgsys.getVal());
                        break;
                    case "r55HolidayA":
                        r55HolidayA = Integer.parseInt(sgsys.getVal());
                        break;
                    case "r55HolidayB":
                        r55HolidayB = Integer.parseInt(sgsys.getVal());
                        break;
                    case "r55HolidayC":
                        r55HolidayC = Integer.parseInt(sgsys.getVal());
                        break;
                    case "r55Wait":
                        r55Wait = Integer.parseInt(sgsys.getVal());
                        break;
                    case "r55Nurse":
                        r55Nurse = Integer.parseInt(sgsys.getVal());
                        break;
                    case "r55WorkStat":
                        r55WorkStat = Integer.parseInt(sgsys.getVal());
                        break;
                    case "r55OPDOR":
                        r55OPDOR = Integer.parseInt(sgsys.getVal());
                        break;
                    case "rd6Manpower":
                        rd6Manpower = Integer.parseInt(sgsys.getVal());
                        break;
                    case "rd6Holiday":
                        rd6Holiday = Integer.parseInt(sgsys.getVal());
                        break;
                    case "rd6ManpowerA":
                        rd6ManpowerA = Integer.parseInt(sgsys.getVal());
                        break;
                    case "rd6ManpowerB":
                        rd6ManpowerB = Integer.parseInt(sgsys.getVal());
                        break;
                    case "rd6ManpowerC":
                        rd6ManpowerC = Integer.parseInt(sgsys.getVal());
                        break;
                    case "ra0Manpower":
                        ra0Manpower = Integer.parseInt(sgsys.getVal());
                        break;
                    case "ra0Holiday":
                        ra0Holiday = Integer.parseInt(sgsys.getVal());
                        break;
                    case "ra0ManpowerA":
                        ra0ManpowerA = Integer.parseInt(sgsys.getVal());
                        break;
                    case "ra0ManpowerB":
                        ra0ManpowerB = Integer.parseInt(sgsys.getVal());
                        break;
                    case "ra0ManpowerC":
                        ra0ManpowerC = Integer.parseInt(sgsys.getVal());
                        break;
                    case "ra8Manpower":
                        ra8Manpower = Integer.parseInt(sgsys.getVal());
                        break;
                    case "ra8ManpowerA":
                        ra8ManpowerA = Integer.parseInt(sgsys.getVal());
                        break;
                    case "ra8ManpowerB":
                        ra8ManpowerB = Integer.parseInt(sgsys.getVal());
                        break;
                    case "ra8ManpowerC":
                        ra8ManpowerC = Integer.parseInt(sgsys.getVal());
                        break;
                }
            }
            // 取得包班名單
            List<String> rd6ScheduledAList = new ArrayList<>();
            List<String> rd6ScheduledBList = new ArrayList<>();
            List<String> rd6ScheduledCList = new ArrayList<>();
            List<String> ra0ScheduledAList = new ArrayList<>();
            List<String> ra0ScheduledBList = new ArrayList<>();
            List<String> ra0ScheduledCList = new ArrayList<>();
            for (Sgruser sgruser : sgruserList) {
                if (sgruser.isUissn()) {
                    if (sgruser.getUteam().equals("A")) rd6ScheduledAList.add(sgruser.getUno());
                    if (sgruser.getUteam().equals("B")) rd6ScheduledBList.add(sgruser.getUno());
                    if (sgruser.getUteam().equals("C")) rd6ScheduledCList.add(sgruser.getUno());
                }
                if (sgruser.isUisbn()) {
                    if (sgruser.getUteam().equals("A")) ra0ScheduledAList.add(sgruser.getUno());
                    if (sgruser.getUteam().equals("B")) ra0ScheduledBList.add(sgruser.getUno());
                    if (sgruser.getUteam().equals("C")) ra0ScheduledCList.add(sgruser.getUno());
                }
            }
            // 同班別同組別中，如有多人包班，則隨機取一。
            List<String> rd6ScheduledList = Stream
                .of(
                    rd6ScheduledAList.size() > 1 ? getRandomList(rd6ScheduledAList, 1) : rd6ScheduledAList,
                    rd6ScheduledBList.size() > 1 ? getRandomList(rd6ScheduledBList, 1) : rd6ScheduledBList,
                    rd6ScheduledCList.size() > 1 ? getRandomList(rd6ScheduledCList, 1) : rd6ScheduledCList
                )
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());
            List<String> ra0ScheduledList = Stream
                .of(
                    ra0ScheduledAList.size() > 1 ? getRandomList(ra0ScheduledAList, 1) : ra0ScheduledAList,
                    ra0ScheduledBList.size() > 1 ? getRandomList(ra0ScheduledBList, 1) : ra0ScheduledBList,
                    ra0ScheduledCList.size() > 1 ? getRandomList(ra0ScheduledCList, 1) : ra0ScheduledCList
                )
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());
            // 獲取上月 D6、A0 名單。
            List<Sgbackup> lastMonthSgbackupList = sgbackupRepository.findAllByDate(lastMonthStart, lastMonthStart);
            List<String> lastMonthD6UnoAList = new ArrayList<>();
            List<String> lastMonthD6UnoBList = new ArrayList<>();
            List<String> lastMonthD6UnoCList = new ArrayList<>();
            List<String> lastMonthA0UnoAList = new ArrayList<>();
            List<String> lastMonthA0UnoBList = new ArrayList<>();
            List<String> lastMonthA0UnoCList = new ArrayList<>();
            for (Sgbackup sgbackup : lastMonthSgbackupList) {
                String uteam = "";
                for (Sgruser sgruser : sgruserList) {
                    Boolean isCurrentUno = sgruser.getUno().equals(sgbackup.getUno());
                    if (isCurrentUno) uteam = sgruser.getUteam();
                }
                Boolean isD6 = sgbackup.getClsno().equals("D6");
                if (isD6) {
                    if (uteam.equals("A")) lastMonthD6UnoAList.add(sgbackup.getUno());
                    if (uteam.equals("B")) lastMonthD6UnoBList.add(sgbackup.getUno());
                    if (uteam.equals("C")) lastMonthD6UnoCList.add(sgbackup.getUno());
                }
                Boolean isA0 = sgbackup.getClsno().equals("A0");
                if (isA0) {
                    if (uteam.equals("A")) lastMonthA0UnoAList.add(sgbackup.getUno());
                    if (uteam.equals("B")) lastMonthA0UnoBList.add(sgbackup.getUno());
                    if (uteam.equals("C")) lastMonthA0UnoCList.add(sgbackup.getUno());
                }
            }
            List<String> lastMonthD6UnoList = Stream
                .of(
                    lastMonthD6UnoAList.size() > 1 ? getRandomList(lastMonthD6UnoAList, 1) : lastMonthD6UnoAList,
                    lastMonthD6UnoBList.size() > 1 ? getRandomList(lastMonthD6UnoBList, 1) : lastMonthD6UnoBList,
                    lastMonthD6UnoCList.size() > 1 ? getRandomList(lastMonthD6UnoCList, 1) : lastMonthD6UnoCList
                )
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());
            List<String> lastMonthA0UnoList = Stream
                .of(
                    lastMonthA0UnoAList.size() > 1 ? getRandomList(lastMonthA0UnoAList, 1) : lastMonthA0UnoAList,
                    lastMonthA0UnoBList.size() > 1 ? getRandomList(lastMonthA0UnoBList, 1) : lastMonthA0UnoBList,
                    lastMonthA0UnoCList.size() > 1 ? getRandomList(lastMonthA0UnoCList, 1) : lastMonthA0UnoCList
                )
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());
            // 依組別取出各組人員名單，並排除包班、上月名單人員。
            List<String> sgruserAList = sgruserList
                .stream()
                .filter(sgruser -> {
                    if (rd6ScheduledList.contains(sgruser.getUno())
                        || ra0ScheduledList.contains(sgruser.getUno())
                        || lastMonthD6UnoList.contains(sgruser.getUno())
                        || lastMonthA0UnoList.contains(sgruser.getUno())
                    ) return false;
                    return sgruser.getUteam().equals("A");
                })
                .map(sgruser -> sgruser.getUno())
                .collect(Collectors.toList());
            List<String> sgruserBList = sgruserList
                .stream()
                .filter(sgruser -> {
                    if (rd6ScheduledList.contains(sgruser.getUno())
                        || ra0ScheduledList.contains(sgruser.getUno())
                        || lastMonthD6UnoList.contains(sgruser.getUno())
                        || lastMonthA0UnoList.contains(sgruser.getUno())
                    ) return false;
                    return sgruser.getUteam().equals("B");
                })
                .map(sgruser -> sgruser.getUno())
                .collect(Collectors.toList());
            List<String> sgruserCList = sgruserList
                .stream()
                .filter(sgruser -> {
                    if (rd6ScheduledList.contains(sgruser.getUno())
                        || ra0ScheduledList.contains(sgruser.getUno())
                        || lastMonthD6UnoList.contains(sgruser.getUno())
                        || lastMonthA0UnoList.contains(sgruser.getUno())
                    ) return false;
                    return sgruser.getUteam().equals("C");
                })
                .map(sgruser -> sgruser.getUno())
                .collect(Collectors.toList());
            // 計算 A0 班出勤人數，優先順序為: 包班名單 > 上月名單 > 隨機名單。
            if (ra0ScheduledAList.size() >= ra0ManpowerA) {
                lastMonthA0UnoAList.removeAll(ra0ScheduledAList);
            } else if (ra0ScheduledAList.size() < ra0ManpowerA) {
                int numberOfShortages = ra0ManpowerA - ra0ScheduledAList.size();
                ra0ScheduledAList.addAll(getRandomList(sgruserAList, numberOfShortages));
            }
            if (ra0ScheduledBList.size() >= ra0ManpowerB) {
                lastMonthA0UnoBList.removeAll(ra0ScheduledBList);
            } else if (ra0ScheduledBList.size() < ra0ManpowerB) {
                int numberOfShortages = ra0ManpowerB - ra0ScheduledBList.size();
                ra0ScheduledBList.addAll(getRandomList(sgruserBList, numberOfShortages));
            }
            if (ra0ScheduledCList.size() >= ra0ManpowerC) {
                lastMonthA0UnoCList.removeAll(ra0ScheduledCList);
            } else if (ra0ScheduledCList.size() < ra0ManpowerC) {
                int numberOfShortages = ra0ManpowerC - ra0ScheduledCList.size();
                ra0ScheduledCList.addAll(getRandomList(sgruserCList, numberOfShortages));
            }
            List<String> ra0ABCList = Stream
                .of(
                    ra0ScheduledAList,
                    ra0ScheduledBList,
                    ra0ScheduledCList
                )
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());
            // 計算 D6 班出勤人數，優先順序為: 包班名單 > 上月名單 > 隨機名單。
            if (rd6ScheduledAList.size() >= rd6ManpowerA) {
                lastMonthD6UnoAList.removeAll(rd6ScheduledAList);
            } else if (rd6ScheduledAList.size() < rd6ManpowerA) {
                int numberOfShortages = rd6ManpowerA - rd6ScheduledAList.size();
                if (lastMonthA0UnoAList.size() >= numberOfShortages) {
                    rd6ScheduledAList.addAll(getRandomList(lastMonthA0UnoAList, numberOfShortages));
                    if (lastMonthA0UnoAList.size() > 0) sgruserAList.addAll(lastMonthA0UnoAList);
                } else if (lastMonthA0UnoAList.size() > 0) {
                    rd6ScheduledAList.addAll(lastMonthA0UnoAList);
                    numberOfShortages = rd6ManpowerA - rd6ScheduledAList.size() - lastMonthA0UnoAList.size();
                    rd6ScheduledAList.addAll(getRandomList(sgruserAList, numberOfShortages));
                } else {
                    rd6ScheduledAList.addAll(getRandomList(sgruserAList, numberOfShortages));
                }
                sgruserAList.removeAll(rd6ScheduledAList);
            }
            if (rd6ScheduledBList.size() >= rd6ManpowerB) {
                lastMonthD6UnoBList.removeAll(rd6ScheduledBList);
            } else if (rd6ScheduledBList.size() < rd6ManpowerB) {
                int numberOfShortages = rd6ManpowerB - rd6ScheduledBList.size();
                if (lastMonthA0UnoBList.size() >= numberOfShortages) {
                    rd6ScheduledBList.addAll(getRandomList(lastMonthA0UnoBList, numberOfShortages));
                    if (lastMonthA0UnoBList.size() > 0) sgruserBList.addAll(lastMonthA0UnoBList);
                } else if (lastMonthA0UnoBList.size() > 0) {
                    rd6ScheduledBList.addAll(lastMonthA0UnoBList);
                    numberOfShortages = rd6ManpowerB - rd6ScheduledBList.size() - lastMonthA0UnoBList.size();
                    rd6ScheduledBList.addAll(getRandomList(sgruserBList, numberOfShortages));
                } else {
                    rd6ScheduledBList.addAll(getRandomList(sgruserBList, numberOfShortages));
                }
            }
            if (rd6ScheduledCList.size() >= rd6ManpowerC) {
                lastMonthD6UnoCList.removeAll(rd6ScheduledCList);
            } else if (rd6ScheduledCList.size() < rd6ManpowerC) {
                int numberOfShortages = rd6ManpowerC - rd6ScheduledCList.size();
                if (lastMonthA0UnoCList.size() >= numberOfShortages) {
                    rd6ScheduledCList.addAll(getRandomList(lastMonthA0UnoCList, numberOfShortages));
                    if (lastMonthA0UnoCList.size() > 0) sgruserCList.addAll(lastMonthA0UnoCList);
                } else if (lastMonthA0UnoCList.size() > 0) {
                    rd6ScheduledCList.addAll(lastMonthA0UnoCList);
                    numberOfShortages = rd6ManpowerC - rd6ScheduledCList.size() - lastMonthA0UnoCList.size();
                    rd6ScheduledCList.addAll(getRandomList(sgruserCList, numberOfShortages));
                } else {
                    rd6ScheduledCList.addAll(getRandomList(sgruserCList, numberOfShortages));
                }
            }
            List<String> rd6ABCList = Stream
                .of(
                    rd6ScheduledAList,
                    rd6ScheduledBList,
                    rd6ScheduledCList
                )
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());
            // 計算 55 班出勤人數，優先順序為: 上月名單 > 隨機名單。
            List<String> r55HolidayAList = new ArrayList<>();
            List<String> r55HolidayBList = new ArrayList<>();
            List<String> r55HolidayCList = new ArrayList<>();
            if (lastMonthD6UnoAList.size() >= r55HolidayA) {
                r55HolidayAList.addAll(getRandomList(lastMonthD6UnoAList, r55HolidayA));
                if (lastMonthD6UnoAList.size() > 0) sgruserAList.addAll(lastMonthD6UnoAList);
            } else if (lastMonthD6UnoAList.size() < r55HolidayA) {
                r55HolidayAList.addAll(lastMonthD6UnoAList);
                int numberOfShortages = r55HolidayA - lastMonthD6UnoAList.size();
                r55HolidayAList.addAll(getRandomList(sgruserAList, numberOfShortages));
            } else {
                r55HolidayAList = getRandomList(sgruserAList, r55HolidayA);
            }
            if (lastMonthD6UnoBList.size() >= r55HolidayB) {
                r55HolidayBList.addAll(getRandomList(lastMonthD6UnoBList, r55HolidayB));
                if (lastMonthD6UnoBList.size() > 0) sgruserBList.addAll(lastMonthD6UnoBList);
            } else if (lastMonthD6UnoBList.size() < r55HolidayB) {
                r55HolidayBList.addAll(lastMonthD6UnoBList);
                int numberOfShortages = r55HolidayB - lastMonthD6UnoBList.size();
                r55HolidayBList.addAll(getRandomList(sgruserBList, numberOfShortages));
            } else {
                r55HolidayBList = getRandomList(sgruserBList, r55HolidayB);
            }
            if (lastMonthD6UnoCList.size() >= r55HolidayC) {
                r55HolidayCList.addAll(getRandomList(lastMonthD6UnoCList, r55HolidayC));
                if (lastMonthD6UnoCList.size() > 0) sgruserCList.addAll(lastMonthD6UnoCList);
            } else if (lastMonthD6UnoCList.size() < r55HolidayC) {
                r55HolidayCList.addAll(lastMonthD6UnoCList);
                int numberOfShortages = r55HolidayC - lastMonthD6UnoCList.size();
                r55HolidayCList.addAll(getRandomList(sgruserCList, numberOfShortages));
            } else {
                r55HolidayCList = getRandomList(sgruserCList, r55HolidayC);
            }
            List<String> r55HolidayABCList = Stream
                .of(
                    r55HolidayAList,
                    r55HolidayBList,
                    r55HolidayCList
                )
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());
            // 計算 A8 班出勤人數，優先順序為: 上月名單 > 隨機名單。
            List<String> ra8ABCList = new ArrayList<>();
            ra8ABCList.addAll(
                getRandomList(
                    Stream
                        .of(
                            getRandomList(sgruserAList, ra8ManpowerA),
                            getRandomList(sgruserBList, ra8ManpowerB),
                            getRandomList(sgruserCList, ra8ManpowerC)
                        )
                        .flatMap(x -> x.stream())
                        .collect(Collectors.toList()
                        ),
                    sgruserBreastFeedList.size() > 0 ? ra8Manpower - 1 : ra8Manpower
                )
            );
            Iterator<String> unoIterator = sgruserList
                .stream()
                .map(sgruser -> sgruser.getUno())
                .filter(uno -> !(r55HolidayABCList.contains(uno)
                        || rd6ABCList.contains(uno)
                        || ra0ABCList.contains(uno)
                        || ra8ABCList.contains(uno)
                    )
                )
                .iterator();
            if (sgruserBreastFeedList.size() > 0) {
                for (Sgruser sgruser : sgruserBreastFeedList) {
                    sgbackupList.add(new Sgbackup(sgruser.getUno(), monthStart, "哺乳"));
                }
            }
            for (Sgshift sgshift : sgshiftList) {
                String currentClsno = sgshift.getClsno();
                // 以當月首日各人員班別作為該月固定班別，並記錄在備分表。
                switch (currentClsno) {
                    case "55":
                        // 常日班
                        for (Sgruser sgruser : sgruserRegularList) {
                            sgbackupList.add(new Sgbackup(sgruser.getUno(), monthStart, "daily"));
                        }
                        // 55 假日班
                        for (String uno : r55HolidayABCList) {
                            sgbackupList.add(new Sgbackup(uno, monthStart, "55"));
                        }
                        // 55 班總人數(已扣除假日班及常日班人數)
                        int clsno55RequireManpower = r55RoomOpen * r55NeedManpower + r55Holiday + r55Wait + r55Nurse + r55WorkStat - r55HolidayABCList.size();
                        // 扣除哺乳班支援人數 (預扣支援A8人數:1人)
                        if (sgruserBreastFeedList.size() > 1)
                            clsno55RequireManpower = clsno55RequireManpower - (sgruserBreastFeedList.size() - 1);
                        while (clsno55RequireManpower > 0) {
                            sgbackupList.add(new Sgbackup(unoIterator.next(), monthStart, "55"));
                            clsno55RequireManpower--;
                        }
                        break;
                    case "D6":
                        for (String uno : rd6ABCList) {
                            sgbackupList.add(new Sgbackup(uno, monthStart, "D6"));
                        }
                        break;
                    case "A0":
                        for (String uno : ra0ABCList) {
                            sgbackupList.add(new Sgbackup(uno, monthStart, "A0"));
                        }
                        break;
                    case "A8":
                        for (String uno : ra8ABCList) {
                            sgbackupList.add(new Sgbackup(uno, monthStart, "A8"));
                        }
                        break;
                }
            }
            List<String> rd6PerCourseUnoList = new ArrayList<>();
            List<String> ra0PerCourseUnoList = new ArrayList<>();
            List<String> r55DaysOffUnoList = new ArrayList<>();
            List<String> ra8BreastFeedUnoList = new ArrayList<>();
            List<String> r55BreastFeedUnoList = new ArrayList<>();
            String ra8BreastFeedUno = "";
            for (int week = 0; week < weekList.size(); week++) {
                // 若本月哺乳班有兩人以上，則每週依序排入A8班別1人，其他人則排入55班別。
                if (sgruserBreastFeedList.size() > 0) {
                    if (ra8BreastFeedUnoList.size() == sgruserBreastFeedList.size()) {
                        ra8BreastFeedUnoList.clear();
                    }
                    ra8BreastFeedUno = sgruserBreastFeedList
                        .stream()
                        .map(sgruser -> sgruser.getUno())
                        .filter(uno -> {
                            if (ra8BreastFeedUnoList.size() > 0) return !ra8BreastFeedUnoList.contains(uno);
                            return true;
                        })
                        .collect(Collectors.toList())
                        .get(0);
                    ra8BreastFeedUnoList.add(ra8BreastFeedUno);
                    if (sgruserBreastFeedList.size() > 1) {
                        r55BreastFeedUnoList.clear();
                        for (Sgruser sgruser : sgruserBreastFeedList) {
                            if (!sgruser.getUno().equals(ra8BreastFeedUno)) {
                                r55BreastFeedUnoList.add(sgruser.getUno());
                            }
                        }
                    }
                }
                LocalDate startDate = LocalDate.parse(weekList.get(week).getStart());
                LocalDate endDate = LocalDate.parse(weekList.get(week).getEnd());
                Locale locale = new Locale("zh", "TW");
                int currentWeek = Integer.parseInt(DateTimeFormatter.ofPattern("yy").format(startDate)
                    + Integer.toString(startDate.get(WeekFields.of(locale).weekOfWeekBasedYear())));
                int totalDates = endDate.getDayOfMonth() - startDate.getDayOfMonth() + 1;
                for (int dateIndex = 0; dateIndex < totalDates; dateIndex++) {
                    LocalDate currentDate = startDate.plusDays(dateIndex);
                    DayOfWeek day = DayOfWeek.of(currentDate.get(ChronoField.DAY_OF_WEEK));
                    Boolean isWeekend = day == DayOfWeek.SUNDAY || day == DayOfWeek.SATURDAY;
                    // 判斷有無休假紀錄，如有排 OFF 則予以 OFF; 如有排公休則予以公休。
                    List<Sgruser> allSgruserList = new ArrayList<>();
                    allSgruserList.addAll(sgruserList);
                    allSgruserList.addAll(sgruserRegularList);
                    allSgruserList.addAll(sgruserBreastFeedList);
                    for (Sgruser sgruser : allSgruserList) {
                        Boolean isAnnualLeave = false;
                        Boolean isDayOff = false;
                        for (Sgbackup sgbackup : unavailableList) {
                            Boolean isCurrentDate = sgbackup.getSchdate().equals(currentDate);
                            Boolean isCurrentUno = sgbackup.getUno().equals(sgruser.getUno());
                            if (isCurrentDate && isCurrentUno) {
                                isAnnualLeave = sgbackup.getClsno().equals("公休");
                                isDayOff = sgbackup.getClsno().equals("OFF");
                            }
                        }
                        if (isAnnualLeave) {
                            sgresultList.add(new Sgresult(sgruser, currentDate, currentWeek, "公休", 0));
                        } else if (isDayOff) {
                            sgresultList.add(new Sgresult(sgruser, currentDate, currentWeek, "OFF", 0));
                        } else {
                            sgresultList.add(new Sgresult(sgruser, currentDate, currentWeek, "", 0));
                        }
                    }
                    // 參考預約休假名單，過濾休假人員，以獲取可出勤人員名單
                    List<Sgbackup> availableList = sgbackupList
                        .stream()
                        .filter(sgbackup -> {
                            Boolean isWorkday = !(sgbackup.getClsno().equals("OFF") || sgbackup.getClsno().equals("公休"));
                            Boolean isCurrentYear = sgbackup.getSchdate().getYear() == monthStart.getYear();
                            Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == monthStart.getMonthValue();
                            Boolean isAvailable = unavailableList
                                .stream()
                                .filter(unavailable -> {
                                    Boolean isCurrentSchdate = unavailable.getSchdate().equals(currentDate);
                                    Boolean isCurrentUno = unavailable.getUno().equals(sgbackup.getUno());
                                    return isCurrentSchdate && isCurrentUno;
                                })
                                .collect(Collectors.toList())
                                .size() == 0;
                            return isWorkday && isCurrentYear && isCurrentMonth && isAvailable;
                        })
                        .collect(Collectors.toList());
                    // 判斷是否為假日，並初始化各班別所需人數。如出勤人數不足，則從 OFF 班名單中選擇遞補人員。
                    if (isWeekend) {
                        // 週末 55 班
                        List<String> r55HolidayAvailableList = availableList
                            .stream()
                            .filter(sgbackup -> r55HolidayABCList.contains(sgbackup.getUno()))
                            .map(sgbackup -> sgbackup.getUno())
                            .collect(Collectors.toList());
                        List<String> unavailable55List = unavailableList
                            .stream()
                            .filter(unavailable -> {
                                Boolean isCurrentSchdate = unavailable.getSchdate().equals(currentDate);
                                Boolean isDayOff = unavailable.getClsno().equals("OFF");
                                Boolean isAvailable = sgbackupList
                                    .stream()
                                    .filter(sgbackup -> {
                                        Boolean isCurrentClsno = sgbackup.getClsno().equals("55");
                                        Boolean isCurrentYear = sgbackup.getSchdate().getYear() == monthStart.getYear();
                                        Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == monthStart.getMonthValue();
                                        Boolean isCurrentUno = sgbackup.getUno().equals(unavailable.getUno()) && r55HolidayABCList.contains(unavailable.getUno());
                                        return isCurrentClsno && isCurrentYear && isCurrentMonth && isCurrentUno;
                                    })
                                    .collect(Collectors.toList())
                                    .size() > 0;
                                return isCurrentSchdate && isDayOff && isAvailable;
                            })
                            .map(sgbackup -> sgbackup.getUno())
                            .collect(Collectors.toList());
                        int r55Manpower = 2;
                        if (r55HolidayAvailableList.size() < r55Manpower) {
                            int numberOfShortages = r55Manpower - r55HolidayAvailableList.size();
                            r55HolidayAvailableList.addAll(getRandomList(unavailable55List, numberOfShortages));
                        } else if (r55HolidayAvailableList.size() > r55Manpower) {
                            r55HolidayAvailableList = getRandomList(r55HolidayAvailableList, 2);
                        }
                        // 更新排班結果表
                        List<String> r55HolidayUnavailableList = new ArrayList<>();
                        for (Sgbackup sgbackup : sgbackupList) {
                            Boolean isCurrentClsno = sgbackup.getClsno().equals("55") || sgbackup.getClsno().equals("daily") ||(sgbackup.getClsno().equals("哺乳") && r55BreastFeedUnoList.contains(sgbackup.getUno()));
                            Boolean isCurrentYear = sgbackup.getSchdate().getYear() == monthStart.getYear();
                            Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == monthStart.getMonthValue();
                            if (isCurrentClsno && isCurrentYear && isCurrentMonth)
                                r55HolidayUnavailableList.add(sgbackup.getUno());
                        }
                        DayOfWeek weekendDay = DayOfWeek.of(currentDate.get(ChronoField.DAY_OF_WEEK));
                        Boolean weekendIsSaturday = weekendDay == DayOfWeek.SATURDAY;
                        Boolean weekendIsSunday = weekendDay == DayOfWeek.SUNDAY;
                        for (String currentUno : r55HolidayAvailableList) {
                            for (Sgresult sgresult : sgresultList) {
                                if (sgresult.getUno().equals(currentUno) && sgresult.getSchdate().equals(currentDate)) {
                                    sgresult.setClsno("55");
                                    sgresult.setClspr(8);
                                    r55HolidayUnavailableList.remove(sgresult.getUno());
                                }
                            }
                            // 週六上班，則下周一補休。
                            if (weekendIsSaturday) {
                                LocalDate nextMonday = currentDate.plusDays(2);
                                // 如有預公休，則將公休改為 OFF
                                for (Sgbackup sgbackup : sgbackupList) {
                                    Boolean isCurrentSchdate = sgbackup.getSchdate().equals(nextMonday);
                                    Boolean isCurrentUno = sgbackup.getUno().equals(currentUno);
                                    Boolean isAnnualLeave = sgbackup.getClsno().equals("公休");
                                    if (isCurrentSchdate && isCurrentUno && isAnnualLeave) {
                                        sgbackup.setClsno("OFF");
                                    }
                                }
                                // 如當天無排 OFF ，則新增 OFF
                                Boolean isDayOffExist = false;
                                for (Sgbackup sgbackup : sgbackupList) {
                                    Boolean isCurrentSchdate = sgbackup.getSchdate().equals(nextMonday);
                                    Boolean isCurrentUno = sgbackup.getUno().equals(currentUno);
                                    Boolean isDayOff = sgbackup.getClsno().equals("OFF");
                                    if (isCurrentSchdate && isCurrentUno && isDayOff) {
                                        isDayOffExist = true;
                                    }
                                }
                                if (!isDayOffExist) {
                                    sgbackupList.add(new Sgbackup(currentUno, nextMonday, "OFF"));
                                }
                                sgbackupRepository.saveAll(sgbackupList);
                            }
                            // 週日上班，則於本週五或週間內以預訂時間補休。
                            if (weekendIsSunday) {
                                LocalDate currentMonday = currentDate.minusDays(6);
                                LocalDate currentFriday = currentDate.minusDays(2);
                                List<LocalDate> daysOffSchdateList = new ArrayList<>();
                                for (
                                    LocalDate currentSchdate = currentMonday;
                                    currentSchdate.isBefore(currentDate.plusDays(1));
                                    currentSchdate = currentSchdate.plusDays(1)
                                ) {
                                    for (Sgresult sgresult : sgresultList) {
                                        Boolean isCurrentYear = currentSchdate.getYear() == currentDate.getYear();
                                        Boolean isCurrentMonth = currentSchdate.getMonthValue() == currentDate.getMonthValue();
                                        Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                        Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentSchdate);
                                        Boolean isDayOff = sgresult.getClsno().equals("OFF");
                                        if (isCurrentYear && isCurrentMonth && isCurrentUno && isCurrentSchdate && isDayOff) {
                                            daysOffSchdateList.add(currentSchdate);
                                        }
                                    }
                                }
                                if (daysOffSchdateList.size() < 2 && !daysOffSchdateList.contains(currentFriday)) {
                                    List<String> r55UnoList = sgbackupList
                                        .stream()
                                        .filter(sgbackup -> {
                                            Boolean isCurrentYear = sgbackup.getSchdate().getYear() == currentDate.getYear();
                                            Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == currentDate.getMonthValue();
                                            Boolean isCurrentClsno = sgbackup.getClsno().equals("55") || (sgbackup.getClsno().equals("哺乳") && r55BreastFeedUnoList.contains(sgbackup.getUno()));
                                            return isCurrentYear && isCurrentMonth && isCurrentClsno;
                                        })
                                        .map(sgbackup -> sgbackup.getUno())
                                        .collect(Collectors.toList());
                                    Boolean isCurrentYear = currentFriday.getYear() == currentDate.getYear();
                                    Boolean isCurrentMonth = currentFriday.getMonthValue() == currentDate.getMonthValue();
                                    if (isCurrentYear && isCurrentMonth) {
                                        List<String> daysOffUnoList = sgresultList
                                            .stream()
                                            .filter(sgresult -> {
                                                Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentFriday);
                                                Boolean isCurrentClsno = sgresult.getClsno().equals("OFF");
                                                Boolean isCurrentUno = r55UnoList.contains(sgresult.getUno());
                                                return isCurrentSchdate && isCurrentClsno && isCurrentUno;
                                            })
                                            .map(sgresult -> sgresult.getUno())
                                            .collect(Collectors.toList());
                                        int random = new Random().nextInt(daysOffUnoList.size());
                                        String alternativeUno = daysOffUnoList.get(random);
                                        for (Sgresult sgresult : sgresultList) {
                                            Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                            Boolean isAlternativeUno = sgresult.getUno().equals(alternativeUno);
                                            Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentFriday);
                                            if (isCurrentSchdate) {
                                                if (isCurrentUno) sgresult.setClsno("OFF");
                                                if (isAlternativeUno) sgresult.setClsno("55");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        for (String currentUno : r55HolidayUnavailableList) {
                            for (Sgresult sgresult : sgresultList) {
                                if (sgresult.getUno().equals(currentUno) && sgresult.getSchdate().equals(currentDate)) {
                                    sgresult.setClsno("OFF");
                                    sgresult.setClspr(0);
                                }
                            }
                        }
                    } else {
                        // 平日 55 班
                        List<String> r55UnoList = sgbackupList
                            .stream()
                            .filter(sgbackup -> {
                                Boolean isCurrentClsno = sgbackup.getClsno().equals("55");
                                Boolean isCurrentYear = sgbackup.getSchdate().getYear() == monthStart.getYear();
                                Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == monthStart.getMonthValue();
                                return isCurrentClsno && isCurrentYear && isCurrentMonth;
                            })
                            .map(sgbackup -> sgbackup.getUno())
                            .collect(Collectors.toList());
                        List<String> r55CompensatoryUnoList = new ArrayList<>();
                        List<String> r55DayOffUnoList = new ArrayList<>();
                        Boolean isMonday = DayOfWeek.of(currentDate.get(ChronoField.DAY_OF_WEEK)) == DayOfWeek.MONDAY;
                        if (isMonday) {
                            r55CompensatoryUnoList = sgresultList
                                .stream()
                                .filter(sgresult -> {
                                    Boolean isCurrentYear = sgresult.getSchdate().getYear() == currentDate.getYear();
                                    Boolean isCurrentMonth = sgresult.getSchdate().getMonthValue() == currentDate.getMonthValue();
                                    Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentDate.minusDays(2));
                                    Boolean isCurrentClsno = sgresult.getClsno().equals("55");
                                    return isCurrentYear && isCurrentMonth && isCurrentSchdate && isCurrentClsno;
                                })
                                .map(sgresult -> sgresult.getUno())
                                .collect(Collectors.toList());
                            if(r55CompensatoryUnoList.size()>0){
                                for (String currentUno : r55CompensatoryUnoList) {
                                    for (Sgresult sgresult : sgresultList) {
                                        if (sgresult.getUno().equals(currentUno) && sgresult.getSchdate().equals(currentDate)) {
                                            sgresult.setClsno("OFF");
                                            sgresult.setClspr(0);
                                        }
                                    }
                                }
                            }
                        }else{
                            if (r55DaysOffUnoList.size() == r55UnoList.size()) r55DaysOffUnoList.clear();
                            r55UnoList.removeAll(r55DaysOffUnoList);
                            if(r55UnoList.size()>=2){
                                r55DayOffUnoList = getRandomList(r55UnoList, 2);
                            }else{
                                r55DayOffUnoList.addAll(r55UnoList);
                            }
                            r55DaysOffUnoList.addAll(r55DayOffUnoList);
                            if(r55DayOffUnoList.size()>0){
                                for (String currentUno : r55DayOffUnoList) {
                                    for (Sgresult sgresult : sgresultList) {
                                        if (sgresult.getUno().equals(currentUno) && sgresult.getSchdate().equals(currentDate)) {
                                            sgresult.setClsno("OFF");
                                            sgresult.setClspr(0);
                                        }
                                    }
                                }
                            }
                        }
                        List<String> r55AvailableList = new ArrayList<>();
                        for (Sgbackup sgbackup : availableList) {
                            Boolean isCurrentClsno = sgbackup.getClsno().equals("55")
                                || sgbackup.getClsno().equals("daily")
                                || (sgbackup.getClsno().equals("哺乳") && r55BreastFeedUnoList.contains(sgbackup.getUno()));
                            if (isCurrentClsno) {
                                Boolean isAvailable = !r55DayOffUnoList.contains(sgbackup.getUno());
                                if (isMonday) {
                                    isAvailable = !r55CompensatoryUnoList.contains(sgbackup.getUno());
                                }
                                if (isAvailable) r55AvailableList.add(sgbackup.getUno());
                            }
                        }
                        r55UnoList = sgbackupList
                            .stream()
                            .filter(sgbackup -> {
                                Boolean isCurrentClsno = sgbackup.getClsno().equals("55")
                                    || sgbackup.getClsno().equals("daily")
                                    || (sgbackup.getClsno().equals("哺乳") && r55BreastFeedUnoList.contains(sgbackup.getUno()));
                                Boolean isCurrentYear = sgbackup.getSchdate().getYear() == monthStart.getYear();
                                Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == monthStart.getMonthValue();
                                return isCurrentClsno && isCurrentYear && isCurrentMonth;
                            })
                            .map(sgbackup -> sgbackup.getUno())
                            .collect(Collectors.toList());
                        List<String> r55UnavailableList = new ArrayList<>();
                        for (Sgbackup sgbackup : unavailableList) {
                            Boolean isCurrentSchdate = sgbackup.getSchdate().equals(currentDate);
                            Boolean isCurrentClsno = sgbackup.getClsno().equals("OFF");
                            Boolean isCurrentUno = r55UnoList.contains(sgbackup.getUno());
                            if (isCurrentSchdate && isCurrentClsno && isCurrentUno) {
                                Boolean isAvailable = !r55DayOffUnoList.contains(sgbackup.getUno());
                                if (isMonday) {
                                    isAvailable = !r55CompensatoryUnoList.contains(sgbackup.getUno());
                                }
                                if (isAvailable)  r55UnavailableList.add(sgbackup.getUno());
                            }
                        }
                        int r55Manpower = r55RoomOpen * r55NeedManpower + r55Wait + r55Nurse + r55WorkStat + r55OPDOR + sgruserRegularList.size();
                        if (r55AvailableList.size() < r55Manpower) {
                            int numberOfShortages = r55Manpower - r55AvailableList.size();
                            r55AvailableList.addAll(getRandomList(r55UnavailableList, numberOfShortages));
                        } else if (r55AvailableList.size() > r55Manpower) {
                            r55AvailableList = getRandomList(r55AvailableList, r55Manpower);
                        }
                        // 更新排班結果表
                        for (String currentUno : r55AvailableList) {
                            for (Sgresult sgresult : sgresultList) {
                                if (sgresult.getUno().equals(currentUno) && sgresult.getSchdate().equals(currentDate)) {
                                    sgresult.setClsno("55");
                                    sgresult.setClspr(8);
                                }
                            }
                        }
                    }
                    // D6 班
                    List<String> rd6AvailableList = availableList
                        .stream()
                        .filter(sgbackup -> rd6ABCList.contains(sgbackup.getUno()))
                        .map(sgbackup -> sgbackup.getUno())
                        .collect(Collectors.toList());
                    List<String> rd6UnavailableList = unavailableList
                        .stream()
                        .filter(unavailable -> {
                            Boolean isCurrentSchdate = unavailable.getSchdate().equals(currentDate);
                            Boolean isDayOff = unavailable.getClsno().equals("OFF");
                            Boolean isAvailable = sgbackupList
                                .stream()
                                .filter(sgbackup -> {
                                    Boolean isCurrentClsno = sgbackup.getClsno().equals("D6");
                                    Boolean isCurrentYear = sgbackup.getSchdate().getYear() == monthStart.getYear();
                                    Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == monthStart.getMonthValue();
                                    Boolean isCurrentUno = sgbackup.getUno().equals(unavailable.getUno());
                                    return isCurrentClsno && isCurrentYear && isCurrentMonth && isCurrentUno;
                                })
                                .collect(Collectors.toList())
                                .size() > 0;
                            return isCurrentSchdate && isDayOff && isAvailable;
                        })
                        .map(sgbackup -> sgbackup.getUno())
                        .collect(Collectors.toList());
                    if (rd6AvailableList.size() < rd6Manpower) {
                        int numberOfShortages = rd6Manpower - rd6AvailableList.size();
                        rd6AvailableList.addAll(getRandomList(rd6UnavailableList, numberOfShortages));
                    } else if (rd6AvailableList.size() > rd6Manpower) {
                        rd6AvailableList = getRandomList(rd6AvailableList, rd6Manpower);
                    }
                    // 備分當前名單
                    List<String> rd6PerCourseBackupList = rd6PerCourseUnoList;
                    // 每月首日及之後每四天，皆執行隨機指派出勤人員。如在下一次隨機指派前，有人員請公休，則再找人頂替。
                    if (rd6PerCourseUnoList.size() == 0 || currentDate.equals(monthStart) || currentDate.minusDays(1).getDayOfMonth() % 4 == 0) {
                        rd6PerCourseUnoList.clear();
                        rd6PerCourseUnoList.addAll(rd6AvailableList);
                    } else {
                        List<String> rd6PerCourseAvailableList = new ArrayList<>();
                        for (String uno : rd6PerCourseUnoList) {
                            if (rd6AvailableList.contains(uno)) {
                                rd6PerCourseAvailableList.add(uno);
                            }
                        }
                        if (rd6PerCourseAvailableList.size() < rd6Manpower) {
                            int numberOfShortages = rd6Manpower - rd6PerCourseAvailableList.size();
                            rd6AvailableList.removeAll(rd6PerCourseAvailableList);
                            if (rd6AvailableList.size() >= numberOfShortages) {
                                rd6PerCourseAvailableList.addAll(getRandomList(rd6AvailableList, numberOfShortages));
                            } else if (rd6AvailableList.size() < numberOfShortages
                                && rd6AvailableList.size() > 0
                                && rd6UnavailableList.size() > 0
                            ) {
                                rd6PerCourseAvailableList.addAll(rd6AvailableList);
                                numberOfShortages = rd6Manpower - rd6PerCourseAvailableList.size() - rd6AvailableList.size();
                                rd6PerCourseAvailableList.addAll(getRandomList(rd6UnavailableList, numberOfShortages));
                            } else if (rd6AvailableList.size() == 0 && rd6UnavailableList.size() > 0) {
                                rd6PerCourseAvailableList.addAll(getRandomList(rd6UnavailableList, numberOfShortages));
                            }
                            rd6PerCourseUnoList = rd6PerCourseAvailableList;
                        }
                    }
                    // 更新排班結果表
                     rd6UnavailableList = sgbackupList
                         .stream()
                         .filter(sgbackup -> {
                             Boolean isCurrentClsno = sgbackup.getClsno().equals("D6");
                             Boolean isCurrentYear = sgbackup.getSchdate().getYear() == monthStart.getYear();
                             Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == monthStart.getMonthValue();
                             return isCurrentClsno && isCurrentYear && isCurrentMonth;
                         })
                         .map(sgbackup -> sgbackup.getUno())
                         .collect(Collectors.toList());
                    for (String currentUno : rd6PerCourseUnoList) {
                        for (Sgresult sgresult : sgresultList) {
                            if (sgresult.getUno().equals(currentUno) && sgresult.getSchdate().equals(currentDate)) {
                                sgresult.setClsno("D6");
                                sgresult.setClspr(8);
                                rd6UnavailableList.remove(currentUno);
                            }
                        }
                    }
                    for (String currentUno : rd6UnavailableList) {
                        for (Sgresult sgresult : sgresultList) {
                            if (sgresult.getUno().equals(currentUno) && sgresult.getSchdate().equals(currentDate)) {
                                sgresult.setClsno("OFF");
                                sgresult.setClspr(0);
                            }
                        }
                    }
                    // 恢復備分名單
                    if (!(rd6PerCourseUnoList.size() == 0 || currentDate.equals(monthStart) || currentDate.minusDays(1).getDayOfMonth() % 4 == 0)) {
                        rd6PerCourseUnoList = rd6PerCourseBackupList;
                    }

                    // A0 班
                    List<String> ra0AvailableList = availableList
                        .stream()
                        .filter(sgbackup -> ra0ABCList.contains(sgbackup.getUno()))
                        .map(sgbackup -> sgbackup.getUno())
                        .collect(Collectors.toList());
                    List<String> ra0UnavailableList = unavailableList
                        .stream()
                        .filter(unavailable -> {
                            Boolean isCurrentSchdate = unavailable.getSchdate().equals(currentDate);
                            Boolean isDayOff = unavailable.getClsno().equals("OFF");
                            Boolean isAvailable = sgbackupList
                                .stream()
                                .filter(sgbackup -> {
                                    Boolean isCurrentClsno = sgbackup.getClsno().equals("A0");
                                    Boolean isCurrentYear = sgbackup.getSchdate().getYear() == monthStart.getYear();
                                    Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == monthStart.getMonthValue();
                                    Boolean isCurrentUno = sgbackup.getUno().equals(unavailable.getUno());
                                    return isCurrentClsno && isCurrentYear && isCurrentMonth && isCurrentUno;
                                })
                                .collect(Collectors.toList())
                                .size() > 0;
                            return isCurrentSchdate && isDayOff && isAvailable;
                        })
                        .map(sgbackup -> sgbackup.getUno())
                        .collect(Collectors.toList());
                    if (ra0AvailableList.size() < ra0Manpower) {
                        int numberOfShortages = ra0Manpower - ra0AvailableList.size();
                        ra0AvailableList.addAll(getRandomList(ra0UnavailableList, numberOfShortages));
                    } else if (ra0AvailableList.size() > ra0Manpower) {
                        ra0AvailableList = getRandomList(ra0AvailableList, ra0Manpower);
                    }
                    // 備分當前名單
                    List<String> ra0PerCourseBackupList = ra0PerCourseUnoList;
                    // 每月首日及之後每四天，皆執行隨機指派出勤人員。如在下一次隨機指派前，有人員請公休，則再找人頂替。
                    if (ra0PerCourseUnoList.size() == 0 || currentDate.equals(monthStart) || currentDate.minusDays(1).getDayOfMonth() % 4 == 0) {
                        ra0PerCourseUnoList.clear();
                        ra0PerCourseUnoList.addAll(ra0AvailableList);
                    } else {
                        List<String> ra0PerCourseAvailableList = new ArrayList<>();
                        for (String uno : ra0PerCourseUnoList) {
                            if (ra0AvailableList.contains(uno)) {
                                ra0PerCourseAvailableList.add(uno);
                            }
                        }
                        if (ra0PerCourseAvailableList.size() < ra0Manpower) {
                            int numberOfShortages = ra0Manpower - ra0PerCourseAvailableList.size();
                            ra0AvailableList.removeAll(ra0PerCourseAvailableList);
                            if (ra0AvailableList.size() >= numberOfShortages) {
                                ra0PerCourseAvailableList.addAll(getRandomList(ra0AvailableList, numberOfShortages));
                            } else if (ra0AvailableList.size() < numberOfShortages
                                && ra0AvailableList.size() > 0
                                && ra0UnavailableList.size() > 0
                            ) {
                                ra0PerCourseAvailableList.addAll(ra0AvailableList);
                                numberOfShortages = ra0Manpower - ra0PerCourseAvailableList.size() - ra0AvailableList.size();
                                ra0PerCourseAvailableList.addAll(getRandomList(ra0UnavailableList, numberOfShortages));
                            } else if (ra0AvailableList.size() == 0 && ra0UnavailableList.size() > 0) {
                                ra0PerCourseAvailableList.addAll(getRandomList(ra0UnavailableList, numberOfShortages));
                            }
                            ra0PerCourseUnoList = ra0PerCourseAvailableList;
                        }
                    }
                    // 更新排班結果表
                    ra0UnavailableList = sgbackupList
                        .stream()
                        .filter(sgbackup -> {
                            Boolean isCurrentClsno = sgbackup.getClsno().equals("A0");
                            Boolean isCurrentYear = sgbackup.getSchdate().getYear() == monthStart.getYear();
                            Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == monthStart.getMonthValue();
                            return isCurrentClsno && isCurrentYear && isCurrentMonth;
                        })
                        .map(sgbackup -> sgbackup.getUno())
                        .collect(Collectors.toList());
                    for (String currentUno : ra0PerCourseUnoList) {
                        for (Sgresult sgresult : sgresultList) {
                            if (sgresult.getUno().equals(currentUno) && sgresult.getSchdate().equals(currentDate)) {
                                sgresult.setClsno("A0");
                                sgresult.setClspr(8);
                                ra0UnavailableList.remove(sgresult.getUno());
                            }
                        }
                    }
                    for (String currentUno : ra0UnavailableList) {
                        for (Sgresult sgresult : sgresultList) {
                            if (sgresult.getUno().equals(currentUno) && sgresult.getSchdate().equals(currentDate)) {
                                sgresult.setClsno("OFF");
                                sgresult.setClspr(0);
                            }
                        }
                    }
                    // 恢復備分名單
                    if (!(ra0PerCourseUnoList.size() == 0 || currentDate.equals(monthStart) || currentDate.minusDays(1).getDayOfMonth() % 4 == 0)) {
                        ra0PerCourseUnoList = ra0PerCourseBackupList;
                    }
                    // A8 班
                    if (isWeekend) {
                        for (Sgbackup sgbackup : sgbackupList) {
                            Boolean isCurrentClsno = sgbackup.getClsno().equals("A8")
                                || (sgbackup.getClsno().equals("哺乳") && sgbackup.getUno().equals(ra8BreastFeedUno));
                            if (isCurrentClsno) {
                                for (Sgresult sgresult : sgresultList) {
                                    Boolean isCurrentUno = sgresult.getUno().equals(sgbackup.getUno());
                                    Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentDate);
                                    if (isCurrentUno && isCurrentClsno && isCurrentSchdate) {
                                        sgresult.setClsno("OFF");
                                        sgresult.setClspr(0);
                                    }
                                }
                            }
                        }
                    } else {
                        List<String> ra8AvailableList = new ArrayList<>();
                        for (Sgbackup sgbackup : availableList) {
                            if (ra8ABCList.contains(sgbackup.getUno()) || sgbackup.getUno().equals(ra8BreastFeedUno)) {
                                ra8AvailableList.add(sgbackup.getUno());
                            }
                        }
                        List<String> unavailableA8List = new ArrayList<>();
                        for (Sgbackup unavailable : unavailableList) {
                            Boolean isCurrentSchdate = unavailable.getSchdate().equals(currentDate);
                            Boolean isDayOff = unavailable.getClsno().equals("OFF");
                            Boolean isAvailable = false;
                            for (Sgbackup sgbackup : sgbackupList) {
                                Boolean isCurrentClsno = sgbackup.getClsno().equals("A8")
                                    || (sgbackup.getClsno().equals("哺乳") && sgbackup.getUno().equals(ra8BreastFeedUno));
                                Boolean isCurrentYear = sgbackup.getSchdate().getYear() == monthStart.getYear();
                                Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == monthStart.getMonthValue();
                                Boolean isCurrentUno = sgbackup.getUno().equals(unavailable.getUno());
                                if (isCurrentClsno && isCurrentYear && isCurrentMonth && isCurrentUno)
                                    isAvailable = true;
                            }
                            if (isCurrentSchdate && isDayOff && isAvailable) {
                                unavailableA8List.add(unavailable.getUno());
                            }
                        }
                        if (ra8AvailableList.size() < ra8Manpower) {
                            int numberOfShortages = ra8Manpower - ra8AvailableList.size();
                            ra8AvailableList.addAll(getRandomList(unavailableA8List, numberOfShortages));
                        } else if (ra8AvailableList.size() > ra8Manpower) {
                            ra8AvailableList = getRandomList(ra8AvailableList, ra8Manpower);
                        }
                        // 更新排班結果表
                        for (String currentUno : ra8AvailableList) {
                            for (Sgresult sgresult : sgresultList) {
                                if (sgresult.getUno().equals(currentUno) && sgresult.getSchdate().equals(currentDate)) {
                                    sgresult.setClsno("A8");
                                    sgresult.setClspr(8);
                                }
                            }
                        }
                    }
                }
            }
            int currentYear = monthStart.getYear();
            String currentMonth = monthStart.getMonthValue() >= 10
                ? String.valueOf(monthStart.getMonthValue())
                : "0" + String.valueOf(monthStart.getMonthValue());
            int manpower = sgruserList.size() + sgruserRegularList.size() + sgruserBreastFeedList.size();
            // 獲取當月預估休假總天數以及當月實際休假總天數
            HashMap<String, Boolean> map = new VacationDayCalculate().yearVacationDay(currentYear);
            Set<String> keySet = map.keySet();
            int estimateDaysOff = keySet
                .stream()
                .filter(key -> map.get(key) && key.startsWith(currentMonth))
                .collect(Collectors.toList())
                .size() * manpower;
            int actualDaysOff = sgresultList
                .stream()
                .filter(sgresult -> {
                    Boolean isCurrentYear = sgresult.getSchdate().getYear() == monthStart.getYear();
                    Boolean isCurrentMonth = sgresult.getSchdate().getMonthValue() == monthStart.getMonthValue();
                    Boolean isDayOff = sgresult.getClsno().equals("OFF");
                    return isCurrentYear && isCurrentMonth && isDayOff;
                })
                .collect(Collectors.toList())
                .size();
            // 當月休假總天數若超過該月期上限，則將平日多出的 OFF 班改排入 A8 班別。
            if (actualDaysOff > estimateDaysOff) {
                int numberOfReplacements = actualDaysOff - estimateDaysOff;
                // 取出當月所有平日 OFF 班
                List<Sgresult> daysOffList = sgresultList
                    .stream()
                    .filter(sgresult -> {
                        LocalDate currentDate = sgresult.getSchdate();
                        Boolean isCurrentYear = currentDate.getYear() == firstDateOfTheMonth
                            .getYear();
                        Boolean isCurrentMonth = currentDate.getMonthValue() == firstDateOfTheMonth
                            .getMonthValue();
                        DayOfWeek day = DayOfWeek.of(currentDate.get(ChronoField.DAY_OF_WEEK));
                        Boolean isWeekday = day != DayOfWeek.SUNDAY && day != DayOfWeek.SATURDAY;
                        Boolean isDayOff = sgresult.getClsno().equals("OFF");
                        return isCurrentYear && isCurrentMonth && isWeekday && isDayOff;
                    })
                    .collect(Collectors.toList());
                // 在當月所有平日 OFF 班中隨機挑選，並替換成 A8 班，再更新排班結果表
                while (numberOfReplacements > 0) {
                    int random = new Random().nextInt(daysOffList.size());
                    Sgresult dayOff = daysOffList.remove(random);
                    for (Sgresult sgresult : sgresultList) {
                        Boolean isCurrentUno = dayOff.getUno().equals(sgresult.getUno());
                        Boolean isCurrentSchdate = dayOff.getSchdate().equals(sgresult.getSchdate());
                        if (isCurrentUno && isCurrentSchdate) {
//                            sgresult.setClsno("A8");
//                            sgresult.setClspr(8);
                        }
                    }
                    numberOfReplacements--;
                }
            }
            // 取出本月出勤 55 班人員名單(常日班別不列入加班順序)。
            List<String> uno55List = sgbackupList
                .stream()
                .filter(sgbackup -> {
                    Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == monthStart.getMonthValue();
                    Boolean isCurrentClsno = sgbackup.getClsno().equals("55");
                    return isCurrentMonth && isCurrentClsno;
                })
                .map(sgbackup -> sgbackup.getUno())
                .collect(Collectors.toList());
            // 依序處理本月每日加班人員順序。
            for (LocalDate currentSchdate = monthStart; currentSchdate.isBefore(monthEnd.plusDays(1)); currentSchdate = currentSchdate.plusDays(1)) {
                // 以當前本月 55 班人員名單索引號為加班順序。
                for (int uno55Index = 0; uno55Index < uno55List.size(); uno55Index++) {
                    for (Sgresult sgresult : sgresultList) {
                        Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentSchdate);
                        Boolean isCurrentUno = sgresult.getUno().equals(uno55List.get(uno55Index));
                        if (isCurrentSchdate && isCurrentUno) {
                            sgresult.setOvertime(uno55Index + 1);
                        }
                    }
                }
                // 重新排序本月 55 班人員名單: 將首位人員編排至序列末位，並由次位人員遞補首位空缺。
                Collections.rotate(uno55List, -1);
            }
            sgresultRepository.saveAll(sgresultList);
        }
        sgresultRepository.saveAll(sgresultList);
        sgbackupRepository.saveAll(sgbackupList);
    }

    @GetMapping("/solve")
    public String solve(LocalDate startSchdate, LocalDate endSchdate) throws
        ExecutionException, InterruptedException {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                stopSolving();
                cleanSgbackupAndSgresult(startSchdate, endSchdate);
                backupSgsch(startSchdate, endSchdate);
                init(startSchdate, endSchdate);
                schedulingService.setSchdate(startSchdate, endSchdate);
                solverManager.solveAndListen(SchedulingService.PROBLEM_ID, schedulingService::findById,
                    schedulingService::save);
                syncSgsch(startSchdate, endSchdate);
                int sleepTime = Integer.parseInt(StringUtils.chop(spentLimit)) * 1000;
                Thread.sleep(sleepTime);
                return "success";
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        return future.get();
    }

    public SolverStatus getSolverStatus() {
        return solverManager.getSolverStatus(SchedulingService.PROBLEM_ID);
    }

    @PostMapping("/stopSolving")
    public String stopSolving() {
        solverManager.terminateEarly(SchedulingService.PROBLEM_ID);
        return "Success";
    }

}
