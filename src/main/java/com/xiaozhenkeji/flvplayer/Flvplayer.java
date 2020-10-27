package com.xiaozhenkeji.flvplayer;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.Surface;

import com.xiaozhenkeji.flvplayer.network.HttpNet;

import java.io.IOException;
import java.nio.ByteBuffer;


public class Flvplayer {

    private  HttpNet httpNet;

    private HttpNet.CallBack callBack;
    private Surface surface;
    private SurfaceTexture surfaceTexture;

    public Flvplayer(){

    }


    public boolean getHttpNet(){
        return httpNet.isStar();
    }

    public void connect(String url, SurfaceTexture surfaceTexture){
        try {
            this.surfaceTexture = surfaceTexture;

            surface = new Surface(surfaceTexture);
            httpNet = new HttpNet(url,surface);
            if(this.callBack!=null){
                httpNet.setCallBack(this.callBack);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void reset(String url){
        if(httpNet!=null&&httpNet.isStar()){
            return;
        }
        if(httpNet!=null){
            httpNet.release();
        }
        if(surface!=null){
            surface.release();
        }
        surface = new Surface(surfaceTexture);
        try {
            httpNet = new HttpNet(url,surface);
            if(this.callBack!=null){
                httpNet.setCallBack(this.callBack);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setCallBack(HttpNet.CallBack callBack) {
        this.callBack = callBack;
        if(httpNet!=null){
            httpNet.setCallBack(callBack);
        }
    }

    public void onDestory(){
        if(httpNet!=null){
            httpNet.release();
        }
        if(surface!=null){
            surface.release();
        }
    }




}
