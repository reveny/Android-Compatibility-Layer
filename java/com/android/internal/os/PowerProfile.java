/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.os;


import android.annotation.LongDef;
import android.annotation.Nullable;
import android.annotation.StringDef;
import android.annotation.XmlRes;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.SparseArray;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Reports power consumption values for various device activities. Reads values from an XML file.
 * Customize the XML file for different devices.
 * [hidden]
 */
@android.ravenwood.annotation.RavenwoodKeepWholeClass
public class PowerProfile {

    public static final String TAG = "PowerProfile";

    /*
     * POWER_CPU_SUSPEND: Power consumption when CPU is in power collapse mode.
     * POWER_CPU_IDLE: Power consumption when CPU is awake (when a wake lock is held). This should
     *                 be zero on devices that can go into full CPU power collapse even when a wake
     *                 lock is held. Otherwise, this is the power consumption in addition to
     * POWER_CPU_SUSPEND due to a wake lock being held but with no CPU activity.
     * POWER_CPU_ACTIVE: Power consumption when CPU is running, excluding power consumed by clusters
     *                   and cores.
     *
     * CPU Power Equation (assume two clusters):
     * Total power = POWER_CPU_SUSPEND  (always added)
     *               + POWER_CPU_IDLE   (skip this and below if in power collapse mode)
     *               + POWER_CPU_ACTIVE (skip this and below if CPU is not running, but a wakelock
     *                                   is held)
     *               + cluster_power.cluster0 + cluster_power.cluster1 (skip cluster not running)
     *               + core_power.cluster0 * num running cores in cluster 0
     *               + core_power.cluster1 * num running cores in cluster 1
     */
    public static final String POWER_CPU_SUSPEND = "cpu.suspend";
    @UnsupportedAppUsage
    public static final String POWER_CPU_IDLE = "cpu.idle";
    @UnsupportedAppUsage
    public static final String POWER_CPU_ACTIVE = "cpu.active";

    /**
     * Power consumption when WiFi driver is scanning for networks.
     */
    @UnsupportedAppUsage
    public static final String POWER_WIFI_SCAN = "wifi.scan";

    /**
     * Power consumption when WiFi driver is on.
     */
    @UnsupportedAppUsage
    public static final String POWER_WIFI_ON = "wifi.on";

    /**
     * Power consumption when WiFi driver is transmitting/receiving.
     */
    @UnsupportedAppUsage
    public static final String POWER_WIFI_ACTIVE = "wifi.active";

    //
    // Updated power constants. These are not estimated, they are real world
    // currents and voltages for the underlying bluetooth and wifi controllers.
    //
    public static final String POWER_WIFI_CONTROLLER_IDLE = "wifi.controller.idle";
    public static final String POWER_WIFI_CONTROLLER_RX = "wifi.controller.rx";
    public static final String POWER_WIFI_CONTROLLER_TX = "wifi.controller.tx";
    public static final String POWER_WIFI_CONTROLLER_TX_LEVELS = "wifi.controller.tx_levels";
    public static final String POWER_WIFI_CONTROLLER_OPERATING_VOLTAGE = "wifi.controller.voltage";

    public static final String POWER_BLUETOOTH_CONTROLLER_IDLE = "bluetooth.controller.idle";
    public static final String POWER_BLUETOOTH_CONTROLLER_RX = "bluetooth.controller.rx";
    public static final String POWER_BLUETOOTH_CONTROLLER_TX = "bluetooth.controller.tx";
    public static final String POWER_BLUETOOTH_CONTROLLER_OPERATING_VOLTAGE =
            "bluetooth.controller.voltage";

    public static final String POWER_MODEM_CONTROLLER_SLEEP = "modem.controller.sleep";
    public static final String POWER_MODEM_CONTROLLER_IDLE = "modem.controller.idle";
    public static final String POWER_MODEM_CONTROLLER_RX = "modem.controller.rx";
    public static final String POWER_MODEM_CONTROLLER_TX = "modem.controller.tx";
    public static final String POWER_MODEM_CONTROLLER_OPERATING_VOLTAGE =
            "modem.controller.voltage";

    /**
     * Power consumption when GPS is on.
     */
    @UnsupportedAppUsage
    public static final String POWER_GPS_ON = "gps.on";

    /**
     * GPS power parameters based on signal quality
     */
    public static final String POWER_GPS_SIGNAL_QUALITY_BASED = "gps.signalqualitybased";
    public static final String POWER_GPS_OPERATING_VOLTAGE = "gps.voltage";

