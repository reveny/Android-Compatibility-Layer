package android.speech;

public interface RecognitionSupportCallback
{
    void onError(int error);

    void onSupportResult(RecognitionSupport recognitionSupport);
}
