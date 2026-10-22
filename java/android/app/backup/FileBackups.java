package android.app.backup;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

final class FileBackups
{
    private static final String TAG = "FileBackups";
    private static final int STATE_MAGIC = 0x46696c65;

    private FileBackups()
    {
    }

    static void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState, File[] files,
                              String[] keys)
    {
        Map<String, long[]> previous = readState(oldState);
        Map<String, long[]> current = new HashMap<>();

        for (int index = 0; index < files.length; index++)
        {
            File file = files[index];
            String key = keys[index];
            if (!file.isFile()) continue;

            long[] stamp = new long[] {file.length(), file.lastModified()};
            current.put(key, stamp);

            long[] old = previous.get(key);
            if (old != null && old[0] == stamp[0] && old[1] == stamp[1]) continue;

            try
            {
                byte[] bytes = Files.readAllBytes(file.toPath());
                data.writeEntityHeader(key, bytes.length);
                data.writeEntityData(bytes, bytes.length);
            }
            catch (IOException failure)
            {
                Log.w(TAG, "could not back up " + file, failure);
            }
        }

        for (String key : previous.keySet())
        {
            if (current.containsKey(key)) continue;

            try
            {
                data.writeEntityHeader(key, -1);
            }
            catch (IOException failure)
            {
                Log.w(TAG, "could not record the deletion of " + key, failure);
            }
        }

        writeState(newState, current);
    }

    static void restoreEntity(BackupDataInputStream data, File destination)
    {
        if (data.size() < 0)
        {
            destination.delete();
            return;
        }

        File parent = destination.getParentFile();
        if (parent != null) parent.mkdirs();

        try (FileOutputStream output = new FileOutputStream(destination))
        {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = data.read(buffer, 0, buffer.length)) > 0) output.write(buffer, 0, read);
        }
        catch (IOException failure)
        {
            Log.w(TAG, "could not restore " + destination, failure);
        }
    }

    static void writeNewStateDescription(ParcelFileDescriptor newState, File[] files, String[] keys)
    {
        Map<String, long[]> current = new HashMap<>();
        for (int index = 0; index < files.length; index++)
        {
            if (files[index].isFile()) current.put(keys[index], new long[] {files[index].length(), files[index].lastModified()});
        }

        writeState(newState, current);
    }

    private static Map<String, long[]> readState(ParcelFileDescriptor state)
    {
        Map<String, long[]> result = new HashMap<>();
        if (state == null) return result;

        try
        {
            InputStream stream = new FileInputStream(state.getFileDescriptor());
            DataInputStream input = new DataInputStream(stream);
            if (input.readInt() != STATE_MAGIC) return result;

            int count = input.readInt();
            for (int index = 0; index < count; index++)
            {
                String key = input.readUTF();
                long size = input.readLong();
                result.put(key, new long[] {size, input.readLong()});
            }
        }
        catch (IOException unreadable)
        {
            result.clear();
        }

        return result;
    }

    private static void writeState(ParcelFileDescriptor state, Map<String, long[]> stamps)
    {
        if (state == null) return;

        try
        {
            DataOutputStream output = new DataOutputStream(new FileOutputStream(state.getFileDescriptor()));
            output.writeInt(STATE_MAGIC);
            output.writeInt(stamps.size());
            for (Map.Entry<String, long[]> entry : stamps.entrySet())
            {
                output.writeUTF(entry.getKey());
                output.writeLong(entry.getValue()[0]);
                output.writeLong(entry.getValue()[1]);
            }
            output.flush();
        }
        catch (IOException failure)
        {
            Log.w(TAG, "could not write the backup state", failure);
        }
    }
}
