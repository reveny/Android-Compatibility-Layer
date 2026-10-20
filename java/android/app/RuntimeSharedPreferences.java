package android.app;

import android.content.SharedPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class RuntimeSharedPreferences implements SharedPreferences
{
    private static final String DECLARATION = "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>";
    private static final long MAX_PREFERENCES_BYTES = 16L * 1024L * 1024L;
    private static final Map<String, RuntimeSharedPreferences> INSTANCES = new HashMap<>();

    private final File mFile;
    private final File mBackup;
    private final Map<String, Object> mValues = new HashMap<>();
    private final List<OnSharedPreferenceChangeListener> mListeners = new ArrayList<>();

    private RuntimeSharedPreferences(File file)
    {
        mFile = file;
        mBackup = new File(file.getPath() + ".bak");
        load();
    }

    static synchronized SharedPreferences forName(File directory, String name)
    {
        File file = new File(directory, name + ".xml");
        String key = file.getAbsolutePath();

        RuntimeSharedPreferences existing = INSTANCES.get(key);
        if (existing != null) return existing;

        RuntimeSharedPreferences created = new RuntimeSharedPreferences(file);
        INSTANCES.put(key, created);
        return created;
    }

    private void load()
    {
        mValues.clear();

        if (mBackup.exists())
        {
            mFile.delete();
            mBackup.renameTo(mFile);
        }

        if (!mFile.exists()) return;

        long length = mFile.length();
        if (length > MAX_PREFERENCES_BYTES)
        {
            android.util.Log.w("SharedPreferences", mFile + " is " + length + " bytes, refusing to load past "
                                                    + MAX_PREFERENCES_BYTES);
            return;
        }

        InputStream stream = null;
        try
        {
            stream = new java.io.FileInputStream(mFile);
            byte[] bytes = new byte[(int)length];

            int read = 0;
            while (read < bytes.length)
            {
                int got = stream.read(bytes, read, bytes.length - read);
                if (got < 0) break;
                read += got;
            }

            parse(new String(bytes, 0, read, "UTF-8"));
        }
        catch (Throwable unreadable)
        {
            mValues.clear();
            android.util.Log.w("SharedPreferences", "could not read " + mFile + ": " + unreadable);
        }
        finally
        {
            if (stream != null)
            {
                try
                {
                    stream.close();
                }
                catch (java.io.IOException ignored)
                {
                    android.util.Log.w("SharedPreferences", "could not close " + mFile);
                }
            }
        }
    }

    private void parse(String text)
    {
        String setName = null;
        Set<String> setValues = null;
        int cursor = 0;

        while (cursor < text.length())
        {
            int open = text.indexOf('<', cursor);
            if (open < 0) break;

            if (text.startsWith("<!--", open))
            {
                int commentEnd = text.indexOf("-->", open + 4);
                if (commentEnd < 0) break;
                cursor = commentEnd + 3;
                continue;
            }

            int close = text.indexOf('>', open);
            if (close < 0) break;

            String tag = text.substring(open + 1, close).trim();
            cursor = close + 1;

            if (tag.startsWith("?") || tag.startsWith("!")) continue;

            if (tag.startsWith("/"))
            {
                if (tag.substring(1).trim().equals("set") && setName != null)
                {
                    mValues.put(setName, setValues);
                    setName = null;
                    setValues = null;
                }
                continue;
            }

            boolean selfClosing = tag.endsWith("/");
            if (selfClosing) tag = tag.substring(0, tag.length() - 1).trim();

            int nameEnd = -1;
            for (int index = 0; index < tag.length(); index++)
            {
                if (Character.isWhitespace(tag.charAt(index)))
                {
                    nameEnd = index;
                    break;
                }
            }

            String element = nameEnd < 0 ? tag : tag.substring(0, nameEnd);
            String attributes = nameEnd < 0 ? "" : tag.substring(nameEnd + 1);

            String name = attributeOf(attributes, "name");
            String value = attributeOf(attributes, "value");

            if (element.equals("map")) continue;

            if (element.equals("set"))
            {
                if (selfClosing)
                {
                    if (name != null) mValues.put(name, new LinkedHashSet<String>());
                    continue;
                }

                setName = name;
                setValues = new LinkedHashSet<>();
                continue;
            }

            if (element.equals("string"))
            {
                String body = "";
                if (!selfClosing)
                {
                    int textEnd = text.indexOf("</string>", cursor);
                    if (textEnd < 0)
                    {
                        android.util.Log.w("SharedPreferences", "unterminated <string> in " + mFile);
                        break;
                    }

                    body = unescape(text.substring(cursor, textEnd));
                    cursor = textEnd + "</string>".length();
                }

                if (setValues != null) setValues.add(body);
                else if (name != null) mValues.put(name, body);
                continue;
            }

            if (name == null || value == null) continue;

            try
            {
                if (element.equals("int")) mValues.put(name, Integer.valueOf(value));
                else if (element.equals("long")) mValues.put(name, Long.valueOf(value));
                else if (element.equals("float")) mValues.put(name, Float.valueOf(value));
                else if (element.equals("boolean")) mValues.put(name, Boolean.valueOf(value));
            }
            catch (NumberFormatException malformed)
            {
                android.util.Log.w("SharedPreferences", "ignoring malformed " + element + " '" + name + "' in " + mFile);
            }
        }
    }

    private static String attributeOf(String attributes, String wanted)
    {
        String needle = wanted + "=\"";
        int start = attributes.indexOf(needle);
        if (start < 0) return null;

        int valueStart = start + needle.length();
        int valueEnd = attributes.indexOf('"', valueStart);
        if (valueEnd < 0) return null;

        return unescape(attributes.substring(valueStart, valueEnd));
    }

    private static String unescape(String value)
    {
        if (value.indexOf('&') < 0) return value;

        return value.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                    .replace("&apos;", "'").replace("&amp;", "&");
    }

    private synchronized boolean write()
    {
        File parent = mFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) return false;

        byte[] encoded;
        try
        {
            encoded = serialize().getBytes("UTF-8");
        }
        catch (Throwable unserialisable)
        {
            android.util.Log.w("SharedPreferences", "could not serialise " + mFile + ": " + unserialisable);
            return false;
        }

        if (mFile.exists() && !mBackup.exists() && !mFile.renameTo(mBackup)) return false;

        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream(mFile);
            out.write(encoded);
            out.getFD().sync();
            out.close();
            out = null;

            mBackup.delete();
            return true;
        }
        catch (Throwable failed)
        {
            android.util.Log.w("SharedPreferences", "could not write " + mFile + ": " + failed);
            mFile.delete();
            mBackup.renameTo(mFile);
            return false;
        }
        finally
        {
            if (out != null)
            {
                try
                {
                    out.close();
                }
                catch (IOException ignored)
                {
                    android.util.Log.w("SharedPreferences", "could not close " + mFile);
                }
            }
        }
    }

    private String serialize()
    {
        StringBuilder text = new StringBuilder();
        text.append(DECLARATION).append('\n');
        text.append("<map>").append('\n');

        List<String> keys = new ArrayList<>();
        for (String key : mValues.keySet())
        {
            if (key == null)
            {
                android.util.Log.w("SharedPreferences", "dropping a null key from " + mFile);
                continue;
            }
            keys.add(key);
        }
        Collections.sort(keys);

        for (String key : keys)
        {
            String name = escape(key);
            Object value = mValues.get(key);

            if (value instanceof String)
            {
                text.append("    <string name=\"").append(name).append("\">")
                    .append(escape((String)value)).append("</string>\n");
            }
            else if (value instanceof Integer)
            {
                text.append("    <int name=\"").append(name).append("\" value=\"").append(value).append("\" />\n");
            }
            else if (value instanceof Long)
            {
                text.append("    <long name=\"").append(name).append("\" value=\"").append(value).append("\" />\n");
            }
            else if (value instanceof Float)
            {
                text.append("    <float name=\"").append(name).append("\" value=\"").append(value).append("\" />\n");
            }
            else if (value instanceof Boolean)
            {
                text.append("    <boolean name=\"").append(name).append("\" value=\"").append(value).append("\" />\n");
            }
            else if (value instanceof Set)
            {
                text.append("    <set name=\"").append(name).append("\">\n");
                for (Object member : (Set<?>)value)
                {
                    text.append("        <string>").append(escape(String.valueOf(member))).append("</string>\n");
                }
                text.append("    </set>\n");
            }
        }

        text.append("</map>").append('\n');
        return text.toString();
    }

    private static String escape(String value)
    {
        StringBuilder out = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++)
        {
            char character = value.charAt(index);
            if (character == '&') out.append("&amp;");
            else if (character == '<') out.append("&lt;");
            else if (character == '>') out.append("&gt;");
            else if (character == '"') out.append("&quot;");
            else if (character == '\'') out.append("&apos;");
            else out.append(character);
        }
        return out.toString();
    }

    @Override
    public synchronized Map<String, ?> getAll()
    {
        return new HashMap<>(mValues);
    }

    @Override
    public synchronized String getString(String key, String defaultValue)
    {
        Object value = mValues.get(key);
        return value instanceof String ? (String)value : defaultValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized Set<String> getStringSet(String key, Set<String> defaultValues)
    {
        Object value = mValues.get(key);
        return value instanceof Set ? new LinkedHashSet<>((Set<String>)value) : defaultValues;
    }

    @Override
    public synchronized int getInt(String key, int defaultValue)
    {
        Object value = mValues.get(key);
        return value instanceof Integer ? (Integer)value : defaultValue;
    }

    @Override
    public synchronized long getLong(String key, long defaultValue)
    {
        Object value = mValues.get(key);
        return value instanceof Long ? (Long)value : defaultValue;
    }

    @Override
    public synchronized float getFloat(String key, float defaultValue)
    {
        Object value = mValues.get(key);
        return value instanceof Float ? (Float)value : defaultValue;
    }

    @Override
    public synchronized boolean getBoolean(String key, boolean defaultValue)
    {
        Object value = mValues.get(key);
        return value instanceof Boolean ? (Boolean)value : defaultValue;
    }

    @Override
    public synchronized boolean contains(String key)
    {
        return mValues.containsKey(key);
    }

    @Override
    public Editor edit()
    {
        return new EditorImpl();
    }

    @Override
    public synchronized void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener)
    {
        if (listener != null && !mListeners.contains(listener)) mListeners.add(listener);
    }

    @Override
    public synchronized void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener)
    {
        mListeners.remove(listener);
    }

    private void notifyChanged(List<String> keys)
    {
        List<OnSharedPreferenceChangeListener> listeners;
        synchronized (this)
        {
            if (mListeners.isEmpty()) return;
            listeners = new ArrayList<>(mListeners);
        }

        for (String key : keys)
        {
            for (OnSharedPreferenceChangeListener listener : listeners)
            {
                listener.onSharedPreferenceChanged(this, key);
            }
        }
    }

    private final class EditorImpl implements Editor
    {
        private final Map<String, Object> mPending = new HashMap<>();
        private final Set<String> mRemoved = new LinkedHashSet<>();
        private boolean mCleared;

        @Override
        public Editor putString(String key, String value)
        {
            return put(key, value);
        }

        @Override
        public Editor putStringSet(String key, Set<String> values)
        {
            return put(key, values == null ? null : new LinkedHashSet<>(values));
        }

        @Override
        public Editor putInt(String key, int value)
        {
            return put(key, Integer.valueOf(value));
        }

        @Override
        public Editor putLong(String key, long value)
        {
            return put(key, Long.valueOf(value));
        }

        @Override
        public Editor putFloat(String key, float value)
        {
            return put(key, Float.valueOf(value));
        }

        @Override
        public Editor putBoolean(String key, boolean value)
        {
            return put(key, Boolean.valueOf(value));
        }

        private Editor put(String key, Object value)
        {
            if (value == null) return remove(key);

            synchronized (RuntimeSharedPreferences.this)
            {
                mPending.put(key, value);
                mRemoved.remove(key);
            }
            return this;
        }

        @Override
        public Editor remove(String key)
        {
            synchronized (RuntimeSharedPreferences.this)
            {
                mRemoved.add(key);
                mPending.remove(key);
            }
            return this;
        }

        @Override
        public Editor clear()
        {
            synchronized (RuntimeSharedPreferences.this)
            {
                mCleared = true;
            }
            return this;
        }

        @Override
        public boolean commit()
        {
            List<String> changed = applyToMap();
            boolean written = write();
            notifyChanged(changed);
            return written;
        }

        @Override
        public void apply()
        {
            commit();
        }

        private List<String> applyToMap()
        {
            List<String> changed = new ArrayList<>();

            synchronized (RuntimeSharedPreferences.this)
            {
                if (mCleared)
                {
                    changed.addAll(mValues.keySet());
                    mValues.clear();
                }

                for (String key : mRemoved)
                {
                    if (mValues.remove(key) != null) changed.add(key);
                }

                for (Map.Entry<String, Object> entry : mPending.entrySet())
                {
                    Object previous = mValues.put(entry.getKey(), entry.getValue());
                    if (previous == null || !previous.equals(entry.getValue())) changed.add(entry.getKey());
                }

                mPending.clear();
                mRemoved.clear();
                mCleared = false;
            }

            return Collections.unmodifiableList(changed);
        }
    }
}
