package com.example.acousens.EventBus;

/**
 * Created by lipeisong
 * on 2019/10/8
 * Description
 */
public class MessageEvent {

    private String message;
    private int type;

    public MessageEvent(int type, String message) {
        this.type = type;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
