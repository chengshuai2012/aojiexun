package com.link.cloud.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.link.cloud.R;
import com.link.cloud.constant.Constant;
import com.link.cloud.utils.ReservoirUtils;
import com.link.cloud.utils.Utils;
import com.orhanobut.logger.Logger;


/**
 * Created by Administrator on 2017/8/16.
 */
public class WelcomeActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐藏标题栏以及状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
         WindowManager.LayoutParams.FLAG_FULLSCREEN);
        /**标题是属于View的，所以窗口所有的修饰部分被隐藏后标题依然有效,需要去掉标题**/
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_welcome);
        //创建桌面快捷方式
        if (!Utils.shortcut2DesktopCreated()) {
            //Logger.e("创建桌面快捷方式");
            createShortcutToDesktop();
        }
        handler.sendEmptyMessageDelayed(0,2000);
        Log.e("onCreate: ",System.currentTimeMillis()+"" );
    }
//    MesReceiver mesReceiver;
    @Override
    protected void onStart() {
        super.onStart();
    }
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            getHome();
            super.handleMessage(msg);
        }
    };
    public void getHome(){
        Logger.e("WelcomeActivity"+"=====getHome====");
        Intent intent = new Intent(WelcomeActivity.this, LockActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
    }
    private void createShortcutToDesktop() {
        Intent intent = new Intent();
        intent.setClass(this, this.getClass());
        /*以下两句是为了在卸载应用的时候同时删除桌面快捷方式*/
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        // 不允许重建
        shortcut.putExtra("duplicate", false);
        // 设置名字
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, this.getString(R.string.app_name));
        // 设置图标
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher));
        // 设置意图和快捷方式关联程序
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
        sendBroadcast(shortcut);
        ReservoirUtils.getInstance().put(Constant.EXTRAS_SHORTCUT, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
