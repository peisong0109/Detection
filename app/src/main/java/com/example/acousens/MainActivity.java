package com.example.acousens;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.acousens.EventBus.MessageEvent;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.mob.MobSDK;
import com.shashank.sony.fancytoastlib.FancyToast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;
import cn.smssdk.gui.RegisterPage;

public class MainActivity extends BaseActivity implements View.OnClickListener{
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSION = 1001;

    private String[] permissions = new String[] {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    /**被用户拒绝的权限列表*/
    private List<String> mPermissionList = new ArrayList<>();
    private AudioRecord mAudioRecord;
    private boolean isRecording;
    private Button mAudioControl, mJson;
    private TextView attension, json, result;

    private Executor executor = Executors.newSingleThreadExecutor();
    private KerasTFLite mTFLite;
    private float[][][][] mfcc;
    private KProgressHUD hud;
    private File f;
    private SharedPreferences sp;
    public String phoneNum;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate");
        sp = getSharedPreferences("User", Context.MODE_PRIVATE);

        mAudioControl = findViewById(R.id.btn_control);

        mJson = findViewById(R.id.btn_json);
        mJson.setEnabled(false);
        mAudioControl.setOnClickListener(this);
        mJson.setOnClickListener(this);

        attension = findViewById(R.id.tv);
        msgShow();
        json = findViewById(R.id.tv_json);
        result = findViewById(R.id.tv_probability);
        initPython();
        privacy();
    }




    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart");
    }
    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        checkIfLogin();
        checkPermissions();
        super.onStart();
    }
    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        initTensorFlowAndLoadModel();
        super.onResume();
    }

    /*
    * 回传隐私授权结果
    * */
    private void privacy(){
        MobSDK.submitPolicyGrantResult(true, null);
    }
    /*
     * 判断用户是否登录
     * */
    private void checkIfLogin(){
//        sp = getSharedPreferences("login", Context.MODE_PRIVATE);
        phoneNum = sp.getString("phoneNum", "0");
    }

    private void msgShow(){
        attension.append(getString(R.string.message0));
        attension.append("\n"+ getString(R.string.message1));
        attension.append("\n"+ getString(R.string.message2));
        attension.append("\n"+ getString(R.string.message3));
    }

    /**
     * 开始录制
     */
    public void startRecord() {
        final int minBufferSize = AudioRecord.getMinBufferSize(Consts.SAMPLE_RATE_INHZ, Consts.CHANNEL_CONFIG, Consts.AUDIO_FORMAT);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, Consts.SAMPLE_RATE_INHZ, Consts.CHANNEL_CONFIG
                , Consts.AUDIO_FORMAT, minBufferSize);

        final byte[] data = new byte[minBufferSize];
        final File file = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.pcm");
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        if (file.exists()) {
            file.delete();
        }

        mAudioRecord.startRecording();
        isRecording = true;

        //TODO pcm 数据无法直接播放， 保存为wav 格式

        new Thread(new Runnable() {
            @Override
            public void run() {
                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(file);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (os != null) {
                    while (isRecording) {
                        int read = mAudioRecord.read(data, 0, minBufferSize);
                        //如果读取音频数据没有出现错误， 就讲数据写入到文件
                        if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                            try {
                                os.write(data);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    try {
                        Log.i(TAG, "run: close file output stream !");
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /***
     * 停止录制
     */
    public void stopRecord() {
        isRecording = false;
        //释放资源
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            for (int i = 0; i < grantResults.length; i ++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, permissions[i] + " 权限被用户禁止！");
                }else {
                    mPermissionList.remove(i);
                }
            }
        }
    }

    /***
     * 检查权限
     */
    private void checkPermissions() {
        mPermissionList.clear();
        //6.0 动态权限判断
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < permissions.length; i ++) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), permissions[i])
                        != PackageManager.PERMISSION_GRANTED) {
                    mPermissionList.add(permissions[i]);
                }
            }
            if (!mPermissionList.isEmpty()) {
                String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION);
            }
        }
    }

    private void pcmToWav(){
        PcmToWavUtil pcmToWavUtil = new PcmToWavUtil(Consts.SAMPLE_RATE_INHZ, Consts.CHANNEL_CONFIG, Consts.AUDIO_FORMAT);
        File pcmFile = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.pcm");
        File wavFile = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.wav");
        f = wavFile;
//                File jsonFile = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.json");
        if (pcmFile.exists()) {
            wavFile.delete();
        }
        pcmToWavUtil.pcmToWav(pcmFile.getAbsolutePath(), wavFile.getAbsolutePath());
        startPython(wavFile.getAbsolutePath());
    }

    // 初始化Python环境
    void initPython() {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }
    public void startPython(String wavFile) {
        new Thread(new Runnable() {
            public void run() {
                // a potentially time consuming task
                Python py = Python.getInstance();
                PyObject obj1 = py.getModule("WAV_TO_MFCC").callAttr("wav_to_mfcc", wavFile);
//                PyObject obj1 = py.getModule("Test").callAttr("test");
//                String result2 = obj1.toJava(String.class);
                mfcc = obj1.toJava(float[][][][].class);

                json.post(new Runnable() {
                    public void run() {
//                        result.setText(Arrays.deepToString(result4[0][0]));
//                        result.setText(Float.toString(result3[0][0][0][0]));
                        if (mfcc.length > 0){
//                            mJson.setEnabled(true);
//                            hud.dismiss();
//                            json.setText(getString(R.string.start_det));
                            float total = 0;
                            for (int i=0; i<mfcc.length; i++){
                                float[][][][] result4 = extendFloat(i, mfcc);
                                float result2 = mTFLite.run(result4);
                                total += result2;
//                                total = total + result2;
                            }
                            hud.dismiss();
                            DecimalFormat dF = new DecimalFormat("0.00");
                            String probability = dF.format(total/mfcc.length);
                            showResult(getString(R.string.result), probability);
                            uploadResult(probability);
                        }else {
                            json.setText("Too short.");
                        }
                    }
                });
            }
        }).start();
    }

    private float[][][][] extendFloat(int x, float[][][][] src) {
        float[][][][] des = new float[1][282][32][1];
        for (int i=0; i < 13; i++){
            for (int j=0; j<282; j++){
                des[0][j][i+9][0] = src[x][j][i][0];
            }
        }
        return des;
    }


    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mTFLite == null)
                        mTFLite = new KerasTFLite(MainActivity.this);

                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    @Override
    protected void onPause() {

        Log.i(TAG, "onPause");
        super.onPause();
    }
    @Override
    protected void onStop() {
        if (mTFLite != null) {
            mTFLite.release();
            mTFLite = null;
        }
        Log.i(TAG, "onStop");
        super.onStop();
    }
    @Override
    protected void onDestroy() {
//        EventBus.getDefault().unregister(this);
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    //倒计时60秒,这里不直接写60000,而用1000*60是因为后者看起来更直观,每走一步是1000毫秒也就是1秒
    CountDownTimer timer = new CountDownTimer(1000 * 3, 1000) {
        @SuppressLint("DefaultLocale")
        @Override
        public void onTick(long millisUntilFinished) {
            mAudioControl.setEnabled(false);
            mAudioControl.setText(String.format("(%d)",millisUntilFinished/1000));
        }

        @Override
        public void onFinish() {
            mAudioControl.setEnabled(true);
            mAudioControl.setText(getString(R.string.stop_record));
        }
    };

    /**
     * Show the messages under "detect" button.
     * @param msg
     * @param prob
     */
    private void showResult(String msg, String prob){
        json.setText(msg);
        result.setText(prob);
    }

    /*
    得到结果后上传数据
     */
    private void uploadResult(String pro){
        boolean agree = sp.getBoolean("allowUpload", false);
        String path = getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/test.wav";
        boolean fileExist = fileIsExists(path);
        if(fileExist & agree){
//            Toast.makeText(MainActivity.this,"开始上传"+f.getAbsolutePath(), Toast.LENGTH_LONG).show();
            try {
                //上传图片
                AudioUploading.run(f, pro, phoneNum);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else {
//            Toast.makeText(MainActivity.this,"文件不存在或未同意上传", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_control:
                if (!mPermissionList.isEmpty()) {
                    String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);
                    ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION);
                }else {
                    if (phoneNum.equals("0")){
                        FancyToast.makeText(this,getString(R.string.toast_login),FancyToast.LENGTH_LONG, FancyToast.WARNING,false).show();
                    }else {
                        if (mAudioControl.getText().toString().endsWith(getString(R.string.start_record))) {
                            timer.start();
                            mAudioControl.setText(getString(R.string.stop_record));
                            startRecord();

                            mJson.setEnabled(false);
                            showResult(getString(R.string.recording), " ");
                        } else {
                            mAudioControl.setText(getString(R.string.start_record));
                            stopRecord();

                            mJson.setEnabled(true);
                            showResult(getString(R.string.start_det), " ");

                        }
                    }
                }


                break;

            case R.id.btn_json:
                pcmToWav();

                hud = KProgressHUD.create(this)
                        .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                        .setLabel(getString(R.string.hud_wait))
                        .setDetailsLabel(getString(R.string.hud_prc))
                        .setBackgroundColor(ContextCompat.getColor(this, R.color.teal_200))
                        .show();
                showResult(" ", " ");

                break;
        }
    }

    //判断文件是否存在
    public boolean fileIsExists(String strFile) {
        try {
            f = new File(strFile);
            if (!f.exists()) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }


//    /*
//    * 验证码
//    * */
//    public void sendCode(Context context) {
//        RegisterPage page = new RegisterPage();
//
//        page.setTempCode(null);
//
//        page.setRegisterCallback(new EventHandler() {
//            public void afterEvent(int event, int result, Object data) {
//                if (result == SMSSDK.RESULT_COMPLETE) {
//
//                    HashMap<String, Object> phoneMap = (HashMap<String, Object>) data;
//                    String country = (String) phoneMap.get("country"); // Country code
//                    String phone = (String) phoneMap.get("phone"); //
//                    // TODO Using the country code and phone number for the subsequent operations
////                    Toast.makeText(getApplicationContext(),country+" "+phone,Toast.LENGTH_SHORT).show();
////                    countryCode = country;
//                    phoneNum = country + "-" +phone;
//                    Toast.makeText(context, country + "-" +phone,Toast.LENGTH_LONG).show();
//                    sp.edit()
//                            .putString("phoneNum", phoneNum)
//                            .apply();
////                    // 异步执行任务
////                    Callable<User> callable = new Callable<User>(){
////                        @Override
////                        public User call() throws Exception {
////                            // do something
////                            // TODO: 2021-12-01 Upload the info to Mongodb (Optional)
////                            return new User(countryCode+phoneNum, "123456");
////                        }
////                    };
////
////                    // 异步回调
////                    AsyncCallback<User> async = new AsyncCallback<User>() {
////                        @Override
////                        public void onSuccess(User user) {
////                            // notify success;
////                            Toast.makeText(context, countryCode+"-"+phoneNum,Toast.LENGTH_LONG).show();
////                            sp.edit()
////                                    .putString("phoneNum", countryCode+"-"+phoneNum)
////                                    .apply();
////                        }
////
////                        @Override
////                        public void onFailed(Throwable t) {
////                            // notify failed.
////                        }
////                    };
////
////                    // 启动异步任务
////                    easyThread.async(callable, async);
//
//
//                }
//            }
//        });
//        page.show(context);
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.login_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_register:
                Intent intent_contacts = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent_contacts);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}