package com.gini.scheduling.model;


import javax.persistence.*;
import java.time.LocalDate;


// 人員手術室排班表
@Entity

@IdClass(SgrroomId.class)
public class Sgrroom {
    // 使用者卡號
    @Id
    @Column(columnDefinition = "CHAR(10) NOT NULL WITH DEFAULT")
    private String uno;

    // 排班日期 yyyy/MM/dd
    @Id
    @Column(columnDefinition = "DATE NOT NULL WITH DEFAULT '0001-01-01'")
    private LocalDate schdate;

    // 手術室號 R1~R12
    @Column(columnDefinition = "CHAR(10) NOT NULL WITH DEFAULT")
    private String rmname;

    // 醫院代碼
    @Id
    @Column(columnDefinition = "CHAR(003) NOT NULL CHECK (HID NOT IN ('   '))")
    private String hid = "2A0";

    // 更新時間
//    @Generated(GenerationTime.ALWAYS)
//    @Column(columnDefinition = "GENERATED ALWAYS FOR EACH ROW ON UPDATE AS ROW CHANGE TIMESTAMP NOT NULL", insertable = false, updatable = false)
//    private Timestamp zrroom;

    public Sgrroom() {
    }

    public Sgrroom(
            String uno,
            LocalDate schdate,
            String rmname
    ) {
        this.uno = uno.trim();
        this.schdate = schdate;
        this.rmname = rmname.trim();
    }

    @Override
    public String toString() {
        return uno.trim();
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public String getUno() {
        return uno.trim();
    }

    public LocalDate getSchdate() {
        return schdate;
    }

    public void setSchdate(LocalDate schdate) {
        this.schdate = schdate;
    }

    public String getRmname() {
        return rmname.trim();
    }

    public void setRmname(String rmname) {
        this.rmname = rmname.trim();
    }

    public String getHid() {
        return hid.trim();
    }

    public void setHid(String hid) {
        this.hid = hid.trim();
    }

//    public Timestamp getZrroom() {
//        return zrroom;
//    }
//
//    public void setZrroom(Timestamp zrroom) {
//        this.zrroom = zrroom;
//    }
}
