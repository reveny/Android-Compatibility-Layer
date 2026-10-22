package android.content.res.loader;

import android.content.res.AssetFileDescriptor;

public interface AssetsProvider
{
    default AssetFileDescriptor loadAssetFd(String path, int accessMode)
    {
        return null;
    }
}
