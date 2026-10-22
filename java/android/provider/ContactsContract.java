package android.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ContactsContract
{
    public static final String AUTHORITY = "com.android.contacts";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
    public static final String CALLER_IS_SYNCADAPTER = "caller_is_syncadapter";
    public static final String DEFERRED_SNIPPETING = "deferred_snippeting";
    public static final String DEFERRED_SNIPPETING_QUERY = "deferred_snippeting_query";
    public static final String DIRECTORY_PARAM_KEY = "directory";
    public static final String LIMIT_PARAM_KEY = "limit";
    public static final String PRIMARY_ACCOUNT_NAME = "name_for_primary_account";
    public static final String PRIMARY_ACCOUNT_TYPE = "type_for_primary_account";
    public static final String REMOVE_DUPLICATE_ENTRIES = "remove_duplicate_entries";
    public static final String STREQUENT_PHONE_ONLY = "strequent_phone_only";

    private static final long ENTERPRISE_CONTACT_ID_BASE = 1000000000L;
    private static final String PHOTO_DIRECTORY = "photo";
    private static final String DISPLAY_PHOTO_DIRECTORY = "display_photo";
    private static final String DATA_DIRECTORY = "data";
    private static final String PHOTO_COLUMN = "data15";

    public ContactsContract()
    {
    }

    public static boolean isProfileId(long id)
    {
        return id >= Profile.MIN_ID;
    }

    private static Uri lookupFromCursor(ContentResolver resolver, Uri uri, String idColumn)
    {
        if (resolver == null || uri == null) return null;

        Cursor cursor = null;

        try
        {
            cursor = resolver.query(uri, new String[] {idColumn, ContactsColumns.LOOKUP_KEY}, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) return null;
            return Contacts.getLookupUri(cursor.getLong(0), cursor.getString(1));
        }
        catch (RuntimeException failure)
        {
            return null;
        }
        finally
        {
            if (cursor != null) cursor.close();
        }
    }

    protected interface BaseSyncColumns
    {
        String SYNC1 = "sync1";
        String SYNC2 = "sync2";
        String SYNC3 = "sync3";
        String SYNC4 = "sync4";
    }

    protected interface SyncColumns extends BaseSyncColumns
    {
        String ACCOUNT_NAME = "account_name";
        String ACCOUNT_TYPE = "account_type";
        String DIRTY = "dirty";
        String SOURCE_ID = "sourceid";
        String VERSION = "version";
    }

    protected interface ContactNameColumns
    {
        String DISPLAY_NAME_ALTERNATIVE = "display_name_alt";
        String DISPLAY_NAME_PRIMARY = "display_name";
        String DISPLAY_NAME_SOURCE = "display_name_source";
        String PHONETIC_NAME = "phonetic_name";
        String PHONETIC_NAME_STYLE = "phonetic_name_style";
        String SORT_KEY_ALTERNATIVE = "sort_key_alt";
        String SORT_KEY_PRIMARY = "sort_key";
    }

    protected interface ContactOptionsColumns
    {
        String CUSTOM_RINGTONE = "custom_ringtone";
        String LAST_TIME_CONTACTED = "last_time_contacted";
        String PINNED = "pinned";
        String SEND_TO_VOICEMAIL = "send_to_voicemail";
        String STARRED = "starred";
        String TIMES_CONTACTED = "times_contacted";
    }

    protected interface ContactStatusColumns
    {
        String CONTACT_CHAT_CAPABILITY = "contact_chat_capability";
        String CONTACT_PRESENCE = "contact_presence";
        String CONTACT_STATUS = "contact_status";
        String CONTACT_STATUS_ICON = "contact_status_icon";
        String CONTACT_STATUS_LABEL = "contact_status_label";
        String CONTACT_STATUS_RES_PACKAGE = "contact_status_res_package";
        String CONTACT_STATUS_TIMESTAMP = "contact_status_ts";
    }

    protected interface ContactsColumns
    {
        String CONTACT_LAST_UPDATED_TIMESTAMP = "contact_last_updated_timestamp";
        String DISPLAY_NAME = "display_name";
        String HAS_PHONE_NUMBER = "has_phone_number";
        String IN_DEFAULT_DIRECTORY = "in_default_directory";
        String IN_VISIBLE_GROUP = "in_visible_group";
        String IS_USER_PROFILE = "is_user_profile";
        String LOOKUP_KEY = "lookup";
        String NAME_RAW_CONTACT_ID = "name_raw_contact_id";
        String PHOTO_FILE_ID = "photo_file_id";
        String PHOTO_ID = "photo_id";
        String PHOTO_THUMBNAIL_URI = "photo_thumb_uri";
        String PHOTO_URI = "photo_uri";
    }

    protected interface RawContactsColumns
    {
        String ACCOUNT_TYPE_AND_DATA_SET = "account_type_and_data_set";
        String AGGREGATION_MODE = "aggregation_mode";
        String BACKUP_ID = "backup_id";
        String CONTACT_ID = "contact_id";
        String DATA_SET = "data_set";
        String DELETED = "deleted";
        String METADATA_DIRTY = "metadata_dirty";
        String RAW_CONTACT_IS_READ_ONLY = "raw_contact_is_read_only";
        String RAW_CONTACT_IS_USER_PROFILE = "raw_contact_is_user_profile";
    }

    protected interface StatusColumns
    {
        int AVAILABLE = 5;
        int AWAY = 2;
        int CAPABILITY_HAS_CAMERA = 4;
        int CAPABILITY_HAS_VIDEO = 2;
        int CAPABILITY_HAS_VOICE = 1;
        String CHAT_CAPABILITY = "chat_capability";
        int DO_NOT_DISTURB = 4;
        int IDLE = 3;
        int INVISIBLE = 1;
        int OFFLINE = 0;
        String PRESENCE = "mode";
        String PRESENCE_CUSTOM_STATUS = "status";
        String PRESENCE_STATUS = "mode";
        String STATUS = "status";
        String STATUS_ICON = "status_icon";
        String STATUS_LABEL = "status_label";
        String STATUS_RES_PACKAGE = "status_res_package";
        String STATUS_TIMESTAMP = "status_ts";
    }

    protected interface DataColumns
    {
        String CARRIER_PRESENCE = "carrier_presence";
        int CARRIER_PRESENCE_VT_CAPABLE = 1;
        String DATA1 = "data1";
        String DATA10 = "data10";
        String DATA11 = "data11";
        String DATA12 = "data12";
        String DATA13 = "data13";
        String DATA14 = "data14";
        String DATA15 = "data15";
        String DATA2 = "data2";
        String DATA3 = "data3";
        String DATA4 = "data4";
        String DATA5 = "data5";
        String DATA6 = "data6";
        String DATA7 = "data7";
        String DATA8 = "data8";
        String DATA9 = "data9";
        String DATA_VERSION = "data_version";
        String IS_PRIMARY = "is_primary";
        String IS_READ_ONLY = "is_read_only";
        String IS_SUPER_PRIMARY = "is_super_primary";
        String MIMETYPE = "mimetype";
        String PREFERRED_PHONE_ACCOUNT_COMPONENT_NAME = "preferred_phone_account_component_name";
        String PREFERRED_PHONE_ACCOUNT_ID = "preferred_phone_account_id";
        String RAW_CONTACT_ID = "raw_contact_id";
        String RES_PACKAGE = "res_package";
        String SYNC1 = "data_sync1";
        String SYNC2 = "data_sync2";
        String SYNC3 = "data_sync3";
        String SYNC4 = "data_sync4";
    }

    protected interface DataUsageStatColumns
    {
        String LAST_TIME_USED = "last_time_used";
        String TIMES_USED = "times_used";
    }

    protected interface DataColumnsWithJoins extends BaseColumns, ContactNameColumns, ContactOptionsColumns, ContactStatusColumns,
        ContactsColumns, DataColumns, DataUsageStatColumns, RawContactsColumns, StatusColumns
    {
    }

    public static class Contacts implements BaseColumns, ContactNameColumns, ContactOptionsColumns, ContactStatusColumns, ContactsColumns
    {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "contacts");
        public static final Uri ENTERPRISE_CONTENT_URI = Uri.withAppendedPath(CONTENT_URI, "enterprise");
        public static final Uri CONTENT_LOOKUP_URI = Uri.withAppendedPath(CONTENT_URI, "lookup");
        public static final Uri CONTENT_VCARD_URI = Uri.withAppendedPath(CONTENT_URI, "as_vcard");
        public static final Uri CONTENT_MULTI_VCARD_URI = Uri.withAppendedPath(CONTENT_URI, "as_multi_vcard");
        public static final Uri CONTENT_FILTER_URI = Uri.withAppendedPath(CONTENT_URI, "filter");
        public static final Uri ENTERPRISE_CONTENT_FILTER_URI = Uri.withAppendedPath(CONTENT_URI, "filter_enterprise");
        public static final Uri CONTENT_STREQUENT_URI = Uri.withAppendedPath(CONTENT_URI, "strequent");
        public static final Uri CONTENT_FREQUENT_URI = Uri.withAppendedPath(CONTENT_URI, "frequent");
        public static final Uri CONTENT_STREQUENT_FILTER_URI = Uri.withAppendedPath(CONTENT_STREQUENT_URI, "filter");
        public static final Uri CONTENT_GROUP_URI = Uri.withAppendedPath(CONTENT_URI, "group");
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/contact";
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/contact";
        public static final String CONTENT_VCARD_TYPE = "text/x-vcard";
        public static final String EXTRA_ADDRESS_BOOK_INDEX = "android.provider.extra.ADDRESS_BOOK_INDEX";
        public static final String EXTRA_ADDRESS_BOOK_INDEX_COUNTS = "android.provider.extra.ADDRESS_BOOK_INDEX_COUNTS";
        public static final String EXTRA_ADDRESS_BOOK_INDEX_TITLES = "android.provider.extra.ADDRESS_BOOK_INDEX_TITLES";
        public static final String QUERY_PARAMETER_VCARD_NO_PHOTO = "no_photo";

        private Contacts()
        {
        }

        public static Uri getLookupUri(ContentResolver resolver, Uri contactUri)
        {
            return lookupFromCursor(resolver, contactUri, _ID);
        }

        public static Uri getLookupUri(long contactId, String lookupKey)
        {
            if (TextUtils.isEmpty(lookupKey)) return null;
            return ContentUris.withAppendedId(Uri.withAppendedPath(CONTENT_LOOKUP_URI, lookupKey), contactId);
        }

        public static Uri lookupContact(ContentResolver resolver, Uri lookupUri)
        {
            if (resolver == null || lookupUri == null) return null;

            Cursor cursor = null;

            try
            {
                cursor = resolver.query(lookupUri, new String[] {_ID}, null, null, null);
                if (cursor == null || !cursor.moveToFirst()) return null;
                return ContentUris.withAppendedId(CONTENT_URI, cursor.getLong(0));
            }
            catch (RuntimeException failure)
            {
                return null;
            }
            finally
            {
                if (cursor != null) cursor.close();
            }
        }

        public static boolean isEnterpriseContactId(long contactId)
        {
            return contactId >= ENTERPRISE_CONTACT_ID_BASE && contactId < Profile.MIN_ID;
        }

        public static void markAsContacted(ContentResolver resolver, long contactId)
        {
        }

        public static InputStream openContactPhotoInputStream(ContentResolver resolver, Uri contactUri)
        {
            return openContactPhotoInputStream(resolver, contactUri, false);
        }

        public static InputStream openContactPhotoInputStream(ContentResolver resolver, Uri contactUri, boolean preferHighres)
        {
            if (resolver == null || contactUri == null) return null;

            if (preferHighres)
            {
                try
                {
                    AssetFileDescriptor descriptor =
                        resolver.openAssetFileDescriptor(Uri.withAppendedPath(contactUri, DISPLAY_PHOTO_DIRECTORY), "r");
                    if (descriptor != null) return descriptor.createInputStream();
                }
                catch (IOException | RuntimeException failure)
                {
                }
            }

            Cursor cursor = null;

            try
            {
                cursor = resolver.query(Uri.withAppendedPath(contactUri, PHOTO_DIRECTORY), new String[] {PHOTO_COLUMN}, null, null,
                    null);
                if (cursor == null || !cursor.moveToFirst()) return null;
                byte[] data = cursor.getBlob(0);
                return data == null ? null : new ByteArrayInputStream(data);
            }
            catch (RuntimeException failure)
            {
                return null;
            }
            finally
            {
                if (cursor != null) cursor.close();
            }
        }
    }

    public static final class Profile implements BaseColumns, ContactNameColumns, ContactOptionsColumns, ContactStatusColumns,
        ContactsColumns
    {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "profile");
        public static final Uri CONTENT_VCARD_URI = Uri.withAppendedPath(CONTENT_URI, "as_vcard");
        public static final Uri CONTENT_RAW_CONTACTS_URI = Uri.withAppendedPath(CONTENT_URI, "raw_contacts");
        public static final long MIN_ID = 9223372034707292160L;

        private Profile()
        {
        }
    }

    public static final class RawContacts implements BaseColumns, ContactNameColumns, ContactOptionsColumns, RawContactsColumns,
        SyncColumns
    {
        public static final int AGGREGATION_MODE_DEFAULT = 0;
        public static final int AGGREGATION_MODE_DISABLED = 3;
        public static final int AGGREGATION_MODE_IMMEDIATE = 1;
        public static final int AGGREGATION_MODE_SUSPENDED = 2;
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/raw_contact";
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/raw_contact";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "raw_contacts");

        private RawContacts()
        {
        }

        public static Uri getContactLookupUri(ContentResolver resolver, Uri rawContactUri)
        {
            if (rawContactUri == null) return null;
            return lookupFromCursor(resolver, Uri.withAppendedPath(rawContactUri, DATA_DIRECTORY), RawContactsColumns.CONTACT_ID);
        }

        public static String getLocalAccountName(Context context)
        {
            return null;
        }

        public static String getLocalAccountType(Context context)
        {
            return null;
        }
    }

    public static final class RawContactsEntity implements BaseColumns, DataColumns, RawContactsColumns
    {
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/raw_contact_entity";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "raw_contact_entities");
        public static final Uri PROFILE_CONTENT_URI = Uri.withAppendedPath(Profile.CONTENT_URI, "raw_contact_entities");
        public static final String DATA_ID = "data_id";

        private RawContactsEntity()
        {
        }
    }

    public static final class Data implements DataColumnsWithJoins
    {
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/data";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "data");
        public static final String EXTRA_ADDRESS_BOOK_INDEX = "android.provider.extra.ADDRESS_BOOK_INDEX";
        public static final String EXTRA_ADDRESS_BOOK_INDEX_COUNTS = "android.provider.extra.ADDRESS_BOOK_INDEX_COUNTS";
        public static final String EXTRA_ADDRESS_BOOK_INDEX_TITLES = "android.provider.extra.ADDRESS_BOOK_INDEX_TITLES";
        public static final String VISIBLE_CONTACTS_ONLY = "visible_contacts_only";

        static final Uri ENTERPRISE_CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "data_enterprise");

        private Data()
        {
        }

        public static Uri getContactLookupUri(ContentResolver resolver, Uri dataUri)
        {
            return lookupFromCursor(resolver, dataUri, RawContactsColumns.CONTACT_ID);
        }
    }

    public static final class CommonDataKinds
    {
        private CommonDataKinds()
        {
        }

        private static int labelResource(String name)
        {
            try
            {
                return Resources.getSystem().getIdentifier(name, "string", "android");
            }
            catch (RuntimeException failure)
            {
                return 0;
            }
        }

        private static CharSequence label(Resources resources, int resource, String fallback)
        {
            if (resources == null || resource == 0) return fallback;

            try
            {
                return resources.getText(resource);
            }
            catch (RuntimeException failure)
            {
                return fallback;
            }
        }

        public interface BaseTypes
        {
            int TYPE_CUSTOM = 0;
        }

        protected interface CommonColumns extends BaseTypes
        {
            String DATA = "data1";
            String LABEL = "data3";
            String TYPE = "data2";
        }

        public static final class Email implements CommonColumns, DataColumnsWithJoins
        {
            public static final String ADDRESS = "data1";
            public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/email_v2";
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/email_v2";
            public static final Uri CONTENT_URI = Uri.withAppendedPath(Data.CONTENT_URI, "emails");
            public static final Uri CONTENT_LOOKUP_URI = Uri.withAppendedPath(CONTENT_URI, "lookup");
            public static final Uri ENTERPRISE_CONTENT_LOOKUP_URI = Uri.withAppendedPath(CONTENT_URI, "lookup_enterprise");
            public static final Uri CONTENT_FILTER_URI = Uri.withAppendedPath(CONTENT_URI, "filter");
            public static final Uri ENTERPRISE_CONTENT_FILTER_URI = Uri.withAppendedPath(CONTENT_URI, "filter_enterprise");
            public static final String DISPLAY_NAME = "data4";
            public static final String EXTRA_ADDRESS_BOOK_INDEX = "android.provider.extra.ADDRESS_BOOK_INDEX";
            public static final String EXTRA_ADDRESS_BOOK_INDEX_COUNTS = "android.provider.extra.ADDRESS_BOOK_INDEX_COUNTS";
            public static final String EXTRA_ADDRESS_BOOK_INDEX_TITLES = "android.provider.extra.ADDRESS_BOOK_INDEX_TITLES";
            public static final int TYPE_HOME = 1;
            public static final int TYPE_MOBILE = 4;
            public static final int TYPE_OTHER = 3;
            public static final int TYPE_WORK = 2;

            private static final String[] RESOURCE_NAMES = {"emailTypeCustom", "emailTypeHome", "emailTypeWork", "emailTypeOther",
                "emailTypeMobile"};
            private static final String[] LABELS = {"Custom", "Home", "Work", "Other", "Mobile"};

            private Email()
            {
            }

            private static int index(int type)
            {
                return type >= 0 && type < LABELS.length ? type : TYPE_CUSTOM;
            }

            public static int getTypeLabelResource(int type)
            {
                return labelResource(RESOURCE_NAMES[index(type)]);
            }

            public static CharSequence getTypeLabel(Resources resources, int type, CharSequence label)
            {
                if (type == TYPE_CUSTOM && !TextUtils.isEmpty(label)) return label;
                return CommonDataKinds.label(resources, getTypeLabelResource(type), LABELS[index(type)]);
            }
        }

        public static final class Phone implements CommonColumns, DataColumnsWithJoins
        {
            public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/phone_v2";
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/phone_v2";
            public static final Uri CONTENT_URI = Uri.withAppendedPath(Data.CONTENT_URI, "phones");
            public static final Uri ENTERPRISE_CONTENT_URI = Uri.withAppendedPath(Data.ENTERPRISE_CONTENT_URI, "phones");
            public static final Uri CONTENT_FILTER_URI = Uri.withAppendedPath(CONTENT_URI, "filter");
            public static final Uri ENTERPRISE_CONTENT_FILTER_URI = Uri.withAppendedPath(CONTENT_URI, "filter_enterprise");
            public static final String EXTRA_ADDRESS_BOOK_INDEX = "android.provider.extra.ADDRESS_BOOK_INDEX";
            public static final String EXTRA_ADDRESS_BOOK_INDEX_COUNTS = "android.provider.extra.ADDRESS_BOOK_INDEX_COUNTS";
            public static final String EXTRA_ADDRESS_BOOK_INDEX_TITLES = "android.provider.extra.ADDRESS_BOOK_INDEX_TITLES";
            public static final String NORMALIZED_NUMBER = "data4";
            public static final String NUMBER = "data1";
            public static final String SEARCH_DISPLAY_NAME_KEY = "search_display_name";
            public static final String SEARCH_PHONE_NUMBER_KEY = "search_phone_number";
            public static final int TYPE_ASSISTANT = 19;
            public static final int TYPE_CALLBACK = 8;
            public static final int TYPE_CAR = 9;
            public static final int TYPE_COMPANY_MAIN = 10;
            public static final int TYPE_FAX_HOME = 5;
            public static final int TYPE_FAX_WORK = 4;
            public static final int TYPE_HOME = 1;
            public static final int TYPE_ISDN = 11;
            public static final int TYPE_MAIN = 12;
            public static final int TYPE_MMS = 20;
            public static final int TYPE_MOBILE = 2;
            public static final int TYPE_OTHER = 7;
            public static final int TYPE_OTHER_FAX = 13;
            public static final int TYPE_PAGER = 6;
            public static final int TYPE_RADIO = 14;
            public static final int TYPE_TELEX = 15;
            public static final int TYPE_TTY_TDD = 16;
            public static final int TYPE_WORK = 3;
            public static final int TYPE_WORK_MOBILE = 17;
            public static final int TYPE_WORK_PAGER = 18;

            private static final String[] RESOURCE_NAMES = {"phoneTypeCustom", "phoneTypeHome", "phoneTypeMobile", "phoneTypeWork",
                "phoneTypeFaxWork", "phoneTypeFaxHome", "phoneTypePager", "phoneTypeOther", "phoneTypeCallback", "phoneTypeCar",
                "phoneTypeCompanyMain", "phoneTypeIsdn", "phoneTypeMain", "phoneTypeOtherFax", "phoneTypeRadio", "phoneTypeTelex",
                "phoneTypeTtyTdd", "phoneTypeWorkMobile", "phoneTypeWorkPager", "phoneTypeAssistant", "phoneTypeMms"};
            private static final String[] LABELS = {"Custom", "Home", "Mobile", "Work", "Work Fax", "Home Fax", "Pager", "Other",
                "Callback", "Car", "Company Main", "ISDN", "Main", "Other Fax", "Radio", "Telex", "TTY TDD", "Work Mobile",
                "Work Pager", "Assistant", "MMS"};

            private Phone()
            {
            }

            private static int index(int type)
            {
                return type >= 0 && type < LABELS.length ? type : TYPE_CUSTOM;
            }

            public static int getTypeLabelResource(int type)
            {
                return labelResource(RESOURCE_NAMES[index(type)]);
            }

            public static CharSequence getTypeLabel(Resources resources, int type, CharSequence label)
            {
                if ((type == TYPE_CUSTOM || type == TYPE_ASSISTANT) && !TextUtils.isEmpty(label)) return label;
                return CommonDataKinds.label(resources, getTypeLabelResource(type), LABELS[index(type)]);
            }
        }
    }
}
