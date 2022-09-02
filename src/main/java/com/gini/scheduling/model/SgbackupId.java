package com.gini.scheduling.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;


public class SgbackupId implements Serializable {
    private String uno;
    private LocalDate schdate;
    private String hid;

    public SgbackupId() {
    }

    public SgbackupId(String uno, LocalDate schdate, String hid) {
        this.uno = uno;
        this.schdate = schdate;
        this.hid = hid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SgbackupId sgbackupId = (SgbackupId) o;
        return uno.equals(sgbackupId.uno) &&
                schdate.equals(sgbackupId.schdate) &&
                hid.equals(sgbackupId.hid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uno, schdate, hid);
    }
}
