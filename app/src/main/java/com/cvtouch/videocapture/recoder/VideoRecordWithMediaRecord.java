package com.cvtouch.videocapture.recoder;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceView;

import com.cvtouch.videocapture.utils.Constans;

import java.io.IOException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author zhangsutao
 * @file filename
 * @brief 视频录制工具类
 * @date 2016/8/31
 */
    public  class VideoRecordWithMediaRecord {
        private MediaRecorder mRecoder;
        private boolean isRecording;
        private Camera mCamera;
        private Timer timer;
        private long timeSize = 0;
        private String saveFileName;
        private RecoderListener mListener;
        private SurfaceView mSv;


        public VideoRecordWithMediaRecord(){
        }
        public void setRecordListener(RecoderListener listener){
            mListener=listener;
        }

        public  void prepare(SurfaceView surfaceView) {
                mSv=surfaceView;
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
//                Camera.Parameters parameters = mCamera.getParameters();
//                parameters.setPreviewFormat(ImageFormat.YV12);
//                mCamera.setParameters(parameters);
                if (mCamera != null) {
                    try {
                        mCamera.setPreviewDisplay(mSv.getHolder());
                        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                            @Override
                            public void onPreviewFrame(byte[] data, Camera camera) {
                                Log.d("frameThread",Thread.currentThread().getName());
                            }
                        });
                        mCamera.startPreview();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }

        public boolean isRecording(){
            return isRecording;
        }
        public void startRecording(){
            if(mRecoder==null)
                mRecoder =  new MediaRecorder(); //  创建mediarecorder对象
            else
                mRecoder.reset();
            mCamera.unlock();
            mRecoder.setCamera(mCamera);
            //  设置录制视频源为Camera(相机)
            mRecoder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            //   设置录音源
            mRecoder.setAudioSource(MediaRecorder.AudioSource.MIC);
            //  设置录制完成后视频的封装格式THREE_GPP为3gp.MPEG_4为mp4
            mRecoder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            //  设置录制的视频编码h263 h264
            mRecoder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            //   设置音频编码aac
            mRecoder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            //  设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
            mRecoder.setVideoSize(1080, 720);
            //  设置声道
            mRecoder.setAudioChannels(2);
            //  设置视频比特率
            mRecoder.setVideoEncodingBitRate(50000);
            //  设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错
            mRecoder.setVideoFrameRate(30);
            //  设置视频文件输出的路径
            saveFileName = newFileName();
            if (mListener!=null){
                mListener.saveFile(saveFileName);
            }
            mRecoder.setOutputFile(saveFileName);

            try {
                //  准备录制
                mRecoder.prepare();
                //  开始录制
                mRecoder.start();
            }  catch (IllegalStateException e) {
                //  TODO Auto-generated catch block
                e.printStackTrace();
            }  catch (IOException e) {
                //  TODO Auto-generated catch block
                e.printStackTrace();
            }
            isRecording =  true;
            timeSize = 0;
            timer =  new Timer();
            timer.schedule( new TimerTask() {

                @Override
                public  void run() {
                    //  TODO Auto-generated method stub
                    timeSize++;
                    if(mListener!=null){
                        mListener.timeInSecond(timeSize);
                    }
                    Log.d("recoder",timeSize+"");
                }
            }, 0,1000);
        }
        public  void stopRecording() {
            if (mRecoder !=  null) {
                //  停止
                isRecording=false;
                mRecoder.stop();
                timer.cancel();
            }
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

        public  void release() {
            if (mRecoder !=  null) {
                if(isRecording)
                    mRecoder.stop();
                mRecoder.release();
                mCamera.lock();
                mCamera.release();
                mRecoder =  null;
                timer.cancel();
            }
        }

    public interface RecoderListener{
        public void timeInSecond(long time);
        public void saveFile(String path);
    }
}

