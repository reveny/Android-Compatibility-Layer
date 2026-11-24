/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.os;



/** @hide */
public class IVold {

    public static final int FSTRIM_FLAG_DEEP_TRIM = 1;

    public static final int MOUNT_FLAG_PRIMARY = 1;
    public static final int MOUNT_FLAG_VISIBLE_FOR_READ = 2;
    public static final int MOUNT_FLAG_VISIBLE_FOR_WRITE = 4;

    public static final int PARTITION_TYPE_PUBLIC = 0;
    public static final int PARTITION_TYPE_PRIVATE = 1;
    public static final int PARTITION_TYPE_MIXED = 2;

    public static final int STORAGE_FLAG_DE = 1;
    public static final int STORAGE_FLAG_CE = 2;

    public static final int REMOUNT_MODE_NONE = 0;
    public static final int REMOUNT_MODE_DEFAULT = 1;
    public static final int REMOUNT_MODE_INSTALLER = 2;
    public static final int REMOUNT_MODE_PASS_THROUGH = 3;
    public static final int REMOUNT_MODE_ANDROID_WRITABLE = 4;

    public static final int VOLUME_STATE_UNMOUNTED = 0;
    public static final int VOLUME_STATE_CHECKING = 1;
    public static final int VOLUME_STATE_MOUNTED = 2;
    public static final int VOLUME_STATE_MOUNTED_READ_ONLY = 3;
    public static final int VOLUME_STATE_FORMATTING = 4;
    public static final int VOLUME_STATE_EJECTING = 5;
    public static final int VOLUME_STATE_UNMOUNTABLE = 6;
    public static final int VOLUME_STATE_REMOVED = 7;
    public static final int VOLUME_STATE_BAD_REMOVAL = 8;

    public static final int VOLUME_TYPE_PUBLIC = 0;
    public static final int VOLUME_TYPE_PRIVATE = 1;
    public static final int VOLUME_TYPE_EMULATED = 2;
    public static final int VOLUME_TYPE_ASEC = 3;
    public static final int VOLUME_TYPE_OBB = 4;
    public static final int VOLUME_TYPE_STUB = 5;
}
