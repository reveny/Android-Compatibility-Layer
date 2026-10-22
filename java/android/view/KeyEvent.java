package android.view;

import android.os.Parcel;
import android.os.Parcelable;

public class KeyEvent extends InputEvent implements Parcelable
{
    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP = 1;
    public static final int ACTION_MULTIPLE = 2;

    public static final int KEYCODE_UNKNOWN = 0;
    public static final int KEYCODE_SOFT_LEFT = 1;
    public static final int KEYCODE_SOFT_RIGHT = 2;
    public static final int KEYCODE_HOME = 3;
    public static final int KEYCODE_BACK = 4;
    public static final int KEYCODE_CALL = 5;
    public static final int KEYCODE_ENDCALL = 6;
    public static final int KEYCODE_0 = 7;
    public static final int KEYCODE_1 = 8;
    public static final int KEYCODE_2 = 9;
    public static final int KEYCODE_3 = 10;
    public static final int KEYCODE_4 = 11;
    public static final int KEYCODE_5 = 12;
    public static final int KEYCODE_6 = 13;
    public static final int KEYCODE_7 = 14;
    public static final int KEYCODE_8 = 15;
    public static final int KEYCODE_9 = 16;
    public static final int KEYCODE_STAR = 17;
    public static final int KEYCODE_POUND = 18;
    public static final int KEYCODE_DPAD_UP = 19;
    public static final int KEYCODE_DPAD_DOWN = 20;
    public static final int KEYCODE_DPAD_LEFT = 21;
    public static final int KEYCODE_DPAD_RIGHT = 22;
    public static final int KEYCODE_DPAD_CENTER = 23;
    public static final int KEYCODE_VOLUME_UP = 24;
    public static final int KEYCODE_VOLUME_DOWN = 25;
    public static final int KEYCODE_POWER = 26;
    public static final int KEYCODE_CAMERA = 27;
    public static final int KEYCODE_CLEAR = 28;
    public static final int KEYCODE_A = 29;
    public static final int KEYCODE_B = 30;
    public static final int KEYCODE_C = 31;
    public static final int KEYCODE_D = 32;
    public static final int KEYCODE_E = 33;
    public static final int KEYCODE_F = 34;
    public static final int KEYCODE_G = 35;
    public static final int KEYCODE_H = 36;
    public static final int KEYCODE_I = 37;
    public static final int KEYCODE_J = 38;
    public static final int KEYCODE_K = 39;
    public static final int KEYCODE_L = 40;
    public static final int KEYCODE_M = 41;
    public static final int KEYCODE_N = 42;
    public static final int KEYCODE_O = 43;
    public static final int KEYCODE_P = 44;
    public static final int KEYCODE_Q = 45;
    public static final int KEYCODE_R = 46;
    public static final int KEYCODE_S = 47;
    public static final int KEYCODE_T = 48;
    public static final int KEYCODE_U = 49;
    public static final int KEYCODE_V = 50;
    public static final int KEYCODE_W = 51;
    public static final int KEYCODE_X = 52;
    public static final int KEYCODE_Y = 53;
    public static final int KEYCODE_Z = 54;
    public static final int KEYCODE_COMMA = 55;
    public static final int KEYCODE_PERIOD = 56;
    public static final int KEYCODE_ALT_LEFT = 57;
    public static final int KEYCODE_ALT_RIGHT = 58;
    public static final int KEYCODE_SHIFT_LEFT = 59;
    public static final int KEYCODE_SHIFT_RIGHT = 60;
    public static final int KEYCODE_TAB = 61;
    public static final int KEYCODE_SPACE = 62;
    public static final int KEYCODE_SYM = 63;
    public static final int KEYCODE_EXPLORER = 64;
    public static final int KEYCODE_ENVELOPE = 65;
    public static final int KEYCODE_ENTER = 66;
    public static final int KEYCODE_DEL = 67;
    public static final int KEYCODE_GRAVE = 68;
    public static final int KEYCODE_MINUS = 69;
    public static final int KEYCODE_EQUALS = 70;
    public static final int KEYCODE_LEFT_BRACKET = 71;
    public static final int KEYCODE_RIGHT_BRACKET = 72;
    public static final int KEYCODE_BACKSLASH = 73;
    public static final int KEYCODE_SEMICOLON = 74;
    public static final int KEYCODE_APOSTROPHE = 75;
    public static final int KEYCODE_SLASH = 76;
    public static final int KEYCODE_AT = 77;
    public static final int KEYCODE_NUM = 78;
    public static final int KEYCODE_HEADSETHOOK = 79;
    public static final int KEYCODE_FOCUS = 80;
    public static final int KEYCODE_PLUS = 81;
    public static final int KEYCODE_MENU = 82;
    public static final int KEYCODE_NOTIFICATION = 83;
    public static final int KEYCODE_SEARCH = 84;
    public static final int KEYCODE_MEDIA_PLAY_PAUSE = 85;
    public static final int KEYCODE_MEDIA_STOP = 86;
    public static final int KEYCODE_MEDIA_NEXT = 87;
    public static final int KEYCODE_MEDIA_PREVIOUS = 88;
    public static final int KEYCODE_MEDIA_REWIND = 89;
    public static final int KEYCODE_MEDIA_FAST_FORWARD = 90;
    public static final int KEYCODE_MUTE = 91;
    public static final int KEYCODE_PAGE_UP = 92;
    public static final int KEYCODE_PAGE_DOWN = 93;
    public static final int KEYCODE_PICTSYMBOLS = 94;
    public static final int KEYCODE_SWITCH_CHARSET = 95;
    public static final int KEYCODE_BUTTON_A = 96;
    public static final int KEYCODE_BUTTON_B = 97;
    public static final int KEYCODE_BUTTON_C = 98;
    public static final int KEYCODE_BUTTON_X = 99;
    public static final int KEYCODE_BUTTON_Y = 100;
    public static final int KEYCODE_BUTTON_Z = 101;
    public static final int KEYCODE_BUTTON_L1 = 102;
    public static final int KEYCODE_BUTTON_R1 = 103;
    public static final int KEYCODE_BUTTON_L2 = 104;
    public static final int KEYCODE_BUTTON_R2 = 105;
    public static final int KEYCODE_BUTTON_THUMBL = 106;
    public static final int KEYCODE_BUTTON_THUMBR = 107;
    public static final int KEYCODE_BUTTON_START = 108;
    public static final int KEYCODE_BUTTON_SELECT = 109;
    public static final int KEYCODE_BUTTON_MODE = 110;
    public static final int KEYCODE_ESCAPE = 111;
    public static final int KEYCODE_FORWARD_DEL = 112;
    public static final int KEYCODE_CTRL_LEFT = 113;
    public static final int KEYCODE_CTRL_RIGHT = 114;
    public static final int KEYCODE_CAPS_LOCK = 115;
    public static final int KEYCODE_SCROLL_LOCK = 116;
    public static final int KEYCODE_META_LEFT = 117;
    public static final int KEYCODE_META_RIGHT = 118;
    public static final int KEYCODE_FUNCTION = 119;
    public static final int KEYCODE_SYSRQ = 120;
    public static final int KEYCODE_BREAK = 121;
    public static final int KEYCODE_MOVE_HOME = 122;
    public static final int KEYCODE_MOVE_END = 123;
    public static final int KEYCODE_INSERT = 124;
    public static final int KEYCODE_FORWARD = 125;
    public static final int KEYCODE_MEDIA_PLAY = 126;
    public static final int KEYCODE_MEDIA_PAUSE = 127;
    public static final int KEYCODE_MEDIA_CLOSE = 128;
    public static final int KEYCODE_MEDIA_EJECT = 129;
    public static final int KEYCODE_MEDIA_RECORD = 130;
    public static final int KEYCODE_F1 = 131;
    public static final int KEYCODE_F2 = 132;
    public static final int KEYCODE_F3 = 133;
    public static final int KEYCODE_F4 = 134;
    public static final int KEYCODE_F5 = 135;
    public static final int KEYCODE_F6 = 136;
    public static final int KEYCODE_F7 = 137;
    public static final int KEYCODE_F8 = 138;
    public static final int KEYCODE_F9 = 139;
    public static final int KEYCODE_F10 = 140;
    public static final int KEYCODE_F11 = 141;
    public static final int KEYCODE_F12 = 142;
    public static final int KEYCODE_NUM_LOCK = 143;
    public static final int KEYCODE_NUMPAD_0 = 144;
    public static final int KEYCODE_NUMPAD_1 = 145;
    public static final int KEYCODE_NUMPAD_2 = 146;
    public static final int KEYCODE_NUMPAD_3 = 147;
    public static final int KEYCODE_NUMPAD_4 = 148;
    public static final int KEYCODE_NUMPAD_5 = 149;
    public static final int KEYCODE_NUMPAD_6 = 150;
    public static final int KEYCODE_NUMPAD_7 = 151;
    public static final int KEYCODE_NUMPAD_8 = 152;
    public static final int KEYCODE_NUMPAD_9 = 153;
    public static final int KEYCODE_NUMPAD_DIVIDE = 154;
    public static final int KEYCODE_NUMPAD_MULTIPLY = 155;
    public static final int KEYCODE_NUMPAD_SUBTRACT = 156;
    public static final int KEYCODE_NUMPAD_ADD = 157;
    public static final int KEYCODE_NUMPAD_DOT = 158;
    public static final int KEYCODE_NUMPAD_COMMA = 159;
    public static final int KEYCODE_NUMPAD_ENTER = 160;
    public static final int KEYCODE_NUMPAD_EQUALS = 161;
    public static final int KEYCODE_NUMPAD_LEFT_PAREN = 162;
    public static final int KEYCODE_NUMPAD_RIGHT_PAREN = 163;
    public static final int KEYCODE_VOLUME_MUTE = 164;
    public static final int KEYCODE_INFO = 165;
    public static final int KEYCODE_CHANNEL_UP = 166;
    public static final int KEYCODE_CHANNEL_DOWN = 167;
    public static final int KEYCODE_ZOOM_IN = 168;
    public static final int KEYCODE_ZOOM_OUT = 169;
    public static final int KEYCODE_TV = 170;
    public static final int KEYCODE_WINDOW = 171;
    public static final int KEYCODE_GUIDE = 172;
    public static final int KEYCODE_DVR = 173;
    public static final int KEYCODE_BOOKMARK = 174;
    public static final int KEYCODE_CAPTIONS = 175;
    public static final int KEYCODE_SETTINGS = 176;
    public static final int KEYCODE_TV_POWER = 177;
    public static final int KEYCODE_TV_INPUT = 178;
    public static final int KEYCODE_STB_POWER = 179;
    public static final int KEYCODE_STB_INPUT = 180;
    public static final int KEYCODE_AVR_POWER = 181;
    public static final int KEYCODE_AVR_INPUT = 182;
    public static final int KEYCODE_PROG_RED = 183;
    public static final int KEYCODE_PROG_GREEN = 184;
    public static final int KEYCODE_PROG_YELLOW = 185;
    public static final int KEYCODE_PROG_BLUE = 186;
    public static final int KEYCODE_APP_SWITCH = 187;
    public static final int KEYCODE_BUTTON_1 = 188;
    public static final int KEYCODE_BUTTON_2 = 189;
    public static final int KEYCODE_BUTTON_3 = 190;
    public static final int KEYCODE_BUTTON_4 = 191;
    public static final int KEYCODE_BUTTON_5 = 192;
    public static final int KEYCODE_BUTTON_6 = 193;
    public static final int KEYCODE_BUTTON_7 = 194;
    public static final int KEYCODE_BUTTON_8 = 195;
    public static final int KEYCODE_BUTTON_9 = 196;
    public static final int KEYCODE_BUTTON_10 = 197;
    public static final int KEYCODE_BUTTON_11 = 198;
    public static final int KEYCODE_BUTTON_12 = 199;
    public static final int KEYCODE_BUTTON_13 = 200;
    public static final int KEYCODE_BUTTON_14 = 201;
    public static final int KEYCODE_BUTTON_15 = 202;
    public static final int KEYCODE_BUTTON_16 = 203;
    public static final int KEYCODE_LANGUAGE_SWITCH = 204;
    public static final int KEYCODE_MANNER_MODE = 205;
    public static final int KEYCODE_3D_MODE = 206;
    public static final int KEYCODE_CONTACTS = 207;
    public static final int KEYCODE_CALENDAR = 208;
    public static final int KEYCODE_MUSIC = 209;
    public static final int KEYCODE_CALCULATOR = 210;
    public static final int KEYCODE_ZENKAKU_HANKAKU = 211;
    public static final int KEYCODE_EISU = 212;
    public static final int KEYCODE_MUHENKAN = 213;
    public static final int KEYCODE_HENKAN = 214;
    public static final int KEYCODE_KATAKANA_HIRAGANA = 215;
    public static final int KEYCODE_YEN = 216;
    public static final int KEYCODE_RO = 217;
    public static final int KEYCODE_KANA = 218;
    public static final int KEYCODE_ASSIST = 219;
    public static final int KEYCODE_BRIGHTNESS_DOWN = 220;
    public static final int KEYCODE_BRIGHTNESS_UP = 221;
    public static final int KEYCODE_MEDIA_AUDIO_TRACK = 222;
    public static final int KEYCODE_SLEEP = 223;
    public static final int KEYCODE_WAKEUP = 224;
    public static final int KEYCODE_PAIRING = 225;
    public static final int KEYCODE_MEDIA_TOP_MENU = 226;
    public static final int KEYCODE_11 = 227;
    public static final int KEYCODE_12 = 228;
    public static final int KEYCODE_LAST_CHANNEL = 229;
    public static final int KEYCODE_TV_DATA_SERVICE = 230;
    public static final int KEYCODE_VOICE_ASSIST = 231;
    public static final int KEYCODE_TV_RADIO_SERVICE = 232;
    public static final int KEYCODE_TV_TELETEXT = 233;
    public static final int KEYCODE_TV_NUMBER_ENTRY = 234;
    public static final int KEYCODE_TV_TERRESTRIAL_ANALOG = 235;
    public static final int KEYCODE_TV_TERRESTRIAL_DIGITAL = 236;
    public static final int KEYCODE_TV_SATELLITE = 237;
    public static final int KEYCODE_TV_SATELLITE_BS = 238;
    public static final int KEYCODE_TV_SATELLITE_CS = 239;
    public static final int KEYCODE_TV_SATELLITE_SERVICE = 240;
    public static final int KEYCODE_TV_NETWORK = 241;
    public static final int KEYCODE_TV_ANTENNA_CABLE = 242;
    public static final int KEYCODE_TV_INPUT_HDMI_1 = 243;
    public static final int KEYCODE_TV_INPUT_HDMI_2 = 244;
    public static final int KEYCODE_TV_INPUT_HDMI_3 = 245;
    public static final int KEYCODE_TV_INPUT_HDMI_4 = 246;
    public static final int KEYCODE_TV_INPUT_COMPOSITE_1 = 247;
    public static final int KEYCODE_TV_INPUT_COMPOSITE_2 = 248;
    public static final int KEYCODE_TV_INPUT_COMPONENT_1 = 249;
    public static final int KEYCODE_TV_INPUT_COMPONENT_2 = 250;
    public static final int KEYCODE_TV_INPUT_VGA_1 = 251;
    public static final int KEYCODE_TV_AUDIO_DESCRIPTION = 252;
    public static final int KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP = 253;
    public static final int KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN = 254;
    public static final int KEYCODE_TV_ZOOM_MODE = 255;
    public static final int KEYCODE_TV_CONTENTS_MENU = 256;
    public static final int KEYCODE_TV_MEDIA_CONTEXT_MENU = 257;
    public static final int KEYCODE_TV_TIMER_PROGRAMMING = 258;
    public static final int KEYCODE_HELP = 259;
    public static final int KEYCODE_NAVIGATE_PREVIOUS = 260;
    public static final int KEYCODE_NAVIGATE_NEXT = 261;
    public static final int KEYCODE_NAVIGATE_IN = 262;
    public static final int KEYCODE_NAVIGATE_OUT = 263;
    public static final int KEYCODE_STEM_PRIMARY = 264;
    public static final int KEYCODE_STEM_1 = 265;
    public static final int KEYCODE_STEM_2 = 266;
    public static final int KEYCODE_STEM_3 = 267;
    public static final int KEYCODE_DPAD_UP_LEFT = 268;
    public static final int KEYCODE_DPAD_DOWN_LEFT = 269;
    public static final int KEYCODE_DPAD_UP_RIGHT = 270;
    public static final int KEYCODE_DPAD_DOWN_RIGHT = 271;
    public static final int KEYCODE_MEDIA_SKIP_FORWARD = 272;
    public static final int KEYCODE_MEDIA_SKIP_BACKWARD = 273;
    public static final int KEYCODE_MEDIA_STEP_FORWARD = 274;
    public static final int KEYCODE_MEDIA_STEP_BACKWARD = 275;
    public static final int KEYCODE_SOFT_SLEEP = 276;
    public static final int KEYCODE_CUT = 277;
    public static final int KEYCODE_COPY = 278;
    public static final int KEYCODE_PASTE = 279;
    public static final int KEYCODE_SYSTEM_NAVIGATION_UP = 280;
    public static final int KEYCODE_SYSTEM_NAVIGATION_DOWN = 281;
    public static final int KEYCODE_SYSTEM_NAVIGATION_LEFT = 282;
    public static final int KEYCODE_SYSTEM_NAVIGATION_RIGHT = 283;
    public static final int KEYCODE_ALL_APPS = 284;
    public static final int KEYCODE_REFRESH = 285;
    public static final int KEYCODE_THUMBS_UP = 286;
    public static final int KEYCODE_THUMBS_DOWN = 287;
    public static final int KEYCODE_PROFILE_SWITCH = 288;
    public static final int KEYCODE_VIDEO_APP_1 = 289;
    public static final int KEYCODE_VIDEO_APP_2 = 290;
    public static final int KEYCODE_VIDEO_APP_3 = 291;
    public static final int KEYCODE_VIDEO_APP_4 = 292;
    public static final int KEYCODE_VIDEO_APP_5 = 293;
    public static final int KEYCODE_VIDEO_APP_6 = 294;
    public static final int KEYCODE_VIDEO_APP_7 = 295;
    public static final int KEYCODE_VIDEO_APP_8 = 296;
    public static final int KEYCODE_FEATURED_APP_1 = 297;
    public static final int KEYCODE_FEATURED_APP_2 = 298;
    public static final int KEYCODE_FEATURED_APP_3 = 299;
    public static final int KEYCODE_FEATURED_APP_4 = 300;
    public static final int KEYCODE_DEMO_APP_1 = 301;
    public static final int KEYCODE_DEMO_APP_2 = 302;
    public static final int KEYCODE_DEMO_APP_3 = 303;
    public static final int KEYCODE_DEMO_APP_4 = 304;
    public static final int KEYCODE_KEYBOARD_BACKLIGHT_DOWN = 305;
    public static final int KEYCODE_KEYBOARD_BACKLIGHT_UP = 306;
    public static final int KEYCODE_KEYBOARD_BACKLIGHT_TOGGLE = 307;
    public static final int KEYCODE_STYLUS_BUTTON_PRIMARY = 308;
    public static final int KEYCODE_STYLUS_BUTTON_SECONDARY = 309;
    public static final int KEYCODE_STYLUS_BUTTON_TERTIARY = 310;
    public static final int KEYCODE_STYLUS_BUTTON_TAIL = 311;
    public static final int KEYCODE_RECENT_APPS = 312;
    public static final int KEYCODE_MACRO_1 = 313;
    public static final int KEYCODE_MACRO_2 = 314;
    public static final int KEYCODE_MACRO_3 = 315;
    public static final int KEYCODE_MACRO_4 = 316;
    public static final int KEYCODE_EMOJI_PICKER = 317;
    public static final int KEYCODE_SCREENSHOT = 318;
    public static final int KEYCODE_DICTATE = 319;
    public static final int KEYCODE_NEW = 320;
    public static final int KEYCODE_CLOSE = 321;
    public static final int KEYCODE_DO_NOT_DISTURB = 322;
    public static final int KEYCODE_PRINT = 323;
    public static final int KEYCODE_LOCK = 324;
    public static final int KEYCODE_FULLSCREEN = 325;
    public static final int KEYCODE_F13 = 326;
    public static final int KEYCODE_F14 = 327;
    public static final int KEYCODE_F15 = 328;
    public static final int KEYCODE_F16 = 329;
    public static final int KEYCODE_F17 = 330;
    public static final int KEYCODE_F18 = 331;
    public static final int KEYCODE_F19 = 332;
    public static final int KEYCODE_F20 = 333;
    public static final int KEYCODE_F21 = 334;
    public static final int KEYCODE_F22 = 335;
    public static final int KEYCODE_F23 = 336;
    public static final int KEYCODE_F24 = 337;

