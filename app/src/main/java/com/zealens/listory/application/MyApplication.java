package com.zealens.listory.application;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.HandlerThread;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.zealens.listory.core.CoreApplication;
import com.zealens.listory.core.CoreContext;
import com.zealens.listory.core.connection.NetworkManager;
import com.zealens.listory.core.download.DownLoadManager;
import com.zealens.listory.core.http.HttpManager;
import com.zealens.listory.core.logger.LoggerManager;
import com.zealens.listory.core.preference.PreferencesManager;
import com.squareup.leakcanary.LeakCanary;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by songkang on 2017/7/2.
 */

public class MyApplication extends CoreApplication {
    private static final String TAG = MyApplication.class.getSimpleName();
    private static final int DEFAULT_THREAD_POOL_SIZE = 4;
    private final AtomicInteger mThreadId = new AtomicInteger(1);

    private static final int PROCESS_MAIN = 0;
    private static final int PROCESS_SERVICE = 1;
    private static final int PROCESS_DEAMON_MIN = 10;
    private static final int PROCESS_DEAMON_1 = 11;
    private static final int PROCESS_DEAMON_2 = 12;
    private static final int PROCESS_DEAMON_3 = 13;
    private static final int PROCESS_DEAMON_MAX = 20;

    private int mProcessType;

    @Override
    public void onCreate() {
        super.onCreate();
        //之所以把tinker的初始化放在super.onCreate()之前
        //是因为在父类初始化之前初始化才能完成对application的修改
//        if(BuildConfig.TINKER_ENABLE) {
//            // 我们可以从这里获得Tinker加载过程的信息
//            ApplicationLike tinkerApplicationLike = TinkerPatchApplicationLike.getTinkerPatchApplicationLike();
//
//            // 初始化TinkerPatch SDK, 更多配置可参照API章节中的,初始化SDK
//            TinkerPatch.init(tinkerApplicationLike)
//                    .reflectPatchLibrary()
//                    .setPatchRollbackOnScreenOff(true)
//                    .setPatchRestartOnSrceenOff(true)
//                    .setFetchPatchIntervalByHours(3);
//
//            Log.d(TAG, "Current path version is:" + TinkerPatch.with().getPatchVersion());
//            // 每隔3个小时(通过setFetchPatchIntervalByHours设置)去访问后台时候有更新,通过handler实现轮训的效果
//            TinkerPatch.with().fetchPatchUpdateAndPollWithInterval();
//        }
//        super.onCreate();
//        if(LeakCanary.isInAnalyzerProcess(this)) {
//            return;
//        }
//        LeakCanary.install(this);
//
//        mApi = WXAPIFactory.createWXAPI(this, DomainConst.APP_ID);
//        mApi.registerApp(DomainConst.APP_ID);
        if(LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        mProcessType = getProcessType(base);
    }
    private int getProcessType(Context context) {
        SparseArray<String> processMap = new SparseArray<>(5);

        try {
            String processName = context.getPackageManager().getApplicationInfo(getPackageName(), 0).processName.trim();
            processMap.put(PROCESS_MAIN, processName);
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        String processName = "";
        int pid = Process.myPid();
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = mActivityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
            if (runningAppProcessInfo.pid == pid) {
                processName = runningAppProcessInfo.processName.trim();
                break;
            }
        }

        for (int index = 0; index < processMap.size(); index++) {
            if (processMap.valueAt(index).compareTo(processName) == 0) {
                return processMap.keyAt(index);
            }
        }

        return -1;
    }

    private boolean isProcessDeamon() {
        return mProcessType > PROCESS_DEAMON_MIN && mProcessType < PROCESS_DEAMON_MAX;
    }

    @Override
    protected void onInitializeApplicationService(CoreContext coreContext) {
        if (isProcessDeamon()) return;
        coreContext.addApplicationService(new HttpManager());
        coreContext.addApplicationService(new LoggerManager(coreContext));
        coreContext.addApplicationService(new NetworkManager(coreContext));
        coreContext.addApplicationService(new PreferencesManager(coreContext));
        coreContext.addApplicationService(new DownLoadManager(coreContext));
    }

    @Override
    protected final ExecutorService onCreateWorkerThreadPool() {
        return createThreadPool(Thread.NORM_PRIORITY);
    }

    @Override
    protected ExecutorService onCreateBackgroundThreadPool() {
        return createThreadPool(Thread.MIN_PRIORITY);
    }

    @Override
    protected ScheduledThreadPoolExecutor onCreateScheduledThreadPool() {
        return new ScheduledThreadPoolExecutor(2) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                logException(r, t);
            }
        };
    }

    @Override
    protected HandlerThread onCreateWorkerThread(String poolName) {
        return new HandlerThread(MyApplication.class.getName());
    }

    private ExecutorService createThreadPool(int priority) {
        int maximumPoolSize = DEFAULT_THREAD_POOL_SIZE + 1;
        final int processors = Runtime.getRuntime().availableProcessors();
        if (processors > 1) {
            maximumPoolSize = processors * 2 + 1;
        }

        return new ThreadPoolExecutor(2, maximumPoolSize, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactory() {
            private final ThreadGroup mThreadGroup = System.getSecurityManager() != null ? System.getSecurityManager().getThreadGroup() : Thread.currentThread().getThreadGroup();
            private final AtomicInteger mAtomic = new AtomicInteger(1);
            private final String mThreadName = "optimize-master-pool-" + mThreadId.getAndIncrement() + "-thread-";

            @Override
            public Thread newThread(@NonNull Runnable task) {
                Thread thread = new Thread(mThreadGroup, task, mThreadName + mAtomic.getAndIncrement() + "-" + priority, 0);
                if (thread.isDaemon()) {
                    thread.setDaemon(false);
                }

                thread.setPriority(priority);
                return thread;
            }
        }) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                logException(r, t);
            }
        };
    }
}
