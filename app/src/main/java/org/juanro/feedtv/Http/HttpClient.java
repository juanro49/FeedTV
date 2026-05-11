package org.juanro.feedtv.Http;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Security;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.Cache;
import okhttp3.ConnectionPool;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.brotli.BrotliInterceptor;
import okhttp3.dnsoverhttps.DnsOverHttps;
import okhttp3.logging.HttpLoggingInterceptor;
import org.conscrypt.Conscrypt;
import org.juanro.feedtv.BuildConfig;

/**
 * Cliente HTTP con DNS over HTTPS inteligente y resiliente.
 */
public class HttpClient {
    private static final String TAG = "HttpClient";
    private static volatile OkHttpClient instance;
    private static OkHttpClient bootstrapClient;
    
    private static final long CACHE_SIZE = 15 * 1024 * 1024; // 15 MB
    private static final String DEFAULT_DOH_PRIMARY = "https://dns.rocksdns.ovh/dns-query";
    private static final String DEFAULT_DOH_SECONDARY = "https://dns2.rocksdns.ovh/dns-query";

    @NonNull
    public static OkHttpClient getInstance() {
        if (instance == null) {
            throw new RuntimeException("HttpClient must be initialized in Application.onCreate()");
        }
        return instance;
    }

    public static void init(@NonNull Context context) {
        if (instance != null) return;

        synchronized (HttpClient.class) {
            if (instance == null) {
                // 1. Motor SSL Moderno
                try {
                    Security.insertProviderAt(Conscrypt.newProvider(), 1);
                } catch (Throwable e) {
                    Log.w(TAG, "Conscrypt fallback");
                }

                // 2. Bootstrap client para DoH
                bootstrapClient = new OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
                        .build();

                OkHttpClient.Builder builder = new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(20, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .connectionPool(new ConnectionPool(10, 3, TimeUnit.MINUTES))
                        .followRedirects(true)
                        .followSslRedirects(true)
                        .retryOnConnectionFailure(true)
                        .addInterceptor(BrotliInterceptor.INSTANCE);

                // Interceptor para User-Agent propio de la aplicación
                builder.addInterceptor(chain -> {
                    okhttp3.Request original = chain.request();
                    okhttp3.Request request = original.newBuilder()
                            .header("User-Agent", "FeedTV/" + BuildConfig.VERSION_NAME + " (Android)")
                            .header("Accept", "application/json, text/plain, */*")
                            .build();
                    return chain.proceed(request);
                });

                // 3. DNS Inteligente
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if (prefs.getBoolean("doh_enabled", true)) {
                    if (isSystemPrivateDnsActive(context)) {
                        Log.i(TAG, "Sistema DNS activo");
                    } else {
                        String userUrl = prefs.getString("doh_url", DEFAULT_DOH_PRIMARY);
                        // Warning arreglado: userUrl nunca será null aquí
                        if (userUrl.isEmpty()) userUrl = DEFAULT_DOH_PRIMARY;
                        builder.dns(createResilientDns(userUrl));
                    }
                }

                // 4. Caché e Interceptores
                try {
                    File cacheDir = new File(context.getCacheDir(), "http_cache");
                    builder.cache(new Cache(cacheDir, CACHE_SIZE));
                } catch (Exception e) {
                    Log.e(TAG, "Cache error", e);
                }

                // Interceptor para caché offline
                builder.addInterceptor(chain -> {
                    okhttp3.Request request = chain.request();
                    if (!isNetworkAvailable(context)) {
                        request = request.newBuilder()
                                .header("Cache-Control", "public, only-if-cached, max-stale=" + (60 * 60 * 24 * 7)) // 1 semana
                                .build();
                    }
                    return chain.proceed(request);
                });

                if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
                    builder.addInterceptor(logging);
                }

                instance = builder.build();
            }
        }
    }

    private static Dns createResilientDns(String primaryUrl) {
        DnsOverHttps doh1 = new DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url(HttpUrl.get(primaryUrl))
                .includeIPv6(false) // Desactivado para evitar ENETUNREACH en redes sin IPv6
                .build();

        DnsOverHttps doh2 = primaryUrl.equals(DEFAULT_DOH_SECONDARY) ? null :
                new DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url(HttpUrl.get(DEFAULT_DOH_SECONDARY))
                .includeIPv6(false) // Desactivado para evitar ENETUNREACH en redes sin IPv6
                .build();

        return new Dns() {
            @NonNull
            @Override
            public List<InetAddress> lookup(@NonNull String hostname) throws UnknownHostException {
                try {
                    return doh1.lookup(hostname);
                } catch (UnknownHostException e) {
                    if (doh2 != null) {
                        try { return doh2.lookup(hostname); } 
                        catch (UnknownHostException ignored) {}
                    }
                }
                return Dns.SYSTEM.lookup(hostname);
            }
        };
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network activeNetwork = cm.getActiveNetwork();
        return activeNetwork != null;
    }

    private static boolean isSystemPrivateDnsActive(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) return false;
            LinkProperties lp = cm.getLinkProperties(activeNetwork);
            return lp != null && lp.getPrivateDnsServerName() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
