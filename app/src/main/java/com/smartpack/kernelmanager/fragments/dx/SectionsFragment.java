package com.smartpack.kernelmanager.fragments.dx;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.textview.MaterialTextView;
import com.smartpack.kernelmanager.R;
import com.smartpack.kernelmanager.utils.DxMotion;
import com.smartpack.kernelmanager.activities.NavigationActivity;
import com.smartpack.kernelmanager.fragments.BaseFragment;
import com.smartpack.kernelmanager.fragments.kernel.BatteryFragment;
import com.smartpack.kernelmanager.fragments.kernel.CPUFragment;
import com.smartpack.kernelmanager.fragments.kernel.GPUFragment;
import com.smartpack.kernelmanager.fragments.kernel.VMFragment;
import com.smartpack.kernelmanager.fragments.kernel.IOFragment;
import com.smartpack.kernelmanager.fragments.kernel.KSMFragment;
import com.smartpack.kernelmanager.fragments.kernel.ScreenFragment;
import com.smartpack.kernelmanager.fragments.kernel.WakelockFragment;
import com.smartpack.kernelmanager.fragments.kernel.EntropyFragment;
import com.smartpack.kernelmanager.utils.DxInfo;
import com.smartpack.kernelmanager.utils.kernel.battery.Battery;

import android.os.Handler;
import android.os.Looper;
import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Kernel Dr.xok: tela "Seções" — todas as áreas do app em cartões e lista.
 */
public class SectionsFragment extends BaseFragment {

    private LinearLayout mFeatured, mList;
    private final List<View> mRows = new ArrayList<>();
    private final List<String> mRowNames = new ArrayList<>();
    private final List<View> mHeaders = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private static final Class<? extends Fragment>[] FEATURED = new Class[]{
            CPUFragment.class, GPUFragment.class, BatteryFragment.class, VMFragment.class
    };
    private static final int[][] FEATURED_COLORS = {
            {R.color.dx_lilac, R.color.dx_ink, R.string.dx_feat_cpu, R.drawable.dx_ic_cpu, R.string.cpu},
            {R.color.dx_pink, R.color.dx_pink_ink, R.string.dx_feat_gpu, R.drawable.dx_ic_gpu, R.string.gpu},
            {R.color.dx_mint, R.color.dx_mint_ink, R.string.dx_feat_battery, R.drawable.dx_ic_battery, R.string.battery},
            {R.color.dx_butter, R.color.dx_butter_ink, R.string.dx_feat_memory, R.drawable.dx_ic_memory, R.string.memory},
    };

    /** cores das caixinhas de ícone da lista, em rodízio */
    private static final int[][] ROW_COLORS = {
            {0x2EC9B8FF, R.color.dx_lilac}, {0x2EF7B8D8, R.color.dx_pink},
            {0x2EB6EBC7, R.color.dx_mint}, {0x2EF3E58F, R.color.dx_butter},
    };

    /** ícones de contorno para as seções do kernel mais usadas */
    private static final Map<Class<?>, Integer> OUTLINE_ICONS = new HashMap<>();
    static {
        OUTLINE_ICONS.put(IOFragment.class, R.drawable.dx_ic_doc);
        OUTLINE_ICONS.put(ScreenFragment.class, R.drawable.dx_ic_clock);
        OUTLINE_ICONS.put(WakelockFragment.class, R.drawable.dx_ic_lock);
        OUTLINE_ICONS.put(KSMFragment.class, R.drawable.dx_ic_merge);
        OUTLINE_ICONS.put(DashboardFragment.class, R.drawable.dx_ic_dashboard);
    }

