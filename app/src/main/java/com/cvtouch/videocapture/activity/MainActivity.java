package com.cvtouch.videocapture.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.cvtouch.videocapture.R;
import com.cvtouch.videocapture.recoder.Mp4Recoder;
import com.cvtouch.videocapture.recoder.VideoRecoder;
import com.cvtouch.videocapture.recoder.VideoRecordWithMediaRecord;
import com.cvtouch.videocapture.utils.Constans;

import java.io.File;

public class MainActivity extends Activity implements SurfaceHolder.Callback{

    private SurfaceView mSv;
    private Mp4Recoder mRecoder;
    private TextView mTime;
    private Button mBtn;
    private VideoRecordWithMediaRecord.RecoderListener mListener;
    private File mFile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
        setContentView(R.layout.activity_main);
        initView();
        initSaveFile();
        initEvent();
    }

    private void initEvent() {
        mListener=new VideoRecordWithMediaRecord.RecoderListener() {
            @Override
            public void timeInSecond(long time) {
                long hour=time/3600;
                int minute= (int) ((time-hour*3600)/60);
                int second= (int) (time-hour*3600-minute*60);
                final String min=minute>=10?minute+"":"0"+minute;
                final String sec=second>=10?second+"":"0"+second;
                final String hou=hour>=10?hour+"":"0"+hour;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTime.setText(hou+":"+min+":"+sec);
                    }
                });
            }

            @Override
            public void saveFile(String path) {
                mFile=new File(path);
            }
        };
//        mRecoder.setRecordListener(mListener);
        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mRecoder.isRecording()){
                    mRecoder.stopRecording();
                    mTime.setVisibility(View.GONE);
                    mBtn.setBackgroundResource(R.drawable.start);
                    shouSaveDialog();
                }else {
                    mRecoder.startRecording();
                    mTime.setText("00:00:00");
                    mTime.setVisibility(View.VISIBLE);
                    mBtn.setBackgroundResource(R.drawable.stop);
                }
            }
        });
    }

    private void shouSaveDialog() {
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("录制结束").setMessage("是否保存到/sdcard/录像？").create();
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "保存", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//                mFile.delete();
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void initView() {
        mSv= (SurfaceView) findViewById(R.id.sv);
        mSv.getHolder().addCallback(this);
        mTime= (TextView) findViewById(R.id.time_show);
        mBtn= (Button) findViewById(R.id.btn);
    }

    private void initSaveFile() {
        File file=new File(Constans.SAVE_PATH);
        if(!file.exists()){
            file.mkdir();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecoder.release();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mRecoder=new Mp4Recoder(mSv.getHolder());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        holder.removeCallback(this);
    }
}
