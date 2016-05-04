package com.slim.slimfilemanager.multichoice;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.support.v7.widget.RebindReportingHolder;
import android.util.StateSet;
import android.util.TypedValue;
import android.view.View;

import com.slim.slimfilemanager.R;

public class MultiChoiceViewHolder extends RebindReportingHolder
        implements SelectableHolder {

    private MultiSelector mMultiSelector;

    private boolean mIsSelectable = false;

    private Drawable mSelectionModeBackgroundDrawable;
    private Drawable mDefaultModeBackgroundDrawable;

    public MultiChoiceViewHolder(View view, MultiSelector selector) {
        super(view);

        mMultiSelector = selector;

        // Default selection mode background drawable is this
        setSelectionModeBackgroundDrawable(
                getAccentStateDrawable(itemView.getContext()));
        setDefaultModeBackgroundDrawable(
                itemView.getBackground());
    }

    @SuppressWarnings("deprecation")
    private static int getAccentColor(Context context) {
        int c = context.getResources().getColor(R.color.accent);
        return Color.argb(99, Color.red(c), Color.green(c), Color.blue(c));
    }

    public void setSelectionModeBackgroundDrawable(Drawable selectionModeBackgroundDrawable) {
        mSelectionModeBackgroundDrawable = selectionModeBackgroundDrawable;

        if (mIsSelectable) {
            itemView.setBackground(selectionModeBackgroundDrawable);
        }
    }

    public void setDefaultModeBackgroundDrawable(Drawable defaultModeBackgroundDrawable) {
        mDefaultModeBackgroundDrawable = defaultModeBackgroundDrawable;

        if (!mIsSelectable) {
            itemView.setBackground(mDefaultModeBackgroundDrawable);
        }
    }

    public void setActivated(boolean isActivated) {
        itemView.setActivated(isActivated);
    }

    public void setSelectable(boolean isSelectable) {
        boolean changed = isSelectable != mIsSelectable;
        mIsSelectable = isSelectable;
        if (changed) {
            refresh();
        }
    }

    public int getHolderPosition() {
        return getLayoutPosition();
    }

    private void refresh() {
        Drawable backgroundDrawable = mIsSelectable ? mSelectionModeBackgroundDrawable
                : mDefaultModeBackgroundDrawable;
        itemView.setBackground(backgroundDrawable);
        if (backgroundDrawable != null) {
            backgroundDrawable.jumpToCurrentState();
        }
    }

    private Drawable getAccentStateDrawable(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);

        Drawable colorDrawable = new ColorDrawable(getAccentColor(context));

        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[]{android.R.attr.state_activated}, colorDrawable);
        stateListDrawable.addState(StateSet.WILD_CARD, null);

        return stateListDrawable;
    }

    @Override
    protected void onRebind() {
        mMultiSelector.bindHolder(this, getAdapterPosition());
    }
}
