package com.listory.songkang.core.preference;

import android.content.Context;
import android.content.SharedPreferences;

import com.listory.songkang.DomainConst;
import com.listory.songkang.core.CoreContext;
import com.listory.songkang.core.CoreManager;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public final class PreferencesManager extends CoreManager implements PreferencesService {

    private WeakReference<SharedPreferences> mSharedPreferences;

    public PreferencesManager(final CoreContext coreContext) {
        super(coreContext);
    }

    @Override
    public void initialize() {
        super.initialize();

        mSharedPreferences = new WeakReference<>(mContext.getSharedPreferences(DomainConst.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE));
    }

    @Override
    public void dispose() {
        mSharedPreferences = null;

        super.dispose();
    }

    @Override
    public void freeMemory() {

    }

    @Override
    public int order() {
        return ORDER.PREFERENCES;
    }

    @Override
    public void put(final String key, final Object value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else {
            editor.putString(key, value.toString());
        }
        SharedPreferencesCompat.apply(editor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <O> O get(final String key, final O defaultValue) {
        if (defaultValue instanceof String)
            return (O) getSharedPreferences().getString(key, (String) defaultValue);
        else if (defaultValue instanceof Integer)
            return (O) (Integer) getSharedPreferences().getInt(key, (Integer) defaultValue);
        else if (defaultValue instanceof Boolean)
            return (O) (Boolean) getSharedPreferences().getBoolean(key, (Boolean) defaultValue);
        else if (defaultValue instanceof Float)
            return (O) (Float) getSharedPreferences().getFloat(key, (Float) defaultValue);
        else if (defaultValue instanceof Long)
            return (O) (Long) getSharedPreferences().getLong(key, (Long) defaultValue);
        return null;
    }

    @Override
    public void remove(final String key) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.remove(key);
        SharedPreferencesCompat.apply(editor);
    }

    @Override
    public void clear() {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.clear();
        SharedPreferencesCompat.apply(editor);
    }

    @Override
    public boolean contains(final String key) {
        return getSharedPreferences().contains(key);
    }

    @Override
    public HashMap<String, ?> getAll() {
        return (HashMap<String, ?>) getSharedPreferences().getAll();
    }

    private SharedPreferences getSharedPreferences() {
        synchronized (PreferencesManager.class) {
            if(mSharedPreferences == null || mSharedPreferences.get() == null) {
                final Context context = mCoreContext.getApplicationContext();
                mSharedPreferences = new WeakReference<>(context.getSharedPreferences(DomainConst.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE));
            }
            return mSharedPreferences.get();
        }
    }

    private static class SharedPreferencesCompat {
        private static final Method sApplyMethod = findApplyMethod();

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static Method findApplyMethod() {
            try {
                Class clz = SharedPreferences.Editor.class;
                return clz.getMethod("apply");
            } catch (NoSuchMethodException ignored) {
            }

            return null;
        }

        public static void apply(SharedPreferences.Editor editor) {
            try {
                if (sApplyMethod != null) {
                    sApplyMethod.invoke(editor);
                    return;
                }
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException ignored) {
            }
            editor.commit();
        }
    }
}
