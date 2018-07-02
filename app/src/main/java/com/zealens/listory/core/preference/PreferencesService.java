package com.zealens.listory.core.preference;

import java.util.HashMap;

public interface PreferencesService {
    void put(final String key, final Object value);

    <O> O get(final String key, final O defaultValue);

    void remove(final String key);

    void clear();

    boolean contains(final String key);

    HashMap<String, ?> getAll();
}
