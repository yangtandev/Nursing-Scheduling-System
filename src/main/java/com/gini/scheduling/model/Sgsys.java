package com.gini.scheduling.model;


import javax.persistence.*;

/*
系統設定表 (排班設定)

* 55(8-16)
* 刀房開放間數 r55RoomOpen
* 每間所需人力 r55NeedManpower
* 每天休假人數 r55HolidayDay
* 假日班 A 組人數 r55HolidayA
* 假日班 B 組人數 r55HolidayB
* 假日班 C 組人數 r55HolidayC
* end 55(8-16)

* D6(16-00)
* 每天所需人數 A 組 rd6ManpowerA
* 每天所需人數 B 組 rd6manpowerB
* 每天所需人數 C 組 rd6ManpowerC
* 每天休假人數 rd6HolidayDay
* end D6(16-00)

* A0(00-08)
* 每天所需人數 A 組 ra0ManpowerA
* 每天所需人數 B 組 ra0ManpowerB
* 每天所需人數 C 組 ra0ManpowerC
* 每天休假人數 ra0HolidayDay
* end A0(00-08)

* 通用規則
* 班與班之間最小時數 generalBetweenHour
* end 通用規則

*/
@Entity

@IdClass(SgsysId.class)
public class Sgsys {
    // table 對應key值
    @Id
    @Column(columnDefinition = "CHAR(20) NOT NULL WITH DEFAULT")
    private String skey;
    
    // table key值對應內容
    @Column(columnDefinition = "CHAR(010) NOT NULL WITH DEFAULT")
    private String val;

    // 醫院代碼
    @Id
    @Column(columnDefinition = "CHAR(003) NOT NULL CHECK (HID NOT IN ('   '))")
    private String hid = "2A0";

    public Sgsys() {
    }

    public Sgsys(
            String skey,
            String val
    ) {
        this.skey = skey.trim();
        this.val = val.trim();
    }

    @Override
    public String toString() {
        return skey.trim();
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public String getSkey() {
        return skey.trim();
    }
    public String getVal() {
        return val.trim();
    }

    public void setVal(String val) {
        this.val = val.trim();
    }

    public String getHid() {
        return hid.trim();
    }

    public void setHid(String hid) {
        this.hid = hid.trim();
    }
}
