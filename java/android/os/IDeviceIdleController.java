package android.os;

public interface IDeviceIdleController extends IInterface
{
    String DESCRIPTOR = "android.os.IDeviceIdleController";

    void addPowerSaveWhitelistApp(String packageName) throws RemoteException;

    int addPowerSaveWhitelistApps(java.util.List<String> packageNames) throws RemoteException;

    void removePowerSaveWhitelistApp(String packageName) throws RemoteException;

    void removeSystemPowerWhitelistApp(String packageName) throws RemoteException;

    void restoreSystemPowerWhitelistApp(String packageName) throws RemoteException;

    String[] getRemovedSystemPowerWhitelistApps() throws RemoteException;

    String[] getSystemPowerWhitelistExceptIdle() throws RemoteException;

    String[] getSystemPowerWhitelist() throws RemoteException;

    String[] getUserPowerWhitelist() throws RemoteException;

    String[] getFullPowerWhitelistExceptIdle() throws RemoteException;

    String[] getFullPowerWhitelist() throws RemoteException;

    int[] getAppIdWhitelistExceptIdle() throws RemoteException;

    int[] getAppIdWhitelist() throws RemoteException;

    int[] getAppIdUserWhitelist() throws RemoteException;

    int[] getAppIdTempWhitelist() throws RemoteException;

    boolean isPowerSaveWhitelistExceptIdleApp(String packageName) throws RemoteException;

    boolean isPowerSaveWhitelistApp(String packageName) throws RemoteException;

    void addPowerSaveTempWhitelistApp(String packageName, long duration, int userId, String reason) throws RemoteException;

    void addPowerSaveTempWhitelistApp(String packageName, long duration, int userId, int reasonCode, String reason)
            throws RemoteException;

    long addPowerSaveTempWhitelistAppForMms(String packageName, int userId, int reasonCode, String reason) throws RemoteException;

    long addPowerSaveTempWhitelistAppForSms(String packageName, int userId, int reasonCode, String reason) throws RemoteException;

    long whitelistAppTemporarily(String packageName, int userId, int reasonCode, String reason) throws RemoteException;

    void exitIdle(String reason) throws RemoteException;

    int setPreIdleTimeoutMode(int mode) throws RemoteException;

    void resetPreIdleTimeoutMode() throws RemoteException;

    abstract class Stub extends Binder implements IDeviceIdleController
    {
        public Stub()
        {
            attachInterface(this, DESCRIPTOR);
        }

        public static IDeviceIdleController asInterface(IBinder binder)
        {
            if (binder == null) return null;

            IInterface local = binder.queryLocalInterface(DESCRIPTOR);
            return local instanceof IDeviceIdleController ? (IDeviceIdleController)local : null;
        }

        @Override
        public IBinder asBinder()
        {
            return this;
        }
    }
}
