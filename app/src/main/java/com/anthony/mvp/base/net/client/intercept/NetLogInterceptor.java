package com.anthony.mvp.base.net.client.intercept;

import android.text.TextUtils;

import com.anthony.mvp.R;
import com.anthony.mvp.base.BaseApplication;
import com.anthony.mvp.base.constant.Constant;
import com.orhanobut.logger.Logger;

import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/**
 * 创建时间:2019/8/6
 * 创建人：anthony.wang
 * 功能描述：该拦截器主要复制打印请求参数和响应参数等信息 方便开发者调试
 */
public class NetLogInterceptor implements Interceptor {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private boolean isOpenLog = true;

    public void setOpenLog(boolean openLog) {
        isOpenLog = openLog;
    }
    public NetLogInterceptor(){}
    public NetLogInterceptor(boolean isOpenLog){
        this.isOpenLog = isOpenLog;
    }

    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        String method = request.method();
        HttpUrl requestUrl = request.url();
        RequestBody requestBody = request.body();

        if(isOpenLog){
            if (requestBody != null) {
                Buffer bufferRequest = new Buffer();
                requestBody.writeTo(bufferRequest);
                logRetrofitRequest(
                        method,
                        requestUrl == null ? null : requestUrl.toString(),
                        bufferRequest.readString(UTF8)
                );
            } else {
                logRetrofitRequest(
                        method,
                        requestUrl == null ? null : requestUrl.toString(),
                        BaseApplication.getApplication().getResources().getString(
                                R.string.no_request_body
                        )
                );
            }
        }


        Response response = chain.proceed(request);

        ResponseBody responseBody = response.body();
        long contentLength = responseBody.contentLength();

        if (bodyEncoded(response.headers())) {
            //HTTP (encoded body omitted)
        } else {
            BufferedSource source = responseBody.source();
            source.request(Long.MAX_VALUE);
            Buffer buffer = source.buffer();

            Charset charset = UTF8;
            MediaType contentType = responseBody.contentType();
            if (contentType != null) {
                try {
                    charset = contentType.charset(UTF8);
                } catch (UnsupportedCharsetException e) {
                    return response;
                }
            }

            if (!isPlaintext(buffer)) {
                return response;
            }
            if (contentLength != 0&&isOpenLog) {
                String result = buffer.clone().readString(charset);
                logRetrofitResponseSuccess(
                        response.request().url().toString(),
                        result
                );
            }

        }
        return response;
    }
    public static void logRetrofitRequest(
            String method,
            String url,
            String request
    ) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        if (TextUtils.isEmpty(request)) {
            return;
        }
        // FORMAT STRING
        String requestText = String.format(
                Constant.NET_REQUEST_STRING,
                method,
                url,
                request
        );
        // LOG
        Logger.t(Constant.NET_LOG_TAG).d(requestText);
    }
    public static void logRetrofitResponseSuccess(
            String url,
            String response
    ) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        if (TextUtils.isEmpty(response)) {
            return;
        }
        // FORMAT STRING
        String responseText = String.format(
                Constant.NET_RESPONSE_SUCESS_STRING,
                url,
                response
        );
        // LOG
        Logger.t(Constant.NET_LOG_TAG).d(responseText);
    }
    static boolean isPlaintext(Buffer buffer) {
        try {
            Buffer prefix = new Buffer();
            long byteCount = buffer.size() < 64 ? buffer.size() : 64;
            buffer.copyTo(prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (EOFException e) {
            return false;
        }
    }

    private boolean bodyEncoded(Headers headers) {
        String contentEncoding = headers.get("Content-Encoding");
        return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
    }
}