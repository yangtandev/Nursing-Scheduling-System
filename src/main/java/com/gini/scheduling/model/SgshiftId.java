package com.gini.scheduling.model;

import java.io.Serializable;
import java.util.Objects;


public class SgshiftId implements Serializable {
    private String clsno;
    private String hid;

    public SgshiftId() {
    }

    public SgshiftId(String clsno,  String hid) {
        this.clsno = clsno;
        this.hid = hid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SgshiftId sgresultId = (SgshiftId) o;
        return clsno.equals(sgresultId.clsno) &&
                hid.equals(sgresultId.hid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clsno, hid);
    }
}
