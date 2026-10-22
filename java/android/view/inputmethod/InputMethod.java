package android.view.inputmethod;

import android.os.IBinder;
import android.os.ResultReceiver;

public interface InputMethod
{
    String SERVICE_INTERFACE = "android.view.InputMethod";
    String SERVICE_META_DATA = "android.view.im";
    int SHOW_EXPLICIT = 1;
    int SHOW_FORCED = 2;

    void attachToken(IBinder token);

    void bindInput(InputBinding binding);

    void unbindInput();

    void startInput(InputConnection inputConnection, EditorInfo editorInfo);

    void restartInput(InputConnection inputConnection, EditorInfo editorInfo);

    void createSession(SessionCallback callback);

    void setSessionEnabled(InputMethodSession session, boolean enabled);

    void revokeSession(InputMethodSession session);

    void showSoftInput(int flags, ResultReceiver resultReceiver);

    void hideSoftInput(int flags, ResultReceiver resultReceiver);

    void changeInputMethodSubtype(InputMethodSubtype subtype);

    interface SessionCallback
    {
        void sessionCreated(InputMethodSession session);
    }
}
