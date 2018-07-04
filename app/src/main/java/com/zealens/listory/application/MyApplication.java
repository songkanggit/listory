package com.zealens.listory.application;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.HandlerThread;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.util.Log;
import android.util.SparseArray;

import com.meituan.android.walle.WalleChannelReader;
import com.tencent.stat.StatConfig;
import com.tencent.stat.StatService;
import com.tencent.tinker.lib.service.PatchResult;
import com.tencent.tinker.loader.app.ApplicationLike;
import com.tinkerpatch.sdk.TinkerPatch;
import com.tinkerpatch.sdk.loader.TinkerPatchApplicationLike;
import com.tinkerpatch.sdk.server.callback.ConfigRequestCallback;
import com.tinkerpatch.sdk.server.callback.RollbackCallBack;
import com.tinkerpatch.sdk.tinker.callback.ResultCallBack;
import com.zealens.listory.BuildConfig;
import com.zealens.listory.core.CoreApplication;
import com.zealens.listory.core.CoreContext;
import com.zealens.listory.core.connection.NetworkManager;
import com.zealens.listory.core.download.DownLoadManager;
import com.zealens.listory.core.http.HttpManager;
import com.zealens.listory.core.logger.LoggerManager;
import com.zealens.listory.core.preference.PreferencesManager;
import com.squareup.leakcanary.LeakCanary;
import com.zealens.listory.service.MediaService;

import java.util.HashMap;
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

    private ApplicationLike tinkerApplicationLike;

    @Override
    public void onCreate() {
        super.onCreate();
        initTinkerPatch();
        startService(new Intent(getApplicationContext(), MediaService.class));
        if(LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);

        if(BuildConfig.DEBUG) {
            // [可选]设置是否打开debug输出，上线时请关闭，Logcat标签为"MtaSDK"
            StatConfig.setDebugEnable(true);
        }
        // 基础统计API
        final String channel = WalleChannelReader.getChannel(this.getApplicationContext());
        StatConfig.setInstallChannel(channel);
        StatService.registerActivityLifecycleCallbacks(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        mProcessType = getProcessType(base);
        MultiDex.install(base);
    }

    //========================Thinker Start=================================================
    /**
     * 我们需要确保至少对主进程跟patch进程初始化 TinkerPatch
     */
    private void initTinkerPatch() {
        // 我们可以从这里获得Tinker加载过程的信息
        if (BuildConfig.TINKER_ENABLE) {
            tinkerApplicationLike = TinkerPatchApplicationLike.getTinkerPatchApplicationLike();
            // 初始化TinkerPatch SDK
            TinkerPatch.init(
                    tinkerApplicationLike
//                new TinkerPatch.Builder(tinkerApplicationLike)
//                    .requestLoader(new OkHttp3Loader())
//                    .build()
            )
                    .reflectPatchLibrary()
                    .setPatchRollbackOnScreenOff(true)
                    .setPatchRestartOnSrceenOff(true)
                    .setFetchPatchIntervalByHours(3)
            ;
            // 获取当前的补丁版本
            Log.d(TAG, "Current patch version is " + TinkerPatch.with().getPatchVersion());

            // fetchPatchUpdateAndPollWithInterval 与 fetchPatchUpdate(false)
            // 不同的是，会通过handler的方式去轮询
            TinkerPatch.with().fetchPatchUpdateAndPollWithInterval();
        }
    }

    /**
     * 在这里给出TinkerPatch的所有接口解释
     * 更详细的解释请参考:http://tinkerpatch.com/Docs/api
     */
    private void useSample() {
        TinkerPatch.init(tinkerApplicationLike)
                //是否自动反射Library路径,无须手动加载补丁中的So文件
                //注意,调用在反射接口之后才能生效,你也可以使用Tinker的方式加载Library
                .reflectPatchLibrary()
                //向后台获取是否有补丁包更新,默认的访问间隔为3个小时
                //若参数为true,即每次调用都会真正的访问后台配置
                .fetchPatchUpdate(false)
                //设置访问后台补丁包更新配置的时间间隔,默认为3个小时
                .setFetchPatchIntervalByHours(3)
                //向后台获得动态配置,默认的访问间隔为3个小时
                //若参数为true,即每次调用都会真正的访问后台配置
                .fetchDynamicConfig(new ConfigRequestCallback() {
                    @Override
                    public void onSuccess(HashMap<String, String> hashMap) {

                    }

                    @Override
                    public void onFail(Exception e) {

                    }
                }, false)
                //设置访问后台动态配置的时间间隔,默认为3个小时
                .setFetchDynamicConfigIntervalByHours(3)
                //设置当前渠道号,对于某些渠道我们可能会想屏蔽补丁功能
                //设置渠道后,我们就可以使用后台的条件控制渠道更新
                .setAppChannel("default")
                //屏蔽部分渠道的补丁功能
                .addIgnoreAppChannel("googleplay")
                //设置tinkerpatch平台的条件下发参数
                .setPatchCondition("test", "1")
                //设置补丁合成成功后,锁屏重启程序
                //默认是等应用自然重启
                .setPatchRestartOnSrceenOff(true)
                //我们可以通过ResultCallBack设置对合成后的回调
                //例如弹框什么
                //注意，setPatchResultCallback 的回调是运行在 intentService 的线程中
                .setPatchResultCallback(new ResultCallBack() {
                    @Override
                    public void onPatchResult(PatchResult patchResult) {
                        Log.i(TAG, "onPatchResult callback here");
                    }
                })
                //设置收到后台回退要求时,锁屏清除补丁
                //默认是等主进程重启时自动清除
                .setPatchRollbackOnScreenOff(true)
                //我们可以通过RollbackCallBack设置对回退时的回调
                .setPatchRollBackCallback(new RollbackCallBack() {
                    @Override
                    public void onPatchRollback() {
                        Log.i(TAG, "onPatchRollback callback here");
                    }
                });
    }
    //========================Thinker End=================================================
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
