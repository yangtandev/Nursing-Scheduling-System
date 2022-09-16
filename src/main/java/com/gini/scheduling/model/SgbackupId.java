package com.gini.scheduling.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;


public class SgbackupId implements Serializable {
    private String schuuid;
    private String hid;

    public SgbackupId() {
    }

    public SgbackupId(String schuuid, String hid) {
        this.schuuid = schuuid;
        this.hid = hid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SgbackupId sgbackupId = (SgbackupId) o;
        return schuuid.equals(sgbackupId.schuuid) &&
                hid.equals(sgbackupId.hid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schuuid, hid);
    }
}
