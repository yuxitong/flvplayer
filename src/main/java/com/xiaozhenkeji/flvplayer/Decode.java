package com.xiaozhenkeji.flvplayer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.xiaozhenkeji.flvplayer.network.HttpNet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


public class Decode {
    private final static int TIME_INTERNAL = 5;

    private Surface surface;

    private MediaCodec mediaCodec;


    private List<DataBean> listVideo;
    private List<DataBean> listAudio;
    private List<byte[]> audioPlayer;

    VideoDecodeThread vt;
    AudioDecodeThread at;
    AudioPlayerThread apt;

    private MediaCodec.BufferInfo audioBufferinfo;
    private MediaFormat audioFormat;
    private MediaCodec audioEncodec;
    private AudioTrack mPlayer;

    private boolean star = false;


    private final Object videoLock = new Object();
    private final Object audioLock = new Object();
    private final Object playerLock = new Object();

    private int width;
    private int height;

    int mCount = 0;

    public Decode(Surface surface, int width, int height) throws IOException {
        star = true;
        this.width = width;
        this.height = height;
        this.surface = surface;
        listVideo = new ArrayList<>();
        listAudio = new ArrayList<>();
        audioPlayer = new ArrayList<>();
        initVideo();
        vt = new VideoDecodeThread();
        vt.start();
        at = new AudioDecodeThread();
        at.start();
        apt = new AudioPlayerThread();
        apt.start();
}

