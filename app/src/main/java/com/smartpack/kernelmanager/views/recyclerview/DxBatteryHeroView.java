package com.smartpack.kernelmanager.views.recyclerview;

import android.view.View;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textview.MaterialTextView;
import com.smartpack.kernelmanager.R;

/**
 * Kernel Dr.xok: cartão verde com o anel de saúde da bateria.
 */
public class DxBatteryHeroView extends RecyclerViewItem {

    private int mPercent;
    private CharSequence mPercentLabel, mTitle, mDetails, mBadge;

    private CircularProgressIndicator mRing;
    private MaterialTextView mPercentView, mPercentLabelView, mTitleView, mDetailsView, mBadgeView;

    @Override
    public int getLayoutRes() {
        return R.layout.rv_dx_battery_hero;
    }

    @Override
    protected boolean cardCompatible() {
        return false;
    }

    @Override
    public void onCreateView(View view) {
        mRing = view.findViewById(R.id.ring);
        mPercentView = view.findViewById(R.id.percent);
        mPercentLabelView = view.findViewById(R.id.percent_label);
        mTitleView = view.findViewById(R.id.title);
        mDetailsView = view.findViewById(R.id.details);
        mBadgeView = view.findViewById(R.id.badge);
        super.onCreateView(view);
    }

    public void setData(int percent, CharSequence percentLabel, CharSequence title,
                        CharSequence details, CharSequence badge) {
        mPercent = Math.max(0, Math.min(100, percent));
        mPercentLabel = percentLabel;
        mTitle = title;
        mDetails = details;
        mBadge = badge;
        refresh();
    }

    @Override
    protected void refresh() {
        super.refresh();
        if (mRing == null) return;
        mRing.setProgress(mPercent);
        mPercentView.setText(mPercent + "%");
        mPercentLabelView.setText(mPercentLabel);
        mTitleView.setText(mTitle);
        mDetailsView.setText(mDetails);
        if (mBadge != null) {
            mBadgeView.setText(mBadge);
            mBadgeView.setVisibility(View.VISIBLE);
        } else {
            mBadgeView.setVisibility(View.GONE);
        }
    }
}
