package android.text.style;

import android.os.Parcel;
import android.os.PersistableBundle;
import android.text.ParcelableSpan;
import android.text.TextUtils;

public class TtsSpan implements ParcelableSpan
{
    public static final String TYPE_TEXT = "android.type.text";
    public static final String TYPE_CARDINAL = "android.type.cardinal";
    public static final String TYPE_ORDINAL = "android.type.ordinal";
    public static final String TYPE_DECIMAL = "android.type.decimal";
    public static final String TYPE_FRACTION = "android.type.fraction";
    public static final String TYPE_MEASURE = "android.type.measure";
    public static final String TYPE_TIME = "android.type.time";
    public static final String TYPE_DATE = "android.type.date";
    public static final String TYPE_TELEPHONE = "android.type.telephone";
    public static final String TYPE_ELECTRONIC = "android.type.electronic";
    public static final String TYPE_MONEY = "android.type.money";
    public static final String TYPE_DIGITS = "android.type.digits";
    public static final String TYPE_VERBATIM = "android.type.verbatim";

    public static final String ARG_GENDER = "android.arg.gender";
    public static final String GENDER_NEUTRAL = "android.neutral";
    public static final String GENDER_MALE = "android.male";
    public static final String GENDER_FEMALE = "android.female";

    public static final String ARG_ANIMACY = "android.arg.animacy";
    public static final String ANIMACY_ANIMATE = "android.animate";
    public static final String ANIMACY_INANIMATE = "android.inanimate";

    public static final String ARG_MULTIPLICITY = "android.arg.multiplicity";
    public static final String MULTIPLICITY_SINGLE = "android.single";
    public static final String MULTIPLICITY_DUAL = "android.dual";
    public static final String MULTIPLICITY_PLURAL = "android.plural";

    public static final String ARG_CASE = "android.arg.case";
    public static final String CASE_NOMINATIVE = "android.nominative";
    public static final String CASE_ACCUSATIVE = "android.accusative";
    public static final String CASE_DATIVE = "android.dative";
    public static final String CASE_ABLATIVE = "android.ablative";
    public static final String CASE_GENITIVE = "android.genitive";
    public static final String CASE_VOCATIVE = "android.vocative";
    public static final String CASE_LOCATIVE = "android.locative";
    public static final String CASE_INSTRUMENTAL = "android.instrumental";

    public static final String ARG_TEXT = "android.arg.text";
    public static final String ARG_NUMBER = "android.arg.number";
    public static final String ARG_INTEGER_PART = "android.arg.integer_part";
    public static final String ARG_FRACTIONAL_PART = "android.arg.fractional_part";
    public static final String ARG_NUMERATOR = "android.arg.numerator";
    public static final String ARG_DENOMINATOR = "android.arg.denominator";
    public static final String ARG_UNIT = "android.arg.unit";
    public static final String ARG_HOURS = "android.arg.hours";
    public static final String ARG_MINUTES = "android.arg.minutes";
    public static final String ARG_WEEKDAY = "android.arg.weekday";
    public static final String ARG_DAY = "android.arg.day";
    public static final String ARG_MONTH = "android.arg.month";
    public static final String ARG_YEAR = "android.arg.year";
    public static final String ARG_COUNTRY_CODE = "android.arg.country_code";
    public static final String ARG_NUMBER_PARTS = "android.arg.number_parts";
    public static final String ARG_EXTENSION = "android.arg.extension";
    public static final String ARG_PROTOCOL = "android.arg.protocol";
    public static final String ARG_USERNAME = "android.arg.username";
    public static final String ARG_PASSWORD = "android.arg.password";
    public static final String ARG_DOMAIN = "android.arg.domain";
    public static final String ARG_PORT = "android.arg.port";
    public static final String ARG_PATH = "android.arg.path";
    public static final String ARG_QUERY_STRING = "android.arg.query_string";
    public static final String ARG_FRAGMENT_ID = "android.arg.fragment_id";
    public static final String ARG_CURRENCY = "android.arg.money";
    public static final String ARG_DIGITS = "android.arg.digits";
    public static final String ARG_VERBATIM = "android.arg.verbatim";
    public static final String ARG_QUANTITY = "android.arg.quantity";

    public static final int WEEKDAY_SUNDAY = 1;
    public static final int WEEKDAY_MONDAY = 2;
    public static final int WEEKDAY_TUESDAY = 3;
    public static final int WEEKDAY_WEDNESDAY = 4;
    public static final int WEEKDAY_THURSDAY = 5;
    public static final int WEEKDAY_FRIDAY = 6;
    public static final int WEEKDAY_SATURDAY = 7;

