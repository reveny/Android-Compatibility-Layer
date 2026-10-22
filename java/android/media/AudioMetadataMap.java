package android.media;

public interface AudioMetadataMap extends AudioMetadataReadMap
{
    <T> T remove(AudioMetadata.Key<T> key);

    <T> T set(AudioMetadata.Key<T> key, T value);
}
