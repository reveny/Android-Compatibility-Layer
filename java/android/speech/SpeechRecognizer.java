package android.speech;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

public class SpeechRecognizer
{
    public static final String CONFIDENCE_SCORES = "confidence_scores";
    public static final String DETECTED_LANGUAGE = "detected_language";
    public static final int ERROR_AUDIO = 3;
    public static final int ERROR_CANNOT_CHECK_SUPPORT = 14;
    public static final int ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS = 15;
    public static final int ERROR_CLIENT = 5;
    public static final int ERROR_INSUFFICIENT_PERMISSIONS = 9;
    public static final int ERROR_LANGUAGE_NOT_SUPPORTED = 12;
    public static final int ERROR_LANGUAGE_UNAVAILABLE = 13;
    public static final int ERROR_NETWORK = 2;
    public static final int ERROR_NETWORK_TIMEOUT = 1;
    public static final int ERROR_NO_MATCH = 7;
    public static final int ERROR_RECOGNIZER_BUSY = 8;
    public static final int ERROR_SERVER = 4;
    public static final int ERROR_SERVER_DISCONNECTED = 11;
    public static final int ERROR_SPEECH_TIMEOUT = 6;
    public static final int ERROR_TOO_MANY_REQUESTS = 10;
    public static final String LANGUAGE_DETECTION_CONFIDENCE_LEVEL = "language_detection_confidence_level";
    public static final int LANGUAGE_DETECTION_CONFIDENCE_LEVEL_CONFIDENT = 2;
    public static final int LANGUAGE_DETECTION_CONFIDENCE_LEVEL_HIGHLY_CONFIDENT = 3;
    public static final int LANGUAGE_DETECTION_CONFIDENCE_LEVEL_NOT_CONFIDENT = 1;
    public static final int LANGUAGE_DETECTION_CONFIDENCE_LEVEL_UNKNOWN = 0;
    public static final String LANGUAGE_SWITCH_RESULT = "language_switch_result";
    public static final int LANGUAGE_SWITCH_RESULT_FAILED = 2;
    public static final int LANGUAGE_SWITCH_RESULT_NOT_ATTEMPTED = 0;
    public static final int LANGUAGE_SWITCH_RESULT_SKIPPED_NO_MODEL = 3;
    public static final int LANGUAGE_SWITCH_RESULT_SUCCEEDED = 1;
    public static final String RECOGNITION_PARTS = "recognition_parts";
    public static final String RESULTS_ALTERNATIVES = "results_alternatives";
    public static final String RESULTS_RECOGNITION = "results_recognition";
    public static final String TOP_LOCALE_ALTERNATIVES = "top_locale_alternatives";

    private final Context mContext;
    private final ComponentName mServiceComponent;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private RecognitionListener mListener;
    private boolean mDestroyed;

    SpeechRecognizer(Context context, ComponentName serviceComponent)
    {
        mContext = context;
        mServiceComponent = serviceComponent;
    }

    public void cancel()
    {
        checkIsCalledFromMainThread();
    }

    public void checkRecognitionSupport(Intent recognizerIntent, Executor executor, final RecognitionSupportCallback supportListener)
    {
        if (recognizerIntent == null) throw new NullPointerException("intent must not be null");
        if (executor == null) throw new NullPointerException("executor must not be null");
        if (supportListener == null) throw new NullPointerException("listener must not be null");

        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                supportListener.onError(ERROR_CANNOT_CHECK_SUPPORT);
            }
        });
    }

    public static SpeechRecognizer createOnDeviceSpeechRecognizer(Context context)
    {
        if (context == null) throw new IllegalArgumentException("Context cannot be null");

        return new SpeechRecognizer(context, null);
    }

    public static SpeechRecognizer createSpeechRecognizer(Context context)
    {
        return createSpeechRecognizer(context, null);
    }

    public static SpeechRecognizer createSpeechRecognizer(Context context, ComponentName serviceComponent)
    {
        if (context == null) throw new IllegalArgumentException("Context cannot be null");

        return new SpeechRecognizer(context, serviceComponent);
    }

    public void destroy()
    {
        mDestroyed = true;
        mListener = null;
    }

    public static boolean isOnDeviceRecognitionAvailable(Context context)
    {
        return false;
    }

    public static boolean isRecognitionAvailable(Context context)
    {
        return false;
    }

    public void setRecognitionListener(RecognitionListener listener)
    {
        checkIsCalledFromMainThread();
        mListener = listener;
    }

    public void startListening(Intent recognizerIntent)
    {
        if (recognizerIntent == null) throw new IllegalArgumentException("intent must not be null");

        checkIsCalledFromMainThread();
        reportError(ERROR_CLIENT);
    }

    public void stopListening()
    {
        checkIsCalledFromMainThread();
    }

    public void triggerModelDownload(Intent recognizerIntent)
    {
        if (recognizerIntent == null) throw new NullPointerException("intent must not be null");

        checkIsCalledFromMainThread();
        reportError(ERROR_CLIENT);
    }

    public void triggerModelDownload(Intent recognizerIntent, Executor executor, final ModelDownloadListener listener)
    {
        if (recognizerIntent == null) throw new NullPointerException("intent must not be null");
        if (executor == null) throw new NullPointerException("executor must not be null");
        if (listener == null) throw new NullPointerException("listener must not be null");

        checkIsCalledFromMainThread();
        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                listener.onError(ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS);
            }
        });
    }

    private void reportError(final int error)
    {
        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                RecognitionListener listener = mListener;

                if (listener != null && !mDestroyed) listener.onError(error);
            }
        });
    }

    private static void checkIsCalledFromMainThread()
    {
        if (Looper.myLooper() != Looper.getMainLooper())
        {
            throw new RuntimeException("SpeechRecognizer should be used only from the application's main thread");
        }
    }
}
