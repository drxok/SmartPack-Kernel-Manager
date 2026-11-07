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

    // --- v19: compactar apps em segundo plano ao apagar a tela
    public static final String BG_RECLAIM = "/sys/module/dx_bg_reclaim/parameters/enabled";
    public static final String BG_RECLAIM_LAST = "/sys/module/dx_bg_reclaim/parameters/last_mb";

    public static boolean hasBgReclaim() {
        return DxInfo.exists(BG_RECLAIM);
    }

    public static boolean isBgReclaim() {
        return yes(DxInfo.read(BG_RECLAIM));
    }

    public static int bgReclaimLastMb() {
        return DxInfo.readInt(BG_RECLAIM_LAST);
    }

    public static void setBgReclaim(boolean on, Context context) {
        Control.runSetting(Control.write(on ? "Y" : "N", BG_RECLAIM),
                ApplyOnBootFragment.VM, BG_RECLAIM, context);
    }

    // --- v19: ZRAM com writeback para a memória interna
    public static final String BD_STAT = "/sys/block/zram0/bd_stat";
    public static final String BACKING_DEV = "/sys/block/zram0/backing_dev";
    public static final String WB_OFF_FLAG = "/data/vendor/dx_zram/desligado";

    public static boolean hasWriteback() {
        return DxInfo.exists(BACKING_DEV);
    }

    /** ativo agora (tem arquivo de apoio ligado) */
    public static boolean isWritebackActive() {
        String b = DxInfo.read(BACKING_DEV);
        return !b.isEmpty() && !"none".equals(b);
    }

    /** vai ser ligado no próximo boot */
    public static boolean isWritebackWanted() {
        return !"1".equals(com.smartpack.kernelmanager.utils.root.RootUtils
                .runAndGetOutput("[ -e " + WB_OFF_FLAG + " ] && echo 1"));
    }

    public static void setWritebackWanted(boolean on) {
        com.smartpack.kernelmanager.utils.root.RootUtils.runCommand(on ? "rm -f " + WB_OFF_FLAG
                : "mkdir -p /data/vendor/dx_zram && touch " + WB_OFF_FLAG);
    }

    /** MB guardados agora na memória interna (bd_count em páginas de 4 KB) */
    public static int writebackMb() {
        try {
            String[] p = DxInfo.read(BD_STAT).trim().split("\\s+");
            return (int) (Long.parseLong(p[0]) * 4 / 1024);
        } catch (Exception e) {
            return 0;
        }
    }

    // --- v19: congelador de apps do Android 12
    public static final String FREEZE_CHECK = "/sys/fs/cgroup/uid_0/cgroup.freeze";

    public static boolean hasFreezer() {
        return DxInfo.exists(FREEZE_CHECK);
    }

    public static boolean isFreezerEnabled() {
        String v = com.smartpack.kernelmanager.utils.root.RootUtils
                .runAndGetOutput("settings get global cached_apps_freezer");
        return v != null && v.trim().equals("enabled");
    }

    public static void setFreezerEnabled(boolean on) {
        com.smartpack.kernelmanager.utils.root.RootUtils.runCommand(
                "settings put global cached_apps_freezer " + (on ? "enabled" : "disabled"));
    }
}
