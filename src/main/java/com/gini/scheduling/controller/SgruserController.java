package com.gini.scheduling.controller;

import com.gini.scheduling.dao.SgruserRepository;

import com.gini.scheduling.model.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SgruserController {
    @Autowired
    private SgruserRepository sgruserRepository;

    @GetMapping("/sgruser")
    public List<Sgruser> getSgruser(
    ) {
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
                case "udsb":
                    sgruser.setUdsb(Boolean.parseBoolean(entry.getValue()));
                    break;
            }
        }
        sgruserRepository.save(sgruser);
        return "Success";
    }

    @PostMapping("/sgrusers")
    public String postSgrusers(@RequestBody Map<String, String> sgrusersMap
    ) {
        for (Map.Entry<String, String> entry : sgrusersMap.entrySet()) {
            String uno = entry.getKey();
            String uname = entry.getValue();
            sgruserRepository.save(new Sgruser(uno, uname));
        }
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
                        case "udsb":
                            sgruser.setUdsb(Boolean.parseBoolean(entry.getValue()));
                            break;
                    }
                }
                sgruserRepository.save(sgruser);
            }
        }
        return "Success";
    }
}
