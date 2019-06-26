package com.liz.whatsai.logic;

import android.text.TextUtils;
import android.util.Xml;

import com.liz.whatsai.utils.LogUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Storage:
 * Created by liz on 2019/2/15.
 */

public class Storage {
    private static Node mRootNode = null;
    private static boolean mListDirty = false;

    private static String ENTER = System.getProperty("line.separator");
    private static final int INDENT_LEFT_START = 0;
    private static final int INDENT_INCREMENT = 4;

    static void init() {
        mRootNode = new WhatsaiDir();
        mRootNode.setName(ComDef.APP_NAME);
        load();
        startSavingTimer();
    }

    static Node getRootNode() {
        return mRootNode;
    }

    public static void setDirty(boolean dirty) {
        mListDirty = dirty;
    }

    private static boolean isDirty() {
        return mListDirty;
    }

    private static void startSavingTimer() {
        new Timer().schedule(new TimerTask() {
            public void run () {
                DataLogic.save();
            }
        }, ComDef.TASK_LIST_SAVING_DELAY, ComDef.TASK_LIST_SAVING_TIMER);
    }

    private static String xmlPullParserTag[] = {
            "START_DOCUMENT",
            "END_DOCUMENT",
            "START_TAG",
            "END_TAG",
            "TEXT",
    };

    private static void load() {
        //##@: loadTestData();
        try {
            File f = new File(ComDef.WHATSAI_DATA_FILE);
            if (!f.exists()) {
                LogUtils.i("whatsai data file \"" + ComDef.WHATSAI_DATA_FILE + "\" not exists");
            }
            else {
                InputStream input = new FileInputStream(f);
                loadFromXML(input, (WhatsaiDir)mRootNode);
            }
        } catch (Exception e) {
            LogUtils.e("load from xml exception: " + e.toString());
        }
    }