    @Deprecated
    public static final int MAX_KEYCODE = 84;

    public static final int META_SHIFT_ON = 0x1;
    public static final int META_ALT_ON = 0x2;
    public static final int META_SYM_ON = 0x4;
    public static final int META_FUNCTION_ON = 0x8;
    public static final int META_ALT_LEFT_ON = 0x10;
    public static final int META_ALT_RIGHT_ON = 0x20;
    public static final int META_SHIFT_LEFT_ON = 0x40;
    public static final int META_SHIFT_RIGHT_ON = 0x80;
    public static final int META_CTRL_ON = 0x1000;
    public static final int META_CTRL_LEFT_ON = 0x2000;
    public static final int META_CTRL_RIGHT_ON = 0x4000;
    public static final int META_META_ON = 0x10000;
    public static final int META_META_LEFT_ON = 0x20000;
    public static final int META_META_RIGHT_ON = 0x40000;
    public static final int META_CAPS_LOCK_ON = 0x100000;
    public static final int META_NUM_LOCK_ON = 0x200000;
    public static final int META_SCROLL_LOCK_ON = 0x400000;
    public static final int META_SHIFT_MASK = META_SHIFT_ON | META_SHIFT_LEFT_ON | META_SHIFT_RIGHT_ON;
    public static final int META_ALT_MASK = META_ALT_ON | META_ALT_LEFT_ON | META_ALT_RIGHT_ON;
    public static final int META_CTRL_MASK = META_CTRL_ON | META_CTRL_LEFT_ON | META_CTRL_RIGHT_ON;
    public static final int META_META_MASK = META_META_ON | META_META_LEFT_ON | META_META_RIGHT_ON;

