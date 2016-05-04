package com.slim.slimfilemanager.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

import com.slim.slimfilemanager.R;

public class PageIndicator extends View implements ViewPager.OnPageChangeListener {

    private static final int FADE_FRAME_MS = 30;

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    ViewPager mViewPager;
    int mCurrentPage;
    float mPositionOffset;
    int mScrollState;

    int mFadeDelay = 100;
    int mFadeBy = 0xFF / (mFadeDelay / FADE_FRAME_MS);
    boolean mFades = false;

    private final Runnable mFadeRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mFades) return;

            final int alpha = Math.max(mPaint.getAlpha() - mFadeBy, 0);
            mPaint.setAlpha(alpha);
            invalidate();
            if (alpha > 0) {
                postDelayed(this, FADE_FRAME_MS);
            }
        }
    };

    public PageIndicator(Context context) {
        this(context, null);
    }

    public PageIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.PageIndicator, defStyle, 0);

        setSelectedColor(a.getColor(
                R.styleable.PageIndicator_selectedColor, Color.WHITE));
        a.recycle();
    }

    public void setViewPager(ViewPager vp) {
        if (mViewPager == vp) {
            return;
        }
        if (mViewPager != null) {
            mViewPager.removeOnPageChangeListener(this);
        }
        mViewPager = vp;
        mViewPager.addOnPageChangeListener(this);
    }

    public void setSelectedColor(int color) {
        mPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mViewPager == null) {
            return;
        }
        final int count = mViewPager.getAdapter().getCount();
        if (count == 0) {
            return;
        }

        if (mCurrentPage >= count) {
            return;
        }

        final int paddingLeft = getPaddingLeft();
        final float pageWidth = (getWidth() - paddingLeft - getPaddingRight()) / (1f * count);
        final float left = paddingLeft + pageWidth * (mCurrentPage + mPositionOffset);
        final float right = left + pageWidth;
        final float top = getPaddingTop();
        final float bottom = getHeight() - getPaddingBottom();

        canvas.drawRect(left, top, right, bottom, mPaint);
    }

    @Override
    public void onPageScrolled(int position,
                               float positionOffset, int positionOffsetPixels) {
        mCurrentPage = position;
        mPositionOffset = positionOffset;
        if (mFades) {
            if (positionOffsetPixels > 0) {
                removeCallbacks(mFadeRunnable);
                mPaint.setAlpha(0xFF);
            } else if (mScrollState != ViewPager.SCROLL_STATE_DRAGGING) {
                postDelayed(mFadeRunnable, mFadeDelay);
            }
        }
        invalidate();
    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {
        mScrollState = state;
    }
}
