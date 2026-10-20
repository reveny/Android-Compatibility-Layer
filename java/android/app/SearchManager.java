package android.app;

import android.content.ComponentName;

public class SearchManager
{
    public static final String QUERY = "query";
    public static final String APP_DATA = "app_data";
    public static final String EXTRA_NEW_SEARCH = "new_search";

    public SearchManager(android.content.Context context)
    {
    }

    public void startSearch(String initialQuery, boolean selectInitialQuery, ComponentName launchActivity, android.os.Bundle appSearchData,
                            boolean globalSearch)
    {
    }

    public void stopSearch()
    {
    }

    public ComponentName getGlobalSearchActivity()
    {
        return null;
    }

    public android.content.ComponentName getWebSearchActivity()
    {
        return null;
    }
}
