package com.gini.scheduling.controller;

import com.gini.scheduling.dao.SgruserRepository;

import com.gini.scheduling.model.*;

import com.gini.scheduling.exception.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class SgruserController {
    @Autowired
    private SgruserRepository sgruserRepository;
    public static final Logger logger = LoggerFactory.getLogger(SgrroomController.class);
    @GetMapping("/sgruser")
    public List<Sgruser> getSgruser(
    )throws EntityNotFoundException {
        return sgruserRepository.findAll();
    }

    @PostMapping("/sgruser")
    public String postSgruser(@RequestBody Map<String, String> sgruserMap
    ) {
        Sgruser sgruser = new Sgruser();
        for (Map.Entry<String, String> entry : sgruserMap.entrySet()) {
            switch (entry.getKey()) {
                case "uno":
                    sgruser.setUno(entry.getValue());
                    break;
                case "uname":
                    sgruser.setUname(entry.getValue());
                    break;
                case "uteam":
                    sgruser.setUteam(entry.getValue());
                    break;
                case "urole":
                    sgruser.setUrole(entry.getValue());
                    break;
                case "uopno":
                    sgruser.setUopno(entry.getValue());
                    break;
                case "uisbn":
                    sgruser.setUisbn(Boolean.parseBoolean(entry.getValue()));
                    break;
                case "uissn":
                    sgruser.setUissn(Boolean.parseBoolean(entry.getValue()));
                    break;
            }
        }
        sgruserRepository.save(sgruser);
        return "Success";
    }

    @PostMapping("/sgrusers")
    public String postSgrusers(@RequestBody Map<String, String> sgrusersMap
    ) {
    	List<Sgruser> sgruserList = new ArrayList<>();
        for (Map.Entry<String, String> entry : sgrusersMap.entrySet()) {
            String uno = entry.getKey();
            String uname = entry.getValue();
            sgruserList.add(new Sgruser(uno, uname));
        }
        sgruserRepository.saveAll(sgruserList);
        return "Success";
    }

    @PutMapping("/sgruser")
    public String putSgruser(
            @RequestParam String uno,
            @RequestBody Map<String, String> sgruserMap
    ) {
        List<Sgruser> sgrusers = sgruserRepository.findAll();
        for (Sgruser sgruser : sgrusers) {
            if (sgruser.getUno().equals(uno)) {
                for (Map.Entry<String, String> entry : sgruserMap.entrySet()) {
                    switch (entry.getKey()) {
                        case "uname":
                            sgruser.setUname(entry.getValue());
                            break;
                        case "uteam":
                            sgruser.setUteam(entry.getValue());
                            break;
                        case "urole":
                            sgruser.setUrole(entry.getValue());
                            break;
                        case "uopno":
                            sgruser.setUopno(entry.getValue());
                            break;
                        case "uisbn":
                            sgruser.setUisbn(Boolean.parseBoolean(entry.getValue()));
                            break;
                        case "uissn":
                            sgruser.setUissn(Boolean.parseBoolean(entry.getValue()));
                            break;
                    }
                }
                sgruserRepository.save(sgruser);
            }
        }
        return "Success";
    }
}
