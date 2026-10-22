package android.telephony;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.regex.Pattern;

public class SmsMessage
{
    public static final int ENCODING_UNKNOWN = 0;
    public static final int ENCODING_7BIT = 1;
    public static final int ENCODING_8BIT = 2;
    public static final int ENCODING_16BIT = 3;
    public static final int ENCODING_KSC5601 = 4;

    public static final String FORMAT_3GPP = "3gpp";
    public static final String FORMAT_3GPP2 = "3gpp2";

    public static final int MAX_USER_DATA_BYTES = 140;
    public static final int MAX_USER_DATA_BYTES_WITH_HEADER = 134;
    public static final int MAX_USER_DATA_SEPTETS = 160;
    public static final int MAX_USER_DATA_SEPTETS_WITH_HEADER = 153;

    private static final int MAX_USER_DATA_UNITS_16BIT = 70;
    private static final int MAX_USER_DATA_UNITS_16BIT_WITH_HEADER = 67;

    private static final String GSM_BASIC_TABLE = "@£$¥èéùìòÇ\nØø\rÅå"
        + "Δ_ΦΓΛΩΠΨΣΘΞ\u001BÆæßÉ"
        + " !\"#¤%&'()*+,-./0123456789:;<=>?"
        + "¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§"
        + "¿abcdefghijklmnopqrstuvwxyzäöñüà";

