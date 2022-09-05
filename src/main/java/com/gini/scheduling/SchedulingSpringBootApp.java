package com.gini.scheduling;

import com.gini.scheduling.controller.SgrroomController;
import com.gini.scheduling.dao.SgruserRepository;
import com.gini.scheduling.dao.SgshiftRepository;
import com.gini.scheduling.model.Sgruser;
import com.gini.scheduling.model.Sgshift;
import com.gini.scheduling.model.Sgsys;
import com.gini.scheduling.dao.SgsysRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

import java.util.*;

@SpringBootApplication
public class SchedulingSpringBootApp extends SpringBootServletInitializer {
    public static final Logger logger = LoggerFactory.getLogger(SgrroomController.class);

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(SchedulingSpringBootApp.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(SchedulingSpringBootApp.class, args);
    }

    @Bean
    public CommandLineRunner initData(SgruserRepository sgruserRepository, SgshiftRepository sgshiftRepository, SgsysRepository sgsysRepository) {
        return (args) -> {
            // 自動分組
//            if (sgruserRepository.findCount() > 0) {
//                List<Sgruser> sgruserList = sgruserRepository.findAll();
//                List<Sgruser> newSgruserList = new ArrayList<>();
//                for (int i = 0; i<sgruserList.size(); i++) {
//                    if(i>=0 && i<30){
//                        sgruserList.get(i).setUteam("A");
//                    }else if (i>=30 && i<60){
//                        sgruserList.get(i).setUteam("B");
//                    }else{
//                        sgruserList.get(i).setUteam("C");
//                    }
//                    newSgruserList.add(sgruserList.get(i));
//                }
//                sgruserRepository.saveAll(newSgruserList);
//            }
            // 自動帶入系統設定
            if (sgsysRepository.findCount() == 0) {
                Map<String, String> sgsys = new HashMap<>();
                // 55(8-16)
                sgsys.put("r55RoomOpen", "12");
                sgsys.put("r55NeedManpower", "2");
                sgsys.put("r55HolidayDay", "2");
                sgsys.put("r55HolidayA", "1");
                sgsys.put("r55HolidayB", "1");
                sgsys.put("r55HolidayC", "1");
                // D6(16-00)
                sgsys.put("rd6ManpowerA", "1");
                sgsys.put("rd6manpowerB", "1");
                sgsys.put("rd6ManpowerC", "1");
                sgsys.put("rd6Manpower", "2");
                sgsys.put("rd6HolidayDay", "1");
                // A0(00-08)
                sgsys.put("ra0ManpowerA", "1");
                sgsys.put("ra0ManpowerB", "1");
                sgsys.put("ra0ManpowerC", "1");
                sgsys.put("ra0Manpower", "2");
                sgsys.put("ra0HolidayDay", "1");
                // A8(12-20)
                sgsys.put("ra8Manpower", "2");
                // 常日
                sgsys.put("rdailyManpower", "4");
                // 通用規則
                sgsys.put("generalBetweenHour", "11");

                for (Map.Entry<String, String> entry : sgsys.entrySet()) {
                    sgsysRepository.save(new Sgsys(entry.getKey(), entry.getValue()));
                }

                // 自動帶入班別
                String[] clsnoArray = new String[] {
                        "55", "D6", "A0", "A8", "常日" };
                for (String clsno : clsnoArray) {
                    sgshiftRepository.save(new Sgshift(clsno));
                }
            }
        };
    }
}
