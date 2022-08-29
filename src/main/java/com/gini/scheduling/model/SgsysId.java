package com.gini.scheduling.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;


public class SgsysId implements Serializable {
    private String skey;
    private String hid;

    public SgsysId() {
    }

    public SgsysId(String skey, Date schdate, String hid) {
        this.skey = skey;
        this.hid = hid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SgsysId sgsysId = (SgsysId) o;
        return skey.equals(sgsysId.skey) &&
                hid.equals(sgsysId.hid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skey, hid);
    }
}
