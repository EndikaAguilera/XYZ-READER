package com.example.xyzreader;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.example.xyzreader.okhttp.OkHttpUrlLoader;

import java.io.InputStream;

/**
 * Created by thisobeystudio on 24/10/17.
 * Copyright: (c) 2017 ThisObey Studio
 * Contact: thisobeystudio@gmail.com
 */

@SuppressWarnings("WeakerAccess")
@GlideModule
public final class MyAppGlideModule extends AppGlideModule {

    /**
     * Manifest Parsing:
     * <p>
     * To maintain backward compatibility with Glide v3’s GlideModules,
     * Glide still parses AndroidManifest.xml files from both the application and
     * any included libraries and will include any legacy GlideModules listed in the manifest.
     * Although this functionality will be removed in a future version,
     * we’ve retained the behavior for now to ease the transition.
     * <p>
     * If you’ve already migrated to the Glide v4 AppGlideModule and LibraryGlideModule,
     * you can disable manifest parsing entirely. Doing so can improve the initial startup time of
     * Glide and avoid some potential problems with trying to parse metadata.
     * To disable manifest parsing, override the isManifestParsingEnabled()
     * method in your AppGlideModule implementation:
     *
     * @return false
     */
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory());
    }

}