package android.app;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

public interface IApplicationThread extends IInterface
{
    String DESCRIPTOR = "android.app.IApplicationThread";

    abstract class Stub extends Binder implements IApplicationThread
    {
        public Stub()
        {
            attachInterface(this, DESCRIPTOR);
        }

        public static IApplicationThread asInterface(IBinder binder)
        {
            if (binder == null) return null;

            IInterface local = binder.queryLocalInterface(DESCRIPTOR);
            return local instanceof IApplicationThread ? (IApplicationThread)local : null;
        }

        @Override
        public IBinder asBinder()
        {
            return this;
        }
    }
}
