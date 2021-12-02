package com.example.acousens;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.acousens.EventBus.MessageEvent;
import com.shashank.sony.fancytoastlib.FancyToast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import stream.customalert.CustomAlertDialogue;

import static android.content.ContentValues.TAG;

/**
 * Created by lipeisong
 * on 2019/10/5
 * Description
 */
public abstract class BaseActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //注册事件
        EventBus.getDefault().register(this);

}
    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");

        super.onStart();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        switch (event.getType()){
            case 0:
                FancyToast.makeText(this,"Failed to upload. "+event.getMessage(),FancyToast.LENGTH_SHORT, FancyToast.ERROR,false).show();
                break;
            case 1:
                FancyToast.makeText(this, event.getMessage(),FancyToast.LENGTH_SHORT, FancyToast.SUCCESS,false).show();
                break;

        }
    };

    @Override
    protected void onRestart() {
        super.onRestart();
        if (!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
            Log.d("BaseState","Event true");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

    }
    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        Log.d("BaseState","Event false");
        super.onDestroy();

    }



}
