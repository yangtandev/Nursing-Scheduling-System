package com.gini.scheduling;

import com.gini.scheduling.domain.Shift;
import com.gini.scheduling.domain.Staff;
import com.gini.scheduling.persistence.ShiftRepository;
import com.gini.scheduling.persistence.StaffRepository;
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
    public CommandLineRunner demoData(ShiftRepository shiftRepository,
                                      StaffRepository staffRepository) {
        return (args) -> {
            if (staffRepository.findAll().isEmpty()) {
                String[] cardIds = {"0435", "1873", "2753", "1641", "1135", "G346", "1592", "1189", "4749", "A021", "H154", "0309", "A169", "G844", "H245", "4891", "G755", "H041", "G983", "1183", "G684", "2477", "4987", "G861", "G845", "0676", "4988", "G715", "H112", "H178", "0314", "G596", "H070", "4304", "G591", "6713", "G878", "H155", "1914", "4805", "G877", "1184", "4922", "F680", "G919", "H302", "G097", "H203", "A263", "F476", "G633", "G595", "1792", "4994", "G730", "1271", "0679", "G273", "H045", "2940", "A014", "4990", "H017", "F608", "G846", "4808", "G360", "H256", "0696", "G008", "H116", "G764", "A023", "A016", "4835", "1657", "1741", "4806", "F675", "4828", "0335", "A020", "4898", "G862", "1582", "4807", "4893", "F451", "1257", "4338", "F793", "1309", "G140", "G336", "G470", "A111", "F942", "F721", "4723", "A109", "2279", "4801"};
                String[] names = {"王淑玲", "林麗華", "丁珮鈞", "林秀萍", "王窈慧", "李皓柔", "胡繕如", "張琳明", "周河呈", "蔡靜宜", "王詩萍", "陳惠玲", "何庭菁", "侯念綺", "雷翔順", "王清雲", "王鈺雅", "李冠儀", "許如逸", "王年春", "呂東桓", "侯瓊惠", "陳榿婷", "洪敏華", "鄭閔羽", "楊萱華", "鄭玲瑋", "孟冠妤", "姜蕙芬", "李俊廷", "溫碧蓮", "周筱浣", "陳姿璉", "呂珮芳", "陳香融", "蔡玉瑛", "莊晴渝", "陳乃慈", "李玉珠", "李珈瑩", "林庭宜", "陳瓊惠", "曾珮綺", "呂之晴", "吳竺螢", "蔡嘉玲", "熊基材", "許雅雯", "陳黛瑋", "謝蕙玲", "黃譯嫻", "陳玅廷", "李珮瑄", "翁家瑜", "方宣雯", "趙新鳳", "連育柔", "施明玲", "孔霈甄", "吳惠卿", "呂俊宏", "丁心怡", "陳姿吟", "李宛真", "鄭雅如", "傅寶慧", "賴仙如", "藍怡妮", "孫嘉文", "朱博儀", "余育昀", "陳佩伶", "吳冠佑", "梁惟婷", "黃瑋婷", "劉美玲", "許素月", "劉憶如", "陳玉芬", "洪筱玉", "高招敏", "蔡念恆", "吳秋慧", "李茵茵", "林桂合", "洪佳楓", "王翊潔", "吳艾", "鍾明玲", "黃雅鈴", "孫玉欣", "陳香祺", "郭祝君", "聶均容", "林岫蓁", "楊宸芸", "邱詩婷", "黃鈺玲", "杜怡萱", "謝昱瑩", "賴慈惠", "楊環霙"};
                String[] teams = {"A", "B", "C"};
                for (int index = 0; index < cardIds.length; index++) {
                    int randomNumber = new Random().nextInt(teams.length);
                    staffRepository.save(new Staff(cardIds[index], names[index], teams[randomNumber]));
                }
                if (shiftRepository.findAll().isEmpty()) {

                    Map<String, Integer> shifts = new HashMap<>();
                    shifts.put("D6", 2);
                    shifts.put("A0", 2);
                    shifts.put("55", 24);
                    shifts.put("A8", 2);
                    shifts.put("常日", 4);
                    shifts.put("哺乳", 1);
                    shifts.put("加班", 1);

                    DateGenerator newDateUtil = new DateGenerator();
                    List<DateGenerator.WeekInfo> dates = newDateUtil.getScope("2022", "8");
                    Staff randomStaff = staffRepository.findAll().iterator().next();
                    for (int week = 0; week < dates.size(); week++) {
                        LocalDate startDate = LocalDate.parse(dates.get(week).getStart());
                        LocalDate endDate = LocalDate.parse(dates.get(week).getEnd());
                        int totalDates = endDate.getDayOfMonth() - startDate.getDayOfMonth() + 1;
                        for (int dateIndex = 0; dateIndex < totalDates; dateIndex++) {
                            LocalDate currentDate
                                    = startDate.plusDays(dateIndex);
                            int currentWeek = week + 1;
                            for (Map.Entry<String, Integer> shift : shifts.entrySet()) {
                                for (int count = 0; count < shift.getValue(); count++) {
                                    shiftRepository.save(new Shift(shift.getKey(), currentDate, currentWeek, randomStaff));
                                }
                            }
                        }
                    }
                }
            }
        };
    }
}
