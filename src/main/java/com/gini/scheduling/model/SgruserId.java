package com.gini.scheduling.model;

import java.io.Serializable;
import java.util.Objects;


public class SgruserId implements Serializable {
    private String uno;
    private String hid;

    public SgruserId() {
    }

    public SgruserId(String uno, String hid) {
        this.uno = uno;
        this.hid = hid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SgruserId sgruserId = (SgruserId) o;
        return uno.equals(sgruserId.uno) &&
                hid.equals(sgruserId.hid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uno, hid);
    }
}
