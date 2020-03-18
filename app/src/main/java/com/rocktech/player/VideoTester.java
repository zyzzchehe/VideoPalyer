package com.rocktech.player;

import android.content.Context;
import android.os.storage.StorageManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class VideoTester {
    private static final String TAG = "VideoTester";
    public String mVideoPath = null;
    private Context mContext;

    public static List<Video> mLstFile = new ArrayList<Video>();// 结果 List
    public static int mFileLength = 0;

    public void getFiles(ArrayList<String> Path){
        for(String str : Path){
            File[] files  = null;
            if(!new File(str).exists()){
                Log.i(TAG, "yazhou "+str+" 不存在");
                continue;
            }else{
                files = new File(str).listFiles();
                if(files == null){
                    continue;
                }
            }
            Log.i(TAG, "yazhou begin 2");
            Video video = null;
            String[] extension = { ".mp4", ".rmvb", ".avi", ".3gp" };

            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                if (f.isFile()) {
                    for (int j = 0; j < extension.length; j++) {
                        if (f.getPath().substring(f.getPath().length() - extension[j].length()).equals(extension[j])){
                            Log.i(TAG, "wz add file:" + f.getPath());
                            video = new Video();
                            video.setVideoPath(f.getPath()); // 路径
                            video.setVideoName(f.getPath().substring(str.length())); // 视频名
                            mLstFile.add(video);
                            video = null;
                            return;
                        }
                    }

                }
            }

            if(mLstFile.size() == 0){
                Toast.makeText(mContext, "U盘中不存在video目录或者video目录下没有视频文件", Toast.LENGTH_SHORT).show();
                return;
            }
        }

    }

    //add by yazhou begin
    private ArrayList<String> getUsbPath(){
        ArrayList<String> list = new ArrayList<String>();
        String usbPath = "";
        Class<?> c = null;
        try {
            c = Class.forName("android.os.storage.StorageManager");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Method setMethod = null;
        try {
            setMethod = c.getMethod("getVolumePaths");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        String[] volumesPath = new String[0];
        try {
            volumesPath = (String[]) setMethod.invoke(c.newInstance());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        for(String sv2 : volumesPath){
            usbPath = sv2+"/video/";
            Log.d(TAG, "usbPath == "+usbPath);
            if(!usbPath.contains("/storage/emulated/")){
                Log.d(TAG, "final usbPath == "+usbPath);
                list.add(usbPath);
            }
        }
        return list;
    }
    //add by yazhou end

    public VideoTester(Context cxt) {
        Log.e(TAG, "VideoTester");
        mContext = cxt;
    }
    public void clearList() {
        mFileLength = 0;
        mLstFile.clear();
    }

    public boolean isTestFileExist() {
        Log.i(TAG, "yazhou begin search file");
        clearList();
        getFiles(getUsbPath());
        mFileLength = mLstFile.size();
        if (mFileLength > 0) {
            mVideoPath = mLstFile.get(0).getVideoPath();
            return true;
        }
        return false;
    }
}
