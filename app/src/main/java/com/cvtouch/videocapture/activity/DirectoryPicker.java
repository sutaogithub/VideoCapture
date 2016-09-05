package com.cvtouch.videocapture.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cvtouch.videocapture.R;

/**
Copyright (C) 2011 by Brad Greco <brad@bgreco.net>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

public class DirectoryPicker extends Activity {

	public static final String START_DIR = "startDir";
	public static final String ONLY_DIRS = "onlyDirs";
	public static final String SHOW_HIDDEN = "showHidden";
	public static final String CHOSEN_DIRECTORY = "chosenDir";
	public static final int PICK_DIRECTORY = 43522432;
	private File mNowDir;
	private boolean showHidden = false;
	private boolean onlyDirs = true ;
	private Button mBtnChoose;
	private Button mBtnReturn;
	private ListView mLv;
	private Stack<String> mBackStack=new Stack();
	private ArrayList<File> mData;
	private BaseAdapter mAdapter;

	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		initView();
		initEvent();
		initData();
    }

	private void initData() {
		Bundle extras = getIntent().getExtras();
		mNowDir = Environment.getExternalStorageDirectory();
		if (extras != null) {
            String preferredStartDir = extras.getString(START_DIR);
            showHidden = extras.getBoolean(SHOW_HIDDEN, false);
            onlyDirs = extras.getBoolean(ONLY_DIRS, true);
            if(preferredStartDir != null) {
                File startDir = new File(preferredStartDir);
                if(startDir.isDirectory()) {
                    mNowDir = startDir;
                }
            }
        }
		setTitle(mNowDir.getAbsolutePath());
		String name = mNowDir.getPath();
		if(name.length() == 0)
			name = "/";
		mBtnChoose.setText("保存在 " + "'" + name + "'");
		if(!mNowDir.canRead()) {
			Context context = getApplicationContext();
			String msg = "无法获取文件内容";
			Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
			toast.show();
			return;
		}
		filter(mNowDir.listFiles(), onlyDirs, showHidden);
		mAdapter=new MyAdapter();
		mLv.setAdapter(mAdapter);
	}

	private void initEvent() {
		mBtnChoose.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                returnDir(mNowDir.getAbsolutePath());
            }
        });
		mBtnReturn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Bundle bundle=new Bundle();
				String preDir=mBackStack.isEmpty()?Environment.getExternalStorageDirectory().getAbsolutePath():mBackStack.pop();
				bundle.putString(DirectoryPicker.START_DIR, preDir);
				bundle.putBoolean(DirectoryPicker.SHOW_HIDDEN, showHidden);
				bundle.putBoolean(DirectoryPicker.ONLY_DIRS, onlyDirs);
				getdir(bundle);
			}
		});
		mLv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(!mData.get(position).isDirectory())
					return;
				String path = mData.get(position).getAbsolutePath();
				Bundle bundle=new Bundle();
				bundle.putString(DirectoryPicker.START_DIR, path);
				bundle.putBoolean(DirectoryPicker.SHOW_HIDDEN, showHidden);
				bundle.putBoolean(DirectoryPicker.ONLY_DIRS, onlyDirs);
				mBackStack.push(mNowDir.getAbsolutePath());
				getdir(bundle);
			}
		});
	}

	private void initView() {
		setContentView(R.layout.chooser_list);
		mBtnChoose = (Button) findViewById(R.id.btnChoose);
		mBtnReturn = (Button) findViewById(R.id.btn_return);
		mLv = (ListView) findViewById(R.id.lv);
		mLv.setTextFilterEnabled(true);
	}

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(requestCode == PICK_DIRECTORY && resultCode == RESULT_OK) {
	    	Bundle extras = data.getExtras();
	    	String path = (String) extras.get(DirectoryPicker.CHOSEN_DIRECTORY);
	        returnDir(path);
    	}
    }
	
    private void returnDir(String path) {
    	Intent result = new Intent();
    	result.putExtra(CHOSEN_DIRECTORY, path);
        setResult(RESULT_OK, result);
    	finish();    	
    }

	public void filter(File[] file_list, boolean onlyDirs, boolean showHidden) {
		if(mData==null)
		 	mData = new ArrayList();
		else
			mData.clear();
		for(File file: file_list) {
			if(onlyDirs && !file.isDirectory())
				continue;
			if(!showHidden && file.isHidden())
				continue;
			mData.add(file);
		}
		Collections.sort(mData);
	}
	

	public void getdir(Bundle extras){
		if (extras != null) {
			String preferredStartDir = extras.getString(START_DIR);
			showHidden = extras.getBoolean(SHOW_HIDDEN, false);
			onlyDirs = extras.getBoolean(ONLY_DIRS, true);
			if(preferredStartDir != null) {
				File startDir = new File(preferredStartDir);
				if(startDir.isDirectory()) {
					mNowDir = startDir;
				}
			}
		}
		setTitle(mNowDir.getAbsolutePath());
		String name = mNowDir.getPath();
		if(name.length() == 0)
			name = "/";
		mBtnChoose.setText("保存在 " + "'" + name + "'");
		if(!mNowDir.canRead()) {
			Context context = getApplicationContext();
			String msg = "无法获取文件内容";
			Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
			toast.show();
			return;
		}
		filter(mNowDir.listFiles(), onlyDirs, showHidden);
		mAdapter.notifyDataSetChanged();
	}

	private class MyAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			if(mData==null)
				return 0;
			else
				return mData.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView==null){
				convertView=View.inflate(DirectoryPicker.this,R.layout.list_item,null);
			}
			TextView tv= (TextView) convertView;
			tv.setText(mData.get(position).getName());
			return convertView;
		}
	}
}

