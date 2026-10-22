package android.media;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class AudioPresentation implements Parcelable
{
    public static final int CONTENT_COMMENTARY = 5;
    public static final int CONTENT_DIALOG = 4;
    public static final int CONTENT_EMERGENCY = 6;
    public static final int CONTENT_HEARING_IMPAIRED = 3;
    public static final int CONTENT_MAIN = 0;
    public static final int CONTENT_MUSIC_AND_EFFECTS = 1;
    public static final int CONTENT_UNKNOWN = -1;
    public static final int CONTENT_VISUALLY_IMPAIRED = 2;
    public static final int CONTENT_VOICEOVER = 7;
    public static final int MASTERED_FOR_3D = 3;
    public static final int MASTERED_FOR_HEADPHONE = 4;
    public static final int MASTERED_FOR_STEREO = 1;
    public static final int MASTERED_FOR_SURROUND = 2;
    public static final int MASTERING_NOT_INDICATED = 0;
    public static final int PRESENTATION_ID_UNKNOWN = -1;
    public static final int PROGRAM_ID_UNKNOWN = -1;

    public static final Parcelable.Creator<AudioPresentation> CREATOR = new Parcelable.Creator<AudioPresentation>()
    {
        @Override
        public AudioPresentation createFromParcel(Parcel source)
        {
            int presentationId = source.readInt();
            int programId = source.readInt();
            String languageTag = source.readString();
            Locale locale = languageTag == null ? null : Locale.forLanguageTag(languageTag);
            int masteringIndication = source.readInt();
            boolean audioDescription = source.readBoolean();
            boolean spokenSubtitles = source.readBoolean();
            boolean dialogueEnhancement = source.readBoolean();
            int labelCount = source.readInt();
            Map<Locale, String> labels = new HashMap<>();

            for (int index = 0; index < labelCount; index++)
            {
                Locale labelLocale = Locale.forLanguageTag(source.readString());
                labels.put(labelLocale, source.readString());
            }

            return new AudioPresentation(presentationId, programId, locale, masteringIndication, audioDescription,
                spokenSubtitles, dialogueEnhancement, labels);
        }

        @Override
        public AudioPresentation[] newArray(int size)
        {
            return new AudioPresentation[size];
        }
    };

    private final int mPresentationId;
    private final int mProgramId;
    private final Locale mLocale;
    private final int mMasteringIndication;
    private final boolean mAudioDescriptionAvailable;
    private final boolean mSpokenSubtitlesAvailable;
    private final boolean mDialogueEnhancementAvailable;
    private final Map<Locale, String> mLabels;

    private AudioPresentation(int presentationId, int programId, Locale locale, int masteringIndication,
        boolean audioDescriptionAvailable, boolean spokenSubtitlesAvailable, boolean dialogueEnhancementAvailable,
        Map<Locale, String> labels)
    {
        mPresentationId = presentationId;
        mProgramId = programId;
        mLocale = locale;
        mMasteringIndication = masteringIndication;
        mAudioDescriptionAvailable = audioDescriptionAvailable;
        mSpokenSubtitlesAvailable = spokenSubtitlesAvailable;
        mDialogueEnhancementAvailable = dialogueEnhancementAvailable;
        mLabels = labels;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) return true;
        if (!(object instanceof AudioPresentation)) return false;

        AudioPresentation other = (AudioPresentation)object;
        return mPresentationId == other.mPresentationId && mProgramId == other.mProgramId
            && Objects.equals(mLocale, other.mLocale) && mMasteringIndication == other.mMasteringIndication
            && mAudioDescriptionAvailable == other.mAudioDescriptionAvailable
            && mSpokenSubtitlesAvailable == other.mSpokenSubtitlesAvailable
            && mDialogueEnhancementAvailable == other.mDialogueEnhancementAvailable && mLabels.equals(other.mLabels);
    }

    public Map<Locale, String> getLabels()
    {
        return new HashMap<>(mLabels);
    }

    public Locale getLocale()
    {
        return mLocale;
    }

    public int getMasteringIndication()
    {
        return mMasteringIndication;
    }

    public int getPresentationId()
    {
        return mPresentationId;
    }

    public int getProgramId()
    {
        return mProgramId;
    }

    public boolean hasAudioDescription()
    {
        return mAudioDescriptionAvailable;
    }

    public boolean hasDialogueEnhancement()
    {
        return mDialogueEnhancementAvailable;
    }

    public boolean hasSpokenSubtitles()
    {
        return mSpokenSubtitlesAvailable;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mPresentationId, mProgramId, mLocale, mMasteringIndication, mAudioDescriptionAvailable,
            mSpokenSubtitlesAvailable, mDialogueEnhancementAvailable, mLabels);
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + " { presentation id=" + mPresentationId + ", program id=" + mProgramId
            + ", language=" + mLocale + ", labels=" + mLabels + ", mastering indication=" + mMasteringIndication
            + ", audio description=" + mAudioDescriptionAvailable + ", spoken subtitles=" + mSpokenSubtitlesAvailable
            + ", dialogue enhancement=" + mDialogueEnhancementAvailable + " }";
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeInt(mPresentationId);
        parcel.writeInt(mProgramId);
        parcel.writeString(mLocale == null ? null : mLocale.toLanguageTag());
        parcel.writeInt(mMasteringIndication);
        parcel.writeBoolean(mAudioDescriptionAvailable);
        parcel.writeBoolean(mSpokenSubtitlesAvailable);
        parcel.writeBoolean(mDialogueEnhancementAvailable);
        parcel.writeInt(mLabels.size());

        for (Map.Entry<Locale, String> entry : mLabels.entrySet())
        {
            parcel.writeString(entry.getKey().toLanguageTag());
            parcel.writeString(entry.getValue());
        }
    }

    public static final class Builder
    {
        private final int mPresentationId;
        private int mProgramId = PROGRAM_ID_UNKNOWN;
        private int mMasteringIndication = MASTERING_NOT_INDICATED;
        private boolean mAudioDescriptionAvailable;
        private boolean mSpokenSubtitlesAvailable;
        private boolean mDialogueEnhancementAvailable;

        public Builder(int presentationId)
        {
            mPresentationId = presentationId;
        }

        public AudioPresentation build()
        {
            return new AudioPresentation(mPresentationId, mProgramId, null, mMasteringIndication, mAudioDescriptionAvailable,
                mSpokenSubtitlesAvailable, mDialogueEnhancementAvailable, new HashMap<>());
        }

        public Builder setHasAudioDescription(boolean audioDescriptionAvailable)
        {
            mAudioDescriptionAvailable = audioDescriptionAvailable;
            return this;
        }

        public Builder setHasDialogueEnhancement(boolean dialogueEnhancementAvailable)
        {
            mDialogueEnhancementAvailable = dialogueEnhancementAvailable;
            return this;
        }

        public Builder setHasSpokenSubtitles(boolean spokenSubtitlesAvailable)
        {
            mSpokenSubtitlesAvailable = spokenSubtitlesAvailable;
            return this;
        }

        public Builder setMasteringIndication(int masteringIndication)
        {
            if (masteringIndication < MASTERING_NOT_INDICATED || masteringIndication > MASTERED_FOR_HEADPHONE)
            {
                throw new IllegalArgumentException("Unknown mastering indication: " + masteringIndication);
            }

            mMasteringIndication = masteringIndication;
            return this;
        }

        public Builder setProgramId(int programId)
        {
            mProgramId = programId;
            return this;
        }
    }
}