    private void initVideo() throws IOException {

        mediaCodec = MediaCodec.createDecoderByType("video/avc");
        //初始化MediaFormat
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc",
                width, height);
        //配置MediaFormat以及需要显示的surface
        mediaCodec.configure(mediaFormat, surface, null, 0);
        mediaCodec.start();
        initAudio();
    }


    private void initAudio() throws IOException {
        audioBufferinfo = new MediaCodec.BufferInfo();
        audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, AudioFormat.ENCODING_PCM_16BIT);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096 * 10);
        audioEncodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        audioEncodec.configure(audioFormat, null, null, 0);
        audioEncodec.start();


        mPlayer = new AudioTrack(AudioManager.STREAM_SYSTEM, 44100, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, 2048, AudioTrack.MODE_STREAM);//


        mPlayer.play();


    }

    public boolean onVideoFrame(byte[] buf, long time, int offset, int length) {
        try {
            // 获取输入buffer index
            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
            //-1表示一直等待；0表示不等待；其他大于0的参数表示等待毫秒数
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                //清空buffer
                inputBuffer.clear();
                //put需要解码的数据
                inputBuffer.put(buf, offset, length);
                //解码mCount * TIME_INTERNAL  time*1000
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, length, time, 0);
                mCount++;

            } else {
                return false;
            }
            // 获取输出buffer index
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            //循环解码，直到数据全部解码完成
            while (outputBufferIndex >= 0) {
                //logger.d("outputBufferIndex = " + outputBufferIndex);
                //true : 将解码的数据显示到surface上
                mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }
            if (outputBufferIndex < 0) {
                //logger.e("outputBufferIndex = " + outputBufferIndex);
            }
        }catch (Exception e){

        }
        return true;
    }


    public void onAudioFrame(byte[] aacData, long time) {
        if (audioBufferinfo == null || audioFormat == null || audioEncodec == null) {
            try {
                initAudio();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (aacData != null && aacData.length != 0) {
            Log.e("aacaacaac",HttpNet.toHexString1(aacData));

            int inputIndex;
            ByteBuffer inputBuffer;
            ByteBuffer outputBuffer;


            inputIndex = audioEncodec.dequeueInputBuffer(0);
            if (inputIndex >= 0) {

                ByteBuffer inputBuf = audioEncodec.getInputBuffer(inputIndex);

                inputBuf.clear();
                inputBuf.put(aacData);
                audioEncodec.queueInputBuffer(inputIndex, 0, aacData.length, time * 1000, 0);
            }


            int outputIndex = audioEncodec.dequeueOutputBuffer(audioBufferinfo, 0);

            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.e("asdfghj","   "+outputIndex);
            } else {
                while (outputIndex >= 0) {
                    try {
                        outputBuffer = audioEncodec.getOutputBuffer(outputIndex);
                        byte[] data = new byte[outputBuffer.remaining()];
                        outputBuffer.get(data, 0, data.length);
                        outputBuffer.position(audioBufferinfo.offset);
//
                        if (audioPlayer != null)
                            audioPlayer.add(data);
                        synchronized (playerLock) {
                            playerLock.notify();
                        }
                    } catch (Exception e) {

                    }
                    audioEncodec.releaseOutputBuffer(outputIndex, false);
                    outputIndex = audioEncodec.dequeueOutputBuffer(audioBufferinfo, 0);
                }
            }
        }

    }


    /**
     * 停止解码，释放解码器
     */
    public void stopCodec() {

        try {
            star = false;

            if (vt != null && vt.isAlive()) {
                vt.interrupt();
            }
            if (at != null && at.isAlive()) {
                at.interrupt();
            }
            if (apt != null && apt.isAlive()) {
                apt.interrupt();
            }
            if (mediaCodec != null) {
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
            }
            if (audioEncodec != null) {
                audioEncodec.stop();
                audioEncodec.release();
                audioEncodec = null;
            }
            if (mPlayer != null) {
                mPlayer.release();
            }

            if (listVideo != null) {
                listVideo.clear();
            }
            if (listAudio != null) {
                listAudio.clear();
            }
            if (audioPlayer != null) {
                audioPlayer.clear();
            }


        } catch (Exception e) {
            e.printStackTrace();
            if (mediaCodec != null) {

                mediaCodec = null;
            }
        }
    }


    public void putVideoData(byte[] data, long time) {
        Log.e("datafps", HttpNet.toHexString1(data,40));

        if (!star) {
            return;
        }
        if (listVideo != null)
            listVideo.add(new DataBean(time, data));
        synchronized (videoLock) {
            videoLock.notify();
        }
    }

    public void putAudioData(byte[] data, long time) {
        if (!star) {
            return;
        }
        if (listAudio != null)
            listAudio.add(new DataBean(time, data));
        synchronized (audioLock) {
            audioLock.notify();
        }
    }


    class VideoDecodeThread extends Thread {


        public VideoDecodeThread() {

            setName("DecodeVideoThread");
        }

        @Override
        public void run() {
            super.run();
            star = true;
            while (true) {

                if (!star) {
                    break;
                }

                if (listVideo != null && listVideo.size() != 0) {
                    DataBean b = listVideo.remove(0);
                    if (b != null) {
                        onVideoFrame(b.getData(), b.getTime(), 0, b.getData().length);
                    }
                } else {
                    synchronized (videoLock) {
                        try {
                            videoLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }


            }
        }
    }

    class AudioDecodeThread extends Thread {


        public AudioDecodeThread() {

            setName("DecodeAudioThread");
        }

        @Override
        public void run() {
            super.run();
            star = true;
            while (true) {
                if (!star) {
                    break;
                }
                if (listAudio != null && listAudio.size() != 0) {
                    DataBean b = listAudio.remove(0);
                    if (b != null) {
                        onAudioFrame(b.getData(), b.getTime());
                    }
                    if (listAudio.size() > 30) {
                        listAudio.clear();
                    }
                } else {
                    synchronized (audioLock) {
                        try {
                            audioLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }


            }
        }
    }

    class AudioPlayerThread extends Thread {


        public AudioPlayerThread() {

            setName("AudioPlayerThread");
        }

        @Override
        public void run() {
            super.run();
            star = true;
            while (true) {
                if (!star) {
                    break;
                }
                if (audioPlayer != null && audioPlayer.size() != 0) {
                    byte[] b = audioPlayer.remove(0);

                    if (mPlayer != null && b != null) {
                        mPlayer.write(b, 0, b.length);
                        if (audioPlayer.size() > 30) {
                            audioPlayer.clear();
                        }
                    }
                } else {
                    synchronized (playerLock) {
                        try {
                            playerLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }


            }
        }
    }


}
