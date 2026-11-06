package com.smartpack.kernelmanager.views.recyclerview;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textview.MaterialTextView;
import com.smartpack.kernelmanager.R;
import com.smartpack.kernelmanager.utils.DxMotion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kernel Dr.xok: escolha em pílulas (substitui a caixa de diálogo de seleção).
 * Mesma API básica do SelectView: setItems, setItem, setOnItemSelected.
 */
public class DxChipsView extends RecyclerViewItem {

    public interface OnItemSelected {
        void onItemSelected(DxChipsView view, int position, String item);
    }

    private MaterialTextView mTitle;
    private MaterialTextView mSummary;
    private ChipGroup mChips;

    private CharSequence mTitleText;
    private CharSequence mSummaryText;
    private List<String> mItems = new ArrayList<>();
    private final Map<String, String> mLabels = new HashMap<>();
    private String mSelected;
    private int mAccentRes = R.color.dx_pink;
    private int mAccentInkRes = R.color.dx_pink_ink;
    private OnItemSelected mOnItemSelected;
    private boolean mBinding;
    private boolean mStandalone;
    private List<String> mPrimary;
    private String mMoreLabel;
    private boolean mExpanded;

    /** mostra só estes (e o selecionado) até tocar em "mais…" */
    public void setPrimary(List<String> primary, String moreLabel) {
        mPrimary = primary;
        mMoreLabel = moreLabel;
        refresh();
    }

    private List<String> visibleItems() {
        if (mPrimary == null || mExpanded) return mItems;
        List<String> out = new ArrayList<>();
        for (String p : mPrimary) if (mItems.contains(p)) out.add(p);
        if (mSelected != null && mItems.contains(mSelected) && !out.contains(mSelected)) out.add(mSelected);
        return out;
    }

    private boolean hasMore() {
        return mPrimary != null && !mExpanded && visibleItems().size() < mItems.size();
    }

    /** fora de cartão: ganha margem lateral como o resto da tela */
    public void setStandalone(boolean standalone) {
        mStandalone = standalone;
        setFullSpan(standalone);
    }

    @Override
    protected boolean cardCompatible() {
        return !mStandalone;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.rv_dx_chips;
    }

    @Override
    public void onCreateView(View view) {
        mTitle = view.findViewById(R.id.title);
        mSummary = view.findViewById(R.id.summary);
        mChips = view.findViewById(R.id.chips);
        if (mStandalone) {
            int p = (int) (16 * view.getResources().getDisplayMetrics().density);
            view.setPadding(p, p / 2, p, p / 2);
        }
        super.onCreateView(view);
    }

    public void setTitle(CharSequence title) {
        mTitleText = title;
        refresh();
    }

    public void setSummary(CharSequence summary) {
        mSummaryText = summary;
        refresh();
    }

    public void setItems(List<String> items) {
        mItems = items == null ? new ArrayList<>() : items;
        refresh();
    }

    /** texto mostrado na pílula para um valor (ex.: "schedutil" -> "schedutil (eco)") */
    public void setItemLabel(String item, String label) {
        mLabels.put(item, label);
        refresh();
    }

    public void setItem(String item) {
        mSelected = item;
        refresh();
    }

    public void setItem(int position) {
        if (position >= 0 && position < mItems.size()) {
            setItem(mItems.get(position));
        }
    }

    public void setAccent(int colorRes, int inkRes) {
        mAccentRes = colorRes;
        mAccentInkRes = inkRes;
        refresh();
    }

    public void setOnItemSelected(OnItemSelected onItemSelected) {
        mOnItemSelected = onItemSelected;
    }

    @Override
    protected void refresh() {
        super.refresh();
        if (mTitle == null) return;

        if (mTitleText != null) {
            mTitle.setText(mTitleText);
            mTitle.setVisibility(View.VISIBLE);
        } else {
            mTitle.setVisibility(View.GONE);
        }
        if (mSummaryText != null) {
            mSummary.setText(mSummaryText);
            mSummary.setVisibility(View.VISIBLE);
        } else {
            mSummary.setVisibility(View.GONE);
        }

        Context context = mChips.getContext();
        mBinding = true;
        List<String> visible = visibleItems();
        boolean more = hasMore();
        String sig = signature();
        if (sig.equals(mChips.getTag())) {
            mBinding = false;
            return;
        }
        mChips.setTag(sig);
        mChips.removeAllViews();
        for (int i = 0; i < visible.size(); i++) {
            Chip chip = makeChip(context, visible.get(i));
            String label = mLabels.get(visible.get(i));
            chip.setText(label != null ? label : visible.get(i));
            boolean on = visible.get(i).equals(mSelected);
            chip.setChecked(on);
            chip.setChipCornerRadius(on ? squareRadius(context) : pillRadius(context));
            mChips.addView(chip);
        }
        if (more) {
            Chip chip = makeChip(context, null);
            chip.setText(mMoreLabel);
            chip.setCheckable(false);
            mChips.addView(chip);
        }
        mBinding = false;
    }

