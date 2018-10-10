package com.shou.demo.jiuray.entity;

/**
 * @author jrhuf
 */
public class EPC {
    private int len;
    private String epc;
    private int count;
    private int rssi;
    private String pc;
    private String note;

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public String getPc() {
        return pc;
    }

    public void setPc(String pc) {
        this.pc = pc;
    }

    /**
     * @return the len
     */
    public int getLen() {
        return len;
    }

    /**
     * @param len the id to set
     */
    public void setLen(int len) {
        this.len = len;
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

    /**
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public void setCount(int count) {
        this.count = count;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "EPC [id=" + len + ", epc=" + epc + ", count=" + count + "]";
    }


}
