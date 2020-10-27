package com.xiaozhenkeji.flvplayer;

public class DataBean {

    private long time;
    private byte[] data;

    public DataBean(long time, byte[] data) {
        this.time = time;
        this.data = data;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