    public static final int MONTH_JANUARY = 0;
    public static final int MONTH_FEBRUARY = 1;
    public static final int MONTH_MARCH = 2;
    public static final int MONTH_APRIL = 3;
    public static final int MONTH_MAY = 4;
    public static final int MONTH_JUNE = 5;
    public static final int MONTH_JULY = 6;
    public static final int MONTH_AUGUST = 7;
    public static final int MONTH_SEPTEMBER = 8;
    public static final int MONTH_OCTOBER = 9;
    public static final int MONTH_NOVEMBER = 10;
    public static final int MONTH_DECEMBER = 11;

    private final String mType;
    private final PersistableBundle mArgs;

    public TtsSpan(String type, PersistableBundle args)
    {
        mType = type;
        mArgs = args;
    }

    public TtsSpan(Parcel source)
    {
        mType = source.readString();
        Object args = source.readValue(null);
        mArgs = args instanceof PersistableBundle ? (PersistableBundle)args : new PersistableBundle();
    }

    public String getType()
    {
        return mType;
    }

    public PersistableBundle getArgs()
    {
        return mArgs;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mType);
        dest.writeValue(mArgs);
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.TTS_SPAN;
    }

    public static class Builder<C extends Builder<?>>
    {
        private final String mType;
        private final PersistableBundle mArgs = new PersistableBundle();

        public Builder(String type)
        {
            mType = type;
        }

        public TtsSpan build()
        {
            return new TtsSpan(mType, mArgs);
        }

        @SuppressWarnings("unchecked")
        public C setStringArgument(String arg, String value)
        {
            mArgs.putString(arg, value);
            return (C)this;
        }

        @SuppressWarnings("unchecked")
        public C setIntArgument(String arg, int value)
        {
            mArgs.putInt(arg, value);
            return (C)this;
        }

        @SuppressWarnings("unchecked")
        public C setLongArgument(String arg, long value)
        {
            mArgs.putLong(arg, value);
            return (C)this;
        }
    }

    public static class SemioticClassBuilder<C extends SemioticClassBuilder<?>> extends Builder<C>
    {
        public SemioticClassBuilder(String type)
        {
            super(type);
        }

        public C setGender(String gender)
        {
            return setStringArgument(ARG_GENDER, gender);
        }

        public C setAnimacy(String animacy)
        {
            return setStringArgument(ARG_ANIMACY, animacy);
        }

        public C setMultiplicity(String multiplicity)
        {
            return setStringArgument(ARG_MULTIPLICITY, multiplicity);
        }

        public C setCase(String grammaticalCase)
        {
            return setStringArgument(ARG_CASE, grammaticalCase);
        }
    }

    public static class TextBuilder extends SemioticClassBuilder<TextBuilder>
    {
        public TextBuilder()
        {
            super(TYPE_TEXT);
        }

        public TextBuilder(String text)
        {
            this();
            setText(text);
        }

        public TextBuilder setText(String text)
        {
            return setStringArgument(ARG_TEXT, text);
        }
    }

    public static class CardinalBuilder extends SemioticClassBuilder<CardinalBuilder>
    {
        public CardinalBuilder()
        {
            super(TYPE_CARDINAL);
        }

        public CardinalBuilder(long number)
        {
            this();
            setNumber(number);
        }

        public CardinalBuilder(String number)
        {
            this();
            setNumber(number);
        }

        public CardinalBuilder setNumber(long number)
        {
            return setNumber(String.valueOf(number));
        }

        public CardinalBuilder setNumber(String number)
        {
            return setStringArgument(ARG_NUMBER, number);
        }
    }

    public static class DigitsBuilder extends SemioticClassBuilder<DigitsBuilder>
    {
        public DigitsBuilder()
        {
            super(TYPE_DIGITS);
        }

        public DigitsBuilder(String digits)
        {
            this();
            setDigits(digits);
        }

        public DigitsBuilder setDigits(String digits)
        {
            return setStringArgument(ARG_DIGITS, digits);
        }
    }

    public static class VerbatimBuilder extends SemioticClassBuilder<VerbatimBuilder>
    {
        public VerbatimBuilder()
        {
            super(TYPE_VERBATIM);
        }

        public VerbatimBuilder(String verbatim)
        {
            this();
            setVerbatim(verbatim);
        }

        public VerbatimBuilder setVerbatim(String verbatim)
        {
            return setStringArgument(ARG_VERBATIM, verbatim);
        }
    }
}
