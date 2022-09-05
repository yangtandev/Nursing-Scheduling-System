package com.gini.scheduling.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;


public class SgresultId implements Serializable {
    private String uno;
    private LocalDate schdate;
    private String hid;

    public SgresultId() {
    }

    public SgresultId(String uno, LocalDate schdate, String hid) {
        this.uno = uno;
        this.schdate = schdate;
        this.hid = hid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SgresultId sgresultId = (SgresultId) o;
        return uno.equals(sgresultId.uno) &&
                schdate.equals(sgresultId.schdate) &&
                hid.equals(sgresultId.hid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uno, schdate, hid);
    }
}
