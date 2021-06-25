package com.xiaozhenkeji.flvplayer.network;

import android.util.Log;
import android.view.Surface;

import com.xiaozhenkeji.flvplayer.Decode;
import com.xiaozhenkeji.flvplayer.h264.H264SPSPaser;
import com.xiaozhenkeji.flvplayer.h264.SpsFrame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HttpNet {


    public static final int ERROR_ANALYSIS = 3;
    public static final int ERROR_NET = 4;
    public static final int ERROR_STREAM_END = 1;
    public static final int ERROR_EXCEPTION = 2;
    public static final int ERROR_NET_EXCEPTION = 2;
    public static final int RUN_NORMAL = 0;


    private HttpThread thread;
    private boolean isStar;


    private String urlStr;

    private CallBack callBack;
    private Decode flvDecode;
    private Surface surface;

    public HttpNet(String url, Surface surface) throws IOException {
        isStar = true;
        this.urlStr = url;

        this.surface = surface;


        thread = new HttpThread();
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();

    }


    public boolean isStar() {
        return isStar;
    }

    private void initDecode(int width, int height) throws IOException {
        if (flvDecode == null)
            flvDecode = new Decode(surface, width, height);
    }

    long timetest;

    class HttpThread extends Thread {

        @Override
        public void run() {
            super.run();
            isStar = true;

            try {
                if (HttpNet.this.callBack != null) {
                    HttpNet.this.callBack.onStatus(RUN_NORMAL, "创建HTTP");
                }
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setChunkedStreamingMode(0);
                if (HttpNet.this.callBack != null) {
                    HttpNet.this.callBack.onStatus(RUN_NORMAL, "Http创建成功");
                }
                conn.setRequestMethod("GET");
                // 添加 HTTP HEAD 中的一些参数，可参考《Java 核心技术 卷II》
                conn.setRequestProperty("Connection", "Keep-Alive");

                // 设置连接超时时间
                conn.setConnectTimeout(60 * 1000);

                // 设置读取超时时间
                conn.setReadTimeout(60 * 1000);

                if (HttpNet.this.callBack != null) {
                    HttpNet.this.callBack.onStatus(RUN_NORMAL, "Http准备连接");
                }
                conn.connect();


                int resCode = conn.getResponseCode();

                if (resCode == HttpURLConnection.HTTP_OK) {

                    if (HttpNet.this.callBack != null) {
                        HttpNet.this.callBack.onStatus(RUN_NORMAL, "HTTP连接成功");
                    }
                    InputStream is = conn.getInputStream();
                    byte[] b = new byte[1024];
                    int len = 0;
                    if (HttpNet.this.callBack != null) {
                        HttpNet.this.callBack.onStatus(RUN_NORMAL, "准备读取数据");
                    }
                    while ((len = is.read(b)) != -1) {
//                        Log.e("timetest","julishangyizhen:"+(System.currentTimeMillis() - timetest));
                        timetest = System.currentTimeMillis();
                        if (!isStar) {
                            if (HttpNet.this.callBack != null) {
                                HttpNet.this.callBack.onFinsh();
                            }
                            break;
                        }

//                        Log.e("lksdflkjsdf", toHexString1(b, len));
//
//                        Log.e("timetest","laliu:"+(System.currentTimeMillis() - timetest));
                        split(Arrays.copyOfRange(b, 0, len));

                    }
                    isStar = false;
                    if (HttpNet.this.callBack != null) {
                        HttpNet.this.callBack.onStatus(ERROR_STREAM_END, "流数据结束");
                        HttpNet.this.callBack.onError(ERROR_STREAM_END);

                    }


                } else {
//                    Log.e("statusaaa", "444444444");
                    if (HttpNet.this.callBack != null) {
                        HttpNet.this.callBack.onStatus(ERROR_NET, "HTTP连接失败");
                        HttpNet.this.callBack.onError(ERROR_NET);

                    }
                }

            } catch (Exception e) {


                if (HttpNet.this.callBack != null) {
                    HttpNet.this.callBack.onStatus(ERROR_NET_EXCEPTION, "网络连接异常" + e.getMessage());
                    HttpNet.this.callBack.onError(ERROR_NET_EXCEPTION);
                }
                isStar = false;
            }

        }
    }


    private ByteArrayOutputStream baos;
    //缺省长度
    private int defaultLen;
    private byte[] linshi;

    private synchronized void split(byte[] data) {
//        if (data[0] == 8)
//            Log.e("aanalysis11", defaultLen + "   " + toHexString1(data, 100));
//        else if (data[0] == 9) {
//            Log.e("aanalysis22", defaultLen + "   " + toHexString1(data, 100));
//
//        }

        if (defaultLen > 100000) {
            isStar = false;
            if (this.callBack != null) {
                this.callBack.onStatus(ERROR_ANALYSIS, "当前解析失败：");
                this.callBack.onError(ERROR_ANALYSIS);
            }
            return;
        }
        if (baos == null) {
            baos = new ByteArrayOutputStream();
        }
        try {
            if (defaultLen == -10 && linshi != null) {
                defaultLen = 0;
                byte[] data1 = data;
                data = new byte[data1.length + linshi.length];
                System.arraycopy(linshi, 0, data, 0, linshi.length);
                System.arraycopy(data1, 0, data, linshi.length, data1.length);
                linshi = null;
            }


            if (defaultLen != 0) {
                if (defaultLen >= 0) {
                    if (data.length >= defaultLen) {
                        int len = defaultLen;
                        baos.write(data, 0, defaultLen);
                        baos.flush();
                        defaultLen = 0;
                        analysis(baos.toByteArray());
                        baos.reset();


                        //完整帧结束
//                        if (len != data.length)
//                            split(Arrays.copyOfRange(data, len, data.length));
                        if (len < data.length - 4) {
                            defaultLen = 0;
                            split(Arrays.copyOfRange(data, len + 4, data.length));

                        } else {
                            defaultLen = data.length - len - 4;
//                            defaultLen = len - data.length - 4;
                        }


                        return;
                    } else {
                        baos.write(data, 0, data.length);
                        baos.flush();
                        defaultLen = defaultLen - data.length;

                    }
                } else {
                    if (Math.abs(defaultLen) >= data.length) {
                        defaultLen += data.length;
                    } else {
                        int leng = defaultLen;
                        defaultLen = 0;
                        split(Arrays.copyOfRange(data, Math.abs(leng), data.length));
                    }
                    return;
                }
            } else {
                if (data.length < 4) {
                    linshi = data;
                    defaultLen = -10;
                    return;
                }
                if (data[0] == 70 && data[1] == 76 && data[2] == 86) {
                    split(Arrays.copyOfRange(data, 13, data.length));
                    return;
                }
//                if (data[0] == 8 | data[0] == 9) {
//
//                } else {
//                    Log.e("yanzheng", toHexString1(data, 100));
//
//                }

                int len = byte3ToInteger(Arrays.copyOfRange(data, 1, 4)) + 11;
                if (data.length >= len) {
                    baos.write(data, 0, len);
                    baos.flush();
                    analysis(baos.toByteArray());
                    baos.reset();
                    //完整帧结束
                    if (data.length - len - 4 > 0) {
                        split(Arrays.copyOfRange(data, len + 4, data.length));
                    } else {
                        defaultLen = data.length - len - 4;
                    }
                } else {
                    baos.write(data, 0, data.length);
                    baos.flush();
                    defaultLen = len - data.length;

                }
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();

            Log.e("shipinshujuyic ", e.getMessage()+"  \n "+ e.getLocalizedMessage());
            if (HttpNet.this.callBack != null) {
                HttpNet.this.callBack.onStatus(ERROR_EXCEPTION, "视频数据异常");
                if (HttpNet.this.callBack != null)
                    HttpNet.this.callBack.onError(ERROR_EXCEPTION);
            }
        }
    }



    private void analysis(byte[] data) {

//        Log.e("analysisjk", toHexString1(data, 50));
        long time = byte4TimeToInteger(Arrays.copyOfRange(data, 4, 8));


        if (data[0] == 9) {
            if (data[12] == 0) {
                //首帧


                if (data[11] == 23) {
                    int spsLen = byte2ToInteger(Arrays.copyOfRange(data, 22, 24));

                    byte[] sps = new byte[spsLen + 4];
                    sps[3] = 1;
                    System.arraycopy(data, 24, sps, 4, spsLen);
                    int ppsLen = byte2ToInteger(Arrays.copyOfRange(data, 25 + spsLen, 27 + spsLen));

                    H264SPSPaser h264SPSPaser = new H264SPSPaser();
                    SpsFrame spsFrame = new SpsFrame().getSpsFrame(Arrays.copyOfRange(data, 24, 24 + spsLen));
                    int width = h264SPSPaser.getWidth(spsFrame);
                    int height = h264SPSPaser.getHeight(spsFrame);
                    if (this.callBack != null) {
                        this.callBack.onWidthHeight(width, height);
                        HttpNet.this.callBack.onStatus(RUN_NORMAL, "获取视频宽高成功");
                        HttpNet.this.callBack.onStatus(RUN_NORMAL, "即将播放");
                    }
//                    Log.e("widthheight",spsFrame.toString());
                    try {
                        initDecode(width, height);
                    } catch (IOException e) {
                        if (HttpNet.this.callBack != null) {
                            HttpNet.this.callBack.onStatus(ERROR_EXCEPTION, "播放器创建异常");
                            HttpNet.this.callBack.onError(ERROR_EXCEPTION);
                        }
                    }
                    byte[] pps = new byte[ppsLen + 4];
                    pps[3] = 1;
                    System.arraycopy(data, 27 + spsLen, pps, 4, ppsLen);
//                    flvDecode.onVideoFrame(sps, time,0 ,sps.length);
//                    flvDecode.onVideoFrame(pps, time,0, pps.length);
                    flvDecode.putVideoData(sps, time);
                    flvDecode.putVideoData(pps, time);
                }

            } else {
                //次帧
//                if(data[11] == 39){
//                    // P帧
//                }else if(data[11] == 23){
//                    //关键帧
//                }
                int naluLen = byte4ToInteger(Arrays.copyOfRange(data, 16, 20));
                byte[] body = new byte[naluLen + 4];
                body[3] = 1;
                System.arraycopy(data, 20, body, 4, naluLen);
//                flvDecode.onVideoFrame(body, time,0, body.length);
                flvDecode.putVideoData(body, time);
            }
        } else if (data[0] == 8) {


//            音频信息在data[11]里
//            if (data[12] == 0) {
//                //首帧
//                flvDecode.putAudioData(Arrays.copyOfRange(data, 13, data.length), time);
//
//            } else {
//                //次帧
//                flvDecode.putAudioData(Arrays.copyOfRange(data, 13, data.length), time);
//            }


        }

//        Log.e("timetest","jiexi:"+(System.currentTimeMillis() - timetest));
//        Log.e("timetest","-------------------------");
    }

    public static int byte4TimeToInteger(byte[] value) {
        return ((value[3] & 0xff) << 24) + ((value[0] & 0xff) << 16) + ((value[1] & 0xff) << 8) + (value[2] & 0xff);
    }

    public static int byte3ToInteger(byte[] value) {
        return ((value[0] & 0xff) << 16) + ((value[1] & 0xff) << 8) + (value[2] & 0xff);
    }

    public static int byte4ToInteger(byte[] value) {
        return ((value[0] & 0xff) << 24) + ((value[1] & 0xff) << 16) + ((value[2] & 0xff) << 8) + (value[3] & 0xff);
    }

    public static int byte2ToInteger(byte[] value) {
        return ((value[0] & 0xff) << 8) + (value[1] & 0xff);
    }


    public void release() {

        if (flvDecode != null)
            flvDecode.stopCodec();
        isStar = false;

        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        if (this.callBack != null) {
            this.callBack.onFinsh();
            this.callBack = null;
        }


    }


    public static String toHexString1(byte[] b) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < b.length; ++i) {
            buffer.append(toHexString1(b[i]));
        }
        return buffer.toString();
    }

    public static String toHexString40len(byte[] b) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < (b.length > 40 ? 40 : b.length); ++i) {
            buffer.append(toHexString1(b[i]));
        }
        return buffer.toString();
    }

    public static String toHexString6len(byte[] b) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < (b.length > 6 ? 6 : b.length); ++i) {
            buffer.append(toHexString1(b[i]));
        }
        return buffer.toString();
    }

    public static String toHexString1(byte[] b, int len) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < (b.length >= len ? len : b.length); ++i) {
            buffer.append(toHexString1(b[i]));
        }
        return buffer.toString();
    }

    public static String toHexString1(byte b) {
        String s = Integer.toHexString(b & 0xFF);
        if (s.length() == 1) {
            return "0" + s;
        } else {
            return s;
        }
    }


    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }

    public interface CallBack {
        void onError(int errorCode);

        void onStatus(int statusCode, String msg);

        void onWidthHeight(int width, int height);

        void onFinsh();
    }
}
