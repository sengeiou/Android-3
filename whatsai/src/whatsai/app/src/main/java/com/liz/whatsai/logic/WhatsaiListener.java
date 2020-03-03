package com.liz.whatsai.logic;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.liz.androidutils.AudioUtils;
import com.liz.androidutils.LogUtils;
import com.liz.androidutils.NumUtils;
import com.liz.androidutils.TimeUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("WeakerAccess")
public class WhatsaiListener {

    /**
     * default max audio power value(from pcm 16bits)
     */
    private static final int DEFAULT_MAX_POWER = 32768;

    /**
     * buffer size for audio power data, unit by byte
     * for pcm16/44100, 1MB is about 10s for pcm16/44100
     * when buffer is full, drop old frame for new frame
     * listening will continue
     * all audio data will save to audio pcm file
     */
    public static final int DEFAULT_MAX_BUFFER_SIZE = 1024*1024;

    @SuppressWarnings("unused")
    public static final int AUDIO_SAMPLE_RATE_8K  =  8000;
    @SuppressWarnings("unused")
    public static final int AUDIO_SAMPLE_RATE_16K = 16000;
    @SuppressWarnings("unused")
    public static final int AUDIO_SAMPLE_RATE_32K = 32000;
    @SuppressWarnings("unused")
    public static final int AUDIO_SAMPLE_RATE_44K = 44100;
    @SuppressWarnings("unused")
    public static final int AUDIO_SAMPLE_RATE_48K = 48000;
    public static final int DEFAULT_AUDIO_SAMPLE_RATE = AUDIO_SAMPLE_RATE_44K;

    // sample rate from all wave sample data for wave data showing
    public static final int DEFAULT_WAVE_SAMPLING_RATE = 100;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Interface Functions

