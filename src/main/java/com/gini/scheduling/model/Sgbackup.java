package com.gini.scheduling.model;


import javax.persistence.*;

import org.optaplanner.core.api.domain.lookup.PlanningId;

import com.gini.scheduling.utils.UUIDGenerator;

import java.time.LocalDate;

// 人員排班備份表
@Entity
@IdClass(SgbackupId.class)
public class Sgbackup {
	@PlanningId
    @Id
    @Column(columnDefinition = "CHAR(22) NOT NULL WITH DEFAULT")
    private String schuuid = UUIDGenerator.generateUUID22();
	
    // 使用者卡號
    @Column(columnDefinition = "CHAR(005) NOT NULL WITH DEFAULT")
    private String uno;
	
    // 排班日期 yyyy/MM/dd
    @Column(columnDefinition = "DATE NOT NULL WITH DEFAULT '0001-01-01'")
    private LocalDate schdate;

    // 班別編號 55, D6, A0, A8, OFF, 公休
    @Column(columnDefinition = "CHAR(006) NOT NULL WITH DEFAULT")
    private String clsno;

    // 醫院代碼
    @Id
    @Column(columnDefinition = "CHAR(003) NOT NULL CHECK (HID NOT IN ('   '))")
    private String hid = "2A0";

    public Sgbackup() {
    }

    public Sgbackup(
            String uno,
            LocalDate schdate,
            String clsno
    ) {
        this.uno = uno;
        this.schdate = schdate;
        this.clsno = clsno.trim();
    }

    @Override
    public String toString() {
        return uno.trim();
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public String getSchuuid() {
        return schuuid;
    }
    
    public LocalDate getSchdate() {
        return schdate;
    }

    public void setSchdate(LocalDate schdate) {
        this.schdate = schdate;
    }

    public String getClsno() {
        return clsno.trim();
    }

    public void setClsno(String clsno) {
        this.clsno = clsno.trim();
    }

    public String getUno() {
        return uno.trim();
    }
}