    public static final int FLAG_WOKE_HERE = 0x1;
    public static final int FLAG_SOFT_KEYBOARD = 0x2;
    public static final int FLAG_KEEP_TOUCH_MODE = 0x4;
    public static final int FLAG_FROM_SYSTEM = 0x8;
    public static final int FLAG_EDITOR_ACTION = 0x10;
    public static final int FLAG_CANCELED = 0x20;
    public static final int FLAG_VIRTUAL_HARD_KEY = 0x40;
    public static final int FLAG_LONG_PRESS = 0x80;
    public static final int FLAG_CANCELED_LONG_PRESS = 0x100;
    public static final int FLAG_TRACKING = 0x200;
    public static final int FLAG_FALLBACK = 0x400;

    private static final int MODIFIER_META_STATE_MASK = META_SHIFT_MASK | META_ALT_MASK | META_CTRL_MASK | META_META_MASK | META_SYM_ON
                                                       | META_FUNCTION_ON;
    private static final int LOCK_META_STATE_MASK = META_CAPS_LOCK_ON | META_NUM_LOCK_ON | META_SCROLL_LOCK_ON;
    private static final int FLAG_START_TRACKING_INTERNAL = 0x40000000;
    private static final String KEYCODE_PREFIX = "KEYCODE_";

    private static final String[] KEYCODE_NAMES = {
        "UNKNOWN", "SOFT_LEFT", "SOFT_RIGHT", "HOME", "BACK", "CALL", "ENDCALL", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "STAR",
        "POUND", "DPAD_UP", "DPAD_DOWN", "DPAD_LEFT", "DPAD_RIGHT", "DPAD_CENTER", "VOLUME_UP", "VOLUME_DOWN", "POWER", "CAMERA",
        "CLEAR", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X",
        "Y", "Z", "COMMA", "PERIOD", "ALT_LEFT", "ALT_RIGHT", "SHIFT_LEFT", "SHIFT_RIGHT", "TAB", "SPACE", "SYM", "EXPLORER",
        "ENVELOPE", "ENTER", "DEL", "GRAVE", "MINUS", "EQUALS", "LEFT_BRACKET", "RIGHT_BRACKET", "BACKSLASH", "SEMICOLON",
        "APOSTROPHE", "SLASH", "AT", "NUM", "HEADSETHOOK", "FOCUS", "PLUS", "MENU", "NOTIFICATION", "SEARCH", "MEDIA_PLAY_PAUSE",
        "MEDIA_STOP", "MEDIA_NEXT", "MEDIA_PREVIOUS", "MEDIA_REWIND", "MEDIA_FAST_FORWARD", "MUTE", "PAGE_UP", "PAGE_DOWN",
        "PICTSYMBOLS", "SWITCH_CHARSET", "BUTTON_A", "BUTTON_B", "BUTTON_C", "BUTTON_X", "BUTTON_Y", "BUTTON_Z", "BUTTON_L1",
        "BUTTON_R1", "BUTTON_L2", "BUTTON_R2", "BUTTON_THUMBL", "BUTTON_THUMBR", "BUTTON_START", "BUTTON_SELECT", "BUTTON_MODE",
        "ESCAPE", "FORWARD_DEL", "CTRL_LEFT", "CTRL_RIGHT", "CAPS_LOCK", "SCROLL_LOCK", "META_LEFT", "META_RIGHT", "FUNCTION", "SYSRQ",
        "BREAK", "MOVE_HOME", "MOVE_END", "INSERT", "FORWARD", "MEDIA_PLAY", "MEDIA_PAUSE", "MEDIA_CLOSE", "MEDIA_EJECT",
        "MEDIA_RECORD", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12", "NUM_LOCK", "NUMPAD_0", "NUMPAD_1",
        "NUMPAD_2", "NUMPAD_3", "NUMPAD_4", "NUMPAD_5", "NUMPAD_6", "NUMPAD_7", "NUMPAD_8", "NUMPAD_9", "NUMPAD_DIVIDE",
        "NUMPAD_MULTIPLY", "NUMPAD_SUBTRACT", "NUMPAD_ADD", "NUMPAD_DOT", "NUMPAD_COMMA", "NUMPAD_ENTER", "NUMPAD_EQUALS",
        "NUMPAD_LEFT_PAREN", "NUMPAD_RIGHT_PAREN", "VOLUME_MUTE", "INFO", "CHANNEL_UP", "CHANNEL_DOWN", "ZOOM_IN", "ZOOM_OUT", "TV",
        "WINDOW", "GUIDE", "DVR", "BOOKMARK", "CAPTIONS", "SETTINGS", "TV_POWER", "TV_INPUT", "STB_POWER", "STB_INPUT", "AVR_POWER",
        "AVR_INPUT", "PROG_RED", "PROG_GREEN", "PROG_YELLOW", "PROG_BLUE", "APP_SWITCH", "BUTTON_1", "BUTTON_2", "BUTTON_3",
        "BUTTON_4", "BUTTON_5", "BUTTON_6", "BUTTON_7", "BUTTON_8", "BUTTON_9", "BUTTON_10", "BUTTON_11", "BUTTON_12", "BUTTON_13",
        "BUTTON_14", "BUTTON_15", "BUTTON_16", "LANGUAGE_SWITCH", "MANNER_MODE", "3D_MODE", "CONTACTS", "CALENDAR", "MUSIC",
        "CALCULATOR", "ZENKAKU_HANKAKU", "EISU", "MUHENKAN", "HENKAN", "KATAKANA_HIRAGANA", "YEN", "RO", "KANA", "ASSIST",
        "BRIGHTNESS_DOWN", "BRIGHTNESS_UP", "MEDIA_AUDIO_TRACK", "SLEEP", "WAKEUP", "PAIRING", "MEDIA_TOP_MENU", "11", "12",
        "LAST_CHANNEL", "TV_DATA_SERVICE", "VOICE_ASSIST", "TV_RADIO_SERVICE", "TV_TELETEXT", "TV_NUMBER_ENTRY",
        "TV_TERRESTRIAL_ANALOG", "TV_TERRESTRIAL_DIGITAL", "TV_SATELLITE", "TV_SATELLITE_BS", "TV_SATELLITE_CS",
        "TV_SATELLITE_SERVICE", "TV_NETWORK", "TV_ANTENNA_CABLE", "TV_INPUT_HDMI_1", "TV_INPUT_HDMI_2", "TV_INPUT_HDMI_3",
        "TV_INPUT_HDMI_4", "TV_INPUT_COMPOSITE_1", "TV_INPUT_COMPOSITE_2", "TV_INPUT_COMPONENT_1", "TV_INPUT_COMPONENT_2",
        "TV_INPUT_VGA_1", "TV_AUDIO_DESCRIPTION", "TV_AUDIO_DESCRIPTION_MIX_UP", "TV_AUDIO_DESCRIPTION_MIX_DOWN", "TV_ZOOM_MODE",
        "TV_CONTENTS_MENU", "TV_MEDIA_CONTEXT_MENU", "TV_TIMER_PROGRAMMING", "HELP", "NAVIGATE_PREVIOUS", "NAVIGATE_NEXT",
        "NAVIGATE_IN", "NAVIGATE_OUT", "STEM_PRIMARY", "STEM_1", "STEM_2", "STEM_3", "DPAD_UP_LEFT", "DPAD_DOWN_LEFT", "DPAD_UP_RIGHT",
        "DPAD_DOWN_RIGHT", "MEDIA_SKIP_FORWARD", "MEDIA_SKIP_BACKWARD", "MEDIA_STEP_FORWARD", "MEDIA_STEP_BACKWARD", "SOFT_SLEEP",
        "CUT", "COPY", "PASTE", "SYSTEM_NAVIGATION_UP", "SYSTEM_NAVIGATION_DOWN", "SYSTEM_NAVIGATION_LEFT", "SYSTEM_NAVIGATION_RIGHT",
        "ALL_APPS", "REFRESH", "THUMBS_UP", "THUMBS_DOWN", "PROFILE_SWITCH", "VIDEO_APP_1", "VIDEO_APP_2", "VIDEO_APP_3",
        "VIDEO_APP_4", "VIDEO_APP_5", "VIDEO_APP_6", "VIDEO_APP_7", "VIDEO_APP_8", "FEATURED_APP_1", "FEATURED_APP_2",
        "FEATURED_APP_3", "FEATURED_APP_4", "DEMO_APP_1", "DEMO_APP_2", "DEMO_APP_3", "DEMO_APP_4", "KEYBOARD_BACKLIGHT_DOWN",
        "KEYBOARD_BACKLIGHT_UP", "KEYBOARD_BACKLIGHT_TOGGLE", "STYLUS_BUTTON_PRIMARY", "STYLUS_BUTTON_SECONDARY",
        "STYLUS_BUTTON_TERTIARY", "STYLUS_BUTTON_TAIL", "RECENT_APPS", "MACRO_1", "MACRO_2", "MACRO_3", "MACRO_4", "EMOJI_PICKER",
        "SCREENSHOT", "DICTATE", "NEW", "CLOSE", "DO_NOT_DISTURB", "PRINT", "LOCK", "FULLSCREEN", "F13", "F14", "F15", "F16", "F17",
        "F18", "F19", "F20", "F21", "F22", "F23", "F24"
    };

