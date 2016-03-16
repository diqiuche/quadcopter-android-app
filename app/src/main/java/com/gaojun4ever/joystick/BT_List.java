package com.gaojun4ever.joystick;

/**
 * Created by gaojun4ever on 2016/2/26.
 */
public class BT_List {
    public BT_List(String btName,String btAddress){
        this.btName=btName;
        this.btAddress=btAddress;
    }
    private String btName;

    public String getBtAddress() {
        return btAddress;
    }

    public void setBtAddress(String btAddress) {
        this.btAddress = btAddress;
    }

    public String getBtName() {
        return btName;
    }

    public void setBtName(String btName) {
        this.btName = btName;
    }

    private String btAddress;
}
