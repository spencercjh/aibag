package com.shou.demo.jiuray.command;

import java.util.List;

/**
 * @author jruhf
 */
public interface CommendManager {

    /**
     * 设置波特率
     *
     * @return
     */
    public boolean setBaudrate();

    /**
     * 获取硬件版本
     */
    public byte[] getFirmware();

    /**
     * 设置输入输出功率
     *
     * @param value
     * @return
     */
    public boolean setOutputPower(int value);

    /**
     * 实时盘存
     *
     * @return
     */
    public List<InventoryInfo> inventoryRealTime();

    /**
     * 选定标签
     *
     * @param epc
     */
    public void selectEPC(byte[] epc);

    /**
     * 读数据
     * int memBank数据区
     * int startAddr数据区起始地址（以字为单位）
     * int length要读取的数据长度(以字为单位)
     * byte[] accessPassword 访问密码
     * 返回的byte[] 为  EPC + 读取的数据
     */
    public byte[] readFrom6C(int memBank, int startAddr, int length, byte[] accessPassword);

    /**
     * 写数据
     * byte[] password 访问密码
     * int memBank 数据区
     * int startAddr 起始地址（以WORD为单位）
     * int wordCnt 写入数据的长度（以WORD为单位 1word = 2bytes）
     * byte[] data 写入数据
     * 返回 boolean，true写入数据正确，false写入数据失败
     */
    public boolean writeTo6C(byte[] password, int memBank, int startAddr, int dataLen, byte[] data);

    /**
     * 设置灵敏度
     *
     * @param value
     */
    public void setSensitivity(int value);

    /**
     * 锁定标签
     *
     * @param password 访问密码
     * @param memBank  数据区
     * @param lockType 锁定类型
     * @return
     */
    public boolean lock6C(byte[] password, int memBank, int lockType);


    public boolean killTag(byte[] password);


    /**
     *
     */
    public void close();

    /**
     * 计算校验和
     *
     * @param data
     * @return
     */
    public byte checkSum(byte[] data);


    /**
     * @param startFrequency 起始频点
     * @param freqSpace      频点间隔
     * @param freqQuality    频点数量
     * @return
     */
    public int setFrequency(int startFrequency, int freqSpace, int freqQuality);


}