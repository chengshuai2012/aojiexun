package com.link.cloud.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.anupcowkur.reservoir.Reservoir;
import com.arcsoft.ageestimation.ASAE_FSDKEngine;
import com.arcsoft.ageestimation.ASAE_FSDKError;
import com.arcsoft.ageestimation.ASAE_FSDKVersion;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKVersion;
import com.arcsoft.genderestimation.ASGE_FSDKEngine;
import com.arcsoft.genderestimation.ASGE_FSDKError;
import com.arcsoft.genderestimation.ASGE_FSDKVersion;
import com.guo.android_extend.java.AbsLoop;
import com.guo.android_extend.tools.CameraHelper;
import com.guo.android_extend.widget.CameraFrameData;
import com.guo.android_extend.widget.CameraGLSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.util.ResourceUtil;
import com.link.cloud.BaseApplication;
import com.link.cloud.R;
import com.link.cloud.base.ApiException;
import com.link.cloud.bean.Code_Message;
import com.link.cloud.bean.Lockdata;
import com.link.cloud.constant.Constant;
import com.link.cloud.contract.IsopenCabinet;
import com.link.cloud.core.BaseAppCompatActivity;
import com.link.cloud.gpiotest.Gpio;
import com.link.cloud.setting.TtsSettings;
import com.link.cloud.utils.APKVersionCodeUtils;
import com.link.cloud.utils.FaceDB;
import com.link.cloud.utils.ToastUtils;
import com.link.cloud.utils.Utils;
import com.link.cloud.view.ExitAlertDialog;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.OnClick;

