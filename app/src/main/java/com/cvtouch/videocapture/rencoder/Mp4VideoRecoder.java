package com.cvtouch.videocapture.rencoder;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import com.cvtouch.videocapture.bean.Frame;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Administrator on 2016/9/4.
 */
public class Mp4VideoRecoder {
    private MediaMuxerWrapper mMuxer;
    private boolean isRecording;
    private Camera mCamera;
    private SurfaceHolder mPreview;
    private final int WIDTH=1280;
    private final int HEIGHT=720;
    private MediaVideoEncoder mVideoEncoder;
    private final String TAG="Mp4VideoRecoder";
    private static final boolean DEBUG = false;	// TODO set false on release
    public Mp4VideoRecoder(SurfaceHolder surface){
        mPreview=surface;
        initCamera();
    }
    public void startRecording(){
        if(!isRecording){
            try {
                isRecording=true;
                mMuxer = new MediaMuxerWrapper(".mp4");	// if you record audio only, ".m4a" is also OK.
                if (true) {
                        // for video capturing
                    mVideoEncoder=new MediaVideoEncoder(mMuxer, mMediaEncoderListener,WIDTH, HEIGHT);
                }
                if (true) {
                        // for audio capturing
                    new MediaAudioEncoder(mMuxer, mMediaEncoderListener);
                }
                mMuxer.prepare();
                mMuxer.startRecording();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }
    public void stopRecording(){
        if(isRecording){
            isRecording=false;
            if (mMuxer != null) {
                mMuxer.stopRecording();
                mMuxer = null;
                // you should not wait here
            }
        }
    }
    public File getSaveFile(){
        if(mMuxer!=null){
            return mMuxer.getSaveFile();
        }
        return null;
    }
    public boolean isRecording(){
        return isRecording;
    }
    private void initCamera() {
        if(mCamera ==null)
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        Camera.Parameters parameters = mCamera.getParameters();
        List<int[]> fps=parameters.getSupportedPreviewFpsRange();
        List<Camera.Size> videoSize=parameters.getSupportedPreviewSizes();
//        WIDTH=videoSize.get(videoSize.size()-1).width;
//        HEIGHT=videoSize.get(videoSize.size()-1).height;
        parameters.setFlashMode("off");
        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        //这个属性要根据硬编码所支持的颜色格式来定默认是nv21，设备需支持COLOR_FormatYUV420SemiPlanar，yv12设备需支持COLOR_FormatYUV420Planar
        parameters.setPreviewFormat(ImageFormat.YV12);
        //这两个属性 如果这两个属性设置的和真实手机的不一样时，就会报错
        parameters.setPreviewSize(WIDTH,HEIGHT);
        parameters.setPictureSize(WIDTH,HEIGHT);
        mCamera.setParameters(parameters);
//        mCameraBuffer=new byte[WIDTH*HEIGHT*3/2];
//        mCamera.addCallbackBuffer(mCameraBuffer);
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(mPreview);
                mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        if(data!=null&&mVideoEncoder!=null){
                            try {
                                swapYV12UV(data);
                                Frame frame=new Frame(data,System.nanoTime());
                                mVideoEncoder.addFrame(frame);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onPrepared:encoder=" + encoder);
            if (encoder instanceof MediaVideoEncoder){
                mCamera.startPreview();
            }
        }
        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onStopped:encoder=" + encoder);
            if (encoder instanceof MediaVideoEncoder) {
                mCamera.stopPreview();
            }
        }
    };
    private void swapYV12UV(byte[] data) {
        byte tmp;
        int endHeight=HEIGHT*5/4;
        int distance=HEIGHT*WIDTH/4;
        int u=0;
        int v=0;
        for(int i=HEIGHT;i<endHeight;i++){
            for(int j=0;j<WIDTH;j++){
                u=i*WIDTH+j;
                v=i*WIDTH+j+distance;
                tmp=data[u];
                data[u]=data[v];
                data[v]=tmp;
            }
        }
    }
    private void swapNV21(byte[] data){
        byte tmp = 0;
        int endheight = HEIGHT*3/2;
        int u=0;
        int v=0;
        for (int i = HEIGHT; i < endheight; i++) {
            for(int j=0;j<WIDTH;j+=2){
                u=i*WIDTH+j;
                v=u+1;
                tmp = data[u];
                data[u] = data[v];
                data[v] = tmp;
            }
        }
    }
}
