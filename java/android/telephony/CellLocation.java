package android.telephony;

import android.os.Bundle;

public abstract class CellLocation
{
    public CellLocation()
    {
    }

    public static void requestLocationUpdate()
    {
    }

    public static CellLocation getEmpty()
    {
        return null;
    }

    public abstract void fillInNotifierBundle(Bundle bundle);

    public abstract boolean isEmpty();

    public abstract void setStateInvalid();
}
