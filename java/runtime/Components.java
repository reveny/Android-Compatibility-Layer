package runtime;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public final class Components
{
    public static final int ACTIVITY = 0;
    public static final int SERVICE = 1;
    public static final int RECEIVER = 2;

    private static final String TAG = "Components";
    private static final int KINDS = 3;

    private static final Record[][] sRecords = new Record[KINDS][];

    public static final class Record
    {
        public final int kind;
        public final String className;
        public final IntentFilter[] filters;
        public final android.os.Bundle metaData;

        Record(int kind, String className, IntentFilter[] filters, android.os.Bundle metaData)
        {
            this.kind = kind;
            this.className = className;
            this.filters = filters;
            this.metaData = metaData;
        }
    }

    private Components()
    {
    }

    public static synchronized Record[] declared(int kind)
    {
        if (kind < 0 || kind >= KINDS) return new Record[0];
        if (sRecords[kind] != null) return sRecords[kind];

        int count = Bootstrap.nativeComponentCount(kind);
        List<Record> records = new ArrayList<>();

        for (int index = 0; index < count; index++)
        {
            String className = Bootstrap.nativeComponentClassName(kind, index);
            if (className == null || className.isEmpty()) continue;

            int filterCount = Bootstrap.nativeComponentFilterCount(kind, index);
            IntentFilter[] filters = new IntentFilter[filterCount];

            for (int at = 0; at < filterCount; at++)
            {
                IntentFilter filter = new IntentFilter();

                for (String action : split(Bootstrap.nativeFilterActions(kind, index, at))) filter.addAction(action);
                for (String category : split(Bootstrap.nativeFilterCategories(kind, index, at))) filter.addCategory(category);
                for (String scheme : split(Bootstrap.nativeFilterSchemes(kind, index, at))) filter.addDataScheme(scheme);
                for (String type : split(Bootstrap.nativeFilterTypes(kind, index, at))) filter.addDataType(type);

                filter.setPriority(Bootstrap.nativeFilterPriority(kind, index, at));
                filters[at] = filter;
            }

            int metaDataCount = Bootstrap.nativeComponentMetaDataCount(kind, index);
            android.os.Bundle metaData = null;

            if (metaDataCount > 0)
            {
                metaData = new android.os.Bundle();

                for (int entry = 0; entry < metaDataCount; entry++)
                {
                    metaData.putString(Bootstrap.nativeComponentMetaDataName(kind, index, entry),
                                       Bootstrap.nativeComponentMetaDataValue(kind, index, entry));
                }
            }

            records.add(new Record(kind, className, filters, metaData));
        }

        sRecords[kind] = records.toArray(new Record[0]);
        return sRecords[kind];
    }

    public static Record resolve(int kind, Intent intent)
    {
        List<Record> matches = resolveAll(kind, intent);
        return matches.isEmpty() ? null : matches.get(0);
    }

    public static List<Record> resolveAll(int kind, Intent intent)
    {
        List<Record> matches = new ArrayList<>();
        if (intent == null) return matches;

        Record[] records = declared(kind);
        ComponentName component = intent.getComponent();

        if (component != null)
        {
            for (Record record : records)
            {
                if (record.className.equals(component.getClassName())) matches.add(record);
            }

            if (matches.isEmpty())
            {
                Log.w(TAG, "no " + describe(kind) + " named " + component.getClassName() + " is declared in the manifest");
            }

            return matches;
        }

        int best = Integer.MIN_VALUE;

        for (Record record : records)
        {
            for (IntentFilter filter : record.filters)
            {
                int outcome = filter.match(intent.getAction(), intent.getType(), intent.getScheme(), intent.getData(),
                                           intent.getCategories(), TAG);
                if (outcome < 0) continue;

                if (outcome > best)
                {
                    best = outcome;
                    matches.add(0, record);
                }
                else
                {
                    matches.add(record);
                }

                break;
            }
        }

        if (matches.isEmpty())
        {
            Log.w(TAG, "nothing declared answers " + intent + " as a " + describe(kind));
        }

        return matches;
    }

    public static ComponentName nameOf(Record record, String packageName)
    {
        return new ComponentName(packageName, record.className);
    }

    private static String describe(int kind)
    {
        if (kind == ACTIVITY) return "activity";
        if (kind == SERVICE) return "service";
        if (kind == RECEIVER) return "receiver";

        return "component";
    }

    private static String[] split(String joined)
    {
        if (joined == null || joined.isEmpty()) return new String[0];

        return joined.split("\n");
    }
}
