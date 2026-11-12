/*
 * Copyright (C) 2015-2016 Willi Ye <williye97@gmail.com>
 *
 * This file is part of Kernel Adiutor.
 *
 * Kernel Adiutor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Kernel Adiutor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Kernel Adiutor.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.smartpack.kernelmanager.fragments.kernel;

import android.text.InputType;

import com.smartpack.kernelmanager.R;
import com.smartpack.kernelmanager.utils.DxKernel;
import com.smartpack.kernelmanager.views.recyclerview.DescriptionView;
import com.smartpack.kernelmanager.views.recyclerview.DxChipsView;
import com.smartpack.kernelmanager.fragments.ApplyOnBootFragment;
import com.smartpack.kernelmanager.fragments.RecyclerViewFragment;
import com.smartpack.kernelmanager.utils.Prefs;
import com.smartpack.kernelmanager.utils.kernel.vm.VM;
import com.smartpack.kernelmanager.utils.kernel.vm.ZRAM;
import com.smartpack.kernelmanager.utils.kernel.vm.ZSwap;
import com.smartpack.kernelmanager.views.recyclerview.CardView;
import com.smartpack.kernelmanager.views.recyclerview.GenericSelectView;
import com.smartpack.kernelmanager.views.recyclerview.RecyclerViewItem;
import com.smartpack.kernelmanager.views.recyclerview.SeekBarView;
import com.smartpack.kernelmanager.views.recyclerview.SelectView;
import com.smartpack.kernelmanager.views.recyclerview.SwitchView;

import java.util.List;

/**
 * Created by willi on 29.06.16.
 */
public class VMFragment extends RecyclerViewFragment {

    @Override
    protected void init() {
        super.init();

        addViewPagerFragment(ApplyOnBootFragment.newInstance(this));
    }

    @Override
    protected void addItems(List<RecyclerViewItem> items) {
        VMInit(items);
        dxMemoryInit(items);
        if (ZRAM.supported()) {
            zramInit(items);
        }
        zswapInit(items);
    }

    private void VMInit(List<RecyclerViewItem> items) {
        CardView vmCard = new CardView(getActivity());
        vmCard.setTitle(getString(R.string.virtual_memory));

        if (VM.hasDirNotify()) {
            SwitchView dirNotifier = new SwitchView();
            dirNotifier.setTitle(getString(R.string.dir_change_notifier));
            dirNotifier.setSummary(getString(R.string.dir_change_notifier_summary));
            dirNotifier.setChecked(VM.isDirNotifyEnabled());
            dirNotifier.addOnSwitchListener((switchView, isChecked) -> VM.enableDirNotify(isChecked, getActivity()));

            vmCard.addItem(dirNotifier);
        }

        if (VM.hasStatInterval()) {
            GenericSelectView statInterval = new GenericSelectView();
            statInterval.setTitle(getString(R.string.vm_stat_interval) + (" (s)"));
            statInterval.setSummary(getString(R.string.vm_stat_interval_summary));
            statInterval.setValue(VM.getStatInterval());
            statInterval.setInputType(InputType.TYPE_CLASS_NUMBER);
            statInterval.setOnGenericValueListener((genericSelectView, value) -> {
                VM.setStatInterval(value, getActivity());
                genericSelectView.setValue(value);
                getHandler().postDelayed(() -> genericSelectView.setValue(VM.getStatInterval()),500);
            });

            vmCard.addItem(statInterval);
        }

        if (!(Prefs.getBoolean("vm_tunables", false, getActivity())))
            Prefs.saveBoolean("vm_tunables", false, getActivity());

        final SwitchView vmTunables = new SwitchView();
        vmTunables.setSummary(getString(R.string.virtual_memory_tunables));
        vmTunables.setChecked(Prefs.getBoolean("vm_tunables", false, getActivity()));

        vmCard.addItem(vmTunables);

        for (int i = 0; i < VM.size(); i++) {
            if (VM.exists(i)) {
                GenericSelectView vm = new GenericSelectView();
                vm.setSummary(VM.getName(i));
                vm.setValue(VM.getValue(i));
                vm.setValueRaw(vm.getValue());
                vm.setInputType(InputType.TYPE_CLASS_NUMBER);

                final int position = i;
                vm.setOnGenericValueListener((genericSelectView, value) -> {
                    VM.setValue(value, position, getActivity());
                    genericSelectView.setValue(value);
                });

                class vmTunablesManager {
                    private void showVMTunables (boolean enable) {
                        if (enable) {
                            vmCard.addItem(vm);
                        } else {
                            vmCard.removeItem(vm);
                        }
                    }
                }

                final vmTunablesManager manager = new vmTunablesManager();
                manager.showVMTunables(Prefs.getBoolean("vm_tunables", false, getActivity()));
                vmTunables.addOnSwitchListener((switchview, isChecked) -> {
                    Prefs.saveBoolean("vm_tunables", isChecked, getActivity());
                    manager.showVMTunables(isChecked);
                });
            }
        }

        if (vmCard.size() > 0) {
            items.add(vmCard);
        }
    }

