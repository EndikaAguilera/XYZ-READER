package com.example.xyzreader.async;

import java.util.List;

/**
 * Created by thisobeystudio on 11/4/18.
 * Copyright: (c) 2018 ThisObey Studio
 * Contact: thisobeystudio@gmail.com
 */
public interface SplitBookCallback {
    List<CharSequence> splitBookDoInBackground();

    void splitBookOnPostExecute(List<CharSequence> charSequences);

}
