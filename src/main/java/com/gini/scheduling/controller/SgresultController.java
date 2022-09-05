package com.gini.scheduling.controller;

import com.gini.scheduling.dao.SgruserRepository;
import com.gini.scheduling.dao.SgresultRepository;
import com.gini.scheduling.exception.EntityNotFoundException;
import com.gini.scheduling.model.Sgresult;
import com.gini.scheduling.model.Sgshift;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class SgresultController {

    @Autowired
    private SgresultRepository sgresultRepository;

    @Autowired
    private SgruserRepository sgruserRepository;
    public static final Logger logger = LoggerFactory.getLogger(SgrroomController.class);
    @GetMapping("/sgresult")
    public List<Sgresult> getSgresult(
            @RequestParam String uno,
            @RequestParam LocalDate startSchdate,
            @RequestParam LocalDate endSchdate
    )throws EntityNotFoundException {
        return sgresultRepository.findAllByUnoAndDate(uno, startSchdate, endSchdate);
    }

    @GetMapping("/sgresults")
    public List<Sgresult> getSgresultes(
            @RequestParam LocalDate startSchdate,
            @RequestParam LocalDate endSchdate
    )throws EntityNotFoundException {
        return sgresultRepository.findAllByDate(startSchdate, endSchdate);
    }

    @PutMapping("/sgresult")
    public String putSgresult(
            @RequestBody Map<String, String> sgresultMap
    ) {
        String uno = "";
        String schdate = "";
        String clsno = "";
        for (Map.Entry<String, String> entry : sgresultMap.entrySet()) {
            switch (entry.getKey()) {
                case "uno":
                    uno = entry.getValue();
                    break;
                case "schdate":
                    schdate = entry.getValue();
                    break;
                case "clsno":
                    clsno = entry.getValue();
                    break;
            }
        }
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(schdate, dateTimeFormatter);
        Sgresult sgresult = sgresultRepository.findAllByUnoAndDate(uno, date, date).get(0);
        sgresult.setSchdate(date);
        sgresult.setSgshift(new Sgshift(clsno));
        sgresultRepository.save(sgresult);
        return "Success";
    }
}
