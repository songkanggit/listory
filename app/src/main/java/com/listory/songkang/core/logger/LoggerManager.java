package com.listory.songkang.core.logger;

import android.os.Environment;
import android.util.Log;

import com.listory.songkang.constant.DomainConst;
import com.listory.songkang.core.BaseCoreManager;
import com.listory.songkang.core.CoreContext;
import com.listory.songkang.utils.FileUtil;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.intellij.lang.annotations.MagicConstant;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Kyle on 08/06/2017
 */
public class LoggerManager extends BaseCoreManager implements LoggerService {
    public final String TAG = LoggerManager.class.getSimpleName();
    private boolean mIsOpen = DomainConst.LOG_INTO_FILE_SWITCH_ON;

    private final String[] LEVEL_TAG_ARR = {
            "V",
            "D",
            "I",
            "W",
            "E",
            "A",
    };

    @MagicConstant(intValues = {LogLevel.V, LogLevel.D, LogLevel.I, LogLevel.W
            , LogLevel.E, LogLevel.A,})
    public @interface LogLevel {
        int V = 0;
        int D = 1;
        int I = 2;
        int W = 3;
        int E = 4;
        int A = 5;
    }

    private final String DIR_ROOT = DomainConst.LOG_PARENT_PATH + LoggerManager.class.getPackage().getName();
    private final String FILE_NAME_EXTENSION_LOG = ".log";
    private CoreContext mCoreContext;
    private File mLogFileDir;

    public LoggerManager(CoreContext coreContext) {
        mCoreContext = coreContext;
        Assert.assertEquals(LogLevel.class.getFields().length, LEVEL_TAG_ARR.length);
    }

    @Override
    public void initialize() {
        super.initialize();
        mLogFileDir = Environment.getExternalStoragePublicDirectory(DIR_ROOT);
        if (mLogFileDir.exists()) {
            if (FileUtils.sizeOfDirectory(mLogFileDir) > DomainConst.LOG_DIR_SIZE_LIMIT) {
                try {
                    FileUtils.cleanDirectory(mLogFileDir);
                } catch (IOException ignore) {
                    ignore.printStackTrace();
                }
            }
        }
    }

    @SuppressWarnings("MagicConstant")
    @Override
    public int order() {
        return ORDER.LOGGER | ORDER.POST_MASK;
    }

    @Override
    public void freeMemory() {

    }

    private void execute(Runnable runnable) {
        if (runnable == null) return;

        mCoreContext.executeAsyncTaskOnQueue(runnable);
    }

    @Override
    public void v(Object... msgs) {
        printGeneralHandle(LogLevel.V, msgs);
    }

    @Override
    public void d(Object... msgs) {
        printGeneralHandle(LogLevel.D, msgs);
    }

    @Override
    public void i(Object... msgs) {
        printGeneralHandle(LogLevel.I, msgs);
    }

    @Override
    public void w(Object... msgs) {
        printGeneralHandle(LogLevel.W, msgs);
    }

    @Override
    public void e(Object... msgs) {
        printGeneralHandle(LogLevel.E, msgs);
    }

    @Override
    public void a(Object... msgs) {
        printGeneralHandle(LogLevel.A, msgs);
    }

    private void printGeneralHandle(@LogLevel int index, Object... msgs) {
        if (msgs == null || msgs.length == 0) return;
        String tag = msgs[0].toString();
        StringBuilder sb = new StringBuilder();
        for (int i = 1, len = msgs.length; i < len; i++)
            if (msgs[i] != null) sb.append(msgs[i]).append(" ");
        levelDistinguishLogProcess(index, tag, sb.toString());
    }

    private void levelDistinguishLogProcess(@LogLevel int index, String tag, String str) {
        switch (index) {
            case LogLevel.V:
                Log.v(tag, str);
                break;
            case LogLevel.D:
                Log.d(tag, str);
                break;
            case LogLevel.I:
                Log.i(tag, str);
                break;
            case LogLevel.W:
                Log.w(tag, str);
                break;
            case LogLevel.E:
                Log.e(tag, str);
                break;
            case LogLevel.A:
                Log.e(tag, str);
                break;
            default:
                break;
        }

        if (!mIsOpen) return;
        writeToFile(LEVEL_TAG_ARR[index], tag, str, null);
    }

    public boolean getDebug() {
        return mIsOpen;
    }

    @Override
    public void setDebug(boolean isOpen) {
        mIsOpen = isOpen;
    }

    /**
     * 将日志信息写到文件里
     *
     * @param levelD 日志级别
     * @param tag    日志标记
     * @param msg    日志内容
     * @param object 保留字段
     */
    private void writeToFile(final String levelD, final String tag, final String msg, Object object) {
        execute(() -> {
            long timeMilli = System.currentTimeMillis();
            String time = getCurrentTime(DomainConst.TIME_FORMAT_LOG_MSG_START_APPEND, timeMilli);
            String str = time + "\t" + timeMilli + "\t" + levelD + ":[" + tag + "]- " + msg +
                    System.getProperties().getProperty("line.separator");
            File file = new File(isInitialized() ? mLogFileDir : Environment.getExternalStoragePublicDirectory(DIR_ROOT)
                    , "log-" + getCurrentTime(DomainConst.TIME_FORMAT_FILE_NAME, timeMilli)
                    + FILE_NAME_EXTENSION_LOG);
            FileUtil.writeStringToFile(file, str);
        });
    }

    public String getCurrentTime(String format, long time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.SIMPLIFIED_CHINESE);
        return dateFormat.format(new Date(time));
    }
}
