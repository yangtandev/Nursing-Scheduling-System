package com.gini.scheduling.service;

import com.gini.scheduling.controller.SgrroomController;
import com.gini.scheduling.dao.*;
import com.gini.scheduling.model.Scheduling;
import com.gini.scheduling.model.Sgbackup;
import com.gini.scheduling.model.Sgresult;
import com.gini.scheduling.model.Sgruser;
import com.gini.scheduling.utils.UUIDGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class SchedulingService {
    public static final String PROBLEM_ID = UUIDGenerator.generateUUID22();
    @Autowired
    private SgresultRepository sgresultRepository;
    @Autowired
    private SgshiftRepository sgshiftRepository;
    @Autowired
    private SgruserRepository sgruserRepository;
    @Autowired
    private SgrroomRepository sgrroomRepository;
    @Autowired
    private SgsysRepository sgsysRepository;
    @Autowired
    private SgbackupRepository sgbackupRepository;

    private LocalDate startSchdate;
    private LocalDate endSchdate;
    private List<Sgruser> sgruserList;
    private List<Sgbackup> sgbackupList;
    private List<Sgresult> sgresultList;
    
    public static final Logger logger = LoggerFactory.getLogger(SchedulingService.class);
    
    public Scheduling findById(String id) {
        if (!PROBLEM_ID.equals(id)) {
            throw new IllegalStateException("There is no timeTable with id (" + id + ").");
        }
        setSgruserList();
        setSgbackupList();
        setSgresultList();
        return new Scheduling(
        		sgruserList, 
        		sgbackupList,
        		sgresultList);
    }

    public void save(Scheduling scheduling) {
        for (Sgresult sgresult : scheduling.getSgresultList()) {
            // TODO this is awfully naive: optimistic locking causes issues if called by the SolverManager
            sgresultRepository.save(sgresult);
        }
    }

	public void setSchdate(LocalDate startSchdate, LocalDate endSchdate) {
		this.startSchdate = startSchdate;
		this.endSchdate = endSchdate;
	}

	public List<Sgruser> getSgruserList() {
		return sgruserList;
	}

	public void setSgruserList() {
		this.sgruserList = sgruserRepository.findAll();
	}

	public List<Sgbackup> getSgbackupList() {
		return sgbackupList;
	}

	public void setSgbackupList() {
		this.sgbackupList = sgbackupRepository.findAll();
	}
	
	public List<Sgresult> getSgresultList() {
		return sgresultList;
	}

	public void setSgresultList() {
		this.sgresultList = sgresultRepository.findAllByDate(this.startSchdate,this.endSchdate);
	}
}
