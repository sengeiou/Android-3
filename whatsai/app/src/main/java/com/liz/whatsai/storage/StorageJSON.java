package com.liz.whatsai.storage;

import android.text.TextUtils;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.liz.androidutils.LJson;
import com.liz.androidutils.LogUtils;
import com.liz.whatsai.logic.ComDef;
import com.liz.whatsai.logic.Node;
import com.liz.whatsai.logic.WhatsaiDir;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.liz.whatsai.logic.WhatsaiFile;

import org.json.JSONObject;

/**
 * WhatsaiStorage:
 * Created by liz on 2019/2/15.
 */

@SuppressWarnings("unused")
public class StorageJSON {

    private static String ENTER = System.getProperty("line.separator");

    public static void saveToJSON(OutputStream out, WhatsaiDir rootNode) {
        String jsonString = node2JsonStr(rootNode);
        jsonString = LJson.formatJson(jsonString);
        try {
            out.write(jsonString.getBytes());
        }
        catch (Exception e) {
            LogUtils.e("saveToJSON: exception = " + e.toString());
            e.printStackTrace();
        }
    }

    public static void loadData(WhatsaiDir rootNode) {
        String jsonStr = LJson.readJsonFile(ComDef.WHATSAI_DATA_FILE);
        if (TextUtils.isEmpty(jsonStr)) {
            LogUtils.e("loadFromJSON failed with empty string");
        }
        else {
            com.alibaba.fastjson.JSONObject jobj = com.alibaba.fastjson.JSON.parseObject(jsonStr);
            jsonObj2Node(jobj, rootNode);
        }
    }

    private static String node2JsonStr(Node node) {
        String jsonStr = "{";
        try {
            jsonStr += buildJsonStr(node);

            if (node.isDir()) {
                String listStr = "[";
                int listSize = node.getChildNumber();
                for (int i=0; i<listSize; i++) {
                    Node subNode = node.getChild(i);
                    listStr += node2JsonStr(subNode);
                    if (i != listSize - 1) {
                        listStr += ",";
                    }
                }
                listStr += "]";
                jsonStr += ", " + "\"" + ComDef.TAG_LIST + "\": " + listStr;
            }

            jsonStr += "}";
            return jsonStr;
        } catch (Exception e) {
            return "";
        }
    }

    private static void jsonObj2Node(com.alibaba.fastjson.JSONObject jobj, Node node) {
        parseNode(jobj, node);
        if (node.isDir()) {
            try {
                com.alibaba.fastjson.JSONArray jsonList = jobj.getJSONArray(ComDef.TAG_LIST);
                for (int i = 0; i < jsonList.size(); i++) {
                    com.alibaba.fastjson.JSONObject jsonObj = (com.alibaba.fastjson.JSONObject) jsonList.get(i);
                    int type = (int) jsonObj.get(ComDef.TAG_TYPE);
                    Node subNode = Node.createNode(type);
                    jsonObj2Node(jsonObj, subNode);
                    node.add(subNode);
                }
            } catch (Exception e) {
                LogUtils.e("jsonObj2Node exception: " + e.toString());
                e.printStackTrace();
            }
        }
    }

    private static String buildJsonStr(Node node) {
        String jsonStr = "";
        jsonStr += "\"" + ComDef.TAG_TYPE + "\": " + node.getType();
        jsonStr += ", " + "\"" + ComDef.TAG_NAME + "\": \"" + node.getName() + "\"";
        if (!TextUtils.isEmpty(node.getDetail())) {
            jsonStr += ", " + "\"" + ComDef.TAG_DETAIL + "\": \"" + node.getDetail() + "\"";
        }
        if (node.getSyncTime() != 0) {
            jsonStr += ", " + "\"" + ComDef.TAG_SYNC_TIME + "\": " + node.getSyncTime();
        }
        return jsonStr;
    }

