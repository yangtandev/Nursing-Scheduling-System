package com.gini.scheduling.model;

import java.io.Serializable;
import java.util.Objects;


public class SgschId implements Serializable {
    private String schuuid;
    private String hid;

    public SgschId() {
    }

    public SgschId(String schuuid, String hid) {
        this.schuuid = schuuid;
        this.hid = hid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SgschId sgschId = (SgschId) o;
        return schuuid.equals(sgschId.schuuid) &&
                hid.equals(sgschId.hid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schuuid, hid);
    }
}