    private final Map<Class<?>, MaterialTextView> mSubtitles = new HashMap<>();
    private final Handler mMain = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dx_sections, container, false);
        mFeatured = root.findViewById(R.id.featured);
        mList = root.findViewById(R.id.list);

        MaterialTextView headline = root.findViewById(R.id.headline);
        String first = getString(R.string.dx_sections_title1);
        String second = getString(R.string.dx_sections_title2);
        SpannableString title = new SpannableString(first + "\n" + second);
        title.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.dx_pink)),
                first.length() + 1, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        headline.setText(title);

        build();
        loadSubtitles();
        if (savedInstanceState == null) {
            DxMotion.stagger(mFeatured);
            DxMotion.stagger(mList);
        }

        EditText search = root.findViewById(R.id.search);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        return root;
    }

    private NavigationActivity nav() {
        return (NavigationActivity) requireActivity();
    }

    private void build() {
        NavigationActivity activity = nav();
        Context c = requireContext();
        List<NavigationActivity.NavigationFragment> fragments = activity.getFragments();

        // cartões grandes (só o que o kernel suporta)
        List<View> cards = new ArrayList<>();
        for (int i = 0; i < FEATURED.length; i++) {
            NavigationActivity.NavigationFragment nf = find(fragments, FEATURED[i]);
            if (nf == null || !activity.getActualFragments().containsValue(FEATURED[i])) continue;
            cards.add(featuredCard(c, nf, FEATURED_COLORS[i]));
        }
        for (int i = 0; i < cards.size(); i += 2) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rp.bottomMargin = dp(12);
            row.setLayoutParams(rp);
            for (int j = i; j < Math.min(i + 2, cards.size()); j++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(118), 1f);
                if (j == i) lp.rightMargin = dp(6); else lp.leftMargin = dp(6);
                row.addView(cards.get(j), lp);
            }
            if (row.getChildCount() == 1) {
                View spacer = new View(c);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(118), 1f);
                lp.leftMargin = dp(6);
                row.addView(spacer, lp);
            }
            mFeatured.addView(row);
        }

        // demais seções, agrupadas (o grupo "Kernel" vem primeiro, como no mockup)
        List<NavigationActivity.NavigationFragment> ordered = new ArrayList<>();
        List<NavigationActivity.NavigationFragment> rest = new ArrayList<>();
        boolean inKernel = false;
        for (NavigationActivity.NavigationFragment nf : fragments) {
            if (nf.mFragmentClass == null) inKernel = nf.mId == R.string.kernel;
            (inKernel ? ordered : rest).add(nf);
        }
        ordered.addAll(rest);
        MaterialTextView header = null;
        int rowIndex = 0;
        for (NavigationActivity.NavigationFragment nf : ordered) {
            if (nf.mFragmentClass == null) {
                header = sectionHeader(c, getString(nf.mId));
                continue;
            }
            if (nf.mFragmentClass == DashboardFragment.class || nf.mFragmentClass == SectionsFragment.class) continue;
            if (isFeatured(nf.mFragmentClass)) continue;
            if (!activity.getActualFragments().containsValue(nf.mFragmentClass)) continue;
            if (header != null) {
                mList.addView(header);
                mHeaders.add(header);
                header = null;
            }
            View row = listRow(c, nf, rowIndex++);
            mList.addView(row);
            mRows.add(row);
            mRowNames.add(getString(nf.mId).toLowerCase(Locale.getDefault()));
        }
    }

    private boolean isFeatured(Class<? extends Fragment> cls) {
        for (Class<? extends Fragment> f : FEATURED) if (f == cls) return true;
        return false;
    }

    private static NavigationActivity.NavigationFragment find(
            List<NavigationActivity.NavigationFragment> list, Class<? extends Fragment> cls) {
        for (NavigationActivity.NavigationFragment nf : list) {
            if (nf.mFragmentClass == cls) return nf;
        }
        return null;
    }

    private View featuredCard(Context c, NavigationActivity.NavigationFragment nf, int[] colors) {
        int bg = ContextCompat.getColor(c, colors[0]);
        int ink = ContextCompat.getColor(c, colors[1]);

        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.dx_tile);
        card.setBackgroundTintList(ColorStateList.valueOf(bg));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setClickable(true);
        card.setFocusable(true);
        if (android.os.Build.VERSION.SDK_INT >= 23) card.setForeground(ContextCompat.getDrawable(c, selectable(c)));
        card.setContentDescription(getString(nf.mId));
        card.setOnClickListener(v -> nav().openSection(nf.mFragmentClass));
        DxMotion.press(card);

        AppCompatImageView icon = new AppCompatImageView(c);
        icon.setImageResource(colors[3]);
        icon.setImageTintList(ColorStateList.valueOf(ink));
        card.addView(icon, new LinearLayout.LayoutParams(dp(26), dp(26)));

        View space = new View(c);
        card.addView(space, new LinearLayout.LayoutParams(1, 0, 1f));

        MaterialTextView title = new MaterialTextView(c);
        title.setText(colors[4]);
        title.setTextColor(ink);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(ResourcesCompat.getFont(c, R.font.sora_bold));
        card.addView(title);

        MaterialTextView sub = new MaterialTextView(c);
        sub.setText(colors[2]);
        sub.setTextColor(ink);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sub.setMaxLines(2);
        card.addView(sub);
        mSubtitles.put(nf.mFragmentClass, sub);
        return card;
    }

    private MaterialTextView sectionHeader(Context c, String text) {
        MaterialTextView h = new MaterialTextView(c);
        h.setText(text);
        h.setAllCaps(true);
        h.setLetterSpacing(0.1f);
        h.setTextColor(ContextCompat.getColor(c, R.color.dx_text_muted));
        h.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        h.setTypeface(ResourcesCompat.getFont(c, R.font.inter_semibold));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        lp.bottomMargin = dp(8);
        lp.leftMargin = dp(4);
        h.setLayoutParams(lp);
        return h;
    }

    private View listRow(Context c, NavigationActivity.NavigationFragment nf, int index) {
        int[] rc = ROW_COLORS[index % ROW_COLORS.length];

        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.dx_glass);
        row.setPadding(dp(12), dp(10), dp(14), dp(10));
        row.setMinimumHeight(dp(64));
        row.setClickable(true);
        row.setFocusable(true);
        if (android.os.Build.VERSION.SDK_INT >= 23) row.setForeground(ContextCompat.getDrawable(c, selectable(c)));
        row.setOnClickListener(v -> nav().openSection(nf.mFragmentClass));
        DxMotion.press(row);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.bottomMargin = dp(8);
        row.setLayoutParams(rp);

        AppCompatImageView icon = new AppCompatImageView(c);
        icon.setBackgroundResource(R.drawable.dx_icon_box);
        icon.setBackgroundTintList(ColorStateList.valueOf(rc[0]));
        int pad = dp(10);
        icon.setPadding(pad, pad, pad, pad);
        Integer outline = OUTLINE_ICONS.get(nf.mFragmentClass);
        if (outline != null) icon.setImageResource(outline);
        else if (nf.getDrawable() != 0) icon.setImageResource(nf.getDrawable());
        icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(c, rc[1])));
        row.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tp.leftMargin = dp(14);
        row.addView(texts, tp);

        MaterialTextView title = new MaterialTextView(c);
        title.setText(nf.mId);
        title.setTextColor(ContextCompat.getColor(c, R.color.dx_text));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(ResourcesCompat.getFont(c, R.font.sora_bold));
        texts.addView(title);

        MaterialTextView sub = new MaterialTextView(c);
        sub.setTextColor(ContextCompat.getColor(c, R.color.dx_text_muted));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sub.setVisibility(View.GONE);
        texts.addView(sub);
        mSubtitles.put(nf.mFragmentClass, sub);

        AppCompatImageView chevron = new AppCompatImageView(c);
        chevron.setImageResource(R.drawable.dx_ic_chevron);
        chevron.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(c, R.color.dx_text_muted)));
        chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(chevron, new LinearLayout.LayoutParams(dp(18), dp(18)));
        return row;
    }

    /** legendas com o estado atual de cada seção (lidas em segundo plano) */
    private void loadSubtitles() {
        final android.content.Context app = requireContext().getApplicationContext();
        final float hz = requireActivity().getWindowManager().getDefaultDisplay().getRefreshRate();
        new Thread(() -> {
            Map<Class<?>, String> info = new HashMap<>();
            try {
                // CPU: governador e frequência máxima do grupo mais forte
                int best = 0; String gov = "";
                for (int n = 0; n < 16; n++) {
                    String p = "/sys/devices/system/cpu/cpufreq/policy" + n;
                    if (!new java.io.File(p).exists()) continue;
                    int max = DxInfo.readInt(p + "/cpuinfo_max_freq");
                    if (max > best) { best = max; gov = DxInfo.read(p + "/scaling_governor"); }
                }
                if (best > 0) {
                    boolean eco = gov.equals("schedutil")
                            && DxInfo.exists("/sys/devices/system/cpu/cpufreq/policy0/schedutil/efficient_freq");
                    info.put(CPUFragment.class, (eco ? "schedutil eco" : gov) + " · " + best / 1000 + " MHz");
                }
                // GPU: modelo e máximo
                String model = DxInfo.read("/sys/class/kgsl/kgsl-3d0/gpu_model");
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{3})").matcher(model);
                int gmax = DxInfo.readInt("/sys/class/kgsl/kgsl-3d0/max_gpuclk") / 1000000;
                if (gmax > 0) info.put(GPUFragment.class, (m.find() ? "Adreno " + m.group(1) + " · " : "") + gmax + " MHz");
                // Bateria: limite e saúde
                StringBuilder bat = new StringBuilder();
                if (Battery.hasDrxokChargeLimit()) {
                    int lim = Battery.getDrxokChargeLimit();
                    bat.append(lim >= 100 ? app.getString(R.string.dx_no_limit_short) : app.getString(R.string.dx_limit_pct, lim));
                }
                int full = Battery.getChargeFullMah();
                int design = Battery.getInstance(app).getDesignMah();
                if (full > 0 && design > 0) {
                    if (bat.length() > 0) bat.append(" · ");
                    bat.append(app.getString(R.string.dx_health_pct, Math.round(full * 100f / design)));
                }
                if (bat.length() > 0) info.put(BatteryFragment.class, bat.toString());
                // Memória: ZRAM e swappiness
                String algo = DxInfo.bracketed(DxInfo.read("/sys/block/zram0/comp_algorithm"));
                int sw = DxInfo.readInt("/proc/sys/vm/swappiness");
                info.put(VMFragment.class, (algo.isEmpty() ? "" : "ZRAM " + algo + " · ") + "swappiness " + sw);
                // I/O
                String sched = DxInfo.bracketed(DxInfo.read("/sys/block/mmcblk0/queue/scheduler"));
                if (!sched.isEmpty()) info.put(IOFragment.class, sched);
                // Tela
                if (hz > 0) info.put(ScreenFragment.class, Math.round(hz) + " Hz");
                // Wakelocks
                if (DxInfo.exists("/sys/class/misc/boeffla_wakelock_blocker"))
                    info.put(WakelockFragment.class, app.getString(R.string.dx_boeffla));
                // KSM
                if (DxInfo.exists("/sys/kernel/mm/ksm/run"))
                    info.put(KSMFragment.class, app.getString(DxInfo.readInt("/sys/kernel/mm/ksm/run") == 1 ? R.string.dx_on : R.string.dx_off));
                // Entropia
                int ent = DxInfo.readInt("/proc/sys/kernel/random/entropy_avail");
                if (ent > 0) info.put(EntropyFragment.class, String.valueOf(ent));
            } catch (Exception ignored) {
            }
            mMain.post(() -> {
                if (!isAdded()) return;
                for (Map.Entry<Class<?>, String> e : info.entrySet()) {
                    MaterialTextView tv = mSubtitles.get(e.getKey());
                    if (tv != null && e.getValue() != null && !e.getValue().isEmpty()) {
                        tv.setText(e.getValue());
                        tv.setVisibility(View.VISIBLE);
                    }
                }
            });
        }, "dx-sections").start();
    }

    private void filter(String query) {
        String q = query.trim().toLowerCase(Locale.getDefault());
        for (int i = 0; i < mRows.size(); i++) {
            mRows.get(i).setVisibility(q.isEmpty() || mRowNames.get(i).contains(q) ? View.VISIBLE : View.GONE);
        }
        for (View h : mHeaders) h.setVisibility(q.isEmpty() ? View.VISIBLE : View.GONE);
        mFeatured.setVisibility(q.isEmpty() ? View.VISIBLE : View.GONE);
        if (!q.isEmpty()) {
            // os cartões grandes entram na busca como linhas normais
            NavigationActivity activity = nav();
            for (Class<? extends Fragment> cls : FEATURED) {
                NavigationActivity.NavigationFragment nf = find(activity.getFragments(), cls);
                if (nf != null && getString(nf.mId).toLowerCase(Locale.getDefault()).contains(q)
                        && activity.getActualFragments().containsValue(cls)) {
                    mFeatured.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private static int selectable(Context c) {
        TypedValue tv = new TypedValue();
        c.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        return tv.resourceId;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
