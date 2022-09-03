package com.gini.scheduling.model;


import javax.persistence.*;
import java.time.LocalDate;

// 人員排班備份表
@Entity

@IdClass(SgschId.class)
public class Sgbackup {
    // 使用者卡號
    @Id
    @Column(columnDefinition = "CHAR(10) NOT NULL WITH DEFAULT")
    private String uno;

    // 排班日期 yyyy/MM/dd
    @Id
    @Column(columnDefinition = "DATE NOT NULL WITH DEFAULT '0001-01-01'")
    private LocalDate schdate;

    // 班別編號 55, D6, A0, A8, OFF, 公休
    @Column(columnDefinition = "CHAR(10) NOT NULL WITH DEFAULT")
    private String clsno;

    // 醫院代碼
    @Id
    @Column(columnDefinition = "CHAR(003) NOT NULL CHECK (HID NOT IN ('   '))")
    private String hid = "2A0";

    // 更新時間
//    @Generated(GenerationTime.ALWAYS)
//    @Column(columnDefinition = "GENERATED ALWAYS FOR EACH ROW ON UPDATE AS ROW CHANGE TIMESTAMP NOT NULL", insertable = false, updatable = false)
//    private Timestamp zsch;

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

//    public Timestamp getZsch() {
//        return zsch;
//    }
//
//    public void setZsch(Timestamp zsch) {
//        this.zsch = zsch;
//    }
}
