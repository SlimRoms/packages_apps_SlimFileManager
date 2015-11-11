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

package com.slim.turboeditor.texteditor;

import android.text.TextUtils;

import com.slim.slimfilemanager.settings.SettingsProvider;
import com.slim.turboeditor.activity.MainActivity;

import java.util.LinkedList;
import java.util.List;

public class PageSystem {

    private MainActivity mActivity;

    private List<String> mPages;
    private int[] mStartingLines;
    private int mCurrentPage = 0;
    private PageSystemInterface mPageSystemInterface;

    public PageSystem(MainActivity activity) {
        mActivity = activity;
        mPageSystemInterface = activity;
        mPages = new LinkedList<>();
    }

    public void setFileText(String text) {

        final int charForPage = 20000;
        final int firstPageChars = 50000;

        int i = 0;
        int to;
        int nextIndexOfReturn;
        final int textLength = TextUtils.isEmpty(text) ? 0 : text.length();
        boolean pageSystemEnabled =
                SettingsProvider.getBoolean(mActivity, SettingsProvider.SPLIT_TEXT, true);

        if (pageSystemEnabled) {
            while (i < textLength) {
                // first page is longer
                to = i + (i == 0 ? firstPageChars : charForPage);
                nextIndexOfReturn = text.indexOf("\n", to);
                if (nextIndexOfReturn > to) to = nextIndexOfReturn;
                if (to > text.length()) to = text.length();
                mPages.add(text.substring(i, to));
                i = to + 1;
            }


            if (i == 0)
                mPages.add("");
        } else {
            mPages.add(text);
        }

        mStartingLines = new int[mPages.size()];
        setStartingLines();
    }

    public int getStartingLine() {
        return mStartingLines[mCurrentPage];
    }

    public String getCurrentPageText() {
        return mPages.get(mCurrentPage);
    }

    public void savePage(String currentText) {
        mPages.set(mCurrentPage, currentText);
    }

    public void nextPage() {
        if (!canReadNextPage()) return;
        goToPage(mCurrentPage + 1);
    }

    public void prevPage() {
        if (!canReadPrevPage()) return;
        goToPage(mCurrentPage - 1);
    }

    public void goToPage(int page) {
        if (page >= mPages.size()) page = mPages.size() - 1;
        if (page < 0) page = 0;
        boolean shouldUpdateLines = page > mCurrentPage && canReadNextPage();
        if (shouldUpdateLines) {
            String text = getCurrentPageText();
            int nOfNewLineNow = (text.length() - text.replace("\n", "").length()) + 1; // normally the last line is not counted so we have to add 1
            int nOfNewLineBefore = mStartingLines[mCurrentPage + 1] - mStartingLines[mCurrentPage];
            int difference = nOfNewLineNow - nOfNewLineBefore;
            updateStartingLines(mCurrentPage + 1, difference);
        }
        mCurrentPage = page;
        mPageSystemInterface.onPageChanged(page);
    }

    public void setStartingLines() {
        int i;
        int startingLine;
        int nOfNewLines;
        String text;
        mStartingLines[0] = 0;
        for (i = 1; i < mPages.size(); i++) {
            text = mPages.get(i - 1);
            nOfNewLines = text.length() - text.replace("\n", "").length() + 1;
            startingLine = mStartingLines[i - 1] + nOfNewLines;
            mStartingLines[i] = startingLine;
        }
    }

    public void updateStartingLines(int fromPage, int difference) {
        if (difference == 0)
            return;
        int i;
        if (fromPage < 1) fromPage = 1;
        for (i = fromPage; i < mPages.size(); i++) {
            mStartingLines[i] += difference;
        }
    }

    public int getMaxPage() {
        return mPages.size() - 1;
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }

    public String getAllText(String currentPageText) {
        mPages.set(mCurrentPage, currentPageText);
        int i;
        StringBuilder allText = new StringBuilder();
        for (i = 0; i < mPages.size(); i++) {
            allText.append(mPages.get(i));
            if (i < mPages.size() - 1)
                allText.append("\n");
        }
        return allText.toString();
    }

    public boolean canReadNextPage() {
        return mCurrentPage < mPages.size() - 1;
    }

    public boolean canReadPrevPage() {
        return mCurrentPage >= 1;
    }

    public interface PageSystemInterface {
        void onPageChanged(int page);
    }
}
