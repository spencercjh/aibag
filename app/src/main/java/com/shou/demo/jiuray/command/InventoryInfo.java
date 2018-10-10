package com.shou.demo.jiuray.command;

/**
 * @author jruhf
 */
public class InventoryInfo {
	private byte[] epc ;
	private int rssi;
	private byte[] pc ;
	
	
	
	public byte[] getEpc() {
		return epc;
	}
	public void setEpc(byte[] epc) {
		this.epc = epc;
	}
	public int getRssi() {
		if(rssi>127){
			rssi = (rssi&0xFF)-0xFF;
			}
		return rssi;
	}
	public void setRssi(int rssi) {
		this.rssi = rssi;
	}
	public byte[] getPc() {
		return pc;
	}
	public void setPc(byte[] pc) {
		this.pc = pc;
	}
	
	

}
