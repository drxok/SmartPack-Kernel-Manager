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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.text.InputType;

import com.smartpack.kernelmanager.R;
import com.smartpack.kernelmanager.fragments.ApplyOnBootFragment;
import com.smartpack.kernelmanager.fragments.RecyclerViewFragment;
import com.smartpack.kernelmanager.utils.Utils;
import com.smartpack.kernelmanager.utils.kernel.battery.Battery;
import com.smartpack.kernelmanager.views.recyclerview.CardView;
import com.smartpack.kernelmanager.views.recyclerview.DescriptionView;
import com.smartpack.kernelmanager.views.recyclerview.DxBatteryHeroView;
import com.smartpack.kernelmanager.views.recyclerview.DxChipsView;
import com.smartpack.kernelmanager.views.recyclerview.GenericSelectView;
import com.smartpack.kernelmanager.views.recyclerview.RecyclerViewItem;
import com.smartpack.kernelmanager.views.recyclerview.SeekBarView;
import com.smartpack.kernelmanager.views.recyclerview.SelectView;
import com.smartpack.kernelmanager.views.recyclerview.SwitchView;
import com.smartpack.kernelmanager.views.recyclerview.TitleView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by willi on 26.06.16.
 */
public class BatteryFragment extends RecyclerViewFragment {

    private Battery mBattery;

    @Override
    protected void init() {
        super.init();

        mBattery = Battery.getInstance(requireActivity());
    }

    @Override
    protected void addItems(List<RecyclerViewItem> items) {
        drxokInit(items);
        if (mBattery.hasbatterychargelimit() || mBattery.hasChargingEnable() || Battery.hasFastCharge()
                || Battery.haschargeLevel() || mBattery.hasBlx() || Battery.hasOPOTGSwitch()
                || Battery.hasThunderCharge()) {
            acciInit(items);
        }
    }

    @Override
    protected void postInit() {
        super.postInit();

        addViewPagerFragment(ApplyOnBootFragment.newInstance(this));
    }

