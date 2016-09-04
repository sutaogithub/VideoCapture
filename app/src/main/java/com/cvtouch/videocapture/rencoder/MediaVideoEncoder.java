package com.cvtouch.videocapture.rencoder;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MediaVideoEncoder.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.opengl.EGLContext;
import android.util.Log;
import android.view.Surface;

import com.cvtouch.videocapture.bean.Frame;


public class MediaVideoEncoder extends MediaEncoder {
	private static final boolean DEBUG = false;    // TODO set false on release
	private static final String TAG = "MediaVideoEncoder";

	private static final String MIME_TYPE = "video/avc";
	// parameters for recording
	private static final int FRAME_RATE = 25;
	private static final float BPP = 0.25f;
	private VideoThread mVideoThread;

	private final int mWidth;
	private final int mHeight;
	private LinkedBlockingQueue<Frame> queue;

	public MediaVideoEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener, final int width, final int height) {
		super(muxer, listener);
		if (DEBUG) Log.i(TAG, "MediaVideoEncoder: ");
		mWidth = width;
		mHeight = height;
		queue = new LinkedBlockingQueue<>(100);
	}


	public void addFrame(Frame frame) throws InterruptedException {
		queue.put(frame);
	}


	@Override
	void startRecording() {
		super.startRecording();
		if (mVideoThread == null) {
			mVideoThread = new VideoThread();
			mVideoThread.start();
		}
	}


	@Override
	protected void prepare() throws IOException {
		if (DEBUG) Log.i(TAG, "prepare: ");
		mTrackIndex = -1;
		mMuxerStarted = mIsEOS = false;
		final MediaCodecInfo videoCodecInfo = selectCodec(MIME_TYPE);
		int colorFormat = selectColorFormat(videoCodecInfo,MIME_TYPE);
		if (videoCodecInfo == null) {
			Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
			return;
		}
		if (DEBUG) Log.i(TAG, "selected codec: " + videoCodecInfo.getName());

		final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
		format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);    // API >= 18
		format.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate());
		format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
		format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
		if (DEBUG) Log.i(TAG, "format: " + format);

		mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
		mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		// get Surface for encoder input
		// this method only can call between #configure and #start
		mMediaCodec.start();
		if (DEBUG) Log.i(TAG, "prepare finishing");
		if (mListener != null) {
			try {
				mListener.onPrepared(this);
			} catch (final Exception e) {
				Log.e(TAG, "prepare:", e);
			}
		}
	}


	@Override
	protected void release() {
		if (DEBUG) Log.i(TAG, "release:");
		mVideoThread.interrupt();
		mVideoThread = null;
		queue.clear();
		super.release();
	}

	private int calcBitRate() {
		final int bitrate = (int) (BPP * FRAME_RATE * mWidth * mHeight);
		Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
		return bitrate;
	}

	/**
	 * select the first codec that match a specific MIME type
	 *
	 * @param mimeType
	 * @return null if no codec matched
	 */
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

	/**
	 * select color format available on specific codec and we can use.
	 *
	 * @return 0 if no colorFormat is matched
	 */
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


	private static final boolean isRecognizedFormat(final int colorFormat) {
		if (DEBUG) Log.i(TAG, "isRecognizedViewoFormat:colorFormat=" + colorFormat);
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

	@Override
	protected void signalEndOfInputStream() {
		super.signalEndOfInputStream();
		mIsEOS = true;
	}

	private class VideoThread extends Thread {
		@Override
		public void run() {
			if (mIsCapturing) {
				if (DEBUG) Log.v(TAG, "VideoThread:start video recording");
				for (; mIsCapturing && !mRequestStop && !mIsEOS; ) {
					// read audio data from internal mic
					try {
						Frame frame = queue.take();
						encode(frame.data, getPTSUs());
						frameAvailableSoon();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				frameAvailableSoon();
			}
		}

	}
}
