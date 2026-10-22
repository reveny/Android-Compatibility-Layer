package android.location;

public interface OnNmeaMessageListener
{
    void onNmeaMessage(String message, long timestamp);
}
