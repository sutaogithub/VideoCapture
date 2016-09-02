package com.cvtouch.videocapture.recoder;
import android.view.SurfaceHolder;
import com.cvtouch.videocapture.utils.Constans;
import java.util.Calendar;

/**
 * @author zhangsutao
 * @file filename
 * @brief 简单的功能介绍
 * @date 2016/9/2
 */
public class Mp4Recoder {
    private AudioRecoder mAudioRecoder;
    private VideoRecoder mVideoRecoder;
    private MediaMuxerWrapper mMuxer;
    private boolean isRecording;

    public Mp4Recoder(SurfaceHolder surface){
        mVideoRecoder=new VideoRecoder(surface);
        mAudioRecoder=new AudioRecoder();
    }
    public void startRecording(){
        initMuxer();
        isRecording=true;
        mVideoRecoder.setmMuxer(mMuxer);
        mVideoRecoder.startRecording();
//        mAudioRecoder.setMuxer(mMuxer);
//        mAudioRecoder.start();
    }
    public boolean isRecording(){
        return isRecording;
    }
    public void stopRecording(){
        isRecording=false;
        mVideoRecoder.stopRecording();
//        mAudioRecoder.stop();
    }
    public void initMuxer(){
        String filePath=newFileName();
        mMuxer = new MediaMuxerWrapper(1,filePath);
    }

    public String newFileName() {
        Calendar time = Calendar.getInstance();
        int year = time.get(Calendar.YEAR);
        int month = time.get(Calendar.MONTH)+1;
        int day = time.get(Calendar.DAY_OF_MONTH);
        int minute = time.get(Calendar.MINUTE);
        int hour = time.get(Calendar.HOUR);
        int sec = time.get(Calendar.SECOND);
        StringBuilder builder=new StringBuilder();
        builder.append(Constans.SAVE_PATH);
        builder.append(year);
        builder.append('-');
        builder.append(month);
        builder.append('-');
        builder.append(day);
        builder.append('-');
        builder.append(hour);
        builder.append('-');
        builder.append(minute);
        builder.append('-');
        builder.append(sec);
        builder.append(".mp4");
        return builder.toString();
    }
    public void release(){
        if(mVideoRecoder!=null){
            mVideoRecoder.release();
        }
    }
}
