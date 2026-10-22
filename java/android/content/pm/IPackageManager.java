package android.content.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import java.util.List;

public interface IPackageManager extends IInterface
{
    String DESCRIPTOR = "android.content.pm.IPackageManager";

    PackageInfo getPackageInfo(String packageName, long flags, int userId) throws RemoteException;

    PackageInfo getPackageInfo(String packageName, int flags, int userId) throws RemoteException;

    ApplicationInfo getApplicationInfo(String packageName, long flags, int userId) throws RemoteException;

    ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) throws RemoteException;

    ActivityInfo getActivityInfo(ComponentName className, long flags, int userId) throws RemoteException;

    ActivityInfo getReceiverInfo(ComponentName className, long flags, int userId) throws RemoteException;

    ServiceInfo getServiceInfo(ComponentName className, long flags, int userId) throws RemoteException;

    ProviderInfo getProviderInfo(ComponentName className, long flags, int userId) throws RemoteException;

    int getPackageUid(String packageName, long flags, int userId) throws RemoteException;

    String[] getPackagesForUid(int uid) throws RemoteException;

    String getNameForUid(int uid) throws RemoteException;

    String getInstallerPackageName(String packageName) throws RemoteException;

    int checkPermission(String permissionName, String packageName, int userId) throws RemoteException;

    int checkUidPermission(String permissionName, int uid) throws RemoteException;

    int checkSignatures(String firstPackage, String secondPackage, int userId) throws RemoteException;

    boolean hasSystemFeature(String name, int version) throws RemoteException;

    boolean isSafeMode() throws RemoteException;

    boolean isPackageAvailable(String packageName, int userId) throws RemoteException;

    ResolveInfo resolveIntent(Intent intent, String resolvedType, long flags, int userId) throws RemoteException;

    ResolveInfo resolveService(Intent intent, String resolvedType, long flags, int userId) throws RemoteException;

    ProviderInfo resolveContentProvider(String authority, long flags, int userId) throws RemoteException;

    List<ResolveInfo> queryIntentActivities(Intent intent, String resolvedType, long flags, int userId) throws RemoteException;

    List<ResolveInfo> queryIntentServices(Intent intent, String resolvedType, long flags, int userId) throws RemoteException;

    List<ResolveInfo> queryIntentReceivers(Intent intent, String resolvedType, long flags, int userId) throws RemoteException;

    int getComponentEnabledSetting(ComponentName componentName, int userId) throws RemoteException;

    void setComponentEnabledSetting(ComponentName componentName, int newState, int flags, int userId, String callingPackage)
            throws RemoteException;

    int getApplicationEnabledSetting(String packageName, int userId) throws RemoteException;

    abstract class Stub extends Binder implements IPackageManager
    {
        public Stub()
        {
            attachInterface(this, DESCRIPTOR);
        }

        public static IPackageManager asInterface(IBinder binder)
        {
            if (binder == null) return null;

            IInterface local = binder.queryLocalInterface(DESCRIPTOR);
            return local instanceof IPackageManager ? (IPackageManager)local : null;
        }

        @Override
        public IBinder asBinder()
        {
            return this;
        }
    }
}
