package com.cvtouch.videocapture.utils;

import android.media.AudioFormat;
import android.media.MediaCodecInfo;

/**
 * @author zhangsutao
 * @file filename
 * @brief 常数类
 * @date 2016/8/31
 */
public class Constans {
    public  static final String SAVE_PATH ="/sdcard/录像/";
    public  static final String MIME_TYPE="audio/mp4a-latm";
    public  static final int KEY_CHANNEL_COUNT=2;
    public  static final int KEY_SAMPLE_RATE=44100;
    public  static final int KEY_BIT_RATE=64000;
    public  static final int KEY_AAC_PROFILE= MediaCodecInfo.CodecProfileLevel.AACObjectLC;
    public  static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public  static final int CHANNEL_MODE = AudioFormat.CHANNEL_IN_STEREO;
}
