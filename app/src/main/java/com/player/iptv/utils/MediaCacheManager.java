package com.player.iptv.utils;

import android.content.Context;

import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import java.io.File;

@UnstableApi
public class MediaCacheManager {

    private static MediaCacheManager instance;
    private final SimpleCache simpleCache;
    private final CacheDataSource.Factory cacheDataSourceFactory;

    private static final long MAX_CACHE_SIZE = 200 * 1024 * 1024;

    private MediaCacheManager(Context context) {
        File cacheDir = new File(context.getCacheDir(), "media_cache");

        LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE);
        simpleCache = new SimpleCache(cacheDir, evictor);

        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("PremiumIPTV")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(30000);

        cacheDataSourceFactory = new CacheDataSource.Factory()
                .setCache(simpleCache)
                .setUpstreamDataSourceFactory(new DefaultDataSource.Factory(context, httpFactory))
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    public static synchronized MediaCacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new MediaCacheManager(context.getApplicationContext());
        }
        return instance;
    }

    public CacheDataSource.Factory getCacheDataSourceFactory() {
        return cacheDataSourceFactory;
    }

    public SimpleCache getCache() {
        return simpleCache;
    }

    public void release() {
        try {
            simpleCache.release();
        } catch (Exception ignored) {
        }
        instance = null;
    }
}
