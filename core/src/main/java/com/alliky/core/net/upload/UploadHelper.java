package com.alliky.core.net.upload;

import com.alliky.core.config.ConfigKeys;
import com.alliky.core.config.Kylin;
import com.alliky.core.ex.HttpException;
import com.alliky.core.net.HttpCreator;
import com.alliky.core.net.Utils;
import com.alliky.core.net.callback.ICancelled;
import com.alliky.core.net.callback.IError;
import com.alliky.core.net.callback.IFinished;
import com.alliky.core.net.callback.ILoading;
import com.alliky.core.net.callback.IStarted;
import com.alliky.core.net.callback.ISuccess;
import com.alliky.core.net.callback.IWaiting;
import com.alliky.core.net.common.Callback;
import com.alliky.core.net.params.RequestParams;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.WeakHashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @Description: DOTO
 * @Author: wxianing
 * @CreateDate: 2019/12/6 14:48
 */
public class UploadHelper {

    private final String URL;
    private static final WeakHashMap<String, Object> PARAMS = HttpCreator.getParams();
    private static final WeakHashMap<String, String> HEADERS = HttpCreator.getHeaders();

    private final ISuccess SUCCESS;
    private final ICancelled CANCELLED;
    private final IFinished FINISHED;
    private final IWaiting WAITING;
    private final IStarted STARTED;
    private final IError ERROR;
    private final ILoading LOADING;

    public UploadHelper(String URL, WeakHashMap<String, Object> PARAM, WeakHashMap<String, String> HEADER, ISuccess SUCCESS, ICancelled CANCELLED, IFinished FINISHED, IWaiting WAITING, IStarted STARTED, IError ERROR, ILoading LOADING) {
        this.URL = URL;
        this.SUCCESS = SUCCESS;
        this.CANCELLED = CANCELLED;
        this.FINISHED = FINISHED;
        this.WAITING = WAITING;
        this.STARTED = STARTED;
        this.ERROR = ERROR;
        this.LOADING = LOADING;
        PARAMS.putAll(PARAM);
        HEADERS.putAll(HEADER);
    }

    /**
     * 上传文件
     */
    public void upLoadFile() {

        String url = Kylin.getConfiguration(ConfigKeys.API_HOST) + URL;

        RequestParams params = new RequestParams(url);

        if (null != PARAMS) {
            for (Map.Entry<String, Object> entry : PARAMS.entrySet()) {
                params.addParameter(entry.getKey(), entry.getValue());
            }
        }

        if (null != HEADERS) {
            for (Map.Entry<String, String> entry : HEADERS.entrySet()) {
                params.addHeader(entry.getKey(),entry.getValue());
            }
        }

        params.setMultipart(true);

        //判断请求是否为https
        if (url.trim().toLowerCase().startsWith("https:")) {
            /** 判断https证书是否成功验证 */
            SSLContext sslContext = getSSLContext();
            if (null == sslContext) {
                return;
            }
            params.setSslSocketFactory(sslContext.getSocketFactory()); //绑定SSL证书(https请求)
        }

        Utils.http().post(params, new Callback.ProgressCallback<String>() {
            @Override
            public void onWaiting() {
                WAITING.onWaiting();
            }

            @Override
            public void onStarted() {
                STARTED.onStarted();
            }

            @Override
            public void onLoading(long total, long current, boolean isDownloading) {
                LOADING.onLoading(total, current);
            }

            @Override
            public void onSuccess(String result) {
                SUCCESS.onSuccess(result);
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                if (ex instanceof HttpException) {//网络异常
                    HttpException httpException = (HttpException) ex;
                    int code = httpException.getCode();
                    String msg = httpException.getMessage();

                    ERROR.onError(code, msg);
                }
            }

            @Override
            public void onCancelled(CancelledException cex) {
                CANCELLED.onCancelled(cex);
            }

            @Override
            public void onFinished() {
                FINISHED.onFinished();
                PARAMS.clear();
            }
        });
    }

    /**
     * 获取Https的证书
     *
     * @return SSL的上下文对象
     */
    private static SSLContext getSSLContext() {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            SSLContext sslContext = SSLContext.getInstance("TLS");
            //信任所有证书 （官方不推荐使用）
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }
            }}, new SecureRandom());
            return sslContext;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
