package com.smartpack.kernelmanager.utils;

import android.content.Context;

import com.smartpack.kernelmanager.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Kernel Dr.xok: nomes e explicações em português para os ajustes dos governadores.
 */
public class DxTunables {

    private static final Map<String, int[]> sMap = new HashMap<>();

    static {
        sMap.put("efficient_freq", new int[]{R.string.dx_t_efficient_freq, R.string.dx_t_efficient_freq_d});
        sMap.put("up_delay_us", new int[]{R.string.dx_t_up_delay, R.string.dx_t_up_delay_d});
        sMap.put("up_rate_limit_us", new int[]{R.string.dx_t_up_rate, R.string.dx_t_up_rate_d});
        sMap.put("down_rate_limit_us", new int[]{R.string.dx_t_down_rate, R.string.dx_t_down_rate_d});
        sMap.put("rate_limit_us", new int[]{R.string.dx_t_rate, R.string.dx_t_rate_d});
        sMap.put("hispeed_freq", new int[]{R.string.dx_t_hispeed_freq, R.string.dx_t_hispeed_freq_d});
        sMap.put("hispeed_load", new int[]{R.string.dx_t_hispeed_load, R.string.dx_t_hispeed_load_d});
        sMap.put("iowait_boost_enable", new int[]{R.string.dx_t_iowait, R.string.dx_t_iowait_d});
        sMap.put("pl", new int[]{R.string.dx_t_pl, R.string.dx_t_pl_d});
    }

    /** título amigável, ou null se não houver tradução */
    public static String title(String name, Context context) {
        int[] r = sMap.get(name);
        return r == null ? null : context.getString(r[0]);
    }

    public static String description(String name, Context context) {
        int[] r = sMap.get(name);
        return r == null ? null : context.getString(r[1]);
    }
}