    public static final Parcelable.Creator<KeyEvent> CREATOR = new Parcelable.Creator<KeyEvent>()
    {
        @Override
        public KeyEvent createFromParcel(Parcel source)
        {
            source.readInt();
            return createFromParcelBody(source);
        }

        @Override
        public KeyEvent[] newArray(int size)
        {
            return new KeyEvent[size];
        }
    };

    public interface Callback
    {
        boolean onKeyDown(int keyCode, KeyEvent event);

        boolean onKeyLongPress(int keyCode, KeyEvent event);

        boolean onKeyUp(int keyCode, KeyEvent event);

        boolean onKeyMultiple(int keyCode, int count, KeyEvent event);
    }

    public static class DispatcherState
    {
        private int mDownKeyCode;
        private Object mDownTarget;
        private final android.util.SparseIntArray mActiveLongPresses = new android.util.SparseIntArray();

        public DispatcherState()
        {
        }

        public void reset()
        {
            mDownKeyCode = 0;
            mDownTarget = null;
            mActiveLongPresses.clear();
        }

        public void reset(Object target)
        {
            if (mDownTarget != target) return;

            mDownKeyCode = 0;
            mDownTarget = null;
        }

        public void startTracking(KeyEvent event, Object target)
        {
            if (event.getAction() != ACTION_DOWN) throw new IllegalArgumentException("Can only start tracking on a down event");

            mDownKeyCode = event.getKeyCode();
            mDownTarget = target;
        }

