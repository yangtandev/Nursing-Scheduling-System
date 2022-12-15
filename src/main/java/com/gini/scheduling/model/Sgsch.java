package com.gini.scheduling.model;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.time.LocalDate;

// 人員排班表
@Entity

@IdClass(SgschId.class)
public class Sgsch {
    // 使用者卡號
    @Id
    @Column(columnDefinition = "CHAR(005) NOT NULL WITH DEFAULT")
    private String uno;
    
    // 排班日期 yyyy/MM/dd
    @Id
    @Column(columnDefinition = "DATE NOT NULL WITH DEFAULT '0001-01-01'")
    private LocalDate schdate;

    // 班別編號 55, D6, A0, A8, OFF, 公休
    @Column(columnDefinition = "CHAR(006) NOT NULL WITH DEFAULT")
    private String clsno;

    // 工作時數 0 or 8

    @Column(columnDefinition = "SMALLINT NOT NULL WITH DEFAULT 8")
    private int clspr;

    // 加班順序 0 (無加班) or 1~24

    @Column(columnDefinition = "SMALLINT NOT NULL WITH DEFAULT 0")
    private int overtime;

    // 備註 YY or ZZ or 00
    @Column(columnDefinition = "CHAR(002) NOT NULL WITH DEFAULT")
    private String remark;

    // 醫院代碼
    @Id
    @Column(columnDefinition = "CHAR(003) NOT NULL CHECK (HID NOT IN ('   '))")
    private String hid = "2A0";

    public Sgsch() {
    }

    public Sgsch(
            String uno,
            LocalDate schdate,
            String clsno,
            int clspr,
            int overtime,
            String remark
    ) {
        this.uno = uno;
        this.schdate = schdate;
        this.clsno = clsno.trim();
        this.clspr = clspr;
        this.overtime = overtime;
        this.remark = remark;
    }

    public Sgsch(
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
        return uno.trim();
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
