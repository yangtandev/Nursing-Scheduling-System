package com.gini.scheduling.controller;

import com.gini.scheduling.dao.SgrroomRepository;
import com.gini.scheduling.dao.SgresultRepository;
import com.gini.scheduling.model.*;
import com.gini.scheduling.exception.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sgrroom")
public class SgrroomController {
    @Autowired
    private SgrroomRepository sgrroomRepository;
    @Autowired
    private SgresultRepository sgresultRepository;

    public static final Logger logger = LoggerFactory.getLogger(SgrroomController.class);
    @GetMapping
    public List<Object> getSgrroom(
            @RequestParam LocalDate schdate
    )throws EntityNotFoundException {
        List<Sgrroom> sgrrooms = sgrroomRepository.findAllByDate(schdate);
        List<Sgresult> sgresultList = sgresultRepository.findAllByDate(schdate, schdate);
        List<Object> list=new ArrayList<>();
        for (Sgrroom sgrroom : sgrrooms) {
            Map<String, String> map = new HashMap<>();
            map.put("uno", sgrroom.getUno());
            map.put("schdate", String.valueOf(sgrroom.getSchdate()));
            map.put("rmname", sgrroom.getRmname());
            list.add(map);
        }
        return list;
    }

    @PostMapping
    public String postSgrroom(@RequestBody Map<String, String> sgrroomMap
    ) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate schdate = null;
        for (Map.Entry<String, String> entry : sgrroomMap.entrySet()) {
            if(entry.getKey().equals("schdate")){
                schdate = LocalDate.parse(entry.getValue(), dateTimeFormatter);
            }else{
                String rmname = entry.getKey();
                String joinedMinusBrackets = entry.getValue().substring( 1, entry.getValue().length() - 1);
                String[] unos = joinedMinusBrackets.split( ", ");
                for ( String uno : unos ) {
                    sgrroomRepository.save(new Sgrroom(uno, schdate, rmname));
                }
            }
        }
        return "Success";
    }

    @PutMapping
    public String putSgrroom(
            @RequestParam LocalDate schdate,
            @RequestBody Map<String, String> sgrroomMap
    ) {
        List<Sgrroom> sgrrooms = sgrroomRepository.findAllByDate(schdate);
        for (Sgrroom sgrroom : sgrrooms) {
            for (Map.Entry<String, String> entry : sgrroomMap.entrySet()) {
                String rmname = entry.getKey();
                String joinedMinusBrackets = entry.getValue().substring( 1, entry.getValue().length() - 1);
                String[] unos = joinedMinusBrackets.split( ", ");
                for ( String uno : unos ) {
                    if(sgrroom.getUno().equals(uno)){
                        sgrroom.setRmname(rmname);
                        sgrroomRepository.save(sgrroom);
                    }
                }
            }
        }
        return "Success";
    }
}
