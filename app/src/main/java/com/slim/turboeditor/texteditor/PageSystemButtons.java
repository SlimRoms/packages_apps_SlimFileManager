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

import android.content.Context;
import android.os.Handler;
import android.view.View;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.slim.slimfilemanager.R;

public class PageSystemButtons {

    private static final int TIME_TO_SHOW_FABS = 2000;
    final Handler handler = new Handler();
    FloatingActionButton prev, next;
    final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            PageSystemButtons.this.next.setVisibility(View.GONE);
            PageSystemButtons.this.prev.setVisibility(View.GONE);
        }
    };
    PageButtonsInterface pageButtonsInterface;

    public PageSystemButtons(Context context, final PageButtonsInterface pageButtonsInterface, FloatingActionButton prev, FloatingActionButton next) {
        this.prev = prev;
        this.next = next;
        this.pageButtonsInterface = pageButtonsInterface;

        this.next.setColorNormal(context.getResources().getColor(R.color.fab_light));
        this.next.setIconDrawable(context.getResources().getDrawable(R.drawable.ic_keyboard_arrow_right));

        this.prev.setColorNormal(context.getResources().getColor(R.color.fab_light));
        this.prev.setIconDrawable(context.getResources().getDrawable(R.drawable.ic_keyboard_arrow_left));

        if (pageButtonsInterface.canReadNextPage())
            next.setVisibility(View.VISIBLE);

        if (pageButtonsInterface.canReadPrevPage())
            prev.setVisibility(View.VISIBLE);

        this.next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pageButtonsInterface.nextPageClicked();
            }
        });

        this.next.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                pageButtonsInterface.pageSystemButtonLongClicked();
                return true;
            }
        });

        this.prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pageButtonsInterface.prevPageClicked();
            }
        });

        this.prev.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                pageButtonsInterface.pageSystemButtonLongClicked();
                return true;
            }
        });
    }

    public void updateVisibility(boolean autoHide) {

        if (pageButtonsInterface.canReadNextPage())
            PageSystemButtons.this.next.setVisibility(View.VISIBLE);
        else
            PageSystemButtons.this.next.setVisibility(View.GONE);

        if (pageButtonsInterface.canReadPrevPage())
            PageSystemButtons.this.prev.setVisibility(View.VISIBLE);
        else
            PageSystemButtons.this.prev.setVisibility(View.GONE);

        /*if(pageButtonsInterface.hasNext())
            next.showFab();
        else
            next.hideFab();

        if(pageButtonsInterface.hasPrev())
            prev.showFab();
        else
            prev.hideFab();*/

        if (autoHide) {
            handler.removeCallbacks(runnable);
            handler.postDelayed(runnable, TIME_TO_SHOW_FABS);
        } else {
            handler.removeCallbacks(runnable);
        }
    }

    public interface PageButtonsInterface {
        void nextPageClicked();
        void prevPageClicked();
        void pageSystemButtonLongClicked();
        boolean canReadNextPage();
        boolean canReadPrevPage();
    }

}
