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

import android.content.Intent;
import android.text.InputType;
import android.util.SparseArray;

import com.smartpack.kernelmanager.R;
import com.smartpack.kernelmanager.activities.CPUBoostActivity;
import com.smartpack.kernelmanager.fragments.ApplyOnBootFragment;
import com.smartpack.kernelmanager.fragments.DescriptionFragment;
import com.smartpack.kernelmanager.fragments.RecyclerViewFragment;
import com.smartpack.kernelmanager.utils.Device;
import com.smartpack.kernelmanager.utils.Prefs;
import com.smartpack.kernelmanager.utils.Utils;
import com.smartpack.kernelmanager.utils.ViewUtils;
import com.smartpack.kernelmanager.utils.kernel.cpu.CPUFreq;
import com.smartpack.kernelmanager.utils.kernel.cpu.Misc;
import com.smartpack.kernelmanager.utils.kernel.cpu.boost.CPUBoost;
import com.smartpack.kernelmanager.utils.kernel.cpu.boost.CPUInputBoost;
import com.smartpack.kernelmanager.utils.kernel.cpu.boost.StuneBoost;
import com.smartpack.kernelmanager.utils.kernel.cpu.boost.VoxPopuli;
import com.smartpack.kernelmanager.utils.kernel.cpuhotplug.MSMLimiter;
import com.smartpack.kernelmanager.utils.tools.PathReader;
import com.smartpack.kernelmanager.views.dialog.Dialog;
import com.smartpack.kernelmanager.views.recyclerview.CardView;
import com.smartpack.kernelmanager.views.recyclerview.DescriptionView;
import com.smartpack.kernelmanager.views.recyclerview.DxChipsView;
import com.smartpack.kernelmanager.utils.root.Control;
import com.smartpack.kernelmanager.views.recyclerview.SeekBarView;
import com.smartpack.kernelmanager.views.recyclerview.DxClusterHeroView;
import com.smartpack.kernelmanager.views.recyclerview.GenericSelectView;
import com.smartpack.kernelmanager.views.recyclerview.RecyclerViewItem;
import com.smartpack.kernelmanager.views.recyclerview.SelectView;
import com.smartpack.kernelmanager.views.recyclerview.SwitchView;
import com.smartpack.kernelmanager.views.recyclerview.XYGraphView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * Created by willi on 01.05.16.
 */
public class CPUFragment extends RecyclerViewFragment {

    private CPUFreq mCPUFreq;

    private XYGraphView mCPUUsageBig;
    private SelectView mCPUMaxBig;
    private SelectView mCPUMinBig;
    private SelectView mCPUMaxScreenOffBig;
    private DxChipsView mCPUGovernorBig;
    private DxClusterHeroView mHeroBig, mHeroLITTLE;

    private XYGraphView mCPUUsageMid;
    private SelectView mCPUMaxMid;
    private SelectView mCPUMinMid;
    private SelectView mCPUMaxScreenOffMid;
    private DxChipsView mCPUGovernorMid;

    private XYGraphView mCPUUsageLITTLE;
    private SelectView mCPUMaxLITTLE;
    private SelectView mCPUMinLITTLE;
    private SelectView mCPUMaxScreenOffLITTLE;
    private DxChipsView mCPUGovernorLITTLE;

    private final SparseArray<SwitchView> mCoresBig = new SparseArray<>();
    private final SparseArray<SwitchView> mCoresMid = new SparseArray<>();
    private final SparseArray<SwitchView> mCoresLITTLE = new SparseArray<>();

    private Dialog mGovernorTunableErrorDialog;

    @Override
    protected void init() {
        super.init();

        mCPUFreq = CPUFreq.getInstance(getActivity());
        addViewPagerFragment(ApplyOnBootFragment.newInstance(this));
        addViewPagerFragment(DescriptionFragment.newInstance(getString(mCPUFreq.getCpuCount() > 1 ?
                R.string.cores : R.string.cores_singular, mCPUFreq.getCpuCount()), Device.getBoard()));

        if (mGovernorTunableErrorDialog != null) {
            mGovernorTunableErrorDialog.show();
        }

        showCPUNotes();
    }

    @Override
    protected void addItems(List<RecyclerViewItem> items) {
        cpuInit(items);
        if (Misc.hasMcPowerSaving()) {
            mcPowerSavingInit(items);
        }
        if (Misc.hasPowerSavingWq()) {
            powerSavingWqInit(items);
        }
        if (Misc.hasCFSScheduler()) {
            cfsSchedulerInit(items);
        }
        if (Misc.hasCpuQuiet()) {
            cpuQuietInit(items);
        }
        if (CPUBoost.getInstance().supported() || CPUInputBoost.getInstance().supported()
                || StuneBoost.supported() || VoxPopuli.hasVoxpopuliTunable()) {
            cpuBoostInit(items);
        }
        if (MSMLimiter.supported()) {
            msmlimiterInit(items);
        }
    }

