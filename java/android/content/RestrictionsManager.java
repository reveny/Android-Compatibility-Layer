package android.content;

import android.os.Bundle;
import android.os.PersistableBundle;

import java.util.ArrayList;
import java.util.List;

public class RestrictionsManager
{
    public static final String ACTION_PERMISSION_RESPONSE_RECEIVED = "android.content.action.PERMISSION_RESPONSE_RECEIVED";
    public static final String ACTION_REQUEST_LOCAL_APPROVAL = "android.content.action.REQUEST_LOCAL_APPROVAL";
    public static final String ACTION_REQUEST_PERMISSION = "android.content.action.REQUEST_PERMISSION";
    public static final String EXTRA_PACKAGE_NAME = "android.content.extra.PACKAGE_NAME";
    public static final String EXTRA_REQUEST_BUNDLE = "android.content.extra.REQUEST_BUNDLE";
    public static final String EXTRA_REQUEST_ID = "android.content.extra.REQUEST_ID";
    public static final String EXTRA_REQUEST_TYPE = "android.content.extra.REQUEST_TYPE";
    public static final String EXTRA_RESPONSE_BUNDLE = "android.content.extra.RESPONSE_BUNDLE";
    public static final String META_DATA_APP_RESTRICTIONS = "android.content.APP_RESTRICTIONS";
    public static final String REQUEST_KEY_APPROVE_LABEL = "android.request.approve_label";
    public static final String REQUEST_KEY_DATA = "android.request.data";
    public static final String REQUEST_KEY_DENY_LABEL = "android.request.deny_label";
    public static final String REQUEST_KEY_ICON = "android.request.icon";
    public static final String REQUEST_KEY_ID = "android.request.id";
    public static final String REQUEST_KEY_MESSAGE = "android.request.mesg";
    public static final String REQUEST_KEY_NEW_REQUEST = "android.request.new_request";
    public static final String REQUEST_KEY_TITLE = "android.request.title";
    public static final String REQUEST_TYPE_APPROVAL = "android.request.type.approval";
    public static final String RESPONSE_KEY_ERROR_CODE = "android.response.errorcode";
    public static final String RESPONSE_KEY_MESSAGE = "android.response.msg";
    public static final String RESPONSE_KEY_RESPONSE_TIMESTAMP = "android.response.timestamp";
    public static final String RESPONSE_KEY_RESULT = "android.response.result";

    public static final int RESULT_APPROVED = 1;
    public static final int RESULT_DENIED = 2;
    public static final int RESULT_NO_RESPONSE = 3;
    public static final int RESULT_UNKNOWN_REQUEST = 4;
    public static final int RESULT_ERROR = 5;
    public static final int RESULT_ERROR_BAD_REQUEST = 1;
    public static final int RESULT_ERROR_NETWORK = 2;
    public static final int RESULT_ERROR_INTERNAL = 3;

    private final Context mContext;

    public RestrictionsManager(Context context)
    {
        mContext = context;
    }

    public Bundle getApplicationRestrictions()
    {
        return new Bundle();
    }

    public List<Bundle> getApplicationRestrictionsPerAdmin()
    {
        return new ArrayList<>();
    }

    public boolean hasRestrictionsProvider()
    {
        return false;
    }

    public void requestPermission(String requestType, String requestId, PersistableBundle request)
    {
        if (requestType == null) throw new NullPointerException("requestType cannot be null");
        if (requestId == null) throw new NullPointerException("requestId cannot be null");
        if (request == null) throw new NullPointerException("request cannot be null");

        throw new IllegalStateException("there is no restrictions provider on this device");
    }

    public Intent createLocalApprovalIntent()
    {
        return null;
    }

    public void notifyPermissionResponse(String packageName, PersistableBundle response)
    {
        if (packageName == null) throw new NullPointerException("packageName cannot be null");
        if (response == null) throw new NullPointerException("request cannot be null");
    }

    public List<RestrictionEntry> getManifestRestrictions(String packageName)
    {
        return new ArrayList<>();
    }

    public static Bundle convertRestrictionsToBundle(List<RestrictionEntry> entries)
    {
        Bundle bundle = new Bundle();

        for (RestrictionEntry entry : entries)
        {
            addRestrictionToBundle(bundle, entry);
        }

        return bundle;
    }

    private static Bundle addRestrictionToBundle(Bundle bundle, RestrictionEntry entry)
    {
        switch (entry.getType())
        {
            case RestrictionEntry.TYPE_BOOLEAN:
                bundle.putBoolean(entry.getKey(), entry.getSelectedState());
                break;
            case RestrictionEntry.TYPE_CHOICE:
            case RestrictionEntry.TYPE_MULTI_SELECT:
                bundle.putStringArray(entry.getKey(), entry.getAllSelectedStrings());
                break;
            case RestrictionEntry.TYPE_INTEGER:
                bundle.putInt(entry.getKey(), entry.getIntValue());
                break;
            case RestrictionEntry.TYPE_STRING:
            case RestrictionEntry.TYPE_NULL:
                bundle.putString(entry.getKey(), entry.getSelectedString());
                break;
            case RestrictionEntry.TYPE_BUNDLE:
                RestrictionEntry[] restrictions = entry.getRestrictions();
                Bundle childBundle = new Bundle();
                if (restrictions != null)
                {
                    for (RestrictionEntry child : restrictions) addRestrictionToBundle(childBundle, child);
                }
                bundle.putBundle(entry.getKey(), childBundle);
                break;
            case RestrictionEntry.TYPE_BUNDLE_ARRAY:
                RestrictionEntry[] bundleRestrictions = entry.getRestrictions();
                Bundle[] bundles = new Bundle[bundleRestrictions == null ? 0 : bundleRestrictions.length];
                for (int index = 0; index < bundles.length; index++)
                {
                    RestrictionEntry[] children = bundleRestrictions[index].getRestrictions();
                    bundles[index] = new Bundle();
                    if (children != null)
                    {
                        for (RestrictionEntry child : children) addRestrictionToBundle(bundles[index], child);
                    }
                }
                bundle.putParcelableArray(entry.getKey(), bundles);
                break;
            default:
                throw new IllegalArgumentException("Unsupported restrictionEntry type: " + entry.getType());
        }

        return bundle;
    }
}
