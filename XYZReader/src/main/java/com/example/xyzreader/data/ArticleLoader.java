package com.example.xyzreader.data;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.CursorLoader;

import static com.example.xyzreader.data.ArticleLoader.Query.ALL_ARTICLE_ITEMS_PROJECTION;
import static com.example.xyzreader.data.ArticleLoader.Query.MAIN_PROJECTION;

/**
 * Helper for loading a list of articles or a single article.
 */
public class ArticleLoader extends CursorLoader {
    public static ArticleLoader newAllArticlesInstance(Context context) {
        return new ArticleLoader(context, ItemsContract.Items.buildDirUri(), MAIN_PROJECTION);
    }

    public static ArticleLoader newInstanceForItemId(Context context, long itemId) {
        return new ArticleLoader(context,
                ItemsContract.Items.buildItemUri(itemId),
                ALL_ARTICLE_ITEMS_PROJECTION);
    }

    private ArticleLoader(Context context, Uri uri, String[] query) {
        super(context, uri, query, null, null, ItemsContract.Items.DEFAULT_SORT);
    }

    public interface Query {
        String[] ALL_ARTICLE_ITEMS_PROJECTION = {
                ItemsContract.Items._ID,
                ItemsContract.Items.TITLE,
                ItemsContract.Items.PUBLISHED_DATE,
                ItemsContract.Items.AUTHOR,
                ItemsContract.Items.THUMB_URL,
                ItemsContract.Items.PHOTO_URL,
                ItemsContract.Items.ASPECT_RATIO,
                ItemsContract.Items.BODY
        };

        String[] MAIN_PROJECTION = {
                ItemsContract.Items._ID,
                ItemsContract.Items.TITLE,
                ItemsContract.Items.PUBLISHED_DATE,
                ItemsContract.Items.AUTHOR,
                ItemsContract.Items.THUMB_URL,
        };

        int _ID = 0;
        int TITLE = 1;
        int PUBLISHED_DATE = 2;
        int AUTHOR = 3;
        int THUMB_URL = 4;
        @SuppressWarnings("unused")
        int PHOTO_URL = 5;
        @SuppressWarnings("unused")
        int ASPECT_RATIO = 6;
        int BODY = 7;
    }
}
