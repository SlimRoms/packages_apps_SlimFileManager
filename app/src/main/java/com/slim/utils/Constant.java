package com.slim.utils;

import org.mozilla.universalchardet.Constants;

public class Constant {

    public static final String[] ENCODINGS = new String[]{
            Constants.CHARSET_BIG5,
            Constants.CHARSET_EUC_JP,
            Constants.CHARSET_EUC_KR,
            Constants.CHARSET_EUC_TW,
            Constants.CHARSET_GB18030,
            "GB2312",
            Constants.CHARSET_IBM855,
            Constants.CHARSET_IBM866,
            Constants.CHARSET_ISO_2022_CN,
            Constants.CHARSET_ISO_2022_JP,
            Constants.CHARSET_ISO_2022_KR,
            "ISO-8859-2",
            Constants.CHARSET_ISO_8859_5,
            Constants.CHARSET_ISO_8859_7,
            Constants.CHARSET_ISO_8859_8,
            Constants.CHARSET_KOI8_R,
            Constants.CHARSET_MACCYRILLIC,
            Constants.CHARSET_SHIFT_JIS,
            Constants.CHARSET_UTF_16BE,
            Constants.CHARSET_UTF_16LE,
            Constants.CHARSET_UTF_32BE,
            Constants.CHARSET_UTF_32LE,
            Constants.CHARSET_UTF_8,
            "UTF-16",
            Constants.CHARSET_WINDOWS_1251,
            Constants.CHARSET_WINDOWS_1252,
            Constants.CHARSET_WINDOWS_1253,
            Constants.CHARSET_WINDOWS_1255
    };

    public static final String DEFAULT_ENCODING = Constants.CHARSET_UTF_8;
}