        public boolean isTracking(KeyEvent event)
        {
            return mDownKeyCode == event.getKeyCode();
        }

        public void performedLongPress(KeyEvent event)
        {
            mActiveLongPresses.put(event.getKeyCode(), 1);
        }

        public void handleUpEvent(KeyEvent event)
        {
            int keyCode = event.getKeyCode();
            int index = mActiveLongPresses.indexOfKey(keyCode);

            if (index >= 0)
            {
                event.mFlags |= FLAG_CANCELED | FLAG_CANCELED_LONG_PRESS;
                mActiveLongPresses.removeAt(index);
            }

            if (mDownKeyCode == keyCode)
            {
                event.mFlags |= FLAG_TRACKING;
                mDownKeyCode = 0;
                mDownTarget = null;
            }
        }
    }

    private int mAction;
    private int mKeyCode;
    private int mRepeat;
    private int mMetaState;
    private long mDownTime;
    private long mEventTime;
    private int mScanCode;
    private int mFlags;
    private int mSource;
    private int mDeviceId;
    private String mCharacters;

    public KeyEvent(int action, int code)
    {
        this(0L, 0L, action, code, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD);
    }

    public KeyEvent(long downTime, long eventTime, int action, int code, int repeat)
    {
        this(downTime, eventTime, action, code, repeat, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD);
    }

