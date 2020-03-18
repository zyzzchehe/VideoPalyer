package com.rocktech.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class RebootCountActivity extends Activity {
    private Context mContext;
    private String recordCountRebootPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/recordCount";
    private File file = new File(recordCountRebootPath);
    Timer timer = new Timer();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        mContext = this;
        timer.schedule(task,10*1000);
        new Thread(new Runnable() {
            @Override
            public void run() {
                recordReboot();
                getRecordFromFile();
            }
        }).start();
        sync();
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
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            reboot();
        }
    };
    private void reboot(){
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String cmd = "sync\n"+"reboot\n"+"exit\n";
        String[] cmds = {"/system/bin/sh","-c", cmd};
        doExecs(cmds);
    }

    private void sync(){
        String cmd = "sync\n"+"exit\n";
        String[] cmds = {"/system/bin/sh","-c", cmd};
    }

    private String doExecs(String[] cmds) {
        String s = "";
        BufferedReader in = null;
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmds);
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                s += line;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(in != null){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return s;
    }

    private void getRecordFromFile(){
        View view = View.inflate(mContext,R.layout.cus_imageview,null);
        TextView tv01 = view.findViewById(R.id.tv_message01);
        TextView tv02 = view.findViewById(R.id.tv_message01);
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        AlertDialog.Builder dialogBuilder = null;
        AlertDialog alertDialog = null;
        int cnt = 0;
        try {
            fis = new FileInputStream(file);
            isr = new InputStreamReader(fis);
            br = new BufferedReader(isr);
            while (br.readLine() != null){
                cnt++;
            }
            Looper.prepare();
            dialogBuilder = new AlertDialog.Builder(mContext);;
            dialogBuilder.setTitle("提示");
            dialogBuilder.setMessage("机器已重启 " + cnt + " 次");
            tv01.setText("机器将在10s后重启，如果需要停止重启机器，请点击停止按钮即可");
            dialogBuilder.setView(view);
            dialogBuilder.setCancelable(false);
            dialogBuilder.setPositiveButton("停止", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (timer != null) {
                        timer.cancel();
                        timer = null;
                    }
                    if (task != null) {
                        task.cancel();
                        task = null;
                    }
                }
            });
            alertDialog = dialogBuilder.create();
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            alertDialog.show();
            Looper.loop();
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
