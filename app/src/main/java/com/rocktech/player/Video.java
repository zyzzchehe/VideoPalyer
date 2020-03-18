package com.rocktech.player;

import java.io.Serializable;

public class Video implements Serializable {
	  
    private static final long serialVersionUID = 1L;  
      
    private String mVideoPath = null;  
    private String mVideoName = null;  
    private boolean mChecked  = false;    //保存复选框的状态 
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	public String getVideoPath() {
		return mVideoPath;
	}
	public String getVideoName() {
		return mVideoName;
	}
	public boolean isChecked() {
		return mChecked;
	}
	public void setVideoPath(String videoPath) {
		this.mVideoPath = videoPath;
	}
	public void setVideoName(String videoName) {
		this.mVideoName = videoName;
	}
	public void setChecked(boolean checked) {
		this.mChecked = checked;
	}
    
}
