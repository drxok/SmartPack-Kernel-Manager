package com.smartpack.kernelmanager.views.recyclerview;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import com.smartpack.kernelmanager.R;
import com.smartpack.kernelmanager.utils.DxMotion;

import java.util.ArrayDeque;

/**
 * Kernel Dr.xok: cartão de destaque de um grupo de núcleos — frequência agora, barras e limites.
 */
public class DxClusterHeroView extends RecyclerViewItem {

    private static final int BARS = 8;

    private int mBgRes = R.color.dx_lilac, mInkRes = R.color.dx_ink;
    private CharSequence mTitle, mBadge, mUnit, mMin, mMax;
    private int mCur, mHwMax;
    private final ArrayDeque<Integer> mHistory = new ArrayDeque<>();

    private MaterialCardView mCard;
    private MaterialTextView mTitleView, mBadgeView, mValueView, mUnitView, mMinView, mMaxView;
    private LinearLayout mBars;

    @Override
    public int getLayoutRes() {
        return R.layout.rv_dx_cluster_hero;
    }

    @Override
    protected boolean cardCompatible() {
        return false;
    }

    @Override
    public void onCreateView(View view) {
        mCard = view.findViewById(R.id.hero_card);
        mTitleView = view.findViewById(R.id.hero_title);
        mBadgeView = view.findViewById(R.id.hero_badge);
        mValueView = view.findViewById(R.id.hero_value);
        mUnitView = view.findViewById(R.id.hero_unit);
        mMinView = view.findViewById(R.id.hero_min);
        mMaxView = view.findViewById(R.id.hero_max);
        mBars = view.findViewById(R.id.hero_bars);
        super.onCreateView(view);
    }

    public void setColors(int bgRes, int inkRes) {
        mBgRes = bgRes;
        mInkRes = inkRes;
        refresh();
    }

    public void setInfo(CharSequence title, CharSequence badge, CharSequence unit,
                        CharSequence min, CharSequence max, int hwMax) {
        mTitle = title;
        mBadge = badge;
        mUnit = unit;
        mMin = min;
        mMax = max;
        mHwMax = hwMax;
        refresh();
    }

    /** frequência atual (kHz) */
    public void push(int curKhz) {
        mCur = curKhz;
        mHistory.addLast(curKhz);
        while (mHistory.size() > BARS) mHistory.removeFirst();
        refresh();
    }

    @Override
    protected void refresh() {
        super.refresh();
        if (mCard == null) return;
        int bg = ContextCompat.getColor(mCard.getContext(), mBgRes);
        int ink = ContextCompat.getColor(mCard.getContext(), mInkRes);
        mCard.setCardBackgroundColor(bg);
        for (MaterialTextView t : new MaterialTextView[]{mTitleView, mValueView, mUnitView, mMinView, mMaxView}) {
            t.setTextColor(ink);
        }
        mTitleView.setText(mTitle);
        mBadgeView.setText(mBadge);
        mBadgeView.setVisibility(mBadge == null ? View.GONE : View.VISIBLE);
        mBadgeView.setBackgroundTintList(ColorStateList.valueOf(ink));
        mBadgeView.setTextColor(bg);
        DxMotion.countOrDash(mValueView, mCur / 1000, "");
        mUnitView.setText(mUnit);
        mMinView.setText(mMin);
        mMaxView.setText(mMax);

        float density = mCard.getResources().getDisplayMetrics().density;
        int full = (int) (44 * density);
        if (mBars.getChildCount() != BARS) {
            mBars.removeAllViews();
            for (int i = 0; i < BARS; i++) {
                View bar = new View(mCard.getContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (10 * density), 1f);
                lp.leftMargin = i == 0 ? 0 : (int) (4 * density);
                mBars.addView(bar, lp);
            }
        }
        Integer[] hist = mHistory.toArray(new Integer[0]);
        for (int i = 0; i < BARS; i++) {
            View bar = mBars.getChildAt(i);
            int idx = hist.length - BARS + i;
            int v = idx >= 0 ? hist[idx] : 0;
            float frac = mHwMax > 0 ? Math.min(1f, (float) v / mHwMax) : 0f;
            DxMotion.height(bar, Math.max((int) (10 * density), (int) (full * frac)));
            GradientDrawable d = new GradientDrawable();
            d.setCornerRadius(6 * density);
            boolean newest = i == BARS - 1;
            d.setColor(newest ? ink : (ink & 0x00FFFFFF) | 0x40000000);
            bar.setBackground(d);
        }
    }
}