    private void cpuInit(List<RecyclerViewItem> items) {
        CardView cpuCard = new CardView(getActivity());
        if (!mCPUFreq.isBigLITTLE()) {
            cpuCard.setTitle(getString(R.string.cpu));
        }

        // Kernel Dr.xok: no lugar do gráfico de uso, o cartão de destaque do grupo
        final List<Integer> bigCores = mCPUFreq.getBigCpuRange();
        mHeroBig = makeHero(mCPUFreq.isBigLITTLE() ? getString(R.string.dx_cluster_big_t) : getString(R.string.cpu),
                bigCores, mCPUFreq.getBigCpu(), R.color.dx_lilac, R.color.dx_ink);

        // Kernel Dr.xok: a frequência de cada núcleo já aparece no cartão de cima;
        // aqui ficam só os limites, em barras
        mCoresBig.clear();
        addFreqBars(cpuCard, mCPUFreq.getBigCpu(), bigCores);

        if (mCPUFreq.hasMaxScreenOffFreq()) {
            mCPUMaxScreenOffBig = new SelectView();
            mCPUMaxScreenOffBig.setTitle(getString(R.string.cpu_max_screen_off_freq));
            mCPUMaxScreenOffBig.setSummary(getString(R.string.cpu_max_screen_off_freq_summary));
            mCPUMaxScreenOffBig.setItems(mCPUFreq.getAdjustedFreq(getActivity()));
            mCPUMaxScreenOffBig.setOnItemSelected((selectView, position, item) -> mCPUFreq.setMaxScreenOffFreq(mCPUFreq.getFreqs().get(position), bigCores.get(0),
                    bigCores.get(bigCores.size() - 1), getActivity()));
            cpuCard.addItem(mCPUMaxScreenOffBig);
        }

        cpuCard.setTitle(getString(R.string.dx_limits, mCPUFreq.isBigLITTLE()
                ? getString(R.string.dx_cluster_big_t) : getString(R.string.cpu)));

        mCPUGovernorBig = new DxChipsView();
        mCPUGovernorBig.setTitle(getString(R.string.dx_governor));
        mCPUGovernorBig.setStandalone(true);
        mCPUGovernorBig.setItems(mCPUFreq.getGovernors());
        labelGovernors(mCPUGovernorBig);
        mCPUGovernorBig.setPrimary(DX_MAIN_GOVS, getString(R.string.dx_more));
        mCPUGovernorBig.setOnItemSelected((selectView, position, item) -> mCPUFreq.setGovernor(item, bigCores.get(0), bigCores.get(bigCores.size() - 1),
                getActivity()));

        DescriptionView governorTunablesBig = new DescriptionView();
        governorTunablesBig.setTitle(getString(R.string.cpu_governor_tunables));
        governorTunablesBig.setSummary(getString(R.string.governor_tunables_summary));
        governorTunablesBig.setOnItemClickListener(item -> showGovernorTunables(bigCores.get(0), bigCores.get(bigCores.size() - 1)));

        items.add(mHeroBig);
        items.add(cpuCard);
        items.add(mCPUGovernorBig);
        addEcoCard(items, mCPUFreq.getBigCpu(), mCPUFreq.isBigLITTLE() ? getString(R.string.dx_cluster_big_t) : null,
                R.color.dx_pink, R.color.dx_pink_ink);
        addTunablesRow(items, governorTunablesBig);

        if (mCPUFreq.isBigLITTLE()) {
            if (mCPUFreq.hasMidCpu()) {
                CardView cpuMidCard = new CardView(getActivity());
                cpuMidCard.setTitle(getString(R.string.dx_cluster_mid_t));

                mCPUUsageMid = new XYGraphView();
                mCPUUsageMid.setTitle(getString(R.string.dx_usage_now));

                mCPUUsageLITTLE = new XYGraphView();
                mCPUUsageLITTLE.setTitle(getString(R.string.cpu_usage_string, getString(R.string.cluster_middle)));

                cpuMidCard.addItem(mCPUUsageMid);

                final List<Integer> MidCores = mCPUFreq.getMidCpuRange();

                mCoresMid.clear();
                for (final int core : MidCores) {
                    SwitchView coreSwitch = new SwitchView();
                    coreSwitch.setSummary(getString(R.string.core, core + 1));

                    mCoresMid.put(core, coreSwitch);
                    cpuMidCard.addItem(coreSwitch);
                }

                mCPUMaxMid = new SelectView();
                mCPUMaxMid.setTitle(getString(R.string.cpu_max_freq));
                mCPUMaxMid.setSummary(getString(R.string.cpu_max_freq_summary));
                mCPUMaxMid.setItems(mCPUFreq.getAdjustedFreq(mCPUFreq.getMidCpu(), getActivity()));
                mCPUMaxMid.setOnItemSelected((selectView, position, item)
                        -> mCPUFreq.setMaxFreq(mCPUFreq.getFreqs(mCPUFreq.getMidCpu()).get(position),
                        MidCores.get(0), MidCores.get(MidCores.size() - 1), getActivity()));
                cpuMidCard.addItem(mCPUMaxMid);

                mCPUMinMid = new SelectView();
                mCPUMinMid.setTitle(getString(R.string.cpu_min_freq));
                mCPUMinMid.setSummary(getString(R.string.cpu_min_freq_summary));
                mCPUMinMid.setItems(mCPUFreq.getAdjustedFreq(mCPUFreq.getMidCpu(), getActivity()));
                mCPUMinMid.setOnItemSelected((selectView, position, item)
                        -> mCPUFreq.setMinFreq(mCPUFreq.getFreqs(mCPUFreq.getMidCpu()).get(position),
                        MidCores.get(0), MidCores.get(MidCores.size() - 1), getActivity()));
                cpuMidCard.addItem(mCPUMinMid);

                if (mCPUFreq.hasMaxScreenOffFreq(mCPUFreq.getMidCpu())) {
                    mCPUMaxScreenOffMid = new SelectView();
                    mCPUMaxScreenOffMid.setTitle(getString(R.string.cpu_max_screen_off_freq));
                    mCPUMaxScreenOffMid.setSummary(getString(R.string.cpu_max_screen_off_freq_summary));
                    mCPUMaxScreenOffMid.setItems(mCPUFreq.getAdjustedFreq(mCPUFreq.getMidCpu(), getActivity()));
                    mCPUMaxScreenOffMid.setOnItemSelected((selectView, position, item)
                            -> mCPUFreq.setMaxScreenOffFreq(mCPUFreq.getFreqs(mCPUFreq.getMidCpu()).get(position),
                            MidCores.get(0), MidCores.get(MidCores.size() - 1), getActivity()));
                    cpuMidCard.addItem(mCPUMaxScreenOffMid);
                }

                mCPUGovernorMid = new DxChipsView();
                mCPUGovernorMid.setTitle(getString(R.string.cpu_governor));
                mCPUGovernorMid.setSummary(getString(R.string.cpu_governor_summary));
                mCPUGovernorMid.setItems(mCPUFreq.getGovernors());
        labelGovernors(mCPUGovernorMid);
                mCPUGovernorMid.setOnItemSelected((selectView, position, item)
                        -> mCPUFreq.setGovernor(item, MidCores.get(0), MidCores.get(MidCores.size() - 1),
                        getActivity()));
                cpuMidCard.addItem(mCPUGovernorMid);

                DescriptionView governorTunablesMid = new DescriptionView();
                governorTunablesMid.setTitle(getString(R.string.cpu_governor_tunables));
                governorTunablesMid.setSummary(getString(R.string.governor_tunables_summary));
                governorTunablesMid.setOnItemClickListener(item
                        -> showGovernorTunables(MidCores.get(0), MidCores.get(MidCores.size() - 1)));
                cpuMidCard.addItem(governorTunablesMid);

                items.add(cpuMidCard);
            }

            CardView cpuLITTLECard = new CardView(getActivity());
            cpuLITTLECard.setTitle(getString(R.string.dx_cluster_little_t));

            final List<Integer> LITTLECores = mCPUFreq.getLITTLECpuRange();
            mHeroLITTLE = makeHero(getString(R.string.dx_cluster_little_t), LITTLECores,
                    mCPUFreq.getLITTLECpu(), R.color.dx_mint, R.color.dx_mint_ink);
            cpuLITTLECard.setTitle(getString(R.string.dx_limits, getString(R.string.dx_cluster_little_t)));

            mCoresLITTLE.clear();
            addFreqBars(cpuLITTLECard, mCPUFreq.getLITTLECpu(), LITTLECores);

            if (mCPUFreq.hasMaxScreenOffFreq(mCPUFreq.getLITTLECpu())) {
                mCPUMaxScreenOffLITTLE = new SelectView();
                mCPUMaxScreenOffLITTLE.setTitle(getString(R.string.cpu_max_screen_off_freq));
                mCPUMaxScreenOffLITTLE.setSummary(getString(R.string.cpu_max_screen_off_freq_summary));
                mCPUMaxScreenOffLITTLE.setItems(mCPUFreq.getAdjustedFreq(mCPUFreq.getLITTLECpu(), getActivity()));
                mCPUMaxScreenOffLITTLE.setOnItemSelected((selectView, position, item) -> mCPUFreq.setMaxScreenOffFreq(mCPUFreq.getFreqs(mCPUFreq.getLITTLECpu()).get(position),
                        LITTLECores.get(0), LITTLECores.get(LITTLECores.size() - 1), getActivity()));
                cpuLITTLECard.addItem(mCPUMaxScreenOffLITTLE);
            }

            mCPUGovernorLITTLE = new DxChipsView();
            mCPUGovernorLITTLE.setTitle(getString(R.string.dx_governor));
            mCPUGovernorLITTLE.setStandalone(true);
            mCPUGovernorLITTLE.setItems(mCPUFreq.getGovernors());
        labelGovernors(mCPUGovernorLITTLE);
            mCPUGovernorLITTLE.setPrimary(DX_MAIN_GOVS, getString(R.string.dx_more));
            mCPUGovernorLITTLE.setOnItemSelected((selectView, position, item) -> mCPUFreq.setGovernor(item, LITTLECores.get(0), LITTLECores.get(LITTLECores.size() - 1),
                    getActivity()));

            DescriptionView governorTunablesLITTLE = new DescriptionView();
            governorTunablesLITTLE.setTitle(getString(R.string.cpu_governor_tunables));
            governorTunablesLITTLE.setSummary(getString(R.string.governor_tunables_summary));
            governorTunablesLITTLE.setOnItemClickListener(item -> showGovernorTunables(LITTLECores.get(0), LITTLECores.get(LITTLECores.size() - 1)));

            items.add(mHeroLITTLE);
            items.add(cpuLITTLECard);
            items.add(mCPUGovernorLITTLE);
            addEcoCard(items, mCPUFreq.getLITTLECpu(), getString(R.string.dx_cluster_little_t),
                    R.color.dx_mint, R.color.dx_mint_ink);
            addTunablesRow(items, governorTunablesLITTLE);
        }
    }

