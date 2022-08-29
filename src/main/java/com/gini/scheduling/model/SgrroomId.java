package com.gini.scheduling.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;


public class SgrroomId implements Serializable {
    private String uno;
    private LocalDate schdate;
    private String hid;

    public SgrroomId() {
    }

    public SgrroomId(String uno, LocalDate schdate, String hid) {
        this.uno = uno;
        this.schdate = schdate;
        this.hid = hid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SgrroomId sgrroomId = (SgrroomId) o;
        return uno.equals(sgrroomId.uno) &&
                schdate.equals(sgrroomId.schdate) &&
                hid.equals(sgrroomId.hid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uno, schdate, hid);
    }
}
