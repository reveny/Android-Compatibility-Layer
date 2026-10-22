package android.provider;

import android.content.Context;
import android.net.Uri;

public final class CalendarContract
{
    public static final String ACCOUNT_TYPE_LOCAL = "LOCAL";
    public static final String ACTION_EVENT_REMINDER = "android.intent.action.EVENT_REMINDER";
    public static final String ACTION_HANDLE_CUSTOM_EVENT = "android.provider.calendar.action.HANDLE_CUSTOM_EVENT";
    public static final String ACTION_VIEW_MANAGED_PROFILE_CALENDAR_EVENT =
        "android.provider.calendar.action.VIEW_MANAGED_PROFILE_CALENDAR_EVENT";
    public static final String AUTHORITY = "com.android.calendar";
    public static final String CALLER_IS_SYNCADAPTER = "caller_is_syncadapter";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    public static final String EXTRA_CUSTOM_APP_URI = "customAppUri";
    public static final String EXTRA_EVENT_ALL_DAY = "allDay";
    public static final String EXTRA_EVENT_BEGIN_TIME = "beginTime";
    public static final String EXTRA_EVENT_END_TIME = "endTime";
    public static final String EXTRA_EVENT_ID = "id";

    private CalendarContract()
    {
    }

    public static boolean startViewCalendarEventInManagedProfile(Context context, long eventId, long startMs, long endMs,
        boolean allDay, int flags)
    {
        return false;
    }

    protected interface CalendarColumns
    {
        String ALLOWED_ATTENDEE_TYPES = "allowedAttendeeTypes";
        String ALLOWED_AVAILABILITY = "allowedAvailability";
        String ALLOWED_REMINDERS = "allowedReminders";
        String CALENDAR_ACCESS_LEVEL = "calendar_access_level";
        String CALENDAR_COLOR = "calendar_color";
        String CALENDAR_COLOR_KEY = "calendar_color_index";
        String CALENDAR_DISPLAY_NAME = "calendar_displayName";
        String CALENDAR_TIME_ZONE = "calendar_timezone";
        int CAL_ACCESS_CONTRIBUTOR = 500;
        int CAL_ACCESS_EDITOR = 600;
        int CAL_ACCESS_FREEBUSY = 100;
        int CAL_ACCESS_NONE = 0;
        int CAL_ACCESS_OVERRIDE = 400;
        int CAL_ACCESS_OWNER = 700;
        int CAL_ACCESS_READ = 200;
        int CAL_ACCESS_RESPOND = 300;
        int CAL_ACCESS_ROOT = 800;
        String CAN_MODIFY_TIME_ZONE = "canModifyTimeZone";
        String CAN_ORGANIZER_RESPOND = "canOrganizerRespond";
        String IS_PRIMARY = "isPrimary";
        String MAX_REMINDERS = "maxReminders";
        String OWNER_ACCOUNT = "ownerAccount";
        String SYNC_EVENTS = "sync_events";
        String VISIBLE = "visible";
    }

    protected interface CalendarSyncColumns
    {
        String CAL_SYNC1 = "cal_sync1";
        String CAL_SYNC10 = "cal_sync10";
        String CAL_SYNC2 = "cal_sync2";
        String CAL_SYNC3 = "cal_sync3";
        String CAL_SYNC4 = "cal_sync4";
        String CAL_SYNC5 = "cal_sync5";
        String CAL_SYNC6 = "cal_sync6";
        String CAL_SYNC7 = "cal_sync7";
        String CAL_SYNC8 = "cal_sync8";
        String CAL_SYNC9 = "cal_sync9";
    }

    protected interface SyncColumns extends CalendarSyncColumns
    {
        String ACCOUNT_NAME = "account_name";
        String ACCOUNT_TYPE = "account_type";
        String CAN_PARTIALLY_UPDATE = "canPartiallyUpdate";
        String DELETED = "deleted";
        String DIRTY = "dirty";
        String MUTATORS = "mutators";
        String _SYNC_ID = "_sync_id";
    }

    protected interface EventsColumns
    {
        int ACCESS_CONFIDENTIAL = 1;
        int ACCESS_DEFAULT = 0;
        String ACCESS_LEVEL = "accessLevel";
        int ACCESS_PRIVATE = 2;
        int ACCESS_PUBLIC = 3;
        String ALL_DAY = "allDay";
        String AVAILABILITY = "availability";
        int AVAILABILITY_BUSY = 0;
        int AVAILABILITY_FREE = 1;
        int AVAILABILITY_TENTATIVE = 2;
        String CALENDAR_ID = "calendar_id";
        String CAN_INVITE_OTHERS = "canInviteOthers";
        String CUSTOM_APP_PACKAGE = "customAppPackage";
        String CUSTOM_APP_URI = "customAppUri";
        String DESCRIPTION = "description";
        String DISPLAY_COLOR = "displayColor";
        String DTEND = "dtend";
        String DTSTART = "dtstart";
        String DURATION = "duration";
        String EVENT_COLOR = "eventColor";
        String EVENT_COLOR_KEY = "eventColor_index";
        String EVENT_END_TIMEZONE = "eventEndTimezone";
        String EVENT_LOCATION = "eventLocation";
        String EVENT_TIMEZONE = "eventTimezone";
        String EXDATE = "exdate";
        String EXRULE = "exrule";
        String GUESTS_CAN_INVITE_OTHERS = "guestsCanInviteOthers";
        String GUESTS_CAN_MODIFY = "guestsCanModify";
        String GUESTS_CAN_SEE_GUESTS = "guestsCanSeeGuests";
        String HAS_ALARM = "hasAlarm";
        String HAS_ATTENDEE_DATA = "hasAttendeeData";
        String HAS_EXTENDED_PROPERTIES = "hasExtendedProperties";
        String IS_ORGANIZER = "isOrganizer";
        String LAST_DATE = "lastDate";
        String LAST_SYNCED = "lastSynced";
        String ORGANIZER = "organizer";
        String ORIGINAL_ALL_DAY = "originalAllDay";
        String ORIGINAL_ID = "original_id";
        String ORIGINAL_INSTANCE_TIME = "originalInstanceTime";
        String ORIGINAL_SYNC_ID = "original_sync_id";
        String RDATE = "rdate";
        String RRULE = "rrule";
        String SELF_ATTENDEE_STATUS = "selfAttendeeStatus";
        String STATUS = "eventStatus";
        int STATUS_CANCELED = 2;
        int STATUS_CONFIRMED = 1;
        int STATUS_TENTATIVE = 0;
        String SYNC_DATA1 = "sync_data1";
        String SYNC_DATA10 = "sync_data10";
        String SYNC_DATA2 = "sync_data2";
        String SYNC_DATA3 = "sync_data3";
        String SYNC_DATA4 = "sync_data4";
        String SYNC_DATA5 = "sync_data5";
        String SYNC_DATA6 = "sync_data6";
        String SYNC_DATA7 = "sync_data7";
        String SYNC_DATA8 = "sync_data8";
        String SYNC_DATA9 = "sync_data9";
        String TITLE = "title";
        String UID_2445 = "uid2445";
    }

    public static final class Events implements BaseColumns, CalendarColumns, EventsColumns, SyncColumns
    {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/events");
        public static final Uri CONTENT_EXCEPTION_URI = Uri.parse("content://" + AUTHORITY + "/exception");
        public static final Uri ENTERPRISE_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/enterprise/events");

        private Events()
        {
        }
    }
}