    private void showGovernorTunables(int min, int max) {
        boolean offline = mCPUFreq.isOffline(min);
        if (offline) {
            mCPUFreq.onlineCpu(min, true, false, null);
        }
        String governor = mCPUFreq.getGovernor(min, false);
        if (governor.isEmpty()) {
            mGovernorTunableErrorDialog = ViewUtils.dialogBuilder(getString(R.string.cpu_governor_tunables_read_error),
                    null, (dialog, which) -> {
                    }, dialog -> mGovernorTunableErrorDialog = null, getActivity());
            mGovernorTunableErrorDialog.show();
        } else {
            new PathReader(min, max, governor, mCPUFreq.getGovernorTunablesPath(min, governor), getString(R.string.tunables_error,
                    governor), ApplyOnBootFragment.CPU, requireActivity()).launch();
        }
        if (offline) {
            mCPUFreq.onlineCpu(min, false, false, null);
        }
    }

    private void mcPowerSavingInit(List<RecyclerViewItem> items) {
        SelectView mcPowerSaving = new SelectView();
        mcPowerSaving.setTitle(getString(R.string.mc_power_saving));
        mcPowerSaving.setSummary(getString(R.string.mc_power_saving_summary));
        mcPowerSaving.setItems(Arrays.asList(getResources().getStringArray(R.array.mc_power_saving_items)));
        mcPowerSaving.setItem(Misc.getCurMcPowerSaving());
        mcPowerSaving.setOnItemSelected((selectView, position, item) -> Misc.setMcPowerSaving(position, getActivity()));

        items.add(mcPowerSaving);
    }

