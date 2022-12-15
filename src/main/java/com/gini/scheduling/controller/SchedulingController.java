package com.gini.scheduling.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gini.scheduling.dao.*;
import com.gini.scheduling.exception.RestExceptionHandler;
import com.gini.scheduling.model.*;
import com.gini.scheduling.utils.DateGenerator;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import net.minidev.json.JSONObject;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping
public class SchedulingController extends RestExceptionHandler {
    public static final Logger logger = LoggerFactory.getLogger(SchedulingController.class);
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
    @Value("${spring.getoffinfo.url}")
    private String GET_OFF_INFO_URL;
    @Value("${spring.apikey}")
    private String API_KEY;
    @Value("${spring.hid}")
    private String HID;
    @Value("${spring.apid}")
    private String APID;

    public static <T> List<T> getRandomList(List<T> sourceList, int total) {
        List<T> tempList = new ArrayList<T>();
        List<T> result = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            SecureRandom rand = new SecureRandom();
            rand.setSeed((new Date()).getTime());
            int random = rand.nextInt(sourceList.size());
            if (!tempList.contains(sourceList.get(random))) {
                tempList.add(sourceList.get(random));
                result.add(sourceList.remove(random));
            } else {
                i--;
            }
        }
        return result;
    }

    public boolean checkSpecialLeave(String uno, LocalDate startSchdate, LocalDate endSchdate) {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };
            SSLContext sslcontext = SSLContext.getInstance("SSL");
            sslcontext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext.getSocketFactory());
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext);
            CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
            Unirest.setHttpClient(httpclient);
            String url = GET_OFF_INFO_URL;
            String apiKey = API_KEY;
            String hid = HID;
            String apid = APID;
            JSONObject object = new JSONObject();
            object.put("hid", hid);
            object.put("apid", apid);
            object.put("userid", uno);
            object.put("begdate", startSchdate.toString());
            object.put("enddate", endSchdate.toString());
            HttpResponse<String> response = Unirest.post(url)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("KeyId", apiKey)
                .body(object.toString())
                .asString();
            if (response.getStatus() == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> result = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {
                });
                if (result.get("success").equals("Y")) {
                    List<Map<String, String>> resultList = (List<Map<String, String>>) result.get("resultList");
                    if (resultList.size() > 0) {
                        for (Map<String, String> resultMap : resultList) {
                            String userid = resultMap.get("USERID").trim();
                            String leaveType = resultMap.get("OFFTYPE").trim();
                            Boolean isCurrentUno = userid.equals(uno);
                            Boolean isCurrentLeaveType = leaveType.equals("02")
                                || leaveType.equals("04")
                                || leaveType.equals("06");
                            if (isCurrentUno && isCurrentLeaveType) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("UnknownHostException", e);
        }
        return false;
    }

    // 總人數檢查
    public ResponseEntity checkManpower() {
        List<Sgsys> sgsysList = sgsysRepository.findAll();
        int r55RoomOpen = 0;
        int r55NeedManpower = 0;
        int r55Holiday = 0;
        int r55Wait = 0;
        int r55Nurse = 0;
        int r55WorkStat = 0;
        int r55OPDOR = 0;
        int rd6Manpower = 0;
        int rd6Holiday = 0;
        int ra0Manpower = 0;
        int ra0Holiday = 0;
        int ra8Manpower = 0;
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
                case "ra0Manpower":
                    ra0Manpower = Integer.parseInt(sgsys.getVal());
                    break;
                case "ra0Holiday":
                    ra0Holiday = Integer.parseInt(sgsys.getVal());
                    break;
                case "ra8Manpower":
                    ra8Manpower = Integer.parseInt(sgsys.getVal());
                    break;
            }
        }
        int totalManpowerRequirement = r55RoomOpen * r55NeedManpower + r55Holiday + r55Wait + r55Nurse + r55WorkStat + r55OPDOR + rd6Manpower + rd6Holiday + ra0Manpower + ra0Holiday + ra8Manpower;
        int totalManpower = sgruserRepository.findAll().stream().filter(sgruser -> !sgruser.getUteam().equals("")).collect(Collectors.toList()).size();
        int numberOfManpowerShortages = totalManpowerRequirement - totalManpower;
        Map<String, String> result = new LinkedHashMap<>();
        if (numberOfManpowerShortages > 0) {
            result.put("httpStatusCode", "400");
            result.put("status", "NOT_FOUND");
            result.put("message", "總人數低於排班最低需求");
            result.put("debugMessage", "請再補 " + numberOfManpowerShortages + " 名人員");
            result.put("subErrors", "null");
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
        } else {
            return ResponseEntity
                .status(HttpStatus.OK)
                .body("");
        }
    }

    // 各組別人數檢查
    public ResponseEntity checkUteam() {
        int numberOfUteamA = 0;
        int numberOfUteamB = 0;
        int numberOfUteamC = 0;
        List<String> uteamList = sgruserRepository.findAll().stream().map(sgruser -> sgruser.getUteam()).collect(Collectors.toList());
        for (String uteam : uteamList) {
            if (uteam.equals("A")) numberOfUteamA++;
            if (uteam.equals("B")) numberOfUteamB++;
            if (uteam.equals("C")) numberOfUteamC++;
        }
        int minimumUteamABCRequirement = 5;
        int numberOfUteamAShortages = numberOfUteamA > 0
            ? minimumUteamABCRequirement - numberOfUteamA
            : minimumUteamABCRequirement;
        int numberOfUteamBShortages = numberOfUteamB > 0
            ? minimumUteamABCRequirement - numberOfUteamB
            : minimumUteamABCRequirement;
        int numberOfUteamCShortages = numberOfUteamC > 0
            ? minimumUteamABCRequirement - numberOfUteamC
            : minimumUteamABCRequirement;
        String debugMessage = "";
        if (numberOfUteamAShortages > 0)
            debugMessage = debugMessage.concat("A 組尚缺 " + numberOfUteamAShortages + " 名人員, ");
        if (numberOfUteamBShortages > 0)
            debugMessage = debugMessage.concat("B 組尚缺 " + numberOfUteamBShortages + " 名人員, ");
        if (numberOfUteamCShortages > 0)
            debugMessage = debugMessage.concat("C 組尚缺 " + numberOfUteamCShortages + " 名人員, ");
        Map<String, String> result = new LinkedHashMap<>();
        if (!debugMessage.equals("")) {
            result.put("httpStatusCode", "400");
            result.put("status", "NOT_FOUND");
            result.put("message", "組別人數低於排班最低需求");
            result.put("debugMessage", debugMessage.trim());
            result.put("subErrors", "null");
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(result);
        } else {
            return ResponseEntity
                .status(HttpStatus.OK)
                .body("");
        }
    }

    // R6、A0 排班設置: 含四天一組、過剩假數檢查等功能。
    public void setShift(List<Sgresult> sgresultList, List<Sgresult> lastMonthSgresultList, List<String> lastMonthUnoList, List<String> lastMonthA0AndD6UnoList, List<String> unoList, LocalDate currentSchdate, String clsno) {
        LocalDate monthStart = currentSchdate;
        LocalDate monthEnd = YearMonth.from(monthStart).atEndOfMonth();
        LocalDate lastMonthStart = currentSchdate.minusMonths(1);
        LocalDate lastMonthEnd = YearMonth.from(lastMonthStart).atEndOfMonth();
        List<Integer> shift1 = new ArrayList<>();
        List<Integer> shift2 = new ArrayList<>();
        List<Integer> shift3 = new ArrayList<>();
        // 上月輪班人員
        String lastMonthUno1 = "";
        String lastMonthUno2 = "";
        String lastMonthUno3 = "";
        Map<String, Integer> occupationMap = new HashMap<String, Integer>();
        if (lastMonthUnoList.size() > 0) {
            lastMonthUno1 = lastMonthUnoList.get(0);
            lastMonthUno2 = lastMonthUnoList.get(1);
            lastMonthUno3 = lastMonthUnoList.get(2);
            // 預設上月佔用天數為 0
            occupationMap.put("occupation1", 0);
            occupationMap.put("occupation2", 0);
            occupationMap.put("occupation3", 0);
            occupationMap.put("singleDayOff1", 0);
            occupationMap.put("singleDayOff2", 0);
            occupationMap.put("singleDayOff3", 0);
            List<Sgresult> lastMonthUno1ShiftList = new ArrayList<>();
            List<Sgresult> lastMonthUno2ShiftList = new ArrayList<>();
            List<Sgresult> lastMonthUno3ShiftList = new ArrayList<>();
            List<Sgresult> lastMonthShiftList = sgresultRepository.findAllByDate(lastMonthStart, lastMonthEnd);
            for (Sgresult sgresult : lastMonthShiftList) {
                if (sgresult.getUno().equals(lastMonthUno1))
                    lastMonthUno1ShiftList.add(sgresult);
                if (sgresult.getUno().equals(lastMonthUno2))
                    lastMonthUno2ShiftList.add(sgresult);
                if (sgresult.getUno().equals(lastMonthUno3))
                    lastMonthUno3ShiftList.add(sgresult);
            }
            // 按日期排序
            lastMonthUno1ShiftList.sort(Comparator.comparing(Sgresult::getSchdate));
            lastMonthUno2ShiftList.sort(Comparator.comparing(Sgresult::getSchdate));
            lastMonthUno3ShiftList.sort(Comparator.comparing(Sgresult::getSchdate));
            // 取上月最後至多四天內出勤名單
            for (int index = lastMonthUno1ShiftList.size() - 1; index >= 0; index--) {
                if (lastMonthUno1ShiftList.get(index).getClsno().equals("OFF")
                    || (lastMonthUno1ShiftList.get(index).getClsno().equals("A8") && lastMonthA0AndD6UnoList.contains(lastMonthUno1ShiftList.get(index)))
                    || (lastMonthUno1ShiftList.get(index).getClsno().equals("55") && lastMonthA0AndD6UnoList.contains(lastMonthUno1ShiftList.get(index)))
                ) {
                    if (!(lastMonthUno1ShiftList.get(index - 1).getClsno().equals("OFF")
                        || (lastMonthUno1ShiftList.get(index - 1).getClsno().equals("A8") && lastMonthA0AndD6UnoList.contains(lastMonthUno1ShiftList.get(index)))
                        || (lastMonthUno1ShiftList.get(index - 1).getClsno().equals("55") && lastMonthA0AndD6UnoList.contains(lastMonthUno1ShiftList.get(index))))
                    ) {
                        occupationMap.put("singleDayOff1", occupationMap.get("singleDayOff1") + 1);
                    }
                    break;
                } else {
                    occupationMap.put("occupation1", occupationMap.get("occupation1") + 1);
                }
            }
            for (int index = lastMonthUno2ShiftList.size() - 1; index >= 0; index--) {
                if (lastMonthUno2ShiftList.get(index).getClsno().equals("OFF")
                    || (lastMonthUno2ShiftList.get(index).getClsno().equals("A8") && lastMonthA0AndD6UnoList.contains(lastMonthUno2ShiftList.get(index)))
                    || (lastMonthUno2ShiftList.get(index).getClsno().equals("55") && lastMonthA0AndD6UnoList.contains(lastMonthUno2ShiftList.get(index)))
                ) {
                    if (!(lastMonthUno2ShiftList.get(index - 1).getClsno().equals("OFF")
                        || (lastMonthUno2ShiftList.get(index - 1).getClsno().equals("A8") && lastMonthA0AndD6UnoList.contains(lastMonthUno2ShiftList.get(index)))
                        || (lastMonthUno2ShiftList.get(index - 1).getClsno().equals("55") && lastMonthA0AndD6UnoList.contains(lastMonthUno2ShiftList.get(index))))
                    ) {
                        occupationMap.put("singleDayOff2", occupationMap.get("singleDayOff2") + 1);
                    }
                    break;
                } else {
                    occupationMap.put("occupation2", occupationMap.get("occupation2") + 1);
                }
            }
            for (int index = lastMonthUno3ShiftList.size() - 1; index >= 0; index--) {
                if (lastMonthUno3ShiftList.get(index).getClsno().equals("OFF")
                    || (lastMonthUno3ShiftList.get(index).getClsno().equals("A8") && lastMonthA0AndD6UnoList.contains(lastMonthUno3ShiftList.get(index)))
                    || (lastMonthUno3ShiftList.get(index).getClsno().equals("55") && lastMonthA0AndD6UnoList.contains(lastMonthUno3ShiftList.get(index)))
                ) {
                    if (!(lastMonthUno3ShiftList.get(index - 1).getClsno().equals("OFF")
                        || (lastMonthUno3ShiftList.get(index - 1).getClsno().equals("A8") && lastMonthA0AndD6UnoList.contains(lastMonthUno3ShiftList.get(index)))
                        || (lastMonthUno3ShiftList.get(index - 1).getClsno().equals("55") && lastMonthA0AndD6UnoList.contains(lastMonthUno3ShiftList.get(index))))
                    ) {
                        occupationMap.put("singleDayOff3", occupationMap.get("singleDayOff3") + 1);
                    }
                    break;
                } else {
                    occupationMap.put("occupation3", occupationMap.get("occupation3") + 1);
                }
            }
            // 計算上月名單將佔本月初多少工作天，以及本月當班人員起始工作日。
            int occupationStartSchdate1 = 0;
            int occupationStartSchdate2 = 0;
            int occupationStartSchdate3 = 0;
            int shiftStartSchdate1 = 0;
            int shiftStartSchdate2 = 0;
            int shiftStartSchdate3 = 0;
            if (occupationMap.get("occupation1") > 0) {
                occupationStartSchdate1 = 4 - occupationMap.get("occupation1");
                shiftStartSchdate1 = occupationStartSchdate1 + 3;
            } else {
                if (occupationMap.get("singleDayOff1") > 0) {
                    shiftStartSchdate1 = 2;
                } else {
                    shiftStartSchdate1 = 1;
                }
            }
            if (occupationMap.get("occupation2") > 0) {
                occupationStartSchdate2 = 4 - occupationMap.get("occupation2");
                shiftStartSchdate2 = occupationStartSchdate2 + 3;
            } else {
                if (occupationMap.get("singleDayOff2") > 0) {
                    shiftStartSchdate2 = 2;
                } else {
                    shiftStartSchdate2 = 1;
                }
            }
            if (occupationMap.get("occupation3") > 0) {
                occupationStartSchdate3 = 4 - occupationMap.get("occupation3");
                shiftStartSchdate3 = occupationStartSchdate3 + 3;
            } else {
                if (occupationMap.get("singleDayOff3") > 0) {
                    shiftStartSchdate3 = 2;
                } else {
                    shiftStartSchdate3 = 1;
                }
            }
            int shift1WorkdayCounter = 0;
            int shift2WorkdayCounter = 0;
            int shift3WorkdayCounter = 0;
            int shift1OffCounter = 0;
            int shift2OffCounter = 0;
            int shift3OffCounter = 0;
            for (LocalDate date = monthStart; date.isBefore(monthEnd.plusDays(1)); date = date.plusDays(1)) {
                if (occupationMap.get("occupation1") > 0 && date.getDayOfMonth() <= occupationStartSchdate1) {
                    for (Sgresult sgresult : sgresultList) {
                        Boolean isCurrentSchdate = sgresult.getSchdate().equals(date);
                        Boolean isCurrentUno = sgresult.getUno().equals(lastMonthUno1);
                        if (isCurrentSchdate && isCurrentUno) {
                            sgresult.setClsno(clsno);
                            sgresult.setClspr(8);
                        }
                    }
                } else if (date.getDayOfMonth() >= shiftStartSchdate1) {
                    if (shift1WorkdayCounter < 4 && shift1OffCounter == 0) {
                        shift1.add(date.getDayOfMonth());
                        shift1WorkdayCounter++;
                        if (shift1WorkdayCounter == 4) {
                            shift1WorkdayCounter = 0;
                            shift1OffCounter = 2;
                        }
                    } else {
                        shift1OffCounter--;
                    }
                }
                if (occupationMap.get("occupation2") > 0 && date.getDayOfMonth() <= occupationStartSchdate2) {
                    for (Sgresult sgresult : sgresultList) {
                        Boolean isCurrentSchdate = sgresult.getSchdate().equals(date);
                        Boolean isCurrentUno = sgresult.getUno().equals(lastMonthUno2);
                        if (isCurrentSchdate && isCurrentUno) {
                            sgresult.setClsno(clsno);
                            sgresult.setClspr(8);
                        }
                    }
                } else if (date.getDayOfMonth() >= shiftStartSchdate2) {
                    if (shift2WorkdayCounter < 4 && shift2OffCounter == 0) {
                        shift2.add(date.getDayOfMonth());
                        shift2WorkdayCounter++;
                        if (shift2WorkdayCounter == 4) {
                            shift2WorkdayCounter = 0;
                            shift2OffCounter = 2;
                        }
                    } else {
                        shift2OffCounter--;
                    }
                }
                if (occupationMap.get("occupation3") > 0 && date.getDayOfMonth() <= occupationStartSchdate3) {
                    for (Sgresult sgresult : sgresultList) {
                        Boolean isCurrentSchdate = sgresult.getSchdate().equals(date);
                        Boolean isCurrentUno = sgresult.getUno().equals(lastMonthUno3);
                        if (isCurrentSchdate && isCurrentUno) {
                            sgresult.setClsno(clsno);
                            sgresult.setClspr(8);
                        }
                    }
                } else if (date.getDayOfMonth() >= shiftStartSchdate3) {
                    if (shift3WorkdayCounter < 4 && shift3OffCounter == 0) {
                        shift3.add(date.getDayOfMonth());
                        shift3WorkdayCounter++;
                        if (shift3WorkdayCounter == 4) {
                            shift3WorkdayCounter = 0;
                            shift3OffCounter = 2;
                        }
                    } else {
                        shift3OffCounter--;
                    }
                }
            }
        } else {
            shift1 = Arrays.asList(1, 2, 3, 4, 7, 8, 9, 10, 13, 14, 15, 16, 19, 20, 21, 22, 25, 26, 27, 28, 31);
            shift2 = Arrays.asList(3, 4, 5, 6, 9, 10, 11, 12, 15, 16, 17, 18, 21, 22, 23, 24, 27, 28, 29, 30);
            shift3 = Arrays.asList(1, 2, 5, 6, 7, 8, 11, 12, 13, 14, 17, 18, 19, 20, 23, 24, 25, 26, 29, 30, 31);
        }
        String uno1 = unoList.get(0);
        String uno2 = unoList.get(1);
        String uno3 = unoList.get(2);
        if (lastMonthSgresultList.size() > 0) {
            int firstDateOfShift1 = shift1.get(0);
            int firstDateOfShift2 = shift2.get(0);
            int firstDateOfShift3 = shift3.get(0);
            List<Sgresult> lastMonthUno1ShiftList = new ArrayList<>();
            List<Sgresult> lastMonthUno2ShiftList = new ArrayList<>();
            List<Sgresult> lastMonthUno3ShiftList = new ArrayList<>();
            for (Sgresult sgresult : lastMonthSgresultList) {
                if (sgresult.getUno().equals(uno1)) lastMonthUno1ShiftList.add(sgresult);
                if (sgresult.getUno().equals(uno2)) lastMonthUno2ShiftList.add(sgresult);
                if (sgresult.getUno().equals(uno3)) lastMonthUno3ShiftList.add(sgresult);
            }
            // 按日期排序
            lastMonthUno1ShiftList.sort(Comparator.comparing(Sgresult::getSchdate));
            lastMonthUno2ShiftList.sort(Comparator.comparing(Sgresult::getSchdate));
            lastMonthUno3ShiftList.sort(Comparator.comparing(Sgresult::getSchdate));
            Map<String, List<Sgresult>> unoOccupationMap = new HashMap<>();
            unoOccupationMap.put("uno1Occupation", lastMonthUno1ShiftList);
            unoOccupationMap.put("uno2Occupation", lastMonthUno2ShiftList);
            unoOccupationMap.put("uno3Occupation", lastMonthUno3ShiftList);
            String tempUno1 = "";
            String tempUno2 = "";
            String tempUno3 = "";
            for (Map.Entry<String, List<Sgresult>> entry : unoOccupationMap.entrySet()) {
                List<Sgresult> lastMonthList = entry.getValue();
                if (lastMonthList.size() > 0) {
                    int unoOccupation = 0;
                    if (lastMonthList.get(lastMonthList.size() - 1).getClsno().equals("D6")) {
                        unoOccupation++;
                        if (lastMonthList.get(lastMonthList.size() - 2).getClsno().equals("D6")) {
                            unoOccupation++;
                            if (lastMonthList.get(lastMonthList.size() - 3).getClsno().equals("D6")) {
                                unoOccupation++;
                                if (lastMonthList.get(lastMonthList.size() - 4).getClsno().equals("D6")) {
                                    unoOccupation++;
                                }
                            }
                        }
                    }
                    if (lastMonthList.get(lastMonthList.size() - 1).getClsno().equals("A0")) {
                        unoOccupation++;
                        if (lastMonthList.get(lastMonthList.size() - 2).getClsno().equals("A0")) {
                            unoOccupation++;
                            if (lastMonthList.get(lastMonthList.size() - 3).getClsno().equals("A0")) {
                                unoOccupation++;
                                if (lastMonthList.get(lastMonthList.size() - 4).getClsno().equals("A0")) {
                                    unoOccupation++;
                                }
                            }
                        }
                    }
                    if (unoOccupation > 0) {
                        unoOccupation = (4 - unoOccupation) + 3;
                        if (entry.getKey().equals("uno1Occupation")) {
                            if (unoOccupation == firstDateOfShift1) tempUno1 = uno1;
                            if (unoOccupation == firstDateOfShift2) tempUno2 = uno1;
                            if (unoOccupation == firstDateOfShift3) tempUno3 = uno1;
                        }
                        if (entry.getKey().equals("uno2Occupation")) {
                            if (unoOccupation == firstDateOfShift1) tempUno1 = uno2;
                            if (unoOccupation == firstDateOfShift2) tempUno2 = uno2;
                            if (unoOccupation == firstDateOfShift3) tempUno3 = uno2;
                        }
                        if (entry.getKey().equals("uno3Occupation")) {
                            if (unoOccupation == firstDateOfShift1) tempUno1 = uno3;
                            if (unoOccupation == firstDateOfShift2) tempUno2 = uno3;
                            if (unoOccupation == firstDateOfShift3) tempUno3 = uno3;
                        }
                    }
                }
            }
            uno1 = tempUno1;
            uno2 = tempUno2;
            uno3 = tempUno3;
            List<String> currentUnoList = new ArrayList<>(unoList);
            currentUnoList.remove(uno1);
            currentUnoList.remove(uno2);
            currentUnoList.remove(uno3);
            if (uno1.equals("")) uno1 = getRandomList(currentUnoList, 1).get(0);
            if (uno2.equals("")) uno2 = getRandomList(currentUnoList, 1).get(0);
            if (uno3.equals("")) uno3 = getRandomList(currentUnoList, 1).get(0);
        }
        List<LocalDate> occupationSchdate1 = new ArrayList<>();
        List<LocalDate> occupationSchdate2 = new ArrayList<>();
        List<LocalDate> occupationSchdate3 = new ArrayList<>();
        if (lastMonthUnoList.size() > 0) {
            for (Sgresult sgresult : sgresultList) {
                Boolean isCurrentYear = sgresult.getSchdate().getYear() == currentSchdate.getYear();
                Boolean isCurrentMonth = sgresult.getSchdate().getMonthValue() == currentSchdate.getMonthValue();
                Boolean isCurrentClsno = sgresult.getClsno().equals(clsno);
                if (isCurrentYear && isCurrentMonth && isCurrentClsno) {
                    Boolean isUno1 = sgresult.getUno().equals(lastMonthUno1);
                    Boolean isUno2 = sgresult.getUno().equals(lastMonthUno2);
                    Boolean isUno3 = sgresult.getUno().equals(lastMonthUno3);
                    if (isUno1) occupationSchdate1.add(sgresult.getSchdate());
                    if (isUno2) occupationSchdate2.add(sgresult.getSchdate());
                    if (isUno3) occupationSchdate3.add(sgresult.getSchdate());
                }
            }
        }
        for (Sgresult sgresult : sgresultList) {
            Boolean isAnnualLeave = sgresult.getClsno().equals("公休");
            if (!isAnnualLeave) {
                Boolean isCurrentYear = sgresult.getSchdate().getYear() == currentSchdate.getYear();
                Boolean isCurrentMonth = sgresult.getSchdate().getMonthValue() == currentSchdate.getMonthValue();
                Boolean isShift1Day = shift1.contains(sgresult.getSchdate().getDayOfMonth());
                Boolean isShift2Day = shift2.contains(sgresult.getSchdate().getDayOfMonth());
                Boolean isShift3Day = shift3.contains(sgresult.getSchdate().getDayOfMonth());
                Boolean isUno1 = sgresult.getUno().equals(uno1);
                Boolean isUno2 = sgresult.getUno().equals(uno2);
                Boolean isUno3 = sgresult.getUno().equals(uno3);
                if (isCurrentYear
                    && isCurrentMonth
                    && (isShift1Day && isUno1)
                    || (isShift2Day && isUno2)
                    || (isShift3Day && isUno3)
                ) {
                    sgresult.setClsno(clsno);
                    sgresult.setClspr(8);
                }
                // 四號前被占用的班，改補 閒置 代替。
                if (lastMonthUnoList.size() > 0
                    && (isUno1 && !lastMonthUnoList.contains(uno1) && occupationSchdate1.contains(sgresult.getSchdate()) && !(sgresult.getClsno().equals("D6") || sgresult.getClsno().equals("A0")))
                    || (isUno2 && !lastMonthUnoList.contains(uno2) && occupationSchdate2.contains(sgresult.getSchdate()) && !(sgresult.getClsno().equals("D6") || sgresult.getClsno().equals("A0")))
                    || (isUno3 && !lastMonthUnoList.contains(uno3) && occupationSchdate3.contains(sgresult.getSchdate()) && !(sgresult.getClsno().equals("D6") || sgresult.getClsno().equals("A0")))
                ) {
                    sgresult.setClsno("閒置");
                    sgresult.setClspr(0);
                }
            }
        }

        for (Sgresult sgresult : sgresultList) {
            Boolean isAnnualLeave = sgresult.getClsno().equals("公休");
            if (!isAnnualLeave) {
                Boolean isCurrentYear = sgresult.getSchdate().getYear() == currentSchdate.getYear();
                Boolean isCurrentMonth = sgresult.getSchdate().getMonthValue() == currentSchdate.getMonthValue();
                Boolean isCurrentClsno = sgresult.getClsno().equals("");
                Boolean isCurrentUno = sgresult.getUno().equals(uno1)
                    || sgresult.getUno().equals(uno2)
                    || sgresult.getUno().equals(uno3);
                if (isCurrentYear
                    && isCurrentMonth
                    && isCurrentClsno
                    && isCurrentUno
                ) {
                    sgresult.setClsno("OFF");
                    sgresult.setClspr(0);
                }
            }
        }
    }

    public void ra8Replacement(List<LocalDate> currentMonthNationalHolidaysList, List<Sgresult> sgresultList, List<Sgresult> lastMonthSgresultList, List<String> unoList, LocalDate monthStart, LocalDate monthEnd, String clsno) {
        // 當月實際假數如大於預估假數，則隨機將平日的 OFF 班轉 A8 班。
        int currentYear = monthStart.getYear();
        int currentMonth = monthStart.getMonthValue();
        // 獲取當月預估休假總天數以及當月實際休假總天數
        int totalWeekendDays = 0;
        for (LocalDate currentDate = monthStart; currentDate.isBefore(monthEnd.plusDays(1)); currentDate = currentDate.plusDays(1)) {
            DayOfWeek day = DayOfWeek.of(currentDate.get(ChronoField.DAY_OF_WEEK));
            if (day == DayOfWeek.SUNDAY || day == DayOfWeek.SATURDAY) totalWeekendDays++;
        }
        int estimateDaysOff = totalWeekendDays + currentMonthNationalHolidaysList.size();
        for (String uno : unoList) {
            List<Sgresult> weekDayList = new ArrayList<>();
            List<LocalDate> weekEndList = new ArrayList<>();
            int actualDaysOff = sgresultList.stream().filter(sgresult -> sgresult.getUno().equals(uno) && sgresult.getClsno().equals("OFF")).collect(Collectors.toList()).size();
            // 找出本月完整假日
            for (Sgresult sgresult1 : sgresultList) {
                for (Sgresult sgresult2 : sgresultList) {
                    Boolean isCurrentYear = sgresult1.getSchdate().getYear() == currentYear
                        && sgresult2.getSchdate().getYear() == currentYear;
                    Boolean isCurrentMonth = sgresult1.getSchdate().getMonthValue() == currentMonth
                        && sgresult2.getSchdate().getMonthValue() == currentMonth;
                    Boolean isCurrentSchdate = sgresult1.getSchdate().plusDays(1).equals(sgresult2.getSchdate())
                        && sgresult1.isWeekend()
                        && sgresult2.isWeekend();
                    Boolean isDayOff = sgresult1.getClsno().equals("OFF")
                        && sgresult2.getClsno().equals("OFF");
                    Boolean isCurrentUno = sgresult1.getUno().equals(uno)
                        && sgresult2.getUno().equals(uno);
                    if (isCurrentYear && isCurrentMonth && isCurrentSchdate && isDayOff && isCurrentUno) {
                        if (!weekEndList.contains(sgresult1.getSchdate())) {
                            weekEndList.add(sgresult1.getSchdate());
                        }
                        if (!weekEndList.contains(sgresult2.getSchdate())) {
                            weekEndList.add(sgresult2.getSchdate());
                        }
                    }
                }
            }
            for (Sgresult sgresult : sgresultList) {
                Boolean isCurrentYear = sgresult.getSchdate().getYear() == currentYear;
                Boolean isCurrentMonth = sgresult.getSchdate().getMonthValue() == currentMonth;
                Boolean isCurrentSchdate = !weekEndList.contains(sgresult.getSchdate());
                Boolean isDayOff = sgresult.getClsno().equals("OFF");
                Boolean isCurrentUno = sgresult.getUno().equals(uno);
                if (isCurrentYear && isCurrentMonth && isCurrentSchdate && isDayOff && isCurrentUno) {
                    weekDayList.add(sgresult);
                }
            }
            // 去除同週 OFF 班
            List<Sgresult> tempList = new ArrayList<>();
            for (int index = 0; index < weekDayList.size(); index++) {
                for (int index2 = 0; index2 < weekDayList.size(); index2++) {
                    Sgresult Sgresult1 = weekDayList.get(index);
                    Sgresult Sgresult2 = weekDayList.get(index2);
                    Boolean isCurrentSchdate = Sgresult1.getSchdate().plusDays(1).equals(Sgresult2.getSchdate());
                    if (isCurrentSchdate) {
                        tempList.add(Sgresult1);
                        tempList.add(Sgresult2);
                    }
                }
            }
            if (clsno.equals("A0")) {
                for (int index = 0; index < tempList.size(); index++) {
                    for (int index2 = 0; index2 < tempList.size(); index2++) {
                        Sgresult Sgresult1 = tempList.get(index);
                        Sgresult Sgresult2 = tempList.get(index2);
                        Boolean isCurrentClsno =
                            Sgresult1.getSchdate().plusDays(1).equals(Sgresult2.getSchdate()) && Sgresult2.getClsno().equals("OFF");
                        if (isCurrentClsno) tempList.remove(index2);
                    }
                }
            }
            if (clsno.equals("D6")) {
                for (int index = 0; index < tempList.size(); index++) {
                    for (int index2 = 0; index2 < tempList.size(); index2++) {
                        Sgresult Sgresult1 = tempList.get(index);
                        Sgresult Sgresult2 = tempList.get(index2);
                        Boolean isCurrentClsno =
                            Sgresult1.getSchdate().plusDays(1).equals(Sgresult2.getSchdate()) && Sgresult2.getClsno().equals("OFF");
                        if (isCurrentClsno) tempList.remove(index);
                    }
                }
            }
            weekDayList = tempList.stream().filter(sgresult -> !sgresult.isWeekend()).collect(Collectors.toList());
            if (actualDaysOff > estimateDaysOff) {
                int numberOfReplacements = actualDaysOff - estimateDaysOff;
                while (numberOfReplacements>0) {
                    SecureRandom rand = new SecureRandom();
                    rand.setSeed((new Date()).getTime());
                    int random = rand.nextInt(weekDayList.size());
                    LocalDate currentDate = weekDayList.remove(random).getSchdate();
                    for (Sgresult sgresult : sgresultList) {
                        Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentDate) && !currentDate.equals(monthEnd);
                        Boolean isCurrentClsno = sgresult.getClsno().equals("OFF");
                        Boolean isCurrentUno = sgresult.getUno().equals(uno);
                        if (isCurrentSchdate && isCurrentClsno && isCurrentUno) {
                            Boolean isAvailable = sgresultList
                                .stream()
                                .filter(sgresult1 -> {
                                    if (sgresult1.getUno().equals(uno)) {
                                        Boolean isTomorrow = sgresult1.getSchdate().equals(currentDate.plusDays(1));
                                        Boolean isYesterday = sgresult1.getSchdate().equals(currentDate.minusDays(1));
                                        Boolean isA0 = sgresult1.getClsno().equals("A0");
                                        Boolean isD6OrA8 = sgresult1.getClsno().equals("D6");
                                        if (isTomorrow && isA0) return true;
                                        if (isYesterday && isD6OrA8) return true;
                                    }
                                    return false;
                                })
                                .collect(Collectors.toList())
                                .size() == 0
                                && lastMonthSgresultList
                                .stream()
                                .filter(sgresult1 -> {
                                    Boolean isYesterday = sgresult1.getSchdate().equals(currentDate.minusDays(1));
                                    Boolean isD6OrA8 = sgresult1.getClsno().equals("D6");
                                    return isYesterday && sgresult1.getUno().equals(uno) && isD6OrA8;
                                })
                                .map(sgresult1 -> sgresult1.getUno())
                                .collect(Collectors.toList())
                                .size() == 0;
                            if (isAvailable && numberOfReplacements>0) {
                                sgresult.setClsno("A8");
                                sgresult.setClspr(8);
                                numberOfReplacements--;
                            }
                        }
                    }
                }
            }
        }
    }

    public void setOvertime(List<Sgresult> sgresultList, List<Sgbackup> sgbackupList, LocalDate startSchdate, LocalDate endSchdate) {
        // 取出本月出勤 55 班人員名單(常日班別不列入加班順序)。
        List<String> uno55List = sgbackupList
            .stream()
            .filter(sgbackup -> {
                Boolean isCurrentYear = sgbackup.getSchdate().getYear() == startSchdate.getYear();
                Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == startSchdate.getMonthValue();
                Boolean isCurrentClsno = sgbackup.getClsno().equals("55");
                return isCurrentYear && isCurrentMonth && isCurrentClsno;
            })
            .map(sgbackup -> sgbackup.getUno())
            .collect(Collectors.toList());
        // 依序處理本月每日加班人員順序。
        for (LocalDate currentSchdate = startSchdate; currentSchdate.isBefore(endSchdate.plusDays(1)); currentSchdate = currentSchdate.plusDays(1)) {
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

    // 清除當月人員排班結果表、人員排班備份表舊資料。
    public void cleanSgbackupAndSgresult(LocalDate startSchdate, LocalDate endSchdate) {
        sgbackupRepository.deleteALLByDate(startSchdate, endSchdate);
        sgresultRepository.deleteALLByDate(startSchdate, endSchdate);
    }

    // 清除人員排班表中，與人員主表內容不符的人員資料。
    public void cleanSgsch(LocalDate startSchdate, LocalDate endSchdate) {
        List<String> sgruserUnoList = sgruserRepository.findAll().stream().map(sgruser -> sgruser.getUno()).collect(Collectors.toList());
        List<Sgsch> sgschList = sgschRepository.findAllByDate(startSchdate, endSchdate);
        List<String> clearUnoList = new ArrayList<>();
        for (Sgsch sgsch : sgschList) {
            if (!sgruserUnoList.contains(sgsch.getUno()) && !clearUnoList.contains(sgsch.getUno())) {
                clearUnoList.add(sgsch.getUno());
            }
        }
        for (String uno : clearUnoList) {
            sgschRepository.deleteALLByDateAndUno(startSchdate, endSchdate, uno);
        }
    }

    // 備份當前人員排班表中的排假資料至人員排班備份表。
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

    // 同步人員排班結果表至人員排班表。
    public void syncSgsch(LocalDate startSchdate, LocalDate endSchdate) {
        List<Sgsch> sgschList = new ArrayList<>();
        List<Sgresult> sgresultList = sgresultRepository.findAllByDate(startSchdate, endSchdate);
        for (Sgresult sgresult : sgresultList) {
            String uno = sgresult.getUno();
            LocalDate schdate = sgresult.getSchdate();
            String clsno = sgresult.getClsno();
            int clspr = sgresult.getClspr();
            int overtime = sgresult.getOvertime();
            String remark = sgresult.getRemark();
            sgschList.add(new Sgsch(uno, schdate, clsno, clspr, overtime, remark));
        }
        sgschRepository.saveAll(sgschList);
    }

    // 啟動排班引擎，並將排班結果儲存至人員排班結果表。
    public void scheduling(LocalDate startSchdate, LocalDate endSchdate) {
        // 獲取本月期所有國定假日日期。
        List<LocalDate> nationalHolidaysList = sgsysRepository.findAllNationalHolidays()
            .stream()
            .map(nationalHoliday -> {
                int year = Integer.parseInt(nationalHoliday.substring(0, 4));
                int month = Integer.parseInt(nationalHoliday.substring(4, 6));
                int date = Integer.parseInt(nationalHoliday.substring(6, 8));
                return LocalDate.of(year, month, date);
            })
            .collect(Collectors.toList());
        // 獲取可用人力資訊，並排除不排班組。
        List<Sgruser> sgruserList = sgruserRepository.findAll();
        List<Sgruser> sgruserBreastFeedList = sgruserList
            .stream()
            .filter(sgruser -> sgruser.getUteam().equals("哺乳"))
            .collect(Collectors.toList());
        sgruserList.removeAll(sgruserBreastFeedList);
        List<Sgruser> sgruserRegularList = sgruserList
            .stream()
            .filter(sgruser -> {
                Boolean isCurrentUteam = sgruser.getUteam().equals("不排班");
                Boolean isSpecialLeave = checkSpecialLeave(sgruser.getUno(), startSchdate, endSchdate);
                return isCurrentUteam || isSpecialLeave;
            })
            .collect(Collectors.toList());
        sgruserList.removeAll(sgruserRegularList);
        List<Sgruser> allSgruserList = Stream
            .of(
                sgruserList,
                sgruserRegularList,
                sgruserBreastFeedList
            )
            .flatMap(x -> x.stream())
            .collect(Collectors.toList());
        List<String> allUnoList = allSgruserList
            .stream()
            .map(sgruser -> sgruser.getUno())
            .collect(Collectors.toList());
        List<String> civilServantList = allUnoList
            .stream()
            .filter(uno -> uno.substring(0, 1).equals("P"))
            .collect(Collectors.toList());
        List<String> laborList = allUnoList
            .stream()
            .filter(uno -> !civilServantList.contains(uno))
            .collect(Collectors.toList());
        List<Sgshift> sgshiftList = sgshiftRepository.findAll();
        List<Sgsys> sgsysList = sgsysRepository.findAll();
        long monthsBetween = ChronoUnit.MONTHS.between(LocalDate.parse(startSchdate.toString()),
            LocalDate.parse(endSchdate.toString()).plusDays(1));
        for (int i = 0; i < monthsBetween; i++) {
            List<Sgresult> sgresultList = new ArrayList<>();
            List<Sgbackup> sgbackupList = new ArrayList<>();
            // 本月日期區間
            LocalDate monthStart = startSchdate.plusMonths(i);
            LocalDate monthEnd = YearMonth.from(monthStart).atEndOfMonth();
            // 獲取當月所有國定假日
            List<LocalDate> currentMonthNationalHolidaysList = nationalHolidaysList
                .stream()
                .filter(NationalHoliday -> NationalHoliday.getMonthValue() == monthStart.getMonthValue())
                .collect(Collectors.toList());
            // 本月 uteam 備份
            for (Sgruser sgruser : sgruserList) {
                if (sgruser.getUteam().equals("A") || sgruser.getUteam().equals("B") || sgruser.getUteam().equals("C")) {
                    sgbackupList.add(new Sgbackup(sgruser.getUno(), monthStart, sgruser.getUteam()));
                }
            }
            // 上月日期區間
            LocalDate lastMonthStart = monthStart.minusMonths(1);
            LocalDate lastMonthEnd = YearMonth.from(lastMonthStart).atEndOfMonth();
            List<Sgbackup> lastMonthSgbackupList = sgbackupRepository.findAllByDate(lastMonthStart, lastMonthEnd);
            List<String> lastMonthUnoList = lastMonthSgbackupList
                .stream()
                .filter(sgbackup -> (sgbackup.getClsno().equals("55")
                    || sgbackup.getClsno().equals("A0")
                    || sgbackup.getClsno().equals("A8")
                    || sgbackup.getClsno().equals("D6"))
                )
                .map(sgbackup -> sgbackup.getUno())
                .collect(Collectors.toList());
            // 為方便本月初，接替離職人員班別的人員出勤，將上月末四天班別備份，並覆蓋接替人員上月對應日期。
            Map<String, List<Sgresult>> lastMonthSgresultMap = new LinkedHashMap<>();
            // 上月名單與本月名單不符時(如人員退休、離職)，清除與本月名單不符的上月人員資料，以利白班。
            if (lastMonthUnoList.size() > 0) {
                List<String> removeUnoList = lastMonthUnoList
                    .stream()
                    .filter(uno -> !allUnoList.contains(uno))
                    .collect(Collectors.toList());
                if (removeUnoList.size() > 0) {
                    for (String uno : removeUnoList) {
                        String clsno = lastMonthSgbackupList
                            .stream()
                            .filter(sgbackup -> sgbackup.getUno().equals(uno)
                                && (sgbackup.getClsno().equals("55")
                                || sgbackup.getClsno().equals("A0")
                                || sgbackup.getClsno().equals("A8")
                                || sgbackup.getClsno().equals("D6"))
                            )
                            .map(sgbackup -> sgbackup.getClsno())
                            .collect(Collectors.toList())
                            .get(0);
                        if (clsno.equals("A0") || clsno.equals("D6")) {
                            String uteam = lastMonthSgbackupList
                                .stream()
                                .filter(sgbackup -> sgbackup.getUno().equals(uno)
                                    && (sgbackup.getClsno().equals("A")
                                    || sgbackup.getClsno().equals("B")
                                    || sgbackup.getClsno().equals("C"))
                                )
                                .map(sgbackup -> sgbackup.getClsno())
                                .collect(Collectors.toList())
                                .get(0);
                            String clsnoUteam = clsno + "-" + uteam;
                            List<Sgresult> lastMonthSgresultList = sgresultRepository.findAllByUnoAndDate(uno, lastMonthEnd.minusDays(3), lastMonthEnd);
                            lastMonthSgresultMap.put(clsnoUteam, lastMonthSgresultList);
                        }
                        sgresultRepository.deleteALLByUnoAndDate(uno, lastMonthStart, lastMonthEnd);
                        lastMonthUnoList.remove(uno);
                    }
                }
            }
            List<Sgbackup> unavailableList = sgbackupRepository.findAllByDate(monthStart, monthEnd)
                .stream()
                .filter(sgbackup -> sgbackup.getClsno().equals("OFF") || sgbackup.getClsno().equals("公休"))
                .collect(Collectors.toList());
            List<DateGenerator.WeekInfo> weekList = new DateGenerator().getScope(
                String.valueOf(monthStart.getYear()),
                String.valueOf(monthStart.getMonth().getValue())
            );
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
            rd6ScheduledAList = rd6ScheduledAList.size() > 0 ? getRandomList(rd6ScheduledAList, 1) : rd6ScheduledAList;
            rd6ScheduledBList = rd6ScheduledBList.size() > 0 ? getRandomList(rd6ScheduledBList, 1) : rd6ScheduledBList;
            rd6ScheduledCList = rd6ScheduledCList.size() > 0 ? getRandomList(rd6ScheduledCList, 1) : rd6ScheduledCList;
            ra0ScheduledAList = ra0ScheduledAList.size() > 0 ? getRandomList(ra0ScheduledAList, 1) : ra0ScheduledAList;
            ra0ScheduledBList = ra0ScheduledBList.size() > 0 ? getRandomList(ra0ScheduledBList, 1) : ra0ScheduledBList;
            ra0ScheduledCList = ra0ScheduledCList.size() > 0 ? getRandomList(ra0ScheduledCList, 1) : ra0ScheduledCList;
            List<String> rd6ScheduledList = Stream
                .of(
                    rd6ScheduledAList,
                    rd6ScheduledBList,
                    rd6ScheduledCList
                )
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());
            List<String> ra0ScheduledList = Stream
                .of(
                    ra0ScheduledAList,
                    ra0ScheduledBList,
                    ra0ScheduledCList
                )
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());
            // 依組別取出各組人員名單，並排除包班、上月名單人員。
            List<String> sgruserAList = sgruserList
                .stream()
                .filter(sgruser -> {
                    if (rd6ScheduledList.contains(sgruser.getUno())
                        || ra0ScheduledList.contains(sgruser.getUno())
//                        || lastMonthD6UnoList.contains(sgruser.getUno())
//                        || lastMonthA0UnoList.contains(sgruser.getUno())
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
//                        || lastMonthD6UnoList.contains(sgruser.getUno())
//                        || lastMonthA0UnoList.contains(sgruser.getUno())
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
//                        || lastMonthD6UnoList.contains(sgruser.getUno())
//                        || lastMonthA0UnoList.contains(sgruser.getUno())
                    ) return false;
                    return sgruser.getUteam().equals("C");
                })
                .map(sgruser -> sgruser.getUno())
                .collect(Collectors.toList());
            // 獲取上月 D6、A0 名單。
            List<Sgresult> lastMonthSgresultList = sgresultRepository.findAllByDate(lastMonthStart, lastMonthEnd);
            List<String> lastMonthD6UnoAList = new ArrayList<>();
            List<String> lastMonthD6UnoBList = new ArrayList<>();
            List<String> lastMonthD6UnoCList = new ArrayList<>();
            List<String> lastMonthA0UnoAList = new ArrayList<>();
            List<String> lastMonthA0UnoBList = new ArrayList<>();
            List<String> lastMonthA0UnoCList = new ArrayList<>();
            List<String> lastMonthA0AndD6UnoList = new ArrayList<>();
            if (lastMonthSgresultList.size() > 0) {
                for (Sgbackup sgbackup : lastMonthSgbackupList) {
                    if (allUnoList.contains(sgbackup.getUno())) {
                        String uteam = "";
                        for (Sgruser sgruser : sgruserList) {
                            Boolean isCurrentUno = sgruser.getUno().equals(sgbackup.getUno());
                            if (isCurrentUno) {
                                uteam = sgruser.getUteam();
                            }
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
                }
                lastMonthD6UnoAList = lastMonthD6UnoAList.size() > 1 ? getRandomList(lastMonthD6UnoAList, 1) : lastMonthD6UnoAList;
                lastMonthD6UnoBList = lastMonthD6UnoBList.size() > 1 ? getRandomList(lastMonthD6UnoBList, 1) : lastMonthD6UnoBList;
                lastMonthD6UnoCList = lastMonthD6UnoCList.size() > 1 ? getRandomList(lastMonthD6UnoCList, 1) : lastMonthD6UnoCList;
                lastMonthA0UnoAList = lastMonthA0UnoAList.size() > 1 ? getRandomList(lastMonthA0UnoAList, 1) : lastMonthA0UnoAList;
                lastMonthA0UnoBList = lastMonthA0UnoBList.size() > 1 ? getRandomList(lastMonthA0UnoBList, 1) : lastMonthA0UnoBList;
                lastMonthA0UnoCList = lastMonthA0UnoCList.size() > 1 ? getRandomList(lastMonthA0UnoCList, 1) : lastMonthA0UnoCList;
                lastMonthSgresultList.sort(Comparator.comparing(Sgresult::getSchdate));
                if (lastMonthA0UnoAList.size() == 0) {
                    List<String> availableList = new ArrayList<>();
                    for (String uno : sgruserAList) {
                        for (Sgresult sgresult : lastMonthSgresultList) {
                            Boolean isCurrentUno = sgresult.getUno().equals(uno) && !lastMonthD6UnoAList.contains(uno);
                            Boolean isCurrentClsno = sgresult.getClsno().equals("OFF");
                            Boolean isCurrentSchdate = sgresult.getSchdate().equals(lastMonthEnd)
                                || sgresult.getSchdate().equals(lastMonthEnd.minusDays(1))
                                || sgresult.getSchdate().equals(lastMonthEnd.minusDays(2));
                            Boolean isAvailable = lastMonthSgresultList
                                .stream()
                                .filter(sgresult1 -> sgresult1.getUno().equals(sgresult.getUno()))
                                .count() > 0;
                            if (isCurrentUno
                                && isCurrentClsno
                                && isCurrentSchdate
                                && isAvailable
                                && !availableList.contains(sgresult.getUno())
                            ) {
                                availableList.add(sgresult.getUno());
                            }
                        }
                    }
                    if (availableList.size() > 0) {
                        String currentUno = getRandomList(availableList, 1).get(0);
                        if (!lastMonthSgresultMap.isEmpty()) {
                            List<Sgresult> extractSgresultList = lastMonthSgresultMap.get("A0-A");
                            if (extractSgresultList.size() > 0) {
                                for (Sgresult extractSgresult : extractSgresultList) {
                                    for (Sgresult sgresult : lastMonthSgresultList) {
                                        Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                        Boolean isCurrentSchdate = sgresult.getSchdate().equals(extractSgresult.getSchdate());
                                        if (isCurrentUno && isCurrentSchdate) {
                                            sgresult.setClsno(extractSgresult.getClsno());
                                            sgresult.setClspr(extractSgresult.getClspr());
                                        }
                                    }
                                }
                            }
                        }
                        lastMonthA0UnoAList.add(currentUno);
                    }
                } else {
                    lastMonthA0AndD6UnoList.addAll(lastMonthA0UnoAList);
                }
                if (lastMonthA0UnoBList.size() == 0) {
                    List<String> availableList = new ArrayList<>();
                    for (String uno : sgruserBList) {
                        for (Sgresult sgresult : lastMonthSgresultList) {
                            Boolean isCurrentUno = sgresult.getUno().equals(uno) && !lastMonthD6UnoBList.contains(uno);
                            Boolean isCurrentClsno = sgresult.getClsno().equals("OFF");
                            Boolean isCurrentSchdate = sgresult.getSchdate().equals(lastMonthEnd)
                                || sgresult.getSchdate().equals(lastMonthEnd.minusDays(1))
                                || sgresult.getSchdate().equals(lastMonthEnd.minusDays(2));
                            Boolean isAvailable = lastMonthSgresultList
                                .stream()
                                .filter(sgresult1 -> sgresult1.getUno().equals(sgresult.getUno()))
                                .count() > 0;
                            if (isCurrentUno
                                && isCurrentClsno
                                && isCurrentSchdate
                                && isAvailable
                                && !availableList.contains(sgresult.getUno())
                            ) {
                                availableList.add(sgresult.getUno());
                            }
                        }
                    }
                    if (availableList.size() > 0) {
                        String currentUno = getRandomList(availableList, 1).get(0);
                        if (!lastMonthSgresultMap.isEmpty()) {
                            List<Sgresult> extractSgresultList = lastMonthSgresultMap.get("A0-B");
                            if (extractSgresultList.size() > 0) {
                                for (Sgresult extractSgresult : extractSgresultList) {
                                    for (Sgresult sgresult : lastMonthSgresultList) {
                                        Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                        Boolean isCurrentSchdate = sgresult.getSchdate().equals(extractSgresult.getSchdate());
                                        if (isCurrentUno && isCurrentSchdate) {
                                            sgresult.setClsno(extractSgresult.getClsno());
                                            sgresult.setClspr(extractSgresult.getClspr());
                                        }
                                    }
                                }
                            }
                        }
                        lastMonthA0UnoBList.add(currentUno);
                    }
                } else {
                    lastMonthA0AndD6UnoList.addAll(lastMonthA0UnoBList);
                }
                if (lastMonthA0UnoCList.size() == 0) {
                    List<String> availableList = new ArrayList<>();
                    for (String uno : sgruserCList) {
                        for (Sgresult sgresult : lastMonthSgresultList) {
                            Boolean isCurrentUno = sgresult.getUno().equals(uno) && !lastMonthD6UnoCList.contains(uno);
                            Boolean isCurrentClsno = sgresult.getClsno().equals("OFF");
                            Boolean isCurrentSchdate = sgresult.getSchdate().equals(lastMonthEnd)
                                || sgresult.getSchdate().equals(lastMonthEnd.minusDays(1))
                                || sgresult.getSchdate().equals(lastMonthEnd.minusDays(2));
                            Boolean isAvailable = lastMonthSgresultList
                                .stream()
                                .filter(sgresult1 -> sgresult1.getUno().equals(sgresult.getUno()))
                                .count() > 0;
                            if (isCurrentUno
                                && isCurrentClsno
                                && isCurrentSchdate
                                && isAvailable
                                && !availableList.contains(sgresult.getUno())
                            ) {
                                availableList.add(sgresult.getUno());
                            }
                        }
                    }
                    if (availableList.size() > 0) {
                        String currentUno = getRandomList(availableList, 1).get(0);
                        if (!lastMonthSgresultMap.isEmpty()) {
                            List<Sgresult> extractSgresultList = lastMonthSgresultMap.get("A0-C");
                            if (extractSgresultList.size() > 0) {
                                for (Sgresult extractSgresult : extractSgresultList) {
                                    for (Sgresult sgresult : lastMonthSgresultList) {
                                        Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                        Boolean isCurrentSchdate = sgresult.getSchdate().equals(extractSgresult.getSchdate());
                                        if (isCurrentUno && isCurrentSchdate) {
                                            sgresult.setClsno(extractSgresult.getClsno());
                                            sgresult.setClspr(extractSgresult.getClspr());
                                        }
                                    }
                                }
                            }
                        }

                        lastMonthA0UnoCList.add(currentUno);
                    }
                } else {
                    lastMonthA0AndD6UnoList.addAll(lastMonthA0UnoCList);
                }
                if (lastMonthD6UnoAList.size() == 0) {
                    List<String> availableList = new ArrayList<>();
                    for (String uno : sgruserAList) {
                        for (Sgresult sgresult : lastMonthSgresultList) {
                            Boolean isCurrentUno = sgresult.getUno().equals(uno) && !lastMonthA0UnoAList.contains(uno);
                            Boolean isCurrentClsno = sgresult.getClsno().equals("OFF");
                            Boolean isCurrentSchdate = sgresult.getSchdate().equals(lastMonthEnd)
                                || sgresult.getSchdate().equals(lastMonthEnd.minusDays(1))
                                || sgresult.getSchdate().equals(lastMonthEnd.minusDays(2));
                            Boolean isAvailable = lastMonthSgresultList
                                .stream()
                                .filter(sgresult1 -> sgresult1.getUno().equals(sgresult.getUno()))
                                .count() > 0;
                            if (isCurrentUno
                                && isCurrentClsno
                                && isCurrentSchdate
                                && isAvailable
                                && !availableList.contains(sgresult.getUno())
                            ) {
                                availableList.add(sgresult.getUno());
                            }
                        }
                    }
                    if (availableList.size() > 0) {
                        String currentUno = getRandomList(availableList, 1).get(0);
                        if (!lastMonthSgresultMap.isEmpty()) {
                            List<Sgresult> extractSgresultList = lastMonthSgresultMap.get("D6-A");
                            if (extractSgresultList.size() > 0) {
                                for (Sgresult extractSgresult : extractSgresultList) {
                                    for (Sgresult sgresult : lastMonthSgresultList) {
                                        Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                        Boolean isCurrentSchdate = sgresult.getSchdate().equals(extractSgresult.getSchdate());
                                        if (isCurrentUno && isCurrentSchdate) {
                                            sgresult.setClsno(extractSgresult.getClsno());
                                            sgresult.setClspr(extractSgresult.getClspr());
                                        }
                                    }
                                }
                            }
                        }

                        lastMonthD6UnoAList.add(currentUno);
                    }
                } else {
                    lastMonthA0AndD6UnoList.addAll(lastMonthD6UnoAList);
                }
                if (lastMonthD6UnoBList.size() == 0) {
                    List<String> availableList = new ArrayList<>();
                    for (String uno : sgruserBList) {
                        for (Sgresult sgresult : lastMonthSgresultList) {
                            Boolean isCurrentUno = sgresult.getUno().equals(uno) && !lastMonthA0UnoBList.contains(uno);
                            Boolean isCurrentClsno = sgresult.getClsno().equals("OFF");
                            Boolean isCurrentSchdate = sgresult.getSchdate().equals(lastMonthEnd)
                                || sgresult.getSchdate().equals(lastMonthEnd.minusDays(1))
                                || sgresult.getSchdate().equals(lastMonthEnd.minusDays(2));
                            Boolean isAvailable = lastMonthSgresultList
                                .stream()
                                .filter(sgresult1 -> sgresult1.getUno().equals(sgresult.getUno()))
                                .count() > 0;
                            if (isCurrentUno
                                && isCurrentClsno
                                && isCurrentSchdate
                                && isAvailable
                                && !availableList.contains(sgresult.getUno())
                            ) {
                                availableList.add(sgresult.getUno());
                            }
                        }
                    }
                    if (availableList.size() > 0) {
                        String currentUno = getRandomList(availableList, 1).get(0);
                        if (!lastMonthSgresultMap.isEmpty()) {
                            List<Sgresult> extractSgresultList = lastMonthSgresultMap.get("D6-B");
                            if (extractSgresultList.size() > 0) {
                                for (Sgresult extractSgresult : extractSgresultList) {
                                    for (Sgresult sgresult : lastMonthSgresultList) {
                                        Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                        Boolean isCurrentSchdate = sgresult.getSchdate().equals(extractSgresult.getSchdate());
                                        if (isCurrentUno && isCurrentSchdate) {
                                            sgresult.setClsno(extractSgresult.getClsno());
                                            sgresult.setClspr(extractSgresult.getClspr());
                                        }
                                    }
                                }
                            }
                        }
                        lastMonthD6UnoBList.add(currentUno);
                    }
                } else {
                    lastMonthA0AndD6UnoList.addAll(lastMonthD6UnoBList);
                }
                if (lastMonthD6UnoCList.size() == 0) {
                    List<String> availableList = new ArrayList<>();
                    for (String uno : sgruserCList) {
                        for (Sgresult sgresult : lastMonthSgresultList) {
                            Boolean isCurrentUno = sgresult.getUno().equals(uno) && !lastMonthA0UnoCList.contains(uno);
                            Boolean isCurrentClsno = sgresult.getClsno().equals("OFF");
                            Boolean isCurrentSchdate = sgresult.getSchdate().equals(lastMonthEnd)
                                || sgresult.getSchdate().equals(lastMonthEnd.minusDays(1))
                                || sgresult.getSchdate().equals(lastMonthEnd.minusDays(2));
                            Boolean isAvailable = lastMonthSgresultList
                                .stream()
                                .filter(sgresult1 -> sgresult1.getUno().equals(sgresult.getUno()))
                                .count() > 0;
                            if (isCurrentUno
                                && isCurrentClsno
                                && isCurrentSchdate
                                && isAvailable
                                && !availableList.contains(sgresult.getUno())
                            ) {
                                availableList.add(sgresult.getUno());
                            }
                        }
                    }
                    if (availableList.size() > 0) {
                        String currentUno = getRandomList(availableList, 1).get(0);
                        if (!lastMonthSgresultMap.isEmpty()) {
                            List<Sgresult> extractSgresultList = lastMonthSgresultMap.get("D6-C");
                            if (extractSgresultList.size() > 0) {
                                for (Sgresult extractSgresult : extractSgresultList) {
                                    for (Sgresult sgresult : lastMonthSgresultList) {
                                        Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                        Boolean isCurrentSchdate = sgresult.getSchdate().equals(extractSgresult.getSchdate());
                                        if (isCurrentUno && isCurrentSchdate) {
                                            sgresult.setClsno(extractSgresult.getClsno());
                                            sgresult.setClspr(extractSgresult.getClspr());
                                        }
                                    }
                                }
                            }
                        }
                        lastMonthD6UnoCList.add(currentUno);
                    }
                } else {
                    lastMonthA0AndD6UnoList.addAll(lastMonthD6UnoCList);
                }
            }
            List<String> lastMonthD6UnoList = Stream
                .of(
                    lastMonthD6UnoAList,
                    lastMonthD6UnoBList,
                    lastMonthD6UnoCList
                )
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());
            sgruserAList.removeAll(lastMonthD6UnoList);
            sgruserBList.removeAll(lastMonthD6UnoList);
            sgruserCList.removeAll(lastMonthD6UnoList);
            List<String> lastMonthA0UnoList = Stream
                .of(
                    lastMonthA0UnoAList,
                    lastMonthA0UnoBList,
                    lastMonthA0UnoCList
                )
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());
            sgruserAList.removeAll(lastMonthA0UnoList);
            sgruserBList.removeAll(lastMonthA0UnoList);
            sgruserCList.removeAll(lastMonthA0UnoList);
            // 組別人數不足基本需求時，則從過剩的組裡取所需人數來彌補。
//            List<String> excessManpowerList = new ArrayList<>();
//            int minimumRequirements = 4;
//            if (sgruserAList.size() > minimumRequirements) {
//                excessManpowerList.addAll(sgruserAList.subList(0, sgruserAList.size() - minimumRequirements));
//            }
//            if (sgruserBList.size() > minimumRequirements) {
//                excessManpowerList.addAll(sgruserBList.subList(0, sgruserBList.size() - minimumRequirements));
//            }
//            if (sgruserCList.size() > minimumRequirements) {
//                excessManpowerList.addAll(sgruserCList.subList(0, sgruserCList.size() - minimumRequirements));
//            }
//            if (sgruserAList.size() < minimumRequirements) {
//                List<String> compensatoryManpowerList = excessManpowerList.subList(0, minimumRequirements - sgruserAList.size());
//                sgruserAList.addAll(compensatoryManpowerList);
//                sgruserBList.removeAll(compensatoryManpowerList);
//                sgruserCList.removeAll(compensatoryManpowerList);
//                excessManpowerList.removeAll(compensatoryManpowerList);
//            }
//            if (sgruserBList.size() < minimumRequirements) {
//                List<String> compensatoryManpowerList = excessManpowerList.subList(0, minimumRequirements - sgruserBList.size());
//                sgruserAList.removeAll(compensatoryManpowerList);
//                sgruserBList.addAll(compensatoryManpowerList);
//                sgruserCList.removeAll(compensatoryManpowerList);
//                excessManpowerList.removeAll(compensatoryManpowerList);
//            }
//            if (sgruserCList.size() < minimumRequirements) {
//                List<String> compensatoryManpowerList = excessManpowerList.subList(0, minimumRequirements - sgruserCList.size());
//                sgruserAList.removeAll(compensatoryManpowerList);
//                sgruserBList.removeAll(compensatoryManpowerList);
//                sgruserCList.addAll(compensatoryManpowerList);
//                excessManpowerList.removeAll(compensatoryManpowerList);
//            }
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
            sgruserAList.removeAll(ra0ABCList);
            sgruserBList.removeAll(ra0ABCList);
            sgruserCList.removeAll(ra0ABCList);
            // 計算 D6 班出勤人數，優先順序為: 包班名單 > 上月名單 > 隨機名單。
            if (rd6ScheduledAList.size() >= rd6ManpowerA) {
                lastMonthD6UnoAList.removeAll(rd6ScheduledAList);
            } else if (rd6ScheduledAList.size() < rd6ManpowerA) {
                int numberOfShortages = rd6ManpowerA - rd6ScheduledAList.size();
                if (lastMonthA0UnoAList.size() >= numberOfShortages) {
                    rd6ScheduledAList.addAll(getRandomList(lastMonthA0UnoAList, numberOfShortages));
                } else if (lastMonthA0UnoAList.size() > 0) {
                    rd6ScheduledAList.addAll(lastMonthA0UnoAList);
                    numberOfShortages = numberOfShortages - lastMonthA0UnoAList.size();
                    rd6ScheduledAList.addAll(getRandomList(sgruserAList, numberOfShortages));
                } else {
                    rd6ScheduledAList.addAll(getRandomList(sgruserAList, numberOfShortages));
                }
            }
            // 如上月 A0 班額度在編排至 D6 後尚有剩，則全數回歸 A 組名單中。
            if (lastMonthA0UnoAList.size() > 0) sgruserAList.addAll(lastMonthA0UnoAList);
            if (rd6ScheduledBList.size() >= rd6ManpowerB) {
                lastMonthD6UnoBList.removeAll(rd6ScheduledBList);
            } else if (rd6ScheduledBList.size() < rd6ManpowerB) {
                int numberOfShortages = rd6ManpowerB - rd6ScheduledBList.size();
                if (lastMonthA0UnoBList.size() >= numberOfShortages) {
                    rd6ScheduledBList.addAll(getRandomList(lastMonthA0UnoBList, numberOfShortages));
                } else if (lastMonthA0UnoBList.size() > 0) {
                    rd6ScheduledBList.addAll(lastMonthA0UnoBList);
                    numberOfShortages = numberOfShortages - lastMonthA0UnoBList.size();
                    rd6ScheduledBList.addAll(getRandomList(sgruserBList, numberOfShortages));
                } else {
                    rd6ScheduledBList.addAll(getRandomList(sgruserBList, numberOfShortages));
                }
            }
            // 如上月 A0 班額度在編排至 D6 後尚有剩，則全數回歸 B 組名單中。
            if (lastMonthA0UnoBList.size() > 0) sgruserBList.addAll(lastMonthA0UnoBList);
            if (rd6ScheduledCList.size() >= rd6ManpowerC) {
                lastMonthD6UnoCList.removeAll(rd6ScheduledCList);
            } else if (rd6ScheduledCList.size() < rd6ManpowerC) {
                int numberOfShortages = rd6ManpowerC - rd6ScheduledCList.size();
                if (lastMonthA0UnoCList.size() >= numberOfShortages) {
                    rd6ScheduledCList.addAll(getRandomList(lastMonthA0UnoCList, numberOfShortages));
                } else if (lastMonthA0UnoCList.size() > 0) {
                    rd6ScheduledCList.addAll(lastMonthA0UnoCList);
                    numberOfShortages = numberOfShortages - lastMonthA0UnoCList.size();
                    rd6ScheduledCList.addAll(getRandomList(sgruserCList, numberOfShortages));
                } else {
                    rd6ScheduledCList.addAll(getRandomList(sgruserCList, numberOfShortages));
                }
            }
            // 如上月 A0 班額度在編排至 D6 後尚有剩，則全數回歸 C 組名單中。
            if (lastMonthA0UnoCList.size() > 0) sgruserCList.addAll(lastMonthA0UnoCList);
            if (lastMonthSgresultList.size() > 0) {
                // D6 人員檢查，如本月 D6 班包括包班人員與上月 A0 班人員，則包班人員本月初的銜接班，不可與上月 A0 人員的銜接班相同天數，如有此清況發生，則於同組候選人員名單中再隨機挑選替補。
                List<Sgresult> lastMonthUno1ShiftList = new ArrayList<>();
                List<Sgresult> lastMonthUno2ShiftList = new ArrayList<>();
                List<Sgresult> lastMonthUno3ShiftList = new ArrayList<>();
                for (Sgresult sgresult : lastMonthSgresultList) {
                    if (sgresult.getUno().equals(rd6ScheduledAList.get(0))) lastMonthUno1ShiftList.add(sgresult);
                    if (sgresult.getUno().equals(rd6ScheduledBList.get(0))) lastMonthUno2ShiftList.add(sgresult);
                    if (sgresult.getUno().equals(rd6ScheduledCList.get(0))) lastMonthUno3ShiftList.add(sgresult);
                }
                // 按日期排序
                lastMonthUno1ShiftList.sort(Comparator.comparing(Sgresult::getSchdate));
                lastMonthUno2ShiftList.sort(Comparator.comparing(Sgresult::getSchdate));
                lastMonthUno3ShiftList.sort(Comparator.comparing(Sgresult::getSchdate));
                Map<String, List<Sgresult>> unoOccupationMap = new HashMap<>();
                unoOccupationMap.put("uno1Occupation", lastMonthUno1ShiftList);
                unoOccupationMap.put("uno2Occupation", lastMonthUno2ShiftList);
                unoOccupationMap.put("uno3Occupation", lastMonthUno3ShiftList);
                int uno1OccupationCounter = 0;
                int uno2OccupationCounter = 0;
                int uno3OccupationCounter = 0;
                for (Map.Entry<String, List<Sgresult>> entry : unoOccupationMap.entrySet()) {
                    List<Sgresult> lastMonthList = entry.getValue();
                    String currentOccupation = entry.getKey();
                    int unoOccupation = 0;
                    if (lastMonthList.get(lastMonthList.size() - 1).getClsno().equals("D6")) {
                        unoOccupation++;
                        if (lastMonthList.get(lastMonthList.size() - 2).getClsno().equals("D6")) {
                            unoOccupation++;
                            if (lastMonthList.get(lastMonthList.size() - 3).getClsno().equals("D6")) {
                                unoOccupation++;
                                if (lastMonthList.get(lastMonthList.size() - 4).getClsno().equals("D6")) {
                                    unoOccupation++;
                                }
                            }
                        }
                    }
                    if (lastMonthList.get(lastMonthList.size() - 1).getClsno().equals("A0")) {
                        unoOccupation++;
                        if (lastMonthList.get(lastMonthList.size() - 2).getClsno().equals("A0")) {
                            unoOccupation++;
                            if (lastMonthList.get(lastMonthList.size() - 3).getClsno().equals("A0")) {
                                unoOccupation++;
                                if (lastMonthList.get(lastMonthList.size() - 4).getClsno().equals("A0")) {
                                    unoOccupation++;
                                }
                            }
                        }
                    }
                    if (currentOccupation.equals("uno1Occupation")) uno1OccupationCounter = unoOccupation;
                    if (currentOccupation.equals("uno2Occupation")) uno2OccupationCounter = unoOccupation;
                    if (currentOccupation.equals("uno3Occupation")) uno3OccupationCounter = unoOccupation;
                }
                if (uno1OccupationCounter == uno2OccupationCounter
                    && uno1OccupationCounter > 0
                    && uno2OccupationCounter > 0
                ) {
                    if (lastMonthA0UnoList.contains(rd6ScheduledAList.get(0)) && rd6ScheduledList.contains(rd6ScheduledBList.get(0))) {
                        String newUno = getRandomList(sgruserAList, 1).get(0);
                        sgruserAList.add(rd6ScheduledAList.remove(0));
                        rd6ScheduledAList.add(newUno);
                    }
                    if (lastMonthA0UnoList.contains(rd6ScheduledBList.get(0)) && rd6ScheduledList.contains(rd6ScheduledAList.get(0))) {
                        String newUno = getRandomList(sgruserBList, 1).get(0);
                        sgruserBList.add(rd6ScheduledBList.remove(0));
                        rd6ScheduledBList.add(newUno);
                    }
                }
                if (uno1OccupationCounter == uno3OccupationCounter
                    && uno1OccupationCounter > 0
                    && uno3OccupationCounter > 0
                ) {
                    if (lastMonthA0UnoList.contains(rd6ScheduledAList.get(0)) && rd6ScheduledList.contains(rd6ScheduledCList.get(0))) {
                        String newUno = getRandomList(sgruserAList, 1).get(0);
                        sgruserAList.add(rd6ScheduledAList.remove(0));
                        rd6ScheduledAList.add(newUno);
                    }
                    if (lastMonthA0UnoList.contains(rd6ScheduledCList.get(0)) && rd6ScheduledList.contains(rd6ScheduledAList.get(0))) {
                        String newUno = getRandomList(sgruserCList, 1).get(0);
                        sgruserCList.add(rd6ScheduledCList.remove(0));
                        rd6ScheduledCList.add(newUno);
                    }
                }
                if (uno2OccupationCounter == uno3OccupationCounter
                    && uno2OccupationCounter > 0
                    && uno3OccupationCounter > 0
                ) {
                    if (lastMonthA0UnoList.contains(rd6ScheduledBList.get(0)) && rd6ScheduledList.contains(rd6ScheduledCList.get(0))) {
                        String newUno = getRandomList(sgruserBList, 1).get(0);
                        sgruserBList.add(rd6ScheduledBList.remove(0));
                        rd6ScheduledBList.add(newUno);
                    }
                    if (lastMonthA0UnoList.contains(rd6ScheduledCList.get(0)) && rd6ScheduledList.contains(rd6ScheduledBList.get(0))) {
                        String newUno = getRandomList(sgruserCList, 1).get(0);
                        sgruserCList.add(rd6ScheduledCList.remove(0));
                        rd6ScheduledCList.add(newUno);
                    }
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
            sgruserAList.removeAll(rd6ABCList);
            sgruserBList.removeAll(rd6ABCList);
            sgruserCList.removeAll(rd6ABCList);
            // 計算 55 班出勤人數，優先順序為: 上月名單 > 隨機名單。
            List<String> r55HolidayAList = getRandomList(
                sgruserAList
                    .stream()
                    .filter(uno -> {
                        Boolean isAvailable =
                            !lastMonthA0UnoList.contains(uno)
                                && !lastMonthD6UnoList.contains(uno)
                                && lastMonthSgresultList
                                .stream()
                                .filter(sgresult -> {
                                    Boolean isCurrentSchdate = sgresult.getSchdate().equals(lastMonthEnd);
                                    Boolean isCurrentClsno = sgresult.getClsno().equals("A8");
                                    Boolean isCurrentUno = sgresult.getUno().equals(uno);
                                    return isCurrentSchdate && isCurrentClsno && isCurrentUno;
                                })
                                .collect(Collectors.toList())
                                .size() == 0;
                        return isAvailable;
                    })
                    .collect(Collectors.toList()),
                r55HolidayA
            );
            List<String> r55HolidayBList = getRandomList(
                sgruserBList
                    .stream()
                    .filter(uno -> {
                        Boolean isAvailable =
                            !lastMonthA0UnoList.contains(uno)
                                && !lastMonthD6UnoList.contains(uno)
                                && lastMonthSgresultList
                                .stream()
                                .filter(sgresult -> {
                                    Boolean isCurrentSchdate = sgresult.getSchdate().equals(lastMonthEnd);
                                    Boolean isCurrentClsno = sgresult.getClsno().equals("A8");
                                    Boolean isCurrentUno = sgresult.getUno().equals(uno);
                                    return isCurrentSchdate && isCurrentClsno && isCurrentUno;
                                })
                                .collect(Collectors.toList())
                                .size() == 0;
                        return isAvailable;
                    })
                    .collect(Collectors.toList()),
                r55HolidayB
            );
            List<String> r55HolidayCList = getRandomList(
                sgruserCList
                    .stream()
                    .filter(uno -> {
                        Boolean isAvailable =
                            !lastMonthA0UnoList.contains(uno)
                                && !lastMonthD6UnoList.contains(uno)
                                && lastMonthSgresultList
                                .stream()
                                .filter(sgresult -> {
                                    Boolean isCurrentSchdate = sgresult.getSchdate().equals(lastMonthEnd);
                                    Boolean isCurrentClsno = sgresult.getClsno().equals("A8");
                                    Boolean isCurrentUno = sgresult.getUno().equals(uno);
                                    return isCurrentSchdate && isCurrentClsno && isCurrentUno;
                                })
                                .collect(Collectors.toList())
                                .size() == 0;
                        return isAvailable;
                    })
                    .collect(Collectors.toList()),
                r55HolidayC
            );
            List<String> r55HolidayABCList = Stream
                .of(
                    r55HolidayAList,
                    r55HolidayBList,
                    r55HolidayCList
                )
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());
            sgruserAList.removeAll(r55HolidayABCList);
            sgruserBList.removeAll(r55HolidayABCList);
            sgruserCList.removeAll(r55HolidayABCList);
            // 計算 A8 班出勤人數，優先順序為: 上月名單 > 隨機名單。
            List<String> ra8ABCList = new ArrayList<>();
            int totalManpower = sgruserList.size();
            int total55Manpower = sgruserRegularList.size() > 2
                ? r55RoomOpen * r55NeedManpower + r55Holiday + r55Wait + r55Nurse + r55WorkStat + r55OPDOR - (sgruserRegularList.size() - 2)
                : r55RoomOpen * r55NeedManpower + r55Holiday + r55Wait + r55Nurse + r55WorkStat + r55OPDOR;
            if (sgruserBreastFeedList.size() > 1)
                total55Manpower = total55Manpower - (sgruserBreastFeedList.size() - 1);
            int totalD6Manpower = rd6ABCList.size();
            int totalA0Manpower = ra0ABCList.size();
            int ra8Manpower = totalManpower - total55Manpower - totalD6Manpower - totalA0Manpower;
            if (ra8Manpower < 2) {
                if (sgruserBreastFeedList.size() > 0) {
                    ra8Manpower = 1;
                } else {
                    ra8Manpower = 2;
                }
            }
            List<String> totalManpowerList = Stream
                .of(
                    sgruserAList,
                    sgruserBList,
                    sgruserCList
                )
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());
            SecureRandom rand = new SecureRandom();
            rand.setSeed((new Date()).getTime());
            String firstA8Uno = totalManpowerList.get(rand.nextInt(totalManpowerList.size()));
            ra8ABCList.add(firstA8Uno);
            if (ra8Manpower >= 2) {
                int numberOfShortages = ra8Manpower - 2;
                if (sgruserAList.contains(firstA8Uno)) {
                    totalManpowerList = Stream
                        .of(
                            sgruserBList,
                            sgruserCList
                        )
                        .flatMap(x -> x.stream())
                        .collect(Collectors.toList());
                } else if (sgruserBList.contains(firstA8Uno)) {
                    totalManpowerList = Stream
                        .of(
                            sgruserAList,
                            sgruserCList
                        )
                        .flatMap(x -> x.stream())
                        .collect(Collectors.toList());
                } else if (sgruserCList.contains(firstA8Uno)) {
                    totalManpowerList = Stream
                        .of(
                            sgruserAList,
                            sgruserBList
                        )
                        .flatMap(x -> x.stream())
                        .collect(Collectors.toList());
                }

                String secondA8Uno = totalManpowerList.get(rand.nextInt(totalManpowerList.size()));
                ra8ABCList.add(secondA8Uno);
                if (numberOfShortages > 0) {
                    totalManpowerList = Stream
                        .of(
                            sgruserAList,
                            sgruserBList,
                            sgruserCList
                        )
                        .flatMap(x -> x.stream())
                        .filter(uno -> !(uno.equals(firstA8Uno) || uno.equals(secondA8Uno)))
                        .collect(Collectors.toList());
                    ra8ABCList.addAll(getRandomList(totalManpowerList, numberOfShortages));
                    sgruserAList.removeAll(ra8ABCList);
                    sgruserBList.removeAll(ra8ABCList);
                    sgruserCList.removeAll(ra8ABCList);
                }
            }
            List<String> unoList = sgruserList
                .stream()
                .map(sgruser -> sgruser.getUno())
                .filter(uno -> !(r55HolidayABCList.contains(uno)
                        || lastMonthD6UnoList.contains(uno)
                        || rd6ABCList.contains(uno)
                        || ra0ABCList.contains(uno)
                        || ra8ABCList.contains(uno)
                    )
                )
                .collect(Collectors.toList());
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
                        int clsno55RequireManpower = r55RoomOpen * r55NeedManpower + r55Holiday + r55Wait + r55Nurse + r55WorkStat + r55OPDOR - r55HolidayABCList.size();
                        // 上月 D6 班(去除於本月再次包班的人員)
                        List<String> d6UnoList = lastMonthD6UnoList
                            .stream()
                            .filter(lastMonthD6Uno -> !(rd6ABCList.contains(lastMonthD6Uno)
                                || ra0ABCList.contains(lastMonthD6Uno))
                            )
                            .collect(Collectors.toList());
                        if (d6UnoList.size() > 0) {
                            for (String uno : d6UnoList) {
                                sgbackupList.add(new Sgbackup(uno, monthStart, "55"));
                            }
                            clsno55RequireManpower = clsno55RequireManpower - d6UnoList.size();
                        }
                        // 扣除哺乳班支援人數 (預扣支援A8人數:1人)
                        if (sgruserBreastFeedList.size() > 1)
                            clsno55RequireManpower = clsno55RequireManpower - (sgruserBreastFeedList.size() - 1);
                        if (clsno55RequireManpower > 0) {
                            for (String uno : unoList) {
                                sgbackupList.add(new Sgbackup(uno, monthStart, "55"));
                            }
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
            // 判斷有無休假紀錄，如有排 OFF 則予以 OFF; 如有排公休則予以公休。
            WeekFields weekFields
                = WeekFields.of(DayOfWeek.MONDAY, 1);
            TemporalField weekOfWeekBasedYear
                = weekFields.weekOfWeekBasedYear();

            for (LocalDate currentDate = monthStart;
                 currentDate.isBefore(monthEnd.plusDays(1));
                 currentDate = currentDate.plusDays(1)
            ) {
                int currentYear = Integer.parseInt(String.valueOf(currentDate.getYear()).substring(2));
                int currentWeek = currentDate.get(weekOfWeekBasedYear) < 10
                    ? Integer.parseInt(String.format("%d0%d", currentYear, currentDate.get(weekOfWeekBasedYear)))
                    : Integer.parseInt(String.format("%d%d", currentYear, currentDate.get(weekOfWeekBasedYear)));
                for (Sgruser sgruser : allSgruserList) {
                    Boolean isAnnualLeave = false;
                    Boolean isDayOff = false;
                    for (Sgbackup sgbackup : unavailableList) {
                        Boolean isCurrentDate = sgbackup.getSchdate().equals(currentDate);
                        Boolean isCurrentUno = sgbackup.getUno().equals(sgruser.getUno());
                        if (isCurrentDate && isCurrentUno) {
                            if (sgbackup.getClsno().equals("公休")) isAnnualLeave = true;
                            if (sgbackup.getClsno().equals("OFF")) isDayOff = true;
                        }
                    }
                    if (isAnnualLeave) {
                        sgresultList.add(new Sgresult(sgruser, currentDate, currentWeek, "公休", ""));
                    } else if (isDayOff) {
                        sgresultList.add(new Sgresult(sgruser, currentDate, currentWeek, "OFF", ""));
                    } else {
                        sgresultList.add(new Sgresult(sgruser, currentDate, currentWeek, "", ""));
                    }
                }
            }
            // A0 排班 (先排 A0，後排 D6)
            setShift(sgresultList, lastMonthSgresultList, lastMonthA0UnoList, lastMonthA0AndD6UnoList, ra0ABCList, monthStart, "A0");
            // D6 排班
            setShift(sgresultList, lastMonthSgresultList, lastMonthD6UnoList, lastMonthA0AndD6UnoList, rd6ABCList, monthStart, "D6");
            // 平日 55 班輪休暫存表
            List<String> r55DaysOffUnoList = new ArrayList<>();
            List<String> ra8BreastFeedUnoList = new ArrayList<>();
            List<String> r55BreastFeedUnoList = new ArrayList<>();
            List<String> r55BreastFeedUnoListBackup = new ArrayList<>();
            String ra8BreastFeedUno = "";
            String ra8BreastFeedUnoBackup = "";
            for (int week = 0; week < weekList.size(); week++) {
                LocalDate weekStart = LocalDate.parse(weekList.get(week).getStart());
                LocalDate weekEnd = LocalDate.parse(weekList.get(week).getEnd());
                int totalDates = weekEnd.getDayOfMonth() - weekStart.getDayOfMonth() + 1;
                for (int dateIndex = 0; dateIndex < totalDates; dateIndex++) {
                    LocalDate currentDate = weekStart.plusDays(dateIndex);
                    DayOfWeek day = DayOfWeek.of(currentDate.get(ChronoField.DAY_OF_WEEK));
                    Boolean isWeekend = day == DayOfWeek.SUNDAY || day == DayOfWeek.SATURDAY;
                    // 常日班排除公休人員
                    List<String> regularUnoList = sgruserRegularList
                        .stream()
                        .filter(sgruser -> {
                            Boolean isAvailable = unavailableList
                                .stream()
                                .filter(sgbackup -> sgbackup.getSchdate().equals(currentDate)
                                    && sgbackup.getClsno().equals("公休")
                                    && sgbackup.getUno().equals(sgruser.getUno())
                                )
                                .collect(Collectors.toList())
                                .size() == 0;
                            return isAvailable;
                        })
                        .map(sgbackup -> sgbackup.getUno())
                        .collect(Collectors.toList());
                    // 若本月哺乳班有兩人以上，則每週依序排入A8班別1人，其他人則排入55班別。
                    ra8BreastFeedUno = ra8BreastFeedUnoBackup;
                    r55BreastFeedUnoList = r55BreastFeedUnoListBackup;
                    if (currentDate.equals(weekStart) && sgruserBreastFeedList.size() > 0) {
                        ra8BreastFeedUnoBackup = "";
                        r55BreastFeedUnoListBackup.clear();
                        if (ra8BreastFeedUnoList.containsAll(sgruserBreastFeedList.stream().map(sgruser -> sgruser.getUno()).collect(Collectors.toList()))) {
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
                        ra8BreastFeedUnoBackup = ra8BreastFeedUno;
                        r55BreastFeedUnoListBackup = r55BreastFeedUnoList;
                    }
                    // 哺乳班當日如有人請公休，則予以公休，不再找人遞補。
                    List<String> breastFeedUnavailableList = new ArrayList<>();
                    for (Sgbackup sgbackup : unavailableList) {
                        Boolean isCurrentSchdate = sgbackup.getSchdate().equals(currentDate);
                        Boolean isAnnaulLeave = sgbackup.getClsno().equals("公休");
                        if (isCurrentSchdate && isAnnaulLeave) {
                            Boolean isCurrentUno = sgruserBreastFeedList.stream().map(sgruser -> sgruser.getUno()).collect(Collectors.toList()).contains(sgbackup.getUno());
                            if (isCurrentUno) {
                                breastFeedUnavailableList.add(sgbackup.getUno());
                            }
                        }
                    }
                    if (breastFeedUnavailableList.size() > 0) {
                        if (breastFeedUnavailableList.contains(ra8BreastFeedUno)) {
                            ra8BreastFeedUno = "";
                        }
                        if (r55BreastFeedUnoList.size() > 0) {
                            r55BreastFeedUnoList = r55BreastFeedUnoList
                                .stream()
                                .filter(uno -> !breastFeedUnavailableList.contains(uno))
                                .collect(Collectors.toList());
                        }
                    }
                    // 參考閒置名單，以獲取可出勤人員名單，不可出勤者則排 OFF 班。
                    List<String> idleUnoList = sgresultList
                        .stream()
                        .filter(sgresult -> {
                            Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentDate);
                            Boolean isCurrentClsno = sgresult.getClsno().equals("閒置");
                            Boolean isAvailable = unavailableList
                                .stream()
                                .filter(unavailable ->
                                    unavailable.getSchdate().equals(currentDate)
                                        && unavailable.getUno().equals(sgresult.getUno())
                                        && unavailable.getClsno().equals("公休")
                                )
                                .collect(Collectors.toList())
                                .size() == 0
                                && sgresultList
                                .stream()
                                .filter(sgresult1 -> {
                                    Boolean isCurrentUno = sgresult1.getUno().equals(sgresult.getUno());
                                    if (isCurrentUno) {
                                        Boolean isTomorrow = sgresult1.getSchdate().equals(currentDate.plusDays(1));
                                        Boolean isYesterday = sgresult1.getSchdate().equals(currentDate.minusDays(1));
                                        Boolean isA0 = sgresult1.getClsno().equals("A0");
                                        Boolean isD6OrA8 = sgresult1.getClsno().equals("D6");
                                        if (isTomorrow && isA0) return true;
                                        if (isYesterday && isD6OrA8) return true;
                                    }
                                    return false;
                                })
                                .collect(Collectors.toList())
                                .size() == 0
                                && lastMonthSgresultList
                                .stream()
                                .filter(sgresult1 -> {
                                    Boolean isYesterday = sgresult1.getSchdate().equals(currentDate.minusDays(1));
                                    Boolean isD6OrA8 = sgresult1.getClsno().equals("D6");
                                    Boolean isCurrentUno = sgresult1.getUno().equals(sgresult.getUno());
                                    return isYesterday && isCurrentUno && isD6OrA8;
                                })
                                .map(sgresult1 -> sgresult1.getUno())
                                .collect(Collectors.toList())
                                .size() == 0;
                            return isCurrentSchdate && isAvailable && isCurrentClsno;
                        })
                        .map(sgresult -> sgresult.getUno())
                        .collect(Collectors.toList());
                    for (String currentUno : new ArrayList<String>(idleUnoList)) {
                        Boolean isUnavailable = sgresultList
                            .stream()
                            .filter(sgresult -> {
                                Boolean isTomorrow = sgresult.getSchdate().equals(currentDate.plusDays(1));
                                Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                Boolean isCurrentClsno = sgresult.getClsno().equals("A0");
                                return isTomorrow && isCurrentUno && isCurrentClsno;
                            })
                            .collect(Collectors.toList())
                            .size() > 0
                            || sgresultList
                            .stream()
                            .filter(sgresult -> {
                                Boolean isYesterday = sgresult.getSchdate().equals(currentDate.minusDays(1));
                                Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                Boolean isCurrentClsno = sgresult.getClsno().equals("D6");
                                return isYesterday && isCurrentUno && isCurrentClsno;
                            })
                            .collect(Collectors.toList())
                            .size() > 0
                            || lastMonthSgresultList
                            .stream()
                            .filter(sgresult -> {
                                Boolean isYesterday = sgresult.getSchdate().equals(currentDate.minusDays(1));
                                Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                Boolean isCurrentClsno = sgresult.getClsno().equals("D6");
                                return isYesterday && isCurrentUno && isCurrentClsno;
                            })
                            .collect(Collectors.toList())
                            .size() > 0;
                        if (isUnavailable) {
                            for (Sgresult sgresult : sgresultList) {
                                boolean isCurrentSchdate = sgresult.getSchdate().equals(currentDate);
                                boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                if (isCurrentSchdate && isCurrentUno) {
                                    sgresult.setClsno("OFF");
                                    sgresult.setClspr(0);
                                    idleUnoList.remove(currentUno);
                                }
                            }
                        }
                    }
                    // 參考預約休假名單，過濾休假人員，以獲取可出勤人員名單
                    List<Sgbackup> availableList = sgbackupList
                        .stream()
                        .filter(sgbackup -> {
                            Boolean isWorkday = sgbackup.getClsno().equals("55")
                                || sgbackup.getClsno().equals("A0")
                                || sgbackup.getClsno().equals("A8")
                                || sgbackup.getClsno().equals("D6");
                            Boolean isCurrentYear = sgbackup.getSchdate().getYear() == monthStart.getYear();
                            Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == monthStart.getMonthValue();
                            Boolean isCurrentUno = !(rd6ABCList.contains(sgbackup.getUno()) || ra0ABCList.contains(sgbackup.getUno()));
                            Boolean isAvailable = unavailableList
                                .stream()
                                .filter(unavailable -> {
                                    Boolean isCurrentSchdate = unavailable.getSchdate().equals(currentDate);
                                    Boolean isUnavailable = unavailable.getUno().equals(sgbackup.getUno());
                                    return isCurrentSchdate && isUnavailable;
                                })
                                .collect(Collectors.toList())
                                .size() == 0
                                && sgresultList
                                .stream()
                                .filter(sgresult -> {
                                    Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentDate);
                                    Boolean isUnavailable = sgresult.getUno().equals(sgbackup.getUno())
                                        && (sgresult.getClsno().equals("D6") || sgresult.getClsno().equals("A0"));
                                    return isCurrentSchdate && isUnavailable;
                                })
                                .collect(Collectors.toList())
                                .size() == 0;
                            return isWorkday && isCurrentYear && isCurrentMonth && isCurrentUno && isAvailable;
                        })
                        .collect(Collectors.toList());
                    // 55 排班
                    if (isWeekend) {
                        // 週末 55 班
                        List<String> r55HolidayAvailableList = availableList
                            .stream()
                            .filter(sgbackup -> {
                                Boolean isCurrentYear = sgbackup.getSchdate().getYear() == monthStart.getYear();
                                Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == monthStart.getMonthValue();
                                Boolean isCurrentClsno = sgbackup.getClsno().equals("55");
                                Boolean isAvailable = sgresultList
                                    .stream()
                                    .filter(sgresult -> {
                                        Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentDate);
                                        Boolean isCurrentUno = sgresult.getUno().equals(sgbackup.getUno());
                                        return isCurrentSchdate
                                            && isCurrentUno;
                                    })
                                    .collect(Collectors.toList())
                                    .size() > 0;
                                if (isCurrentYear && isCurrentMonth && isCurrentClsno && isAvailable) {
                                    Boolean isCurrentUno = r55HolidayABCList.contains(sgbackup.getUno());
                                    return isCurrentUno;
                                }
                                return false;
                            })
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
                                        if (isCurrentClsno && isCurrentYear && isCurrentMonth) {
                                            Boolean isCurrentUno = sgbackup.getUno().equals(unavailable.getUno()) && r55HolidayABCList.contains(unavailable.getUno());
                                            return isCurrentUno;
                                        }
                                        return false;
                                    })
                                    .collect(Collectors.toList())
                                    .size() > 0
                                    && sgresultList
                                    .stream()
                                    .filter(sgresult -> {
                                        Boolean isYesterday = sgresult.getSchdate().equals(currentDate.minusDays(1));
                                        Boolean isUnavailable = sgresult.getUno().equals(unavailable.getUno())
                                            && (sgresult.getClsno().equals("D6") || sgresult.getClsno().equals("A8"));
                                        return isYesterday && isUnavailable;
                                    })
                                    .collect(Collectors.toList())
                                    .size() == 0
                                    && lastMonthSgresultList
                                    .stream()
                                    .filter(sgresult -> {
                                        Boolean isYesterday = sgresult.getSchdate().equals(currentDate.minusDays(1));
                                        Boolean isUnavailable = sgresult.getUno().equals(unavailable.getUno())
                                            && (sgresult.getClsno().equals("D6") || sgresult.getClsno().equals("A8"));
                                        return isYesterday && isUnavailable;
                                    })
                                    .collect(Collectors.toList())
                                    .size() == 0;
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
                            Boolean isCurrentYear = sgbackup.getSchdate().getYear() == currentDate.getYear();
                            Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == currentDate.getMonthValue();
                            if (isCurrentYear && isCurrentMonth) {
                                Boolean isCurrentClsno =
                                    sgresultList
                                        .stream()
                                        .filter(sgresult -> {
                                            Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentDate);
                                            Boolean isUnavailable = sgresult.getUno().equals(sgbackup.getUno())
                                                && (sgresult.getClsno().equals("D6") || sgresult.getClsno().equals("A0"));
                                            return isCurrentSchdate && isUnavailable;
                                        })
                                        .collect(Collectors.toList())
                                        .size() == 0
                                        && sgbackup.getClsno().equals("55")
                                        || sgbackup.getClsno().equals("daily")
                                        || (sgbackup.getClsno().equals("哺乳") && r55BreastFeedUnoList.contains(sgbackup.getUno()))
                                        || idleUnoList.contains(sgbackup.getUno());
                                if (isCurrentClsno) {
                                    r55HolidayUnavailableList.add(sgbackup.getUno());
                                }
                            }
                        }
                        DayOfWeek weekendDay = DayOfWeek.of(currentDate.get(ChronoField.DAY_OF_WEEK));
                        Boolean weekendIsSaturday = weekendDay == DayOfWeek.SATURDAY;
                        Boolean weekendIsSunday = weekendDay == DayOfWeek.SUNDAY;
                        for (String currentUno : r55HolidayAvailableList) {
                            // 如出勤 55 班的人在前一天為 A8 班，則以 OFF 班代替。
                            Boolean isAvailable = sgresultList
                                .stream()
                                .filter(sgresult -> {
                                    Boolean isYesterday = sgresult.getSchdate().equals(currentDate.minusDays(1));
                                    Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                    Boolean isCurrentClsno = sgresult.getClsno().equals("A8") || sgresult.getClsno().equals("D6");
                                    return isYesterday && isCurrentUno && isCurrentClsno;
                                })
                                .collect(Collectors.toList())
                                .size() == 0
                                && lastMonthSgresultList
                                .stream()
                                .filter(sgresult -> {
                                    Boolean isYesterday = sgresult.getSchdate().equals(currentDate.minusDays(1));
                                    Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                    Boolean isCurrentClsno = sgresult.getClsno().equals("A8") || sgresult.getClsno().equals("D6");
                                    return isYesterday && isCurrentUno && isCurrentClsno;
                                })
                                .collect(Collectors.toList())
                                .size() == 0;
                            if (isAvailable) {
                                for (Sgresult sgresult : sgresultList) {
                                    if (sgresult.getUno().equals(currentUno) && sgresult.getSchdate().equals(currentDate)) {
                                        sgresult.setClsno("55");
                                        sgresult.setClspr(8);
                                        r55HolidayUnavailableList.remove(sgresult.getUno());
                                    }
                                }
                            } else {
                                for (Sgresult sgresult : sgresultList) {
                                    boolean isCurrentSchdate = sgresult.getSchdate().equals(currentDate);
                                    boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                    if (isCurrentSchdate && isCurrentUno) {
                                        sgresult.setClsno("OFF");
                                        sgresult.setClspr(0);
                                    }
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
                                    Boolean isCurrentYear = currentSchdate.getYear() == currentDate.getYear();
                                    Boolean isCurrentMonth = currentSchdate.getMonthValue() == currentDate.getMonthValue();
                                    if (isCurrentYear && isCurrentMonth) {
                                        for (Sgresult sgresult : sgresultList) {
                                            Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                            Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentSchdate);
                                            Boolean isDayOff = sgresult.getClsno().equals("OFF");
                                            if (isCurrentUno && isCurrentSchdate && isDayOff) {
                                                daysOffSchdateList.add(currentSchdate);
                                            }
                                        }
                                    }
                                }
                                if (daysOffSchdateList.size() < 2 && !daysOffSchdateList.contains(currentFriday)) {
                                    List<String> r55UnoList = new ArrayList<>();
                                    for (Sgbackup sgbackup : sgbackupList) {
                                        Boolean isCurrentYear = sgbackup.getSchdate().getYear() == currentDate.getYear();
                                        Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == currentDate.getMonthValue();
                                        if (isCurrentYear && isCurrentMonth) {
                                            Boolean isCurrentClsno = sgbackup.getClsno().equals("55");
                                            Boolean isCurrentUno = !r55HolidayAvailableList.contains(sgbackup.getUno());
                                            if (isCurrentClsno && isCurrentUno) {
                                                r55UnoList.add(sgbackup.getUno());
                                            }
                                        }
                                    }
                                    Boolean isCurrentYear = currentFriday.getYear() == currentDate.getYear();
                                    Boolean isCurrentMonth = currentFriday.getMonthValue() == currentDate.getMonthValue();
                                    if (isCurrentYear && isCurrentMonth) {
                                        List<String> daysOffUnoList = sgresultList
                                            .stream()
                                            .filter(sgresult -> {
                                                Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentFriday);
                                                Boolean isCurrentClsno = sgresult.getClsno().equals("OFF");
                                                Boolean is55 = r55UnoList.contains(sgresult.getUno());
                                                Boolean isNotD6NorA8 = sgresultList
                                                    .stream()
                                                    .filter(sgresult1 -> {
                                                        Boolean isYesterday = sgresult1.getSchdate().equals(currentFriday.minusDays(1));
                                                        Boolean isD6OrA8 = sgresult1.getClsno().equals("D6") || sgresult1.getClsno().equals("A8");
                                                        Boolean isCurrentUno = sgresult1.getUno().equals(sgresult.getUno());
                                                        return isYesterday && isCurrentUno && isD6OrA8;
                                                    })
                                                    .collect(Collectors.toList())
                                                    .size() == 0
                                                    && lastMonthSgresultList
                                                    .stream()
                                                    .filter(sgresult1 -> {
                                                        Boolean isYesterday = sgresult1.getSchdate().equals(currentFriday.minusDays(1));
                                                        Boolean isD6OrA8 = sgresult1.getClsno().equals("D6") || sgresult1.getClsno().equals("A8");
                                                        Boolean isCurrentUno = sgresult1.getUno().equals(sgresult.getUno());
                                                        return isYesterday && isCurrentUno && isD6OrA8;
                                                    })
                                                    .collect(Collectors.toList())
                                                    .size() == 0;
                                                return isCurrentSchdate && isCurrentClsno && is55 && isNotD6NorA8;
                                            })
                                            .map(sgresult -> sgresult.getUno())
                                            .collect(Collectors.toList());
                                        if (daysOffUnoList.size() == 0 && r55Holiday < 2) {
                                            for (Sgresult sgresult : sgresultList) {
                                                Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                                Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentFriday);
                                                if (isCurrentSchdate && isCurrentUno) {
                                                    sgresult.setClsno("OFF");
                                                    sgresult.setClspr(0);
                                                }
                                            }
                                        } else {
                                            int random = rand.nextInt(daysOffUnoList.size());
                                            String alternativeUno = daysOffUnoList.get(random);
                                            for (Sgresult sgresult : sgresultList) {
                                                Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                                Boolean isAlternativeUno = sgresult.getUno().equals(alternativeUno);
                                                Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentFriday);
                                                if (isCurrentSchdate) {
                                                    if (isCurrentUno) {
                                                        sgresult.setClsno("OFF");
                                                        sgresult.setClspr(0);
                                                    }
                                                    if (isAlternativeUno) {
                                                        sgresult.setClsno("55");
                                                        sgresult.setClspr(8);
                                                    }
                                                }
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
                        List<String> r55DayOffUnoList = new ArrayList<>();
                        Boolean isMonday = DayOfWeek.of(currentDate.get(ChronoField.DAY_OF_WEEK)) == DayOfWeek.MONDAY;
                        List<String> r55CompensatoryUnoList = sgresultList
                            .stream()
                            .filter(sgresult -> {
                                Boolean isCurrentYear = sgresult.getSchdate().getYear() == currentDate.getYear();
                                Boolean isCurrentMonth = sgresult.getSchdate().getMonthValue() == currentDate.getMonthValue();
                                Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentDate.minusDays(2));
                                Boolean isSaturday = DayOfWeek.of(sgresult.getSchdate().get(ChronoField.DAY_OF_WEEK)) == DayOfWeek.SATURDAY;
                                Boolean isCurrentClsno = sgresult.getClsno().equals("55");
                                return isCurrentYear && isCurrentMonth && isCurrentSchdate && isSaturday && isCurrentClsno;
                            })
                            .map(sgresult -> sgresult.getUno())
                            .collect(Collectors.toList());
                        if (isMonday && r55CompensatoryUnoList.size() > 0) {
                            for (String currentUno : r55CompensatoryUnoList) {
                                for (Sgresult sgresult : sgresultList) {
                                    if (sgresult.getUno().equals(currentUno) && sgresult.getSchdate().equals(currentDate)) {
                                        sgresult.setClsno("OFF");
                                        sgresult.setClspr(0);
                                    }
                                }
                            }
                        } else {
                            List<String> r55UnoList = sgbackupList
                                .stream()
                                .filter(sgbackup -> {
                                    Boolean is55 = sgbackup.getClsno().equals("55");
                                    Boolean isCurrentYear = sgbackup.getSchdate().getYear() == monthStart.getYear();
                                    Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == monthStart.getMonthValue();
                                    Boolean isAvailable = sgresultList
                                        .stream()
                                        .filter(sgresult -> {
                                            Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentDate);
                                            Boolean isCurrentClsno = sgresult.getClsno().equals("D6") || sgresult.getClsno().equals("A0");
                                            Boolean isCurrentUno = sgresult.getUno().equals(sgbackup.getUno());
                                            return isCurrentSchdate && isCurrentClsno && isCurrentUno;
                                        })
                                        .map(sgresult -> sgresult.getUno())
                                        .collect(Collectors.toList())
                                        .size() == 0;
                                    return is55 && isCurrentYear && isCurrentMonth && isAvailable;
                                })
                                .map(sgbackup -> sgbackup.getUno())
                                .collect(Collectors.toList());

                            // 輪休清單額滿，則清空，釋放可輪休名單。
                            if (r55DaysOffUnoList.containsAll(r55UnoList)) {
                                r55DaysOffUnoList.clear();
                            }
                            r55UnoList.removeAll(r55DaysOffUnoList);
                            if (r55UnoList.size() >= r55Holiday) {
                                r55DayOffUnoList = getRandomList(r55UnoList, r55Holiday);
                            } else {
                                r55DayOffUnoList.addAll(r55UnoList);
                            }
                            r55DaysOffUnoList.addAll(r55DayOffUnoList);
                            if (r55DayOffUnoList.size() > 0) {
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
                            Boolean isCurrentYear = sgbackup.getSchdate().getYear() == currentDate.getYear();
                            Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == currentDate.getMonthValue();
                            Boolean isCurrentClsno = sgbackup.getClsno().equals("55");
                            Boolean isAvailable = !r55DayOffUnoList.contains(sgbackup.getUno())
                                && sgresultList
                                .stream()
                                .filter(sgresult -> {
                                    Boolean isCurrentUno = sgresult.getUno().equals(sgbackup.getUno());
                                    if (isCurrentUno) {
                                        Boolean isTomorrow = sgresult.getSchdate().equals(currentDate.plusDays(1));
                                        Boolean isYesterday = sgresult.getSchdate().equals(currentDate.minusDays(1));
                                        Boolean isA0 = sgresult.getClsno().equals("A0");
                                        Boolean isD6OrA8 = sgresult.getClsno().equals("D6") || sgresult.getClsno().equals("A8");
                                        if (isTomorrow && isA0) return true;
                                        if (isYesterday && isD6OrA8) return true;
                                    }
                                    return false;
                                })
                                .collect(Collectors.toList())
                                .size() == 0
                                && lastMonthSgresultList
                                .stream()
                                .filter(sgresult -> {
                                    Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentDate.minusDays(1));
                                    Boolean isD6OrA8 = sgresult.getClsno().equals("D6") || sgresult.getClsno().equals("A8");
                                    Boolean isCurrentUno = sgresult.getUno().equals(sgbackup.getUno());
                                    return isCurrentSchdate && isCurrentUno && isD6OrA8;
                                })
                                .map(sgresult -> sgresult.getUno())
                                .collect(Collectors.toList())
                                .size() == 0;
                            if (isCurrentYear && isCurrentMonth && isCurrentClsno && isAvailable) {
                                Boolean isCurrentUno = !r55DayOffUnoList.contains(sgbackup.getUno());
                                if (isMonday) {
                                    isCurrentUno = !r55CompensatoryUnoList.contains(sgbackup.getUno());
                                }
                                if (isCurrentUno) {
                                    r55AvailableList.add(sgbackup.getUno());
                                }
                            }
                        }
                        List<String> r55UnavailableList = new ArrayList<>();
                        for (Sgbackup unavailable : unavailableList) {
                            Boolean isCurrentSchdate = unavailable.getSchdate().equals(currentDate);
                            Boolean isDayOff = unavailable.getClsno().equals("OFF");
                            Boolean isCurrentClsno = sgbackupList
                                .stream()
                                .filter(sgbackup -> {
                                    Boolean isCurrentUno = sgbackup.getUno().equals(unavailable.getUno());
                                    Boolean is55 = sgbackup.getClsno().equals("55");
                                    Boolean isCurrentYear = sgbackup.getSchdate().getYear() == monthStart.getYear();
                                    Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == monthStart.getMonthValue();
                                    return isCurrentUno && is55 && isCurrentYear && isCurrentMonth;
                                })
                                .map(sgbackup -> sgbackup.getUno())
                                .collect(Collectors.toList())
                                .size() > 0;
                            Boolean isAvailable = !r55DayOffUnoList.contains(unavailable.getUno())
                                && !r55HolidayABCList.contains(unavailable.getUno())
                                && sgresultList
                                .stream()
                                .filter(sgresult -> {
                                    Boolean isYesterday = sgresult.getSchdate().equals(currentDate.plusDays(1));
                                    Boolean isA0 = sgresult.getClsno().equals("A0");
                                    Boolean isCurrentUno = sgresult.getUno().equals(unavailable.getUno());
                                    return isYesterday && isCurrentUno && isA0;
                                })
                                .map(sgresult -> sgresult.getUno())
                                .collect(Collectors.toList())
                                .size() == 0
                                && sgresultList
                                .stream()
                                .filter(sgresult -> {
                                    Boolean isYesterday = sgresult.getSchdate().equals(currentDate.minusDays(1));
                                    Boolean isD6OrA8 = sgresult.getClsno().equals("D6") || sgresult.getClsno().equals("A8");
                                    Boolean isCurrentUno = sgresult.getUno().equals(unavailable.getUno());
                                    return isYesterday && isCurrentUno && isD6OrA8;
                                })
                                .map(sgresult -> sgresult.getUno())
                                .collect(Collectors.toList())
                                .size() == 0
                                && lastMonthSgresultList
                                .stream()
                                .filter(sgresult -> {
                                    Boolean isYesterday = sgresult.getSchdate().equals(currentDate.minusDays(1));
                                    Boolean isD6OrA8 = sgresult.getClsno().equals("D6") || sgresult.getClsno().equals("A8");
                                    Boolean isCurrentUno = sgresult.getUno().equals(unavailable.getUno());
                                    return isYesterday && isCurrentUno && isD6OrA8;
                                })
                                .map(sgresult -> sgresult.getUno())
                                .collect(Collectors.toList())
                                .size() == 0;
                            if (isCurrentSchdate && isDayOff && isCurrentClsno && isAvailable) {
                                Boolean isCurrentUno = !r55DayOffUnoList.contains(unavailable.getUno());
                                if (isMonday) {
                                    isCurrentUno = !r55CompensatoryUnoList.contains(unavailable.getUno());
                                }
                                if (isCurrentUno) r55UnavailableList.add(unavailable.getUno());
                            }
                        }
                        int r55Manpower = r55RoomOpen * r55NeedManpower + r55Wait + r55Nurse + r55WorkStat + r55OPDOR;
                        if (r55AvailableList.size() < r55Manpower) {
                            int numberOfShortages = r55Manpower - r55AvailableList.size();
                            // 如 55 班可出勤人數還是不足，則從今日閒置班名單中挑選遞補人員。
                            List<String> idle55UnoList = idleUnoList
                                .stream()
                                .filter(uno -> {
                                    Boolean isAvailable = sgresultList
                                        .stream()
                                        .filter(sgresult -> {
                                            Boolean isYesterday = sgresult.getSchdate().equals(currentDate.minusDays(1));
                                            Boolean isCurrentUno = sgresult.getUno().equals(uno);
                                            Boolean isA8 = sgresult.getClsno().equals("A8");
                                            return isYesterday && isCurrentUno && isA8;
                                        })
                                        .collect(Collectors.toList())
                                        .size() == 0
                                        && lastMonthSgresultList
                                        .stream()
                                        .filter(sgresult -> {
                                            Boolean isYesterday = sgresult.getSchdate().equals(currentDate.minusDays(1));
                                            Boolean isCurrentUno = sgresult.getUno().equals(uno);
                                            Boolean isA8 = sgresult.getClsno().equals("A8");
                                            return isYesterday && isCurrentUno && isA8;
                                        })
                                        .collect(Collectors.toList())
                                        .size() == 0;
                                    return isAvailable;
                                })
                                .collect(Collectors.toList());
                            if (idle55UnoList.size() >= numberOfShortages) {
                                for (int index = 0; index < numberOfShortages; index++) {
                                    if (idle55UnoList.size() > 0) {
                                        r55AvailableList.add(idle55UnoList.remove(0));
                                    }
                                }
                                idleUnoList.removeAll(r55AvailableList);
                            } else {
                                r55AvailableList.addAll(idle55UnoList);
                                idle55UnoList.clear();
                                idleUnoList.removeAll(r55AvailableList);
                                numberOfShortages = numberOfShortages - idleUnoList.size();
                                // 如 55 班可出勤人數不足，則從今日 OFF 名單中挑選遞補人員。
                                if (r55UnavailableList.size() >= numberOfShortages) {
                                    r55AvailableList.addAll(getRandomList(r55UnavailableList, numberOfShortages));
                                } else {
                                    r55AvailableList.addAll(r55UnavailableList);
                                    numberOfShortages = numberOfShortages - r55UnavailableList.size();
                                    // 如 55 班可出勤人數還是不足，則從今日哺乳班名單中挑選遞補人員。
                                    if (r55BreastFeedUnoList.size() >= numberOfShortages) {
                                        r55AvailableList.addAll(getRandomList(r55BreastFeedUnoList.stream().collect(Collectors.toList()), numberOfShortages));
                                    } else {
                                        r55AvailableList.addAll(r55BreastFeedUnoList);
                                        numberOfShortages = numberOfShortages - r55BreastFeedUnoList.size();
                                        // 如 55 班可出勤人數還是不足，則從今日 55 輪休名單中挑選遞補人員。
                                        if (currentDate.equals(monthStart) && r55DayOffUnoList.size() >= numberOfShortages) {
                                            r55AvailableList.addAll(getRandomList(r55DayOffUnoList, numberOfShortages));
                                        } else {
                                            if (currentDate.equals(monthStart) && r55DayOffUnoList.size() > 0) {
                                                r55AvailableList.addAll(r55DayOffUnoList);
                                                numberOfShortages = numberOfShortages - r55DayOffUnoList.size();
                                            }
                                            // 如 55 班可出勤人數還是不足，則從今日固定白班名單中挑選遞補人員。
                                            if (regularUnoList.size() >= numberOfShortages) {
                                                r55AvailableList.addAll(
                                                    getRandomList(regularUnoList, numberOfShortages)
                                                );
                                            } else {
                                                r55AvailableList.addAll(regularUnoList);
                                                numberOfShortages = numberOfShortages - regularUnoList.size();
                                                // 如 55 班可出勤人數還是不足，則從今日A0休假名單中挑選遞補人員。
                                                List<String> currentA0UnoList = new ArrayList<>();
                                                for (Sgresult currentSgresult : sgresultList) {
                                                    Boolean isCurrentDate = currentSgresult.getSchdate().equals(currentDate);
                                                    Boolean isCurrentClsno = currentSgresult.getClsno().equals("A8")
                                                        || currentSgresult.getClsno().equals("OFF");
                                                    Boolean isAvailable = false;
                                                    for (Sgresult sgresult : sgresultList) {
                                                        Boolean isCurrentYear = sgresult.getSchdate().getYear() == currentDate.getYear();
                                                        Boolean isCurrentMonth = sgresult.getSchdate().getMonthValue() == currentDate.getMonthValue();
                                                        Boolean isYesterday = sgresult.getSchdate().equals(currentSgresult.getSchdate().minusDays(1))
                                                            && !(currentSgresult.isWeekend() && sgresult.isWeekend());
                                                        Boolean isA0 = sgresult.getClsno().equals("A0");
                                                        Boolean isCurrentUno = sgresult.getUno().equals(currentSgresult.getUno());
                                                        if (isCurrentYear && isCurrentMonth && isYesterday && isA0 && isCurrentUno) {
                                                            isAvailable = true;
                                                        }
                                                    }
                                                    if (isCurrentDate && isCurrentClsno && isAvailable) {
                                                        currentA0UnoList.add(currentSgresult.getUno());
                                                    }
                                                }
                                                if (currentA0UnoList.size() >= numberOfShortages) {
                                                    r55AvailableList.addAll(getRandomList(currentA0UnoList, numberOfShortages));
                                                } else {
                                                    r55AvailableList.addAll(currentA0UnoList);
                                                    numberOfShortages = numberOfShortages - currentA0UnoList.size();
                                                    // 如 55 班可出勤人數還是不足，則從今日請公休名單中挑選遞補人員。
                                                    List<String> current55UnoList = new ArrayList<>();
                                                    for (Sgresult sgresult : sgresultList) {
                                                        Boolean isCurrentDate = sgresult.getSchdate().equals(currentDate);
                                                        Boolean isCurrentClsno = sgresult.getClsno().equals("公休");
                                                        Boolean isAvailable = false;
                                                        for (Sgbackup sgbackup : sgbackupList) {
                                                            Boolean isCurrentYear = sgbackup.getSchdate().getYear() == currentDate.getYear();
                                                            Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == currentDate.getMonthValue();
                                                            Boolean isCurrentUno = sgbackup.getUno().equals(sgresult.getUno());

                                                            if (isCurrentYear && isCurrentMonth && isCurrentUno) {
                                                                Boolean is55 = sgbackup.getClsno().equals("55")
                                                                    || sgbackup.getClsno().equals("daily")
                                                                    || (sgbackup.getClsno().equals("哺乳") && r55BreastFeedUnoList.contains(sgbackup.getUno()));
                                                                if (is55) {
                                                                    isAvailable = true;
                                                                }
                                                            }
                                                        }
                                                        if (isCurrentDate && isCurrentClsno && isAvailable) {
                                                            current55UnoList.add(sgresult.getUno());
                                                        }
                                                    }
                                                    if (current55UnoList.size() >= numberOfShortages) {
                                                        r55AvailableList.addAll(getRandomList(current55UnoList, numberOfShortages));
                                                    } else {
                                                        r55AvailableList.addAll(current55UnoList);
                                                    }
                                                }
                                                for (Sgresult sgresult : sgresultList) {
                                                    Boolean isCurrentDate = sgresult.getSchdate().equals(currentDate);
                                                    Boolean isCurrentUno = r55AvailableList.contains(sgresult.getUno());
                                                    if (isCurrentDate && isCurrentUno) {
                                                        sgresult.setClsno("55");
                                                        sgresult.setClspr(8);
                                                    }
                                                }
                                            }
                                        }
                                    }

                                }


                            }
                        } else if (r55AvailableList.size() > r55Manpower) {
                            r55AvailableList = getRandomList(r55AvailableList, r55Manpower);
                        }
                        for (String uno : regularUnoList) {
                            if (!r55AvailableList.contains(uno)) {
                                r55AvailableList.add(uno);
                            }
                        }
                        if (r55BreastFeedUnoList.size() > 0) {
                            for (String uno : r55BreastFeedUnoList) {
                                if (!r55AvailableList.contains(uno)) {
                                    r55AvailableList.add(uno);
                                }
                            }
                        }
                        // 更新排班結果表
                        for (String currentUno : r55AvailableList) {
                            for (Sgresult sgresult : sgresultList) {
                                Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentDate);
                                Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                if (isCurrentSchdate && isCurrentUno) {
                                    sgresult.setClsno("55");
                                    sgresult.setClspr(8);
                                }
                            }
                        }
                        List<String> r55UnoList = new ArrayList<>();
                        for (Sgbackup sgbackup : sgbackupList) {
                            Boolean is55 = sgbackup.getClsno().equals("55")
                                || sgbackup.getClsno().equals("daily")
                                || r55BreastFeedUnoList.contains(sgbackup.getUno());
                            Boolean isCurrentYear = sgbackup.getSchdate().getYear() == monthStart.getYear();
                            Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == monthStart.getMonthValue();
                            Boolean isAvailable = sgresultList
                                .stream()
                                .filter(sgresult -> {
                                    Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentDate);
                                    Boolean isCurrentUno = sgresult.getUno().equals(sgbackup.getUno());
                                    Boolean isCurrentClsno = sgresult.getClsno().equals("A0") || sgresult.getClsno().equals("D6") || sgresult.getClsno().equals("公休");
                                    return isCurrentSchdate && isCurrentUno && isCurrentClsno;
                                })
                                .collect(Collectors.toList())
                                .size() == 0;
                            if (is55 && isCurrentYear && isCurrentMonth && isAvailable) {
                                r55UnoList.add(sgbackup.getUno());
                            }
                        }
                        for (String currentUno : r55UnoList) {
                            Boolean isUnavailable = sgresultList
                                .stream()
                                .filter(sgresult -> {
                                    Boolean isTomorrow = sgresult.getSchdate().equals(currentDate.plusDays(1));
                                    Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                    Boolean isCurrentClsno = sgresult.getClsno().equals("A0");
                                    return isTomorrow && isCurrentUno && isCurrentClsno;
                                })
                                .collect(Collectors.toList())
                                .size() > 0
                                || sgresultList
                                .stream()
                                .filter(sgresult -> {
                                    Boolean isYesterday = sgresult.getSchdate().equals(currentDate.minusDays(1));
                                    Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                    Boolean isCurrentClsno = sgresult.getClsno().equals("A0") || sgresult.getClsno().equals("A8") || sgresult.getClsno().equals("D6");
                                    return isYesterday && isCurrentUno && isCurrentClsno;
                                })
                                .collect(Collectors.toList())
                                .size() > 0
                                || lastMonthSgresultList
                                .stream()
                                .filter(sgresult -> {
                                    Boolean isYesterday = sgresult.getSchdate().equals(currentDate.minusDays(1));
                                    Boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                    Boolean isCurrentClsno = sgresult.getClsno().equals("A0") || sgresult.getClsno().equals("A8") || sgresult.getClsno().equals("D6");
                                    return isYesterday && isCurrentUno && isCurrentClsno;
                                })
                                .collect(Collectors.toList())
                                .size() > 0;
                            if (isUnavailable) {
                                for (Sgresult sgresult : sgresultList) {
                                    boolean isCurrentSchdate = sgresult.getSchdate().equals(currentDate);
                                    boolean isCurrentUno = sgresult.getUno().equals(currentUno);
                                    boolean isCurrentClsno = !(sgresult.getClsno().equals("A0") || sgresult.getClsno().equals("D6"));
                                    if (isCurrentSchdate && isCurrentUno && isCurrentClsno) {

                                        sgresult.setClsno("OFF");
                                        sgresult.setClspr(0);
                                    }
                                }
                            }
                        }
                    }
                    // A8 排班
                    if (isWeekend) {
                        for (Sgbackup sgbackup : sgbackupList) {
                            Boolean isCurrentYear = sgbackup.getSchdate().getYear() == currentDate.getYear();
                            Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == currentDate.getMonthValue();
                            Boolean isCurrentClsno = sgbackup.getClsno().equals("A8");
                            if (isCurrentYear && isCurrentMonth && isCurrentClsno) {
                                for (Sgresult sgresult : sgresultList) {
                                    Boolean isCurrentUno = sgresult.getUno().equals(sgbackup.getUno()) || sgresult.getUno().equals(ra8BreastFeedUno);
                                    Boolean isAvailable = !(sgresult.getClsno().equals("A0") || sgresult.getClsno().equals("D6"));
                                    Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentDate);
                                    if (isCurrentUno && isCurrentSchdate && isAvailable) {
                                        sgresult.setClsno("OFF");
                                        sgresult.setClspr(0);
                                    }
                                }
                            }
                        }
                    } else {
                        List<String> ra8AvailableList = new ArrayList<>();
                        for (Sgbackup sgbackup : sgbackupList) {
                            Boolean isCurrentYear = sgbackup.getSchdate().getYear() == monthStart.getYear();
                            Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == monthStart.getMonthValue();
                            if (isCurrentYear && isCurrentMonth) {
                                if (ra8ABCList.contains(sgbackup.getUno()) || sgbackup.getUno().equals(ra8BreastFeedUno)) {
                                    Boolean isAvailable = sgresultList
                                        .stream()
                                        .filter(sgresult -> {
                                            Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentDate);
                                            Boolean isCurrentClsno = sgresult.getClsno().equals("D6") || sgresult.getClsno().equals("A0");
                                            Boolean isCurrentUno = sgresult.getUno().equals(sgbackup.getUno());
                                            return isCurrentSchdate && isCurrentClsno && isCurrentUno;
                                        })
                                        .map(sgresult -> sgresult.getUno())
                                        .collect(Collectors.toList())
                                        .size() == 0;
                                    if (isAvailable) {
                                        ra8AvailableList.add(sgbackup.getUno());
                                    }
                                }
                                Boolean isAnnaulLeave = sgbackup.getClsno().equals("公休");
                                if (isAnnaulLeave) {
                                    for (int index = 0; index < ra8AvailableList.size(); index++) {
                                        if (ra8AvailableList.get(index).equals(sgbackup.getUno())) {
                                            ra8AvailableList.remove(index);
                                        }
                                    }
                                }
                            }
                        }
                        List<String> unavailableA8List = new ArrayList<>();
                        for (Sgbackup unavailable : unavailableList) {
                            Boolean isCurrentSchdate = unavailable.getSchdate().equals(currentDate);
                            Boolean isAnnualLeave = unavailable.getClsno().equals("公休");
                            Boolean isAvailable = false;
                            for (Sgbackup sgbackup : sgbackupList) {
                                Boolean isCurrentYear = sgbackup.getSchdate().getYear() == currentDate.getYear();
                                Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == currentDate.getMonthValue();
                                if (isCurrentYear && isCurrentMonth) {
                                    Boolean isCurrentClsno = sgbackup.getClsno().equals("A8")
                                        && sgresultList
                                        .stream()
                                        .filter(sgresult -> {
                                            Boolean isYesterday = sgresult.getSchdate().equals(currentDate.minusDays(1));
                                            Boolean isUnavailable = sgresult.getUno().equals(sgbackup.getUno())
                                                && sgresult.getClsno().equals("D6");
                                            return isYesterday && isUnavailable;
                                        })
                                        .collect(Collectors.toList())
                                        .size() == 0
                                        && lastMonthSgresultList
                                        .stream()
                                        .filter(sgresult -> {
                                            Boolean isYesterday = sgresult.getSchdate().equals(currentDate.minusDays(1));
                                            Boolean isUnavailable = sgresult.getUno().equals(sgbackup.getUno())
                                                && sgresult.getClsno().equals("D6");
                                            return isYesterday && isUnavailable;
                                        })
                                        .collect(Collectors.toList())
                                        .size() == 0;
                                    Boolean isCurrentUno = sgbackup.getUno().equals(unavailable.getUno());
                                    if (isCurrentClsno && isCurrentUno) {
                                        isAvailable = true;
                                    }
                                }
                            }
                            if (isCurrentSchdate && isAnnualLeave && isAvailable) {
                                unavailableA8List.add(unavailable.getUno());
                            }
                        }
                        // 如果哺乳班請公休，則將原來占用 A8 班的額度釋出。
                        int currentManpower = ra8BreastFeedUno.equals("") ? 2 : ra8Manpower;
                        if (ra8AvailableList.size() < currentManpower) {
                            int numberOfShortages = currentManpower - ra8AvailableList.size();
                            if (unavailableA8List.size() >= numberOfShortages) {
                                ra8AvailableList.addAll(getRandomList(unavailableA8List, numberOfShortages));
                            } else {
                                ra8AvailableList.addAll(unavailableA8List);
                                numberOfShortages = numberOfShortages - unavailableA8List.size();
                                List<String> ra8UnoList = new ArrayList<>();
                                for (Sgbackup sgbackup : sgbackupList) {
                                    Boolean isCurrentYear = sgbackup.getSchdate().getYear() == currentDate.getYear();
                                    Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == currentDate.getMonthValue();
                                    Boolean isCurrentClsno = sgbackup.getClsno().equals("A8")
                                        || (
                                        sgruserBreastFeedList
                                            .stream()
                                            .map(sgruser -> sgruser.getUno())
                                            .collect(Collectors.toList())
                                            .contains(sgbackup.getUno())
                                            && !r55BreastFeedUnoList.contains(sgbackup.getUno())
                                    );
                                    if (isCurrentYear && isCurrentMonth && isCurrentClsno) {
                                        ra8UnoList.add(sgbackup.getUno());
                                    }
                                }
                                for (Sgbackup sgbackup : unavailableList) {
                                    Boolean isCurrentSchdate = sgbackup.getSchdate().equals(currentDate);
                                    Boolean isCurrentUno = ra8UnoList.contains(sgbackup.getUno());
                                    Boolean isCurrentClsno = sgbackup.getClsno().equals("公休");
                                    if (isCurrentSchdate && isCurrentUno && isCurrentClsno) {

                                        ra8AvailableList.add(sgbackup.getUno());
                                    }
                                }
                            }
                        }
                        // 更新排班結果表
                        if (!ra8BreastFeedUno.equals("")) {
                            ra8AvailableList.add(ra8BreastFeedUno);
                        }
                        if (idleUnoList.size() > 0) {
                            ra8AvailableList.addAll(idleUnoList);
                        }
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
            for (LocalDate currentSchdate = monthStart; currentSchdate.isBefore(monthEnd.plusDays(1)); currentSchdate = currentSchdate.plusDays(1)) {
                // 55 班人數檢查，不足部分由符合資格的 A8 班人員代替
                int numberOf55 = 0;
                for (Sgresult sgresult : sgresultList) {
                    Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentSchdate);
                    Boolean isCurrentClsno = sgresult.getClsno().equals("55");
                    if (isCurrentSchdate && isCurrentClsno) {
                        numberOf55++;
                    }
                }
                DayOfWeek day = DayOfWeek.of(currentSchdate.get(ChronoField.DAY_OF_WEEK));
                int r55Manpower = 0;
                if (day == DayOfWeek.SUNDAY || day == DayOfWeek.SATURDAY) {
                    r55Manpower = 2;
                } else {
                    r55Manpower = r55RoomOpen * r55NeedManpower + r55Wait + r55Nurse + r55WorkStat + r55OPDOR;
                }
                int numberOfShortages = r55Manpower - numberOf55;
                if (numberOfShortages > 0) {
                    int numberOfA8 = 0;
                    List<String> ra8AlternativeUnoList = new ArrayList<>();
                    for (Sgresult sgresult : sgresultList) {
                        LocalDate finalCurrentSchdate = currentSchdate;
                        Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentSchdate);
                        Boolean isA8 = sgresult.getClsno().equals("A8");
                        if (isCurrentSchdate && isA8) {
                            numberOfA8++;
                        }
                        Boolean isAvailable = true;
                        for (Sgresult sgresult2 : sgresultList) {
                            Boolean isTomorrow = sgresult2.getSchdate().equals(currentSchdate.plusDays(1));
                            Boolean isYesterday = sgresult2.getSchdate().equals(currentSchdate.minusDays(1));
                            Boolean isCurrentUno = sgresult2.getUno().equals(sgresult.getUno());
                            Boolean isD6AndA8 = sgresult2.getClsno().equals("D6") || sgresult2.getClsno().equals("A8");
                            Boolean isA0 = sgresult2.getClsno().equals("A0");
                            if (isYesterday && isCurrentUno && isD6AndA8) {
                                isAvailable = false;
                            }
                            if (isTomorrow && isCurrentUno && isA0) {
                                isAvailable = false;
                            }
                        }
                        for (Sgresult sgresult2 : lastMonthSgresultList) {
                            Boolean isYesterday = sgresult2.getSchdate().equals(currentSchdate.minusDays(1));
                            Boolean isCurrentUno = sgresult2.getUno().equals(sgresult.getUno());
                            Boolean isCurrentClsno = sgresult2.getClsno().equals("D6") || sgresult2.getClsno().equals("A8");
                            if (isYesterday && isCurrentUno && isCurrentClsno) {
                                isAvailable = false;
                            }
                        }
                        if (isCurrentSchdate && isA8 && isAvailable) {
                            Boolean isNotBreastFeeding = true;
                            for (Sgbackup sgbackup : sgbackupList) {
                                Boolean isCurrentYear = sgbackup.getSchdate().getYear() == currentSchdate.getYear();
                                Boolean isCurrentMonth = sgbackup.getSchdate().getMonthValue() == currentSchdate.getMonthValue();
                                Boolean isCurrentUno = sgbackup.getUno().equals(sgresult.getUno());
                                Boolean isBreastFeeding = sgbackup.getClsno().equals("哺乳");
                                if (isCurrentYear && isCurrentMonth && isCurrentUno && isBreastFeeding) {
                                    isNotBreastFeeding = false;
                                }
                            }
                            if (isNotBreastFeeding) {
                                ra8AlternativeUnoList.add(sgresult.getUno());
                            }
                        }
                    }
                    int remainingA8 = numberOfA8 - ra8AlternativeUnoList.size();
                    if (remainingA8 < 2) {
                        int numberOfA8Shortages = 2 - remainingA8;
                        for (int index = 0; index < numberOfA8Shortages; index++) {
                            if (ra8AlternativeUnoList.size() > 0) {
                                int random = rand.nextInt(ra8AlternativeUnoList.size());
                                ra8AlternativeUnoList.remove(random);
                            }
                        }
                    }
                    for (int index = 0; index < numberOfShortages; index++) {
                        if (ra8AlternativeUnoList.size() > 0) {
                            String ra8AlternativeUno = getRandomList(ra8AlternativeUnoList, 1).get(0);
                            for (Sgresult sgresult : sgresultList) {
                                Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentSchdate);
                                Boolean isCurrentClsno = sgresult.getClsno().equals("A8");
                                Boolean isCurrentUno = sgresult.getUno().equals(ra8AlternativeUno);
                                if (isCurrentSchdate && isCurrentClsno && isCurrentUno) {
                                    sgresult.setClsno("55");
                                    sgresult.setClspr(8);
                                    numberOfShortages--;
                                }
                            }
                        }
                    }
                    if (numberOfShortages > 0) {
                        // 如 55 班可出勤人數不足，則從今日其他班別 OFF 名單中挑選遞補人員。
                        List<String> alternativeUnoList = new ArrayList<>();
                        for (Sgresult sgresult : sgresultList) {
                            Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentSchdate);
                            Boolean isDayOff = sgresult.getClsno().equals("OFF");
                            if (isCurrentSchdate && isDayOff) {
                                for (Sgresult tomorrowSgresult : sgresultList) {
                                    Boolean isCurrentUno = tomorrowSgresult.getUno().equals(sgresult.getUno());
                                    Boolean isTomorrow = tomorrowSgresult.getSchdate().equals(currentSchdate.plusDays(1));
                                    if (isCurrentUno && isTomorrow) {
                                        if (tomorrowSgresult.getClsno().equals("OFF")) {
                                            for (Sgresult yesterdaySgresult : sgresultList) {
                                                Boolean isYesterday = yesterdaySgresult.getSchdate().equals(currentSchdate.minusDays(1));
                                                Boolean isCurrentClsno = yesterdaySgresult.getClsno().equals("55")
                                                    || yesterdaySgresult.getClsno().equals("A0")
                                                    || yesterdaySgresult.getClsno().equals("OFF");
                                                Boolean isAvailable = yesterdaySgresult.getUno().equals(sgresult.getUno());
                                                if (isYesterday && isCurrentClsno && isAvailable) {
                                                    alternativeUnoList.add(sgresult.getUno());
                                                }
                                            }
                                            for (Sgresult yesterdaySgresult : lastMonthSgresultList) {
                                                Boolean isYesterday = yesterdaySgresult.getSchdate().equals(currentSchdate.minusDays(1));
                                                Boolean isCurrentClsno = yesterdaySgresult.getClsno().equals("55")
                                                    || yesterdaySgresult.getClsno().equals("A0")
                                                    || yesterdaySgresult.getClsno().equals("OFF");
                                                Boolean isAvailable = yesterdaySgresult.getUno().equals(sgresult.getUno());
                                                if (isYesterday && isCurrentClsno && isAvailable) {
                                                    alternativeUnoList.add(sgresult.getUno());
                                                }
                                            }
                                        }
                                        if (tomorrowSgresult.getClsno().equals("D6")) {
                                            for (Sgresult yesterdaySgresult : sgresultList) {
                                                Boolean isYesterday = yesterdaySgresult.getSchdate().equals(currentSchdate.minusDays(1));
                                                Boolean isCurrentClsno = yesterdaySgresult.getClsno().equals("OFF");
                                                Boolean isAvailable = yesterdaySgresult.getUno().equals(sgresult.getUno());
                                                if (isYesterday && isCurrentClsno && isAvailable) {
                                                    alternativeUnoList.add(sgresult.getUno());
                                                }
                                            }
                                            for (Sgresult yesterdaySgresult : lastMonthSgresultList) {
                                                Boolean isYesterday = yesterdaySgresult.getSchdate().equals(currentSchdate.minusDays(1));
                                                Boolean isCurrentClsno = yesterdaySgresult.getClsno().equals("OFF");
                                                Boolean isAvailable = yesterdaySgresult.getUno().equals(sgresult.getUno());
                                                if (isYesterday && isCurrentClsno && isAvailable) {
                                                    alternativeUnoList.add(sgresult.getUno());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        for (String uno : alternativeUnoList) {
                            for (Sgresult sgresult : sgresultList) {
                                Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentSchdate);
                                Boolean isCurrentUno = sgresult.getUno().equals(uno);
                                if (isCurrentSchdate && isCurrentUno) {
                                    sgresult.setClsno("55");
                                    sgresult.setClspr(8);
                                }
                            }
                        }
                    }

                }

                // A0 本月首日與上月最後一日衝突檢查。
                // 如發生衝突，則找上月最後一日為 OFF 的人代替。
                List<String> rA0UnoList = new ArrayList<>();
                for (Sgresult sgresult : sgresultList) {
                    Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentSchdate);
                    Boolean isCurrentClsno = sgresult.getClsno().equals("A0");
                    if (isCurrentSchdate && isCurrentClsno) {
                        rA0UnoList.add(sgresult.getUno());
                    }
                }
                for (String uno : rA0UnoList) {
                    Boolean isWorkday1 = false;
                    Boolean isWorkday2 = false;
                    Boolean isWorkday3 = false;
                    Boolean isWorkday4 = false;
                    for (Sgresult sgresult : sgresultList) {
                        Boolean isFirstDay = sgresult.getSchdate().equals(monthStart);
                        Boolean isSecondDay = sgresult.getSchdate().equals(monthStart.plusDays(1));
                        Boolean isThirdDay = sgresult.getSchdate().equals(monthStart.plusDays(2));
                        Boolean isFourthDay = sgresult.getSchdate().equals(monthStart.plusDays(3));
                        Boolean isCurrentClsno = sgresult.getClsno().equals("A0");
                        Boolean isCurrentUno = sgresult.getUno().equals(uno);
                        if (isCurrentClsno && isCurrentUno) {
                            if (isFirstDay) isWorkday1 = true;
                            if (isSecondDay) isWorkday2 = true;
                            if (isThirdDay) isWorkday3 = true;
                            if (isFourthDay) isWorkday4 = true;
                        }
                    }
                    if (isWorkday1 && isWorkday2 && isWorkday3 && isWorkday4) {
                        List<String> rD6UnoList = sgbackupList
                            .stream()
                            .filter(sgbackup -> {
                                Boolean isCurrentYear = sgbackup.getSchdate().getYear() == monthStart.getYear();
                                Boolean isCurrentMonth = sgbackup.getSchdate().getMonth() == monthStart.getMonth();
                                Boolean isCurrentClsno = sgbackup.getClsno().equals("D6");
                                return isCurrentYear && isCurrentMonth && isCurrentClsno;
                            })
                            .map(sgbackup -> sgbackup.getUno())
                            .collect(Collectors.toList());
                        List<String> alternativeUnoList = new ArrayList<>();
                        for (Sgresult sgresult : lastMonthSgresultList) {
                            Boolean isCurrentSchdate = sgresult.getSchdate().equals(lastMonthEnd);
                            Boolean isDayOff = sgresult.getClsno().equals("OFF");
                            Boolean isAvailable = !(rD6UnoList.contains(sgresult.getUno())
                                || r55HolidayABCList.contains(sgresult.getUno())
                                || sgruserRegularList.stream().map(sgruser -> sgruser.getUno()).collect(Collectors.toList()).contains(sgresult.getUno())
                                || sgruserBreastFeedList.stream().map(sgruser -> sgruser.getUno()).collect(Collectors.toList()).contains(sgresult.getUno())
                            );
                            if (isCurrentSchdate && isDayOff && isAvailable) {
                                alternativeUnoList.add(sgresult.getUno());
                            }
                        }
                        Boolean isConflict = false;
                        for (Sgresult sgresult : lastMonthSgresultList) {
                            Boolean isCurrentSchdate = sgresult.getSchdate().equals(lastMonthEnd);
                            Boolean isCurrentClsno = !sgresult.getClsno().equals("OFF");
                            Boolean isCurrentUno = sgresult.getUno().equals(uno);
                            if (isCurrentSchdate && isCurrentClsno && isCurrentUno) {
                                isConflict = true;
                            }
                        }
                        // 如有發生衝突、且有可提換人選，則再替換後，於次日讓替換人員補假。
                        if (isConflict && alternativeUnoList.size() > 0) {
                            String alternativeUno = getRandomList(alternativeUnoList, 1).get(0);
                            for (Sgresult sgresult : sgresultList) {
                                Boolean isCurrentSchdate = sgresult.getSchdate().equals(monthStart);
                                Boolean isCurrentUno = sgresult.getUno().equals(uno);
                                Boolean isAlternativeUno = sgresult.getUno().equals(alternativeUno);
                                if (isCurrentSchdate) {
                                    if (isCurrentUno) {
                                        sgresult.setClsno("OFF");
                                        sgresult.setClspr(0);
                                    }
                                    if (isAlternativeUno) {
                                        sgresult.setClsno("A0");
                                        sgresult.setClspr(8);
                                    }
                                }
                            }
                            for (Sgresult sgresult : sgresultList) {
                                Boolean isCurrentSchdate = sgresult.getSchdate().equals(monthStart.plusDays(1));
                                Boolean isAlternativeUno = sgresult.getUno().equals(alternativeUno);
                                if (isCurrentSchdate && isAlternativeUno) {
                                    sgresult.setClsno("OFF");
                                    sgresult.setClspr(0);
                                }
                            }
                        }
                    }
                }
                // D6 和 A0 公休換班檢查。
                // 如遇公休，則找前一天 OFF 班，且今天非公休的人代替，沒有適當頂替人選，則不予公休。
                List<String> D6AndA0UnoList = new ArrayList<>();
                for (Sgresult sgresult : sgresultList) {
                    Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentSchdate);
                    Boolean isCurrentClsno = sgresult.getClsno().equals("D6") || sgresult.getClsno().equals("A0");
                    if (isCurrentSchdate && isCurrentClsno) {
                        D6AndA0UnoList.add(sgresult.getUno());
                    }
                }
                List<String> annualLeaveUnoList = new ArrayList<>();
                for (Sgbackup sgbackup : unavailableList) {
                    Boolean isCurrentSchdate = sgbackup.getSchdate().equals(currentSchdate);
                    Boolean isAnnualLeave = sgbackup.getClsno().equals("公休");
                    if (isCurrentSchdate && isAnnualLeave) {
                        annualLeaveUnoList.add(sgbackup.getUno());
                    }
                }
                List<String> D6AndA0AnnualLeaveUnoList = annualLeaveUnoList
                    .stream()
                    .filter(uno -> D6AndA0UnoList.contains(uno))
                    .collect(Collectors.toList());
                if (D6AndA0AnnualLeaveUnoList.size() > 0) {
                    List<String> alternativeUnoList = new ArrayList<>();
                    for (Sgresult sgresult : sgresultList) {
                        Boolean isCurrentYear = true;
                        Boolean isCurrentMonth = true;
                        Boolean isYesterday = sgresult.getSchdate().equals(currentSchdate.minusDays(1));
                        Boolean isCurrentClsno = sgresult.getClsno().equals("OFF") || sgresult.getClsno().equals("55");
                        Boolean isCurrentUno = !(annualLeaveUnoList.contains(sgresult.getUno())
                            || D6AndA0UnoList.contains(sgresult.getUno())
                            || r55BreastFeedUnoList.contains(sgresult.getUno())
                            || sgruserRegularList
                            .stream()
                            .filter(sgruser -> sgruser.getUno().equals(sgresult.getUno()))
                            .collect(Collectors.toList())
                            .size() > 0
                        );
                        if (isCurrentYear && isCurrentMonth && isYesterday && isCurrentClsno && isCurrentUno) {
                            alternativeUnoList.add(sgresult.getUno());
                        }
                    }
                    for (String annualLeaveUno : D6AndA0AnnualLeaveUnoList) {
                        if (alternativeUnoList.size() > 0) {
                            String alternativeUno = getRandomList(alternativeUnoList, 1).get(0);
                            String currentClsno = "";
                            for (Sgresult sgresult : sgresultList) {
                                Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentSchdate);
                                if (isCurrentSchdate) {
                                    Boolean isCurrentUno = sgresult.getUno().equals(annualLeaveUno);
                                    Boolean isAlternativeUno = sgresult.getUno().equals(alternativeUno);
                                    if (isCurrentUno) {
                                        currentClsno = sgresult.getClsno();
                                        sgresult.setClsno("公休");
                                        sgresult.setClspr(0);
                                    }
                                    if (isAlternativeUno) {
                                        if (currentClsno.equals("D6")) {
                                            sgresult.setClsno("D6");
                                            sgresult.setClspr(8);
                                        } else if (currentClsno.equals("A0")) {
                                            sgresult.setClsno("A0");
                                            sgresult.setClspr(8);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // A8 人數檢查，優先遞補前一天班為 A8，且今明天日皆為 OFF 者。
                DayOfWeek today = DayOfWeek.of(currentSchdate.get(ChronoField.DAY_OF_WEEK));
                Boolean isWeekday = !(today == DayOfWeek.SUNDAY || today == DayOfWeek.SATURDAY);
                int ra8Counter = 0;
                for (Sgresult sgresult : sgresultList) {
                    Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentSchdate);
                    Boolean isA8 = sgresult.getClsno().equals("A8");
                    if (isCurrentSchdate && isA8) {
                        ra8Counter++;
                    }
                }
                if (ra8Counter < 2 && isWeekday) {
                    int ra8NumberOfShortages = 2 - ra8Counter;
                    List<String> alternativeUnoList = new ArrayList<>();
                    for (Sgresult sgresult : sgresultList) {
                        Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentSchdate);
                        Boolean isDayOff = sgresult.getClsno().equals("OFF");
                        Boolean isTomorrowAvailable = false;
                        Boolean isYesterdayAvailable = false;
                        for (Sgresult sgresult2 : sgresultList) {
                            Boolean isTomorrow = sgresult2.getSchdate().equals(currentSchdate.plusDays(1));
                            Boolean isCurrentUno = sgresult2.getUno().equals(sgresult.getUno());
                            if (isTomorrow && isCurrentUno && sgresult2.getClsno().equals("OFF")) {
                                isTomorrowAvailable = true;
                            }
                        }
                        for (Sgresult sgresult2 : sgresultList) {
                            Boolean isYesterday = sgresult2.getSchdate().equals(currentSchdate.minusDays(1));
                            Boolean isCurrentUno = sgresult2.getUno().equals(sgresult.getUno());
                            Boolean isA8 = sgresult2.getClsno().equals("A8");
                            if (isYesterday && isCurrentUno && isA8) {
                                isYesterdayAvailable = true;
                            }
                        }
                        for (Sgresult sgresult2 : lastMonthSgresultList) {
                            Boolean isYesterday = sgresult2.getSchdate().equals(currentSchdate.minusDays(1));
                            Boolean isCurrentUno = sgresult2.getUno().equals(sgresult.getUno());
                            Boolean isA8 = sgresult2.getClsno().equals("A8");
                            if (isYesterday && isCurrentUno && isA8) {
                                isYesterdayAvailable = true;
                            }
                        }
                        if (isCurrentSchdate && isDayOff && isTomorrowAvailable && isYesterdayAvailable) {
                            alternativeUnoList.add(sgresult.getUno());
                        }
                    }
                    for (int index = 0; index < ra8NumberOfShortages; index++) {
                        if (alternativeUnoList.size() > 0) {
                            String alternativeUno = getRandomList(alternativeUnoList, 1).get(0);
                            for (Sgresult sgresult : sgresultList) {
                                Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentSchdate);
                                Boolean isAlternativeUno = sgresult.getUno().equals(alternativeUno);
                                if (isCurrentSchdate && isAlternativeUno) {
                                    sgresult.setClsno("A8");
                                    sgresult.setClspr(8);
                                }
                            }
                        }
                    }
                }
                // 如遇國定假日，則各班別人力充足時，可以維持最低需求人力為前提，讓剩餘人員放假。
                if (currentMonthNationalHolidaysList.contains(currentSchdate) && isWeekday) {
                    LocalDate finalCurrentSchdate = currentSchdate;
                    List<String> unavailableUnoList = sgresultList
                        .stream()
                        .filter(sgresult -> {
                            Boolean isCurrentSchdate = sgresult.getSchdate().equals(finalCurrentSchdate);
                            Boolean isA0 = sgresult.getClsno().equals("A0");
                            Boolean isD6 = sgresult.getClsno().equals("D6");
                            return isCurrentSchdate && (isA0 || isD6);
                        })
                        .map(sgresult -> sgresult.getUno())
                        .collect(Collectors.toList());
                    List<String> unavailable55List = getRandomList(sgresultList
                            .stream()
                            .filter(sgresult -> {
                                Boolean isCurrentSchdate = sgresult.getSchdate().equals(finalCurrentSchdate);
                                Boolean is55 = sgresult.getClsno().equals("55");
                                return isCurrentSchdate && is55;
                            })
                            .map(sgresult -> sgresult.getUno())
                            .collect(Collectors.toList())
                        ,
                        r55Manpower);
                    unavailableUnoList.addAll(unavailable55List);
                    List<String> unavailableA8List = getRandomList(sgresultList
                            .stream()
                            .filter(sgresult -> {
                                Boolean isCurrentSchdate = sgresult.getSchdate().equals(finalCurrentSchdate);
                                Boolean isA8 = sgresult.getClsno().equals("A8");
                                return isCurrentSchdate && isA8;
                            })
                            .map(sgresult -> sgresult.getUno())
                            .collect(Collectors.toList())
                        ,
                        2);
                    unavailableUnoList.addAll(unavailableA8List);
                    for (Sgresult sgresult : sgresultList) {
                        Boolean isCurrentSchdate = sgresult.getSchdate().equals(currentSchdate);
                        Boolean isCurrentUno = !unavailableUnoList.contains(sgresult.getUno());
                        if (isCurrentSchdate && isCurrentUno) {
                            sgresult.setClsno("OFF");
                            sgresult.setClspr(0);
                        }
                    }
                }
            }

            ra8Replacement(currentMonthNationalHolidaysList, sgresultList, lastMonthSgresultList, ra0ABCList, monthStart, monthEnd, "A0");
            ra8Replacement(currentMonthNationalHolidaysList, sgresultList, lastMonthSgresultList, rd6ABCList, monthStart, monthEnd, "D6");
            setOvertime(sgresultList, sgbackupList, monthStart, monthEnd);
            setRemark(currentMonthNationalHolidaysList, sgresultList, civilServantList, laborList, monthStart, monthEnd);
            sgresultRepository.saveAll(sgresultList);
            sgbackupRepository.saveAll(sgbackupList);
        }

    }

    // 判斷 OFF 班類型，並加入備註。
    public void setRemark(List<LocalDate> currentMonthNationalHolidaysList, List<Sgresult> sgresultList, List<String> civilServantList, List<String> laborList, LocalDate monthStart, LocalDate monthEnd) {
        // 取得當月 YY、ZZ 總數
        int totalYY = 0;
        int totalZZ = 0;
        for (LocalDate currentSchdate = monthStart; currentSchdate.isBefore(monthEnd.plusDays(1)); currentSchdate = currentSchdate.plusDays(1)) {
            DayOfWeek day = DayOfWeek.of(currentSchdate.get(ChronoField.DAY_OF_WEEK));
            if (day == DayOfWeek.SATURDAY) {
                totalYY++;
            }
            if (day == DayOfWeek.SUNDAY) {
                totalZZ++;
            }
        }
        // 取得當月所有週
        List<Integer> weekList = sgresultList
            .stream()
            .map(sgresult -> sgresult.getSchweek())
            .distinct()
            .collect(Collectors.toList());
        for (Sgresult sgresult : sgresultList) {
            Boolean isLabor = laborList.contains(sgresult.getUno());
            Boolean isCivilServant = civilServantList.contains(sgresult.getUno());
            Boolean isDayOff = sgresult.getClsno().equals("OFF");
            if (isDayOff) {
                // 公務員如為 OFF 班，則當天的備註為 00。
                if (isCivilServant) {
                    sgresult.setRemark("00");
                }
                // 勞基法人員如為 OFF 班，則當天的備註為 YY (休息日、週六)、ZZ (例假日、週日) 00 (國定假日)。
                if (isLabor) {
                    Boolean isNationalHoliday = currentMonthNationalHolidaysList.contains(sgresult.getSchdate());
                    DayOfWeek day = DayOfWeek.of(sgresult.getSchdate().get(ChronoField.DAY_OF_WEEK));
                    if (day == DayOfWeek.SATURDAY) {
                        sgresult.setRemark("YY");
                    }
                    if (day == DayOfWeek.SUNDAY) {
                        sgresult.setRemark("ZZ");
                    }
                    if (isNationalHoliday) {
                        sgresult.setRemark("00");
                    }
                }
            }
        }
        for (String uno : laborList) {
            // 尚須補足的 YY、ZZ 數量
            int requiredQuantityOfYY = totalYY;
            int requiredQuantityOfZZ = totalZZ;
            // 扣除已存在的 YY、ZZ
            for (Sgresult sgresult : sgresultList) {
                Boolean isLabor = laborList.contains(sgresult.getUno());
                Boolean isCurrentUno = sgresult.getUno().equals(uno);
                Boolean isDayOff = sgresult.getClsno().equals("OFF");
                if (isLabor && isCurrentUno && isDayOff) {
                    Boolean isYY = sgresult.getRemark().equals("YY");
                    Boolean isZZ = sgresult.getRemark().equals("ZZ");
                    if (isYY) requiredQuantityOfYY--;
                    if (isZZ) requiredQuantityOfZZ--;
                }
            }
            for (int week : weekList) {
                // 檢查當週是否有週六、週日
                Boolean hasYY = false;
                Boolean hasZZ = false;
                for (Sgresult sgresult : sgresultList) {
                    Boolean isCurrentWeek = sgresult.getSchweek() == week;
                    Boolean isCurrentMonth = sgresult.getSchdate().getMonthValue() == monthStart.getMonthValue();
                    if (isCurrentMonth && isCurrentWeek) {
                        DayOfWeek day = DayOfWeek.of(sgresult.getSchdate().get(ChronoField.DAY_OF_WEEK));
                        if (day == DayOfWeek.SATURDAY) {
                            hasYY = true;
                        }
                        if (day == DayOfWeek.SUNDAY) {
                            hasZZ = true;
                        }
                    }
                }
                // 計算當週尚缺多少 YY、ZZ。
                Boolean lackOfYY = hasYY ? true : false;
                Boolean lackOfZZ = hasZZ ? true : false;
                if (lackOfYY || lackOfZZ) {
                    for (Sgresult sgresult : sgresultList) {
                        Boolean isLabor = laborList.contains(sgresult.getUno());
                        Boolean isCurrentUno = sgresult.getUno().equals(uno);
                        Boolean isCurrentWeek = sgresult.getSchweek() == week;
                        Boolean isDayOff = sgresult.getClsno().equals("OFF");
                        if (isLabor && isCurrentUno && isCurrentWeek && isDayOff) {
                            Boolean isYY = sgresult.getRemark().equals("YY");
                            Boolean isZZ = sgresult.getRemark().equals("ZZ");
                            if (isYY) {
                                lackOfYY = false;
                            }
                            if (isZZ) {
                                lackOfZZ = false;
                            }
                        }
                    }
                }
                // 若當週缺少 YY 或 ZZ，則於當週其中一個備註為空的 OFF 班補上備註。
                if (lackOfYY || lackOfZZ) {
                    for (Sgresult sgresult : sgresultList) {
                        Boolean isLabor = laborList.contains(sgresult.getUno());
                        Boolean isCurrentUno = sgresult.getUno().equals(uno);
                        Boolean isCurrentWeek = sgresult.getSchweek() == week;
                        Boolean isDayOff = sgresult.getClsno().equals("OFF");
                        Boolean isEmpty = sgresult.getRemark().equals("");
                        if (isLabor && isCurrentUno && isCurrentWeek && isDayOff && isEmpty) {
                            if (lackOfYY && requiredQuantityOfYY > 0) {
                                sgresult.setRemark("YY");
                                requiredQuantityOfYY--;
                                lackOfYY = false;
                                continue;
                            }
                            if (lackOfZZ && requiredQuantityOfZZ > 0) {
                                sgresult.setRemark("ZZ");
                                requiredQuantityOfZZ--;
                                lackOfZZ = false;
                                break;
                            }
                        }
                    }
                    // 若缺少的 YY、ZZ 沒有可用的日期，則再從國定假日替補。
                    if (lackOfYY || lackOfZZ) {
                        for (Sgresult sgresult : sgresultList) {
                            Boolean isLabor = laborList.contains(sgresult.getUno());
                            Boolean isCurrentUno = sgresult.getUno().equals(uno);
                            Boolean isCurrentWeek = sgresult.getSchweek() == week;
                            Boolean isDayOff = sgresult.getClsno().equals("OFF");
                            Boolean isNationalHoliday = sgresult.getRemark().equals("00");
                            if (isLabor && isCurrentUno && isCurrentWeek && isDayOff && isNationalHoliday) {
                                if (lackOfYY && requiredQuantityOfYY > 0) {
                                    sgresult.setRemark("YY");
                                    requiredQuantityOfYY--;
                                    lackOfYY = false;
                                    continue;
                                }
                                if (lackOfZZ && requiredQuantityOfZZ > 0) {
                                    sgresult.setRemark("ZZ");
                                    requiredQuantityOfZZ--;
                                    lackOfZZ = false;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            // 如備註存在空值，則按 YY、ZZ 缺額補齊。
            String lastRemark = requiredQuantityOfYY > 0 ? "YY" : "ZZ";
            for (Sgresult sgresult : sgresultList) {
                Boolean isLabor = laborList.contains(sgresult.getUno());
                Boolean isCurrentUno = sgresult.getUno().equals(uno);
                Boolean isDayOff = sgresult.getClsno().equals("OFF");
                if (isLabor && isCurrentUno && isDayOff) {
                    Boolean isYY = sgresult.getRemark().equals("YY");
                    Boolean isZZ = sgresult.getRemark().equals("ZZ");
                    Boolean isEmpty = sgresult.getRemark().equals("");
                    if (isYY) {
                        lastRemark = "YY";
                    } else if (isZZ) {
                        lastRemark = "ZZ";
                    } else if (isEmpty) {
                        if (lastRemark.equals("ZZ") && requiredQuantityOfYY > 0) {
                            sgresult.setRemark("YY");
                            requiredQuantityOfYY--;
                            lastRemark = "YY";
                        } else if (lastRemark.equals("YY") && requiredQuantityOfZZ > 0) {
                            sgresult.setRemark("ZZ");
                            requiredQuantityOfZZ--;
                            lastRemark = "ZZ";
                        }else if (requiredQuantityOfYY > 0){
                            sgresult.setRemark("YY");
                            requiredQuantityOfYY--;
                            lastRemark = "YY";
                        }else if (requiredQuantityOfZZ > 0){
                            sgresult.setRemark("ZZ");
                            requiredQuantityOfZZ--;
                            lastRemark = "ZZ";
                        }
                    }
                }
            }
            // 若備住還存在剩餘缺額，檢查是否有國定假日欠假，如是則利用空白備住補足。
            int lackOfNationalHolidays = currentMonthNationalHolidaysList.size() -
                sgresultList.stream().filter(sgresult -> {
                    Boolean isLabor = laborList.contains(sgresult.getUno());
                    Boolean isCurrentUno = sgresult.getUno().equals(uno);
                    Boolean isDayOff = sgresult.getClsno().equals("OFF");
                    Boolean is00 = sgresult.getRemark().equals("00");
                    return isLabor && isCurrentUno && isDayOff && is00;
                }).collect(Collectors.toList()).size();
            for (Sgresult sgresult : sgresultList) {
                if (lackOfNationalHolidays > 0) {
                    Boolean isLabor = laborList.contains(sgresult.getUno());
                    Boolean isCurrentUno = sgresult.getUno().equals(uno);
                    Boolean isDayOff = sgresult.getClsno().equals("OFF");
                    Boolean isEmpty = sgresult.getRemark().equals("");
                    if (isLabor && isCurrentUno && isDayOff && isEmpty) {
                        sgresult.setRemark("00");
                        lackOfNationalHolidays--;
                    }
                }
            }
            // 若備住還存在剩餘缺額，應為人員因輪班而產生的多出的假，輪流在備註補上 YY、ZZ 即可。
            lastRemark = "YY";
            for (Sgresult sgresult : sgresultList) {
                Boolean isLabor = laborList.contains(sgresult.getUno());
                Boolean isCurrentUno = sgresult.getUno().equals(uno);
                Boolean isDayOff = sgresult.getClsno().equals("OFF");
                if (isLabor && isCurrentUno && isDayOff) {
                    Boolean isYY = sgresult.getRemark().equals("YY");
                    Boolean isZZ = sgresult.getRemark().equals("ZZ");
                    Boolean isEmpty = sgresult.getRemark().equals("");
                    if (isYY) {
                        lastRemark = "YY";
                    } else if (isZZ) {
                        lastRemark = "ZZ";
                    } else if (isEmpty) {
                        if (lastRemark.equals("ZZ"))  {
                            sgresult.setRemark("YY");
                            lastRemark = "YY";
                        } else if (lastRemark.equals("YY")) {
                            sgresult.setRemark("ZZ");
                            lastRemark = "ZZ";
                        }
                    }
                }
            }
        }
    }

    @GetMapping("/solve")
    public ResponseEntity solve(LocalDate startSchdate, LocalDate endSchdate) {
        ResponseEntity checkManpowerResponse = checkManpower();
        if (checkManpowerResponse.getStatusCode().value() == 500) return checkManpowerResponse;
        ResponseEntity checkUteamResponse = checkUteam();
        if (checkUteamResponse.getStatusCode().value() == 500) return checkUteamResponse;
        cleanSgbackupAndSgresult(startSchdate, endSchdate);
        cleanSgsch(startSchdate, endSchdate);
        backupSgsch(startSchdate, endSchdate);
        scheduling(startSchdate, endSchdate);
        syncSgsch(startSchdate, endSchdate);
        // 若執行成功，則回傳 success。
        Map<String, String> result = new LinkedHashMap<>();
        result.put("httpStatusCode", "200");
        result.put("status", "success");
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(result);
    }
}
