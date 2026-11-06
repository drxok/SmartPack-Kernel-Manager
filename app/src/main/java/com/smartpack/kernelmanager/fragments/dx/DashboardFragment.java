package com.smartpack.kernelmanager.fragments.dx;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;
import com.smartpack.kernelmanager.R;
import com.smartpack.kernelmanager.activities.NavigationActivity;
import com.smartpack.kernelmanager.fragments.BaseFragment;
import com.smartpack.kernelmanager.fragments.other.SettingsFragment;
import com.smartpack.kernelmanager.utils.root.RootUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Kernel Dr.xok: Painel (tela inicial do visual novo).
 */
public class DashboardFragment extends BaseFragment {

    private static final long REFRESH_MS = 1500;
    private static final Locale PT = new Locale("pt", "BR");

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Set<String> mNeedsRoot = new HashSet<>();
    private boolean mRunning;
    private boolean mStaticLoaded;
    private volatile List<String> mPolicies;

    private MaterialTextView mGreeting, mKernelName, mKernelVersion, mZramAlgo, mRamText, mZramText;
    private ChipGroup mKernelChips;
    private LinearProgressIndicator mRamBar, mZramBar;
    private Tile mTileBig, mTileLittle, mTileTemp, mTileGpu;

    private static class Tile {
        MaterialTextView label, value, sub;
        LinearProgressIndicator bar;
    }

    /** dados lidos em segundo plano */
    private static class Snapshot {
        String kernelName, kernelVersion, zramAlgo;
        int cpuMaxMhz, gpuMaxMhz;
        int bigCur, bigMax, littleCur, littleMax;
        boolean hasLittle;
        String bigGov;
        int gpuCur;
        float tempC = Float.NaN;
        long memTotal, memAvail, swapTotal, swapFree;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dx_dashboard, container, false);

        mGreeting = root.findViewById(R.id.greeting);
        mKernelName = root.findViewById(R.id.kernel_name);
        mKernelVersion = root.findViewById(R.id.kernel_version);
        mKernelChips = root.findViewById(R.id.kernel_chips);
        mZramAlgo = root.findViewById(R.id.zram_algo);
        mRamText = root.findViewById(R.id.ram_text);
        mZramText = root.findViewById(R.id.zram_text);
        mRamBar = root.findViewById(R.id.ram_bar);
        mZramBar = root.findViewById(R.id.zram_bar);

        mTileBig = tile(root, R.id.tile_big, R.color.dx_pink, R.color.dx_pink_ink, true);
        mTileLittle = tile(root, R.id.tile_little, R.color.dx_mint, R.color.dx_mint_ink, true);
        mTileTemp = tile(root, R.id.tile_temp, R.color.dx_butter, R.color.dx_butter_ink, false);
        mTileGpu = tile(root, R.id.tile_gpu, 0, 0, false);

        mTileBig.label.setText(R.string.dx_cpu_big);
        mTileLittle.label.setText(R.string.dx_cpu_little);
        mTileTemp.label.setText(R.string.dx_temperature);
        mTileTemp.sub.setText(R.string.battery);
        mTileGpu.label.setText(R.string.gpu);

        mGreeting.setText(getString(R.string.dx_hello, deviceName(requireContext())));

