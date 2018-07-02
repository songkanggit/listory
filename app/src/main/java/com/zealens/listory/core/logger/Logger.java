package com.zealens.listory.core.logger;


import com.zealens.listory.constant.DomainConst;

public final class Logger {

    public static final boolean S_DEBUG = DomainConst.USE_LOGGER;

    public static void i(final Class<?> _class, final String msg) {
        i(_class.getName(), msg);
    }

    public static void d(final Class<?> _class, final String msg) {
        d(_class.getName(), msg);
    }

    public static void w(final Class<?> _class, final String msg) {
        w(_class.getName(), msg);
    }

    public static void e(final Class<?> _class, final String msg) {
        e(_class.getName(), msg);
    }

    public static void v(final Class<?> _class, final String msg) {
        v(_class.getName(), msg);
    }

    public static void i(final String tag, final String msg) {
        if (S_DEBUG) {
            systemOutPrintln(tag + msg);
        }
    }

    public static void d(final String tag, final String msg) {
        if (S_DEBUG) {
            systemOutPrintln(tag + msg);
        }
    }

    public static void w(final String tag, final String msg) {
        if (S_DEBUG) {
            systemOutPrintln(tag + msg);
        }
    }

    public static void e(final String tag, final String msg) {
        if (S_DEBUG) {
            systemOutPrintln(tag + msg);
        }
    }

    public static void v(final String tag, final String msg) {
        if (S_DEBUG) {
            systemOutPrintln(tag + msg);
        }
    }

    private static void systemOutPrintln(final String str) {
        System.out.println(str);
    }
}
