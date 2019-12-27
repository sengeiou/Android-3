package com.liz.multidialer.logic;

import android.text.TextUtils;

import com.jcraft.jsch.ChannelSftp;
import com.liz.androidutils.FileUtils;
import com.liz.androidutils.LogUtils;
import com.liz.multidialer.net.SFTPManager;

import java.util.Vector;

@SuppressWarnings({"unused", "WeakerAccess"})
public class MultiDialClient {

    private static String mDeviceId = "";
    private static String mServerAddress = "";
    private static int mServerPort = 0;
    private static String mUserName = "";
    private static String mPassword = "";
    private static String mNetworkType = ComDef.DEFAULT_NETWORK_TYPE;
    private static String mServerHome;
    private static int mJpegQuality;

    protected static void loadSettings() {
        mDeviceId = Settings.readDeviceId();
        mServerAddress = Settings.readServerAddress();
        mServerPort = Settings.readServerPort();
        mUserName = Settings.readUserName();
        mPassword = Settings.readPassword();
        mNetworkType = Settings.readNetworkType();
        mServerHome = Settings.readServerHome();
        mJpegQuality = Settings.readJpegQuality();
    }

    private static String getListFileString() {
        return ComDef.SFTP_PATH_NUM_WAIT_DATA + "/" + DataLogic.getDeviceId() + "*.txt";
    }

    // NOTE: network operation can't run on main thread
    protected static void fetchTelListFile() {
        new Thread() {
            @Override
            public void run() {
                _fetchTelListFile();
            }
        }.start();
    }

    //
    //do fetch tellist file from server
    //
    private static void _fetchTelListFile() {
        SFTPManager sftpMgr = new SFTPManager(getServerAddress(), getServerPort(), getUserName(), getPassword());
        LogUtils.d("SFTP: connect " + getServerAddress() + ":" + getServerPort() + "...");
        if (!sftpMgr.connect()) {
            LogUtils.d("SFTP: connect failed.");
        } else {
            LogUtils.d("SFTP: connect ok, list file of " + getListFileString() + "...");
            Vector vf = sftpMgr.listFiles(getListFileString());
            if (vf == null) {
                LogUtils.d("SFTP: list files failed.");
                return;
            }
            LogUtils.d("SFTP: list files success, size = " + vf.size());
            if (vf.size() < 1) {
                LogUtils.d("SFTP: list files empty.");
                return;
            }
            String fileName = ((ChannelSftp.LsEntry)vf.get(0)).getFilename();
            LogUtils.d("SFTP: get tel list file, name = " + fileName + ", download...");
            if (!sftpMgr.downloadFile(FileUtils.dirSeparator(ComDef.SFTP_PATH_NUM_WAIT_DATA), fileName,
                    FileUtils.dirSeparator(ComDef.DIALER_NUM_DIR), fileName)) {
                LogUtils.d("SFTP: download file failed.");
                return;
            }
            LogUtils.d("SFTP: download file success.");

            String srcFilePath = FileUtils.dirSeparator(ComDef.SFTP_PATH_NUM_WAIT_DATA) + fileName;
            String tarFilePath = FileUtils.dirSeparator(ComDef.SFTP_PATH_NUM_RUN_DATA) + fileName;
            LogUtils.d("SFTP: mv file " + srcFilePath + " to " + tarFilePath + "...");
            sftpMgr.mv(srcFilePath, tarFilePath);

            DataLogic.setTelListFileName(fileName);
            DataLogic.loadTelList();
            sftpMgr.disconnect();
        }
    }

    // NOTE: network operation can't run on main thread
    public static void uploadPicData(final String fileName, final String fileNameDone) {
        new Thread() {
            @Override
            public void run() {
                _uploadPicData(fileName, fileNameDone);
                DataLogic.onUploadFinished();
            }
        }.start();
    }

