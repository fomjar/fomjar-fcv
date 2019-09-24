package com.fomjar.fcv.core;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FCV {

    private static final ExecutorService pool = Executors.newCachedThreadPool();

    private boolean isRun;
    private FrameGrabber grabber;
    private FrameRecorder recorder;

    public FCV() {
        this.isRun = false;
    }

    public FCV rtsp2rtmp(String rtsp, String rtmp) throws FrameGrabber.Exception {
        this.grabber = new FFmpegFrameGrabber(rtsp);
        this.grabber.setOption("rtsp_transport", "tcp");
        this.grabber.setTimestamp(0);
//        this.grabber.setOption("stimeout", String.valueOf(1000000L * 10));

        this.recorder = new FFmpegFrameRecorder(rtmp, 2);
        this.recorder.setInterleaved(true);
        this.recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        this.recorder.setFormat("flv");  // rtmp
        this.recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);

        return this;
    }

    private void prepare() {
        this.recorder.setAudioChannels(this.grabber.getAudioChannels());
        this.recorder.setFrameRate(this.grabber.getFrameRate());
        this.recorder.setImageWidth(this.grabber.getImageWidth());
        this.recorder.setImageHeight(this.grabber.getImageHeight());
    }

    public void start() {
        if (this.isRun) return;
        if (null == this.grabber || null == this.recorder)
            throw new IllegalStateException("Action not initialized!");

        FCV.pool.submit(() -> {
            try {
                this.isRun = true;

                this.grabber.start();
                this.prepare();
                this.recorder.start();

                while (this.isRun) {
                    Frame frame = this.grabber.grab();
                    if (null != frame && this.isRun)
                        this.recorder.record(frame);
                }
            } catch (FrameGrabber.Exception | FrameRecorder.Exception e) {
                e.printStackTrace();
            } finally {
                try {this.grabber.close();}
                catch (FrameGrabber.Exception e) {e.printStackTrace();}
                try {this.recorder.close();}
                catch (FrameRecorder.Exception e) {e.printStackTrace();}
            }
        });
    }

    public void stop() {
        if (!this.isRun) return;

        this.isRun = false;
    }

}
