package com.listory.songkang.core.logger;

/**
 * Created by Kyle on 08/06/2017
 */

public interface LoggerService {
    void v(Object... msgs);

    void d(Object... msgs);

    void i(Object... msgs);

    void w(Object... msgs);

    void e(Object... msgs);

    void a(Object... msgs);

    void setDebug(boolean isOpen);
}
