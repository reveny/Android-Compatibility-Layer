package android.provider;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsMessage;

public final class Telephony
{
    private Telephony()
    {
    }

    public interface TextBasedSmsColumns
    {
        String ADDRESS = "address";
        String BODY = "body";
        String CREATOR = "creator";
        String DATE = "date";
        String DATE_SENT = "date_sent";
        String ERROR_CODE = "error_code";
        String LOCKED = "locked";
        int MESSAGE_TYPE_ALL = 0;
        int MESSAGE_TYPE_DRAFT = 3;
        int MESSAGE_TYPE_FAILED = 5;
        int MESSAGE_TYPE_INBOX = 1;
        int MESSAGE_TYPE_OUTBOX = 4;
        int MESSAGE_TYPE_QUEUED = 6;
        int MESSAGE_TYPE_SENT = 2;
        String PERSON = "person";
        String PROTOCOL = "protocol";
        String READ = "read";
        String REPLY_PATH_PRESENT = "reply_path_present";
        String SEEN = "seen";
        String SERVICE_CENTER = "service_center";
        String STATUS = "status";
        int STATUS_COMPLETE = 0;
        int STATUS_FAILED = 64;
        int STATUS_NONE = -1;
        int STATUS_PENDING = 32;
        String SUBJECT = "subject";
        String SUBSCRIPTION_ID = "sub_id";
        String THREAD_ID = "thread_id";
        String TYPE = "type";
    }

    public static final class Sms implements BaseColumns, TextBasedSmsColumns
    {
        public static final Uri CONTENT_URI = Uri.parse("content://sms");
        public static final String DEFAULT_SORT_ORDER = "date DESC";

        private Sms()
        {
        }

        public static String getDefaultSmsPackage(Context context)
        {
            return null;
        }

        public static final class Intents
        {
            public static final String ACTION_CHANGE_DEFAULT = "android.provider.Telephony.ACTION_CHANGE_DEFAULT";
            public static final String ACTION_DEFAULT_SMS_PACKAGE_CHANGED = "android.provider.action.DEFAULT_SMS_PACKAGE_CHANGED";
            public static final String ACTION_EXTERNAL_PROVIDER_CHANGE = "android.provider.action.EXTERNAL_PROVIDER_CHANGE";
            public static final String DATA_SMS_RECEIVED_ACTION = "android.intent.action.DATA_SMS_RECEIVED";
            public static final String EXTRA_IS_DEFAULT_SMS_APP = "android.provider.extra.IS_DEFAULT_SMS_APP";
            public static final String EXTRA_PACKAGE_NAME = "package";
            public static final int RESULT_SMS_DATABASE_ERROR = 10;
            public static final int RESULT_SMS_DISPATCH_FAILURE = 6;
            public static final int RESULT_SMS_DUPLICATED = 5;
            public static final int RESULT_SMS_GENERIC_ERROR = 2;
            public static final int RESULT_SMS_HANDLED = 1;
            public static final int RESULT_SMS_INVALID_URI = 11;
            public static final int RESULT_SMS_NULL_MESSAGE = 8;
            public static final int RESULT_SMS_NULL_PDU = 7;
            public static final int RESULT_SMS_OUT_OF_MEMORY = 3;
            public static final int RESULT_SMS_RECEIVED_WHILE_ENCRYPTED = 9;
            public static final int RESULT_SMS_UNSUPPORTED = 4;
            public static final String SECRET_CODE_ACTION = "android.provider.Telephony.SECRET_CODE";
            public static final String SIM_FULL_ACTION = "android.provider.Telephony.SIM_FULL";
            public static final String SMS_CB_RECEIVED_ACTION = "android.provider.Telephony.SMS_CB_RECEIVED";
            public static final String SMS_DELIVER_ACTION = "android.provider.Telephony.SMS_DELIVER";
            public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
            public static final String SMS_REJECTED_ACTION = "android.provider.Telephony.SMS_REJECTED";
            public static final String SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED_ACTION =
                "android.provider.Telephony.SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED";
            public static final String WAP_PUSH_DELIVER_ACTION = "android.provider.Telephony.WAP_PUSH_DELIVER";
            public static final String WAP_PUSH_RECEIVED_ACTION = "android.provider.Telephony.WAP_PUSH_RECEIVED";

            private Intents()
            {
            }

            public static SmsMessage[] getMessagesFromIntent(Intent intent)
            {
                if (intent == null) return null;

                Object pdus = intent.getSerializableExtra("pdus");
                if (!(pdus instanceof Object[])) return null;

                Object[] messages = (Object[])pdus;
                String format = intent.getStringExtra("format");
                SmsMessage[] result = new SmsMessage[messages.length];

                for (int index = 0; index < messages.length; index++)
                {
                    result[index] = SmsMessage.createFromPdu((byte[])messages[index], format);
                }

                return result;
            }
        }
    }
}
