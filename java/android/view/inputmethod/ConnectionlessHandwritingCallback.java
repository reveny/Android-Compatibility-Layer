package android.view.inputmethod;

public interface ConnectionlessHandwritingCallback
{
    int CONNECTIONLESS_HANDWRITING_ERROR_NO_TEXT_RECOGNIZED = 0;
    int CONNECTIONLESS_HANDWRITING_ERROR_UNSUPPORTED = 1;
    int CONNECTIONLESS_HANDWRITING_ERROR_OTHER = 2;

    void onResult(CharSequence text);

    void onError(int errorCode);
}
