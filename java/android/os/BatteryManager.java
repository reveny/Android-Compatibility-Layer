package android.os;

import android.content.Context;
import android.content.Intent;

public class BatteryManager
{
    public static final String ACTION_CHARGING = "android.os.action.CHARGING";
    public static final String ACTION_DISCHARGING = "android.os.action.DISCHARGING";

    public static final int BATTERY_HEALTH_UNKNOWN = 1;
    public static final int BATTERY_HEALTH_GOOD = 2;
    public static final int BATTERY_HEALTH_OVERHEAT = 3;
    public static final int BATTERY_HEALTH_DEAD = 4;
    public static final int BATTERY_HEALTH_OVER_VOLTAGE = 5;
    public static final int BATTERY_HEALTH_UNSPECIFIED_FAILURE = 6;
    public static final int BATTERY_HEALTH_COLD = 7;

    public static final int BATTERY_PLUGGED_AC = 1;
    public static final int BATTERY_PLUGGED_USB = 2;
    public static final int BATTERY_PLUGGED_WIRELESS = 4;
    public static final int BATTERY_PLUGGED_DOCK = 8;

    public static final int BATTERY_PROPERTY_CHARGE_COUNTER = 1;
    public static final int BATTERY_PROPERTY_CURRENT_NOW = 2;
    public static final int BATTERY_PROPERTY_CURRENT_AVERAGE = 3;
    public static final int BATTERY_PROPERTY_CAPACITY = 4;
    public static final int BATTERY_PROPERTY_ENERGY_COUNTER = 5;
    public static final int BATTERY_PROPERTY_STATUS = 6;

    public static final int BATTERY_STATUS_UNKNOWN = 1;
    public static final int BATTERY_STATUS_CHARGING = 2;
    public static final int BATTERY_STATUS_DISCHARGING = 3;
    public static final int BATTERY_STATUS_NOT_CHARGING = 4;
    public static final int BATTERY_STATUS_FULL = 5;

    public static final String EXTRA_BATTERY_LOW = "battery_low";
    public static final String EXTRA_CHARGING_STATUS = "android.os.extra.CHARGING_STATUS";
    public static final String EXTRA_CYCLE_COUNT = "android.os.extra.CYCLE_COUNT";
    public static final String EXTRA_HEALTH = "health";
    public static final String EXTRA_ICON_SMALL = "icon-small";
    public static final String EXTRA_LEVEL = "level";
    public static final String EXTRA_PLUGGED = "plugged";
    public static final String EXTRA_PRESENT = "present";
    public static final String EXTRA_SCALE = "scale";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_TECHNOLOGY = "technology";
    public static final String EXTRA_TEMPERATURE = "temperature";
    public static final String EXTRA_VOLTAGE = "voltage";

    private static final int LEVEL = 100;
    private static final int SCALE = 100;
    private static final int TEMPERATURE_TENTHS_CELSIUS = 250;
    private static final int VOLTAGE_MILLIVOLTS = 4350;
    private static final long CHARGE_COUNTER_MICROAMPERE_HOURS = 4000000L;
    private static final long ENERGY_COUNTER_NANOWATT_HOURS = 17400000000L;

    public BatteryManager(Context context)
    {
    }

    public boolean isCharging()
    {
        return true;
    }

    public int getIntProperty(int id)
    {
        switch (id)
        {
            case BATTERY_PROPERTY_CAPACITY:
                return LEVEL;
            case BATTERY_PROPERTY_STATUS:
                return BATTERY_STATUS_FULL;
            case BATTERY_PROPERTY_CHARGE_COUNTER:
                return (int)CHARGE_COUNTER_MICROAMPERE_HOURS;
            case BATTERY_PROPERTY_CURRENT_NOW:
            case BATTERY_PROPERTY_CURRENT_AVERAGE:
                return 0;
            default:
                return Integer.MIN_VALUE;
        }
    }

    public long getLongProperty(int id)
    {
        switch (id)
        {
            case BATTERY_PROPERTY_CAPACITY:
                return LEVEL;
            case BATTERY_PROPERTY_STATUS:
                return BATTERY_STATUS_FULL;
            case BATTERY_PROPERTY_CHARGE_COUNTER:
                return CHARGE_COUNTER_MICROAMPERE_HOURS;
            case BATTERY_PROPERTY_ENERGY_COUNTER:
                return ENERGY_COUNTER_NANOWATT_HOURS;
            case BATTERY_PROPERTY_CURRENT_NOW:
            case BATTERY_PROPERTY_CURRENT_AVERAGE:
                return 0L;
            default:
                return Long.MIN_VALUE;
        }
    }

    public long computeChargeTimeRemaining()
    {
        return 0L;
    }

    public static Intent createBatteryChangedIntent()
    {
        Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED);
        intent.putExtra(EXTRA_PRESENT, true);
        intent.putExtra(EXTRA_LEVEL, LEVEL);
        intent.putExtra(EXTRA_SCALE, SCALE);
        intent.putExtra(EXTRA_STATUS, BATTERY_STATUS_FULL);
        intent.putExtra(EXTRA_HEALTH, BATTERY_HEALTH_GOOD);
        intent.putExtra(EXTRA_PLUGGED, BATTERY_PLUGGED_AC);
        intent.putExtra(EXTRA_VOLTAGE, VOLTAGE_MILLIVOLTS);
        intent.putExtra(EXTRA_TEMPERATURE, TEMPERATURE_TENTHS_CELSIUS);
        intent.putExtra(EXTRA_TECHNOLOGY, "Li-ion");
        intent.putExtra(EXTRA_BATTERY_LOW, false);
        intent.putExtra(EXTRA_CYCLE_COUNT, 1);
        intent.putExtra(EXTRA_CHARGING_STATUS, 1);
        intent.putExtra(EXTRA_ICON_SMALL, 0);
        return intent;
    }
}
