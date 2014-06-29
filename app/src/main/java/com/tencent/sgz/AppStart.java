package com.tencent.sgz;

import java.io.File;
import java.util.List;

import com.tencent.sgz.common.FileUtils;
import com.tencent.sgz.common.OpenQQHelper;
import com.tencent.sgz.common.StringUtils;
import com.tencent.sgz.entity.AppData;
import com.tencent.sgz.ui.Main;
import com.tencent.sgz.ui.MainActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * 应用程序启动类：1,显示欢迎界面并跳转到主界面 2,加载数据包
 * @author lv (http://t.qq.com/badstyle)
 * @version 1.0
 * @created 2014-5-11
 */
public class AppStart extends Activity {
    
	private static final String TAG  = AppStart.class.getName();
	AppContext ac = null;
	boolean isRedirecting;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ac = AppContext.Instance;

        // 初始化登录
        OpenQQHelper.attachTo(this);
        ac.initLoginInfo();
       
        final View view = View.inflate(this, R.layout.start, null);
		FrameLayout wellcome = (FrameLayout) view.findViewById(R.id.app_start_view);
		check(wellcome);
		setContentView(view);
        
		//渐变展示启动屏
		final AlphaAnimation aa = new AlphaAnimation(0.3f,1.0f);
		aa.setDuration(2000);
		view.startAnimation(aa);
		aa.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationEnd(Animation arg0) {
				//redirectTo();

                if(ac.getData()!=null){
                    Log.e(TAG,"AppData loaded and startup animation ended, preparing redirecting...");
                    redirectTo();
                }

			}
			@Override
			public void onAnimationRepeat(Animation animation) {}
			@Override
			public void onAnimationStart(Animation animation) {}
			
		});
		
		//兼容低版本cookie（1.5版本以下，包括1.5.0,1.5.1）
		String cookie = ac.getProperty("cookie");
		if(StringUtils.isEmpty(cookie)) {
			String cookie_name = ac.getProperty("cookie_name");
			String cookie_value = ac.getProperty("cookie_value");
			if(!StringUtils.isEmpty(cookie_name) && !StringUtils.isEmpty(cookie_value)) {
				cookie = cookie_name + "=" + cookie_value;
                ac.setProperty("cookie", cookie);
                ac.removeProperty("cookie_domain","cookie_name","cookie_value","cookie_version","cookie_path");
			}
		}

        final Handler onAppDataGot = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle data = msg.getData();
                int errCode = data.getInt("errCode");
                String errMsg = data.getString("errMsg");
                ac.setData((AppData)data.getSerializable("data"));

                Log.e(TAG,"AppData loaded, "+(aa.hasEnded()?"startup animation ended,let's do redirect.":"startup animation running..."));

                if(aa.hasEnded()){
                    redirectTo();
                }

            }
        };

        //初始化数据
        AppDataProvider.getAppData(ac,onAppDataGot , false);
    }
    
    /**
     * 检查是否需要换图片
     * @param view
     */
    private void check(FrameLayout view) {
    	String path = FileUtils.getAppCache(this, "wellcomeback");
    	List<File> files = FileUtils.listPathFiles(path);
    	if (!files.isEmpty()) {
    		File f = files.get(0);
    		long time[] = getTime(f.getName());
    		long today = StringUtils.getToday();
    		if (today >= time[0] && today <= time[1]) {
    			view.setBackgroundDrawable(Drawable.createFromPath(f.getAbsolutePath()));
    		}
    	}
    }
    
    /**
     * 分析显示的时间
     * @param time
     * @return
     */
    private long[] getTime(String time) {
    	long res[] = new long[2];
    	try {
    		time = time.substring(0, time.indexOf("."));
        	String t[] = time.split("-");
        	res[0] = Long.parseLong(t[0]);
        	if (t.length >= 2) {
        		res[1] = Long.parseLong(t[1]);
        	} else {
        		res[1] = Long.parseLong(t[0]);
        	}
		} catch (Exception e) {
		}
    	return res;
    }
    
    /**
     * 跳转到...
     */
    private void redirectTo(){
        if(isRedirecting) return;
        isRedirecting = true;
        Intent intent = new Intent(this, MainActivity.class);
        //Intent intent = new Intent(this, Main.class);
        startActivity(intent);
        finish();
    }
}