    /** Kernel Dr.xok v19: compactação, writeback e congelador */
    private void dxMemoryInit(List<RecyclerViewItem> items) {
        CardView card = new CardView(getActivity());
        card.setTitle(getString(R.string.dx_mem_title));

        if (DxKernel.hasBgReclaim()) {
            SwitchView bg = new SwitchView();
            bg.setTitle(getString(R.string.dx_bg_reclaim));
            int last = DxKernel.bgReclaimLastMb();
            bg.setSummary(getString(R.string.dx_bg_reclaim_summary)
                    + (last > 0 ? "\n" + getString(R.string.dx_bg_reclaim_last, last) : ""));
            bg.setChecked(DxKernel.isBgReclaim());
            bg.addOnSwitchListener((v, on) -> DxKernel.setBgReclaim(on, getActivity()));
            card.addItem(bg);
        }

        if (DxKernel.hasWriteback()) {
            SwitchView wb = new SwitchView();
            wb.setTitle(getString(R.string.dx_writeback));
            String state = DxKernel.isWritebackActive()
                    ? getString(R.string.dx_writeback_active, DxKernel.writebackMb())
                    : getString(R.string.dx_writeback_inactive);
            wb.setSummary(getString(R.string.dx_writeback_summary) + "\n" + state);
            wb.setChecked(DxKernel.isWritebackWanted());
            wb.addOnSwitchListener((v, on) -> {
                DxKernel.setWritebackWanted(on);
                v.setSummary(getString(R.string.dx_writeback_summary) + "\n"
                        + getString(R.string.dx_after_reboot));
            });
            card.addItem(wb);
        }

        if (DxKernel.hasFreezer()) {
            SwitchView fr = new SwitchView();
            fr.setTitle(getString(R.string.dx_freezer));
            fr.setSummary(getString(R.string.dx_freezer_summary));
            fr.setChecked(DxKernel.isFreezerEnabled());
            fr.addOnSwitchListener((v, on) -> {
                DxKernel.setFreezerEnabled(on);
                v.setSummary(getString(R.string.dx_freezer_summary) + "\n"
                        + getString(R.string.dx_after_reboot));
            });
            card.addItem(fr);
        }

        if (DxKernel.hasLz4kd()) {
            String[] algos = {"zstd", "lz4kd"};
            String cur = DxKernel.currentAlgo();
            DxChipsView algo = new DxChipsView();
            algo.setTitle(getString(R.string.dx_zram_algo));
            algo.setSummary(getString(R.string.dx_zram_algo_summary, cur));
            algo.setAccent(R.color.dx_butter, R.color.dx_butter_ink);
            algo.setItems(java.util.Arrays.asList(algos));
            algo.setItem("lz4kd".equals(cur) ? 1 : 0);
            algo.setOnItemSelected((view, position, item) -> {
                DxKernel.setWantedAlgo(algos[position]);
                view.setSummary(getString(R.string.dx_zram_algo_summary, cur) + "\n"
                        + getString(R.string.dx_after_reboot));
            });
            card.addItem(algo);
        }

        if (DxKernel.hasMglru()) {
            SwitchView mglru = new SwitchView();
            mglru.setTitle(getString(R.string.dx_mglru));
            mglru.setSummary(getString(R.string.dx_mglru_summary));
            mglru.setChecked(DxKernel.isMglru());
            mglru.addOnSwitchListener((v, on) -> DxKernel.setMglru(on, getActivity()));
            card.addItem(mglru);
        }

        if (DxKernel.hasProtection()) {
            DescriptionView prot = new DescriptionView();
            prot.setTitle(getString(R.string.dx_protected));
            prot.setSummary(getString(R.string.dx_protected_summary,
                    DxKernel.protectedPackages().size()));
            prot.setOnItemClickListener(item -> showProtectedDialog(prot));
            card.addItem(prot);
        }

        if (DxKernel.isPerAppMemcg()) {
            DescriptionView memcg = new DescriptionView();
            memcg.setTitle(getString(R.string.dx_memcg));
            memcg.setSummary(getString(R.string.dx_memcg_summary));
            card.addItem(memcg);
        }

        if (card.size() > 0) items.add(card);
    }

