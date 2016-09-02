package com.cvtouch.videocapture.recoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author zhangsutao
 * @file filename
 * @brief 简单的功能介绍
 * @date 2016/9/2
 */
public class MediaMuxerWrapper {
    private int mStartNum;
    private MediaMuxer mMuxer;
    private boolean mIsStarted;
    private int mStartCount;
    public MediaMuxerWrapper(int numOfStart,String filePath){
        mStartNum =numOfStart;
        mMuxer = null;
        try {
            mMuxer = new MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mStartCount=0;
    }
    public synchronized boolean isStarted() {
        return mIsStarted;
    }
    public synchronized boolean start() {
        mStartCount++;
        if (mStartCount == mStartNum) {
            mMuxer.start();
            mIsStarted = true;
            notifyAll();
        }else {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }
        return mIsStarted;
    }
    public synchronized void stop() {
        mStartCount--;
        if (mStartCount <= 0) {
            mMuxer.stop();
            mMuxer.release();
            mIsStarted = false;
        }
    }
    public  synchronized int addTrack(final MediaFormat format) {
        if (mIsStarted)
            throw new IllegalStateException("muxer already started");
        final int trackIx = mMuxer.addTrack(format);
        return trackIx;
    }
    public synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
            mMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
    }
}
