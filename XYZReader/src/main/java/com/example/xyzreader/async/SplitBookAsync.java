package com.example.xyzreader.async;

import android.os.AsyncTask;

import java.util.List;

/**
 * Created by thisobeystudio on 11/4/18.
 * Copyright: (c) 2018 ThisObey Studio
 * Contact: thisobeystudio@gmail.com
 */
public class SplitBookAsync extends AsyncTask<Void, Void, List<CharSequence>> {

    public SplitBookCallback delegate = null;

    @Override
    protected List<CharSequence> doInBackground(Void... voids) {
        if (delegate == null) return null;
        return delegate.splitBookDoInBackground();
    }

    @Override
    protected void onPostExecute(List<CharSequence> charSequences) {
        if (delegate == null) return;
        delegate.splitBookOnPostExecute(charSequences);
    }
}
