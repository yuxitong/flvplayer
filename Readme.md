



这个是专门用来给android播放flv的播放器。他是基于Mediacodec的 只能解析FLV包含H264+AAC的音视频



使用方法：




        if (flv == null) {
            flv = new Flvplayer();

            flv.setCallBack(new HttpNet.CallBack() {
                @Override
                public void onError(int errorCode) {
                    runUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(VideoPlayerActivity.this.getApplicationContext(), "网络信号不好或视频已经播完", Toast.LENGTH_SHORT).show();
                            VideoPlayerActivity.this.finish();
                        }
                    });
                }

                @Override
                public void onStatus(int statusCode, String msg) {
                    Log.e("flvflv", "msg:" + msg);
                }

                @Override
                public void onWidthHeight(int width, int height) {

                }

                @Override
                public void onFinsh() {

                }
            });
        }

        flv.connect(url, surfaceTexture);