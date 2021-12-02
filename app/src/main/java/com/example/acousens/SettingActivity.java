package com.example.acousens;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;

import android.content.SharedPreferences;

import android.os.Bundle;

import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.acousens.settingbar.SettingBar;
import com.suke.widget.SwitchButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;
import cn.smssdk.gui.RegisterPage;


public class SettingActivity extends BaseActivity {
    private SharedPreferences sp;
    private LinearLayout container;
    private List<SettingBar> views;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        sp = getSharedPreferences("User", Context.MODE_PRIVATE);
        Boolean needRecord = sp.getBoolean("allowUpload", false);

        container = (LinearLayout) findViewById(R.id.container);
        InitList();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//这句话必须要写

        initShare(needRecord);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home://增加点击事件
                super.onBackPressed();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 是否需要实时记录展示自己的轨迹？
     *
     * @param bl true代表需要
     */
    public void NeedRecord(Boolean bl) {
        sp.edit()
                .putBoolean("allowUpload", bl)
                .apply();
    }

    /**
     * switch Button
     */
    private void initShare(Boolean value2) {

        SwitchButton switchButton = views.get(1).getRightButton();
        switchButton.setChecked(value2);
        switchButton.isChecked();
        switchButton.toggle();     //switch state
        switchButton.toggle(true);//switch without animation
        switchButton.setShadowEffect(true);//disable shadow effect
        switchButton.setEnabled(true);//disable button
        switchButton.setEnableEffect(true);//disable the switch animation
        switchButton.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                if (isChecked) {
                    NeedRecord(true);
                } else {
                    NeedRecord(false);
                }
            }
        });
    }


    public void InitList() {
        views = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            views.add((SettingBar) container.getChildAt(i));
        }
        views.get(0).setRightIconVisibility(false);
        views.get(0).setLeftIconVisibility(true);
        views.get(0).setLeftIcon(R.drawable.ic_account);
        views.get(0).setRightText(sp.getString("phoneNum", "0"));
        views.get(1).setRightButtonVisibility(true);
        views.get(1).setRightIconVisibility(false);
        views.get(1).setLeftIconVisibility(true);
        views.get(1).setLeftIcon(R.drawable.ic_track);
        views.get(2).setTopTitle("");
        views.get(2).setLeftIconVisibility(true);
        views.get(2).setLeftIcon(R.drawable.ic_phone);
        views.get(2).setOnBarClickListener(new SettingBar.OnBarClickListener() {
            @Override
            public void onBarClick() {
                sendCode(getApplicationContext());
            }
        });

    }



    /*============================================  mob  ============================================*/

    /*
     * 验证码
     * */
    public void sendCode(Context context) {
        RegisterPage page = new RegisterPage();

        page.setTempCode(null);

        page.setRegisterCallback(new EventHandler() {
            public void afterEvent(int event, int result, Object data) {
                if (result == SMSSDK.RESULT_COMPLETE) {

                    HashMap<String, Object> phoneMap = (HashMap<String, Object>) data;
                    String country = (String) phoneMap.get("country"); // Country code
                    String phone = (String) phoneMap.get("phone"); //
                    // TODO Using the country code and phone number for the subsequent operations
//                    Toast.makeText(getApplicationContext(),country+" "+phone,Toast.LENGTH_SHORT).show();
//                    countryCode = country;
//                    phoneNum = country + "-" +phone;
//                    Toast.makeText(context, country + "-" +phone,Toast.LENGTH_LONG).show();
                    sp.edit()
                            .putString("phoneNum", country + "-" +phone)
                            .apply();
                    views.get(0).setRightText(country + "-" +phone);
                }
            }
        });
        page.show(context);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        stopThread = false;
    }
}