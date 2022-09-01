package com.st.http;

import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

/**
 * @author zhangtian1
 */
@Service
public class AsyncHttpClient extends BaseHttpClient implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncHttpClient.class);

    private static OkHttpClient okHttpClient;

    private static final okhttp3.MediaType JSON = okhttp3.MediaType.parse(APPLICATION_JSON_UTF8_VALUE);

    private static int connectTimeOut = 60000;

    /**
     * 读超时，单位：毫秒
     */
    private static int readTimeout = 60000;

    /**
     * 读超时，单位：毫秒
     */
    private static int writeTimeout = 60000;

    /**
     * 连接池大小
     */
    private static int maxIdleConnections = 20;

    /**
     * 可用空闲连接过期时间，单位：秒
     */
    private static int keepAliveDuration = 600;


    public static Headers jsonAcceptHeaders() {
        return new Headers.Builder().add(HttpHeaders.ACCEPT, APPLICATION_JSON_UTF8_VALUE).build();
    }

    public static Headers formContentHeaders() {
        return new Headers.Builder()
                .add(HttpHeaders.ACCEPT, APPLICATION_JSON_UTF8_VALUE)
                .add(HttpHeaders.CONTENT_TYPE, APPLICATION_FORM_URLENCODED_VALUE)
                .build();
    }

    @Override
    public void afterPropertiesSet() {
        ConnectionPool pool = new ConnectionPool(maxIdleConnections, keepAliveDuration, TimeUnit.SECONDS);
        OkHttpClient.Builder builder = createBuilder(true)
                .connectTimeout(connectTimeOut, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                .connectionPool(pool)
                .followRedirects(true)
                .retryOnConnectionFailure(true);
        okHttpClient = builder.build();
        LOGGER.info("AsyncHttpClient created");
    }

    private OkHttpClient.Builder createBuilder(boolean enableSslValidation) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (enableSslValidation) {
            try {
                X509TrustManager disabledTrustManager = new DisableValidationTrustManager();
                TrustManager[] trustManagers = new TrustManager[1];
                trustManagers[0] = disabledTrustManager;
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustManagers, new java.security.SecureRandom());
                SSLSocketFactory disabledSSLSocketFactory = sslContext.getSocketFactory();
                builder.sslSocketFactory(disabledSSLSocketFactory, disabledTrustManager);
                builder.hostnameVerifier((String s, SSLSession sslSession) -> true);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                LOGGER.warn("Error setting SSLSocketFactory in OKHttpClient", e);
            }
        }
        return builder;
    }

    public static class DisableValidationTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    @PreDestroy
    public void destroy() {
        if (okHttpClient != null) {
            LOGGER.info("Closing AsyncHttpClient...");
            okHttpClient.dispatcher().executorService().shutdown();
            okHttpClient.connectionPool().evictAll();
            LOGGER.info("AsyncHttpClient closed");
        }
    }

    public static Request buildPostRequest(String url, Headers headers) {
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            builder.headers(headers);
        }
        return builder.method("POST", emptyBody()).build();
    }

    public static Request buildPostRequest(String url, Headers headers, Map<String, String> params) {
        RequestBody body = null;
        if (params != null) {
            body = buildFormBody(params);
        } else {
            body = emptyBody();
        }
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            builder.headers(headers);
        }
        return builder.post(body).build();
    }

    public static Request buildPostRequest(String url, Headers headers, String json) {
        RequestBody body;
        if (StringUtils.isNotBlank(json)) {
            body = RequestBody.create(JSON, json);
        } else {
            body = emptyBody();
        }
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            builder.headers(headers);
        }
        return builder.post(body).build();
    }

    public static RequestBody buildFormBody(Map<String, String> params) {
        FormBody.Builder builder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    public static String extractBody(Response response) {
        String body = null;
        try {
            if (response.body() != null) {
                body = response.body().string();
            }
        } catch (IOException e) {
            LOGGER.error("Fail to extract body", e);
            throw new RuntimeException(e.getMessage());
        }
        return body;
    }

    private static RequestBody emptyBody() {
        return RequestBody.create(null, new byte[0]);
    }

    public static Response execute(Request request) {
        try {
            LOGGER.info("executing sync request [{}]", request.toString());
            Response response = okHttpClient.newCall(request).execute();
            LOGGER.info("get response [{}]", response.toString());
            return response;
        } catch (IOException e) {
            LOGGER.error("Fail to execute request with OkHttpClient", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public static String post(String url) {
        Request request = buildPostRequest(url, null);
        return extractBody(execute(request));
    }

    public static String post(String url, Headers headers) {
        Request request = buildPostRequest(url, headers);
        return extractBody(execute(request));
    }

    public static String post(String url, Map<String, String> params) {
        Request request = buildPostRequest(url, null, params);
        return extractBody(execute(request));
    }

    public static String post(String url, Headers headers, Map<String, String> params) {
        Request request = buildPostRequest(url, headers, params);
        return extractBody(execute(request));
    }

    public static String post(String url, String json) {
        Request request = buildPostRequest(url, null, json);
        return extractBody(execute(request));
    }

    public static String post(String url, Headers headers, String json) {
        Request request = buildPostRequest(url, headers, json);
        return extractBody(execute(request));
    }

    public static Request buildGetRequest(String url, Headers headers, Map<String, String> params) {
        String uri = parse(url, params);
        Request.Builder builder = new Request.Builder().url(uri);
        if (headers != null) {
            builder.headers(headers);
        }
        return builder.get().build();
    }

    public static String get(String url) {
        Request request = buildGetRequest(url, null, null);
        return extractBody(execute(request));
    }

    public static String get(String url, Map<String, String> params) {
        Request request = buildGetRequest(url, null, params);
        return extractBody(execute(request));
    }

    public static String get(String url, Headers headers, Map<String, String> params) {
        Request request = buildGetRequest(url, headers, params);
        return extractBody(execute(request));
    }


    public static Request buildPutRequest(String url, Headers headers) {
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            builder.headers(headers);
        }
        return builder.method("PUT", emptyBody()).build();
    }

    public static Request buildPutRequest(String url, Headers headers, String json) {
        RequestBody body;
        if (StringUtils.isNotBlank(json)) {
            body = RequestBody.create(JSON, json);
        } else {
            body = emptyBody();
        }
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            builder.headers(headers);
        }
        return builder.put(body).build();
    }

    public static Request buildPutRequest(String url, Headers headers, Map<String, String> params) {
        RequestBody body = null;
        if (params != null) {
            body = buildFormBody(params);
        } else {
            body = emptyBody();
        }
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            builder.headers(headers);
        }
        return builder.put(body).build();
    }

    public static String put(String url) {
        Request request = buildPutRequest(url, null);
        return extractBody(execute(request));
    }

    public static String put(String url, Headers headers) {
        Request request = buildPutRequest(url, headers);
        return extractBody(execute(request));
    }

    public static String put(String url, Map<String, String> params) {
        Request request = buildPutRequest(url, null, params);
        return extractBody(execute(request));
    }

    public static String put(String url, Headers headers, Map<String, String> params) {
        Request request = buildPutRequest(url, headers, params);
        return extractBody(execute(request));
    }

    public static String put(String url, String json) {
        Request request = buildPutRequest(url, null, json);
        return extractBody(execute(request));
    }

    public static String put(String url, Headers headers, String json) {
        Request request = buildPutRequest(url, headers, json);
        return extractBody(execute(request));
    }
    
}

