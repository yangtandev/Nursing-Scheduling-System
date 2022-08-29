package com.gini.scheduling.model;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.optaplanner.core.api.domain.lookup.PlanningId;

import javax.persistence.*;
import java.sql.Timestamp;

// 人員主表
@Entity
@Table(schema = "SG")
@IdClass(SgruserId.class)
public class Sgruser {
    // 使用者卡號
    @PlanningId
    @Id
    @Column(columnDefinition = "CHAR(10) NOT NULL WITH DEFAULT")
    private String uno;

    // 使用者姓名
    @Column(columnDefinition = "CHAR(10) NOT NULL WITH DEFAULT")
    private String uname;

    // 使用者組別
    @Column(columnDefinition = "CHAR(10) NOT NULL WITH DEFAULT")
    private String uteam;

    // 使用者角色
    @Column(columnDefinition = "CHAR(10) NOT NULL WITH DEFAULT")
    private String urole;

    // 手術室號 R1~R12
    @Column(columnDefinition = "CHAR(10) NOT NULL WITH DEFAULT")
    private String uopno;

    // 是否大夜 0 or 1
    @Column(columnDefinition = "BOOLEAN NOT NULL WITH DEFAULT 0")
    private boolean uisbn;

    // 是否小夜 0 or 1
    @Column(columnDefinition = "BOOLEAN NOT NULL WITH DEFAULT 0")
    private boolean uissn;

    // 是否失效 0 or 1
    @Column(columnDefinition = "BOOLEAN NOT NULL WITH DEFAULT 0")
    private boolean udsb;

    // 醫院代碼
    @Id
    @Column(columnDefinition = "CHAR(003) NOT NULL CHECK (HID NOT IN ('   '))")
    private String hid = "2A0";

    // 更新時間
    @Generated(GenerationTime.ALWAYS)
    @Column(columnDefinition = "GENERATED ALWAYS FOR EACH ROW ON UPDATE AS ROW CHANGE TIMESTAMP NOT NULL",insertable=false,updatable=false)
    private Timestamp zruser;

    public Sgruser() {
    }

    public Sgruser(String uno) {
        this.uno = uno.trim();
    }

    public Sgruser(String uno, String uname) {
        this.uno = uno.trim();
        this.uname = uname.trim();
        this.uteam = "";
        this.urole = "使用者";
        this.uopno = "R1";
        this.uisbn = false;
        this.uissn = false;
        this.udsb = false;
    }

    public Sgruser(String uno, String uname, String uteam) {
        this.uno = uno.trim();
        this.uname = uname.trim();
        this.uteam = uteam.trim();
        this.urole = "使用者";
        this.uopno = "R1";
        this.uisbn = false;
        this.uissn = false;
        this.udsb = false;
    }

    public Sgruser(String uno, String uname, String uteam, String urole, String uopno, Boolean uisbn, Boolean uissn, Boolean udsb) {
        this.uno = uno.trim();
        this.uname = uname.trim();
        this.uteam = uteam.trim();
        this.urole = urole.trim();
        this.uopno = uopno.trim();
        this.uisbn = uisbn;
        this.uissn = uissn;
        this.udsb = udsb;
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

    public void setUno(String uno) {
        this.uno = uno.trim();
    }

    public String getUname() {
        return uname.trim();
    }

    public void setUname(String uname) {
        this.uname = uname.trim();
    }

    public String getUteam() {
        return uteam.trim();
    }

    public void setUteam(String uteam) {
        this.uteam = uteam.trim();
    }

    public String getUrole() {
        return urole.trim();
    }

    public void setUrole(String urole) {
        this.urole = urole.trim();
    }

    public String getUopno() {
        return uopno.trim();
    }

    public void setUopno(String uopno) {
        this.uopno = uopno.trim();
    }

    public boolean isUisbn() {
        return uisbn;
    }

    public void setUisbn(boolean uisbn) {
        this.uisbn = uisbn;
    }

    public boolean isUissn() {
        return uissn;
    }

    public void setUissn(boolean uissn) {
        this.uissn = uissn;
    }

    public boolean isUdsb() {
        return udsb;
    }

    public void setUdsb(boolean udsb) {
        this.udsb = udsb;
    }

    public String getHid() {
        return hid.trim();
    }

    public void setHid(String hid) {
        this.hid = hid.trim();
    }

    public Timestamp getZruser() {
        return zruser;
    }

    public void setZruser(Timestamp zruser) {
        this.zruser = zruser;
    }
}