    /**
     * Power consumption when Bluetooth driver is on.
     *
     * @deprecated
     */
    @Deprecated
    @UnsupportedAppUsage
    public static final String POWER_BLUETOOTH_ON = "bluetooth.on";

    /**
     * Power consumption when Bluetooth driver is transmitting/receiving.
     *
     * @deprecated
     */
    @Deprecated
    public static final String POWER_BLUETOOTH_ACTIVE = "bluetooth.active";

    /**
     * Power consumption when Bluetooth driver gets an AT command.
     *
     * @deprecated
     */
    @Deprecated
    @UnsupportedAppUsage
    public static final String POWER_BLUETOOTH_AT_CMD = "bluetooth.at";

    /**
     * Power consumption when screen is in doze/ambient/always-on mode, including backlight power.
     *
     * @deprecated Use {@link #POWER_GROUP_DISPLAY_AMBIENT} instead.
     */
    @Deprecated
    public static final String POWER_AMBIENT_DISPLAY = "ambient.on";

    /**
     * Power consumption when screen is on, not including the backlight power.
     *
     * @deprecated Use {@link #POWER_GROUP_DISPLAY_SCREEN_ON} instead.
     */
    @Deprecated
    @UnsupportedAppUsage
    public static final String POWER_SCREEN_ON = "screen.on";

    /**
     * Power consumption when cell radio is on but not on a call.
     */
    @UnsupportedAppUsage
    public static final String POWER_RADIO_ON = "radio.on";

    /**
     * Power consumption when cell radio is hunting for a signal.
     */
    @UnsupportedAppUsage
    public static final String POWER_RADIO_SCANNING = "radio.scanning";

    /**
     * Power consumption when talking on the phone.
     */
    @UnsupportedAppUsage
    public static final String POWER_RADIO_ACTIVE = "radio.active";

    /**
     * Power consumption at full backlight brightness. If the backlight is at
     * 50% brightness, then this should be multiplied by 0.5
     *
     * @deprecated Use {@link #POWER_GROUP_DISPLAY_SCREEN_FULL} instead.
     */
    @Deprecated
    @UnsupportedAppUsage
    public static final String POWER_SCREEN_FULL = "screen.full";

    /**
     * Power consumed by the audio hardware when playing back audio content. This is in addition
     * to the CPU power, probably due to a DSP and / or amplifier.
     */
    public static final String POWER_AUDIO = "audio";

    /**
     * Power consumed by any media hardware when playing back video content. This is in addition
     * to the CPU power, probably due to a DSP.
     */
    public static final String POWER_VIDEO = "video";

    /**
     * Average power consumption when camera flashlight is on.
     */
    public static final String POWER_FLASHLIGHT = "camera.flashlight";

    /**
     * Power consumption when DDR is being used.
     */
    public static final String POWER_MEMORY = "memory.bandwidths";

    /**
     * Average power consumption when the camera is on over all standard use cases.
     *
     * TODO: Add more fine-grained camera power metrics.
     */
    public static final String POWER_CAMERA = "camera.avg";

    /**
     * Power consumed by wif batched scaning.  Broken down into bins by
     * Channels Scanned per Hour.  May do 1-720 scans per hour of 1-100 channels
     * for a range of 1-72,000.  Going logrithmic (1-8, 9-64, 65-512, 513-4096, 4097-)!
     */
    public static final String POWER_WIFI_BATCHED_SCAN = "wifi.batchedscan";

    /**
     * Battery capacity in milliAmpHour (mAh).
     */
    public static final String POWER_BATTERY_CAPACITY = "battery.capacity";

    /**
     * Power consumption when a screen is in doze/ambient/always-on mode, including backlight power.
     */
    public static final String POWER_GROUP_DISPLAY_AMBIENT = "ambient.on.display";

    /**
     * Power consumption when a screen is on, not including the backlight power.
     */
    public static final String POWER_GROUP_DISPLAY_SCREEN_ON = "screen.on.display";

    /**
     * Power consumption of a screen at full backlight brightness.
     */
    public static final String POWER_GROUP_DISPLAY_SCREEN_FULL = "screen.full.display";