    public WhatsaiListener() {
        LogUtils.d("WhatsaiListener:WhatsaiListener");
        mAudioBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, mAudioFormat);
        LogUtils.d("WhatsaiListener:WhatsaiListener: mAudioBufferSize = " + mAudioBufferSize);
        mAudioRecord = new AudioRecord(mAudioSource, mSampleRate, mChannelConfig, mAudioFormat, mAudioBufferSize);
    }

    public boolean isListening() {
        return this.mIsListening;
    }

    public void switchListening() {
        LogUtils.d("WhatsaiListener:switchListening: mIsListening = " + mIsListening);
        if (mIsListening) {
            stopListening();
            stopWorkingTimer();
        }
        else {
            startListening();
            startWorkingTimer();
        }
    }

    public void playAudio() {
        playAudio(getPCMFileAbsolute());
    }

    public void playAudio(String fileAbsolute) {
        AudioUtils.playPCM(fileAbsolute, mAudioBufferSize, mSampleRate, mAudioFormat, mChannelConfig);
        //##@: AudioUtils.playPCM16(mPowerList, mAudioBufferSize, mSampleRate, mChannelConfig);
        /*
        //save to wav file
        LogUtils.d("Convert pcm file to wave...");
        AudioUtils.pcmToWave(mPCMFileName, mPCMFileName + ".wav",
                mSampleRate, mAudioBufferSize, mAudioFormat, AudioUtils.AUDIO_TRACK_SINGLE);
        //*/
    }
    @SuppressWarnings("unused")
    public String getAudioConfigInfoFull() {
        String configInfo = "Audio Source: <font color=\"#ff0000\">" + AudioUtils.audioSourceName(MediaRecorder.AudioSource.MIC) + "</font>";
        configInfo += "<br>Sample Rate(Hz): <font color=\"#ff0000\">" + mSampleRate + "</font>";
        configInfo += "<br>Audio Format: <font color=\"#ff0000\">" + AudioUtils.audioFormatName(mAudioFormat) + "</font>";
        configInfo += "<br>Channel Config: <font color=\"#ff0000\">" + AudioUtils.channelConfigName(mChannelConfig) + "</font>";
        configInfo += "<br>Audio Buffer Size(B): <font color=\"#ff0000\">" + mAudioBufferSize + "</font>";
        configInfo += "<br>Audio Path: <font color=\"#ff0000\">" + ComDef.WHATSAI_AUDIO_DIR + "</font>";
        return configInfo;
    }

    public String getAudioConfigInfo() {
        String configInfo = "<font color=\"#ff0000\">" + AudioUtils.audioSourceName(MediaRecorder.AudioSource.MIC) + "</font>";
        configInfo += " | <font color=\"#ff0000\">" + mSampleRate + "</font>";
        configInfo += " | <font color=\"#ff0000\">" + AudioUtils.audioFormatName(mAudioFormat) + "</font>";
        configInfo += " | <font color=\"#ff0000\">" + AudioUtils.channelConfigName(mChannelConfig) + "</font>";
        configInfo += " | <font color=\"#ff0000\">" + mAudioBufferSize + "</font>";
        return configInfo;
    }

    public String getProgressInfo() {
        String info = "";
        info += " <font color=\"#ff0000\">" + mPCMFileName + "</font>";
        info += " | <font color=\"#ff0000\">" + mTimeElapsed + "</font>";
        info += " | <font color=\"#ff0000\">" + NumUtils.formatSize(mTotalSize) + "</font>";
        info += " | FP: <font color=\"#ff0000\">" + mFramePower + "</font>";
        info += " | FS: <font color=\"#ff0000\">" + mFrameSize + "</font>";
        info += " | FC: <font color=\"#ff0000\">" + mFrameCount + "</font>";
        info += " | FR: <font color=\"#ff0000\">" + mFrameRate + "</font>";
        info += " | DR: <font color=\"#ff0000\">" + NumUtils.formatSize(mDataRate) + "</font>";
        info += " | <font color=\"#ff0000\">" + mWaveSamplingRate + "</font>";
        info += " | <font color=\"#ff0000\">" + mPowerList.size() + "</font>";
        return info;
    }

    public String getSpeechText() {
        return mSpeechText;
    }

    public String getPCMFileAbsolute() {
        return ComDef.WHATSAI_AUDIO_DIR + "/" + mPCMFileName;
    }

    public String getPCMFileName() {
        return mPCMFileName;
    }

    public interface ListenerCallback {
        void onPowerUpdated();
    }

    public void setCallback(ListenerCallback callback) {
        mCallback = callback;
    }

    @SuppressWarnings("unused")
    public int getPowerListSize() {
        if (this.mPowerList == null) {
            return -1;
        }
        else {
            return this.mPowerList.size();
        }
    }

    @SuppressWarnings("unused")
    public double getLastPower() {
        if (this.mPowerList == null || this.mPowerList.isEmpty()){
            return -1;
        }
        else {
            return this.mPowerList.get(this.mPowerList.size() - 1);
        }
    }

    public List<Integer> getPowerList() {
        return this.mPowerList;
    }

    public int getMaxPower() {
        return mMaxPower;
    }
    public void setMaxPower(int maxPower) {
        mMaxPower = maxPower;
    }

    public int getMaxBufferSize() {
        return mMaxBufferSize;
    }
    public void setMaxBufferSize(int maxSize) {
        mAudioBufferSize = maxSize;
    }

    public int getWaveSamplingRate() {
        return mWaveSamplingRate;
    }
    public void setWaveSamplingRate(int samplingRate) {
        mWaveSamplingRate = samplingRate;
    }

    public void setVoiceRecognition(boolean recognition) {
        mVoiceRecognition = recognition;
    }

    // Interface Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private AudioRecord mAudioRecord;
    private boolean mIsListening = false;

    private int mMaxPower = DEFAULT_MAX_POWER;
    private int mMaxBufferSize = DEFAULT_MAX_BUFFER_SIZE;

    private int mAudioSource = MediaRecorder.AudioSource.MIC;
    private int mSampleRate = DEFAULT_AUDIO_SAMPLE_RATE;
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int mChannelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int mAudioBufferSize;  // unit by byte

    /**
     * sampling rate for wave showing, i.e. 100 means pick up one from 100 power data
     */
    private int mWaveSamplingRate = DEFAULT_WAVE_SAMPLING_RATE;

    private String mPCMFileName = "";
    private int mFrameSize = 0;
    private int mFrameCount = 0;
    private int mTotalSize = 0;
    private int mLastFrameCount = 0;
    private int mLastDataSize = 0;
    private long mStartTime = 0;
    private int mFrameRate = 0;
    private int mFramePower = 0;
    private int mDataRate = 0;  // unit by b/s(bit/s)
    private String mTimeElapsed = "";  // format as hh:mm:ss
    private ArrayList<Integer> mPowerList = new ArrayList<>();
    private ArrayList<AudioFrame> mFrameList = new ArrayList<>();
    private ArrayList<AudioTemplate> mTemplateList = new ArrayList<>();

    private ListenerCallback mCallback = null;

    private boolean mVoiceRecognition = false;
    private final Object mRecognitionObject = new Object();
    private String mSpeechText = "";

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Working Timer
    private static final long WORKING_TIMER_DELAY = 0L;
    private static final long WORKING_TIMER_PERIOD = 1000L;

    private Timer mWorkingTimer;

    private void startWorkingTimer() {
        mWorkingTimer = new Timer();
        mWorkingTimer.schedule(new TimerTask() {
            public void run () {
                onWorkingTimer();
            }
        }, WORKING_TIMER_DELAY, WORKING_TIMER_PERIOD);
    }

    private void stopWorkingTimer() {
        if (mWorkingTimer != null) {
            mWorkingTimer.cancel();
            mWorkingTimer = null;
        }
    }

    // Working Timer
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void onWorkingTimer() {
        LogUtils.d("WhatsaiListener:onWorkingTimer");
        mFrameRate = mFrameCount - mLastFrameCount;
        mLastFrameCount = mFrameCount;
        mDataRate = mTotalSize - mLastDataSize;
        mLastDataSize = mTotalSize;
        mTimeElapsed = TimeUtils.elapsed(mStartTime);
    }

    private void resetParams() {
        LogUtils.d("WhatsaiListener:onStartWorkingTimer");
        mPCMFileName = "";
        mFrameSize = 0;
        mFrameCount = 0;
        mTotalSize = 0;
        mLastFrameCount = 0;
        mLastDataSize = 0;
        mStartTime = 0;
        mFrameRate = 0;
        mFramePower = 0;
        mDataRate = 0;
        mTimeElapsed = "00:00:00";
        mPowerList.clear();
        mFrameList.clear();
        mTemplateList.clear();
    }

    private void startListening() {
        LogUtils.d("WhatsaiListener:startListening: mIsListening = " + mIsListening);
        if (mIsListening) {
            LogUtils.d("WhatsaiListener:startListening: already started");
            return;
        }
        mIsListening = true;

        resetParams();

        final FileOutputStream outputStream = getPCMOutputStream();
        if (outputStream == null) {
            LogUtils.e("WhatsaiListener:startListening: get output stream for audio buffer failed.");
            return;
        }

        mAudioRecord.startRecording();
        mStartTime = System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    LogUtils.d("WhatsaiListener: Start loop write audio data to pcm file...");
                    byte[] audioData = new byte[mAudioBufferSize];
                    int readSize;
                    while (mIsListening) {
                        readSize = mAudioRecord.read(audioData, 0, audioData.length);
                        onReadBuffer(readSize, audioData);
                        outputStream.write(audioData, 0, readSize);
                        outputStream.flush();
                        if (mVoiceRecognition) {
                            synchronized (mRecognitionObject) {
                                mRecognitionObject.notify();
                            }
                        }
                    }
                    outputStream.close();
                } catch (Exception e) {
                    LogUtils.e("WhatsaiListener: startListening: listen thread exception " + e.toString());
                    e.printStackTrace();
                }
                LogUtils.d("WhatsaiListener: listener stop.");
            }
        }).start();

        if (mVoiceRecognition) {
            if (mTemplateList.isEmpty()) {
                if (!loadAudioTemplates()) {
                    LogUtils.e("WhatsaiListener: load audio templates failed.");
                    return;
                }
            }
            LogUtils.d("WhatsaiListener: load audio templates ok, size = " + mTemplateList.size());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        LogUtils.d("WhatsaiListener: Start voice recognition...");
                        while (mIsListening) {
                            LogUtils.d("WhatsaiListener: wait recognition...");
                            synchronized (mRecognitionObject) {
                                mRecognitionObject.wait();
                            }
                            LogUtils.d("WhatsaiListener: recognition on frame #" + mFrameCount);
                            onVoiceRecognition();
                        }
                    } catch (Exception e) {
                        LogUtils.e("WhatsaiListener: startListening: recognition exception: " + e.toString());
                        e.printStackTrace();
                    }
                    LogUtils.d("WhatsaiListener: voice recognition stop.");
                }
            }).start();
        }
    }

    private void onReadBuffer(final int readSize, byte[] audioData) {
        mFrameSize = readSize / 2;

        // sample for showing
        for (int i = 0; i < mFrameSize; i += mWaveSamplingRate) {
            mPowerList.add(audioData[i * 2 + 1] << 8 | audioData[i * 2]);
            /*
            // audio zoom
            int pcmPower = (audioData[i * 2 + 1] << 8 | audioData[i * 2]);
            pcmPower *= 2;
            if (pcmPower > DEFAULT_MAX_POWER) {
                pcmPower = DEFAULT_MAX_POWER;
            }
            audioData[i * 2] = (byte)(pcmPower & 0x000000ff);
            audioData[i * 2 + 1] = (byte)((pcmPower & 0x0000ff00) >> 8);
            //*/
        }

        // calculate frame power, for pcm16, combined two bytes into one short
        {
            int pcmVal;
            int valid = 0;
            double pcmSum = 0;
            for (int i = 0; i < mFrameSize; i++) {
                pcmVal = (audioData[i*2 + 1] << 8 | audioData[i*2]);
                if (pcmVal != 0) {
                    pcmSum += pcmVal * pcmVal;
                    valid ++;
                }
            }
            pcmSum /= valid;
            mFramePower = (int)Math.sqrt(pcmSum);
        }

        mTotalSize += readSize;
        mFrameCount ++;
        LogUtils.v("WhatsaiListener: read #" + mFrameCount + ": " + readSize + "/" + mTotalSize + "/" + mFramePower);

        // save a period of audio buffer for recognition
        if (mVoiceRecognition) {
            AudioFrame frame = new AudioFrame(audioData);
            if (mTotalSize > mMaxBufferSize) {
                mFrameList.remove(0);
            }
            mFrameList.add(frame);
        }

        if (mCallback != null) {
            mCallback.onPowerUpdated();
        }
    }

    public void onVoiceRecognition() {
        //###@: todo: search templates words from frame list...
    }

    private void stopListening() {
        LogUtils.d("WhatsaiListener:stopRecord: mIsListening = " + mIsListening);
        if (!mIsListening) {
            LogUtils.d("WhatsaiListener: stopRecord: already stopped");
            return;
        }
        mIsListening = false;
        mAudioRecord.stop();
    }

    private boolean loadAudioTemplates() {
        //####@:
        return true;
    }

    private FileOutputStream getPCMOutputStream() {
        File pcmFile = createPCMFile();
        if (pcmFile == null) {
            LogUtils.e("WhatsaiListener:getPCMOutputStream: create pcm file failed");
            return null;
        }
        try {
            return new FileOutputStream(pcmFile.getAbsoluteFile());
        }
        catch (Exception e) {
            LogUtils.e("WhatsaiListener:getPCMOutputStream: create output stream exception " + e.toString());
            return null;
        }
    }

    /**
     * create a pcm file to write pcm file
     * @return file which name format as yy.MMdd.HHmmss(19.1103.173655)
     */
    private File createPCMFile() {
        String strFileTime = new SimpleDateFormat("yy.MMdd.HHmmss").format(new java.util.Date());
        String fileName = strFileTime + ".pcm";
        String filePath = ComDef.WHATSAI_AUDIO_DIR + "/" + fileName;
        LogUtils.i("WhatsaiListener:createPCMFile: filePath = " + filePath);

        File objFile = new File(filePath);
        if (!objFile.exists()) {
            try {
                if (!objFile.createNewFile()) {
                    LogUtils.e("WhatsaiListener: createPCMFile failed.");
                    return null;
                }
                mPCMFileName = fileName;
                return objFile;
            } catch (IOException e) {
                LogUtils.e("WhatsaiListener: createPCMFile exception: " + e.toString());
                e.printStackTrace();
            }
        }
        return null;
    }
}