        root.findViewById(R.id.settings).setOnClickListener(v -> {
            if (getActivity() instanceof NavigationActivity) {
                ((NavigationActivity) getActivity()).openSection(SettingsFragment.class);
            }
        });
        return root;
    }

    private Tile tile(View root, int id, int bgRes, int inkRes, boolean bar) {
        View v = root.findViewById(id);
        Tile t = new Tile();
        t.label = v.findViewById(R.id.tile_label);
        t.value = v.findViewById(R.id.tile_value);
        t.sub = v.findViewById(R.id.tile_sub);
        t.bar = v.findViewById(R.id.tile_bar);
        Context c = root.getContext();
        if (bgRes != 0) {
            int ink = ContextCompat.getColor(c, inkRes);
            v.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c, bgRes)));
            t.label.setTextColor(ink);
            t.value.setTextColor(ink);
            t.sub.setTextColor(ink);
            t.bar.setIndicatorColor(ink);
            t.bar.setTrackColor((ink & 0x00FFFFFF) | 0x26000000);
        } else {
            v.setBackgroundResource(R.drawable.dx_glass);
            int text = ContextCompat.getColor(c, R.color.dx_text);
            int muted = ContextCompat.getColor(c, R.color.dx_text_muted);
            t.label.setTextColor(muted);
            t.value.setTextColor(text);
            t.sub.setTextColor(muted);
        }
        t.bar.setVisibility(bar ? View.VISIBLE : View.GONE);
        return t;
    }

    static String deviceName(Context context) {
        String name = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            name = Settings.Global.getString(context.getContentResolver(), Settings.Global.DEVICE_NAME);
        }
        if (name == null || name.trim().isEmpty()) {
            name = Build.MODEL;
        }
        return name.trim();
    }

    @Override
    public void onResume() {
        super.onResume();
        mRunning = true;
        mHandler.post(mTick);
    }

    @Override
    public void onPause() {
        super.onPause();
        mRunning = false;
        mHandler.removeCallbacks(mTick);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
    }

    private final Runnable mTick = new Runnable() {
        @Override
        public void run() {
            if (!mRunning) return;
            try {
                mExecutor.execute(() -> {
                    Snapshot s = read(!mStaticLoaded);
                    mHandler.post(() -> {
                        if (mRunning && isAdded()) show(s);
                    });
                });
            } catch (Exception ignored) {
            }
            mHandler.postDelayed(this, REFRESH_MS);
        }
    };

    // ---------------------------------------------------------------- leitura

    private String readFile(String path) {
        if (!mNeedsRoot.contains(path)) {
            try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(line);
                }
                return sb.toString().trim();
            } catch (Exception e) {
                mNeedsRoot.add(path);
            }
        }
        try {
            String out = RootUtils.runAndGetOutput("cat '" + path + "' 2>/dev/null");
            return out == null ? "" : out.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private int readInt(String path) {
        try {
            return Integer.parseInt(readFile(path).split("\\s+")[0]);
        } catch (Exception e) {
            return 0;
        }
    }

    private Snapshot read(boolean withStatic) {
        Snapshot s = new Snapshot();

        if (withStatic) {
            String version = readFile("/proc/version");
            String release = "";
            String[] parts = version.split("\\s+");
            if (parts.length >= 3) release = parts[2];
            int dash = release.indexOf('-');
            String base = dash > 0 ? release.substring(0, dash) : release;
            String suffix = dash > 0 ? release.substring(dash + 1).replace("+", "") : "";
            s.kernelName = suffix.isEmpty() || suffix.equals("perf") ? "Kernel" : "Kernel " + suffix;
            s.kernelVersion = "Linux " + base + " · " + Build.MODEL;

            String algo = readFile("/sys/block/zram0/comp_algorithm");
            int a = algo.indexOf('['), b = algo.indexOf(']');
            s.zramAlgo = a >= 0 && b > a ? algo.substring(a + 1, b) : "";

            int gpuMax = readInt("/sys/class/kgsl/kgsl-3d0/max_gpuclk");
            if (gpuMax == 0) gpuMax = readInt("/sys/class/kgsl/kgsl-3d0/devfreq/max_freq");
            s.gpuMaxMhz = gpuMax / 1000000;
        }

        // CPU: uma "policy" por grupo de núcleos (descobertas uma vez)
        if (mPolicies == null) {
            List<String> found = new ArrayList<>();
            for (int n = 0; n < 16; n++) {
                String p = "/sys/devices/system/cpu/cpufreq/policy" + n;
                if (new File(p).exists() && readInt(p + "/cpuinfo_max_freq") > 0) found.add(p);
            }
            if (found.isEmpty()) {
                // sem acesso direto: pergunta ao root
                String ls = RootUtils.runAndGetOutput("ls -d /sys/devices/system/cpu/cpufreq/policy* 2>/dev/null");
                if (ls != null) {
                    for (String p : ls.split("\\s+")) if (!p.isEmpty()) found.add(p.trim());
                }
            }
            mPolicies = found;
        }
        List<int[]> clusters = new ArrayList<>(); // {max, cur}
        List<String> govs = new ArrayList<>();
        for (String p : mPolicies) {
            int max = readInt(p + "/cpuinfo_max_freq");
            int cur = readInt(p + "/scaling_cur_freq");
            clusters.add(new int[]{max, cur});
            govs.add(readFile(p + "/scaling_governor"));
        }
        int bigIdx = -1, littleIdx = -1;
        for (int i = 0; i < clusters.size(); i++) {
            if (bigIdx < 0 || clusters.get(i)[0] > clusters.get(bigIdx)[0]) bigIdx = i;
            if (littleIdx < 0 || clusters.get(i)[0] < clusters.get(littleIdx)[0]) littleIdx = i;
        }
        if (bigIdx >= 0) {
            s.bigMax = clusters.get(bigIdx)[0] / 1000;
            s.bigCur = clusters.get(bigIdx)[1] / 1000;
            s.bigGov = govs.get(bigIdx);
            s.cpuMaxMhz = s.bigMax;
        }
        if (littleIdx >= 0 && littleIdx != bigIdx) {
            s.hasLittle = true;
            s.littleMax = clusters.get(littleIdx)[0] / 1000;
            s.littleCur = clusters.get(littleIdx)[1] / 1000;
        }

        int gpu = readInt("/sys/class/kgsl/kgsl-3d0/gpuclk");
        if (gpu == 0) gpu = readInt("/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq");
        s.gpuCur = gpu / 1000000;

        int t = readInt("/sys/class/power_supply/battery/temp");
        if (t != 0) s.tempC = t / 10f;

        String meminfo = readFile("/proc/meminfo");
        for (String line : meminfo.split("\n")) {
            String[] kv = line.split(":");
            if (kv.length < 2) continue;
            long v;
            try {
                v = Long.parseLong(kv[1].trim().split("\\s+")[0]);
            } catch (Exception e) {
                continue;
            }
            switch (kv[0].trim()) {
                case "MemTotal": s.memTotal = v; break;
                case "MemAvailable": s.memAvail = v; break;
                case "SwapTotal": s.swapTotal = v; break;
                case "SwapFree": s.swapFree = v; break;
            }
        }
        return s;
    }

    // ---------------------------------------------------------------- tela

    private void show(Snapshot s) {
        if (s.kernelName != null) {
            mStaticLoaded = true;
            mKernelName.setText(s.kernelName);
            mKernelVersion.setText(s.kernelVersion);
            mZramAlgo.setText(s.zramAlgo.isEmpty() ? "" : "ZRAM " + s.zramAlgo);

            mKernelChips.removeAllViews();
            if (s.cpuMaxMhz > 0) addChip("CPU " + s.cpuMaxMhz + " MHz", true);
            if (s.gpuMaxMhz > 0) addChip("GPU " + s.gpuMaxMhz, false);
            float hz = requireActivity().getWindowManager().getDefaultDisplay().getRefreshRate();
            if (hz > 0) addChip(Math.round(hz) + " Hz", false);
        }

        mTileBig.value.setText(s.bigCur > 0 ? String.valueOf(s.bigCur) : "—");
        mTileBig.sub.setText(getString(R.string.dx_mhz_gov, s.bigGov == null ? "" : prettyGov(s.bigGov)));
        mTileBig.bar.setProgress(s.bigMax > 0 ? s.bigCur * 100 / s.bigMax : 0);

        if (s.hasLittle) {
            mTileLittle.value.setText(s.littleCur > 0 ? String.valueOf(s.littleCur) : "—");
            mTileLittle.sub.setText(getString(R.string.dx_mhz_max, s.littleMax));
            mTileLittle.bar.setProgress(s.littleMax > 0 ? s.littleCur * 100 / s.littleMax : 0);
        } else {
            mTileLittle.value.setText("—");
            mTileLittle.sub.setText("");
        }

        mTileTemp.value.setText(Float.isNaN(s.tempC) ? "—" : String.format(PT, "%.0f°C", s.tempC));

        mTileGpu.value.setText(s.gpuCur > 0 ? String.valueOf(s.gpuCur) : "—");
        mTileGpu.sub.setText(s.gpuMaxMhz > 0 ? getString(R.string.dx_mhz_max, s.gpuMaxMhz) : "MHz");

        if (s.memTotal > 0) {
            long used = s.memTotal - s.memAvail;
            mRamText.setText(getString(R.string.dx_of, gb(used), gb(s.memTotal)));
            mRamBar.setProgress((int) (used * 100 / s.memTotal));
        }
        if (s.swapTotal > 0) {
            long used = s.swapTotal - s.swapFree;
            mZramText.setText(getString(R.string.dx_of, size(used), gb(s.swapTotal)));
            mZramBar.setProgress((int) (used * 100 / s.swapTotal));
        } else {
            mZramText.setText(R.string.dx_off);
            mZramBar.setProgress(0);
        }
    }

    private String prettyGov(String gov) {
        if (gov.equals("schedutil") && mStaticLoaded && mKernelName.getText().toString().contains("Dr.xok")) {
            return "eco";
        }
        return gov;
    }

    private void addChip(String text, boolean dark) {
        Context c = requireContext();
        MaterialTextView chip = new MaterialTextView(c);
        chip.setText(text);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        chip.setTypeface(ResourcesCompat.getFont(c, R.font.inter_semibold));
        int px = (int) (12 * getResources().getDisplayMetrics().density);
        int py = (int) (7 * getResources().getDisplayMetrics().density);
        chip.setPadding(px, py, px, py);
        chip.setBackgroundResource(dark ? R.drawable.dx_chip_dark : R.drawable.dx_chip_soft);
        chip.setTextColor(ContextCompat.getColor(c, dark ? R.color.dx_text : R.color.dx_ink));
        mKernelChips.addView(chip);
    }

    private static String gb(long kb) {
        return String.format(PT, "%.2f GB", kb / 1048576f);
    }

    private static String size(long kb) {
        if (kb < 1048576) return String.format(PT, "%d MB", kb / 1024);
        return gb(kb);
    }
}
