package com.gini.scheduling.model;

import java.io.Serializable;
import java.util.Objects;


public class SgresultId implements Serializable {
    private String schuuid;
    private String hid;

    public SgresultId() {
    }

    public SgresultId(String schuuid, String hid) {
        this.schuuid = schuuid;
        this.hid = hid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SgresultId sgresultId = (SgresultId) o;
        return schuuid.equals(sgresultId.schuuid) &&
                hid.equals(sgresultId.hid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schuuid, hid);
    }
}
