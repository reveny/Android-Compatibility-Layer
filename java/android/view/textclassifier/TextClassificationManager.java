package android.view.textclassifier;

public final class TextClassificationManager
{
    private static TextClassificationManager sInstance;

    private final Object mLock = new Object();
    private TextClassifier mCustomTextClassifier;
    private TextClassificationSessionFactory mSessionFactory;

    private TextClassificationManager()
    {
    }

    public static TextClassificationManager getInstance()
    {
        if (sInstance == null) sInstance = new TextClassificationManager();

        return sInstance;
    }

    public TextClassifier getTextClassifier()
    {
        synchronized (mLock)
        {
            return mCustomTextClassifier != null ? mCustomTextClassifier : TextClassifier.NO_OP;
        }
    }

    public void setTextClassifier(TextClassifier textClassifier)
    {
        synchronized (mLock)
        {
            mCustomTextClassifier = textClassifier;
        }
    }

    public TextClassifier createTextClassificationSession(TextClassificationContext classificationContext)
    {
        if (classificationContext == null) throw new NullPointerException("classificationContext");

        TextClassificationSessionFactory factory;
        synchronized (mLock)
        {
            factory = mSessionFactory;
        }

        if (factory != null)
        {
            TextClassifier session = factory.createTextClassificationSession(classificationContext);
            if (session != null) return session;
        }

        return getTextClassifier();
    }

    public void setTextClassificationSessionFactory(TextClassificationSessionFactory factory)
    {
        synchronized (mLock)
        {
            mSessionFactory = factory;
        }
    }
}
