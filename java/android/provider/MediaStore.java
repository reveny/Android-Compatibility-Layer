package android.provider;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Size;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MediaStore
{
    public static final String ACCESS_MEDIA_OWNER_PACKAGE_NAME_PERMISSION =
        "com.android.providers.media.permission.ACCESS_MEDIA_OWNER_PACKAGE_NAME";
    public static final String ACCESS_OEM_METADATA_PERMISSION = "com.android.providers.media.permission.ACCESS_OEM_METADATA";
    public static final String ACTION_IMAGE_CAPTURE = "android.media.action.IMAGE_CAPTURE";
    public static final String ACTION_IMAGE_CAPTURE_SECURE = "android.media.action.IMAGE_CAPTURE_SECURE";
    public static final String ACTION_MOTION_PHOTO_CAPTURE = "android.provider.action.MOTION_PHOTO_CAPTURE";
    public static final String ACTION_MOTION_PHOTO_CAPTURE_SECURE = "android.provider.action.MOTION_PHOTO_CAPTURE_SECURE";
    public static final String ACTION_PICK_IMAGES = "android.provider.action.PICK_IMAGES";
    public static final String ACTION_PICK_IMAGES_SETTINGS = "android.provider.action.PICK_IMAGES_SETTINGS";
    public static final String ACTION_REVIEW = "android.provider.action.REVIEW";
    public static final String ACTION_REVIEW_SECURE = "android.provider.action.REVIEW_SECURE";
    public static final String ACTION_VIDEO_CAPTURE = "android.media.action.VIDEO_CAPTURE";
    public static final String AUTHORITY = "media";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
    public static final String EXTRA_ACCEPT_ORIGINAL_MEDIA_FORMAT = "android.provider.extra.ACCEPT_ORIGINAL_MEDIA_FORMAT";
    public static final String EXTRA_BRIGHTNESS = "android.provider.extra.BRIGHTNESS";
    public static final String EXTRA_DURATION_LIMIT = "android.intent.extra.durationLimit";
    public static final String EXTRA_FINISH_ON_COMPLETION = "android.intent.extra.finishOnCompletion";
    public static final String EXTRA_FULL_SCREEN = "android.intent.extra.fullScreen";
    public static final String EXTRA_MEDIA_ALBUM = "android.intent.extra.album";
    public static final String EXTRA_MEDIA_ARTIST = "android.intent.extra.artist";
    public static final String EXTRA_MEDIA_CAPABILITIES = "android.provider.extra.MEDIA_CAPABILITIES";
    public static final String EXTRA_MEDIA_CAPABILITIES_UID = "android.provider.extra.MEDIA_CAPABILITIES_UID";
    public static final String EXTRA_MEDIA_FOCUS = "android.intent.extra.focus";
    public static final String EXTRA_MEDIA_GENRE = "android.intent.extra.genre";
    public static final String EXTRA_MEDIA_PLAYLIST = "android.intent.extra.playlist";
    public static final String EXTRA_MEDIA_RADIO_CHANNEL = "android.intent.extra.radio_channel";
    public static final String EXTRA_MEDIA_TITLE = "android.intent.extra.title";
    public static final String EXTRA_OUTPUT = "output";
    public static final String EXTRA_PICKER_PRE_SELECTION_URIS = "android.provider.extra.PICKER_PRE_SELECTION_URIS";
    public static final String EXTRA_PICK_IMAGES_ACCENT_COLOR = "android.provider.extra.PICK_IMAGES_ACCENT_COLOR";
    public static final String EXTRA_PICK_IMAGES_IN_ORDER = "android.provider.extra.PICK_IMAGES_IN_ORDER";
    public static final String EXTRA_PICK_IMAGES_LAUNCH_TAB = "android.provider.extra.PICK_IMAGES_LAUNCH_TAB";
    public static final String EXTRA_PICK_IMAGES_MAX = "android.provider.extra.PICK_IMAGES_MAX";
    public static final String EXTRA_SCREEN_ORIENTATION = "android.intent.extra.screenOrientation";
    public static final String EXTRA_SHOW_ACTION_ICONS = "android.intent.extra.showActionIcons";
    public static final String EXTRA_SIZE_LIMIT = "android.intent.extra.sizeLimit";
    public static final String EXTRA_VIDEO_QUALITY = "android.intent.extra.videoQuality";
    public static final String INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH = "android.media.action.MEDIA_PLAY_FROM_SEARCH";
    public static final String INTENT_ACTION_MEDIA_SEARCH = "android.intent.action.MEDIA_SEARCH";
    public static final String INTENT_ACTION_MUSIC_PLAYER = "android.intent.action.MUSIC_PLAYER";
    public static final String INTENT_ACTION_STILL_IMAGE_CAMERA = "android.media.action.STILL_IMAGE_CAMERA";
    public static final String INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE = "android.media.action.STILL_IMAGE_CAMERA_SECURE";
    public static final String INTENT_ACTION_TEXT_OPEN_FROM_SEARCH = "android.media.action.TEXT_OPEN_FROM_SEARCH";
    public static final String INTENT_ACTION_VIDEO_CAMERA = "android.media.action.VIDEO_CAMERA";
    public static final String INTENT_ACTION_VIDEO_PLAY_FROM_SEARCH = "android.media.action.VIDEO_PLAY_FROM_SEARCH";
    public static final int MATCH_DEFAULT = 0;
    public static final int MATCH_EXCLUDE = 2;
    public static final int MATCH_INCLUDE = 1;
    public static final int MATCH_ONLY = 3;
    public static final String MEDIA_IGNORE_FILENAME = ".nomedia";
    public static final String MEDIA_SCANNER_VOLUME = "volume";
    public static final String META_DATA_REVIEW_GALLERY_PREWARM_SERVICE = "android.media.review_gallery_prewarm_service";
    public static final String META_DATA_STILL_IMAGE_CAMERA_PREWARM_SERVICE = "android.media.still_image_camera_preview_service";
    public static final int PICK_IMAGES_TAB_ALBUMS = 0;
    public static final int PICK_IMAGES_TAB_IMAGES = 1;
    public static final String QUERY_ARG_INCLUDE_RECENTLY_UNMOUNTED_VOLUMES = "android:query-arg-recently-unmounted-volumes";
    public static final String QUERY_ARG_LATEST_SELECTION_ONLY = "android:query-arg-latest-selection-only";
    public static final String QUERY_ARG_MATCH_FAVORITE = "android:query-arg-match-favorite";
    public static final String QUERY_ARG_MATCH_PENDING = "android:query-arg-match-pending";
    public static final String QUERY_ARG_MATCH_TRASHED = "android:query-arg-match-trashed";
    public static final String QUERY_ARG_MEDIA_STANDARD_SORT_ORDER = "android:query-arg-media-standard-sort-order";
    public static final String QUERY_ARG_RELATED_URI = "android:query-arg-related-uri";
    public static final String UNKNOWN_STRING = "<unknown>";
    public static final String VOLUME_EXTERNAL = "external";
    public static final String VOLUME_EXTERNAL_PRIMARY = "external_primary";
    public static final String VOLUME_INTERNAL = "internal";

    private static final String PARAM_INCLUDE_PENDING = "includePending";
    private static final String PARAM_REQUIRE_ORIGINAL = "requireOriginal";
    private static final String ACTION_WRITE_REQUEST = "android.provider.action.WRITE_REQUEST";
    private static final String ACTION_DELETE_REQUEST = "android.provider.action.DELETE_REQUEST";
    private static final String ACTION_TRASH_REQUEST = "android.provider.action.TRASH_REQUEST";
    private static final String ACTION_FAVORITE_REQUEST = "android.provider.action.FAVORITE_REQUEST";
    private static final String EXTRA_URIS = "android.provider.extra.URIS";
    private static final String EXTRA_VALUE = "android.provider.extra.VALUE";
    private static final int PICK_IMAGES_MAX_LIMIT = 100;

    public MediaStore()
    {
    }

    private static Uri volumeUri(String volumeName, String first, String second)
    {
        if (volumeName == null) throw new IllegalArgumentException("Volume must not be null");

        Uri.Builder builder = AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath(first);
        if (second != null) builder.appendPath(second);
        return builder.build();
    }

    private static Uri withId(Uri base, long id)
    {
        return ContentUris.withAppendedId(base, id);
    }

    private static PendingIntent createRequest(ContentResolver resolver, String action, Collection<Uri> uris, Boolean value)
    {
        if (resolver == null) throw new NullPointerException("resolver");
        if (uris == null) throw new NullPointerException("uris");

        for (Uri uri : uris)
        {
            if (uri == null) throw new NullPointerException("uri");
            if (!AUTHORITY.equals(uri.getAuthority())) throw new IllegalArgumentException("All requested items must be media items");
        }

        Intent intent = new Intent(action);
        intent.putParcelableArrayListExtra(EXTRA_URIS, new ArrayList<>(uris));
        if (value != null) intent.putExtra(EXTRA_VALUE, value.booleanValue());
        return PendingIntent.getActivity(resolver.getContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    public static PendingIntent createWriteRequest(ContentResolver resolver, Collection<Uri> uris)
    {
        return createRequest(resolver, ACTION_WRITE_REQUEST, uris, null);
    }

    public static PendingIntent createDeleteRequest(ContentResolver resolver, Collection<Uri> uris)
    {
        return createRequest(resolver, ACTION_DELETE_REQUEST, uris, null);
    }

    public static PendingIntent createTrashRequest(ContentResolver resolver, Collection<Uri> uris, boolean value)
    {
        return createRequest(resolver, ACTION_TRASH_REQUEST, uris, value);
    }

    public static PendingIntent createFavoriteRequest(ContentResolver resolver, Collection<Uri> uris, boolean value)
    {
        return createRequest(resolver, ACTION_FAVORITE_REQUEST, uris, value);
    }

    public static void markIsFavoriteStatus(ContentResolver resolver, Collection<Uri> uris, boolean areFavorites)
    {
        if (resolver == null || uris == null) return;

        ContentValues values = new ContentValues();
        values.put(MediaColumns.IS_FAVORITE, areFavorites ? 1 : 0);

        for (Uri uri : uris)
        {
            try
            {
                resolver.update(uri, values, null, null);
            }
            catch (RuntimeException failure)
            {
            }
        }
    }

    public static boolean canManageMedia(Context context)
    {
        return false;
    }

    public static Uri setIncludePending(Uri uri)
    {
        return uri.buildUpon().appendQueryParameter(PARAM_INCLUDE_PENDING, "1").build();
    }

    public static Uri setRequireOriginal(Uri uri)
    {
        return uri.buildUpon().appendQueryParameter(PARAM_REQUIRE_ORIGINAL, "1").build();
    }

    public static boolean getRequireOriginal(Uri uri)
    {
        return uri.getBooleanQueryParameter(PARAM_REQUIRE_ORIGINAL, false);
    }

    public static ParcelFileDescriptor openFileDescriptor(ContentResolver resolver, Uri uri, String mode, CancellationSignal signal)
        throws FileNotFoundException
    {
        return resolver.openFileDescriptor(uri, mode, signal);
    }

    public static AssetFileDescriptor openAssetFileDescriptor(ContentResolver resolver, Uri uri, String mode,
        CancellationSignal signal) throws FileNotFoundException
    {
        return resolver.openAssetFileDescriptor(uri, mode, signal);
    }

    public static AssetFileDescriptor openTypedAssetFileDescriptor(ContentResolver resolver, Uri uri, String mimeType, Bundle options,
        CancellationSignal signal) throws FileNotFoundException
    {
        return resolver.openTypedAssetFileDescriptor(uri, mimeType, options, signal);
    }

    public static ParcelFileDescriptor getOriginalMediaFormatFileDescriptor(Context context, ParcelFileDescriptor fileDescriptor)
        throws IOException
    {
        return fileDescriptor.dup();
    }

    public static Set<String> getExternalVolumeNames(Context context)
    {
        return Collections.singleton(VOLUME_EXTERNAL_PRIMARY);
    }

    public static Set<String> getRecentExternalVolumeNames(Context context)
    {
        return Collections.singleton(VOLUME_EXTERNAL_PRIMARY);
    }

    public static String getVolumeName(Uri uri)
    {
        List<String> segments = uri.getPathSegments();
        if (AUTHORITY.equals(uri.getAuthority()) && segments != null && segments.size() > 0) return segments.get(0);
        throw new IllegalArgumentException("Missing volume name: " + uri);
    }

    public static Uri getMediaScannerUri()
    {
        return AUTHORITY_URI.buildUpon().appendPath("none").appendPath("media_scanner").build();
    }

    public static String getVersion(Context context)
    {
        return getVersion(context, VOLUME_EXTERNAL_PRIMARY);
    }

    public static String getVersion(Context context, String volumeName)
    {
        return "acl:" + volumeName;
    }

    public static long getGeneration(Context context, String volumeName)
    {
        return 0;
    }

    public static Uri getDocumentUri(Context context, Uri mediaUri)
    {
        return null;
    }

    public static Uri getMediaUri(Context context, Uri documentUri)
    {
        return null;
    }

    public static int getPickImagesMaxLimit()
    {
        return PICK_IMAGES_MAX_LIMIT;
    }

    public static Uri getRedactedUri(ContentResolver resolver, Uri uri)
    {
        return null;
    }

    public static List<Uri> getRedactedUri(ContentResolver resolver, List<Uri> uris)
    {
        List<Uri> result = new ArrayList<>();
        if (uris == null) return result;

        for (int index = 0; index < uris.size(); index++)
        {
            result.add(null);
        }

        return result;
    }

    public static boolean isCurrentCloudMediaProviderAuthority(ContentResolver resolver, String authority)
    {
        return false;
    }

    public static boolean isCurrentSystemGallery(ContentResolver resolver, int uid, String packageName)
    {
        return false;
    }

    public static boolean isSupportedCloudMediaProviderAuthority(ContentResolver resolver, String authority)
    {
        return false;
    }

    public static void notifyCloudMediaChangedEvent(ContentResolver resolver, String authority, String currentMediaCollectionId)
        throws SecurityException
    {
        throw new SecurityException("Caller is not the current cloud media provider");
    }

    private static Size kindSize(int kind)
    {
        if (kind == Images.Thumbnails.MICRO_KIND) return new Size(96, 96);
        if (kind == Images.Thumbnails.MINI_KIND) return new Size(512, 384);
        if (kind == Images.Thumbnails.FULL_SCREEN_KIND) return new Size(1024, 786);
        throw new IllegalArgumentException("Unsupported kind: " + kind);
    }

    private static Bitmap loadThumbnail(ContentResolver resolver, Uri uri, int kind)
    {
        try
        {
            return resolver.loadThumbnail(uri, kindSize(kind), null);
        }
        catch (IOException | RuntimeException failure)
        {
            return null;
        }
    }

    public interface MediaColumns extends BaseColumns
    {
        String ALBUM = "album";
        String ALBUM_ARTIST = "album_artist";
        String ARTIST = "artist";
        String AUTHOR = "author";
        String BITRATE = "bitrate";
        String BUCKET_DISPLAY_NAME = "bucket_display_name";
        String BUCKET_ID = "bucket_id";
        String CAPTURE_FRAMERATE = "capture_framerate";
        String CD_TRACK_NUMBER = "cd_track_number";
        String COMPILATION = "compilation";
        String COMPOSER = "composer";
        String DATA = "_data";
        String DATE_ADDED = "date_added";
        String DATE_EXPIRES = "date_expires";
        String DATE_MODIFIED = "date_modified";
        String DATE_TAKEN = "datetaken";
        String DISC_NUMBER = "disc_number";
        String DISPLAY_NAME = "_display_name";
        String DOCUMENT_ID = "document_id";
        String DURATION = "duration";
        String GENERATION_ADDED = "generation_added";
        String GENERATION_MODIFIED = "generation_modified";
        String GENRE = "genre";
        String HEIGHT = "height";
        String INFERRED_DATE = "inferred_date";
        String INSTANCE_ID = "instance_id";
        String IS_DOWNLOAD = "is_download";
        String IS_DRM = "is_drm";
        String IS_FAVORITE = "is_favorite";
        String IS_PENDING = "is_pending";
        String IS_TRASHED = "is_trashed";
        String MIME_TYPE = "mime_type";
        String NUM_TRACKS = "num_tracks";
        String OEM_METADATA = "oem_metadata";
        String ORIENTATION = "orientation";
        String ORIGINAL_DOCUMENT_ID = "original_document_id";
        String OWNER_PACKAGE_NAME = "owner_package_name";
        String RELATIVE_PATH = "relative_path";
        String RESOLUTION = "resolution";
        String SIZE = "_size";
        String TITLE = "title";
        String VOLUME_NAME = "volume_name";
        String WIDTH = "width";
        String WRITER = "writer";
        String XMP = "xmp";
        String YEAR = "year";
    }

    public interface DownloadColumns extends MediaColumns
    {
        String DOWNLOAD_URI = "download_uri";
        String REFERER_URI = "referer_uri";
    }

    public static final class Downloads implements DownloadColumns
    {
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/download";
        public static final Uri EXTERNAL_CONTENT_URI = getContentUri(VOLUME_EXTERNAL);
        public static final Uri INTERNAL_CONTENT_URI = getContentUri(VOLUME_INTERNAL);

        private Downloads()
        {
        }

        public static Uri getContentUri(String volumeName)
        {
            return volumeUri(volumeName, "downloads", null);
        }

        public static Uri getContentUri(String volumeName, long id)
        {
            return withId(getContentUri(volumeName), id);
        }
    }

    public static final class Files
    {
        public Files()
        {
        }

        public static Uri getContentUri(String volumeName)
        {
            return volumeUri(volumeName, "file", null);
        }

        public static Uri getContentUri(String volumeName, long rowId)
        {
            return withId(getContentUri(volumeName), rowId);
        }

        public interface FileColumns extends MediaColumns
        {
            String MEDIA_TYPE = "media_type";
            int MEDIA_TYPE_AUDIO = 2;
            int MEDIA_TYPE_DOCUMENT = 6;
            int MEDIA_TYPE_IMAGE = 1;
            int MEDIA_TYPE_NONE = 0;
            int MEDIA_TYPE_PLAYLIST = 4;
            int MEDIA_TYPE_SUBTITLE = 5;
            int MEDIA_TYPE_VIDEO = 3;
            String MIME_TYPE = "mime_type";
            String PARENT = "parent";
        }
    }

    public static final class Audio
    {
        public Audio()
        {
        }

        public static String keyFor(String name)
        {
            if (name == null) return null;

            String key = name.trim().toLowerCase(Locale.ROOT);
            if (key.equals(UNKNOWN_STRING)) return "\001";
            if (key.startsWith("the ")) key = key.substring(4);
            else if (key.startsWith("an ")) key = key.substring(3);
            else if (key.startsWith("a ")) key = key.substring(2);
            if (key.endsWith(", the") || key.endsWith(",the") || key.endsWith(", an") || key.endsWith(",an")
                || key.endsWith(", a") || key.endsWith(",a"))
            {
                key = key.substring(0, key.lastIndexOf(','));
            }

            StringBuilder builder = new StringBuilder();

            for (int index = 0; index < key.length(); index++)
            {
                char character = key.charAt(index);
                if (Character.isLetterOrDigit(character)) builder.append(character);
            }

            return builder.toString();
        }

        public interface AudioColumns extends MediaColumns
        {
            String ALBUM_ID = "album_id";
            String ALBUM_KEY = "album_key";
            String ARTIST_ID = "artist_id";
            String ARTIST_KEY = "artist_key";
            String BITS_PER_SAMPLE = "bits_per_sample";
            String BOOKMARK = "bookmark";
            String GENRE = "genre";
            String GENRE_ID = "genre_id";
            String GENRE_KEY = "genre_key";
            String IS_ALARM = "is_alarm";
            String IS_AUDIOBOOK = "is_audiobook";
            String IS_MUSIC = "is_music";
            String IS_NOTIFICATION = "is_notification";
            String IS_PODCAST = "is_podcast";
            String IS_RECORDING = "is_recording";
            String IS_RINGTONE = "is_ringtone";
            String SAMPLERATE = "samplerate";
            String TITLE_KEY = "title_key";
            String TITLE_RESOURCE_URI = "title_resource_uri";
            String TRACK = "track";
            String YEAR = "year";
        }

        public static final class Media implements AudioColumns
        {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/audio";
            public static final String DEFAULT_SORT_ORDER = "title_key";
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/audio";
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri(VOLUME_EXTERNAL);
            public static final String EXTRA_MAX_BYTES = "android.provider.MediaStore.extra.MAX_BYTES";
            public static final Uri INTERNAL_CONTENT_URI = getContentUri(VOLUME_INTERNAL);
            public static final String RECORD_SOUND_ACTION = "android.provider.MediaStore.RECORD_SOUND";

            public Media()
            {
            }

            public static Uri getContentUri(String volumeName)
            {
                return volumeUri(volumeName, "audio", "media");
            }

            public static Uri getContentUri(String volumeName, long id)
            {
                return withId(getContentUri(volumeName), id);
            }

            public static Uri getContentUriForPath(String path)
            {
                File external = Environment.getExternalStorageDirectory();
                boolean onExternal = path != null && external != null && path.startsWith(external.getPath());
                return onExternal ? EXTERNAL_CONTENT_URI : INTERNAL_CONTENT_URI;
            }
        }
    }

    public static final class Images
    {
        public Images()
        {
        }

        public interface ImageColumns extends MediaColumns
        {
            String DESCRIPTION = "description";
            String EXPOSURE_TIME = "exposure_time";
            String F_NUMBER = "f_number";
            String ISO = "iso";
            String IS_PRIVATE = "isprivate";
            String LATITUDE = "latitude";
            String LONGITUDE = "longitude";
            String MINI_THUMB_MAGIC = "mini_thumb_magic";
            String PICASA_ID = "picasa_id";
            String SCENE_CAPTURE_TYPE = "scene_capture_type";
        }

        public static final class Media implements ImageColumns
        {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/image";
            public static final String DEFAULT_SORT_ORDER = "bucket_display_name";
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri(VOLUME_EXTERNAL);
            public static final Uri INTERNAL_CONTENT_URI = getContentUri(VOLUME_INTERNAL);

            public Media()
            {
            }

            public static Uri getContentUri(String volumeName)
            {
                return volumeUri(volumeName, "images", "media");
            }

            public static Uri getContentUri(String volumeName, long id)
            {
                return withId(getContentUri(volumeName), id);
            }

            public static Cursor query(ContentResolver resolver, Uri uri, String[] projection)
            {
                return resolver.query(uri, projection, null, null, DEFAULT_SORT_ORDER);
            }

            public static Cursor query(ContentResolver resolver, Uri uri, String[] projection, String where, String orderBy)
            {
                return resolver.query(uri, projection, where, null, orderBy == null ? DEFAULT_SORT_ORDER : orderBy);
            }

            public static Cursor query(ContentResolver resolver, Uri uri, String[] projection, String selection, String[] selectionArgs,
                String orderBy)
            {
                return resolver.query(uri, projection, selection, selectionArgs, orderBy == null ? DEFAULT_SORT_ORDER : orderBy);
            }

            public static Bitmap getBitmap(ContentResolver resolver, Uri url) throws FileNotFoundException, IOException
            {
                InputStream input = resolver.openInputStream(url);
                if (input == null) throw new FileNotFoundException(String.valueOf(url));

                try
                {
                    return BitmapFactory.decodeStream(input);
                }
                finally
                {
                    input.close();
                }
            }

            public static String insertImage(ContentResolver resolver, String imagePath, String name, String description)
                throws FileNotFoundException
            {
                if (imagePath == null || !new File(imagePath).isFile()) throw new FileNotFoundException(imagePath);

                Bitmap source = BitmapFactory.decodeFile(imagePath);
                if (source == null) return null;
                return insertImage(resolver, source, name, description);
            }

            public static String insertImage(ContentResolver resolver, Bitmap source, String title, String description)
            {
                if (resolver == null || source == null) return null;

                ContentValues values = new ContentValues();
                values.put(DISPLAY_NAME, title);
                values.put(TITLE, title);
                values.put(DESCRIPTION, description);
                values.put(MIME_TYPE, "image/jpeg");
                values.put(IS_PENDING, 1);

                Uri url = null;

                try
                {
                    url = resolver.insert(EXTERNAL_CONTENT_URI, values);
                    if (url == null) return null;

                    OutputStream output = resolver.openOutputStream(url);
                    if (output == null) throw new IOException("Unable to open " + url);

                    try
                    {
                        source.compress(Bitmap.CompressFormat.JPEG, 90, output);
                    }
                    finally
                    {
                        output.close();
                    }

                    values.clear();
                    values.put(IS_PENDING, 0);
                    resolver.update(url, values, null, null);
                    return url.toString();
                }
                catch (IOException | RuntimeException failure)
                {
                    if (url != null)
                    {
                        try
                        {
                            resolver.delete(url, null, null);
                        }
                        catch (RuntimeException ignored)
                        {
                        }
                    }

                    return null;
                }
            }
        }

        public static class Thumbnails implements BaseColumns
        {
            public static final String DATA = "_data";
            public static final String DEFAULT_SORT_ORDER = "image_id ASC";
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri(VOLUME_EXTERNAL);
            public static final int FULL_SCREEN_KIND = 2;
            public static final String HEIGHT = "height";
            public static final String IMAGE_ID = "image_id";
            public static final Uri INTERNAL_CONTENT_URI = getContentUri(VOLUME_INTERNAL);
            public static final String KIND = "kind";
            public static final int MICRO_KIND = 3;
            public static final int MINI_KIND = 1;
            public static final String THUMB_DATA = "thumb_data";
            public static final String WIDTH = "width";

            public Thumbnails()
            {
            }

            public static Uri getContentUri(String volumeName)
            {
                return volumeUri(volumeName, "images", "thumbnails");
            }

            public static final Cursor query(ContentResolver resolver, Uri uri, String[] projection)
            {
                return resolver.query(uri, projection, null, null, DEFAULT_SORT_ORDER);
            }

            public static final Cursor queryMiniThumbnails(ContentResolver resolver, Uri uri, int kind, String[] projection)
            {
                return resolver.query(uri, projection, KIND + " = " + kind, null, DEFAULT_SORT_ORDER);
            }

            public static final Cursor queryMiniThumbnail(ContentResolver resolver, long origId, int kind, String[] projection)
            {
                return resolver.query(EXTERNAL_CONTENT_URI, projection, IMAGE_ID + " = " + origId + " AND " + KIND + " = " + kind, null,
                    null);
            }

            public static void cancelThumbnailRequest(ContentResolver resolver, long origId)
            {
            }

            public static void cancelThumbnailRequest(ContentResolver resolver, long origId, long groupId)
            {
            }

            public static Bitmap getThumbnail(ContentResolver resolver, long origId, int kind, BitmapFactory.Options options)
            {
                return loadThumbnail(resolver, withId(Media.EXTERNAL_CONTENT_URI, origId), kind);
            }

            public static Bitmap getThumbnail(ContentResolver resolver, long origId, long groupId, int kind,
                BitmapFactory.Options options)
            {
                return getThumbnail(resolver, origId, kind, options);
            }

            public static Size getKindSize(int kind)
            {
                return kindSize(kind);
            }
        }
    }

    public static final class Video
    {
        public static final String DEFAULT_SORT_ORDER = "_display_name";

        public Video()
        {
        }

        public static Cursor query(ContentResolver resolver, Uri uri, String[] projection)
        {
            return resolver.query(uri, projection, null, null, DEFAULT_SORT_ORDER);
        }

        public interface VideoColumns extends MediaColumns
        {
            String BOOKMARK = "bookmark";
            String CATEGORY = "category";
            String COLOR_RANGE = "color_range";
            String COLOR_STANDARD = "color_standard";
            String COLOR_TRANSFER = "color_transfer";
            String DESCRIPTION = "description";
            String IS_PRIVATE = "isprivate";
            String LANGUAGE = "language";
            String LATITUDE = "latitude";
            String LONGITUDE = "longitude";
            String MINI_THUMB_MAGIC = "mini_thumb_magic";
            String TAGS = "tags";
        }

        public static final class Media implements VideoColumns
        {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/video";
            public static final String DEFAULT_SORT_ORDER = "title";
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri(VOLUME_EXTERNAL);
            public static final Uri INTERNAL_CONTENT_URI = getContentUri(VOLUME_INTERNAL);

            public Media()
            {
            }

            public static Uri getContentUri(String volumeName)
            {
                return volumeUri(volumeName, "video", "media");
            }

            public static Uri getContentUri(String volumeName, long id)
            {
                return withId(getContentUri(volumeName), id);
            }
        }

        public static class Thumbnails implements BaseColumns
        {
            public static final String DATA = "_data";
            public static final String DEFAULT_SORT_ORDER = "video_id ASC";
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri(VOLUME_EXTERNAL);
            public static final int FULL_SCREEN_KIND = 2;
            public static final String HEIGHT = "height";
            public static final Uri INTERNAL_CONTENT_URI = getContentUri(VOLUME_INTERNAL);
            public static final String KIND = "kind";
            public static final int MICRO_KIND = 3;
            public static final int MINI_KIND = 1;
            public static final String VIDEO_ID = "video_id";
            public static final String WIDTH = "width";

            public Thumbnails()
            {
            }

            public static Uri getContentUri(String volumeName)
            {
                return volumeUri(volumeName, "video", "thumbnails");
            }

            public static void cancelThumbnailRequest(ContentResolver resolver, long origId)
            {
            }

            public static void cancelThumbnailRequest(ContentResolver resolver, long origId, long groupId)
            {
            }

            public static Bitmap getThumbnail(ContentResolver resolver, long origId, int kind, BitmapFactory.Options options)
            {
                return loadThumbnail(resolver, withId(Media.EXTERNAL_CONTENT_URI, origId), kind);
            }

            public static Bitmap getThumbnail(ContentResolver resolver, long origId, long groupId, int kind,
                BitmapFactory.Options options)
            {
                return getThumbnail(resolver, origId, kind, options);
            }

            public static Size getKindSize(int kind)
            {
                return kindSize(kind);
            }
        }
    }
}
