

package com.gini.scheduling.model;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

// 人員排班班別表
@Entity
@IdClass(SgshiftId.class)
public class Sgshift {
    // 班別編號 55, D6, A0, A8, OFF, 公休
    @Id
    @Column(columnDefinition = "CHAR(006) NOT NULL WITH DEFAULT")
    private String clsno;
	
    // 醫院代碼
    @Id
    @Column(columnDefinition = "CHAR(003) NOT NULL CHECK (HID NOT IN ('   '))")
    private String hid = "2A0";

    public Sgshift() {
    }

    public Sgshift(
            String clsno
    ) {
        this.clsno = clsno.trim();
    }

    @Override
    public String toString() {
        return clsno.trim();
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public String getClsno() {
        return clsno.trim();
    }

    public void setClsno(String clsno) {
        this.clsno = clsno.trim();
    }
}