    @StringDef(prefix = { "POWER_GROUP_" }, value = {
            POWER_GROUP_DISPLAY_AMBIENT,
            POWER_GROUP_DISPLAY_SCREEN_ON,
            POWER_GROUP_DISPLAY_SCREEN_FULL,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface PowerGroup {}

    /**
     * Constants for generating a 64bit power constant key.
     *
     * The bitfields of a key describes what its corresponding power constant represents:
     * [63:40] - RESERVED
     * [39:32] - {@link Subsystem} (max count = 16).
     * [31:0] - per Subsystem fields, see {@link ModemPowerProfile}.
     *
     */
    private static final long SUBSYSTEM_MASK = 0xF_0000_0000L;
    /**
     * Power constant not associated with a subsystem.
     */
    public static final long SUBSYSTEM_NONE = 0x0_0000_0000L;
    /**
     * Modem power constant.
     */
    public static final long SUBSYSTEM_MODEM = 0x1_0000_0000L;

    @LongDef(prefix = { "SUBSYSTEM_" }, value = {
            SUBSYSTEM_NONE,
            SUBSYSTEM_MODEM,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Subsystem {}

    private static final long SUBSYSTEM_FIELDS_MASK = 0xFFFF_FFFF;

    public static final int POWER_BRACKETS_UNSPECIFIED = -1;

    /**
     * A map from Power Use Item to its power consumption.
     */
    static final HashMap<String, Double> sPowerItemMap = new HashMap<>();
    /**
     * A map from Power Use Item to an array of its power consumption
     * (for items with variable power e.g. CPU).
     */
    static final HashMap<String, Double[]> sPowerArrayMap = new HashMap<>();


    private static final String TAG_DEVICE = "device";
    private static final String TAG_ITEM = "item";
    private static final String TAG_ARRAY = "array";
    private static final String TAG_ARRAYITEM = "value";
    private static final String ATTR_NAME = "name";

    private static final String TAG_MODEM = "modem";

    private static final Object sLock = new Object();

    private int mCpuPowerBracketCount;

    @VisibleForTesting
    public PowerProfile() {
        synchronized (sLock) {
            initLocked();
        }
    }





    /**
     * Reinitialize the PowerProfile with the provided XML, using optional Resources for fallback
     * configuration settings.
     * WARNING: use only for testing!
     */


    @GuardedBy("sLock")
    private void initLocked(Context context, @XmlRes int xmlId) {
        initLocked();
    }

    private void initLocked() {
        initCpuClusters();
        initCpuScalingPolicies();
        initCpuPowerBrackets();
        initDisplays();
    }





    private CpuClusterKey[] mCpuClusters;

    private static final String CPU_PER_CLUSTER_CORE_COUNT = "cpu.clusters.cores";
    private static final String CPU_CLUSTER_POWER_COUNT = "cpu.cluster_power.cluster";
    private static final String CPU_CORE_SPEED_PREFIX = "cpu.core_speeds.cluster";
    private static final String CPU_CORE_POWER_PREFIX = "cpu.core_power.cluster";
    private static final String CPU_POWER_BRACKETS_PREFIX = "cpu.power_brackets.policy";

    private void initCpuClusters() {
        if (sPowerArrayMap.containsKey(CPU_PER_CLUSTER_CORE_COUNT)) {
            final Double[] data = sPowerArrayMap.get(CPU_PER_CLUSTER_CORE_COUNT);
            mCpuClusters = new CpuClusterKey[data.length];
            for (int cluster = 0; cluster < data.length; cluster++) {
                int numCpusInCluster = (int) Math.round(data[cluster]);
                mCpuClusters[cluster] = new CpuClusterKey(
                        CPU_CORE_SPEED_PREFIX + cluster, CPU_CLUSTER_POWER_COUNT + cluster,
                        CPU_CORE_POWER_PREFIX + cluster, numCpusInCluster);
            }
        } else {
            // Default to single.
            mCpuClusters = new CpuClusterKey[1];
            int numCpus = 1;
            if (sPowerItemMap.containsKey(CPU_PER_CLUSTER_CORE_COUNT)) {
                numCpus = (int) Math.round(sPowerItemMap.get(CPU_PER_CLUSTER_CORE_COUNT));
            }
            mCpuClusters[0] = new CpuClusterKey(CPU_CORE_SPEED_PREFIX + 0,
                    CPU_CLUSTER_POWER_COUNT + 0, CPU_CORE_POWER_PREFIX + 0, numCpus);
        }
    }

    private SparseArray<CpuScalingPolicyPower> mCpuScalingPolicies;
    private static final String CPU_SCALING_POLICY_POWER_POLICY = "cpu.scaling_policy_power.policy";
    private static final String CPU_SCALING_STEP_POWER_POLICY = "cpu.scaling_step_power.policy";

    private void initCpuScalingPolicies() {
        int policyCount = 0;
        for (String key : sPowerItemMap.keySet()) {
            if (key.startsWith(CPU_SCALING_POLICY_POWER_POLICY)) {
                int policy =
                        Integer.parseInt(key.substring(CPU_SCALING_POLICY_POWER_POLICY.length()));
                policyCount = Math.max(policyCount, policy + 1);
            }
        }
        for (String key : sPowerArrayMap.keySet()) {
            if (key.startsWith(CPU_SCALING_STEP_POWER_POLICY)) {
                int policy =
                        Integer.parseInt(key.substring(CPU_SCALING_STEP_POWER_POLICY.length()));
                policyCount = Math.max(policyCount, policy + 1);
            }
        }

        if (policyCount > 0) {
            mCpuScalingPolicies = new SparseArray<>(policyCount);
            for (int policy = 0; policy < policyCount; policy++) {
                Double policyPower = sPowerItemMap.get(CPU_SCALING_POLICY_POWER_POLICY + policy);
                Double[] stepPower = sPowerArrayMap.get(CPU_SCALING_STEP_POWER_POLICY + policy);
                if (policyPower != null || stepPower != null) {
                    double[] primitiveStepPower;
                    if (stepPower != null) {
                        primitiveStepPower = new double[stepPower.length];
                        for (int i = 0; i < stepPower.length; i++) {
                            primitiveStepPower[i] = stepPower[i];
                        }
                    } else {
                        primitiveStepPower = new double[0];
                    }
                    mCpuScalingPolicies.put(policy, new CpuScalingPolicyPower(
                            policyPower != null ? policyPower : 0, primitiveStepPower));
                }
            }
        } else {
            // Legacy power_profile.xml
            int cpuId = 0;
            for (CpuClusterKey cpuCluster : mCpuClusters) {
                policyCount = cpuId + 1;
                cpuId += cpuCluster.numCpus;
            }

            if (policyCount > 0) {
                mCpuScalingPolicies = new SparseArray<>(policyCount);
                cpuId = 0;
                for (CpuClusterKey cpuCluster : mCpuClusters) {
                    double clusterPower = getAveragePower(cpuCluster.clusterPowerKey);
                    double[] stepPower;
                    int numSteps = getNumElements(cpuCluster.corePowerKey);
                    if (numSteps != 0) {
                        stepPower = new double[numSteps];
                        for (int step = 0; step < numSteps; step++) {
                            stepPower[step] = getAveragePower(cpuCluster.corePowerKey, step);
                        }
                    } else {
                        stepPower = new double[1];
                    }
                    mCpuScalingPolicies.put(cpuId,
                            new CpuScalingPolicyPower(clusterPower, stepPower));
                    cpuId += cpuCluster.numCpus;
                }
            } else {
                mCpuScalingPolicies = new SparseArray<>(1);
                mCpuScalingPolicies.put(0,
                        new CpuScalingPolicyPower(getAveragePower(POWER_CPU_ACTIVE),
                                new double[]{0}));
            }
        }
    }

    /**
     * Parses or computes CPU power brackets: groups of states with similar power requirements.
     */
    private void initCpuPowerBrackets() {
        boolean anyBracketsSpecified = false;
        boolean allBracketsSpecified = true;
        for (int i = mCpuScalingPolicies.size() - 1; i >= 0; i--) {
            int policy = mCpuScalingPolicies.keyAt(i);
            CpuScalingPolicyPower cpuScalingPolicyPower = mCpuScalingPolicies.valueAt(i);
            final int steps = cpuScalingPolicyPower.stepPower.length;
            cpuScalingPolicyPower.powerBrackets = new int[steps];
            if (sPowerArrayMap.get(CPU_POWER_BRACKETS_PREFIX + policy) != null) {
                anyBracketsSpecified = true;
            } else {
                allBracketsSpecified = false;
            }
        }
        if (anyBracketsSpecified && !allBracketsSpecified) {
            throw new RuntimeException(
                    "Power brackets should be specified for all scaling policies or none");
        }

        if (!allBracketsSpecified) {
            mCpuPowerBracketCount = POWER_BRACKETS_UNSPECIFIED;
            return;
        }

        mCpuPowerBracketCount = 0;
        for (int i = mCpuScalingPolicies.size() - 1; i >= 0; i--) {
            int policy = mCpuScalingPolicies.keyAt(i);
            CpuScalingPolicyPower cpuScalingPolicyPower = mCpuScalingPolicies.valueAt(i);
            final Double[] data = sPowerArrayMap.get(CPU_POWER_BRACKETS_PREFIX + policy);
            if (data.length != cpuScalingPolicyPower.powerBrackets.length) {
                throw new RuntimeException(
                        "Wrong number of items in " + CPU_POWER_BRACKETS_PREFIX + policy
                                + ", expected: "
                                + cpuScalingPolicyPower.powerBrackets.length);
            }

            for (int j = 0; j < data.length; j++) {
                final int bracket = (int) Math.round(data[j]);
                cpuScalingPolicyPower.powerBrackets[j] = bracket;
                if (bracket > mCpuPowerBracketCount) {
                    mCpuPowerBracketCount = bracket;
                }
            }
        }
        mCpuPowerBracketCount++;
    }

    private static class CpuScalingPolicyPower {
        public final double policyPower;
        public final double[] stepPower;
        public int[] powerBrackets;

        private CpuScalingPolicyPower(double policyPower, double[] stepPower) {
            this.policyPower = policyPower;
            this.stepPower = stepPower;
        }
    }

    /**
     * Returns the average additional power in (mA) when the CPU scaling policy <code>policy</code>
     * is used.
     *
     * @param policy Policy ID as per <code>ls /sys/devices/system/cpu/cpufreq</code>. Typically,
     *               policy ID corresponds to the index of the first related CPU, e.g. for "policy6"
     *               <code>/sys/devices/system/cpu/cpufreq/policy6/related_cpus</code> will
     *               contain CPU IDs like <code>6, 7</code>
     */
    public double getAveragePowerForCpuScalingPolicy(int policy) {
        CpuScalingPolicyPower cpuScalingPolicyPower = mCpuScalingPolicies.get(policy);
        if (cpuScalingPolicyPower != null) {
            return cpuScalingPolicyPower.policyPower;
        }
        return 0;
    }

    /**
     * Returns the average additional power in (mA) when the CPU scaling policy <code>policy</code>
     * is used at the <code>step</code> frequency step (this is not the frequency itself, but the
     * integer index of the frequency step).
     */
    public double getAveragePowerForCpuScalingStep(int policy, int step) {
        CpuScalingPolicyPower cpuScalingPolicyPower = mCpuScalingPolicies.get(policy);
        if (cpuScalingPolicyPower != null
                && step >= 0 && step < cpuScalingPolicyPower.stepPower.length) {
            return cpuScalingPolicyPower.stepPower[step];
        }
        return 0;
    }

    private static class CpuClusterKey {
        public final String freqKey;
        public final String clusterPowerKey;
        public final String corePowerKey;
        public final int numCpus;

        private CpuClusterKey(String freqKey, String clusterPowerKey,
                String corePowerKey, int numCpus) {
            this.freqKey = freqKey;
            this.clusterPowerKey = clusterPowerKey;
            this.corePowerKey = corePowerKey;
            this.numCpus = numCpus;
        }
    }

    /**
     * @deprecated Use CpuScalingPolicy instead
     */
    @UnsupportedAppUsage
    @Deprecated
    public int getNumCpuClusters() {
        return mCpuClusters.length;
    }

    /**
     * @deprecated Use CpuScalingPolicy instead
     */
    @Deprecated
    public int getNumCoresInCpuCluster(int cluster) {
        if (cluster < 0 || cluster >= mCpuClusters.length) {
            return 0; // index out of bound
        }
        return mCpuClusters[cluster].numCpus;
    }

    /**
     * @deprecated Use CpuScalingPolicy instead
     */
    @UnsupportedAppUsage
    @Deprecated
    public int getNumSpeedStepsInCpuCluster(int cluster) {
        if (cluster < 0 || cluster >= mCpuClusters.length) {
            return 0; // index out of bound
        }
        if (sPowerArrayMap.containsKey(mCpuClusters[cluster].freqKey)) {
            return sPowerArrayMap.get(mCpuClusters[cluster].freqKey).length;
        }
        return 1; // Only one speed
    }

    /**
     * @deprecated Use getAveragePowerForCpuScalingPolicy
     */
    @Deprecated
    public double getAveragePowerForCpuCluster(int cluster) {
        if (cluster >= 0 && cluster < mCpuClusters.length) {
            return getAveragePower(mCpuClusters[cluster].clusterPowerKey);
        }
        return 0;
    }

    /**
     * @deprecated Use getAveragePowerForCpuScalingStep
     */
    @Deprecated
    public double getAveragePowerForCpuCore(int cluster, int step) {
        if (cluster >= 0 && cluster < mCpuClusters.length) {
            return getAveragePower(mCpuClusters[cluster].corePowerKey, step);
        }
        return 0;
    }

    /**
     * Returns the number of CPU power brackets: groups of states with similar power requirements.
     * If power brackets are not specified, returns {@link #POWER_BRACKETS_UNSPECIFIED}
     */
    public int getCpuPowerBracketCount() {
        return mCpuPowerBracketCount;
    }

    /**
     * Returns the CPU power bracket corresponding to the specified scaling policy and frequency
     * step
     */
    public int getCpuPowerBracketForScalingStep(int policy, int step) {
        CpuScalingPolicyPower cpuScalingPolicyPower = mCpuScalingPolicies.get(policy);
        if (cpuScalingPolicyPower != null
                && step >= 0 && step < cpuScalingPolicyPower.powerBrackets.length) {
            return cpuScalingPolicyPower.powerBrackets[step];
        }
        return 0;
    }

    private int mNumDisplays;

    private void initDisplays() {
        // Figure out how many displays are listed in the power profile.
        mNumDisplays = 0;
        while (!Double.isNaN(
                getAveragePowerForOrdinal(POWER_GROUP_DISPLAY_AMBIENT, mNumDisplays, Double.NaN))
                || !Double.isNaN(
                getAveragePowerForOrdinal(POWER_GROUP_DISPLAY_SCREEN_ON, mNumDisplays, Double.NaN))
                || !Double.isNaN(
                getAveragePowerForOrdinal(POWER_GROUP_DISPLAY_SCREEN_FULL, mNumDisplays,
                        Double.NaN))) {
            mNumDisplays++;
        }

        // Handle legacy display power constants.
        final Double deprecatedAmbientDisplay = sPowerItemMap.get(POWER_AMBIENT_DISPLAY);
        boolean legacy = false;
        if (deprecatedAmbientDisplay != null && mNumDisplays == 0) {
            final String key = getOrdinalPowerType(POWER_GROUP_DISPLAY_AMBIENT, 0);
            Slog.w(TAG, POWER_AMBIENT_DISPLAY + " is deprecated! Use " + key + " instead.");
            sPowerItemMap.put(key, deprecatedAmbientDisplay);
            legacy = true;
        }

        final Double deprecatedScreenOn = sPowerItemMap.get(POWER_SCREEN_ON);
        if (deprecatedScreenOn != null && mNumDisplays == 0) {
            final String key = getOrdinalPowerType(POWER_GROUP_DISPLAY_SCREEN_ON, 0);
            Slog.w(TAG, POWER_SCREEN_ON + " is deprecated! Use " + key + " instead.");
            sPowerItemMap.put(key, deprecatedScreenOn);
            legacy = true;
        }

        final Double deprecatedScreenFull = sPowerItemMap.get(POWER_SCREEN_FULL);
        if (deprecatedScreenFull != null && mNumDisplays == 0) {
            final String key = getOrdinalPowerType(POWER_GROUP_DISPLAY_SCREEN_FULL, 0);
            Slog.w(TAG, POWER_SCREEN_FULL + " is deprecated! Use " + key + " instead.");
            sPowerItemMap.put(key, deprecatedScreenFull);
            legacy = true;
        }
        if (legacy) {
            mNumDisplays = 1;
        }
    }

    /**
     * Returns the number built in displays on the device as defined in the power_profile.xml.
     */
    public int getNumDisplays() {
        return mNumDisplays;
    }




    /**
     * Returns the number of memory bandwidth buckets defined in power_profile.xml, or a
     * default value if the subsystem has no recorded value.
     *
     * @return the number of memory bandwidth buckets.
     */
    public int getNumElements(String key) {
        if (sPowerItemMap.containsKey(key)) {
            return 1;
        } else if (sPowerArrayMap.containsKey(key)) {
            return sPowerArrayMap.get(key).length;
        }
        return 0;
    }

    /**
     * Returns the average current in mA consumed by the subsystem, or the given
     * default value if the subsystem has no recorded value.
     *
     * @param type         the subsystem type
     * @param defaultValue the value to return if the subsystem has no recorded value.
     * @return the average current in milliAmps.
     */
    public double getAveragePowerOrDefault(String type, double defaultValue) {
        if (sPowerItemMap.containsKey(type)) {
            return sPowerItemMap.get(type);
        } else if (sPowerArrayMap.containsKey(type)) {
            return sPowerArrayMap.get(type)[0];
        } else {
            return defaultValue;
        }
    }

    /**
     * Returns the average current in mA consumed by the subsystem
     *
     * @param type the subsystem type
     * @return the average current in milliAmps.
     */
    @UnsupportedAppUsage
    public double getAveragePower(String type) {
        return getAveragePowerOrDefault(type, 0);
    }

    /**
     * Returns the average current in mA consumed by a subsystem's specified operation, or the given
     * default value if the subsystem has no recorded value.
     *
     * @param key that describes a subsystem's battery draining operation
     *            The key is built from multiple constant, see {@link Subsystem} and
     *            {@link ModemPowerProfile}.
     * @param defaultValue the value to return if the subsystem has no recorded value.
     * @return the average current in milliAmps.
     */
    public double getAverageBatteryDrainOrDefaultMa(long key, double defaultValue) {
        final long subsystemType = key & SUBSYSTEM_MASK;
        final int subsystemFields = (int) (key & SUBSYSTEM_FIELDS_MASK);

        final double value;
        
        value = Double.NaN;
        

        if (Double.isNaN(value)) return defaultValue;
        return value;
    }

    /**
     * Returns the average current in mA consumed by a subsystem's specified operation.
     *
     * @param key that describes a subsystem's battery draining operation
     *            The key is built from multiple constant, see {@link Subsystem} and
     *            {@link ModemPowerProfile}.
     * @return the average current in milliAmps.
     */
    public double getAverageBatteryDrainMa(long key) {
        return getAverageBatteryDrainOrDefaultMa(key, 0);
    }

    /**
     * Returns the average current in mA consumed by the subsystem for the given level.
     *
     * @param type  the subsystem type
     * @param level the level of power at which the subsystem is running. For instance, the
     *              signal strength of the cell network between 0 and 4 (if there are 4 bars max.)
     *              If there is no data for multiple levels, the level is ignored.
     * @return the average current in milliAmps.
     */
    @UnsupportedAppUsage
    public double getAveragePower(String type, int level) {
        if (sPowerItemMap.containsKey(type)) {
            return sPowerItemMap.get(type);
        } else if (sPowerArrayMap.containsKey(type)) {
            final Double[] values = sPowerArrayMap.get(type);
            if (values.length > level && level >= 0) {
                return values[level];
            } else if (level < 0 || values.length == 0) {
                return 0;
            } else {
                return values[values.length - 1];
            }
        } else {
            return 0;
        }
    }

    /**
     * Returns the average current in mA consumed by an ordinaled subsystem, or the given
     * default value if the subsystem has no recorded value.
     *
     * @param group        the subsystem {@link PowerGroup}.
     * @param ordinal      which entity in the {@link PowerGroup}.
     * @param defaultValue the value to return if the subsystem has no recorded value.
     * @return the average current in milliAmps.
     */
    public double getAveragePowerForOrdinal(@PowerGroup String group, int ordinal,
            double defaultValue) {
        final String type = getOrdinalPowerType(group, ordinal);
        return getAveragePowerOrDefault(type, defaultValue);
    }

    /**
     * Returns the average current in mA consumed by an ordinaled subsystem.
     *
     * @param group        the subsystem {@link PowerGroup}.
     * @param ordinal      which entity in the {@link PowerGroup}.
     * @return the average current in milliAmps.
     */
    public double getAveragePowerForOrdinal(@PowerGroup String group, int ordinal) {
        return getAveragePowerForOrdinal(group, ordinal, 0);
    }

    /**
     * Returns the battery capacity, if available, in milli Amp Hours. If not available,
     * it returns zero.
     *
     * @return the battery capacity in mAh
     */
    @UnsupportedAppUsage
    public double getBatteryCapacity() {
        return getAveragePower(POWER_BATTERY_CAPACITY);
    }

    // Creates the key for an ordinaled power constant from the group and ordinal.
    private static String getOrdinalPowerType(@PowerGroup String group, int ordinal) {
        return group + ordinal;
    }
}
