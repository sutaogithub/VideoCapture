package com.cvtouch.videocapture.recoder;

import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.view.SurfaceHolder;

import com.cvtouch.videocapture.bean.Frame;
import com.cvtouch.videocapture.utils.VideoConstans;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author zhangsutao
 * @file filename
 * @brief 视频录制编码成h264
 * @date 2016/8/31
 */
public class VideoRecoder {
    private SurfaceHolder mPreview;
    private Camera mCamera;
    private MediaCodec.BufferInfo mBufferInfo;
    private MediaCodec mEncoder;
    private volatile boolean isRunning;
    private byte[] mFrameByte;
    private FileOutputStream mOutput;
    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;
    private LinkedBlockingQueue<Frame> mQueue;
    private Worker worker;
    private byte[] mCameraBuffer;
    private   int WIDTH=1280;
    private   int HEIGHT=720;
    private MediaMuxerWrapper mMuxer;
    private int mVideoTrackIndex;
    private MediaFormat mMediaFormat;
    public VideoRecoder(SurfaceHolder surface){
        mPreview =surface;
        mQueue=new LinkedBlockingQueue<>(1000);
        mBufferInfo = new MediaCodec.BufferInfo();
        try {
            mOutput=new FileOutputStream(new File("/sdcard/h264"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void setmMuxer(MediaMuxerWrapper muxer){
        mMuxer=muxer;
    }
        public boolean isRecording() {
            return isRunning;
        }

        public void startRecording(){
            isRunning=true;
            prepare();
            if(worker==null){
                worker=new Worker();
                worker.start();
            }
            mCamera.startPreview();
        }

        public void stopRecording(){
            isRunning=false;
            if(worker!=null){
                worker.interrupt();
                worker=null;
            }
        }

        protected void onEncodedSample(MediaCodec.BufferInfo info, ByteBuffer data) {
            if(mFrameByte==null||mFrameByte.length<info.size){
                mFrameByte=new byte[info.size];
            }
            data.position(info.offset);
            data.limit(info.offset+info.size);
            data.get(mFrameByte,0,info.size);
            if(!mMuxer.isStarted()){
                mMuxer.start();
            }
            mMuxer.writeSampleData(mVideoTrackIndex,data,info);
           try{
               mOutput.write(mFrameByte,0,info.size);
           } catch (IOException e) {
               e.printStackTrace();
            }
            Log.d("videoRecoder",""+info.size);
        }

        public void release() {
            if(mEncoder !=null){
                mEncoder.stop();
                mEncoder.release();
                mEncoder =null;
            }
            if(mCamera !=null){
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera =null;
            }
            if(mMuxer!=null&&mMuxer.isStarted()){
                mMuxer.stop();
            }
            if(mOutput!=null){
                try {
                    mOutput.close();
                    mOutput=null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        private boolean prepare() {
            initCamera();
            MediaCodecInfo codecInfo = selectCodec(VideoConstans.MIME_TYPE);
            int colorFormat = selectColorFormat(codecInfo, VideoConstans.MIME_TYPE);
            MediaFormat format = MediaFormat.createVideoFormat(VideoConstans.MIME_TYPE, WIDTH, HEIGHT);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
            format.setInteger(MediaFormat.KEY_BIT_RATE, VideoConstans.VIDEO_BITRATE);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, VideoConstans.VIDEO_FRAME_PER_SECOND);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,VideoConstans.VIDEO_I_FRAME_INTERVAL);
            try {
                mEncoder = MediaCodec.createEncoderByType(VideoConstans.MIME_TYPE);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mEncoder.start();
            inputBuffers= mEncoder.getInputBuffers();
            outputBuffers= mEncoder.getOutputBuffers();
            return true;
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
//        parameters.setPreviewFormat(ImageFormat.YV12);
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
//                        try {
//                            mOutput.write(data,0,data.length);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }

                        if(isRunning){
                            try {
                                Frame frame=new Frame(data,System.nanoTime());
                                Log.d("queuesize",mQueue.size()+"");
                                mQueue.put(frame);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
//                        camera.addCallbackBuffer(mCameraBuffer);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void signalEndOfStream(){
        int inputBufferIndex = mEncoder.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0){
            mEncoder.queueInputBuffer(inputBufferIndex, 0, 0,
                    System.currentTimeMillis(), MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        }
        int outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo,0);
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
            onEncodedSample(mBufferInfo,outputBuffer);
            outputBuffer.clear();
            mEncoder.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
        }
    }
    private void encode(Frame frame) {
        int inputBufferIndex = mEncoder.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0){
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(frame.data);
            mEncoder.queueInputBuffer(inputBufferIndex, 0, frame.length,frame.timeStamp, 0);
        }
        int outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo,0);
        if(outputBufferIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
            mMediaFormat=mEncoder.getOutputFormat();
            mVideoTrackIndex=mMuxer.addTrack(mMediaFormat);
        }
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
            onEncodedSample(mBufferInfo,outputBuffer);
            outputBuffer.clear();
            mEncoder.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
        }
    }

    public class Worker extends Thread {
        @Override
        public void run() {
            while(isRunning){
                try {
                    Frame frame=mQueue.take();
                    swapNV21(frame.data);
                    encode(frame);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
            signalEndOfStream();
            release();
        }
    }


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

    private static int selectColorFormat(MediaCodecInfo codecInfo,
                                         String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo
                .getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        //没有找到支持的颜色格式
        return 0;
    }
    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            //yuv的颜色格式
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }
    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }
}

