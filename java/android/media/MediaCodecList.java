package android.media;

public final class MediaCodecList
{
    public static final int ALL_CODECS = 1;
    public static final int REGULAR_CODECS = 0;

    private static final MediaCodecInfo[] CODEC_INFOS = new MediaCodecInfo[0];

    private final int mKind;

    public MediaCodecList(int kind)
    {
        mKind = kind;
    }

    public String findDecoderForFormat(MediaFormat format)
    {
        return findCodecForFormat(false, format);
    }

    public String findEncoderForFormat(MediaFormat format)
    {
        return findCodecForFormat(true, format);
    }

    public static int getCodecCount()
    {
        return CODEC_INFOS.length;
    }

    public static MediaCodecInfo getCodecInfoAt(int index)
    {
        if (index < 0 || index >= CODEC_INFOS.length) throw new IllegalArgumentException();

        return CODEC_INFOS[index];
    }

    public MediaCodecInfo[] getCodecInfos()
    {
        return CODEC_INFOS.clone();
    }

    private String findCodecForFormat(boolean encoder, MediaFormat format)
    {
        String mime = format.getString(MediaFormat.KEY_MIME);
        if (mime == null) return null;

        for (MediaCodecInfo info : CODEC_INFOS)
        {
            if (info.isEncoder() != encoder) continue;

            for (String type : info.getSupportedTypes())
            {
                if (type.equalsIgnoreCase(mime) && info.getCapabilitiesForType(type).isFormatSupported(format))
                {
                    return info.getName();
                }
            }
        }

        return null;
    }
}