    /** Kernel Dr.xok: lista de apps protegidos da ZRAM */
    private void showProtectedDialog(DescriptionView prot) {
        android.content.pm.PackageManager pm = requireActivity().getPackageManager();
        android.content.Intent main = new android.content.Intent(android.content.Intent.ACTION_MAIN)
                .addCategory(android.content.Intent.CATEGORY_LAUNCHER);
        java.util.TreeMap<String, String> apps = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (android.content.pm.ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
            String pkg = ri.activityInfo.packageName;
            apps.put(ri.loadLabel(pm) + "  (" + pkg + ")", pkg);
        }
        java.util.List<String> cur = DxKernel.protectedPackages();
        // apps do sistema protegidos que não aparecem na gaveta (ex.: interface do sistema)
        for (String p : cur) {
            if (!apps.containsValue(p)) apps.put(p, p);
        }
        String[] labels = apps.keySet().toArray(new String[0]);
        String[] pkgs = apps.values().toArray(new String[0]);
        boolean[] checked = new boolean[pkgs.length];
        for (int i = 0; i < pkgs.length; i++) checked[i] = cur.contains(pkgs[i]);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.dx_protected)
                .setMultiChoiceItems(labels, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.apply, (d, w) -> {
                    java.util.List<String> sel = new java.util.ArrayList<>();
                    for (int i = 0; i < pkgs.length; i++) if (checked[i]) sel.add(pkgs[i]);
                    DxKernel.setProtectedPackages(sel, requireActivity());
                    prot.setSummary(getString(R.string.dx_protected_summary, sel.size()));
                })
                .show();
    }

    /** Kernel Dr.xok: quanto o dedup já economizou */
    private String dedupSummary() {
        long[] st = DxKernel.dedupStat();
        long net = st[1] - st[2];
        String base = getString(R.string.dx_dedup_summary);
        if (st[0] <= 0) return base;
        return base + "\n" + getString(R.string.dx_dedup_saved, st[0],
                Math.max(0, net) / (1024f * 1024f));
    }

    private void zramInit(List<RecyclerViewItem> items) {
        CardView zRAM = new CardView(getActivity());
        zRAM.setTitle(getString(R.string.zram));

        SeekBarView zram = new SeekBarView();
        zram.setTitle(getString(R.string.disksize));
        zram.setSummary(getString(R.string.disksize_summary));
        zram.setUnit(getString(R.string.mb));
        zram.setMax(4096);
        zram.setOffset(8);
        zram.setProgress(ZRAM.getDisksize() / 8);
        zram.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
            @Override
            public void onStop(SeekBarView seekBarView, int position, String value) {
                ZRAM.setDisksize(position * 8L, getActivity());
                getHandler().postDelayed(() -> zram.setProgress(ZRAM.getDisksize() / 8),
                        500);
            }

            @Override
            public void onMove(SeekBarView seekBarView, int position, String value) {
            }
        });

        zRAM.addItem(zram);

        int zRAMvalue = ZRAM.getDisksize();

        if (ZRAM.hasZRAMAlgo()) {
            SelectView zramAlgo = new SelectView();
            zramAlgo.setTitle(getString(R.string.zram_algo));
            zramAlgo.setSummary(getString(R.string.zram_algo_summary) + ("\n") + getString(R.string.warning) +
                    (" ") + getString(R.string.zram_algo_warning));
            zramAlgo.setItems(ZRAM.getAvailableZRAMAlgos());
            zramAlgo.setItem(ZRAM.getZRAMAlgo());
            zramAlgo.setOnItemSelected((selectView, position, item) -> {
                ZRAM.setDisksize(0, getActivity());
                ZRAM.setZRAMAlgo(item, getActivity());
                ZRAM.setDisksize((zRAMvalue), getActivity());
                getHandler().postDelayed(() -> zramAlgo.setItem(ZRAM.getZRAMAlgo()),
                        500);
            });

            zRAM.addItem(zramAlgo);
        }

        if (DxKernel.hasDedup()) {
            SwitchView dedup = new SwitchView();
            dedup.setTitle(getString(R.string.dx_dedup));
            dedup.setSummary(dedupSummary());
            dedup.setChecked(DxKernel.isDedup());
            dedup.addOnSwitchListener((switchView, isChecked) -> {
                DxKernel.setDedup(isChecked, getActivity());
                getHandler().postDelayed(() -> switchView.setSummary(dedupSummary()), 300);
            });
            zRAM.addItem(dedup);
        }

        if (zRAM.size() > 0) {
            items.add(zRAM);
        }
    }

    private void zswapInit(List<RecyclerViewItem> items) {
        CardView zswapCard = new CardView(getActivity());
        zswapCard.setTitle(getString(R.string.zswap));

        if (ZSwap.hasEnable()) {
            SwitchView zswap = new SwitchView();
            zswap.setTitle(getString(R.string.zswap));
            zswap.setSummary(getString(R.string.zswap_summary));
            zswap.setChecked(ZSwap.isEnabled());
            zswap.addOnSwitchListener((switchView, isChecked) -> {
                ZSwap.enable(isChecked, getActivity());
        getHandler().postDelayed(() -> zswap.setChecked(ZSwap.isEnabled()),
        500);
            });

            zswapCard.addItem(zswap);
        }

        if (ZSwap.hasMaxPoolPercent()) {
            SeekBarView maxPoolPercent = new SeekBarView();
            maxPoolPercent.setTitle(getString(R.string.memory_pool));
            maxPoolPercent.setSummary(getString(R.string.memory_pool_summary));
            maxPoolPercent.setUnit("%");
            maxPoolPercent.setMax(50);
            maxPoolPercent.setProgress(ZSwap.getMaxPoolPercent());
            maxPoolPercent.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                @Override
                public void onStop(SeekBarView seekBarView, int position, String value) {
                    ZSwap.setMaxPoolPercent(position, getActivity());
		    getHandler().postDelayed(() -> maxPoolPercent.setProgress(ZSwap.getMaxPoolPercent()),
	    	500);
                }

                @Override
                public void onMove(SeekBarView seekBarView, int position, String value) {
                }
            });

            zswapCard.addItem(maxPoolPercent);
        }

        if (ZSwap.hasMaxCompressionRatio()) {
            SeekBarView maxCompressionRatio = new SeekBarView();
            maxCompressionRatio.setTitle(getString(R.string.maximum_compression_ratio));
            maxCompressionRatio.setSummary(getString(R.string.maximum_compression_ratio_summary));
            maxCompressionRatio.setUnit("%");
            maxCompressionRatio.setProgress(ZSwap.getMaxCompressionRatio());
            maxCompressionRatio.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                @Override
                public void onStop(SeekBarView seekBarView, int position, String value) {
                    ZSwap.setMaxCompressionRatio(position, getActivity());
		    getHandler().postDelayed(() -> maxCompressionRatio.setProgress(ZSwap.getMaxCompressionRatio()),
	    	500);
                }

                @Override
                public void onMove(SeekBarView seekBarView, int position, String value) {
                }
            });

            zswapCard.addItem(maxCompressionRatio);
        }

        if (zswapCard.size() > 0) {
            items.add(zswapCard);
        }
    }

}