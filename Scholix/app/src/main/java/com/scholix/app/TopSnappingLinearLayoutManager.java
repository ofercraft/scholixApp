package com.scholix.app;

import android.content.Context;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

public class TopSnappingLinearLayoutManager extends LinearLayoutManager {

    public TopSnappingLinearLayoutManager(Context context) {
        super(context);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
            @Override
            protected int getVerticalSnapPreference() {
                return SNAP_TO_START; // 🟢 Snap to top
            }
        };
        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }
}
