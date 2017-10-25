package com.example.xyzreader.ui;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.GlideApp;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.support.v4.app.ActivityOptionsCompat.makeSceneTransitionAnimation;
import static com.example.xyzreader.ui.ArticleDetailActivity.ARG_ITEM_ID;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleListActivity.class.toString();

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @BindView(R.id.on_refresh_disable_interaction_background_helper)
    View mOnRefreshBackgroundView;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @BindView(R.id.main_recycler_view)
    RecyclerView mRecyclerView;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss",
            Locale.getDefault());
    // Use default locale format
    @SuppressLint("SimpleDateFormat")
    private final SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private final GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        ButterKnife.bind(this);

        Toolbar mToolbar = findViewById(R.id.toolbar);

        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        setTitle(""); // remove toolbar title

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        mOnRefreshBackgroundView.setOnClickListener(null); // disable interactions
        mOnRefreshBackgroundView.setVisibility(View.GONE); // disable interactions

        if (savedInstanceState == null) {
            refresh();
        }

        getSupportLoaderManager().initLoader(0, null, this);

    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private final BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {

        if (mIsRefreshing) mOnRefreshBackgroundView.setVisibility(View.VISIBLE);
        else mOnRefreshBackgroundView.setVisibility(View.GONE);

        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Adapter adapter = new Adapter(data);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mRecyclerView != null)
            mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ArticleViewHolder> {
        private final Cursor mCursor;

        Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ArticleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ArticleViewHolder vh = new ArticleViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setOnItemClick(vh);
                }
            });
            return vh;
        }

        private void setOnItemClick(ArticleViewHolder holder) {
            View statusBar = findViewById(android.R.id.statusBarBackground);

            Pair<View, String> p0 =
                    Pair.create(statusBar,
                            Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME);
            Pair<View, String> p2 =
                    Pair.create((View) holder.thumbnailView,
                            holder.thumbnailView.getTransitionName());

            final Pair[] pairs = new Pair[]{
                    p0, p2
            };

            @SuppressWarnings("unchecked") ActivityOptionsCompat options =
                    makeSceneTransitionAnimation(ArticleListActivity.this, pairs);

            Intent intent = new Intent(Intent.ACTION_VIEW,
                    ItemsContract.Items.buildItemUri(getItemId(holder.getAdapterPosition())));
            intent.putExtra(ARG_ITEM_ID, getItemId(holder.getAdapterPosition()));
            startActivity(intent, options.toBundle());
        }

        private Date parsePublishedDate() {
            try {
                String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(ArticleViewHolder holder, int position) {
            mCursor.moveToPosition(position);

            holder.titleView.setTypeface(getSerifTypeFace());
            holder.subtitleView.setTypeface(getAppCustomTypeface(ArticleListActivity.this));

            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                holder.subtitleView.setText(DateUtils.getRelativeTimeSpanString(
                        publishedDate.getTime(),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString()
                        + "\nby "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR));
            } else {
                holder.subtitleView.setText(outputFormat.format(publishedDate)
                        + "\nby "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR));
            }

            GlideApp.with(ArticleListActivity.this)
                    .load(Uri.parse(mCursor.getString(ArticleLoader.Query.THUMB_URL)))
                    //.centerCrop()
                    //.circleCrop()
                    .override(Target.SIZE_ORIGINAL)
                    .into(holder.thumbnailView);

        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    static class ArticleViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.thumbnail)
        ImageView thumbnailView;
        @BindView(R.id.article_title)
        TextView titleView;
        @BindView(R.id.article_subtitle)
        TextView subtitleView;

        ArticleViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    // APP CUSTOM FONTS
    private static Typeface mSerifTypeface = null;

    public static Typeface getSerifTypeFace() {
        if (ArticleListActivity.mSerifTypeface == null)
            ArticleListActivity.mSerifTypeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL);
        return ArticleListActivity.mSerifTypeface;
    }

    private static Typeface mCustomTypeface = null;

    public static Typeface getAppCustomTypeface(Context context) {
        if (ArticleListActivity.mCustomTypeface == null)
            ArticleListActivity.mCustomTypeface = Typeface.createFromAsset(
                    context.getResources().getAssets(),
                    "Montserrat-Regular.ttf");
        return mCustomTypeface;
    }

}
