package com.zealens.listory.core.connection;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.zealens.listory.core.CoreContext;
import com.zealens.listory.core.CoreManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NetworkManager extends CoreManager {
    public static final int NETWORK_TYPE_WIFI = 1 << 0;
    public static final int NETWORK_TYPE_MOBILE = 1 << 1;
    public static final int NETWORK_TYPE_MOBILE_2G = 1 << 2;
    public static final int NETWORK_TYPE_MOBILE_3G = 1 << 3;
    public static final int NETWORK_TYPE_MOBILE_4G = 1 << 4;

    private static final String PROC_UID_STAT = "/proc/uid_stat/";
    private static final String TCP_SND = "tcp_snd";
    private static final String TCP_RCV = "tcp_rcv";

    public NetworkManager(CoreContext coreContext) {
        super(coreContext);
    }

    public static String getNetworkConnectionSSID(final Context context) {
        final String ssid = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getSSID();
        if (TextUtils.isEmpty(ssid))
            return null;

        final Matcher matcher = Pattern.compile("\"(.*)\"").matcher(ssid);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return ssid;
    }

    public static boolean isNetworkInfoWifi(NetworkInfo networkInfo) {
        return !(networkInfo == null || !networkInfo.isConnected() || networkInfo.getType() != 1);

    }

    private static NetworkInfo getNetworkInfo(final Context context) {
        return ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public int order() {
        return ORDER.NETWORK;
    }

    @Override
    public void freeMemory() {
    }

    /**
     * @param networkInfo
     * @return 1、NETWORK_TYPE_1xRTT     常量值：7   网络类型：1xRTT
     * 2、NETWORK_TYPE_CDMA      常量值：4   网络类型：CDMA （电信2g）
     * 3、NETWORK_TYPE_EDGE      常量值：2   网络类型：EDGE（移动2g）
     * 4、NETWORK_TYPE_EHRPD     常量值：14  网络类型：eHRPD
     * 5、NETWORK_TYPE_EVDO_0    常量值：5   网络类型：EVDO 版本0.（电信3g）
     * 6、NETWORK_TYPE_EVDO_A    常量值：6   网络类型：EVDO 版本A （电信3g）
     * 7、NETWORK_TYPE_EVDO_B    常量值：12  网络类型：EVDO 版本B（电信3g）
     * 8、NETWORK_TYPE_GPRS      常量值：1   网络类型：GPRS （联通2g）
     * 9、NETWORK_TYPE_HSDPA     常量值：8   网络类型：HSDPA（联通3g）
     * 10、NETWORK_TYPE_HSPA     常量值：10  网络类型：HSPA
     * 11、NETWORK_TYPE_HSPAP    常量值：15  网络类型：HSPA+
     * 12、NETWORK_TYPE_HSUPA    常量值：9   网络类型：HSUPA
     * 13、NETWORK_TYPE_IDEN     常量值：11  网络类型：iDen
     * 14、NETWORK_TYPE_LTE      常量值：13  网络类型：LTE(3g到4g的一个过渡，称为准4g)
     * 15、NETWORK_TYPE_UMTS     常量值：3   网络类型：UMTS（联通3g）
     * 16、NETWORK_TYPE_UNKNOWN  常量值：0   网络类型：未知
     */
    public int getNetworkType(final NetworkInfo networkInfo) {
        int networkType = NETWORK_TYPE_WIFI;
        if (networkInfo == null) {
            networkType = NETWORK_TYPE_MOBILE;
        } else {
            final boolean isConnected = networkInfo.isConnected();
            final int type = networkInfo.getType();
            if (type == ConnectivityManager.TYPE_WIFI) {
                networkType = isConnected ? NETWORK_TYPE_WIFI : NETWORK_TYPE_MOBILE;
            } else if (type == ConnectivityManager.TYPE_MOBILE) {
                networkType = NETWORK_TYPE_MOBILE;
            }
        }

        if (networkInfo != null && networkType == NETWORK_TYPE_MOBILE) {
            final int subType = networkInfo.getSubtype();
            switch (subType) {
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    networkType |= NETWORK_TYPE_MOBILE_2G;
                    break;
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    networkType |= NETWORK_TYPE_MOBILE_3G;
                    break;
                case TelephonyManager.NETWORK_TYPE_LTE:
                    networkType |= NETWORK_TYPE_MOBILE_4G;
                    break;
            }
        }

        return networkType;
    }

    public boolean isConnected(final Context context) {
        final NetworkInfo networkInfo = getNetworkInfo(context);
        return !(networkInfo == null || !networkInfo.isConnected());

    }

    public boolean isConnectedFast(final Context context) {
        final NetworkInfo networkInfo = getNetworkInfo(context);
        if (networkInfo == null || !networkInfo.isConnected())
            return false;

        return isConnectionFast(networkInfo.getType(), networkInfo.getSubtype());

    }

    public boolean isConnectedWifi(final Context context) {
        return isNetworkInfoWifi(getNetworkInfo(context));
    }

    public boolean isConnectionFast(int type, int subType) {
        if (type == ConnectivityManager.TYPE_WIFI)
            return true;

        boolean result = false;
        if (type == ConnectivityManager.TYPE_MOBILE) {
            switch (subType) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    result = false;
                    break;
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_LTE:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    result = true;
                    break;
                default:
                    result = false;
                    break;
            }
        }
        return result;
    }

    private long getTotalSendBytes() {
        return TrafficStats.getTotalTxBytes();
    }

    private long getTotalReceiveBytes() {
        return TrafficStats.getTotalRxBytes();
    }

    private long getUidSendBytes(final int uid) {
        return TrafficStats.getUidTxBytes(uid);
    }

    private long getUidReceiveBytes(final int uid) {
        return TrafficStats.getUidRxBytes(uid);
    }

    private long getUidSendBytesEx(final int uid) {
        String[] uidStat = new File(PROC_UID_STAT).list();
        if (uidStat == null || uidStat.length == 0)
            return 0;

        if (!Arrays.asList(uidStat).contains(String.valueOf(uid)))
            return 0;

        String line = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(new File(new File(PROC_UID_STAT + String.valueOf(uid)), TCP_SND)));
            line = reader.readLine();
        } catch (FileNotFoundException e) {
            e("File " + PROC_UID_STAT + uid + " not found");
        } catch (IOException e) {
            e("Read " + PROC_UID_STAT + uid + " file failure");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e(e.getMessage());
                }
            }
        }

        if (TextUtils.isEmpty(line))
            return 0;

        return Long.valueOf(line);
    }
}
