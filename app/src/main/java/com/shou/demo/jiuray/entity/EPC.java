package com.shou.demo.jiuray.entity;

import java.util.Objects;

/**
 * @author jrhuf
 */
public class EPC {
    private String epc;
    private String note;

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    /**
     * @return the epc
     */
    public String getEpc() {
        return epc;
    }

    /**
     * @param epc the epc to set
     */
    public void setEpc(String epc) {
        this.epc = epc;
    }

    @Override
    public String toString() {
        return "EPC [epc=" + epc + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EPC epc1 = (EPC) o;
        return Objects.equals(epc, epc1.epc) &&
                Objects.equals(note, epc1.note);
    }

    @Override
    public int hashCode() {
        return Objects.hash(epc, note);
    }
}
