package com.gini.scheduling.controller;

import com.gini.scheduling.dao.*;
import com.gini.scheduling.dao.SgschRepository;
import com.gini.scheduling.model.*;
import com.gini.scheduling.utils.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SgschController {

    @Autowired
    private SgschRepository sgschRepository;

    @Autowired
    private SgruserRepository sgruserRepository;

    @GetMapping("/sgsch")
    public List<Sgsch> getSgsch(
            @RequestParam String uno,
            @RequestParam LocalDate startSchdate,
            @RequestParam LocalDate endSchdate
    )throws EntityNotFoundException {
        return sgschRepository.findAllByUnoAndDate(uno, startSchdate, endSchdate);
    }

    @GetMapping("/sgsches")
    public List<Sgsch> getSgsches(
            @RequestParam LocalDate startSchdate,
            @RequestParam LocalDate endSchdate
    )throws EntityNotFoundException {
        return sgschRepository.findAllByDate(startSchdate, endSchdate);
    }

    @PutMapping("/sgsch")
    public String putSgsch(
            @RequestBody Map<String, String> sgschMap
    ) {
        String uno = "";
        String schdate = "";
        String clsno = "";
        for (Map.Entry<String, String> entry : sgschMap.entrySet()) {
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
        Sgsch sgsch = sgschRepository.findAllByUnoAndDate(uno, date, date).get(0);
        sgsch.setSchdate(date);
        sgsch.setClsno(clsno);
        sgschRepository.save(sgsch);
        return "Success";
    }
}