    public KeyEvent(long downTime, long eventTime, int action, int code, int repeat, int metaState)
    {
        this(downTime, eventTime, action, code, repeat, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD);
    }

    public KeyEvent(long downTime, long eventTime, int action, int code, int repeat, int metaState, int deviceId, int scanCode)
    {
        this(downTime, eventTime, action, code, repeat, metaState, deviceId, scanCode, 0, InputDevice.SOURCE_KEYBOARD);
    }

    public KeyEvent(long downTime, long eventTime, int action, int code, int repeat, int metaState, int deviceId, int scanCode,
                    int flags)
    {
        this(downTime, eventTime, action, code, repeat, metaState, deviceId, scanCode, flags, InputDevice.SOURCE_KEYBOARD);
    }

    public KeyEvent(long downTime, long eventTime, int action, int code, int repeat, int metaState, int deviceId, int scanCode,
                    int flags, int source)
    {
        mDownTime = downTime;
        mEventTime = eventTime;
        mAction = action;
        mKeyCode = code;
        mRepeat = repeat;
        mMetaState = metaState;
        mDeviceId = deviceId;
        mScanCode = scanCode;
        mFlags = flags;
        mSource = source;
    }

    public KeyEvent(long time, String characters, int deviceId, int flags)
    {
        this(time, time, ACTION_MULTIPLE, KEYCODE_UNKNOWN, 0, 0, deviceId, 0, flags, InputDevice.SOURCE_KEYBOARD);
        mCharacters = characters;
    }

    public KeyEvent(KeyEvent origin)
    {
        this(origin.mDownTime, origin.mEventTime, origin.mAction, origin.mKeyCode, origin.mRepeat, origin.mMetaState, origin.mDeviceId,
             origin.mScanCode, origin.mFlags, origin.mSource);
        mCharacters = origin.mCharacters;
    }

    @Deprecated
    public KeyEvent(KeyEvent origin, long eventTime, int newRepeat)
    {
        this(origin);
        mEventTime = eventTime;
        mRepeat = newRepeat;
    }

    public static int getMaxKeyCode()
    {
        return KEYCODE_NAMES.length - 1;
    }

    public static int getDeadChar(int accent, int character)
    {
        return 0;
    }

    public static KeyEvent changeTimeRepeat(KeyEvent event, long eventTime, int newRepeat)
    {
        return new KeyEvent(event, eventTime, newRepeat);
    }

    public static KeyEvent changeTimeRepeat(KeyEvent event, long eventTime, int newRepeat, int newFlags)
    {
        KeyEvent changed = new KeyEvent(event, eventTime, newRepeat);
        changed.mFlags = newFlags;
        return changed;
    }

    public static KeyEvent changeAction(KeyEvent event, int action)
    {
        KeyEvent changed = new KeyEvent(event);
        changed.mAction = action;
        return changed;
    }

    public static KeyEvent changeFlags(KeyEvent event, int flags)
    {
        KeyEvent changed = new KeyEvent(event);
        changed.mFlags = flags;
        return changed;
    }

    public final boolean isSystem()
    {
        return isSystemKey(mKeyCode);
    }

    private static boolean isSystemKey(int keyCode)
    {
        switch (keyCode)
        {
            case KEYCODE_MENU:
            case KEYCODE_SOFT_RIGHT:
            case KEYCODE_HOME:
            case KEYCODE_BACK:
            case KEYCODE_CALL:
            case KEYCODE_ENDCALL:
            case KEYCODE_VOLUME_UP:
            case KEYCODE_VOLUME_DOWN:
            case KEYCODE_VOLUME_MUTE:
            case KEYCODE_MUTE:
            case KEYCODE_POWER:
            case KEYCODE_HEADSETHOOK:
            case KEYCODE_MEDIA_PLAY:
            case KEYCODE_MEDIA_PAUSE:
            case KEYCODE_MEDIA_PLAY_PAUSE:
            case KEYCODE_MEDIA_STOP:
            case KEYCODE_MEDIA_NEXT:
            case KEYCODE_MEDIA_PREVIOUS:
            case KEYCODE_MEDIA_REWIND:
            case KEYCODE_MEDIA_RECORD:
            case KEYCODE_MEDIA_FAST_FORWARD:
            case KEYCODE_CAMERA:
            case KEYCODE_FOCUS:
            case KEYCODE_SEARCH:
            case KEYCODE_BRIGHTNESS_DOWN:
            case KEYCODE_BRIGHTNESS_UP:
            case KEYCODE_MEDIA_AUDIO_TRACK:
                return true;
            default:
                return false;
        }
    }

    public static final boolean isGamepadButton(int keyCode)
    {
        return (keyCode >= KEYCODE_BUTTON_A && keyCode <= KEYCODE_BUTTON_MODE)
               || (keyCode >= KEYCODE_BUTTON_1 && keyCode <= KEYCODE_BUTTON_16);
    }

