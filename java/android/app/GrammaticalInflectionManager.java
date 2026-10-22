package android.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.Log;

public class GrammaticalInflectionManager
{
    private static final String TAG = "GrammaticalInflection";
    private static final String PREFERENCES = "android.app.grammatical_inflection";
    private static final String KEY_GENDER = "requested_gender";

    private final Context mContext;

    GrammaticalInflectionManager()
    {
        this(null);
    }

    GrammaticalInflectionManager(Context context)
    {
        mContext = context;
    }

    private SharedPreferences preferences()
    {
        return mContext == null ? null : mContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public int getApplicationGrammaticalGender()
    {
        SharedPreferences preferences = preferences();
        int requested = preferences == null ? Configuration.GRAMMATICAL_GENDER_NOT_SPECIFIED
                                            : preferences.getInt(KEY_GENDER, Configuration.GRAMMATICAL_GENDER_NOT_SPECIFIED);

        if (requested != Configuration.GRAMMATICAL_GENDER_NOT_SPECIFIED) return requested;

        return mContext == null ? Configuration.GRAMMATICAL_GENDER_NOT_SPECIFIED
                                : mContext.getResources().getConfiguration().getGrammaticalGender();
    }

    public void setRequestedApplicationGrammaticalGender(int grammaticalGender)
    {
        if (grammaticalGender < Configuration.GRAMMATICAL_GENDER_NOT_SPECIFIED || grammaticalGender > Configuration.GRAMMATICAL_GENDER_MASCULINE)
        {
            throw new IllegalArgumentException("Unknown grammatical gender");
        }

        SharedPreferences preferences = preferences();
        if (preferences != null) preferences.edit().putInt(KEY_GENDER, grammaticalGender).apply();

        Log.i(TAG, "the application asked for grammatical gender " + grammaticalGender);
    }

    public int getSystemGrammaticalGender()
    {
        return Configuration.GRAMMATICAL_GENDER_NOT_SPECIFIED;
    }
}
