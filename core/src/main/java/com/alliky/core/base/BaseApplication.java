package com.alliky.core.base;

import android.app.Application;

import com.alliky.core.net.Utils;

/**
 * @Description: DOTO
 * @Author: wxianing
 * @CreateDate: 2019/9/29 17:33
 */
public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.Ext.init(this);
    }
}