    private static void parseNode(com.alibaba.fastjson.JSONObject jobj, Node node) {
        try {
            {
                Object obj = jobj.get(ComDef.TAG_NAME);
                if (obj != null) {
                    node.setName((String) obj);
                } else {
                    LogUtils.e("null node name");
                }
            }
            {
                Object obj = jobj.get(ComDef.TAG_DETAIL);
                if (obj != null) {
                    node.setDetail((String) obj);
                }
            }
            {
                Object obj = jobj.get(ComDef.TAG_SYNC_TIME);
                if (obj != null) {
                    node.setSyncTime((long) obj);
                }
            }
        }
        catch (Exception e) {
            LogUtils.e("parseNode exception: " + e.toString());
            e.printStackTrace();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Test Functions

    //Android modules must run on android devices
    public static void test() {
        test_json();
    }

    public static void main(String[] args) {
        test_jsonLoad();
        //test_json();
        //test_Gson();
        //test_fastJson();
        //test_fastJson2();
        //test_net_sf_json();
    }

    public static void test_jsonLoad() {
        String jsonStr = " {TYPE: 1, NAME: \"whatsai\", LIST: [{TYPE: 0, NAME: \"TaskName0\"},{TYPE: 0, NAME: \"TaskName1\"},{TYPE: 1, NAME: \"TaskGroup1\", LIST: [{TYPE: 0, NAME: \"SubTask10\"},{TYPE: 0, NAME: \"SubTask11\"},{TYPE: 1, NAME: \"taskgroup11\", LIST: [{TYPE: 0, NAME: \"SubTask110\"},{TYPE: 0, NAME: \"SubTask111\"},{TYPE: 0, NAME: \"SubTask112\"}]}]},{TYPE: 0, NAME: \"TaskName2\"},{TYPE: 0, NAME: \"TaskName3\"},{TYPE: 0, NAME: \"TaskName4\"},{TYPE: 1, NAME: \"TaskGroup2\", LIST: [{TYPE: 0, NAME: \"SubTask20\"},{TYPE: 0, NAME: \"SubTask21\"},{TYPE: 0, NAME: \"SubTask22\"},{TYPE: 0, NAME: \"SubTask23\"},{TYPE: 0, NAME: \"SubTask24\"},{TYPE: 0, NAME: \"SubTask25\"},{TYPE: 0, NAME: \"SubTask26\"},{TYPE: 0, NAME: \"SubTask27\"},{TYPE: 0, NAME: \"SubTask28\"},{TYPE: 0, NAME: \"SubTask29\"},{TYPE: 0, NAME: \"SubTask210\"},{TYPE: 0, NAME: \"SubTask211\"},{TYPE: 0, NAME: \"SubTask212\"},{TYPE: 0, NAME: \"SubTask213\"},{TYPE: 0, NAME: \"SubTask214\"},{TYPE: 0, NAME: \"SubTask215\"},{TYPE: 0, NAME: \"SubTask216\"},{TYPE: 0, NAME: \"SubTask217\"},{TYPE: 0, NAME: \"SubTask218\"},{TYPE: 0, NAME: \"SubTask219\"},{TYPE: 0, NAME: \"SubTask220\"},{TYPE: 0, NAME: \"SubTask221\"},{TYPE: 0, NAME: \"SubTask222\"},{TYPE: 0, NAME: \"SubTask223\"},{TYPE: 0, NAME: \"SubTask224\"},{TYPE: 0, NAME: \"SubTask225\"},{TYPE: 0, NAME: \"SubTask226\"},{TYPE: 0, NAME: \"SubTask227\"},{TYPE: 0, NAME: \"SubTask228\"}]}]}";

    }

    public static void test_json() {
//        //{TYPE: 1, NAME: "whatsai", LIST: [{TYPE: 0, NAME: "TaskName0"},{TYPE: 0, NAME: "TaskName1"}]}
//        String jsonStr = "{TYPE: 1, NAME: \"whatsai\", LIST: [{TYPE: 0, NAME: \"TaskName0\"},{TYPE: 0, NAME: \"TaskName1\"}]}";
//        String newStr = LJson.formatJson(jsonStr);
//        System.out.println(newStr);
        Node rootNode = WhatsaiStorage.loadTestData();
        String jsonString = node2JsonStr(rootNode);
        LogUtils.d("test_json=\n" + jsonString);
        String jsonStringFormat = LJson.formatJson(jsonString);
        LogUtils.d("test_json format =\n" + jsonStringFormat);
    }

    public static void test_net_sf_json() {
//        String json = "[{'name':'huangbiao','age':15},{'name':'liumei','age':14}]";
//        net.sf.json.JSONArray jsonarray =  net.sf.json.JSONArray.fromObject(json);
//        System.out.println(jsonarray);
//        List list = (List) net.sf.json.JSONArray.toCollection(jsonarray, User.class);
//        Iterator it = list.iterator();
//        while (it.hasNext()) {
//            User p = (User) it.next();
//            System.out.println(p.age);
//        }
    }

    public static void test_fastJson2() {
        try {
            JSONObject object = new JSONObject();
            //string
            object.put("string", "this is my string");
            //int
            object.put("int", 2);
            //boolean
            object.put("boolean", true);
            //array
            List<Integer> integers = Arrays.asList(1, 2, 3);
            object.put("list", integers);
            //null
            object.put("null", null);
            System.out.println("###object=" + object);
        }
        catch (Exception e) {
            System.out.println("JSONObject Exception: e=" + e.toString());
        }
    }

    public static class User {
        public User() {

        }
        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }
        public String name;
        public int age;

        @SerializedName("email_address")
        public String emailAddress;

        public List<User> children = new ArrayList<>();

        //因业务需要增加，但并不需要序列化
        public User parent;
    }

    public static void test_fastJson() {
//        WhatsaiDir rootNode = new WhatsaiDir("aaa");
//        WhatsaiDir subNode = new WhatsaiDir("bbb");
//        rootNode.add(subNode);
//
//        //String jsonString = JSON.toJSONString(node.getRemindString());
//        String jsonString = JSON.toJSONString(rootNode);
//        System.out.println("fastJson=" + jsonString);
//
//        WhatsaiDir rootNode2 = JSONObject.parseObject(jsonString, WhatsaiDir.class);
//        System.out.println("JSONString->JavaObject: " + rootNode2);
    }

    public static class UserTypeAdapter extends TypeAdapter<User> {

        @Override
        public void write(JsonWriter out, User value) throws IOException {
            out.beginObject();
            write_obj(out, value);
            out.endObject();
        }

        public void write_obj(JsonWriter out, User value) throws IOException {
            out.name("name").value(value.name);
            out.name("age").value(value.age);
            out.name("email").value(value.emailAddress);
            for (int i=0; i<value.children.size(); i++) {
                write_obj(out, value.children.get(i));
            }
        }

        @Override
        public User read(JsonReader in) throws IOException {
            User user = new User();
            in.beginObject();
            while (in.hasNext()) {
                switch (in.nextName()) {
                    case "name":
                        user.name = in.nextString();
                        break;
                    case "age":
                        user.age = in.nextInt();
                        break;
                    case "email":
                    case "email_address":
                    case "emailAddress":
                        user.emailAddress = in.nextString();
                        break;
                }
            }
            in.endObject();
            return user;
        }
    }

    public static void test_Gson() {
//        Gson gson = new Gson();
//        int i = gson.fromJson("100", int.class);              //100
//        double d = gson.fromJson("\"99.99\"", double.class);  //99.99
//        boolean b = gson.fromJson("true", boolean.class);     // true
//        String str = gson.fromJson("String", String.class);   // String

        {
            Gson gson = new Gson();
            User user = new User("怪盗kidou", 24);
            user.emailAddress = "aaaaaa";
            String jsonString = gson.toJson(user); // {"name":"怪盗kidou","age":24}
            System.out.println("jsonObject=" + jsonString);
        }
        {
            Gson gson = new Gson();
            String jsonString = "{\"name\":\"怪盗kidou\",\"age\":24,\"emailAddress\":\"ikidou@example.com\"}";//{"name":"怪盗kidou","age":24,"emailAddress":"ikidou@example.com"};  //"{\"name\":\"怪盗kidou\",\"age\":24}";
            User user = gson.fromJson(jsonString, User.class);
            System.out.println("jsonObject.emailAddress=" + user.emailAddress);
        }
        {
            try {
                JsonWriter writer = new JsonWriter(new OutputStreamWriter(System.out));
                writer.beginObject() // throws IOException
                        .name("name").value("怪盗kidou")
                        .name("age").value(24)
                        .name("email").nullValue() //演示null
                        .endObject(); // throws IOException
                writer.flush(); // throws IOException
                System.out.println("");
            }
            catch(Exception e){

            }
        }
        {
            User user = new User("怪盗kidou", 44);
            user.emailAddress = "ikidou@example.com";
            User user2 = new User("user2", 20);
            user.children.add(user2);
            user2.parent = user;

            Gson gson = new GsonBuilder()
                    //为User注册TypeAdapter
                    .registerTypeAdapter(User.class, new UserTypeAdapter())
                    .create();
            System.out.println("gson TypeAdapter=" + gson.toJson(user));
        }
//        User user = new User("怪盗kidou", 24);
//        user.emailAddress = "ikidou@example.com";
//        Gson gson = new GsonBuilder()
//                //为User注册TypeAdapter
//                .registerTypeAdapter(User.class, new UserTypeAdapter())
//                .create();
//        System.out.println(gson.toJson(user));
    }


//
//    public static RespQueryMdn parseResponseQueryMdn(String resStr) {
//        RespQueryMdn resp = new RespQueryMdn();
//
//        try {
//            JSONObject obj = new JSONObject(resStr);
//            int resCode = obj.optInt("resCode");
//            LogUtils.d("parseResponseQueryMdn: resCode = " + resCode);
//            switch(resCode) {
//                case REQ_QUERY_MDN_SUCCESS:
//                    String resMsg = obj.optString("resMsg");
//                    String dataStr = obj.optString("data");
//                    LogUtils.d("resMsg: " + resMsg);
//                    LogUtils.d("data: " + dataStr);
//                    if (TextUtils.isEmpty(dataStr)) {
//                        LogUtils.e("ERROR: parseResponseQueryMdn: data empty");
//                        resp.errMsg = "RESPONSE DATA EMPTY";
//                    } else {
//                        JSONObject dataObj = new JSONObject(dataStr);
//                        String status = dataObj.optString("status");
//                        LogUtils.i("parseResponseQueryMdn: status = " + status);
//                        if (TextUtils.equals(status, MDN_STATUS_ACTIVE)) {
//                            String nodeUrl = dataObj.optString("url");
//                            if (TextUtils.isEmpty(nodeUrl)) {
//                                resp.errMsg = "URL EMPTY";
//                            }
//                            else {
//                                resp.url = nodeUrl;
//                                resp.errCode = RESP_ERR_SUCCESS;
//                            }
//                        }
//                        else {
//                            resp.errMsg = "对不起，该手机号码业务已取消";
//                        }
//                    }
//                    break;
//                case REQ_QUERY_MDN_ERROR:
//                    resp.errMsg = "QUERY ERROR";
//                    break;
//                case REQ_QUERY_MDN_NOT_FOUND:
//                    resp.errMsg = "对不起，该手机号码尚未开通该业务";
//                    break;
//                case REQ_SERVER_ERROR:
//                    resp.errMsg = "SERVER ERROR";
//                    break;
//                default:
//                    LogUtils.e("ERROR: parseResponseQueryMdn: Unknown resCode " + resCode);
//                    resp.errMsg = "";
//                    break;
//            }
//        } catch (Exception e) {
//            LogUtils.e("parseResponseQueryMdn Exception: " + e.toString());
//            e.printStackTrace();
//        }
//
//        return resp;
//    }
//
//
//
//
//
//    private static String ENTER = System.getProperty("line.separator");
//    private static final int INDENT_LEFT_START = 0;
//    private static final int INDENT_INCREMENT = 4;
//
//    private static String xmlPullParserTag[] = {
//            "START_DOCUMENT",
//            "END_DOCUMENT",
//            "START_TAG",
//            "END_TAG",
//            "TEXT",
//    };
//
//    static void loadFromFile(InputStream xmlStream, WhatsaiDir rootNode) throws Exception {
//        XmlPullParser pullParser = Xml.newPullParser();
//        pullParser.setInput(xmlStream, "UTF-8");
//
//        Node curDir = rootNode;
//        Node newNode;
//        String attrValue;
//
//        int event = pullParser.getEventType();
//        String tagName;
//        while (event != XmlPullParser.END_DOCUMENT) {
//            tagName = pullParser.getName();
//            LogUtils.v("loadFromXML: event=" + xmlPullParserTag[event] + ", tagName=" + tagName);
//
//            switch (event) {
//                case XmlPullParser.START_DOCUMENT:
//                    //we already have rootNode, so here do nothing
//                    //taskGroup = new WhatsaiDir();
//                    break;
//                case XmlPullParser.START_TAG:
//                    if (ComDef.XML_TAG_DIR.equals(tagName)) {
//                        //ENTER into a new taskgroup
//                        newNode = new WhatsaiDir();
//                        attrValue = pullParser.getAttributeValue(null, ComDef.TAG_NAME);
//                        newNode.setName(attrValue);
//                        LogUtils.v("loadFromXML: enter taskgroup, name=" + attrValue);
//                        curDir.add(newNode);
//                        curDir = newNode;
//                    } else if (ComDef.XML_TAG_FILE.equals(tagName)) {
//                        //ENTER into a new task
//                        newNode = new Task();
//                        attrValue = pullParser.getAttributeValue(null, ComDef.TAG_NAME);
//                        newNode.setName(attrValue);
//                        LogUtils.v("loadFromXML: enter task, name=" + attrValue);
//                        attrValue = pullParser.getAttributeValue(null, ComDef.TAG_DETAIL);
//                        newNode.detail = attrValue;
//                        //LogUtils.v("loadFromXML: enter task, detail=" + attrValue);
//                        attrValue = pullParser.getAttributeValue(null, ComDef.XML_ATTR_DONE);
//                        newNode.setDone(TextUtils.equals(attrValue, ComDef.XML_BOOL_TRUE));
//                        //LogUtils.v("loadFromXML: enter task, done=" + attrValue);
//                        attrValue = pullParser.getAttributeValue(null, ComDef.XML_ATTR_REMIND);
//                        newNode.setRemindString(attrValue);
//                        Reminder.checkRemind(newNode);
//                        //LogUtils.v("loadFromXML: enter task, remind=" + attrValue);
//                        curDir.add(newNode);
//                    } else {
//                        LogUtils.e("loadFromXML: Unknown START_TAG: " + tagName);
//                    }
//                    break;
//                case XmlPullParser.END_TAG:
//                    if (ComDef.XML_TAG_DIR.equals(tagName)) {
//                        //exit current group, back to upper group
//                        LogUtils.v("loadFromXML: exit taskgroup, name=" + curDir.getName());
//                        curDir = curDir.getParent();
//                    } else if (ComDef.XML_TAG_FILE.equals(tagName)) {
//                        //exit current task, change current node to group
//                        LogUtils.v("loadFromXML: exit task, name=" + curDir.getName());
//                    } else {
//                        LogUtils.e("loadFromXML: Unknown END_TAG: " + tagName);
//                    }
//                    break;
//                case XmlPullParser.TEXT:
//                    LogUtils.v("loadFromXML: getText: \"" + pullParser.getText() + "\"");
//                    break;
//                default:
//                    LogUtils.w("loadFromXML: Unhandled event: " + event);
//                    break;
//            }
//
//            event = pullParser.next();
//        }
//    }
//
//    static void saveToXML(OutputStream out, WhatsaiDir rootNode) throws Exception {
//        XmlSerializer serializer = Xml.newSerializer();
//        serializer.setOutput(out, "UTF-8");
//        serializer.startDocument("UTF-8", true); endLine(serializer, ENTER);
//
//        for (Node node : rootNode.getList()) {
//            writeNode(serializer, node, INDENT_LEFT_START);
//        }
//
//        serializer.endDocument();
//    }
//
//    private static void writeNode(XmlSerializer s, Node node, int indent) throws Exception {
//        if (node.isDir()) {
//            newLine(s, indent);
//            s.startTag(null, ComDef.XML_TAG_DIR);
//            s.attribute(null, ComDef.TAG_NAME, node.getName()); endLine(s, ENTER);
//            for (Node subNode : node.getList()) {
//                writeNode(s, subNode, indent+ INDENT_INCREMENT);
//            }
//            newLine(s, indent); s.endTag(null, ComDef.XML_TAG_DIR); endLine(s, ENTER);
//        }
//        else {
//            newLine(s, indent);
//            s.startTag(null, ComDef.XML_TAG_FILE);
//            s.attribute(null, ComDef.TAG_NAME, node.getName()==null?"":node.getName());
//            s.attribute(null, ComDef.TAG_DETAIL, node.detail==null?"":node.detail);
//            if (node.isDone()) {
//                s.attribute(null, ComDef.XML_ATTR_DONE, ComDef.XML_BOOL_TRUE);
//            }
//            s.attribute(null, ComDef.XML_ATTR_REMIND, node.getRemindString()==null?"":node.getRemindString());
//            s.endTag(null, ComDef.XML_TAG_FILE);
//            endLine(s, ENTER);
//        }
//    }
//
//    private static void newLine(XmlSerializer s, int indent){
//        try{
//            StringBuilder spaceIndent = new StringBuilder();
//            for (int i=0; i<indent; i++) spaceIndent.append(" ");
//            s.text(spaceIndent.toString());
//        }catch(IOException e){
//            System.out.println(e.getMessage());
//        }
//    }
//
//    private static void endLine(XmlSerializer s, String ENTER){
//        try{
//            s.text(ENTER);
//        }catch(IOException e){
//            System.out.println(e.getMessage());
//        }
//    }
}