    private void powerSavingWqInit(List<RecyclerViewItem> items) {
        SwitchView powerSavingWq = new SwitchView();
        powerSavingWq.setSummary(getString(R.string.power_saving_wq));
        powerSavingWq.setChecked(Misc.isPowerSavingWqEnabled());
        powerSavingWq.addOnSwitchListener((switchView, isChecked) -> {
            Misc.enablePowerSavingWq(isChecked, getActivity());
            getHandler().postDelayed(() -> powerSavingWq.setChecked(Misc.isPowerSavingWqEnabled()),
                    500);
        });

        items.add(powerSavingWq);
    }

    private void cfsSchedulerInit(List<RecyclerViewItem> items) {
        SelectView cfsScheduler = new SelectView();
        cfsScheduler.setTitle(getString(R.string.cfs_scheduler_policy));
        cfsScheduler.setSummary(getString(R.string.cfs_scheduler_policy_summary));
        cfsScheduler.setItems(Misc.getAvailableCFSSchedulers());
        cfsScheduler.setItem(Misc.getCurrentCFSScheduler());
        cfsScheduler.setOnItemSelected((selectView, position, item) -> {
            Misc.setCFSScheduler(item, getActivity());
            getHandler().postDelayed(() -> cfsScheduler.setItem(Misc.getCurrentCFSScheduler()),
                    500);
        });

        items.add(cfsScheduler);
    }

    private void cpuQuietInit(List<RecyclerViewItem> items) {
        List<RecyclerViewItem> views = new ArrayList<>();
        CardView cpuQuietCard = new CardView(getActivity());
        cpuQuietCard.setTitle(getString(R.string.cpu_quiet));

        if (Misc.hasCpuQuietEnable()) {
            SwitchView cpuQuietEnable = new SwitchView();
            cpuQuietEnable.setSummary(getString(R.string.cpu_quiet));
            cpuQuietEnable.setChecked(Misc.isCpuQuietEnabled());
            cpuQuietEnable.addOnSwitchListener((switchView, isChecked) -> {
                Misc.enableCpuQuiet(isChecked, getActivity());
                getHandler().postDelayed(() -> cpuQuietEnable.setChecked(Misc.isCpuQuietEnabled()),
                        500);
            });

            views.add(cpuQuietEnable);
        }

        if (Misc.hasCpuQuietGovernors()) {
            SelectView cpuQuietGovernors = new SelectView();
            cpuQuietGovernors.setSummary(getString(R.string.cpu_quiet_governor));
            cpuQuietGovernors.setItems(Misc.getCpuQuietAvailableGovernors());
            cpuQuietGovernors.setItem(Misc.getCpuQuietCurGovernor());
            cpuQuietGovernors.setOnItemSelected((selectView, position, item) -> Misc.setCpuQuietGovernor(item, getActivity()));

            views.add(cpuQuietGovernors);
        }

        if (Misc.hasCpuQuietNrMaxCPUs()) {
            GenericSelectView NrMaxCPUs = new GenericSelectView();
            NrMaxCPUs.setSummary(getString(R.string.nr_max_cpu));
            NrMaxCPUs.setValue(Misc.getCpuQuietNrMaxCPUs());
            NrMaxCPUs.setInputType(InputType.TYPE_CLASS_NUMBER);
            NrMaxCPUs.setOnGenericValueListener((genericSelectView, value) -> {
                Misc.setCpuQuietNrMaxCPUs(value, getActivity());
                genericSelectView.setValue(value);
                getHandler().postDelayed(() -> NrMaxCPUs.setValue(Misc.getCpuQuietNrMaxCPUs()),
                        500);
            });

            views.add(NrMaxCPUs);
        }

        if (Misc.hasCpuQuietNrMinCPUs()) {
            GenericSelectView NrMinCPUs = new GenericSelectView();
            NrMinCPUs.setSummary(getString(R.string.nr_min_cpu));
            NrMinCPUs.setValue(Misc.getCpuQuietNrMinCPUs());
            NrMinCPUs.setInputType(InputType.TYPE_CLASS_NUMBER);
            NrMinCPUs.setOnGenericValueListener((genericSelectView, value) -> {
                Misc.setCpuQuietNrMinCPUs(value, getActivity());
                genericSelectView.setValue(value);
                getHandler().postDelayed(() -> NrMinCPUs.setValue(Misc.getCpuQuietNrMinCPUs()),
                        500);
            });

            views.add(NrMinCPUs);
        }

        if (views.size() > 0) {
            DescriptionView descriptionView = new DescriptionView();
            descriptionView.setSummary(getString(R.string.cpu_quiet_summary));
            cpuQuietCard.addItem(descriptionView);

            for (RecyclerViewItem item : views) {
                cpuQuietCard.addItem(item);
            }
            items.add(cpuQuietCard);
        }
    }