    private Chip makeChip(Context context, final String item) {
        Chip chip = new Chip(context);
        chip.setTag(item);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setEnsureMinTouchTargetSize(true);
        chip.setChipCornerRadius(999f);
        chip.setChipMinHeight(context.getResources().getDisplayMetrics().density * 40);
        chip.setTypeface(ResourcesCompat.getFont(context, R.font.inter_semibold));

        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        int accent = ContextCompat.getColor(context, mAccentRes);
        int ink = ContextCompat.getColor(context, mAccentInkRes);
        int text = ContextCompat.getColor(context, R.color.dx_text);
        int stroke = ContextCompat.getColor(context, R.color.dx_stroke);
        chip.setChipBackgroundColor(new ColorStateList(states, new int[]{accent, 0x00000000}));
        chip.setTextColor(new ColorStateList(states, new int[]{ink, text}));
        chip.setChipStrokeColor(new ColorStateList(states, new int[]{accent, stroke}));
        chip.setChipStrokeWidth(context.getResources().getDisplayMetrics().density);
        chip.setRippleColor(ColorStateList.valueOf(0x22FFFFFF));

        chip.setOnClickListener(v -> {
            if (mBinding) return;
            if (item == null) {          // "mais…"
                mExpanded = true;
                refresh();
                return;
            }
            String before = signature();
            String previous = mSelected;
            mSelected = item;
            if (!animateSelection(before, previous)) refresh();
            if (mOnItemSelected != null) {
                mOnItemSelected.onItemSelected(this, mItems.indexOf(item), item);
            }
        });
        return chip;
    }

    private String signature() {
        return visibleItems() + "|" + mSelected + "|" + hasMore() + "|" + mLabels;
    }

    private static float pillRadius(Context c) {
        return 20 * c.getResources().getDisplayMetrics().density;
    }

    private static float squareRadius(Context c) {
        return 12 * c.getResources().getDisplayMetrics().density;
    }

    /**
     * Kernel Dr.xok: a pílula escolhida muda de cor e de forma (pílula → quadrado arredondado)
     * com mola, sem recriar a lista. Retorna false se for preciso reconstruir.
     */
    private boolean animateSelection(String before, String previous) {
        if (mChips == null || !before.equals(mChips.getTag())) return false;
        String after = signature();
        // a lista visível mudou (ex.: item extra some): reconstrói
        String beforeList = before.substring(0, before.indexOf('|'));
        String afterList = after.substring(0, after.indexOf('|'));
        if (!beforeList.equals(afterList)) return false;
        mChips.setTag(after);
        mBinding = true;
        for (int i = 0; i < mChips.getChildCount(); i++) {
            View child = mChips.getChildAt(i);
            if (!(child instanceof Chip) || child.getTag() == null) continue;
            Chip chip = (Chip) child;
            boolean on = child.getTag().equals(mSelected);
            boolean wasOn = child.getTag().equals(previous);
            // o Chip já alterna sozinho ao ser tocado; a gente manda no estado
            if (wasOn != on) morph(chip, on);
            else chip.setChecked(on);
        }
        mBinding = false;
        return true;
    }

    private void morph(Chip chip, boolean on) {
        Context c = chip.getContext();
        ColorStateList bgList = chip.getChipBackgroundColor();
        ColorStateList textList = chip.getTextColors();
        int accent = ContextCompat.getColor(c, mAccentRes);
        int ink = ContextCompat.getColor(c, mAccentInkRes);
        int text = ContextCompat.getColor(c, R.color.dx_text);
        int bgFrom = on ? 0x00FFFFFF & accent : accent, bgTo = on ? accent : 0x00FFFFFF & accent;
        int fgFrom = on ? text : ink, fgTo = on ? ink : text;
        float rFrom = on ? pillRadius(c) : squareRadius(c), rTo = on ? squareRadius(c) : pillRadius(c);
        chip.setChecked(on);

        android.animation.ArgbEvaluator argb = new android.animation.ArgbEvaluator();
        android.animation.ValueAnimator va = android.animation.ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(280);
        va.setInterpolator(new android.view.animation.PathInterpolator(0.05f, 0.7f, 0.1f, 1f));
        va.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            chip.setChipBackgroundColor(ColorStateList.valueOf((int) argb.evaluate(f, bgFrom, bgTo)));
            chip.setTextColor((int) argb.evaluate(f, fgFrom, fgTo));
        });
        va.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                chip.setChipBackgroundColor(bgList);
                chip.setTextColor(textList);
            }
        });
        va.start();

        // forma: mola entre pílula e quadrado arredondado
        androidx.dynamicanimation.animation.FloatValueHolder holder =
                new androidx.dynamicanimation.animation.FloatValueHolder(rFrom);
        androidx.dynamicanimation.animation.SpringAnimation shape =
                new androidx.dynamicanimation.animation.SpringAnimation(holder);
        shape.setSpring(new androidx.dynamicanimation.animation.SpringForce(rTo)
                .setDampingRatio(0.55f).setStiffness(500f));
        shape.addUpdateListener((a, value, velocity) -> chip.setChipCornerRadius(Math.max(0f, value)));
        shape.start();

        if (on) {
            chip.setScaleX(0.9f);
            chip.setScaleY(0.9f);
            DxMotion.scale(chip, 1f, DxMotion.BOUNCY, 500f);
        }
    }
}
