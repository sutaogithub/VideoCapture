package com.cvtouch.videocapture.recoder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import com.cvtouch.videocapture.utils.Constans;


/**
 * @author zhangsutao
	* @file AudioRecoder.java
	* @brief aac音频编码器
	* @date 2016/8/7
			*/
public class AudioRecoder {

	private Worker mWorker;
	private final String TAG="AudioRecoder";
	private byte[] mFrameByte;
	private long mTimeStamp;
	private final int mFrameSize = 2048;
	private byte[] mBuffer;
	private boolean isRunning=false;
	private MediaCodec mEncoder;
	private AudioRecord mRecord;
	MediaCodec.BufferInfo mBufferInfo;
	private ByteBuffer[] inputBuffers;
	private ByteBuffer[] outputBuffers;
	private MediaMuxerWrapper mMuxer;
	private  MediaFormat mMediaFormat;
	private int mAudioTrackIndex;
	private ByteBuffer mMuxerBuffer=ByteBuffer.allocate(1000);
	private FileOutputStream mOutput;
	public AudioRecoder() {

		try {
			mOutput=new FileOutputStream(new File("/sdcard/pcm"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	public void setMuxer(MediaMuxerWrapper muxer){
		mMuxer=muxer;
	}
	public void start(){
		if(mWorker==null){
			mWorker=new Worker();
			mWorker.setRunning(true);
			mWorker.start();
		}

	}
	public void stop(){
		if(mWorker!=null){
			mWorker.setRunning(false);
			mWorker=null;
		}
	}
	/**
	 * 编码器配置
	 * @return true配置成功，false配置失败
	 */
	private boolean prepare() {
		try {
			mBufferInfo = new MediaCodec.BufferInfo();
			mEncoder = MediaCodec.createEncoderByType(Constans.MIME_TYPE);
			MediaFormat mediaFormat = MediaFormat.createAudioFormat(Constans.MIME_TYPE,
					Constans.KEY_SAMPLE_RATE, Constans.KEY_CHANNEL_COUNT);
			mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, Constans.KEY_BIT_RATE);
			mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
					Constans.KEY_AAC_PROFILE);
			mEncoder.configure(mediaFormat, null, null,
					MediaCodec.CONFIGURE_FLAG_ENCODE);
			mEncoder.start();
			inputBuffers=mEncoder.getInputBuffers();
			outputBuffers =mEncoder.getOutputBuffers();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		mBuffer = new byte[mFrameSize];
		int minBufferSize = AudioRecord.getMinBufferSize(Constans.KEY_SAMPLE_RATE,Constans. CHANNEL_MODE,
				Constans.AUDIO_FORMAT);
		mRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
				Constans.KEY_SAMPLE_RATE,Constans.CHANNEL_MODE, Constans.AUDIO_FORMAT, minBufferSize * 2);
		mRecord.startRecording();
		return true;
	}
	private class Worker extends Thread{

		@Override
		public void run() {
			if(!prepare()){
				return ;
			}
			while(isRunning){
				mTimeStamp=System.nanoTime()/1000;
				int num = mRecord.read(mBuffer, 0, mFrameSize);
				Log.d(TAG, "buffer = " + mBuffer.toString() + ", num = " + num);
				encode(mBuffer);
			}
			signalEndOfStream();
			release();
		}

		public void setRunning(boolean run){
			isRunning=run;
		}

		/**
		 * 释放资源
		 */
		private void release() {
			if(mEncoder!=null){
				mEncoder.stop();
				mEncoder.release();
			}
			if(mRecord!=null){
				mRecord.stop();
				mRecord.release();
				mRecord = null;
			}
			if(mMuxer!=null&&mMuxer.isStarted()){
				mMuxer.stop();
			}
			if(mOutput!=null){
				try {
					mOutput.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		private void signalEndOfStream(){
			int inputBufferIndex = mEncoder.dequeueInputBuffer(-1);
			if (inputBufferIndex >= 0){
				mEncoder.queueInputBuffer(inputBufferIndex, 0, 0,
						mTimeStamp, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
			}
			int outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo,0);
			while (outputBufferIndex >= 0) {
				ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
				//给adts头字段空出7的字节
//				int length=mBufferInfo.size+7;
//				if(mFrameByte==null||mFrameByte.length<length){
//					mFrameByte=new byte[length];
//				}
//				addADTStoPacket(mFrameByte,length);
//				outputBuffer.position(mBufferInfo.offset);
//				outputBuffer.limit(mBufferInfo.size);
//				outputBuffer.get(mFrameByte,7,mBufferInfo.size);
//				try {
//					mOutput.write(mFrameByte,0,length);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//				mMuxerBuffer.clear();
//				mMuxerBuffer.put(mFrameByte,0,length);
//				mBufferInfo.offset=0;
//				mBufferInfo.size=length;
				if(!mMuxer.isStarted()){
					mMuxer.start();
				}
				mMuxer.writeSampleData(mAudioTrackIndex,outputBuffer,mBufferInfo);
				outputBuffer.clear();
				mEncoder.releaseOutputBuffer(outputBufferIndex, false);
				outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
			}
		}

		private void encode(byte[] data) {
			int inputBufferIndex = mEncoder.dequeueInputBuffer(-1);
			if (inputBufferIndex >= 0) {
				ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];

				inputBuffer.clear();
				inputBuffer.put(data);
				inputBuffer.limit(data.length);
				mEncoder.queueInputBuffer(inputBufferIndex, 0, data.length,
					mTimeStamp, 0);
			}
			int outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
			if(outputBufferIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
				mMediaFormat=mEncoder.getOutputFormat();
				mAudioTrackIndex =mMuxer.addTrack(mMediaFormat);
				Log.d("mxxx","success");
			}
			while (outputBufferIndex >= 0) {
				Log.d("mxxx","encode");
				ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
				//给adts头字段空出7的字节
//				int length=mBufferInfo.size+7;
//				if(mFrameByte==null||mFrameByte.length<length){
//					mFrameByte=new byte[length];
//				}
//				addADTStoPacket(mFrameByte,length);
//				outputBuffer.position(mBufferInfo.offset);
//				outputBuffer.limit(mBufferInfo.size);
//				outputBuffer.get(mFrameByte,7,mBufferInfo.size);
//				try {
//					mOutput.write(mFrameByte,0,length);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//				mMuxerBuffer.clear();
//				mMuxerBuffer.put(mFrameByte,0,length);
//				mBufferInfo.offset=0;
//				mBufferInfo.size=length;
				if(!mMuxer.isStarted()){
					mMuxer.start();
				}
				mMuxer.writeSampleData(mAudioTrackIndex,outputBuffer,mBufferInfo);
				Log.d("music",mBufferInfo.presentationTimeUs+"");
				outputBuffer.clear();
				mEncoder.releaseOutputBuffer(outputBufferIndex, false);
				outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
			}
		}

		/**
		 * 给编码出的aac裸流添加adts头字段
		 * @param packet 要空出前7个字节，否则会搞乱数据
		 * @param packetLen
         */
		private void addADTStoPacket(byte[] packet, int packetLen) {
			int profile = 2;  //AAC LC
			int freqIdx = 4;  //44.1KHz
			int chanCfg = 2;  //CPE
			packet[0] = (byte)0xFF;
			packet[1] = (byte)0xF9;
			packet[2] = (byte)(((profile-1)<<6) + (freqIdx<<2) +(chanCfg>>2));
			packet[3] = (byte)(((chanCfg&3)<<6) + (packetLen>>11));
			packet[4] = (byte)((packetLen&0x7FF) >> 3);
			packet[5] = (byte)(((packetLen&7)<<5) + 0x1F);
			packet[6] = (byte)0xFC;
		}
	}
}
