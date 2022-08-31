package com.gini.scheduling;

import com.gini.scheduling.controller.SgrroomController;
import com.gini.scheduling.model.Sgsys;
import com.gini.scheduling.dao.SgrroomRepository;
import com.gini.scheduling.dao.SgschRepository;
import com.gini.scheduling.dao.SgruserRepository;
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
    public CommandLineRunner initData(SgschRepository sgschRepository,
                                      SgruserRepository sgruserRepository, SgrroomRepository sgrroomRepository, SgsysRepository sgsysRepository) {
        return (args) -> {
            // 自動帶入系統設定
            if (sgsysRepository.findCount() == 0) {
                Map<String, String> sgsys = new HashMap<>();
                // 55(8-16)
                sgsys.put("r55RoomOpen", "12");
                sgsys.put("r55NeedManpower", "2");
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
