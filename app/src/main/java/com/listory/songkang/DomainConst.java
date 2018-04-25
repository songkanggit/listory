package com.listory.songkang;

/**
 * Created by songkang on 2017/7/2.
 */

public class DomainConst {
    public static final boolean USE_LOGGER = true;
    public static final String APP_ID = "wx605ca2108dd6e7d3";

    public static final String DOMAIN = "https://admin.guostory.com";

    public static final String ACCOUNT_INFO_REEUEST = DOMAIN + "/account/query.do";

    public static final String WX_UNI_ORDER_URL = DOMAIN + "/weixin/order/preOrder.do";
    public static final String ALIPAY_ORDER_URL = DOMAIN + "/alipay/buildOrderParam.do";
    public static final String SMS_CODE_REQUEST_URL = DOMAIN + "/sms/requestToken.do";
    public static final String SMS_CODE_VERIFY_REQUEST_URL = DOMAIN + "/sms/verifyToken.do";
    public static final String ALBUM_PRECIOUS_REQUEST = DOMAIN + "/album/queryList.do";
    public static final String MELODY_PRECIOUS_REQUEST = DOMAIN + "/melody/queryList.do";
    public static final String FAVORITE_REQUEST_DEL_URL = DOMAIN + "/app/favoriteMelody/delete.do";
    public static final String FAVORITE_REQUEST_ADD_URL = DOMAIN + "/app/favoriteMelody/appAdd.do";
    public static final String ACCOUNT_FAVORITE_MELODY_RESUQEST = DOMAIN + "/app/favoriteMelody/queryList.do";
    public static final String ARTICLE_LIST_REQUEST = DOMAIN + "/article/queryList.do";

    //Preference
    public static final String ACCOUNT_ID = "accountId";
    public static final String ACCOUNT_TELEPHONE = "telephone";
    public static final String ACCOUNT_ICON = "icon";
    public static final String ACCOUNT_VIP = "isVip";
    public static final String ACCOUNT_NEED_UPDATE = "upadte";
    public static final String ACCOUNT_NICK_NAME = "nickName";

    public static boolean LOG_INTO_FILE_SWITCH_ON = true;
    public static final String TIME_FORMAT_FILE_NAME = "dd-MM-yy";
    public static final String TIME_FORMAT_LOG_MSG_START_APPEND = "MM-dd HH:mm:ss";
    public static final String LOG_PARENT_PATH = "AndroidFlex/";
    public static final long LOG_DIR_SIZE_LIMIT = 50 * 1024 * 1024;//50M
    public static final String SHARED_PREFERENCES_KEY = "global_preference";
}
