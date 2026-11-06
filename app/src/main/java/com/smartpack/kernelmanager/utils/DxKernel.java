package com.smartpack.kernelmanager.utils;

import android.content.Context;

import com.smartpack.kernelmanager.fragments.ApplyOnBootFragment;
import com.smartpack.kernelmanager.utils.root.Control;

/**
 * Kernel Dr.xok v18: controles que só existem no kernel Dr.xok.
 */
public class DxKernel {

    public static final String FAST_MIGRATE = "/proc/sys/kernel/sched_dx_fast_migrate";
    public static final String DEDUP = "/sys/module/zram/parameters/dedup";
    public static final String DEDUP_STAT = "/sys/block/zram0/dedup_stat";
    public static final String THERMAL_PROFILE = "/sys/module/dx_thermal/parameters/profile";

    private static boolean yes(String v) {
        return "Y".equals(v) || "1".equals(v);
    }

    // --- núcleos fortes mais cedo (WALT)
    public static boolean hasFastMigrate() {
        return DxInfo.exists(FAST_MIGRATE);
    }

    public static boolean isFastMigrate() {
        return yes(DxInfo.read(FAST_MIGRATE));
    }

    public static void setFastMigrate(boolean on, Context context) {
        Control.runSetting(Control.write(on ? "1" : "0", FAST_MIGRATE),
                ApplyOnBootFragment.CPU, FAST_MIGRATE, context);
    }

    // --- dedup da ZRAM
    public static boolean hasDedup() {
        return DxInfo.exists(DEDUP);
    }

    public static boolean isDedup() {
        return yes(DxInfo.read(DEDUP));
    }

    public static void setDedup(boolean on, Context context) {
        Control.runSetting(Control.write(on ? "Y" : "N", DEDUP),
                ApplyOnBootFragment.VM, DEDUP, context);
    }

    /** {páginas repetidas, bytes economizados, bytes de controle} */
    public static long[] dedupStat() {
        long[] out = new long[3];
        try {
            String[] p = DxInfo.read(DEDUP_STAT).trim().split("\\s+");
            for (int i = 0; i < 3 && i < p.length; i++) out[i] = Long.parseLong(p[i]);
        } catch (Exception ignored) {
        }
        return out;
    }

    // --- perfil térmico
    public static boolean hasThermalProfile() {
        return DxInfo.exists(THERMAL_PROFILE);
    }

    public static int getThermalProfile() {
        return DxInfo.readInt(THERMAL_PROFILE);
    }

    public static void setThermalProfile(int profile, Context context) {
        Control.runSetting(Control.write(String.valueOf(profile), THERMAL_PROFILE),
                ApplyOnBootFragment.THERMAL, THERMAL_PROFILE, context);
    }
}