    private static void loadFromXML(InputStream xmlStream, WhatsaiDir rootNode) throws Exception {
        XmlPullParser pullParser = Xml.newPullParser();
        pullParser.setInput(xmlStream, "UTF-8");

        Node curDir = rootNode;
        Node newNode;
        String attrValue;

        int event = pullParser.getEventType();
        String tagName;
        while (event != XmlPullParser.END_DOCUMENT) {
            tagName = pullParser.getName();
            LogUtils.v("loadFromXML: event=" + xmlPullParserTag[event] + ", tagName=" + tagName);

            switch (event) {
                case XmlPullParser.START_DOCUMENT:
                    //we already have rootNode, so here do nothing
                    //taskGroup = new WhatsaiDir();
                    break;
                case XmlPullParser.START_TAG:
                    if (ComDef.XML_TAG_DIR.equals(tagName)) {
                        //ENTER into a new taskgroup
                        newNode = new WhatsaiDir();
                        attrValue = pullParser.getAttributeValue(null, ComDef.XML_ATTR_NAME);
                        newNode.setName(attrValue);
                        LogUtils.v("loadFromXML: enter taskgroup, name=" + attrValue);
                        curDir.add(newNode);
                        curDir = newNode;
                    } else if (ComDef.XML_TAG_FILE.equals(tagName)) {
                        //ENTER into a new task
                        newNode = new Task();
                        attrValue = pullParser.getAttributeValue(null, ComDef.XML_ATTR_NAME);
                        newNode.setName(attrValue);
                        LogUtils.v("loadFromXML: enter task, name=" + attrValue);
                        attrValue = pullParser.getAttributeValue(null, ComDef.XML_ATTR_DETAIL);
                        newNode.detail = attrValue;
                        //LogUtils.v("loadFromXML: enter task, detail=" + attrValue);
                        attrValue = pullParser.getAttributeValue(null, ComDef.XML_ATTR_DONE);
                        newNode.setDone(TextUtils.equals(attrValue, ComDef.XML_BOOL_TRUE));
                        //LogUtils.v("loadFromXML: enter task, done=" + attrValue);
                        attrValue = pullParser.getAttributeValue(null, ComDef.XML_ATTR_REMIND);
                        newNode.setRemindString(attrValue);
                        Reminder.checkRemind(newNode);
                        //LogUtils.v("loadFromXML: enter task, remind=" + attrValue);
                        curDir.add(newNode);
                    } else {
                        LogUtils.e("loadFromXML: Unknown START_TAG: " + tagName);
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (ComDef.XML_TAG_DIR.equals(tagName)) {
                        //exit current group, back to upper group
                        LogUtils.v("loadFromXML: exit taskgroup, name=" + curDir.getName());
                        curDir = curDir.getParent();
                    } else if (ComDef.XML_TAG_FILE.equals(tagName)) {
                        //exit current task, change current node to group
                        LogUtils.v("loadFromXML: exit task, name=" + curDir.getName());
                    } else {
                        LogUtils.e("loadFromXML: Unknown END_TAG: " + tagName);
                    }
                    break;
                case XmlPullParser.TEXT:
                    LogUtils.v("loadFromXML: getText: \"" + pullParser.getText() + "\"");
                    break;
                default:
                    LogUtils.w("loadFromXML: Unhandled event: " + event);
                    break;
            }

            event = pullParser.next();
        }
    }

    public static void save() {
        if (isDirty()) {
            try {
                File path = new File(ComDef.WHATSAI_DATA_PATH);
                if (!path.exists()) {
                    if (!path.mkdirs()) {
                        LogUtils.e("make dir of whatsai path \"" + ComDef.WHATSAI_DATA_PATH + "\" failed.");
                        return;
                    }
                }
                File f = new File(ComDef.WHATSAI_DATA_FILE);
                if (!f.exists()) {
                    if (!f.createNewFile()) {
                        LogUtils.e("create whatsai file \"" + ComDef.WHATSAI_DATA_FILE + "\" failed.");
                        return;
                    }
                }
                OutputStream output = new FileOutputStream(f);
                saveToXML(output, (WhatsaiDir)mRootNode);
                output.flush();
                output.close();

                LogUtils.d("Storage: list data updated and saved.");
                setDirty(false);
            } catch (Exception e) {
                LogUtils.e("save exception: " + e.toString());
            }
        }
        else {
            LogUtils.v("Storage: list data not change for save.");
        }
    }

    private static void saveToXML(OutputStream out, WhatsaiDir rootNode) throws Exception {
        XmlSerializer serializer = Xml.newSerializer();
        serializer.setOutput(out, "UTF-8");
        serializer.startDocument("UTF-8", true); endLine(serializer, ENTER);

        for (Node node : rootNode.getList()) {
            writeNode(serializer, node, INDENT_LEFT_START);
        }

        serializer.endDocument();
    }

    private static void writeNode(XmlSerializer s, Node node, int indent) throws Exception {
        if (node.isDir()) {
            newLine(s, indent);
            s.startTag(null, ComDef.XML_TAG_DIR);
            s.attribute(null, ComDef.XML_ATTR_NAME, node.getName()); endLine(s, ENTER);
            for (Node subNode : node.getList()) {
                writeNode(s, subNode, indent+ INDENT_INCREMENT);
            }
            newLine(s, indent); s.endTag(null, ComDef.XML_TAG_DIR); endLine(s, ENTER);
        }
        else {
            newLine(s, indent);
            s.startTag(null, ComDef.XML_TAG_FILE);
            s.attribute(null, ComDef.XML_ATTR_NAME, node.getName()==null?"":node.getName());
            s.attribute(null, ComDef.XML_ATTR_DETAIL, node.detail==null?"":node.detail);
            if (node.isDone()) {
                s.attribute(null, ComDef.XML_ATTR_DONE, ComDef.XML_BOOL_TRUE);
            }
            s.attribute(null, ComDef.XML_ATTR_REMIND, node.getRemindString()==null?"":node.getRemindString());
            s.endTag(null, ComDef.XML_TAG_FILE);
            endLine(s, ENTER);
        }
    }

    private static void newLine(XmlSerializer s, int indent){
        try{
            StringBuilder spaceIndent = new StringBuilder();
            for (int i=0; i<indent; i++) spaceIndent.append(" ");
            s.text(spaceIndent.toString());
        }catch(IOException e){
            System.out.println(e.getMessage());
        }
    }

    private static void endLine(XmlSerializer s, String ENTER){
        try{
            s.text(ENTER);
        }catch(IOException e){
            System.out.println(e.getMessage());
        }
    }

     /* for test
    protected static void loadTestData() {
        {Task task = new Task(); task.name = "TaskName"; task.setDone(true); mRootNode.add(task);}
        {Task task = new Task(); task.name = "TaskName"; task.setDone(false); mRootNode.add(task);}

        {
            WhatsaiDir tg = new WhatsaiDir();
            tg.name = ComDef.XML_TAG_DIR;
            mRootNode.add(tg);
            for (int i = 0; i < 19; i++) {
                Task task = new Task();
                task.name = "SubTask" + i;
                task.setDone(i % 2 != 0);
                tg.list.add(task);
            }
            {
                WhatsaiDir tg22 = new WhatsaiDir();
                tg22.name = "taskgroup22";
                tg.add(tg22);
                for (int i = 0; i < 5; i++) {
                    Task task = new Task();
                    task.name = "SubTask" + i;
                    task.setDone(i % 2 != 0);
                    tg22.list.add(task);
                }
            }
        }

        {Task task = new Task(); task.name = "TaskName"; task.setDone(false); mRootNode.add(task);}
        {Task task = new Task(); task.name = "TaskName"; task.setDone(true); mRootNode.add(task);}
        {Task task = new Task(); task.name = "TaskName"; task.setDone(true); mRootNode.add(task);}

        {
            WhatsaiDir tg = new WhatsaiDir();
            tg.name = "taskgroup2";
            mRootNode.add(tg);
            for (int i = 0; i < 9; i++) {
                Task task = new Task();
                task.name = "SubTask" + i;
                task.setDone(i % 2 == 0);
                tg.list.add(task);
            }
        }
    }
    //*/
}