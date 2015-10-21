package com.slim.slimfilemanager.multichoice;

import android.util.SparseArray;
import android.util.SparseBooleanArray;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MultiSelector {

    private SparseBooleanArray mSelections = new SparseBooleanArray();
    private WeakHolderTracker mTracker = new WeakHolderTracker();

    private boolean mIsSelectable;

    private void refreshHolder(SelectableHolder holder) {
        if (holder == null) {
            return;
        }
        holder.setSelectable(mIsSelectable);

        boolean isActivated = mSelections.get(holder.getHolderPosition());
        holder.setActivated(isActivated);
    }

    public void bindHolder(MultiChoiceViewHolder holder, int position) {
        mTracker.bindHolder(holder, position);
        refreshHolder(holder);
    }

    public void setSelectable(boolean isSelectable) {
        mIsSelectable = isSelectable;
        refreshAllHolders();
    }

    private void refreshAllHolders() {
        for (SelectableHolder holder : mTracker.getTrackedHolders()) {
            refreshHolder(holder);
        }
    }

    public boolean isSelected(int position) {
        return mSelections.get(position);
    }

    public void setSelected(int position, boolean isSelected) {
        mSelections.put(position, isSelected);
        refreshHolder(mTracker.getHolder(position));
    }

    public void clearSelections() {
        mSelections.clear();
        refreshAllHolders();
    }

    public List<Integer> getSelectedPositions() {
        List<Integer> positions = new ArrayList<>();

        for (int i = 0; i < mSelections.size(); i++) {
            if (mSelections.valueAt(i)) {
                positions.add(mSelections.keyAt(i));
            }
        }
        return positions;
    }

    public void setSelected(SelectableHolder holder, boolean isSelected) {
        setSelected(holder.getHolderPosition(), isSelected);
    }

    public boolean tapSelection(SelectableHolder holder) {
        return tapSelection(holder.getHolderPosition());
    }

    private boolean tapSelection(int position) {
        if (mIsSelectable) {
            boolean isSelected = isSelected(position);
            setSelected(position, !isSelected);
            return true;
        }
        return false;

    }

    class WeakHolderTracker {
        private SparseArray<WeakReference<SelectableHolder>> mHoldersByPosition =
                new SparseArray<>();

        public SelectableHolder getHolder(int position) {
            WeakReference<SelectableHolder> holderRef = mHoldersByPosition.get(position);
            if (holderRef == null) {
                return null;
            }

            SelectableHolder holder = holderRef.get();
            if (holder == null || holder.getHolderPosition() != position) {
                mHoldersByPosition.remove(position);
                return null;
            }

            return holder;
        }

        public void bindHolder(SelectableHolder holder, int position) {
            mHoldersByPosition.put(position, new WeakReference<>(holder));
        }

        public List<SelectableHolder> getTrackedHolders() {
            List<SelectableHolder> holders = new ArrayList<>();

            for (int i = 0; i < mHoldersByPosition.size(); i++) {
                int key = mHoldersByPosition.keyAt(i);
                SelectableHolder holder = getHolder(key);

                if (holder != null) {
                    holders.add(holder);
                }
            }

            return holders;
        }
    }
}