    public static final boolean isMediaSessionKey(int keyCode)
    {
        switch (keyCode)
        {
            case KEYCODE_MEDIA_PLAY:
            case KEYCODE_MEDIA_PAUSE:
            case KEYCODE_MEDIA_PLAY_PAUSE:
            case KEYCODE_MUTE:
            case KEYCODE_HEADSETHOOK:
            case KEYCODE_MEDIA_STOP:
            case KEYCODE_MEDIA_NEXT:
            case KEYCODE_MEDIA_PREVIOUS:
            case KEYCODE_MEDIA_REWIND:
            case KEYCODE_MEDIA_RECORD:
            case KEYCODE_MEDIA_FAST_FORWARD:
                return true;
            default:
                return false;
        }
    }

    @Override
    public final int getDeviceId()
    {
        return mDeviceId;
    }

    @Override
    public final int getSource()
    {
        return mSource;
    }

    public final void setSource(int source)
    {
        mSource = source;
    }

    public final int getMetaState()
    {
        return mMetaState;
    }

    public final int getModifiers()
    {
        return normalizeMetaState(mMetaState) & MODIFIER_META_STATE_MASK;
    }

    public final int getFlags()
    {
        return mFlags;
    }

    public static int getModifierMetaStateMask()
    {
        return MODIFIER_META_STATE_MASK;
    }

    public static boolean isModifierKey(int keyCode)
    {
        switch (keyCode)
        {
            case KEYCODE_SHIFT_LEFT:
            case KEYCODE_SHIFT_RIGHT:
            case KEYCODE_ALT_LEFT:
            case KEYCODE_ALT_RIGHT:
            case KEYCODE_CTRL_LEFT:
            case KEYCODE_CTRL_RIGHT:
            case KEYCODE_META_LEFT:
            case KEYCODE_META_RIGHT:
            case KEYCODE_SYM:
            case KEYCODE_NUM:
            case KEYCODE_FUNCTION:
                return true;
            default:
                return false;
        }
    }

    public static int normalizeMetaState(int metaState)
    {
        if ((metaState & (META_SHIFT_LEFT_ON | META_SHIFT_RIGHT_ON)) != 0) metaState |= META_SHIFT_ON;
        if ((metaState & (META_ALT_LEFT_ON | META_ALT_RIGHT_ON)) != 0) metaState |= META_ALT_ON;
        if ((metaState & (META_CTRL_LEFT_ON | META_CTRL_RIGHT_ON)) != 0) metaState |= META_CTRL_ON;
        if ((metaState & (META_META_LEFT_ON | META_META_RIGHT_ON)) != 0) metaState |= META_META_ON;

        return metaState & (MODIFIER_META_STATE_MASK | LOCK_META_STATE_MASK);
    }

    public static boolean metaStateHasNoModifiers(int metaState)
    {
        return (normalizeMetaState(metaState) & MODIFIER_META_STATE_MASK) == 0;
    }

    public static boolean metaStateHasModifiers(int metaState, int modifiers)
    {
        if ((modifiers & LOCK_META_STATE_MASK) != 0) throw new IllegalArgumentException("modifiers must not contain lock keys");

        metaState = normalizeMetaState(metaState) & MODIFIER_META_STATE_MASK;
        return modifierGroupMatches(metaState, modifiers, META_SHIFT_ON, META_SHIFT_LEFT_ON, META_SHIFT_RIGHT_ON)
               && modifierGroupMatches(metaState, modifiers, META_ALT_ON, META_ALT_LEFT_ON, META_ALT_RIGHT_ON)
               && modifierGroupMatches(metaState, modifiers, META_CTRL_ON, META_CTRL_LEFT_ON, META_CTRL_RIGHT_ON)
               && modifierGroupMatches(metaState, modifiers, META_META_ON, META_META_LEFT_ON, META_META_RIGHT_ON)
               && (metaState & (META_SYM_ON | META_FUNCTION_ON)) == (modifiers & (META_SYM_ON | META_FUNCTION_ON));
    }

    private static boolean modifierGroupMatches(int metaState, int modifiers, int basic, int left, int right)
    {
        int group = basic | left | right;
        int wanted = modifiers & group;

        if ((wanted & basic) != 0) return (metaState & basic) != 0 && (metaState & (left | right) & ~wanted & ~basic) == 0;

        return (metaState & group) == wanted;
    }

    public final boolean hasNoModifiers()
    {
        return metaStateHasNoModifiers(mMetaState);
    }

    public final boolean hasModifiers(int modifiers)
    {
        return metaStateHasModifiers(mMetaState, modifiers);
    }

    public final boolean isAltPressed()
    {
        return (mMetaState & META_ALT_ON) != 0;
    }

    public final boolean isShiftPressed()
    {
        return (mMetaState & META_SHIFT_ON) != 0;
    }

    public final boolean isSymPressed()
    {
        return (mMetaState & META_SYM_ON) != 0;
    }

    public final boolean isCtrlPressed()
    {
        return (mMetaState & META_CTRL_ON) != 0;
    }

    public final boolean isMetaPressed()
    {
        return (mMetaState & META_META_ON) != 0;
    }

    public final boolean isFunctionPressed()
    {
        return (mMetaState & META_FUNCTION_ON) != 0;
    }

    public final boolean isCapsLockOn()
    {
        return (mMetaState & META_CAPS_LOCK_ON) != 0;
    }

    public final boolean isNumLockOn()
    {
        return (mMetaState & META_NUM_LOCK_ON) != 0;
    }

    public final boolean isScrollLockOn()
    {
        return (mMetaState & META_SCROLL_LOCK_ON) != 0;
    }

    public final int getAction()
    {
        return mAction;
    }

    public final boolean isCanceled()
    {
        return (mFlags & FLAG_CANCELED) != 0;
    }

    public final void startTracking()
    {
        mFlags |= FLAG_START_TRACKING_INTERNAL;
    }

    public final boolean isTracking()
    {
        return (mFlags & FLAG_TRACKING) != 0;
    }

    public final boolean isLongPress()
    {
        return (mFlags & FLAG_LONG_PRESS) != 0;
    }

    public final int getKeyCode()
    {
        return mKeyCode;
    }

    public final String getCharacters()
    {
        return mCharacters;
    }

    public final int getScanCode()
    {
        return mScanCode;
    }

    public final int getRepeatCount()
    {
        return mRepeat;
    }

    public final long getDownTime()
    {
        return mDownTime;
    }

    @Override
    public final long getEventTime()
    {
        return mEventTime;
    }

    public final KeyCharacterMap getKeyCharacterMap()
    {
        return KeyCharacterMap.load(mDeviceId);
    }

    public char getDisplayLabel()
    {
        int unicode = getUnicodeChar(0);
        return (char)Character.toUpperCase(unicode);
    }

    public int getUnicodeChar()
    {
        return getUnicodeChar(mMetaState);
    }

