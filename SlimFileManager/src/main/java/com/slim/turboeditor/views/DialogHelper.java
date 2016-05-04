/*
 * Copyright (C) 2014 Vlad Mihalachi
 *
 * This file is part of Turbo Editor.
 *
 * Turbo Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Turbo Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.slim.turboeditor.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.slim.slimfilemanager.R;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Helper class for showing fragment dialogs.
 */
public class DialogHelper {

    /**
     * Helper class to implement custom dialog's design.
     */
    public static class Builder {

        protected final Context mContext;

        private Drawable mIcon;
        private CharSequence mTitleText;
        private CharSequence mMessageText;
        private View mView;
        private int mViewRes;

        public Builder(Context context) {
            mContext = context;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return new HashCodeBuilder(201, 17)
                    .append(mContext)
                    .append(mIcon)
                    .append(mTitleText)
                    .append(mMessageText)
                    .append(mViewRes)
                    .append(mView)
                    .toHashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;
            if (o == this)
                return true;
            if (!(o instanceof Builder))
                return false;

            Builder builder = (Builder) o;
            return new EqualsBuilder()
                    .append(mContext, builder.mContext)
                    .append(mIcon, builder.mIcon)
                    .append(mTitleText, builder.mTitleText)
                    .append(mMessageText, builder.mMessageText)
                    .append(mViewRes, builder.mViewRes)
                    .append(mView, builder.mView)
                    .isEquals();
        }

        public Builder setIcon(Drawable icon) {
            mIcon = icon;
            return this;
        }

        public Builder setTitle(CharSequence title) {
            mTitleText = title;
            return this;
        }

        public Builder setMessage(CharSequence message) {
            mMessageText = message;
            return this;
        }

        public Builder setIcon(int iconRes) {
            return setIcon(iconRes == 0 ? null : mContext.getResources().getDrawable(iconRes));
        }

        public Builder setTitle(int titleRes) {
            return setTitle(titleRes == 0 ? null : getString(titleRes));
        }

        private String getString(int stringRes) {
            return mContext.getResources().getString(stringRes);
        }

        public Builder setView(View view) {
            mView = view;
            mViewRes = 0;
            return this;
        }

        public Builder setView(int layoutRes) {
            mView = null;
            mViewRes = layoutRes;
            return this;
        }

        /**
         * Builds view that based on simple {@link android.widget.ScrollView} container.
         * This is nice to display simple layout without scrollable elements such as
         * {@link android.widget.ListView} or any similar. Use {@link #createSkeletonView()}
         * for them.
         */
        public View createCommonView() {

            // Creating skeleton layout will also try
            // to add custom view. Avoid of doing it.
            int customViewRes = mViewRes;
            View customView = mView;
            mViewRes = 0;
            mView = null;

            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            ViewGroup rootLayout = (ViewGroup) createSkeletonView();
            View bodyRootView = inflater.inflate(R.layout.dialog, rootLayout, false);
            ViewGroup bodyLayout = (ViewGroup) bodyRootView.findViewById(R.id.content);
            TextView messageView = (TextView) bodyLayout.findViewById(R.id.message);

            rootLayout.addView(bodyRootView);

            // Setup content
            bodyLayout.removeView(messageView);
            if (!TextUtils.isEmpty(mMessageText)) {
                messageView.setMovementMethod(new LinkMovementMethod());
                messageView.setText(mMessageText);
                bodyLayout.addView(messageView);
            }

            // Custom view
            if (customViewRes != 0) customView = inflater.inflate(customViewRes, bodyLayout, false);
            if (customView != null) bodyLayout.addView(customView);
            mView = customView;

            return rootLayout;
        }

        public View createSkeletonView() {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            ViewGroup rootLayout = (ViewGroup) inflater.inflate(R.layout.dialog_skeleton, null);
            TextView titleView = (TextView) rootLayout.findViewById(R.id.title);

            // Setup title
            if (mTitleText != null) {
                titleView.setText(mTitleText);
                titleView.setCompoundDrawablesWithIntrinsicBounds(mIcon, null, null, null);
            } else {
                // This also removes an icon.
                rootLayout.removeView(titleView);
            }

            // Custom view
            if (mViewRes != 0) mView = inflater.inflate(mViewRes, rootLayout, false);
            if (mView != null) rootLayout.addView(mView);

            return rootLayout;
        }
    }

}