    /*
     * Kernel Dr.xok: saúde da bateria (cartão verde) e limite de carga do kernel
     */
    private void drxokInit(List<RecyclerViewItem> items) {
        int full = Battery.getChargeFullMah();
        int design = mBattery.getDesignMah();
        int cycles = Battery.getCycleCount();
        if (full > 0) {
            int pct = design > 0 ? Math.round(full * 100f / design) : 100;
            StringBuilder details = new StringBuilder();
            details.append(design > 0 ? getString(R.string.drxok_health_of, full, design)
                    : getString(R.string.drxok_health_capacity_only, full));
            if (cycles >= 0) {
                details.append("\n").append(getString(R.string.drxok_health_cycles, cycles));
            }
            String badge = getString(pct >= 80 ? R.string.drxok_health_good
                    : pct >= 60 ? R.string.drxok_health_fair : R.string.drxok_health_worn);

            DxBatteryHeroView hero = new DxBatteryHeroView();
            hero.setData(pct, getString(R.string.drxok_health_short), getString(R.string.drxok_health),
                    details.toString(), badge);
            hero.setFullSpan(true);
            items.add(hero);
        }

        if (!Battery.hasDrxokChargeLimit()) return;

        CardView card = new CardView(getActivity());
        card.setTitle(getString(R.string.drxok_charge_limit));

        // 50% ... 99% e "sem limite" (100)
        List<String> limits = new ArrayList<>();
        for (int i = 50; i <= 99; i++) {
            limits.add(i + "%");
        }
        limits.add(getString(R.string.drxok_no_limit));

        int cur = Battery.getDrxokChargeLimit();
        if (cur < 50 || cur > 100) cur = 100;

        SeekBarView limit = new SeekBarView();
        DxChipsView presets = new DxChipsView();

        limit.setSummary(getString(R.string.drxok_charge_limit_summary));
        limit.setItems(limits);
        limit.setProgress(cur - 50);
        limit.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
            @Override
            public void onStop(SeekBarView seekBarView, int position, String value) {
                mBattery.setDrxokChargeLimit(position + 50, getActivity());
                presets.setItem(String.valueOf(position + 50));
            }

            @Override
            public void onMove(SeekBarView seekBarView, int position, String value) {
            }
        });
        card.addItem(limit);

        List<String> presetValues = new ArrayList<>();
        presetValues.add("70");
        presetValues.add("80");
        presetValues.add("90");
        presetValues.add("100");
        presets.setItems(presetValues);
        presets.setItemLabel("70", "70%");
        presets.setItemLabel("80", "80%");
        presets.setItemLabel("90", "90%");
        presets.setItemLabel("100", getString(R.string.drxok_no_limit));
        presets.setAccent(R.color.dx_butter, R.color.dx_butter_ink);
        presets.setItem(String.valueOf(cur));
        presets.setOnItemSelected((view, position, item) -> {
            int v = Integer.parseInt(item);
            mBattery.setDrxokChargeLimit(v, getActivity());
            limit.setProgress(v - 50);
        });
        card.addItem(presets);

        if (Battery.hasDrxokChargeHyst()) {
            int h = Battery.getDrxokChargeHyst();
            if (h < 1 || h > 30) h = 5;

            SeekBarView hyst = new SeekBarView();
            hyst.setTitle(getString(R.string.drxok_charge_hyst));
            hyst.setSummary(getString(R.string.drxok_charge_hyst_summary));
            hyst.setUnit("%");
            hyst.setMin(1);
            hyst.setMax(30);
            hyst.setProgress(h - 1);
            hyst.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                @Override
                public void onStop(SeekBarView seekBarView, int position, String value) {
                    mBattery.setDrxokChargeHyst(position + 1, getActivity());
                }

                @Override
                public void onMove(SeekBarView seekBarView, int position, String value) {
                }
            });
            card.addItem(hyst);
        }

        items.add(card);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void acciInit(List<RecyclerViewItem> items) {
        CardView acci = new CardView(getActivity());
        acci.setTitle(getString(R.string.acci));

        if (mBattery.hasbatterychargelimit()) {
            SwitchView enablecharging = new SwitchView();
            enablecharging.setTitle(getString(R.string.charging_enable));
            enablecharging.setSummary(getString(R.string.charging_enable_summary));
            enablecharging.setChecked(mBattery.batterychargelimitenabled());
            enablecharging.addOnSwitchListener((switchView, isChecked) -> mBattery.enablebatterychargelimit(isChecked, getActivity()));

            acci.addItem(enablecharging);
        }

        if (mBattery.hasChargingEnable()) {
            SwitchView enablecharging = new SwitchView();
            enablecharging.setTitle(getString(R.string.charging_enable));
            enablecharging.setSummary(getString(R.string.charging_enable_summary));
            enablecharging.setChecked(mBattery.ChargingEnabled());
            enablecharging.addOnSwitchListener((switchView, isChecked) -> mBattery.enableCharging(isChecked, getActivity()));

            acci.addItem(enablecharging);
        }

        if ((Battery.hasForceFastCharge()) && (Battery.hasFastChargeControlAC()) && (Battery.hasFastChargeControlUSB())) {
            // Initialize Advanced Charge Control Interface
            SelectView forceFastCharge = new SelectView();
            forceFastCharge.setTitle(getString(R.string.fast_charge));
            forceFastCharge.setSummary(getString(R.string.fast_charge_summary));
            forceFastCharge.setItems(Battery.enableForceFastCharge(requireActivity()));
            forceFastCharge.setItem(Battery.getForceFastCharge());
            forceFastCharge.setOnItemSelected((selectView, position, item) -> mBattery.setForceFastCharge(position, getActivity()));
            acci.addItem(forceFastCharge);

        } else {
            // Initialize (only) USB Fast Charge
            if (Battery.hasForceFastCharge() || Battery.hasUSBFastCharge()) {
                SwitchView forceFastCharge = new SwitchView();
                forceFastCharge.setTitle(getString(R.string.fast_charge));
                forceFastCharge.setSummary(getString(R.string.usb_fast_charge_summary));
                if (Battery.hasForceFastCharge()) {
                    forceFastCharge.setChecked(mBattery.isForceFastChargeEnabled());
                } else {
                    forceFastCharge.setChecked(mBattery.isUSBFastChargeEnabled());
                }
                forceFastCharge.addOnSwitchListener((switchView, isChecked) -> {
                    if (Battery.hasForceFastCharge()) {
                        mBattery.ForceFastChargeenable(isChecked, getActivity());
                    } else {
                        mBattery.USBFastChargeenable(isChecked, getActivity());
                    }
                });
                acci.addItem(forceFastCharge);
            }
        }

        if (Battery.hasFastChargeControlAC()) {
            SelectView ACLevelCard = new SelectView();
            ACLevelCard.setTitle(getString(R.string.charge_level_ac));
            ACLevelCard.setSummary(getString(R.string.charge_level_summary));
            ACLevelCard.setItems(Battery.getFastChargeControlAC());
            ACLevelCard.setItem(Battery.getFastChargeCustomAC());
            ACLevelCard.setOnItemSelected((selectView, position, item) -> {
                mBattery.setFastChargeControlAC(item, getActivity());
                getHandler().postDelayed(() -> ACLevelCard.setItem(Battery.getFastChargeCustomAC()),
                        500);
            });
            acci.addItem(ACLevelCard);

        }

        if (Battery.hasFastChargeControlUSB()) {
            SelectView USBLevelCard = new SelectView();
            USBLevelCard.setTitle(getString(R.string.charge_level_usb));
            USBLevelCard.setSummary(getString(R.string.charge_level_summary));
            USBLevelCard.setItems(Battery.getFastChargeControlUSB());
            USBLevelCard.setItem(Battery.getFastChargeCustomUSB());
            USBLevelCard.setOnItemSelected((selectView, position, item) -> {
                mBattery.setFastChargeControlUSB(item, getActivity());
                getHandler().postDelayed(() -> USBLevelCard.setItem(Battery.getFastChargeCustomUSB()),
                        500);
            });
            acci.addItem(USBLevelCard);
        }
        if (mBattery.hasFastChargeControlWIRELESS()) {
            SelectView WirelessLevelCard = new SelectView();
            WirelessLevelCard.setTitle(getString(R.string.charge_level_wireless));
            WirelessLevelCard.setSummary(getString(R.string.charge_level_summary));
            WirelessLevelCard.setItems(Battery.getFastChargeControlWIRELESS());
            WirelessLevelCard.setItem(Battery.getFastChargeCustomWIRELESS());
            WirelessLevelCard.setOnItemSelected((selectView, position, item) -> {
                mBattery.setFastChargeControlWIRELESS(item, getActivity());
                getHandler().postDelayed(() -> WirelessLevelCard.setItem(Battery.getFastChargeCustomWIRELESS()),
                        500);
            });
            acci.addItem(WirelessLevelCard);
        }

        if (mBattery.hasMtpForceFastCharge()) {
            SwitchView MtpFastCharge = new SwitchView();
            MtpFastCharge.setTitle(getString(R.string.mtp_fast_charge));
            MtpFastCharge.setSummary(getString(R.string.mtp_fast_charge_summary));
            MtpFastCharge.setChecked(mBattery.isMtpForceFastChargeEnabled());
            MtpFastCharge.addOnSwitchListener((switchView, isChecked) -> mBattery.enableMtpForceFastCharge(isChecked, getActivity()));
            acci.addItem(MtpFastCharge);
        }

        if (mBattery.hasScreenCurrentLimit()) {
            SwitchView ScreenLimit = new SwitchView();
            ScreenLimit.setTitle(getString(R.string.screen_limit));
            ScreenLimit.setSummary(getString(R.string.screen_limit_summary));
            ScreenLimit.setChecked(mBattery.isScreenCurrentLimit());
            ScreenLimit.addOnSwitchListener((switchView, isChecked) -> mBattery.enableScreenCurrentLimit(isChecked, getActivity()));
            acci.addItem(ScreenLimit);
        }

        if (Battery.haschargeLevelAC() || Battery.haschargeLevelUSB() || Battery.haschargeLevelWL()) {
            DescriptionView stockchargelogic = new DescriptionView();
            stockchargelogic.setTitle(("(") + getString(R.string.stockchargelogic) + (")"));
            acci.addItem(stockchargelogic);
        }

        if (Battery.haschargeLevelAC()) {
            SeekBarView chargeLevelAC = new SeekBarView();
            chargeLevelAC.setTitle(getString(R.string.charge_level_ac));
            chargeLevelAC.setUnit(getString(R.string.ma));
            chargeLevelAC.setMax(2000);
            chargeLevelAC.setOffset(25);
            chargeLevelAC.setProgress(Battery.getchargeLevelAC() / 25 );
            chargeLevelAC.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                @Override
                public void onStop(SeekBarView seekBarView, int position, String value) {
                    mBattery.setchargeLevelAC((position * 25), getActivity());
                    getHandler().postDelayed(() -> chargeLevelAC.setProgress(Battery.getchargeLevelAC() / 25 ),
                            500);
                }

                @Override
                public void onMove(SeekBarView seekBarView, int position, String value) {
                }
            });

            acci.addItem(chargeLevelAC);
        }

        if (Battery.haschargeLevelUSB()) {
            SeekBarView chargeLevelUSB = new SeekBarView();
            chargeLevelUSB.setTitle(getString(R.string.charge_level_usb));
            chargeLevelUSB.setUnit(getString(R.string.ma));
            chargeLevelUSB.setMax(1600);
            chargeLevelUSB.setOffset(25);
            chargeLevelUSB.setProgress(Battery.getchargeLevelUSB() / 25 );
            chargeLevelUSB.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                @Override
                public void onStop(SeekBarView seekBarView, int position, String value) {
                    mBattery.setchargeLevelUSB((position * 25), getActivity());
                    getHandler().postDelayed(() -> chargeLevelUSB.setProgress(Battery.getchargeLevelUSB() / 25 ),
                            500);
                }

                @Override
                public void onMove(SeekBarView seekBarView, int position, String value) {
                }
            });

            acci.addItem(chargeLevelUSB);
        }

        if (Battery.haschargeLevelWL()) {
            SeekBarView chargeLevelWL = new SeekBarView();
            chargeLevelWL.setTitle(getString(R.string.charge_level_wireless));
            chargeLevelWL.setUnit(getString(R.string.ma));
            chargeLevelWL.setMax(1600);
            chargeLevelWL.setOffset(25);
            chargeLevelWL.setProgress(Battery.getchargeLevelWL() / 25 );
            chargeLevelWL.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                @Override
                public void onStop(SeekBarView seekBarView, int position, String value) {
                    mBattery.setchargeLevelWL((position * 25), getActivity());
                    getHandler().postDelayed(() -> chargeLevelWL.setProgress(Battery.getchargeLevelWL() / 25 ),
                            500);
                }

                @Override
                public void onMove(SeekBarView seekBarView, int position, String value) {
                }
            });

            acci.addItem(chargeLevelWL);
        }

        if (mBattery.hasBlx()) {
            List<String> list = new ArrayList<>();
            list.add(getString(R.string.disabled));
            for (int i = 0; i <= 100; i++) {
                list.add(String.valueOf(i));
            }

            SeekBarView blx = new SeekBarView();
            blx.setTitle(getString(R.string.blx));
            blx.setSummary(getString(R.string.blx_summary));
            blx.setItems(list);
            blx.setProgress(mBattery.getBlx());
            blx.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
                @Override
                public void onStop(SeekBarView seekBarView, int position, String value) {
                    mBattery.setBlx(position, getActivity());
                }

                @Override
                public void onMove(SeekBarView seekBarView, int position, String value) {
                }
            });

            acci.addItem(blx);
        }

        if (Battery.hasOPOTGSwitch()) {
            SwitchView OnePlusOTG = new SwitchView();
            OnePlusOTG.setTitle(getString(R.string.otg_enable));
            OnePlusOTG.setSummary(getString(R.string.otg_enable_summary));
            OnePlusOTG.setChecked(mBattery.isOPOTGEnabled());
            OnePlusOTG.addOnSwitchListener((switchView, isChecked) -> mBattery.OPOTGenable(isChecked, getActivity()));
            acci.addItem(OnePlusOTG);
        }

        if (Battery.hasThunderChargeEnable()) {
            SwitchView enable = new SwitchView();
            enable.setTitle(getString(R.string.thunder_charge));
            enable.setSummary(getString(R.string.thunder_charge_summary));
            enable.setChecked(mBattery.isThunderChargeEnabled());
            enable.addOnSwitchListener((switchView, isChecked) -> mBattery.enableThunderCharge(isChecked, getActivity()));

            acci.addItem(enable);
        }

        if (Battery.hasThunderChargeAC()) {
            GenericSelectView acharge = new GenericSelectView();
            acharge.setTitle(getString(R.string.charge_level_ac));
            acharge.setSummary(getString(R.string.charge_level_summary));
            acharge.setValue(Battery.getThunderChargeAC());
            acharge.setInputType(InputType.TYPE_CLASS_NUMBER);
            acharge.setOnGenericValueListener((genericSelectView, value) -> {
                mBattery.setThunderChargeAC(value, getActivity());
                genericSelectView.setValue(value);
                getHandler().postDelayed(() -> acharge.setValue(Battery.getThunderChargeAC()),
                        500);
            });

            acci.addItem(acharge);
        }

        if (Battery.hasThunderChargeUSB()) {
            GenericSelectView usbcharge = new GenericSelectView();
            usbcharge.setTitle(getString(R.string.charge_level_usb));
            usbcharge.setSummary(getString(R.string.charge_level_summary));
            usbcharge.setValue(Battery.getThunderChargeUSB());
            usbcharge.setInputType(InputType.TYPE_CLASS_NUMBER);
            usbcharge.setOnGenericValueListener((genericSelectView, value) -> {
                mBattery.setThunderChargeUSB(value, getActivity());
                genericSelectView.setValue(value);
                getHandler().postDelayed(() -> usbcharge.setValue(Battery.getThunderChargeUSB()),
                        500);
            });

            acci.addItem(usbcharge);
        }

        if (acci.size() > 0) {
            items.add(acci);
        }

        TitleView bg = new TitleView();
        bg.setText(getString(R.string.battery_guru));

        items.add(bg);

        DescriptionView battery_guru = new DescriptionView();
        battery_guru.setDrawable(getResources().getDrawable(R.drawable.ic_batteryguru));
        battery_guru.setSummary(getString(R.string.battery_guru_summary));
        battery_guru.setFullSpan(true);
        battery_guru.setOnItemClickListener(item -> {
            if (Utils.isPackageInstalled("com.paget96.batteryguru")) {
                Intent launchIntent = requireActivity().getPackageManager().getLaunchIntentForPackage("com.paget96.batteryguru");
                if (launchIntent != null) {
                    startActivity(launchIntent);
                    requireActivity().finish();
                }
            } else {
                Utils.launchUrl("https://play.google.com/store/apps/details?id=com.paget96.batteryguru", getActivity());
            }
        });
        items.add(battery_guru);
    }

}