    public int getUnicodeChar(int metaState)
    {
        return KeyCharacterMap.unicodeFor(mKeyCode, metaState);
    }

    public boolean getKeyData(KeyCharacterMap.KeyData results)
    {
        return getKeyCharacterMap().getKeyData(mKeyCode, results);
    }

    public char getMatch(char[] chars)
    {
        return getMatch(chars, 0);
    }

    public char getMatch(char[] chars, int metaState)
    {
        return getKeyCharacterMap().getMatch(mKeyCode, chars, metaState);
    }

    public char getNumber()
    {
        return getKeyCharacterMap().getNumber(mKeyCode);
    }

    public boolean isPrintingKey()
    {
        return getUnicodeChar(0) != 0;
    }

    @Deprecated
    public final boolean dispatch(Callback receiver)
    {
        return dispatch(receiver, null, null);
    }

    public final boolean dispatch(Callback receiver, DispatcherState state, Object target)
    {
        switch (mAction)
        {
            case ACTION_DOWN:
            {
                mFlags &= ~FLAG_START_TRACKING_INTERNAL;
                boolean result = receiver.onKeyDown(mKeyCode, this);

                if (state != null)
                {
                    if (result && mRepeat == 0 && (mFlags & FLAG_START_TRACKING_INTERNAL) != 0)
                    {
                        state.startTracking(this, target);
                    }
                    else if (isLongPress() && state.isTracking(this))
                    {
                        if (receiver.onKeyLongPress(mKeyCode, this))
                        {
                            state.performedLongPress(this);
                            result = true;
                        }
                    }
                }

                return result;
            }

            case ACTION_UP:
                if (state != null) state.handleUpEvent(this);
                return receiver.onKeyUp(mKeyCode, this);

            case ACTION_MULTIPLE:
            {
                int count = mRepeat;
                int code = mKeyCode;

                if (receiver.onKeyMultiple(code, count, this)) return true;
                if (code == KEYCODE_UNKNOWN) return false;

                mAction = ACTION_DOWN;
                mRepeat = 0;
                boolean handled = receiver.onKeyDown(code, this);

                if (handled)
                {
                    mAction = ACTION_UP;
                    receiver.onKeyUp(code, this);
                }

                mAction = ACTION_MULTIPLE;
                mRepeat = count;
                return handled;
            }

            default:
                return false;
        }
    }

    public static String keyCodeToString(int keyCode)
    {
        if (keyCode >= 0 && keyCode < KEYCODE_NAMES.length) return KEYCODE_PREFIX + KEYCODE_NAMES[keyCode];
        return Integer.toString(keyCode);
    }

    private static final String[] META_SYMBOLIC_NAMES = {"META_SHIFT_ON", "META_ALT_ON", "META_SYM_ON", "META_FUNCTION_ON",
                                                         "META_ALT_LEFT_ON", "META_ALT_RIGHT_ON", "META_SHIFT_LEFT_ON",
                                                         "META_SHIFT_RIGHT_ON", "META_CAP_LOCKED", "META_ALT_LOCKED", "META_SYM_LOCKED",
                                                         "0x00000800", "META_CTRL_ON", "META_CTRL_LEFT_ON", "META_CTRL_RIGHT_ON",
                                                         "0x00008000", "META_META_ON", "META_META_LEFT_ON", "META_META_RIGHT_ON",
                                                         "0x00080000", "META_CAPS_LOCK_ON", "META_NUM_LOCK_ON", "META_SCROLL_LOCK_ON"};

    public static String metaStateToString(int metaState)
    {
        if (metaState == 0) return "0";

        StringBuilder result = null;
        int index = 0;

        while (metaState != 0)
        {
            boolean isSet = (metaState & 1) != 0;
            metaState >>>= 1;

            if (isSet)
            {
                String name = index < META_SYMBOLIC_NAMES.length ? META_SYMBOLIC_NAMES[index]
                                                                 : "0x" + String.format("%08x", 1 << index);

                if (result == null)
                {
                    if (metaState == 0) return name;
                    result = new StringBuilder(name);
                }
                else
                {
                    result.append('|').append(name);
                }
            }

            index++;
        }

        return result.toString();
    }

    public static int keyCodeFromString(String symbolicName)
    {
        if (symbolicName == null) throw new IllegalArgumentException("symbolicName must not be null");

        String name = symbolicName.startsWith(KEYCODE_PREFIX) ? symbolicName.substring(KEYCODE_PREFIX.length()) : symbolicName;

        for (int index = 0; index < KEYCODE_NAMES.length; index++)
        {
            if (KEYCODE_NAMES[index].equals(name)) return index;
        }

        try
        {
            return Integer.parseInt(symbolicName);
        }
        catch (NumberFormatException malformed)
        {
            return KEYCODE_UNKNOWN;
        }
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    static KeyEvent createFromParcelBody(Parcel source)
    {
        source.readInt();
        int deviceId = source.readInt();
        int inputSource = source.readInt();
        source.readInt();
        source.createByteArray();
        int action = source.readInt();
        int keyCode = source.readInt();
        int repeat = source.readInt();
        int metaState = source.readInt();
        int scanCode = source.readInt();
        int flags = source.readInt();
        long downTime = source.readLong();
        long eventTime = source.readLong();

        KeyEvent event = new KeyEvent(downTime, eventTime, action, keyCode, repeat, metaState, deviceId, scanCode, flags, inputSource);
        event.mCharacters = source.readString();
        return event;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeInt(PARCEL_TOKEN_KEY_EVENT);
        out.writeInt(0);
        out.writeInt(mDeviceId);
        out.writeInt(mSource);
        out.writeInt(0);
        out.writeByteArray(null);
        out.writeInt(mAction);
        out.writeInt(mKeyCode);
        out.writeInt(mRepeat);
        out.writeInt(mMetaState);
        out.writeInt(mScanCode);
        out.writeInt(mFlags);
        out.writeLong(mDownTime);
        out.writeLong(mEventTime);
        out.writeString(mCharacters);
    }

    private static final int PARCEL_TOKEN_KEY_EVENT = 2;

    @Override
    public String toString()
    {
        return "KeyEvent { action=" + (mAction == ACTION_DOWN ? "ACTION_DOWN" : mAction == ACTION_UP ? "ACTION_UP" : "ACTION_MULTIPLE")
               + ", keyCode=" + keyCodeToString(mKeyCode) + ", metaState=" + mMetaState + ", flags=0x" + Integer.toHexString(mFlags)
               + ", repeatCount=" + mRepeat + ", eventTime=" + mEventTime + ", downTime=" + mDownTime + ", deviceId=" + mDeviceId
               + ", source=0x" + Integer.toHexString(mSource) + " }";
    }
}
