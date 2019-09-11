package com.liz.whatsai.logic;

/**
 * Task.java
 * Created by liz on 2018/9/17.
 */

public class Task extends WhatsaiFile {
    private boolean done = false;

    public Task() {
        super();
        done = false;
    }

    public Task(String name) {
        super(name);
        done = false;
    }

    public int getTaskNumber() {
        return 1;  //only itself
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public int getType() {
        return ComDef.NODE_TYPE_TASK;
    }

    @Override
    public void setDone(boolean done) {
        this.done = done;
    }

    @Override
    public void reverseDone() {
        this.done = !this.done;
    }
}
