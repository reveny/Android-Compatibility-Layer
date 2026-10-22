package android.widget;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.ActionProvider;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShareActionProvider extends ActionProvider
{
    public static final String DEFAULT_SHARE_HISTORY_FILE_NAME = "share_history.xml";

    private static final int DEFAULT_INITIAL_ACTIVITY_COUNT = 4;

    private static final HashMap<String, HashMap<String, Integer>> sHistories = new HashMap<>();

    private final Context mContext;
    private String mShareHistoryFileName = DEFAULT_SHARE_HISTORY_FILE_NAME;
    private Intent mShareIntent;
    private OnShareTargetSelectedListener mListener;

    public ShareActionProvider(Context context)
    {
        super(context);
        mContext = context;
    }

    public void setOnShareTargetSelectedListener(OnShareTargetSelectedListener listener)
    {
        mListener = listener;
    }

    public void setShareHistoryFileName(String shareHistoryFile)
    {
        mShareHistoryFileName = shareHistoryFile;
    }

    public void setShareIntent(Intent shareIntent)
    {
        if (shareIntent != null)
        {
            String action = shareIntent.getAction();
            if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action))
            {
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            }
        }

        mShareIntent = shareIntent;
    }

    @Override
    public View onCreateActionView()
    {
        ImageButton button = new ImageButton(mContext);
        List<ResolveInfo> targets = targets();
        PackageManager packageManager = mContext.getPackageManager();

        if (!targets.isEmpty()) button.setImageDrawable(targets.get(0).loadIcon(packageManager));
        button.setContentDescription("Share with");
        button.setEnabled(mShareIntent != null && !targets.isEmpty());
        button.setOnClickListener(view ->
        {
            List<ResolveInfo> current = targets();
            if (current.isEmpty()) return;

            if (current.size() == 1)
            {
                choose(current.get(0));
                return;
            }

            PopupMenu popup = new PopupMenu(mContext, view);
            populate(popup.getMenu(), current, current.size());
            popup.show();
        });

        return button;
    }

    @Override
    public boolean hasSubMenu()
    {
        return true;
    }

    @Override
    public void onPrepareSubMenu(SubMenu subMenu)
    {
        subMenu.clear();

        List<ResolveInfo> targets = targets();
        int shown = Math.min(targets.size(), DEFAULT_INITIAL_ACTIVITY_COUNT);
        populate(subMenu, targets, shown);

        if (shown >= targets.size()) return;

        SubMenu expanded = subMenu.addSubMenu(0, shown, shown, "See all");
        populate(expanded, targets, targets.size());
    }

    private void populate(android.view.Menu menu, List<ResolveInfo> targets, int count)
    {
        PackageManager packageManager = mContext.getPackageManager();

        for (int index = 0; index < count; index++)
        {
            ResolveInfo target = targets.get(index);
            MenuItem item = menu.add(0, index, index, target.loadLabel(packageManager));
            item.setIcon(target.loadIcon(packageManager));
            item.setOnMenuItemClickListener(clicked ->
            {
                choose(target);
                return true;
            });
        }
    }

    private List<ResolveInfo> targets()
    {
        if (mShareIntent == null) return new ArrayList<>();

        List<ResolveInfo> resolved = mContext.getPackageManager().queryIntentActivities(mShareIntent, 0);
        ArrayList<ResolveInfo> sorted = resolved == null ? new ArrayList<>() : new ArrayList<>(resolved);
        HashMap<String, Integer> history = history();
        sorted.sort((first, second) -> Integer.compare(count(history, second), count(history, first)));
        return sorted;
    }

    private HashMap<String, Integer> history()
    {
        synchronized (sHistories)
        {
            String key = mShareHistoryFileName == null ? "" : mShareHistoryFileName;
            HashMap<String, Integer> history = sHistories.get(key);

            if (history == null)
            {
                history = new HashMap<>();
                sHistories.put(key, history);
            }

            return history;
        }
    }

    private static int count(HashMap<String, Integer> history, ResolveInfo target)
    {
        synchronized (sHistories)
        {
            Integer count = history.get(componentOf(target).flattenToString());
            return count == null ? 0 : count;
        }
    }

    private static ComponentName componentOf(ResolveInfo target)
    {
        return new ComponentName(target.activityInfo.packageName, target.activityInfo.name);
    }

    private void choose(ResolveInfo target)
    {
        if (mShareIntent == null) return;

        ComponentName component = componentOf(target);
        Intent intent = new Intent(mShareIntent);
        intent.setComponent(component);

        if (mShareHistoryFileName != null)
        {
            HashMap<String, Integer> history = history();
            synchronized (sHistories)
            {
                String key = component.flattenToString();
                Integer count = history.get(key);
                history.put(key, count == null ? 1 : count + 1);
            }
        }

        if (mListener != null) mListener.onShareTargetSelected(this, intent);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    public interface OnShareTargetSelectedListener
    {
        boolean onShareTargetSelected(ShareActionProvider source, Intent intent);
    }
}
