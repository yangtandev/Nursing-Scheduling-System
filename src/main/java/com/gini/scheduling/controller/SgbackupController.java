package com.gini.scheduling.controller;

import com.gini.scheduling.dao.SgruserRepository;
import com.gini.scheduling.dao.SgbackupRepository;
import com.gini.scheduling.exception.EntityNotFoundException;
import com.gini.scheduling.model.Sgbackup;
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
public class SgbackupController {

    @Autowired
    private SgbackupRepository sgbackupRepository;

    @Autowired
    private SgruserRepository sgruserRepository;
    public static final Logger logger = LoggerFactory.getLogger(SgbackupController.class);
    @GetMapping("/sgbackup")
    public List<Sgbackup> getSgbackup(
            @RequestParam String uno,
            @RequestParam LocalDate startSchdate,
            @RequestParam LocalDate endSchdate
    )throws EntityNotFoundException {
        return sgbackupRepository.findAllByUnoAndDate(uno, startSchdate, endSchdate);
    }

    @GetMapping("/sgbackupes")
    public List<Sgbackup> getSgbackupes(
            @RequestParam LocalDate startSchdate,
            @RequestParam LocalDate endSchdate
    )throws EntityNotFoundException {
        return sgbackupRepository.findAllByDate(startSchdate, endSchdate);
    }

    @PutMapping("/sgbackup")
    public String putSgbackup(
            @RequestBody Map<String, String> sgbackupMap
    ) {
        String uno = "";
        String schdate = "";
        String clsno = "";
        for (Map.Entry<String, String> entry : sgbackupMap.entrySet()) {
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
        Sgbackup sgbackup = sgbackupRepository.findAllByUnoAndDate(uno, date, date).get(0);
        sgbackup.setSchdate(date);
        sgbackup.setClsno(clsno);
        sgbackupRepository.save(sgbackup);
        return "Success";
    }
}
