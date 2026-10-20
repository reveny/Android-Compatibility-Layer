package android.os;

public interface IBinder
{
    int FIRST_CALL_TRANSACTION = 0x00000001;
    int LAST_CALL_TRANSACTION = 0x00ffffff;
    int INTERFACE_TRANSACTION = ('_' << 24) | ('N' << 16) | ('T' << 8) | 'F';
    int DUMP_TRANSACTION = ('_' << 24) | ('D' << 16) | ('M' << 8) | 'P';
    int FLAG_ONEWAY = 0x00000001;

    interface DeathRecipient
    {
        void binderDied();
    }

    String getInterfaceDescriptor();

    boolean pingBinder();

    boolean isBinderAlive();

    IInterface queryLocalInterface(String descriptor);

    boolean transact(int code, Parcel data, Parcel reply, int flags);

    void linkToDeath(DeathRecipient recipient, int flags);

    boolean unlinkToDeath(DeathRecipient recipient, int flags);

    void dump(java.io.FileDescriptor target, String[] arguments);
}
