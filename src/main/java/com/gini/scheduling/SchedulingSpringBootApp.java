package com.gini.scheduling;

import com.gini.scheduling.model.Sgruser;
import com.gini.scheduling.model.Sgsch;
import com.gini.scheduling.model.Sgsys;
import com.gini.scheduling.dao.SgrroomRepository;
import com.gini.scheduling.dao.SgschRepository;
import com.gini.scheduling.dao.SgruserRepository;
import com.gini.scheduling.dao.SgsysRepository;
import com.gini.scheduling.utils.DateGenerator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.util.*;

@SpringBootApplication
public class SchedulingSpringBootApp extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(SchedulingSpringBootApp.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(SchedulingSpringBootApp.class, args);
    }


    @Bean
    public CommandLineRunner demoData(SgschRepository sgschRepository,
                                      SgruserRepository sgruserRepository, SgrroomRepository sgrroomRepository, SgsysRepository sgsysRepository) {
        return (args) -> {
            if (sgruserRepository.findAll().isEmpty()) {
                String[] unos = {"0435", "1873", "2753", "1641", "1135", "G346", "1592", "1189", "4749", "A021", "H154", "0309", "A169", "G844", "H245", "4891", "G755", "H041", "G983", "1183", "G684", "2477", "4987", "G861", "G845", "0676", "4988", "G715", "H112", "H178", "0314", "G596", "H070", "4304", "G591", "6713", "G878", "H155", "1914", "4805", "G877", "1184", "4922", "F680", "G919", "H302", "G097", "H203", "A263", "F476", "G633", "G595", "1792", "4994", "G730", "1271", "0679", "G273", "H045", "2940", "A014", "4990", "H017", "F608", "G846", "4808", "G360", "H256", "0696", "G008", "H116", "G764", "A023", "A016", "4835", "1657", "1741", "4806", "F675", "4828", "0335", "A020", "4898", "G862", "1582", "4807", "4893", "F451", "1257", "4338", "F793", "1309", "G140", "G336", "G470", "A111", "F942", "F721", "4723", "A109", "2279", "4801"};
                String[] unames = {"王淑玲", "林麗華", "丁珮鈞", "林秀萍", "王窈慧", "李皓柔", "胡繕如", "張琳明", "周河呈", "蔡靜宜", "王詩萍", "陳惠玲", "何庭菁", "侯念綺", "雷翔順", "王清雲", "王鈺雅", "李冠儀", "許如逸", "王年春", "呂東桓", "侯瓊惠", "陳榿婷", "洪敏華", "鄭閔羽", "楊萱華", "鄭玲瑋", "孟冠妤", "姜蕙芬", "李俊廷", "溫碧蓮", "周筱浣", "陳姿璉", "呂珮芳", "陳香融", "蔡玉瑛", "莊晴渝", "陳乃慈", "李玉珠", "李珈瑩", "林庭宜", "陳瓊惠", "曾珮綺", "呂之晴", "吳竺螢", "蔡嘉玲", "熊基材", "許雅雯", "陳黛瑋", "謝蕙玲", "黃譯嫻", "陳玅廷", "李珮瑄", "翁家瑜", "方宣雯", "趙新鳳", "連育柔", "施明玲", "孔霈甄", "吳惠卿", "呂俊宏", "丁心怡", "陳姿吟", "李宛真", "鄭雅如", "傅寶慧", "賴仙如", "藍怡妮", "孫嘉文", "朱博儀", "余育昀", "陳佩伶", "吳冠佑", "梁惟婷", "黃瑋婷", "劉美玲", "許素月", "劉憶如", "陳玉芬", "洪筱玉", "高招敏", "蔡念恆", "吳秋慧", "李茵茵", "林桂合", "洪佳楓", "王翊潔", "吳艾", "鍾明玲", "黃雅鈴", "孫玉欣", "陳香祺", "郭祝君", "聶均容", "林岫蓁", "楊宸芸", "邱詩婷", "黃鈺玲", "杜怡萱", "謝昱瑩", "賴慈惠", "楊環霙"};
                String[] uteams = {"A", "B", "C"};
                for (int index = 0; index < unos.length; index++) {
                    int randomNumber = new Random().nextInt(uteams.length);
                    sgruserRepository.save(new Sgruser(unos[index], unames[index], uteams[randomNumber]));
                }
                if (sgschRepository.findAll().isEmpty()) {
                    Map<String, Integer> sgschs = new HashMap<>();
                    sgschs.put("D6", 2);
                    sgschs.put("A0", 2);
                    sgschs.put("55", 24);
                    sgschs.put("A8", 2);
                    sgschs.put("常日", 4);
                    sgschs.put("哺乳", 1);
                    sgschs.put("加班", 1);

                    DateGenerator newDateUtil = new DateGenerator();
                    List<DateGenerator.WeekInfo> weeks = newDateUtil.getScope("2022", "9");

                    for (int week = 0; week < weeks.size(); week++) {
                        int currentWeek = week + 1;
                        Iterator<Sgruser> sgrusers = sgruserRepository.findAll().iterator();
                        LocalDate startDate = LocalDate.parse(weeks.get(week).getStart());
                        LocalDate endDate = LocalDate.parse(weeks.get(week).getEnd());
                        int totalDates = endDate.getDayOfMonth() - startDate.getDayOfMonth() + 1;
                        for (int dateIndex = 0; dateIndex < totalDates; dateIndex++) {
                            LocalDate currentDate
                                    = startDate.plusDays(dateIndex);
                            for (Map.Entry<String, Integer> entry : sgschs.entrySet()) {
                                for (int count = 0; count < entry.getValue(); count++) {
                                    if (!sgrusers.hasNext()) {
                                        sgrusers = sgruserRepository.findAll().iterator();
                                    }
                                    Sgruser sgruser = sgrusers.next();
                                    sgschRepository.save(new Sgsch(sgruser, currentDate, currentWeek, entry.getKey()));

                                }
                            }

                        }
                    }
                }
            }
            if (sgsysRepository.findAll().isEmpty()) {
                Map<String, String> sgsys = new HashMap<>();
                // 55(8-16)
                sgsys.put("r55RoomOpen", "12");
                sgsys.put("r55NeedManpower", "24");
                sgsys.put("r55HolidayDay", "2");
                sgsys.put("r55HolidayA", "5");
                sgsys.put("r55HolidayB", "7");
                sgsys.put("r55HolidayC", "9");
                // D6(16-00)
                sgsys.put("rd6ManpowerA", "5");
                sgsys.put("rd6manpowerB", "7");
                sgsys.put("rd6ManpowerC", "9");
                sgsys.put("rd6HolidayDay", "2");
                // A0(00-08)
                sgsys.put("ra0ManpowerA", "5");
                sgsys.put("ra0ManpowerB", "7");
                sgsys.put("ra0ManpowerC", "9");
                sgsys.put("ra0HolidayDay", "2");
                // A8(12-20)
                sgsys.put("ra8ManpowerA", "5");
                sgsys.put("ra8ManpowerB", "7");
                sgsys.put("ra8ManpowerC", "9");
                sgsys.put("ra8HolidayDay", "2");
                // 通用規則
                sgsys.put("generalBetweenHour", "12");

                for (Map.Entry<String, String> entry : sgsys.entrySet()) {
                    sgsysRepository.save(new Sgsys(entry.getKey(), entry.getValue()));
                }
            }
        };
    }
}
