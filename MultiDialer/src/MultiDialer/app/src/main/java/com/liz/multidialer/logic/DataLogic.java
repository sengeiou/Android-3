package com.liz.multidialer.logic;

import android.content.Context;
import android.graphics.Color;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.liz.androidutils.FileUtils;
import com.liz.androidutils.ImageUtils;
import com.liz.androidutils.LogUtils;
import com.liz.androidutils.TelUtils;
import com.liz.androidutils.TimeUtils;
import com.liz.androidutils.ZipUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * DataLogic:
 * Created by liz on 2018/3/8.
 */

@SuppressWarnings({"unused", "WeakerAccess"})
public class DataLogic extends MultiDialClient {

    private static ArrayList<String> mTelList = null;
    private static String mTelListFileName = "";
    private static int mCurrentCallIndex = 0;
    private static String mPicturePath = "";
    private static long mEndCallDelay = ComDef.DEFAULT_END_CALL_DELAY;
    private static int mTotalCalledNum = 0;
    private static boolean mWorking = false;
    private static boolean mDialing = false;

    public static void init() {
        LogUtils.d("DataLogic: init");

        touchDirs();
        loadSettings();
        if (loadTelList()) {
            //#####@: todo: check if picture path already exists
            //should read from preferences to check if picture path exists
            //if (mCurrentCallIndex)
            //    genPicturePathForTelList(fileName);
        }
        else {
            MultiDialClient.fetchTelListFile();
        }

        /*
        //##@: test
        new Thread() {
            @Override
            public void run() {
                SFTPManager sftpMgr = new SFTPManager(getServerAddress(), getServerPort(), getUserName(), getPassword());
                sftpMgr.connect();
                sftpMgr.exec("mv /home/shandong1/PUB_SPACE/NUM_DATA/WAIT_DATA/M01_000001.txt /home/shandong1/PUB_SPACE/NUM_DATA/RUN_DATA/M01_000001.txt");
            }
        }.start();
        //*/
    }

    private static void touchDirs() {
        if (!FileUtils.touchDir(ComDef.DIALER_DIR)) {
            LogUtils.e("ERROR: touch dir \"" + ComDef.DIALER_DIR + "\" failed.");
        }
        if (!FileUtils.touchDir(ComDef.DIALER_NUM_DIR)) {
            LogUtils.e("ERROR: touch dir \"" + ComDef.DIALER_NUM_DIR + "\" failed.");
        }
        if (!FileUtils.touchDir(ComDef.DIALER_PIC_DIR)) {
            LogUtils.e("ERROR: touch dir \"" + ComDef.DIALER_PIC_DIR + "\" failed.");
        }
    }

    protected static void loadSettings() {
        MultiDialClient.loadSettings();
        mTelListFileName = Settings.readFileListFile();
        mCurrentCallIndex = Settings.readCurrentCallIndex();
    }

    public static void onTelListFileUpdate(String fileName) {
        setTelListFileName(fileName);
        if (!loadTelList()) {
            showProgress("ERROR: onTelListFileUpdate: load tel list failed by name \"" + fileName + "\"");
        }
        else {
            setCurrentCallIndex(0);
            genPicturePathForTelList(fileName);
        }
    }

    public static boolean loadTelList() {
        String filePath = getTelListFilePath();
        if (!FileUtils.isExists(filePath)) {
            showProgress("ERROR: loadTelList: tel list file not exist by name \"" + filePath + "\"");
            return false;
        }

        mTelList = FileUtils.readTxtFileLines(filePath);
        if (!checkTelList()) {
            showProgress("ERROR: loadTelList: read tel list from file \"" + filePath + "\" failed.");
            return false;
        }

        showProgress("loadTelList: load success, size = " + mTelList.size());
        return true;
    }

    public static void clearTelListFile() {
        setTelListFileName("");
        if (mTelList != null) {
            mTelList.clear();
        }
        setCurrentCallIndex(0);
    }

    private static String getTelListFilePath() {
        return ComDef.DIALER_NUM_DIR + "/" + mTelListFileName;
    }

    private static boolean genPicturePathForTelList(String telListFileName) {
        String strDateTime = new SimpleDateFormat("yyMMdd_HHmmss").format(new Date(System.currentTimeMillis()));
        String picPath = ComDef.DIALER_PIC_DIR + "/" + FileUtils.getFileNeatName(telListFileName) + "_" + strDateTime;
        if (FileUtils.touchDir(picPath)) {
            mPicturePath = picPath;
            showProgress("genPicturePathForTelList: generate picture path \"" + mPicturePath + "\"");

            //####@: todo: should save picture path for app exit
            //because we add time on the pic path

            return true;
        }
        else {
            mPicturePath = null;
            showProgress("ERROR: genPicturePathForTelList: touch dir failed for \"" + picPath + "\"");
            return false;
        }
    }

