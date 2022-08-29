package com.gini.scheduling.controller;


import com.gini.scheduling.dao.SgsysRepository;
import com.gini.scheduling.model.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sgsys")
public class SgsysController {
    @Autowired
    private SgsysRepository sgsysRepository;

    @GetMapping
    public List<Sgsys> getSgsys(
    ) {
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
