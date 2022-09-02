package com.gini.scheduling.model;

import com.gini.scheduling.utils.UUIDGenerator;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import javax.persistence.*;
import java.time.LocalDate;

// 人員排班結果表
@Entity
@PlanningEntity
@Table(schema = "SG")
@IdClass(SgresultId.class)
public class Sgresult {
    // 班別序號 UUID
    @PlanningId
    @Id
    @Column(columnDefinition = "CHAR(100) NOT NULL WITH DEFAULT")
    private String schuuid = UUIDGenerator.generateUUID22();


    // 排班日期 yyyy/MM/dd
    @Column(columnDefinition = "DATE NOT NULL WITH DEFAULT '0001-01-01'")
    private LocalDate schdate;

    // 排班日期所在週
    @Column(columnDefinition = "INTEGER NOT NULL WITH DEFAULT")
    private int schweek;

    // 班別編號 55, D6, A0, A8, OFF, 公休
    @Column(columnDefinition = "CHAR(10) NOT NULL WITH DEFAULT")
    private String clsno;

    // 工作時數 0 or 8
    @Generated(GenerationTime.INSERT)
    @Column(columnDefinition = "INTEGER NOT NULL WITH DEFAULT 8")
    private int clspr;

    // 加班順序 0 (無加班) or 1~24
    @Generated(GenerationTime.INSERT)
    @Column(columnDefinition = "INTEGER NOT NULL WITH DEFAULT 0")
    private int overtime;

    // 出勤人員卡號、出勤人員之醫院代碼
    @PlanningVariable(valueRangeProviderRefs = "sgruserRange")
    @OneToOne(cascade = CascadeType.ALL,orphanRemoval = true)
    @JoinColumns(value = {
            @JoinColumn(name = "uno", referencedColumnName = "uno"),
            @JoinColumn(name = "userhid", referencedColumnName = "hid", columnDefinition = "CHAR(003) NOT NULL CHECK (USERHID NOT IN ('   '))")
    }, foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    private Sgruser sgruser;

    // 醫院代碼
    @Id
    @Column(columnDefinition = "CHAR(003) NOT NULL CHECK (HID NOT IN ('   '))")
    private String hid = "2A0";

    // 更新時間
//    @Generated(GenerationTime.ALWAYS)
//    @Column(columnDefinition = "GENERATED ALWAYS FOR EACH ROW ON UPDATE AS ROW CHANGE TIMESTAMP NOT NULL", insertable = false, updatable = false)
//    private Timestamp zresult;

    public Sgresult() {
    }

    public Sgresult(
            Sgruser sgruser,
            LocalDate schdate,
            int schweek,
            String clsno
    ) {
        this.sgruser = sgruser;
        this.schdate = schdate;
        this.schweek = schweek;
        this.clsno = clsno.trim();
    }

    @Override
    public String toString() {
        return schuuid;
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public LocalDate getSchdate() {
        return schdate;
    }

    public void setSchdate(LocalDate schdate) {
        this.schdate = schdate;
    }

    public int getSchweek() {
        return schweek;
    }

    public void setSchweek(int schweek) {
        this.schweek = schweek;
    }

    public String getClsno() {
        return clsno.trim();
    }

    public void setClsno(String clsno) {
        this.clsno = clsno.trim();
    }

    public int getClspr() {
        return clspr;
    }

    public void setClspr(int clspr) {
        this.clspr = clspr;
    }

    public int getOvertime() {
        return overtime;
    }

    public void setOvertime(int overtime) {
        this.overtime = overtime;
    }

    public String getUno() {
        return sgruser.toString();
    }

    public String getUname() {
        return sgruser.getUname();
    }

    public void setSgruser(Sgruser sgruser) {
        this.sgruser = sgruser;
    }

//    public Timestamp getZresult() {
//        return zresult;
//    }
//
//    public void setZresult(Timestamp zresult) {
//        this.zresult = zresult;
//    }
}