    private void cpuBoostInit(List<RecyclerViewItem> items) {
        CardView cpuBoost = new CardView(getActivity());
        DescriptionView cpuBoostSettings = new DescriptionView();
        cpuBoostSettings.setTitle(getString(R.string.cpu_boost));
        cpuBoostSettings.setSummary(getString(R.string.cpu_boost_settings));
        cpuBoostSettings.setOnItemClickListener(item -> {
            Intent cpuboost = new Intent(getActivity(), CPUBoostActivity.class);
            startActivity(cpuboost);
        });
        cpuBoost.addItem(cpuBoostSettings);
        items.add(cpuBoost);
    }

    private void msmlimiterInit(List<RecyclerViewItem> items) {
        CardView msmLimiter = new CardView(getActivity());
        msmLimiter.setTitle(getString(R.string.msm_limiter));

        if (MSMLimiter.hasenable()) {
            SwitchView enable = new SwitchView();
            enable.setSummary(getString(R.string.msm_limiter_summary));
            enable.setChecked(MSMLimiter.isEnabled());
            enable.addOnSwitchListener((switchView, isChecked) -> MSMLimiter.enable(isChecked, getActivity()));

            msmLimiter.addItem(enable);
        }

        if (MSMLimiter.hasFreqControl()) {
            SwitchView freqControl = new SwitchView();
            freqControl.setTitle(getString(R.string.freq_control));
            freqControl.setSummary(getString(R.string.freq_control_summary));
            freqControl.setChecked(MSMLimiter.isFreqControlEnabled());
            freqControl.addOnSwitchListener((switchView, isChecked) -> MSMLimiter.enableFreqControl(isChecked, getActivity()));

            msmLimiter.addItem(freqControl);
        }

        if (MSMLimiter.hasDebugMask()) {
            SwitchView debugMask = new SwitchView();
            debugMask.setTitle(getString(R.string.debug_mask));
            debugMask.setSummary(getString(R.string.debug_mask_summary));
            debugMask.setChecked(MSMLimiter.isDebugMaskEnabled());
            debugMask.addOnSwitchListener((switchView, isChecked) -> MSMLimiter.enableDebugMask(isChecked, getActivity()));

            msmLimiter.addItem(debugMask);
        }

        if (msmLimiter.size() > 0) {
            items.add(msmLimiter);
        }
    }

    private void showCPUNotes() {
        if (Prefs.getBoolean("cpuNotes", true, requireActivity())) {
            new Dialog(requireActivity())
                    .setIcon(R.mipmap.ic_launcher)
                    .setTitle(getString(R.string.warning))
                    .setMessage(getString(R.string.cpu_note))
                    .setCancelable(false)
                    .setPositiveButton(R.string.got_it, (dialog, id) -> Prefs.saveBoolean("cpuNotes", false, requireActivity())).show();
        }
    }

    private float[] mCPUUsages;
    private boolean[] mCPUStates;
    private int[] mCPUFreqs;
    private int mCPUMaxFreqBig;
    private int mCPUMinFreqBig;
    private int mCPUMaxScreenOffFreqBig;
    private String mCPUGovernorStrBig;
    private int mCPUMaxFreqMid;
    private int mCPUMinFreqMid;
    private int mCPUMaxScreenOffFreqMid;
    private String mCPUGovernorStrMid;
    private int mCPUMaxFreqLITTLE;
    private int mCPUMinFreqLITTLE;
    private int mCPUMaxScreenOffFreqLITTLE;
    private String mCPUGovernorStrLITTLE;