    private static void _uploadPicData(String fileName, String fileNameDone) {
        LogUtils.d("_uploadPicData: fileName = " + fileName);
        if (TextUtils.isEmpty(fileName)) {
            LogUtils.e("ERROR: _uploadPicData: no file name to upload");
            return;
        }
        SFTPManager sftpMgr = new SFTPManager(getServerAddress(), getServerPort(), getUserName(), getPassword());
        DataLogic.showProgress("_uploadPicData: SFTP: connect " + getServerAddress() + ":" + getServerPort() + "...");
        if (!sftpMgr.connect()) {
            DataLogic.showProgress("_uploadPicData: SFTP: connect failed.");
        } else {
            DataLogic.showProgress("_uploadPicData: SFTP: connect ok");

            String remotePath = FileUtils.dirSeparator(ComDef.SFTP_PATH_PIC_WAIT_DATA);
            String remoteFileName = fileName;
            String localPath = FileUtils.dirSeparator(ComDef.DIALER_PIC_DIR);
            String localFileName = fileName;

            DataLogic.showProgress("_uploadPicData: SFTP: upload file " + fileName + " to " + remotePath + "...");
            if (!sftpMgr.uploadFile(remotePath, remoteFileName, localPath, localFileName)) {
                LogUtils.e("ERROR: upload failed.");
            }
            else {
                LogUtils.d("_uploadPicData: upload success");
                String srcFilePath = remotePath + fileName;
                String tarFilePath = remotePath + fileNameDone;
                DataLogic.showProgress("SFTP: rename file " + srcFilePath + " to " + tarFilePath + "...");
                sftpMgr.mv(srcFilePath, tarFilePath);
            }

            sftpMgr.disconnect();
        }
    }

    // NOTE: network operation can't run on main thread
    protected static void moveRunDataToEnd(String fileName) {
        final String _fileName = fileName;
        new Thread() {
            @Override
            public void run() {
                _moveRunDataToEnd(_fileName);
            }
        }.start();
    }

    public static void _moveRunDataToEnd(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            LogUtils.e("ERROR: _moveRunDataToEnd: no file name to mv");
            return;
        }
        SFTPManager sftpMgr = new SFTPManager(getServerAddress(), getServerPort(), getUserName(), getPassword());
        DataLogic.showProgress("_moveRunDataToEnd: SFTP: connect " + getServerAddress() + ":" + getServerPort() + "...");
        if (!sftpMgr.connect()) {
            DataLogic.showProgress("_moveRunDataToEnd: SFTP: connect failed.");
        } else {
            DataLogic.showProgress("_moveRunDataToEnd: SFTP: connect ok");
            String srcFilePath = FileUtils.dirSeparator(ComDef.SFTP_PATH_NUM_RUN_DATA) + fileName;
            String tarFilePath = FileUtils.dirSeparator(ComDef.SFTP_PATH_NUM_END_DATA) + fileName;
            DataLogic.showProgress("_moveRunDataToEnd: SFTP: mv file " + srcFilePath + " to " + tarFilePath + "...");
            sftpMgr.mv(srcFilePath, tarFilePath);
            sftpMgr.disconnect();
        }
    }

    public static String getDeviceId() { return mDeviceId;  }
    public static void setDeviceId(String value) { mDeviceId = value; Settings.saveDeviceId(value); }

    public static String getServerAddress() { return mServerAddress; }
    public static void setServerAddress(String value) { mServerAddress = value; Settings.saveServerAddress(value); }

    public static int getServerPort() { return mServerPort; }
    public static String getServerPortInfo() { return mServerPort + ""; }
    public static void setServerPort(int value) { mServerPort = value; Settings.saveServerPort(value); }

    public static String getUserName() { return mUserName; }
    public static void setUserName(String value) { mUserName = value; Settings.saveUserName(value); }

    public static String getPassword() { return mPassword; }
    public static void setPassword(String value) { mPassword = value; Settings.savePassword(value); }

    public static String getNetworkType() { return mNetworkType; }
    public static void setNetworkType(String value) { mNetworkType = value; Settings.saveNetworkType(value); }

    public static String getServerHome() { return mServerHome; }
    public static void setServerHome(String value) { mServerHome = value; Settings.saveServerHome(value); }

    public static int getJpegQuality() { return mJpegQuality; }
    public static String getJpegQualityInfo() { return mJpegQuality + ""; }
    public static void setJpegQuality(int value) { mJpegQuality = value; Settings.saveJpegQuality(value); }
}
