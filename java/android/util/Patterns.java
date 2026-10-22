package android.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Patterns
{
    public static final String TOP_LEVEL_DOMAIN_STR = "((aero|arpa|asia|a[cdefgilmnoqrstuwxz])|(biz|b[abdefghijmnorstvwyz])|(cat|com|coop|c[acdfghiklmnoruvxyz])|d[ejkmoz]|(edu|e[cegrstu])|f[ijkmor]|(gov|g[abdefghilmnpqrstuwy])|h[kmnrtu]|(info|int|i[delmnoqrst])|(jobs|j[emop])|k[eghimnprwyz]|l[abcikrstuvy]|(mil|mobi|museum|m[acdeghklmnopqrstuvwxyz])|(name|net|n[acefgilopruz])|(org|om)|(pro|p[aefghklmnrstwy])|qa|r[eosuw]|s[abcdeghijklmnortuvyz]|(tel|travel|t[cdfghjklmnoprtvwz])|u[agksyz]|v[aceginu]|w[fs]|(δοκιμή|испытание|рф|срб|טעסט|آزمایشی|إختبار|الاردن|الجزائر|السعودية|المغرب|امارات|بھارت|تونس|سورية|فلسطين|قطر|مصر|परीक्षा|भारत|ভারত|ਭਾਰਤ|ભારત|இந்தியா|இலங்கை|சிங்கப்பூர்|பரிட்சை|భారత్|ලංකා|ไทย|テスト|中国|中國|台湾|台灣|新加坡|测试|測試|香港|테스트|한국|xn\\-\\-0zwm56d|xn\\-\\-11b5bs3a9aj6g|xn\\-\\-3e0b707e|xn\\-\\-45brj9c|xn\\-\\-80akhbyknj4f|xn\\-\\-90a3ac|xn\\-\\-9t4b11yi5a|xn\\-\\-clchc0ea0b2g2a9gcd|xn\\-\\-deba0ad|xn\\-\\-fiqs8s|xn\\-\\-fiqz9s|xn\\-\\-fpcrj9c3d|xn\\-\\-fzc2c9e2c|xn\\-\\-g6w251d|xn\\-\\-gecrj9c|xn\\-\\-h2brj9c|xn\\-\\-hgbk6aj7f53bba|xn\\-\\-hlcj6aya9esc7a|xn\\-\\-j6w193g|xn\\-\\-jxalpdlp|xn\\-\\-kgbechtv|xn\\-\\-kprw13d|xn\\-\\-kpry57d|xn\\-\\-lgbbat1ad8j|xn\\-\\-mgbaam7a8h|xn\\-\\-mgbayh7gpa|xn\\-\\-mgbbh1a71e|xn\\-\\-mgbc0a9azcg|xn\\-\\-mgberp4a5d4ar|xn\\-\\-o3cw4h|xn\\-\\-ogbpf8fl|xn\\-\\-p1ai|xn\\-\\-pgbs0dh|xn\\-\\-s9brj9c|xn\\-\\-wgbh1c|xn\\-\\-wgbl6a|xn\\-\\-xkc2al3hye2a|xn\\-\\-xkc2dl3a5ee0h|xn\\-\\-yfro4i67o|xn\\-\\-ygbi2ammx|xn\\-\\-zckzah|xxx)|y[et]|z[amw])";

    public static final Pattern TOP_LEVEL_DOMAIN = Pattern.compile(TOP_LEVEL_DOMAIN_STR);

    public static final String TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL = "(?:(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])|(?:biz|b[abdefghijmnorstvwyz])|(?:cat|com|coop|c[acdfghiklmnoruvxyz])|d[ejkmoz]|(?:edu|e[cegrstu])|f[ijkmor]|(?:gov|g[abdefghilmnpqrstuwy])|h[kmnrtu]|(?:info|int|i[delmnoqrst])|(?:jobs|j[emop])|k[eghimnprwyz]|l[abcikrstuvy]|(?:mil|mobi|museum|m[acdeghklmnopqrstuvwxyz])|(?:name|net|n[acefgilopruz])|(?:org|om)|(?:pro|p[aefghklmnrstwy])|qa|r[eosuw]|s[abcdeghijklmnortuvyz]|(?:tel|travel|t[cdfghjklmnoprtvwz])|u[agksyz]|v[aceginu]|w[fs]|(?:δοκιμή|испытание|рф|срб|טעסט|آزمایشی|إختبار|الاردن|الجزائر|السعودية|المغرب|امارات|بھارت|تونس|سورية|فلسطين|قطر|مصر|परीक्षा|भारत|ভারত|ਭਾਰਤ|ભારત|இந்தியா|இலங்கை|சிங்கப்பூர்|பரிட்சை|భారత్|ලංකා|ไทย|テスト|中国|中國|台湾|台灣|新加坡|测试|測試|香港|테스트|한국|xn\\-\\-0zwm56d|xn\\-\\-11b5bs3a9aj6g|xn\\-\\-3e0b707e|xn\\-\\-45brj9c|xn\\-\\-80akhbyknj4f|xn\\-\\-90a3ac|xn\\-\\-9t4b11yi5a|xn\\-\\-clchc0ea0b2g2a9gcd|xn\\-\\-deba0ad|xn\\-\\-fiqs8s|xn\\-\\-fiqz9s|xn\\-\\-fpcrj9c3d|xn\\-\\-fzc2c9e2c|xn\\-\\-g6w251d|xn\\-\\-gecrj9c|xn\\-\\-h2brj9c|xn\\-\\-hgbk6aj7f53bba|xn\\-\\-hlcj6aya9esc7a|xn\\-\\-j6w193g|xn\\-\\-jxalpdlp|xn\\-\\-kgbechtv|xn\\-\\-kprw13d|xn\\-\\-kpry57d|xn\\-\\-lgbbat1ad8j|xn\\-\\-mgbaam7a8h|xn\\-\\-mgbayh7gpa|xn\\-\\-mgbbh1a71e|xn\\-\\-mgbc0a9azcg|xn\\-\\-mgberp4a5d4ar|xn\\-\\-o3cw4h|xn\\-\\-ogbpf8fl|xn\\-\\-p1ai|xn\\-\\-pgbs0dh|xn\\-\\-s9brj9c|xn\\-\\-wgbh1c|xn\\-\\-wgbl6a|xn\\-\\-xkc2al3hye2a|xn\\-\\-xkc2dl3a5ee0h|xn\\-\\-yfro4i67o|xn\\-\\-ygbi2ammx|xn\\-\\-zckzah|xxx)|y[et]|z[amw]))";

    public static final String GOOD_IRI_CHAR = "a-zA-Z0-9 -퟿豈-﷏ﷰ-￯";

    private static final String IP_ADDRESS_STRING = "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                                                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                                                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                                                    + "|[1-9][0-9]|[0-9]))";

    public static final Pattern IP_ADDRESS = Pattern.compile(IP_ADDRESS_STRING);

    private static final String UCS_CHAR = "[ -퟿豈-﷏ﷰ-￯"
                                           + "𐀀-🿽𠀀-𯿽𰀀-𿿽"
                                           + "񀀀-񏿽񐀀-񟿽񠀀-񯿽"
                                           + "񰀀-񿿽򀀀-򏿽򐀀-򟿽"
                                           + "򠀀-򯿽򰀀-򿿽󀀀-󏿽"
                                           + "󐀀-󟿽󡀀-󯿽"
                                           + "&&[^ [ - ]   　]]";

    private static final String LABEL_CHAR = "a-zA-Z0-9" + UCS_CHAR;

    private static final String TLD_CHAR = "a-zA-Z" + UCS_CHAR;

    private static final String IRI_LABEL = "[" + LABEL_CHAR + "](?:[" + LABEL_CHAR + "_\\-]{0,61}[" + LABEL_CHAR + "]){0,1}";

    private static final String PUNYCODE_TLD = "xn\\-\\-[\\w\\-]{0,58}\\w";

    private static final String TLD = "(" + PUNYCODE_TLD + "|" + "[" + TLD_CHAR + "]{2,63}" + ")";

    private static final String HOST_NAME = "(" + IRI_LABEL + "\\.)+" + TLD;

    private static final String DOMAIN_NAME_STR = "(" + HOST_NAME + "|" + IP_ADDRESS_STRING + ")";

    public static final Pattern DOMAIN_NAME = Pattern.compile(DOMAIN_NAME_STR);

    private static final String PROTOCOL = "(?i:http|https|rtsp|ftp)://";

    private static final String WORD_BOUNDARY = "(?:\\b|$|^)";

    private static final String USER_INFO = "(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
                                            + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
                                            + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@";

    private static final String PORT_NUMBER = "\\:\\d{1,5}";

    private static final String PATH_AND_QUERY = "[/\\?](?:(?:[" + LABEL_CHAR + ";/\\?:@&=#~"
                                                 + "\\-\\.\\+!\\*'\\(\\),_\\$])|(?:%[a-fA-F0-9]{2}))*";

    public static final Pattern WEB_URL = Pattern.compile("(" + "(" + "(?:" + PROTOCOL + "(?:" + USER_INFO + ")?" + ")?" + "(?:"
                                                          + DOMAIN_NAME_STR + ")" + "(?:" + PORT_NUMBER + ")?" + ")" + "("
                                                          + PATH_AND_QUERY + ")?" + WORD_BOUNDARY + ")");

    private static final String STRICT_TLD = "(?:" + PUNYCODE_TLD + "|(?:" + TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL + ")";

    private static final String STRICT_HOST_NAME = "(?:(?:" + IRI_LABEL + "\\.)+" + STRICT_TLD + ")";

    private static final String STRICT_DOMAIN_NAME = "(?:" + STRICT_HOST_NAME + "|" + IP_ADDRESS_STRING + ")";

    private static final String RELAXED_DOMAIN_NAME = "(?:" + "(?:" + IRI_LABEL + "(?:\\.(?=\\S))" + "?)+" + "|"
                                                      + IP_ADDRESS_STRING + ")";

    private static final String WEB_URL_WITHOUT_PROTOCOL = "(" + WORD_BOUNDARY + "(?<!:\\/\\/)" + "(" + "(?:" + STRICT_DOMAIN_NAME
                                                           + ")" + "(?:" + PORT_NUMBER + ")?" + ")" + "(?:" + PATH_AND_QUERY
                                                           + ")?" + WORD_BOUNDARY + ")";

    private static final String WEB_URL_WITH_PROTOCOL = "(" + WORD_BOUNDARY + "(?:" + "(?:" + PROTOCOL + "(?:" + USER_INFO + ")?"
                                                        + ")" + "(?:" + RELAXED_DOMAIN_NAME + ")?" + "(?:" + PORT_NUMBER + ")?"
                                                        + ")" + "(?:" + PATH_AND_QUERY + ")?" + WORD_BOUNDARY + ")";

    public static final Pattern AUTOLINK_WEB_URL = Pattern.compile("(" + WEB_URL_WITH_PROTOCOL + "|" + WEB_URL_WITHOUT_PROTOCOL + ")");

    private static final String EMAIL_CHAR = LABEL_CHAR + "\\+\\-_%'";

    private static final String EMAIL_ADDRESS_LOCAL_PART = "[" + EMAIL_CHAR + "]" + "(?:[" + EMAIL_CHAR + "\\.]{0,62}[" + EMAIL_CHAR
                                                           + "])?";

    private static final String EMAIL_ADDRESS_DOMAIN = "(?=.{1,255}(?:\\s|$|^))" + HOST_NAME;

    public static final Pattern AUTOLINK_EMAIL_ADDRESS = Pattern.compile("(" + WORD_BOUNDARY + "(?:" + EMAIL_ADDRESS_LOCAL_PART + "@"
                                                                  + EMAIL_ADDRESS_DOMAIN + ")" + WORD_BOUNDARY + ")");

    public static final Pattern EMAIL_ADDRESS = Pattern.compile("[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" + "\\@"
                                                                + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" + "(" + "\\."
                                                                + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" + ")+");

    public static final Pattern PHONE = Pattern.compile("(\\+[0-9]+[\\- \\.]*)?" + "(\\([0-9]+\\)[\\- \\.]*)?"
                                                        + "([0-9][0-9\\- \\.]+[0-9])");

    private Patterns()
    {
    }

    public static final String concatGroups(Matcher matcher)
    {
        StringBuilder builder = new StringBuilder();
        int count = matcher.groupCount();

        for (int index = 1; index <= count; index++)
        {
            String group = matcher.group(index);
            if (group != null) builder.append(group);
        }

        return builder.toString();
    }

    public static final String digitsAndPlusOnly(Matcher matcher)
    {
        StringBuilder builder = new StringBuilder();
        String matching = matcher.group();

        for (int index = 0, size = matching.length(); index < size; index++)
        {
            char character = matching.charAt(index);
            if (character == '+' || Character.isDigit(character)) builder.append(character);
        }

        return builder.toString();
    }
}
