package android.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class SearchManager implements DialogInterface.OnDismissListener, DialogInterface.OnCancelListener
{
    public static final char MENU_KEY = 's';
    public static final int MENU_KEYCODE = 47;
    public static final String QUERY = "query";
    public static final String USER_QUERY = "user_query";
    public static final String APP_DATA = "app_data";
    public static final String ACTION_KEY = "action_key";
    public static final String EXTRA_DATA_KEY = "intent_extra_data_key";
    public static final String EXTRA_SELECT_QUERY = "select_query";
    public static final String EXTRA_WEB_SEARCH_PENDINGINTENT = "web_search_pendingintent";
    public static final String EXTRA_NEW_SEARCH = "new_search";
    public static final String CURSOR_EXTRA_KEY_IN_PROGRESS = "in_progress";
    public static final String ACTION_MSG = "action_msg";
    public static final int FLAG_QUERY_REFINEMENT = 1;
    public static final String SUGGEST_URI_PATH_QUERY = "search_suggest_query";
    public static final String SUGGEST_MIME_TYPE = "vnd.android.cursor.dir/vnd.android.search.suggest";
    public static final String SUGGEST_URI_PATH_SHORTCUT = "search_suggest_shortcut";
    public static final String SHORTCUT_MIME_TYPE = "vnd.android.cursor.item/vnd.android.search.suggest";
    public static final String SUGGEST_COLUMN_FORMAT = "suggest_format";
    public static final String SUGGEST_COLUMN_TEXT_1 = "suggest_text_1";
    public static final String SUGGEST_COLUMN_TEXT_2 = "suggest_text_2";
    public static final String SUGGEST_COLUMN_TEXT_2_URL = "suggest_text_2_url";
    public static final String SUGGEST_COLUMN_ICON_1 = "suggest_icon_1";
    public static final String SUGGEST_COLUMN_ICON_2 = "suggest_icon_2";
    public static final String SUGGEST_COLUMN_RESULT_CARD_IMAGE = "suggest_result_card_image";
    public static final String SUGGEST_COLUMN_INTENT_ACTION = "suggest_intent_action";
    public static final String SUGGEST_COLUMN_INTENT_DATA = "suggest_intent_data";
    public static final String SUGGEST_COLUMN_INTENT_EXTRA_DATA = "suggest_intent_extra_data";
    public static final String SUGGEST_COLUMN_INTENT_DATA_ID = "suggest_intent_data_id";
    public static final String SUGGEST_COLUMN_QUERY = "suggest_intent_query";
    public static final String SUGGEST_COLUMN_SHORTCUT_ID = "suggest_shortcut_id";
    public static final String SUGGEST_COLUMN_SPINNER_WHILE_REFRESHING = "suggest_spinner_while_refreshing";
    public static final String SUGGEST_COLUMN_CONTENT_TYPE = "suggest_content_type";
    public static final String SUGGEST_COLUMN_IS_LIVE = "suggest_is_live";
    public static final String SUGGEST_COLUMN_VIDEO_WIDTH = "suggest_video_width";
    public static final String SUGGEST_COLUMN_VIDEO_HEIGHT = "suggest_video_height";
    public static final String SUGGEST_COLUMN_AUDIO_CHANNEL_CONFIG = "suggest_audio_channel_config";
    public static final String SUGGEST_COLUMN_PURCHASE_PRICE = "suggest_purchase_price";
    public static final String SUGGEST_COLUMN_RENTAL_PRICE = "suggest_rental_price";
    public static final String SUGGEST_COLUMN_RATING_STYLE = "suggest_rating_style";
    public static final String SUGGEST_COLUMN_RATING_SCORE = "suggest_rating_score";
    public static final String SUGGEST_COLUMN_PRODUCTION_YEAR = "suggest_production_year";
    public static final String SUGGEST_COLUMN_DURATION = "suggest_duration";
    public static final String SUGGEST_COLUMN_FLAGS = "suggest_flags";
    public static final String SUGGEST_COLUMN_LAST_ACCESS_HINT = "suggest_last_access_hint";
    public static final String SUGGEST_NEVER_MAKE_SHORTCUT = "_-1";
    public static final String SUGGEST_PARAMETER_LIMIT = "limit";
    public static final String INTENT_ACTION_GLOBAL_SEARCH = "android.search.action.GLOBAL_SEARCH";
    public static final String INTENT_ACTION_SEARCH_SETTINGS = "android.search.action.SEARCH_SETTINGS";
    public static final String INTENT_ACTION_WEB_SEARCH_SETTINGS = "android.search.action.WEB_SEARCH_SETTINGS";
    public static final String INTENT_ACTION_SEARCHABLES_CHANGED = "android.search.action.SEARCHABLES_CHANGED";
    public static final String INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED = "android.search.action.GLOBAL_SEARCH_ACTIVITY_CHANGED";
    public static final String INTENT_ACTION_SEARCH_SETTINGS_CHANGED = "android.search.action.SETTINGS_CHANGED";

    public interface OnDismissListener
    {
        void onDismiss();
    }

    public interface OnCancelListener
    {
        void onCancel();
    }

    private final Context mContext;
    private OnDismissListener mDismissListener;
    private OnCancelListener mCancelListener;

    SearchManager()
    {
        this(null);
    }

    public SearchManager(Context context)
    {
        mContext = context;
    }

    public void startSearch(String initialQuery, boolean selectInitialQuery, ComponentName launchActivity, Bundle appSearchData,
                            boolean globalSearch)
    {
        android.util.Log.i("SearchManager", "startSearch has no system search dialog on this device");
    }

    public void triggerSearch(String query, ComponentName launchActivity, Bundle appSearchData)
    {
        if (query == null || query.trim().isEmpty()) throw new IllegalArgumentException("query cannot be empty");
        if (mContext == null || launchActivity == null) return;

        Intent intent = new Intent(Intent.ACTION_SEARCH);
        intent.setComponent(launchActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(QUERY, query);
        if (appSearchData != null) intent.putExtra(APP_DATA, appSearchData);

        mContext.startActivity(intent);
    }

    public void stopSearch()
    {
    }

    public void setOnDismissListener(OnDismissListener listener)
    {
        mDismissListener = listener;
    }

    public void setOnCancelListener(OnCancelListener listener)
    {
        mCancelListener = listener;
    }

    @Override
    public void onCancel(DialogInterface dialog)
    {
        if (mCancelListener != null) mCancelListener.onCancel();
    }

    @Override
    public void onDismiss(DialogInterface dialog)
    {
        if (mDismissListener != null) mDismissListener.onDismiss();
    }

    public SearchableInfo getSearchableInfo(ComponentName componentName)
    {
        if (mContext == null || componentName == null) return null;

        return SearchableInfo.getActivityMetaData(mContext, componentName);
    }

    public List<SearchableInfo> getSearchablesInGlobalSearch()
    {
        return new ArrayList<>();
    }

    public ComponentName getGlobalSearchActivity()
    {
        return null;
    }

    public ComponentName getWebSearchActivity()
    {
        return null;
    }
}