    private static final int GSM_ESCAPE = 0x1B;
    private static final char[] GSM_EXTENSION_CHARS = {'\f', '^', '{', '}', '\\', '[', '~', ']', '|', '€'};
    private static final int[] GSM_EXTENSION_CODES = {0x0A, 0x14, 0x28, 0x29, 0x2F, 0x3C, 0x3D, 0x3E, 0x40, 0x65};

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9\\+\\.\\_\\%\\-]{1,256}\\@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}"
        + "(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+");

    public enum MessageClass
    {
        UNKNOWN,
        CLASS_0,
        CLASS_1,
        CLASS_2,
        CLASS_3
    }

    public static class SubmitPdu
    {
        public byte[] encodedScAddress;
        public byte[] encodedMessage;

        SubmitPdu()
        {
        }

        @Override
        public String toString()
        {
            return "SubmitPdu: encodedScAddress = " + Arrays.toString(encodedScAddress) + ", encodedMessage = "
                + Arrays.toString(encodedMessage);
        }
    }

    private byte[] mPdu;
    private String mScAddress;
    private String mOriginatingAddress;
    private String mRecipientAddress;
    private String mMessageBody;
    private byte[] mUserData;
    private int mProtocolIdentifier;
    private int mDataCodingScheme;
    private long mTimestampMillis;
    private boolean mReplyPathPresent;
    private boolean mIsStatusReport;
    private int mStatus;
    private MessageClass mMessageClass = MessageClass.UNKNOWN;
    private boolean mIsMwi;
    private boolean mMwiSense;
    private boolean mMwiDontStore;
    private boolean mIsCphsMwi;
    private boolean mCphsMwiSet;
    private boolean mIsEmail;
    private String mEmailFrom;
    private String mEmailBody;
    private int mIndexOnIcc = -1;
    private int mStatusOnIcc = -1;

    private SmsMessage()
    {
    }

    @Deprecated
    public static SmsMessage createFromPdu(byte[] pdu)
    {
        return createFromPdu(pdu, FORMAT_3GPP);
    }

    public static SmsMessage createFromPdu(byte[] pdu, String format)
    {
        if (pdu == null || !FORMAT_3GPP.equals(format)) return null;
        try
        {
            SmsMessage message = new SmsMessage();
            message.parsePdu(pdu);
            return message;
        }
        catch (RuntimeException exception)
        {
            return null;
        }
    }

    public static int getTPLayerLengthForPDU(String pdu)
    {
        int length = pdu.length() / 2;
        int smscLength = Integer.parseInt(pdu.substring(0, 2), 16);
        return length - smscLength - 1;
    }

    public static int[] calculateLength(CharSequence messageBody, boolean use7bitOnly)
    {
        int septets = countGsmSeptets(messageBody, use7bitOnly);
        int[] result = new int[4];
        if (septets >= 0)
        {
            result[1] = septets;
            if (septets > MAX_USER_DATA_SEPTETS)
            {
                result[0] = (septets + MAX_USER_DATA_SEPTETS_WITH_HEADER - 1) / MAX_USER_DATA_SEPTETS_WITH_HEADER;
                result[2] = result[0] * MAX_USER_DATA_SEPTETS_WITH_HEADER - septets;
            }
            else
            {
                result[0] = 1;
                result[2] = MAX_USER_DATA_SEPTETS - septets;
            }
            result[3] = ENCODING_7BIT;
            return result;
        }
        int units = messageBody.length();
        result[1] = units;
        if (units > MAX_USER_DATA_UNITS_16BIT)
        {
            result[0] = (units + MAX_USER_DATA_UNITS_16BIT_WITH_HEADER - 1) / MAX_USER_DATA_UNITS_16BIT_WITH_HEADER;
            result[2] = result[0] * MAX_USER_DATA_UNITS_16BIT_WITH_HEADER - units;
        }
        else
        {
            result[0] = 1;
            result[2] = MAX_USER_DATA_UNITS_16BIT - units;
        }
        result[3] = ENCODING_16BIT;
        return result;
    }

    public static int[] calculateLength(String messageBody, boolean use7bitOnly)
    {
        return calculateLength((CharSequence)messageBody, use7bitOnly);
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, String message, boolean statusReportRequested)
    {
        if (message == null || destinationAddress == null) return null;
        ByteArrayOutputStream stream = startSubmitPdu(destinationAddress, statusReportRequested, false);
        if (stream == null) return null;
        byte[] septets = encodeGsm7Bit(message);
        if (septets != null)
        {
            if (septets.length > MAX_USER_DATA_SEPTETS) return null;
            stream.write(0x00);
            stream.write(0x00);
            stream.write(septets.length);
            byte[] packed = packSeptets(septets);
            stream.write(packed, 0, packed.length);
        }
        else
        {
            byte[] ucs2 = message.getBytes(StandardCharsets.UTF_16BE);
            if (ucs2.length > MAX_USER_DATA_BYTES) return null;
            stream.write(0x00);
            stream.write(0x08);
            stream.write(ucs2.length);
            stream.write(ucs2, 0, ucs2.length);
        }
        return finishSubmitPdu(scAddress, stream);
    }

    public static SubmitPdu getSubmitPdu(String scAddress, String destinationAddress, short destinationPort, byte[] data,
        boolean statusReportRequested)
    {
        if (data == null || destinationAddress == null) return null;
        byte[] header = {0x05, 0x04, (byte)((destinationPort >> 8) & 0xFF), (byte)(destinationPort & 0xFF), 0x00, 0x00};
        if (data.length + header.length + 1 > MAX_USER_DATA_BYTES) return null;
        ByteArrayOutputStream stream = startSubmitPdu(destinationAddress, statusReportRequested, true);
        if (stream == null) return null;
        stream.write(0x00);
        stream.write(0x04);
        stream.write(data.length + header.length + 1);
        stream.write(header.length);
        stream.write(header, 0, header.length);
        stream.write(data, 0, data.length);
        return finishSubmitPdu(scAddress, stream);
    }

    private static ByteArrayOutputStream startSubmitPdu(String destinationAddress, boolean statusReportRequested, boolean hasHeader)
    {
        byte[] destination = PhoneNumberUtils.networkPortionToCalledPartyBCD(destinationAddress);
        if (destination == null) return null;
        ByteArrayOutputStream stream = new ByteArrayOutputStream(MAX_USER_DATA_BYTES + 40);
        int firstByte = 0x01;
        if (hasHeader) firstByte |= 0x40;
        if (statusReportRequested) firstByte |= 0x20;
        stream.write(firstByte);
        stream.write(0x00);
        int semiOctets = (destination.length - 1) * 2 - ((destination[destination.length - 1] & 0xF0) == 0xF0 ? 1 : 0);
        stream.write(semiOctets);
        stream.write(destination, 0, destination.length);
        return stream;
    }

    private static SubmitPdu finishSubmitPdu(String scAddress, ByteArrayOutputStream stream)
    {
        SubmitPdu result = new SubmitPdu();
        result.encodedScAddress = scAddress == null ? null : PhoneNumberUtils.networkPortionToCalledPartyBCDWithLength(scAddress);
        result.encodedMessage = stream.toByteArray();
        return result;
    }

    private static int gsmCode(char c)
    {
        return GSM_BASIC_TABLE.indexOf(c);
    }

    private static int gsmExtensionCode(char c)
    {
        for (int index = 0; index < GSM_EXTENSION_CHARS.length; index++)
        {
            if (GSM_EXTENSION_CHARS[index] == c) return GSM_EXTENSION_CODES[index];
        }
        return -1;
    }

    private static int countGsmSeptets(CharSequence text, boolean use7bitOnly)
    {
        int count = 0;
        for (int index = 0; index < text.length(); index++)
        {
            char c = text.charAt(index);
            if (c != GSM_ESCAPE && gsmCode(c) >= 0)
            {
                count++;
            }
            else if (gsmExtensionCode(c) >= 0)
            {
                count += 2;
            }
            else if (use7bitOnly)
            {
                count++;
            }
            else
            {
                return -1;
            }
        }
        return count;
    }

    private static byte[] encodeGsm7Bit(String text)
    {
        ByteArrayOutputStream septets = new ByteArrayOutputStream(text.length());
        for (int index = 0; index < text.length(); index++)
        {
            char c = text.charAt(index);
            int code = c == GSM_ESCAPE ? -1 : gsmCode(c);
            if (code >= 0)
            {
                septets.write(code);
                continue;
            }
            int extension = gsmExtensionCode(c);
            if (extension < 0) return null;
            septets.write(GSM_ESCAPE);
            septets.write(extension);
        }
        return septets.toByteArray();
    }

    private static byte[] packSeptets(byte[] septets)
    {
        byte[] packed = new byte[(septets.length * 7 + 7) / 8];
        int bitOffset = 0;
        for (byte septet : septets)
        {
            int value = septet & 0x7F;
            int byteIndex = bitOffset / 8;
            int shift = bitOffset % 8;
            packed[byteIndex] |= (byte)(value << shift);
            if (shift > 1) packed[byteIndex + 1] |= (byte)(value >> (8 - shift));
            bitOffset += 7;
        }
        return packed;
    }

    private static String unpackGsm7Bit(byte[] pdu, int offset, int septetCount, int fillBits)
    {
        StringBuilder result = new StringBuilder(septetCount);
        boolean escaped = false;
        for (int index = 0; index < septetCount; index++)
        {
            int bitOffset = 7 * index + fillBits;
            int byteIndex = offset + bitOffset / 8;
            int shift = bitOffset % 8;
            int value = ((pdu[byteIndex] & 0xFF) >> shift) & 0x7F;
            if (shift > 1 && byteIndex + 1 < pdu.length) value = (value | ((pdu[byteIndex + 1] & 0xFF) << (8 - shift))) & 0x7F;
            if (escaped)
            {
                char extension = ' ';
                for (int code = 0; code < GSM_EXTENSION_CODES.length; code++)
                {
                    if (GSM_EXTENSION_CODES[code] == value) extension = GSM_EXTENSION_CHARS[code];
                }
                result.append(extension);
                escaped = false;
            }
            else if (value == GSM_ESCAPE)
            {
                escaped = true;
            }
            else
            {
                result.append(GSM_BASIC_TABLE.charAt(value));
            }
        }
        return result.toString();
    }

    private static int gsmBcdByteToInt(byte b)
    {
        int result = 0;
        if ((b & 0xF0) <= 0x90) result = (b >> 4) & 0xF;
        if ((b & 0x0F) <= 0x09) result += (b & 0xF) * 10;
        return result;
    }

    private void parsePdu(byte[] pdu)
    {
        mPdu = pdu;
        int position = 0;
        int scLength = pdu[position++] & 0xFF;
        if (scLength > 0)
        {
            mScAddress = PhoneNumberUtils.calledPartyBCDToString(pdu, position, scLength);
            position += scLength;
        }
        int firstByte = pdu[position++] & 0xFF;
        int messageType = firstByte & 0x3;
        switch (messageType)
        {
            case 0:
            case 3:
                parseDeliver(pdu, position, firstByte);
                break;
            case 1:
                parseSubmit(pdu, position, firstByte);
                break;
            case 2:
                parseStatusReport(pdu, position, firstByte);
                break;
            default:
                throw new IllegalStateException("Unsupported message type");
        }
    }

    private int parseOriginatingAddress(byte[] pdu, int position, boolean recipient)
    {
        int semiOctets = pdu[position] & 0xFF;
        int typeOfAddress = pdu[position + 1] & 0xFF;
        int byteCount = (semiOctets + 1) / 2;
        String address;
        if ((typeOfAddress & 0x70) == 0x50)
        {
            address = unpackGsm7Bit(pdu, position + 2, semiOctets * 4 / 7, 0);
            if (semiOctets == 4 && !recipient)
            {
                int first = pdu[position + 2] & 0xFF;
                int second = pdu[position + 3] & 0xFF;
                mIsCphsMwi = (first & 0x7E) == 0x10 && second == 0x00;
                mCphsMwiSet = mIsCphsMwi && (first & 0x01) == 0x01;
            }
        }
        else
        {
            String digits = PhoneNumberUtils.calledPartyBCDFragmentToString(pdu, position + 2, byteCount);
            if (digits.length() > semiOctets) digits = digits.substring(0, semiOctets);
            address = (typeOfAddress & 0x70) == 0x10 ? "+" + digits : digits;
        }
        if (recipient)
        {
            mRecipientAddress = address;
        }
        else
        {
            mOriginatingAddress = address;
        }
        return position + 2 + byteCount;
    }

    private long parseTimestamp(byte[] pdu, int position)
    {
        int year = gsmBcdByteToInt(pdu[position]);
        int month = gsmBcdByteToInt(pdu[position + 1]);
        int day = gsmBcdByteToInt(pdu[position + 2]);
        int hour = gsmBcdByteToInt(pdu[position + 3]);
        int minute = gsmBcdByteToInt(pdu[position + 4]);
        int second = gsmBcdByteToInt(pdu[position + 5]);
        byte zone = pdu[position + 6];
        int quarters = gsmBcdByteToInt((byte)(zone & ~0x08));
        if ((zone & 0x08) != 0) quarters = -quarters;
        try
        {
            LocalDateTime time = LocalDateTime.of(year >= 90 ? 1900 + year : 2000 + year, month, day, hour, minute, second);
            return time.toInstant(ZoneOffset.UTC).toEpochMilli() - quarters * 15L * 60L * 1000L;
        }
        catch (RuntimeException exception)
        {
            return 0;
        }
    }

    private void parseDeliver(byte[] pdu, int position, int firstByte)
    {
        mReplyPathPresent = (firstByte & 0x80) != 0;
        position = parseOriginatingAddress(pdu, position, false);
        mProtocolIdentifier = pdu[position++] & 0xFF;
        mDataCodingScheme = pdu[position++] & 0xFF;
        mTimestampMillis = parseTimestamp(pdu, position);
        position += 7;
        parseUserData(pdu, position, (firstByte & 0x40) != 0);
    }

    private void parseSubmit(byte[] pdu, int position, int firstByte)
    {
        position++;
        position = parseOriginatingAddress(pdu, position, true);
        mProtocolIdentifier = pdu[position++] & 0xFF;
        mDataCodingScheme = pdu[position++] & 0xFF;
        int validityFormat = (firstByte >> 3) & 0x3;
        if (validityFormat == 2)
        {
            position += 1;
        }
        else if (validityFormat != 0)
        {
            position += 7;
        }
        parseUserData(pdu, position, (firstByte & 0x40) != 0);
    }

    private void parseStatusReport(byte[] pdu, int position, int firstByte)
    {
        mIsStatusReport = true;
        position++;
        position = parseOriginatingAddress(pdu, position, true);
        mTimestampMillis = parseTimestamp(pdu, position);
        position += 14;
        mStatus = pdu[position++] & 0xFF;
        if (position < pdu.length)
        {
            int extraParams = pdu[position++] & 0xFF;
            if ((extraParams & 0x01) != 0 && position < pdu.length) mProtocolIdentifier = pdu[position++] & 0xFF;
            if ((extraParams & 0x02) != 0 && position < pdu.length) mDataCodingScheme = pdu[position++] & 0xFF;
            if ((extraParams & 0x04) != 0 && position < pdu.length) parseUserData(pdu, position, (firstByte & 0x40) != 0);
        }
    }

    private int decodeDataCodingScheme()
    {
        int scheme = mDataCodingScheme;
        int encoding = ENCODING_7BIT;
        if ((scheme & 0x80) == 0)
        {
            if ((scheme & 0x20) != 0)
            {
                encoding = ENCODING_UNKNOWN;
            }
            else
            {
                int alphabet = (scheme >> 2) & 0x3;
                encoding = alphabet == 1 ? ENCODING_8BIT : alphabet == 2 ? ENCODING_16BIT : ENCODING_7BIT;
            }
            if ((scheme & 0x10) != 0) mMessageClass = MessageClass.values()[(scheme & 0x3) + 1];
        }
        else if ((scheme & 0xF0) == 0xF0)
        {
            encoding = (scheme & 0x04) == 0 ? ENCODING_7BIT : ENCODING_8BIT;
            mMessageClass = MessageClass.values()[(scheme & 0x3) + 1];
        }
        else if ((scheme & 0xF0) == 0xC0 || (scheme & 0xF0) == 0xD0 || (scheme & 0xF0) == 0xE0)
        {
            encoding = (scheme & 0xF0) == 0xE0 ? ENCODING_16BIT : ENCODING_7BIT;
            mIsMwi = true;
            mMwiSense = (scheme & 0x08) == 0x08;
            mMwiDontStore = (scheme & 0xF0) == 0xC0;
        }
        else if ((scheme & 0xC0) == 0x80)
        {
            encoding = scheme == 0x84 ? ENCODING_KSC5601 : ENCODING_UNKNOWN;
        }
        return encoding;
    }

    private void parseUserData(byte[] pdu, int position, boolean hasHeader)
    {
        int encoding = decodeDataCodingScheme();
        int userDataLength = pdu[position++] & 0xFF;
        int headerOctets = 0;
        if (hasHeader) headerOctets = (pdu[position] & 0xFF) + 1;

        if (encoding == ENCODING_7BIT)
        {
            int headerSeptets = (headerOctets * 8 + 6) / 7;
            int fillBits = headerSeptets * 7 - headerOctets * 8;
            int bodySeptets = Math.max(0, userDataLength - headerSeptets);
            int bodyOffset = position + headerOctets;
            mMessageBody = unpackGsm7Bit(pdu, bodyOffset, bodySeptets, fillBits);
            int packedLength = Math.min(pdu.length - position, (userDataLength * 7 + 7) / 8);
            mUserData = Arrays.copyOfRange(pdu, bodyOffset, Math.max(bodyOffset, position + packedLength));
        }
        else
        {
            int end = Math.min(pdu.length, position + userDataLength);
            int bodyOffset = Math.min(end, position + headerOctets);
            mUserData = Arrays.copyOfRange(pdu, bodyOffset, end);
            Charset charset = encoding == ENCODING_16BIT ? StandardCharsets.UTF_16BE
                : encoding == ENCODING_KSC5601 ? Charset.forName("EUC-KR") : StandardCharsets.ISO_8859_1;
            mMessageBody = encoding == ENCODING_UNKNOWN ? null : new String(mUserData, charset);
        }

        if (mMessageBody != null && mOriginatingAddress != null && mOriginatingAddress.length() <= 4 && !mOriginatingAddress.isEmpty())
        {
            String[] parts = mMessageBody.split("( /)|( )", 2);
            if (parts.length >= 2 && EMAIL_PATTERN.matcher(parts[0]).matches())
            {
                mIsEmail = true;
                mEmailFrom = parts[0];
                mEmailBody = parts[1];
            }
        }
    }

    public String getServiceCenterAddress()
    {
        return mScAddress;
    }

    public String getOriginatingAddress()
    {
        return mOriginatingAddress;
    }

    public String getDisplayOriginatingAddress()
    {
        return mIsEmail ? mEmailFrom : mOriginatingAddress;
    }

    public String getRecipientAddress()
    {
        return mRecipientAddress;
    }

    public String getMessageBody()
    {
        return mMessageBody;
    }

    public MessageClass getMessageClass()
    {
        return mMessageClass;
    }

    public String getDisplayMessageBody()
    {
        return mIsEmail ? mEmailBody : mMessageBody;
    }

    public String getPseudoSubject()
    {
        return "";
    }

    public long getTimestampMillis()
    {
        return mTimestampMillis;
    }

    public boolean isEmail()
    {
        return mIsEmail;
    }

    public String getEmailBody()
    {
        return mEmailBody;
    }

    public String getEmailFrom()
    {
        return mEmailFrom;
    }

    public int getProtocolIdentifier()
    {
        return mProtocolIdentifier;
    }

    public boolean isReplace()
    {
        return (mProtocolIdentifier & 0xC0) == 0x40 && (mProtocolIdentifier & 0x3F) > 0 && (mProtocolIdentifier & 0x3F) < 8;
    }

    public boolean isCphsMwiMessage()
    {
        return mIsCphsMwi;
    }

    public boolean isMWIClearMessage()
    {
        if (mIsMwi && !mMwiSense) return true;
        return mIsCphsMwi && !mCphsMwiSet;
    }

    public boolean isMWISetMessage()
    {
        if (mIsMwi && mMwiSense) return true;
        return mIsCphsMwi && mCphsMwiSet;
    }

    public boolean isMwiDontStore()
    {
        if (mIsMwi && mMwiDontStore) return true;
        return mIsCphsMwi && " ".equals(mMessageBody);
    }

    public byte[] getUserData()
    {
        return mUserData;
    }

    public byte[] getPdu()
    {
        return mPdu;
    }

    @Deprecated
    public int getStatusOnSim()
    {
        return mStatusOnIcc;
    }

    public int getStatusOnIcc()
    {
        return mStatusOnIcc;
    }

    @Deprecated
    public int getIndexOnSim()
    {
        return mIndexOnIcc;
    }

    public int getIndexOnIcc()
    {
        return mIndexOnIcc;
    }

    public int getStatus()
    {
        return mStatus;
    }

    public boolean isStatusReportMessage()
    {
        return mIsStatusReport;
    }

    public boolean isReplyPathPresent()
    {
        return mReplyPathPresent;
    }
}