    private static boolean checkTelList() {
        if (mTelList == null) {
            showProgress("ERROR: mTelList null");
            return false;
        }
        if (mTelList.size() == 0) {
            showProgress("ERROR: mTelList empty");
            return false;
        }
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // UI Progress Info Callback

    public interface ShowProgressCallback {
        void onShowProgress(String msg);
    }

    private static ShowProgressCallback mProgressCallback;
    public static void setProgressCallback(ShowProgressCallback callback) {
        mProgressCallback = callback;
    }

    public static void showProgress(String msg) {
        if (ComDef.DEBUG && mProgressCallback != null) {
            mProgressCallback.onShowProgress(msg);
        }
        else {
            //not show log on ui, only log by android
            LogUtils.i(msg);
        }
    }

    // UI Progress Info Callback
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public static String getTelListFileInfo() {
        if (TextUtils.isEmpty(mTelListFileName)) {
            return "未知";
        }
        else {
            return mTelListFileName;
        }
    }

    public static String getTelListFileName() { return mTelListFileName; }
    public static void setTelListFileName(String value) { mTelListFileName = value; Settings.saveFileListFile(value); }

    private static long getEndCallDelay() {
        return mEndCallDelay;
    }
    public static void setEndCallDelay(long endCallDelay) {
        mEndCallDelay = endCallDelay;
    }

    public static String getTelListInfo() {
        String telListInfo;
        if (mTelList == null) {
            telListInfo = "ERROR: 没有号码列表文件: " + ComDef.TEL_LIST_FILE_PATH;
        }
        else if (mTelList.size() == 0) {
            telListInfo = "ERROR: 号码列表为空";
        }
        else {
            telListInfo = "电话号码列表总数(" + ComDef.TEL_LIST_FILE_PATH
                    + "):  <font color='#FF0000'>" + mTelList.size() + "</font>";
        }
        return telListInfo;
    }

    public static String getTelListNumInfo() {
        return "" + getTelListNum();
    }

    public static String getCalledNumInfo() {
        return "" + mCurrentCallIndex;
    }

    public static String getFloatingButtonText() {
        if (!mWorking) {
            return "NOT WORKING";
        }
        else {
            if (!mDialing) {
                return "NOT DIALING";
            }
            else {
                String dialInfo = "DIALING "
                        + (mCurrentCallIndex + 1) + "/" + getTelListNum() + ": "
                        + getCurrentTelNumber()
                        + "\n"
                        + "点击暂停";
                return dialInfo;
            }
        }
    }

    public static int getFloatingButtonColor() {
        return Color.RED;
    }

    private static int getTelListNum() {
        if (mTelList == null) {
            LogUtils.e("ERROR: getTelListNum: list null");
            return 0;
        }
        else {
            return mTelList.size();
        }
    }

    private static String getCurrentTelNumber() {
        return getTelNumber(mCurrentCallIndex);
    }

    private static String getTelNumber(int index) {
        LogUtils.d("getTelNumber: index=" + index);
        if (mTelList == null) {
            LogUtils.e("ERROR: getTelNumber: list null");
            return "";
        }
        else if (index < 0 || index >= mTelList.size()) {
            LogUtils.e("ERROR: getTelNumber: invalid tel index = " + index);
            return "";
        }
        else {
            return mTelList.get(index);
        }
    }

    public static boolean isWorking() {
        return mWorking;
    }

    public static boolean isDialing() {
        return mDialing;
    }

    public static void startWorking(Context context) {
        mWorking = true;
        startWorkingTimer(context);
    }

    public static void stopWorking() {
        mWorking = false;
        stopWorkingTimer();
        mDialing = false;
    }

    private static void checkDialing(Context context) {
        LogUtils.d("DataLogic: checkDialing: mDialing = " + mDialing);
        if (!mDialing) {
            if (canDial()) {
                mDialing = true;
                startLoopCallOnNum(context);
            } else {
                MultiDialClient.fetchTelListFile();
            }
        }
    }

    //loopCallOnNum has handler, here using Main looper
    private static void startLoopCallOnNum(final Context context) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                loopCallOnNum(context);
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Daemon Timer

    private static final long WORKING_TIMER_DELAY = 1000L;  //unit by ms
    private static final long WORKING_TIMER_PERIOD = 5000L;  //unit by ms

    private static Timer mWorkingTimer = null;

    private static void startWorkingTimer(final Context context) {
        showProgress("startWorkingTimer");
        mWorkingTimer = new Timer();
        mWorkingTimer.schedule(new TimerTask() {
            public void run() {
                LogUtils.d("DataLogic: startWorkingTimer: ThreadId = " + android.os.Process.myTid());
                checkDialing(context);
            }
        }, WORKING_TIMER_DELAY, WORKING_TIMER_PERIOD);
    }

    private static void stopWorkingTimer() {
        showProgress("stopWorkingTimer");
        if (mWorkingTimer != null) {
            mWorkingTimer.cancel();
            mWorkingTimer = null;
        }
    }

    // Daemon Timer
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private static boolean canDial() {
        if (mTelList == null) {
            LogUtils.e("ERROR: canDial: mTelList null");
            return false;
        }

        if (mTelList.size() == 0) {
            LogUtils.e("ERROR: canDial: mTelList empty");
            return false;
        }

        if (TextUtils.isEmpty(mPicturePath)) {
            LogUtils.e("ERROR: canDial: mPicturePath empty");
            return false;
        }

        if (mCurrentCallIndex >= mTelList.size()) {
            LogUtils.i("***canDial: All call numbers(" + mCurrentCallIndex + "/" + mTelList.size() + ") have been finished.");
            return false;
        }

        LogUtils.i("***canDial: yes");
        return true;
    }

    private static void loopCallOnNum(final Context context) {
        LogUtils.d("loopCallOnNum: mCurrentCallIndex=" + mCurrentCallIndex);

        String strTel = getCurrentTelNumber();
        if (!TelUtils.isValidTelNumber(strTel)) {
            showProgress("#" + (mCurrentCallIndex+1) + ": Invalid Tel Number = \"" + strTel + "\"");
            callNextNumber(context);
            return;
        }

        if (TelUtils.isCalling(context)) {
            showProgress("#" + (mCurrentCallIndex+1) + ": Last call not ended, try end it and call again...");
            TelUtils.endCall(context);
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    loopCallOnNum(context);
                }
            }, ComDef.WAIT_CALL_IDLE_DELAY);
            return;
        }

        try {
            String ret = TelUtils.startCall(context, strTel);
            showProgress("#" + (mCurrentCallIndex+1) + ": Start Call, Tel = " + strTel + ", " + ret);

            new Handler().postDelayed(new Runnable() {
                public void run() {
                    ScreenCapture.captureOnce();
                }
            }, getScreenCaptureDelay());

            new Handler().postDelayed(new Runnable() {
                public void run() {
                    String retEndCall = TelUtils.endCall(context);
                    showProgress("End Call: " + retEndCall);
                }
            }, getEndCallDelay());

            new Handler().postDelayed(new Runnable() {
                public void run() {
                    callNextNumber(context);
                }
            }, getCallNextDelay());

        } catch (Exception e) {
            //Toast.makeText(context, "loopCallOnNum Exception: " + e.toString(), Toast.LENGTH_SHORT).show();
            showProgress("loopCallOnNum Exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private static long getScreenCaptureDelay() {
        long delay = getEndCallDelay() - ComDef.CAPTURE_SCREEN_OFFSET;
        if (delay < 0) {
            return 0;
        }
        else {
            return delay;
        }
    }

    private static long getCallNextDelay() {
        return getEndCallDelay() + ComDef.CALL_NEXT_OFFSET;
    }

    private static void callNextNumber(final Context context) {
        if (!mWorking) {
            LogUtils.d("callNextNumber: Working stopped.");
            return;
        }

        mTotalCalledNum++;
        LogUtils.d("callNextNumber: mTotalCalledNum = " + mTotalCalledNum);

        setCurrentCallIndex(mCurrentCallIndex + 1);

        if (mCurrentCallIndex >= mTelList.size()) {
            LogUtils.d("callNextNumber: mCurrentCallIndex(" + mCurrentCallIndex + ") up to tel list size(" + mTelList.size() + ")");
            onAllCallFinished();
        }
        else {
            loopCallOnNum(context);
        }
    }

    private static void setCurrentCallIndex(int index) {
        LogUtils.d("setCurrentCallIndex: index = " + index);
        mCurrentCallIndex = index;
        Settings.saveCurrentCallIndex(mCurrentCallIndex);
    }

    private static void onAllCallFinished() {
        LogUtils.i("onAllCallFinished");
        String strDateTime = new SimpleDateFormat("yyMMdd_HHmmss").format(new Date(System.currentTimeMillis()));

        //zip pic data and upload
        String zipFileName = FileUtils.getFileNeatName(mTelListFileName) + "_" + strDateTime + ".zip";
        String zipFileNameDone = FileUtils.getFileNeatName(mTelListFileName) + "_" + strDateTime + "_done.zip";
        String zipFilePath = ComDef.DIALER_PIC_DIR + "/" + zipFileName;
        LogUtils.d("onAllCallFinished: mPicturePath = " + mPicturePath);
        LogUtils.d("onAllCallFinished: zipFilePath = " + zipFilePath);

        if (!ZipUtils.zip(zipFilePath, mPicturePath, true)) {
            LogUtils.e("ERROR: onAllCallFinished: zip pic failed");
            onUploadFinished();
        }
        else {
            MultiDialClient.uploadPicData(zipFileName, zipFileNameDone);
        }
    }

    public static void onUploadFinished() {
        MultiDialClient.moveRunDataToEnd(mTelListFileName);
        mDialing = false;
    }

    protected static void saveCaptureImage(Image img) {
        LogUtils.d("saveCaptureImage: E...");
        String jpgFileName = mPicturePath + "/" + getCurrentTelNumber() + "_" + TimeUtils.getFileTime(false) + ".jpg";

        int ret = ImageUtils.saveImage2JPGFile(img, jpgFileName, getJpegQuality());
        if (ret < 0) {
            LogUtils.e("save screen image to " + jpgFileName + " failed with error " + ret);
        } else {
            LogUtils.i("screen image saved to " + jpgFileName);
        }
    }
}