    @Override
    protected void refreshThread() {
        super.refreshThread();

        try {
            mCPUUsages = mCPUFreq.getCpuUsage();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mCPUStates = new boolean[mCPUFreq.getCpuCount()];
        for (int i = 0; i < mCPUStates.length; i++) {
            mCPUStates[i] = !mCPUFreq.isOffline(i);
        }

        mCPUFreqs = new int[mCPUFreq.getCpuCount()];
        for (int i = 0; i < mCPUFreqs.length; i++) {
            mCPUFreqs[i] = mCPUFreq.getCurFreq(i);
        }

        if (mCPUMaxBig != null) {
            mCPUMaxFreqBig = mCPUFreq.getMaxFreq(mCPUMaxFreqBig == 0);
        }
        if (mCPUMinBig != null) {
            mCPUMinFreqBig = mCPUFreq.getMinFreq(mCPUMinFreqBig == 0);
        }
        if (mCPUMaxScreenOffBig != null) {
            mCPUMaxScreenOffFreqBig = mCPUFreq.getMaxScreenOffFreq(mCPUMaxScreenOffFreqBig == 0);
        }
        if (mCPUGovernorBig != null) {
            mCPUGovernorStrBig = mCPUFreq.getGovernor(mCPUGovernorStrBig == null);
        }
        if (mCPUMaxMid != null) {
            mCPUMaxFreqMid = mCPUFreq.getMaxFreq(mCPUFreq.getMidCpu(), mCPUMaxFreqMid == 0);
        }
        if (mCPUMinMid != null) {
            mCPUMinFreqMid = mCPUFreq.getMinFreq(mCPUFreq.getMidCpu(), mCPUMinFreqMid == 0);
        }
        if (mCPUMaxScreenOffMid != null) {
            mCPUMaxScreenOffFreqMid = mCPUFreq.getMaxScreenOffFreq(mCPUFreq.getMidCpu(),
                    mCPUMaxScreenOffFreqMid == 0);
        }
        if (mCPUGovernorMid != null) {
            mCPUGovernorStrMid = mCPUFreq.getGovernor(mCPUFreq.getMidCpu(),
                    mCPUGovernorStrMid == null);
        }
        if (mCPUMaxLITTLE != null) {
            mCPUMaxFreqLITTLE = mCPUFreq.getMaxFreq(mCPUFreq.getLITTLECpu(), mCPUMaxFreqLITTLE == 0);
        }
        if (mCPUMinLITTLE != null) {
            mCPUMinFreqLITTLE = mCPUFreq.getMinFreq(mCPUFreq.getLITTLECpu(), mCPUMinFreqLITTLE == 0);
        }
        if (mCPUMaxScreenOffLITTLE != null) {
            mCPUMaxScreenOffFreqLITTLE = mCPUFreq.getMaxScreenOffFreq(mCPUFreq.getLITTLECpu(),
                    mCPUMaxScreenOffFreqLITTLE == 0);
        }
        if (mCPUGovernorLITTLE != null) {
            mCPUGovernorStrLITTLE = mCPUFreq.getGovernor(mCPUFreq.getLITTLECpu(),
                    mCPUGovernorStrLITTLE == null);
        }
    }

    @Override
    protected void refresh() {
        super.refresh();

        if (mCPUUsages != null && mCPUStates != null) {
            refreshUsages(mCPUUsages, mCPUUsageBig, mCPUFreq.getBigCpuRange(), mCPUStates);
            if (mCPUFreq.isBigLITTLE()) {
                refreshUsages(mCPUUsages, mCPUUsageLITTLE, mCPUFreq.getLITTLECpuRange(), mCPUStates);
                if (mCPUFreq.hasMidCpu()) {
                    refreshUsages(mCPUUsages, mCPUUsageMid, mCPUFreq.getMidCpuRange(), mCPUStates);
                }
            }
        }

        if (mCPUMaxBig != null && mCPUMaxFreqBig != 0) {
            mCPUMaxBig.setItem((mCPUMaxFreqBig / 1000) + getString(R.string.mhz));
        }
        if (mCPUMinBig != null && mCPUMinFreqBig != 0) {
            mCPUMinBig.setItem((mCPUMinFreqBig / 1000) + getString(R.string.mhz));
        }
        if (mCPUMaxScreenOffBig != null && mCPUMaxScreenOffFreqBig != 0) {
            mCPUMaxScreenOffBig.setItem((mCPUMaxScreenOffFreqBig / 1000) + getString(R.string.mhz));
        }
        if (mCPUGovernorBig != null && mCPUGovernorStrBig != null && !mCPUGovernorStrBig.isEmpty()) {
            mCPUGovernorBig.setItem(mCPUGovernorStrBig);
        }
        if (mCPUMaxMid != null && mCPUMaxFreqMid != 0) {
            mCPUMaxMid.setItem((mCPUMaxFreqMid / 1000) + getString(R.string.mhz));
        }
        if (mCPUMinMid != null && mCPUMinFreqMid != 0) {
            mCPUMinMid.setItem((mCPUMinFreqMid / 1000) + getString(R.string.mhz));
        }
        if (mCPUMaxScreenOffMid != null && mCPUMaxScreenOffFreqMid != 0) {
            mCPUMaxScreenOffMid.setItem((mCPUMaxScreenOffFreqMid / 1000) + getString(R.string.mhz));
        }
        if (mCPUGovernorMid != null && mCPUGovernorStrMid != null && !mCPUGovernorStrMid.isEmpty()) {
            mCPUGovernorMid.setItem(mCPUGovernorStrMid);
        }
        if (mCPUMaxLITTLE != null && mCPUMaxFreqLITTLE != 0) {
            mCPUMaxLITTLE.setItem((mCPUMaxFreqLITTLE / 1000) + getString(R.string.mhz));
        }
        if (mCPUMinLITTLE != null && mCPUMinFreqLITTLE != 0) {
            mCPUMinLITTLE.setItem((mCPUMinFreqLITTLE / 1000) + getString(R.string.mhz));
        }
        if (mCPUMaxScreenOffLITTLE != null && mCPUMaxScreenOffFreqLITTLE != 0) {
            mCPUMaxScreenOffLITTLE.setItem((mCPUMaxScreenOffFreqLITTLE / 1000) + getString(R.string.mhz));
        }
        if (mCPUGovernorLITTLE != null && mCPUGovernorStrLITTLE != null && !mCPUGovernorStrLITTLE.isEmpty()) {
            mCPUGovernorLITTLE.setItem(mCPUGovernorStrLITTLE);
        }

        if (mCPUFreqs != null) {
            pushHero(mHeroBig, mCPUFreq.getBigCpuRange());
            if (mCPUFreq.isBigLITTLE()) pushHero(mHeroLITTLE, mCPUFreq.getLITTLECpuRange());
            refreshCores(mCoresBig, mCPUFreqs);
            if (mCPUFreq.isBigLITTLE()) {
                refreshCores(mCoresLITTLE, mCPUFreqs);
                if (mCPUFreq.hasMidCpu()) {
                    refreshCores(mCoresMid, mCPUFreqs);
                }
            }
        }
    }

    private void refreshUsages(float[] usages, XYGraphView graph, List<Integer> cores,
                               boolean[] coreStates) {
        if (graph != null) {
            float average = 0;
            int size = 0;
            for (int core : cores) {
                if (core + 1 < usages.length) {
                    if (coreStates[core]) {
                        average += usages[core + 1];
                    }
                    size++;
                }
            }
            average /= size;
            graph.setText(Math.round(average) + "%");
            graph.addPercentage(Math.round(average));
        }
    }

    private void refreshCores(SparseArray<SwitchView> array, int[] freqs) {
        try {
            for (int i = 0; i < array.size(); i++) {
                SwitchView switchView = array.valueAt(i);
                if (switchView != null) {
                    final int core = array.keyAt(i);
                    int freq = freqs[core];

                    String freqText = freq == 0 ? getString(R.string.offline) : (freq / 1000)
                            + getString(R.string.mhz);
                    switchView.clearOnSwitchListener();
                    switchView.setChecked(freq != 0);
                    switchView.setSummary(getString(R.string.core, core + 1) + " - " + freqText);
                    switchView.addOnSwitchListener((switchView1, isChecked) -> {
                        if (core == 0) {
                            Utils.snackbar(getRootView(), getString(R.string.no_offline_core));
                        } else {
                            mCPUFreq.onlineCpu(core, isChecked, true, getActivity());
                        }
                    });
                }
            }
        } catch (ConcurrentModificationException ignored) {
        }
    }


    /** Kernel Dr.xok: o "schedutil" do kernel Dr.xok é o governador econômico */
    private void labelGovernors(DxChipsView view) {
        List<String> governors = mCPUFreq.getGovernors();
        if (governors != null && governors.contains("schedutil_orig")) {
            view.setItemLabel("schedutil", getString(R.string.drxok_gov_eco));
            view.setItemLabel("schedutil_orig", getString(R.string.drxok_gov_orig));
        }
    }


    // ------------------------------------------------------------ Kernel Dr.xok

    private DxClusterHeroView makeHero(String title, List<Integer> cores, int cpu, int bgRes, int inkRes) {
        List<Integer> freqs = mCPUFreq.getFreqs(cpu);
        int min = freqs != null && !freqs.isEmpty() ? freqs.get(0) : 0;
        int max = freqs != null && !freqs.isEmpty() ? freqs.get(freqs.size() - 1) : 0;
        String badge = cores.isEmpty() ? null : (cores.get(0) + 1) + "–" + (cores.get(cores.size() - 1) + 1);
        DxClusterHeroView hero = new DxClusterHeroView();
        hero.setColors(bgRes, inkRes);
        hero.setInfo(title, badge, getString(R.string.dx_mhz_now),
                getString(R.string.dx_min_mhz, min / 1000), getString(R.string.dx_max_mhz, max / 1000), max);
        hero.setFullSpan(true);
        return hero;
    }

    private void pushHero(DxClusterHeroView hero, List<Integer> cores) {
        if (hero == null) return;
        int cur = 0;
        for (int core : cores) {
            if (core < mCPUFreqs.length) cur = Math.max(cur, mCPUFreqs[core]);
        }
        hero.push(cur);
    }

    /** Ajustes do governador eco direto na tela (só aparece com o kernel Dr.xok) */
    private void addEcoCard(List<RecyclerViewItem> items, int cpu, String group, int accentRes, int inkRes) {
        final String base = "/sys/devices/system/cpu/cpu" + cpu + "/cpufreq/schedutil/";
        final String effPath = base + "efficient_freq";
        final String delayPath = base + "up_delay_us";
        if (!Utils.existFile(effPath)) return;

        CardView eco = new CardView(getActivity());
        eco.setTitle(group == null ? getString(R.string.dx_eco_title) : getString(R.string.dx_eco_title_group, group));

        final List<Integer> freqs = mCPUFreq.getFreqs(cpu);
        if (freqs != null && !freqs.isEmpty()) {
            List<String> labels = new java.util.ArrayList<>();
            labels.add(getString(R.string.dx_off));
            for (int f : freqs) labels.add((f / 1000) + " MHz");
            int cur = Utils.strToInt(Utils.readFile(effPath));
            // o kernel arredonda para o passo seguinte da tabela (ex.: 1363 -> 1401 nos grandes)
            int pos = 0;
            if (cur > 0) {
                pos = freqs.size();
                for (int i = 0; i < freqs.size(); i++) {
                    if (freqs.get(i) >= cur) { pos = i + 1; break; }
                }
            }

            SeekBarView eff = new SeekBarView();
            eff.setTitle(getString(R.string.dx_t_efficient_freq));
            eff.setSummary(getString(R.string.dx_t_efficient_freq_d));
            eff.setItems(labels);
            eff.setProgress(pos);
            eff.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                @Override
                public void onStop(SeekBarView seekBarView, int position, String value) {
                    String v = position == 0 ? "0" : String.valueOf(freqs.get(position - 1));
                    Control.runSetting(Control.write(v, effPath), ApplyOnBootFragment.CPU, effPath, getActivity());
                }

                @Override
                public void onMove(SeekBarView seekBarView, int position, String value) {
                }
            });
            eco.addItem(eff);
        }