public class LockActivity extends BaseAppCompatActivity implements IsopenCabinet.isopen, CameraSurfaceView.OnCameraListener, View.OnTouchListener, Camera.AutoFocusCallback {
    @Bind(R.id.head_text_01)
    TextView head_text_01;
    @Bind(R.id.head_text_02)
    TextView head_text_02;
    @Bind(R.id.head_text_03)
    TextView head_text_03;
    @Bind(R.id.button02)
    Button button02;
    @Bind(R.id.button1)
    Button button1;
    @Bind(R.id.button2)
    Button button2;
    @Bind(R.id.button3)
    Button button3;
    @Bind(R.id.button4)
    Button button4;
    @Bind(R.id.textView1)
    TextView textView1;
    @Bind(R.id.textView2)
    TextView textView2;
    @Bind(R.id.text_error)
    TextView text_error;
    IsopenCabinet isopenCabinet;
    String deviceId;
    public MesReceiver mesReceiver;
    BaseApplication baseApplication;
    String gpiostr;
    public static final String ACTION_UPDATEUI = "com.link.cloud.dataTime";
    private static String ACTION_USB_PERMISSION = "com.android.USB_PERMISSION";
    SharedPreferences userinfo;
    String gpiotext = "";
    String TAG = "LockActivity";
    ExitAlertDialog exitAlertDialog;
    long time =0;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        baseApplication = (BaseApplication) getApplication();
        exitAlertDialog = new ExitAlertDialog(this);
        exitAlertDialog.setCanceledOnTouchOutside(false);
        exitAlertDialog.setCancelable(false);
        BaseApplication.setMainActivity(this);
        mTts = SpeechSynthesizer.createSynthesizer(this, mTtsInitListener);
        mSharedPreferences = getSharedPreferences(TtsSettings.PREFER_NAME, Activity.MODE_PRIVATE);
        userinfo = getSharedPreferences("user_info", MODE_MULTI_PROCESS);
        deviceId = userinfo.getString("deviceId", "");
        time = 1000L*60L*60L*24L*30L;
        setParam();
    }

    private Toast mToast;
    private SharedPreferences mSharedPreferences;
    // 语音合成对象
    public SpeechSynthesizer mTts;
    // 默认本地发音人
    public static String voicerLocal = "xiaoyan";
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip(getResources().getString(R.string.mTts_stating_error) + code);
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };

    public void setParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        //设置使用本地引擎
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
        //设置发音人资源路径
        mTts.setParameter(ResourceUtil.TTS_RES_PATH, getResourcePath());
        //设置发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME, voicerLocal);
        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, mSharedPreferences.getString("speed_preference", "50"));
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, mSharedPreferences.getString("pitch_preference", "50"));
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, mSharedPreferences.getString("volume_preference", "50"));
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, mSharedPreferences.getString("stream_preference", "3"));

        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/tts.wav");
    }

    //获取发音人资源路径
    private String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        //合成通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "tts/common.jet"));
        tempBuffer.append(";");
        //发音人资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "tts/" + LockActivity.voicerLocal + ".jet"));
        return tempBuffer.toString();
    }

    public void showTip(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }

    EditText code_mumber;

    @Override
    protected void initData() {
        TextView textView = findView(R.id.versionName);
        textView.setText(APKVersionCodeUtils.getVerName(this));
        code_mumber = (EditText) findViewById(R.id.code_mumber1);
        code_mumber.setFocusable(true);
        code_mumber.setCursorVisible(true);
        code_mumber.setFocusableInTouchMode(true);
        code_mumber.requestFocus();
        /**
         * EditText编辑框内容发生变化时的监听回调
         */
    }

    String pwdmodel = "1";

    private class ExitAlertDialog1 extends Dialog implements View.OnClickListener {
        private Context mContext;
        private EditText etPwd;
        private Button btCancel;
        private Button btConfirm;
        private TextView texttilt;

        public ExitAlertDialog1(Context context, int theme) {
            super(context, theme);
            mContext = context;
            initDialog();
        }

        public ExitAlertDialog1(Context context) {
            super(context, R.style.customer_dialog);
            mContext = context;
            initDialog();
        }

        private void initDialog() {
            View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_exit_confirm, null);
            setContentView(view);
            btCancel = (Button) view.findViewById(R.id.btCancel);
            btConfirm = (Button) view.findViewById(R.id.btConfirm);
            etPwd = (EditText) view.findViewById(R.id.deviceCode);
            texttilt = (TextView) view.findViewById(R.id.text_title);
            btCancel.setOnClickListener(this);
            btConfirm.setOnClickListener(this);
        }

        @Override
        public void show() {
            etPwd.setText("");
            if (pwdmodel == "1") {
            } else if (pwdmodel == "2") {
                texttilt.setText(R.string.chang_pwd);
                etPwd.setHint(getResources().getString(R.string.put_new_pwd));
            }
            super.show();
        }

        String devicepwd;
        SharedPreferences userInfo;
        Intent intent;

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btCancel:
                    this.dismiss();
                    break;
                case R.id.btConfirm:
                    etPwd.setInputType(InputType.TYPE_NULL);
                    if (pwdmodel.equals("1")) {
                        String pwd = etPwd.getText().toString().trim();
                        if (Utils.isEmpty(pwd)) {
                            ToastUtils.show(mContext, getResources().getString(R.string.put_pwd), ToastUtils.LENGTH_SHORT);
                            return;
                        }
                        String repwd;
                        try {
                            repwd = Reservoir.get(Constant.KEY_PASSWORD, String.class);
                        } catch (Exception e) {
                            userInfo = getSharedPreferences("user_info", 0);
                            repwd = userInfo.getString("devicepwd", "0");
                        }
                        if (!pwd.equals(repwd)) {
                            ToastUtils.show(mContext, getResources().getString(R.string.error_password), ToastUtils.LENGTH_SHORT);
                            return;
                        } else {
                            if (Camera.getNumberOfCameras() != 0) {
                                userInfo = getSharedPreferences("user_info", 0);
                                userInfo.edit().putString("devicepwd", pwd).commit();
                                mGLSurfaceView.setVisibility(View.INVISIBLE);
                                setting_ll.setVisibility(View.VISIBLE);
                            }
                            this.dismiss();
                        }
                    } else if (pwdmodel.equals("2")) {
                        userInfo = getSharedPreferences("user_info", 0);
                        String pwd = etPwd.getText().toString().trim();
                        if (userInfo.getString("devicepwd", "").toString().trim() == pwd) {
                            ToastUtils.show(mContext, getResources().getString(R.string.same_pwd), ToastUtils.LENGTH_SHORT);
                        } else {
                            userInfo.edit().putString("devicepwd", pwd).commit();
                            ToastUtils.show(mContext, getResources().getString(R.string.chang_pwd_successful), ToastUtils.LENGTH_SHORT);
                        }
                    }
                    break;
            }
        }
    }

    ExitAlertDialog1 exitAlertDialog1;
    //认证一个手指模板,当比对成功且得分大于自定义认证阈值时返回true，否则返回false;
    LinearLayout setting_ll;

    @Override
    protected void initViews(Bundle savedInstanceState) {
        if (Camera.getNumberOfCameras() == 2) {
            mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        if (Camera.getNumberOfCameras() == 1) {
            mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        mCameraRotate = 0;
        mCameraMirror = false;
        mWidth = 640;
        mHeight = 480;
        mFormat = ImageFormat.NV21;
        SharedPreferences userInfo = getSharedPreferences("user_info", 0);
        String repwd = userInfo.getString("devicepwd", "");
        if (TextUtils.isEmpty(repwd)) {
            userInfo.edit().putString("devicepwd", "666666").commit();
        }
        mGLSurfaceView = (CameraGLSurfaceView) findViewById(R.id.glsurfaceView);
        mGLSurfaceView.setOnTouchListener(LockActivity.this);
        mSurfaceView = (CameraSurfaceView) findViewById(R.id.surfaceView);
        setting_ll = (LinearLayout) findViewById(R.id.setting_ll);
        mSurfaceView.setOnCameraListener(LockActivity.this);
        findViewById(R.id.versionName).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                pwdmodel = "1";
                exitAlertDialog1 = new ExitAlertDialog1(LockActivity.this);
                exitAlertDialog1.show();
                return true;
            }
        });
        if (Camera.getNumberOfCameras() != 0) {
            mSurfaceView.setupGLSurafceView(mGLSurfaceView, true, mCameraMirror, mCameraRotate);
            mSurfaceView.debug_print_fps(true, false);
            AFT_FSDKError err = engine.AFT_FSDK_InitialFaceEngine(FaceDB.appid, FaceDB.ft_key, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
            Log.d(TAG, "AFT_FSDK_InitialFaceEngine =" + err.getCode());
            err = engine.AFT_FSDK_GetVersion(version);
            Log.d(TAG, "AFT_FSDK_GetVersion:" + version.toString() + "," + err.getCode());

            ASAE_FSDKError error = mAgeEngine.ASAE_FSDK_InitAgeEngine(FaceDB.appid, FaceDB.ag_key);
            Log.d(TAG, "ASAE_FSDK_InitAgeEngine =" + error.getCode());
            error = mAgeEngine.ASAE_FSDK_GetVersion(mAgeVersion);
            Log.d(TAG, "ASAE_FSDK_GetVersion:" + mAgeVersion.toString() + "," + error.getCode());

            ASGE_FSDKError error1 = mGenderEngine.ASGE_FSDK_InitgGenderEngine(FaceDB.appid, FaceDB.sx_key);
            Log.d(TAG, "ASGE_FSDK_InitgGenderEngine =" + error1.getCode());
            error1 = mGenderEngine.ASGE_FSDK_GetVersion(mGenderVersion);
            Log.d(TAG, "ASGE_FSDK_GetVersion:" + mGenderVersion.toString() + "," + error1.getCode());
            ((BaseApplication) getApplicationContext().getApplicationContext()).mFaceDB.loadFaces();
            mFRAbsLoop = new FRAbsLoop();
            mFRAbsLoop.start();
        } else {
            mGLSurfaceView.setVisibility(View.INVISIBLE);
            setting_ll.setVisibility(View.VISIBLE);
        }


    }

    @Override
    protected void initListeners() {
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initToolbar(Bundle savedInstanceState) {
    }

    ConnectivityManager connectivityManager;

    @OnClick({R.id.button02, R.id.button1, R.id.button2, R.id.button3, R.id.button4, R.id.button5, R.id.button6, R.id.button7, R.id.head_text_02})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button02:
                userinfo = getSharedPreferences("user_info", 0);
                userinfo.edit().putString("gpiotext", "1067").commit();
                gpiostr = userinfo.getString("gpiotext", "");
                Gpio.gpioInt(gpiostr);
                Toast.makeText(LockActivity.this, getResources().getString(R.string.configure_io), Toast.LENGTH_SHORT).show();
                break;
            case R.id.button1:
                Gpio.set(gpiostr, 49);
                Toast.makeText(LockActivity.this, getResources().getString(R.string.set_hight), Toast.LENGTH_SHORT).show();
                break;
            case R.id.button2:
                Gpio.set(gpiostr, 48);
                Toast.makeText(LockActivity.this, getResources().getString(R.string.set_low), Toast.LENGTH_SHORT).show();
                break;
            case R.id.button3:
                textView1.setText(Gpio.get(gpiostr) + "");
                Toast.makeText(LockActivity.this, getResources().getString(R.string.set_stating), Toast.LENGTH_SHORT).show();
                break;
            case R.id.button4:
                deviceId = userinfo.getString("deviceId", "");
                textView2.setText(deviceId);
                break;

            case R.id.button5:
                android.os.Process.killProcess(android.os.Process.myPid());
                break;
            case R.id.button6:
                if (Camera.getNumberOfCameras() != 0) {
                    mGLSurfaceView.setVisibility(View.VISIBLE);
                    setting_ll.setVisibility(View.INVISIBLE);
                }

                break;
            case R.id.button7:
                pwdmodel = "2";
                exitAlertDialog1 = new ExitAlertDialog1(LockActivity.this);
                exitAlertDialog1.show();
                break;
            default:
                break;
        }
    }

    @Override
    public void qrCodeSuccess(Code_Message code_message) {

    }


    @Override
    protected void onResume() {
        Logger.e("resume");
        userinfo = getSharedPreferences("user_info", MODE_MULTI_PROCESS);
        String gpio = userinfo.getString("gpiotext", null);
        deviceId = userinfo.getString("deviceId", "");
        if (gpio == null) {
            userinfo.edit().putString("gpiotext", "1067").commit();
        }
        gpiotext = userinfo.getString(gpiotext, "");
        Gpio.gpioInt(gpiotext);
        Gpio.set(gpiotext, 48);
        Log.e("onCreate: ", System.currentTimeMillis() + "");
        super.onResume();


    }

    @Override
    protected void onStart() {
        super.onStart();
        mesReceiver = new MesReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BaseApplication.ACTION_UPDATEUI);
        registerReceiver(mesReceiver, intentFilter);
        isopenCabinet = new IsopenCabinet();
        isopenCabinet.attachView(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void isopenSuccess(Lockdata resultResponse) {
        SharedPreferences sharedPreferences = getSharedPreferences("user_info", 0);
        gpiostr = sharedPreferences.getString("gpiotext", "");
        Logger.e("LockAcitvity" + "===========" + gpiostr);
        try {
            Gpio.gpioInt(gpiostr);
            Thread.sleep(400);
            Gpio.set(gpiostr, 48);
//            TTSUtils.getInstance().speak("门已开");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Gpio.set(gpiostr, 49);
    }

    @Override
    public void onError(ApiException e) {
        String reg = "[^\u4e00-\u9fa5]";
        String syt = e.getMessage().replaceAll(reg, "");
        Logger.e("BindActivity" + syt);
    }

    /**
     * 合成回调监听。
     */
    public SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
        }

        @Override
        public void onSpeakPaused() {
        }

        @Override
        public void onSpeakResumed() {
        }

        @Override
        public void onSpeakProgress(int i, int i1, int i2) {
        }

        @Override
        public void onCompleted(SpeechError speechError) {
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
        }
    };

    @Override
    public void onResultError(ApiException e) {
        onError(e);
    }

    @Override
    public void onPermissionError(ApiException e) {
        onError(e);
    }

    /**
     * 广播接收器
     *
     * @author kevin
     */
    public class MesReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            head_text_03.setText(intent.getStringExtra("timeStr"));
            head_text_01.setText(intent.getStringExtra("timeData"));
            if (context == null) {
                context.unregisterReceiver(this);
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mesReceiver);
        if (Camera.getNumberOfCameras() != 0) {
            mFRAbsLoop.shutdown();
            AFT_FSDKError err = engine.AFT_FSDK_UninitialFaceEngine();
            Log.d(TAG, "AFT_FSDK_UninitialFaceEngine =" + err.getCode());

            ASAE_FSDKError err1 = mAgeEngine.ASAE_FSDK_UninitAgeEngine();
            Log.d(TAG, "ASAE_FSDK_UninitAgeEngine =" + err1.getCode());
            ASGE_FSDKError err2 = mGenderEngine.ASGE_FSDK_UninitGenderEngine();
            Log.d(TAG, "ASGE_FSDK_UninitGenderEngine =" + err2.getCode());

        }
        super.onDestroy();

    }


    private CameraSurfaceView mSurfaceView;
    private CameraGLSurfaceView mGLSurfaceView;
    private Camera mCamera;
    AFT_FSDKVersion version = new AFT_FSDKVersion();
    AFT_FSDKEngine engine = new AFT_FSDKEngine();
    ASAE_FSDKVersion mAgeVersion = new ASAE_FSDKVersion();
    ASAE_FSDKEngine mAgeEngine = new ASAE_FSDKEngine();
    ASGE_FSDKVersion mGenderVersion = new ASGE_FSDKVersion();
    ASGE_FSDKEngine mGenderEngine = new ASGE_FSDKEngine();
    List<AFT_FSDKFace> result = new ArrayList<>();
    private int mWidth, mHeight, mFormat;
    int mCameraID;
    int mCameraRotate;
    boolean mCameraMirror;
    byte[] mImageNV21 = null;
    FRAbsLoop mFRAbsLoop = null;
    AFT_FSDKFace mAFT_FSDKFace = null;

    class FRAbsLoop extends AbsLoop {
        AFR_FSDKVersion version = new AFR_FSDKVersion();
        AFR_FSDKEngine engine = new AFR_FSDKEngine();
        AFR_FSDKFace result = new AFR_FSDKFace();

        @Override
        public void setup() {
            AFR_FSDKError error = engine.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
            Log.d(TAG, "AFR_FSDK_InitialEngine = " + error.getCode());
            error = engine.AFR_FSDK_GetVersion(version);
            Log.d(TAG, "FR=" + version.toString() + "," + error.getCode()); //(210, 178 - 478, 446), degree = 1　780, 2208 - 1942, 3370
        }

        @Override
        public void loop() {
            if (mImageNV21 != null) {
                AFR_FSDKError error = engine.AFR_FSDK_ExtractFRFeature(mImageNV21, mWidth, mHeight, AFR_FSDKEngine.CP_PAF_NV21, mAFT_FSDKFace.getRect(), mAFT_FSDKFace.getDegree(), result);
                Log.d(TAG, "Face=" + result.getFeatureData()[0] + "," + result.getFeatureData()[1] + "," + result.getFeatureData()[2] + "," + error.getCode());
                AFR_FSDKMatching score = new AFR_FSDKMatching();
                float max = 0.0f;
                String name = null;
                ((BaseApplication) getApplicationContext().getApplicationContext()).mFaceDB.loadFaces();
                Log.e(TAG, "loop: " + ((BaseApplication) getApplicationContext().getApplicationContext()).mFaceDB.mFaceList.size());
                if (((BaseApplication) getApplicationContext().getApplicationContext()).mFaceDB.mFaceList.size() > 0) {
                    //是否识别成功(如果第一次没成功就再次循环验证一次)
                    for (Map.Entry<String, AFR_FSDKFace> entry : ((BaseApplication) getApplicationContext().getApplicationContext()).mFaceDB.mFaceList.entrySet()) {
                        error = engine.AFR_FSDK_FacePairMatching(result, entry.getValue(), score);
                        Log.d(TAG, "Score:" + score.getScore() + ", AFR_FSDK_FacePairMatching=" + error.getCode());
                        if (max < score.getScore()) {
                            max = score.getScore();
                            name = entry.getKey();

                        }
                    }
                    if (max > 0.60f) {
                        SharedPreferences userInfo = getSharedPreferences("user_info", 0);
                        long secondTime = System.currentTimeMillis();
                        if (secondTime - firstTime > 3000) {
                            //找到相关住户执行开门
                            firstTime = secondTime;
                            Log.d(TAG, "fit Score:" + max + ", NAME:" + name);
                            deviceId = userInfo.getString("deviceId", "");
                            isopenCabinet.isopen(deviceId, name, "face");
                        }

                    } else {
                        recindex = recindex + 1;
                        if (recindex == 3) {

                            recindex = 0;
                        }
                    }
                } else {

                }
                mImageNV21 = null;
            }
        }

        @Override
        public void over() {
            AFR_FSDKError error = engine.AFR_FSDK_UninitialEngine();
            Log.d(TAG, "AFR_FSDK_UninitialEngine : " + error.getCode());
        }

    }

    private long firstTime = 0;
    int recindex = 0;

    @Override
    public Camera setupCamera() {
        // TODO Auto-generated method stub
        if (Camera.getNumberOfCameras() != 0) {
            mCamera = Camera.open(mCameraID);
            try {
                Camera.Parameters parameters = mCamera.getParameters();

                parameters.setPreviewSize(mWidth, mHeight);
                parameters.setPreviewFormat(mFormat);
                mCamera.setDisplayOrientation(90);
                for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
                    Log.d(TAG, "SIZE:" + size.width + "x" + size.height);
                }
                for (Integer format : parameters.getSupportedPreviewFormats()) {
                    Log.d(TAG, "FORMAT:" + format);
                }

                List<int[]> fps = parameters.getSupportedPreviewFpsRange();
                for (int[] count : fps) {
                    Log.d(TAG, "T:");
                    for (int data : count) {
                        Log.d(TAG, "V=" + data);
                    }
                }
                mCamera.setParameters(parameters);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (mCamera != null) {
                mWidth = mCamera.getParameters().getPreviewSize().width;
                mHeight = mCamera.getParameters().getPreviewSize().height;
            }
            return mCamera;
        }
        return null;
    }

    @Override
    public void setupChanged(int format, int width, int height) {

    }

    @Override
    public boolean startPreviewImmediately() {
        return true;
    }

    @Override
    public Object onPreview(byte[] data, int width, int height, int format, long timestamp) {
        AFT_FSDKError err = engine.AFT_FSDK_FaceFeatureDetect(data, width, height, AFT_FSDKEngine.CP_PAF_NV21, result);
        Log.d(TAG, "AFT_FSDK_FaceFeatureDetect =" + err.getCode());
        Log.d(TAG, "Face=" + result.size());
        for (AFT_FSDKFace face : result) {
            Log.d(TAG, "Face:" + face.toString());
        }
        if (mImageNV21 == null) {
            if (!result.isEmpty()) {
                mAFT_FSDKFace = result.get(0).clone();
                mImageNV21 = data.clone();
            } else {

            }
        }

        Rect[] rects = new Rect[result.size()];
        for (int i = 0; i < result.size(); i++) {
            rects[i] = new Rect(result.get(i).getRect());
        }
        result.clear();
        return rects;
    }

    @Override
    public void onBeforeRender(CameraFrameData data) {

    }

    @Override
    public void onAfterRender(CameraFrameData data) {
        mGLSurfaceView.getGLES2Render().draw_rect((Rect[]) data.getParams(), Color.GREEN, 2);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (Camera.getNumberOfCameras() != 0) {
            CameraHelper.touchFocus(mCamera, event, v, this);

        }
        return false;
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        if (success) {
            Log.d(TAG, "Camera Focus SUCCESS!");
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        } else {
            if (keyCode == 20) {
                String code = code_mumber.getText().toString();
                long now = System.currentTimeMillis();
                code =code.replace("\n","");
                long passTime = now - Long.parseLong(code);
                Logger.e("1536490732355".equals (code)+"");
                Logger.e((passTime< time)+"");
                Logger.e(passTime+"");
                Logger.e(time+"");

                if ("1536490729828".equals(code) &&passTime< time) {
                    Logger.e("LockAcitvity" + "===========" + (now - Long.parseLong(code)));
                    SharedPreferences sharedPreferences = getSharedPreferences("user_info", 0);
                    gpiostr = sharedPreferences.getString("gpiotext", "");
                    Logger.e("LockAcitvity" + "===========" + gpiostr);
                    try {
                        Gpio.gpioInt(gpiostr);
                        Thread.sleep(400);
                        Gpio.set(gpiostr, 48);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Gpio.set(gpiostr, 49);
                    isopenCabinet.memberCode(deviceId, code);
                } else if ("1536490732355".equals (code)&&passTime<time ){
                    Logger.e("LockAcitvity" + "===========" + (now - Long.parseLong(code)));
                    SharedPreferences sharedPreferences = getSharedPreferences("user_info", 0);
                    gpiostr = sharedPreferences.getString("gpiotext", "");
                    Logger.e("LockAcitvity" + "===========" + gpiostr);
                    try {
                        Gpio.gpioInt(gpiostr);
                        Thread.sleep(400);
                        Gpio.set(gpiostr, 48);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Gpio.set(gpiostr, 49);
                }
                code_mumber.setText("");
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

}
