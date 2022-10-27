package com.gini.scheduling.controller;


import com.gini.scheduling.dao.SgsysRepository;
import com.gini.scheduling.model.*;

import com.gini.scheduling.exception.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sgsys")
public class SgsysController {
    @Autowired
    private SgsysRepository sgsysRepository;
    public static final Logger logger = LoggerFactory.getLogger(SgsysController.class);
    @GetMapping
    public List<Sgsys> getSgsys(
    )throws EntityNotFoundException {
        return sgsysRepository.findAll();
    }

    @PutMapping
    public String putSgsys(
            @RequestBody Map<String, String> sgsysMap
    ) {
        List<Sgsys> sgsyss = sgsysRepository.findAll();
        for (Sgsys sgsys : sgsyss) {
            for (Map.Entry<String, String> entry : sgsysMap.entrySet()) {
                String skey = entry.getKey();
                if (sgsys.getSkey().equals(skey)) {
                    sgsys.setVal(entry.getValue());
                    sgsysRepository.save(sgsys);
                }
            }
        }
        return "Success";
    }
}
