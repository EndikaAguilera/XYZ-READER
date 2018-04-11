package com.example.xyzreader.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.GlideApp;
import com.example.xyzreader.R;
import com.example.xyzreader.async.SplitBookAsync;
import com.example.xyzreader.async.SplitBookCallback;
import com.example.xyzreader.custom.PageSplitter;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.xyzreader.ui.ArticleListActivity.getAppCustomTypeface;
import static com.example.xyzreader.ui.ArticleListActivity.getSerifTypeFace;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        AppBarLayout.OnOffsetChangedListener,
        SplitBookCallback {

    private static final String TAG = ArticleDetailActivity.class.getSimpleName();

    public static final String ARG_ITEM_ID = "item_id";

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @BindView(R.id.app_bar)
    AppBarLayout mDetailsAppbar;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @BindView(R.id.detail_collapsing_toolbar)
    CollapsingToolbarLayout mCollapsingToolbar;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @BindView(R.id.details_toolbar)
    Toolbar mToolbar;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @BindView(R.id.article_subtitle)
    TextView mByLineView;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @BindView(R.id.body_view_pager)
    ViewPager mViewPager;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @BindView(R.id.pages_counter_text_view)
    TextView mPageCounterView;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @BindView(R.id.fab)
    FloatingActionButton fab;

    private SectionsPagerAdapter adapter;

    private Cursor mCursor;
    private long mItemId;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss",
            Locale.getDefault());
    // Use default locale format
    @SuppressLint("SimpleDateFormat")
    private final SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private final GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    private SplitBookAsync mLoadTask = null;

    private Animation initialAnim;
    private Animation showAnim;

    private boolean isInitAnim = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        ButterKnife.bind(this);

        getWindow().setStatusBarColor(Color.TRANSPARENT);

        // Postpone the transition until the window's decor view has
        // finished its layout.
        postponeEnterTransition();

        final View decor = getWindow().getDecorView();
        decor.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                decor.getViewTreeObserver().removeOnPreDrawListener(this);
                startPostponedEnterTransition();
                return true;
            }
        });

        getWindow().setSharedElementEnterTransition(TransitionInflater
                .from(this).inflateTransition(R.transition.curve));

        getWindow().getSharedElementEnterTransition().addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
                if (isInitAnim) {
                    setInitialAnim();
                    isInitAnim = false;
                } else animHideAll();
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                animShowAll();
            }

            @Override
            public void onTransitionCancel(Transition transition) {
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }
        });

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null && extras.containsKey(ARG_ITEM_ID)) {
            mItemId = extras.getLong(ARG_ITEM_ID);
        }

        getSupportLoaderManager().initLoader(0, null, ArticleDetailActivity.this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mDetailsAppbar.addOnOffsetChangedListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDetailsAppbar.removeOnOffsetChangedListener(this);
        if (mLoadTask != null && !mLoadTask.isCancelled()) {
            mLoadTask.cancel(true);
        }
    }

    private void setInitialAnim() {

        mCollapsingToolbar.setTitleEnabled(false);

        // clear animations
        mToolbar.clearAnimation();
        mByLineView.clearAnimation();
        mPageCounterView.clearAnimation();
        fab.clearAnimation();

        // start animations
        initialAnim = AnimationUtils.loadAnimation(ArticleDetailActivity.this, R.anim.initial_anim);
        mToolbar.startAnimation(initialAnim);
        initialAnim = AnimationUtils.loadAnimation(ArticleDetailActivity.this, R.anim.initial_anim);
        mByLineView.startAnimation(initialAnim);
        initialAnim = AnimationUtils.loadAnimation(ArticleDetailActivity.this, R.anim.initial_anim);
        mPageCounterView.startAnimation(initialAnim);
        initialAnim = AnimationUtils.loadAnimation(ArticleDetailActivity.this, R.anim.initial_anim);
        fab.startAnimation(initialAnim);
/*
        View v = makeCollapsingToolbarLayoutLooksGood(mCollapsingToolbar);
        if (v == null) return;
        initialAnim = AnimationUtils.loadAnimation(ArticleDetailActivity.this, R.anim.initial_anim);
        v.startAnimation(initialAnim);
*/
    }

    private void animHideAll() {
        // clear animations

        mCollapsingToolbar.setTitleEnabled(false);

        mToolbar.clearAnimation();
        mByLineView.clearAnimation();
        mPageCounterView.clearAnimation();
        fab.clearAnimation();

        // start animations
        Animation hideAnim;
        hideAnim = AnimationUtils.loadAnimation(ArticleDetailActivity.this, R.anim.hide_anim);
        mToolbar.startAnimation(hideAnim);
        hideAnim = AnimationUtils.loadAnimation(ArticleDetailActivity.this, R.anim.hide_anim);
        mByLineView.startAnimation(hideAnim);
        hideAnim = AnimationUtils.loadAnimation(ArticleDetailActivity.this, R.anim.hide_anim);
        mPageCounterView.startAnimation(hideAnim);
        hideAnim = AnimationUtils.loadAnimation(ArticleDetailActivity.this, R.anim.hide_anim);
        fab.startAnimation(hideAnim);

  /*      View v = makeCollapsingToolbarLayoutLooksGood(mCollapsingToolbar);
        if (v == null) return;
        hideAnim = AnimationUtils.loadAnimation(ArticleDetailActivity.this, R.anim.hide_anim);
        v.startAnimation(hideAnim);
    */
    }

    private void animShowAll() {
        // clear animations

        mCollapsingToolbar.setTitleEnabled(true);

        mToolbar.clearAnimation();
        mByLineView.clearAnimation();
        mPageCounterView.clearAnimation();
        fab.clearAnimation();

        // start animations
        showAnim = AnimationUtils.loadAnimation(ArticleDetailActivity.this, R.anim.show_anim);
        mToolbar.startAnimation(showAnim);
        showAnim = AnimationUtils.loadAnimation(ArticleDetailActivity.this, R.anim.show_anim);
        mByLineView.startAnimation(showAnim);
        showAnim = AnimationUtils.loadAnimation(ArticleDetailActivity.this, R.anim.show_anim);
        mPageCounterView.startAnimation(showAnim);
        showAnim = AnimationUtils.loadAnimation(ArticleDetailActivity.this, R.anim.show_anim);
        fab.startAnimation(showAnim);

      /*  View v = makeCollapsingToolbarLayoutLooksGood(mCollapsingToolbar);
        if (v == null) return;
        showAnim = AnimationUtils.loadAnimation(ArticleDetailActivity.this, R.anim.show_anim);
        v.startAnimation(showAnim);
    */
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

    private void bindViews() {

        if (mToolbar != null) {
            setSupportActionBar(mToolbar);

            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);

            mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onBackPressed();
                }
            });

        }

        if (mCursor != null) {

            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(Intent.createChooser(ShareCompat.IntentBuilder
                            .from(ArticleDetailActivity.this)
                            .setType("text/plain")
                            .setText(getShareContent())
                            .getIntent(), getString(R.string.action_share)));
                }
            });

            mCollapsingToolbar.setTitle(mCursor.getString(ArticleLoader.Query.TITLE));

            // set custom  typeface
            if (getSerifTypeFace() != null) {
                mCollapsingToolbar.setExpandedTitleTypeface(getSerifTypeFace());
                mCollapsingToolbar.setCollapsedTitleTypeface(getSerifTypeFace());
            }

            if (getAppCustomTypeface(this) != null)
                mByLineView.setTypeface(getAppCustomTypeface(this));

            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                String str = DateUtils.getRelativeTimeSpanString(
                        publishedDate.getTime(),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString()
                        + "\nby "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR);
                mByLineView.setText(str);
            } else {
                String str = outputFormat.format(publishedDate)
                        + "\nby "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR);
                mByLineView.setText(str);
            }

            ImageView mPhotoView = findViewById(R.id.thumbnail);
            GlideApp.with(this)
                    .load(Uri.parse(mCursor.getString(ArticleLoader.Query.THUMB_URL)))
                    //.centerCrop()
                    //.circleCrop()
                    .override(Target.SIZE_ORIGINAL)
                    .into(mPhotoView);

            if (mLoadTask != null && !mLoadTask.isCancelled()) {
                mLoadTask.cancel(true);
                mLoadTask = null;
            }

            mLoadTask = new SplitBookAsync();
            mLoadTask.delegate = this;
            mLoadTask.execute();

            mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position,
                                           float positionOffset,
                                           int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    if (adapter != null) {
                        setPageCounterText((position + 1) + "/" + adapter.getCount());
                    } else {
                        setPageCounterText("Loading data...");
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });

            mByLineView.post(new Runnable() {
                @Override
                public void run() {

                    setPageCounterText("Loading data...");

                }
            });

        } else {
            mCollapsingToolbar.setTitle(getString(R.string.app_name));
            mByLineView.setText("N/A");
            //bodyTextView.setText("N/A");
            setPageCounterText("N/A");
        }

    }

    private Spanned getFormattedBookText(String text) {

        String cleanedText = text
                .replaceAll("(?<!\\r\\n)(\\r\\n)(?!\\r\\n)", " ")
                .replaceAll("(\\r\\n|\\n)", "<br />");

        Spanned result;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(cleanedText, Html.FROM_HTML_MODE_LEGACY);
        } else {
            //noinspection deprecation
            result = Html.fromHtml(cleanedText);
        }
        return result;
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return ArticleLoader.newInstanceForItemId(ArticleDetailActivity.this, mItemId);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (data == null) {
            mCursor = null;
            return;
        }

        mCursor = data;

        if (!mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mCursor = null;
        bindViews();
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        int maxScroll = appBarLayout.getTotalScrollRange();
        float percentage = (float) Math.abs(verticalOffset) / maxScroll;
        float noAlpha = 1;
        final float fadeSpeedModifier = 4;
        mByLineView.setAlpha(noAlpha - percentage * fadeSpeedModifier); // min = 0f, max = 1f
        mPageCounterView.setAlpha(noAlpha - percentage * 0.5f);         // min = 0.5f, max = 1f
    }

    /**
     * @return simple share content text (article title + article author + article date)
     */
    private String getShareContent() {
        return mCollapsingToolbar.getTitle() + "\n" + mByLineView.getText().toString();
    }

    private void setPageCounterText(String text) {
        mPageCounterView.setText(text);
    }

    @Override
    public List<CharSequence> splitBookDoInBackground() {
        if (mCursor == null) return null;

        final Spanned bodyText =
                getFormattedBookText(mCursor.getString(ArticleLoader.Query.BODY));

        int desiredDimen;

        // catch mid dimen between device height and width
        if (getResources().getDisplayMetrics().heightPixels
                > getResources().getDisplayMetrics().widthPixels) {
            int dif = (getResources().getDisplayMetrics().heightPixels -
                    getResources().getDisplayMetrics().widthPixels) / 2;
            desiredDimen = getResources().getDisplayMetrics().heightPixels - dif;
        } else {
            int dif = (getResources().getDisplayMetrics().widthPixels -
                    getResources().getDisplayMetrics().heightPixels) / 2;
            desiredDimen = getResources().getDisplayMetrics().widthPixels - dif;
        }

        final PageSplitter pageSplitter =
                new PageSplitter(desiredDimen,
                        desiredDimen * 2,
                        1f,
                        1f);

        pageSplitter.append(bodyText.toString());

        TextPaint mPaintDetail = new TextPaint();
        mPaintDetail.setColor(Color.DKGRAY);
        mPaintDetail.setShadowLayer(2.0f, 0f, 2.0f, Color.DKGRAY);
        mPaintDetail.setTextSize(18 * getResources().getDisplayMetrics().density);
        mPaintDetail.setAntiAlias(true);

        pageSplitter.split(mPaintDetail);

        return pageSplitter.getPages();
    }

    @Override
    public void splitBookOnPostExecute(List<CharSequence> charSequences) {
        if (charSequences == null) return;

        adapter = new
                SectionsPagerAdapter(getSupportFragmentManager(), charSequences);
        mViewPager.setOffscreenPageLimit(1);
        mViewPager.setAdapter(adapter);

        int currentPos = mViewPager.getCurrentItem() + 1;
        setPageCounterText(currentPos + "/" + adapter.getCount());

        initialAnim = AnimationUtils
                .loadAnimation(ArticleDetailActivity.this, R.anim.initial_anim);
        mPageCounterView.clearAnimation();
        mPageCounterView.startAnimation(initialAnim);

        mPageCounterView.clearAnimation();
        mViewPager.clearAnimation();
        showAnim = AnimationUtils
                .loadAnimation(ArticleDetailActivity.this, R.anim.show_anim);
        mPageCounterView.startAnimation(showAnim);
        showAnim = AnimationUtils
                .loadAnimation(ArticleDetailActivity.this, R.anim.show_anim);
        mViewPager.startAnimation(showAnim);

    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        private final List<CharSequence> content;

        SectionsPagerAdapter(FragmentManager fm, List<CharSequence> content) {
            super(fm);
            this.content = content;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position, content.get(position).toString());
        }

        @Override
        public int getCount() {
            if (content == null) return 0;
            return content.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "PAGE " + position;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static final String ARG_SECTION_CONTENT = "section_content";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        static PlaceholderFragment newInstance(int sectionNumber, String content) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            args.putString(ARG_SECTION_CONTENT, content);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.body_item, container, false);

            CardView cardView = rootView.findViewById(R.id.body_item_card_view);
            ViewGroup.LayoutParams params = cardView.getLayoutParams();
            final float maxWidthInt = 720;
            final int maxDesiredWidth =
                    (int) (maxWidthInt * getResources().getDisplayMetrics().density);
            if (getResources().getDisplayMetrics().widthPixels > maxDesiredWidth) {
                params.width = maxDesiredWidth;
                cardView.setLayoutParams(params);
            }

            if (getArguments() != null && getArguments().containsKey(ARG_SECTION_CONTENT)) {
                TextView textView = rootView.findViewById(R.id.article_body);
                textView.setText(getArguments().getString(ARG_SECTION_CONTENT));
                if (getAppCustomTypeface(getActivity()) != null)
                    textView.setTypeface(getAppCustomTypeface(getActivity()));
            }

            return rootView;
        }
    }
}
