package com.cvtouch.videocapture.bean;

/**
 * @author zhangsutao
 * @file filename
 * @brief 简单的功能介绍
 * @date 2016/9/2
 */
public class Frame {
    public byte[] data;
    public long timeStamp;
    public int length;
    public Frame(byte[] data,long timeStamp){
        this.data=data;
        this.timeStamp=timeStamp;
        length=data.length;
    }
}