        if (Utils.existFile(delayPath)) {
            List<String> delays = new java.util.ArrayList<>();
            for (int ms = 0; ms <= 200; ms += 5) delays.add(ms + " ms");
            int curMs = Utils.strToInt(Utils.readFile(delayPath)) / 1000;

            SeekBarView delay = new SeekBarView();
            delay.setTitle(getString(R.string.dx_t_up_delay));
            delay.setSummary(getString(R.string.dx_t_up_delay_short));
            delay.setItems(delays);
            delay.setProgress(Math.max(0, Math.min(40, Math.round(curMs / 5f))));
            delay.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                @Override
                public void onStop(SeekBarView seekBarView, int position, String value) {
                    String v = String.valueOf(position * 5 * 1000);
                    Control.runSetting(Control.write(v, delayPath), ApplyOnBootFragment.CPU, delayPath, getActivity());
                }

                @Override
                public void onMove(SeekBarView seekBarView, int position, String value) {
                }
            });
            eco.addItem(delay);
        }

        if (eco.size() > 0) items.add(eco);
    }


    /** barras de frequência máxima e mínima de um grupo de núcleos */
    private void addFreqBars(CardView card, int cpu, List<Integer> cores) {
        final List<Integer> freqs = mCPUFreq.getFreqs(cpu);
        if (freqs == null || freqs.isEmpty() || cores.isEmpty()) return;
        List<String> labels = new java.util.ArrayList<>();
        for (int f : freqs) labels.add((f / 1000) + " MHz");
        final int first = cores.get(0), last = cores.get(cores.size() - 1);

        int curMax = nearestIndex(freqs, mCPUFreq.getMaxFreq(cpu, true));
        int curMin = nearestIndex(freqs, mCPUFreq.getMinFreq(cpu, true));

        SeekBarView max = new SeekBarView();
        SeekBarView min = new SeekBarView();

        max.setTitle(getString(R.string.dx_max));
        max.setItems(labels);
        max.setProgress(curMax < 0 ? freqs.size() - 1 : curMax);
        max.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
            @Override
            public void onStop(SeekBarView seekBarView, int position, String value) {
                mCPUFreq.setMaxFreq(freqs.get(position), first, last, getActivity());
                // o mínimo nunca fica acima do máximo
                if (min.getProgress() > position) {
                    min.setProgress(position);
                    mCPUFreq.setMinFreq(freqs.get(position), first, last, getActivity());
                }
            }

            @Override
            public void onMove(SeekBarView seekBarView, int position, String value) {
            }
        });
        card.addItem(max);

        min.setTitle(getString(R.string.dx_min));
        min.setItems(labels);
        min.setProgress(curMin < 0 ? 0 : curMin);
        min.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
            @Override
            public void onStop(SeekBarView seekBarView, int position, String value) {
                mCPUFreq.setMinFreq(freqs.get(position), first, last, getActivity());
                if (max.getProgress() < position) {
                    max.setProgress(position);
                    mCPUFreq.setMaxFreq(freqs.get(position), first, last, getActivity());
                }
            }

            @Override
            public void onMove(SeekBarView seekBarView, int position, String value) {
            }
        });
        card.addItem(min);
    }


    private static final List<String> DX_MAIN_GOVS = java.util.Arrays.asList(
            "schedutil", "schedutil_orig", "performance", "powersave", "interactive");

    private static int nearestIndex(List<Integer> freqs, int value) {
        if (value <= 0) return -1;
        for (int i = 0; i < freqs.size(); i++) if (freqs.get(i) >= value) return i;
        return freqs.size() - 1;
    }

    /** linha discreta para todos os ajustes do governador */
    private void addTunablesRow(List<RecyclerViewItem> items, DescriptionView row) {
        row.setTitle(getString(R.string.dx_all_tunables));
        row.setSummary(getString(R.string.dx_all_tunables_sub));
        CardView card = new CardView(getActivity());
        card.addItem(row);
        items.add(card);
    }

}
