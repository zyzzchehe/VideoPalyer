package com.rocktech.player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class TestActivity extends Activity {
    private String TAG = "TestActivity";
    private Context mContext;
    private VideoView videoView;
    private AlertDialog alertDialog;
    private String recordCountRebootPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/recordCount";
    private File file = new File(recordCountRebootPath);
    private ArrayList<String> list = new ArrayList<>();//add by yazhou
    private String fromFile = "/storage/udisk/video.mp4";
    private String toFile = "/sdcard/video.mp4";
    private File tmpFile = new File(toFile);
    private ProgressDialog progressDialog;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private boolean hasCopy;

    private Handler handler = new Handler(){
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == 100){//视频copy不全
                alertDialog.show();
                alertDialog.setCancelable(false);
            } else if (msg.what == 200) {//正在copy视频
                progressDialog = ProgressDialog.show(TestActivity.this, "你好", "正在copy视频文件，请稍等");
            } else if(msg.what == 300){//视频已准备好，开始播放
                if(progressDialog != null){
                    progressDialog.dismiss();
                    progressDialog = null;
                }
                editor.putBoolean("hascopy",true);
                editor.commit();
                startTargetPlayerEx();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo("com.rocktech.player",0);
            int mFlags = applicationInfo.flags & ApplicationInfo.FLAG_STOPPED;
            Log.i(TAG, "onCreate: mFlags = "+mFlags);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        mContext = this;
        videoView = (VideoView) findViewById(R.id.video_view);
        videoView.setVideoURI(Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath()+"/video.mp4"));//为视频播放器设置视频路径
        videoView.setMediaController(new MediaController(TestActivity.this));//显示控制栏
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.i(TAG, "--------------视频准备完毕,可以进行播放.......");
            }
        });
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
//                videoView.stopPlayback();
//                videoView.resume();
                videoView.start();
            }
        });
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.i(TAG, "---------------------视频播放失败...........");
                return false;
            }
        });

        alertDialog = new AlertDialog.Builder(this)
                .setTitle("你好")
                .setMessage("视频copy不完整，请重启设备，不要拔掉U盘")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create();

        preferences = getSharedPreferences("iscopy", Context.MODE_PRIVATE);
        editor = preferences.edit();

        hasCopy = preferences.getBoolean("hascopy",false);
        Log.d(TAG,"hasCopy == "+hasCopy);

        if(new File("/sdcard/video.mp4").exists()){
            startTargetPlayerEx();
        }else{
            //android代码中，调用反射
            StorageManager storageManager = (StorageManager) getSystemService(STORAGE_SERVICE);
            Method setMethod = null;
            try {
                setMethod = StorageManager.class.getMethod("getVolumePaths");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            String[] volumesPath = new String[0];
            try {
                volumesPath = (String[]) setMethod.invoke(storageManager);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            for (String path : volumesPath){
                list.add(path);
            }
            if(list.size() == 0){
                Log.i(TAG, "onCreate: list size is 0, has no video file");
                return;
            }
            /*启动一个子线程去copy视频*/
            new Thread(mRunnable).start();

            OutputStream os = null;
            try {
                java.lang.Process p = Runtime.getRuntime().exec("sh");
                String cmd = "sync\n"+"exit\n";
                os = p.getOutputStream();
                os.write(cmd.getBytes());
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                try {
                    if (os != null)  os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
    private void startTargetPlayerEx(){
        recordReboot();
        getRecordFromFile();
        videoView.start();
    }

    /**
     * copy 视频是耗时操作，放在子线程
     */
    Runnable mRunnable = new Runnable(){
        @Override
        public void run() {
            if(!hasCopy){
                handler.sendEmptyMessage(200);
            }
            Log.i(TAG, "run: list size = "+list.size());
            for (String filePath : list) {
                File dir = new File(filePath+"/video.mp4");
                if(dir.exists()){
                    fromFile = filePath+"/video.mp4";
                    copyFile(fromFile,toFile);//copy video
                    break;
                }
            }
            if ((tmpFile.exists() && hasCopy) || checkIsCompleteEx(fromFile).equals(checkIsCompleteEx(toFile))) {
                handler.sendEmptyMessage(300);
            } else {
                handler.sendEmptyMessage(100);
            }
        }
    };

    private String checkIsCompleteEx(String filePath){
        File file = new File(filePath);
        if(!file.exists()){
            if(filePath.equals(fromFile)){
                return "fromFile not exist";
            } else if(filePath.equals(toFile)){
                return "toFile not exist";
            }
        }
        if (!file.isFile()) return "";
        MessageDigest digest;
        FileInputStream in = null;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            byte buffer[] = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }
            BigInteger bigInt = new BigInteger(1, digest.digest());
            String md5 = bigInt.toString(16);
            Log.d(TAG,"md5 == "+md5);
            return md5;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }


    public void copyFile(String oldPath, String newPath) {
        FileInputStream inStream = null;//读入原文件
        FileOutputStream fs = null;
        Looper.prepare();
        try {
            File oldFile = new File(oldPath);
            if (!oldFile.exists()) {
                Toast.makeText(mContext, "源视频文件不存在！！！！！", Toast.LENGTH_SHORT).show();
                return;
            }
            File newFile = new File(newPath);
            if (!newFile.exists()) { //文件不存在时
                inStream = new FileInputStream(oldPath); // 读入原文件
                fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[8192];
                int length;
                while ((length = inStream.read(buffer)) > 0) {
                    fs.write(buffer, 0, length);
                }
            }
        } catch (Exception e) {
            Toast.makeText(mContext, "拷贝视频文件到主目录失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } finally {
            try {
                if(inStream != null) inStream.close();
                if(fs != null) fs.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*记录重启次数*/
    private void  recordReboot() {
        long currentTime = System.currentTimeMillis();//当前时间
        Date date = new Date(currentTime);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String displayTime = sdf.format(date);
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        BufferedWriter bw = null;
        try {
            fos = new FileOutputStream(file,true);//参数append 为true，表示可以向文件末尾不断拼接，否则只能写入一行
            osw = new OutputStreamWriter(fos);
            bw = new BufferedWriter(osw);
            bw.write(displayTime + ": system reboot again");
            bw.write("\n");
            bw.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(fos != null) fos.close();
                if(osw != null) osw.close();
                if(bw != null) bw.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void getRecordFromFile(){
        View view = View.inflate(mContext,R.layout.cus_imageview,null);
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        int cnt = 0;
        try {
            fis = new FileInputStream(file);
            isr = new InputStreamReader(fis);
            br = new BufferedReader(isr);
            while (br.readLine() != null){
                cnt++;
            }
            if(cnt >= 10){
                alertDialog.setTitle("警告");
                alertDialog.setMessage("该设备可能有故障，已经重启 " + cnt + " 次");
                alertDialog.setView(view);
            }else{
                alertDialog.setTitle("提示");
                alertDialog.setMessage("该设备已经重启 " + cnt + " 次");
            }
//            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            alertDialog.show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if(fis != null) fis.close();
                if(isr != null) isr.close();
                if(br != null) br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
