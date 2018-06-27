package com.listory.songkang.constant;

/**
 * Created by songkang on 2017/7/2.
 */

public class DomainConst {
    public static final boolean USE_LOGGER = true;
    public static final String APP_ID = "wx605ca2108dd6e7d3";

    public static final String DOMAIN = "https://admin.liyangstory.com";
    public static final String PICTURE_DOMAIN = "http://img.liyangstory.com";
    public static final String MEDIA_DOMAIN = "http://video.liyangstory.com";
    public static final String DOMAIN_LOCAL = "http://10.96.155.101:8080/storytree";

    public static final String ACCOUNT_INFO_URL = DOMAIN + "/account/query.do";
    public static final String ACCOUNT_UPDATE_URL = DOMAIN + "/account/update.do";
    public static final String ACCOUNT_THIRD_PARTY_LOGIN_URL = DOMAIN + "/sms/thirdPartyLogin.do";

    public static final String WX_UNI_ORDER_URL = DOMAIN + "/weixin/preOrder.do";
    public static final String ALIPAY_ORDER_URL = DOMAIN + "/alipay/buildOrderParam.do";
    public static final String ALIPAY_LOGIN_URL = DOMAIN + "/alipay/buildAuthParam.do";

    public static final String SMS_CODE_REQUEST_URL = DOMAIN + "/sms/requestToken.do";
    public static final String SMS_CODE_VERIFY_REQUEST_URL = DOMAIN + "/sms/verifyToken.do";

    public static final String ALBUM_LIST_URL = DOMAIN + "/album/queryList.do";
    public static final String ALBUM_ITEM_URL = DOMAIN + "/album/query.do";
    public static final String MELODY_LIST_URL = DOMAIN + "/melody/queryList.do";
    public static final String MELODY_ITEM_URL = DOMAIN + "/melody/query.do";
    public static final String FAVORITE_REQUEST_URL = DOMAIN + "/app/favoriteMelody/saveOrDelete.do";
    public static final String FAVORITE_MELODY_LIST_URL = DOMAIN + "/app/favoriteMelody/queryList.do";

    public static final String STORY_COLLECTION_LIST_URL = DOMAIN + "/app/accountStoryCollection/queryList.do";
    public static final String STORY_COLLECTION_ADD_URL = DOMAIN + "/app/accountStoryCollection/add.do";
    public static final String STORY_COLLECTION_DELETE_URL = DOMAIN + "/app/accountStoryCollection/delete.do";
    public static final String STORY_COLLECTION_UPDATE_URL = DOMAIN + "/app/accountStoryCollection/update.do";

    public static final String STORY_COLLECTION_LIST_ADD_URL = DOMAIN + "/app/accountStoryCollectionList/add.do";
    public static final String STORY_COLLECTION_LIST_DELETE_URL = DOMAIN + "/app/accountStoryCollectionList/delete.do";
    public static final String STORY_COLLECTION_LIST_LIST_URL = DOMAIN + "/app/accountStoryCollectionList/queryList.do";

    public static final String ARTICLE_LIST_URL = DOMAIN + "/article/queryList.do";
    public static final String BANNER_VIEW_ITEM_URL = DOMAIN + "/banner/queryList.do";
    public static final String UPLOAD_FILE_URL = DOMAIN + "/common/appImageUpload.do";

    public static final String MELODY_COMMENTS_LIST_URL = DOMAIN + "/app/comment/queryList.do";
    public static final String MELODY_COMMENTS_ADD_URL = DOMAIN + "/app/comment/add.do";
    public static final String MELODY_COMMENTS_APPROVE_URL = DOMAIN + "/app/comment/approve.do";

    public static final String COUPON_VERIFY_URL = DOMAIN + "/giftcard/verifyGiftCard.do";

    public static final String THIRD_PARTY_WEIXIN = "wechat";
    public static final String THIRD_PARTY_ALIPAY = "alipay";

    public static boolean LOG_INTO_FILE_SWITCH_ON = true;
    public static final String TIME_FORMAT_FILE_NAME = "dd-MM-yy";
    public static final String TIME_FORMAT_LOG_MSG_START_APPEND = "MM-dd HH:mm:ss";
    public static final String LOG_PARENT_PATH = "AndroidFlex/";
    public static final long LOG_DIR_SIZE_LIMIT = 50 * 1024 * 1024;//50M
    public static final String SHARED_PREFERENCES_KEY = "global_preference";

    public static final String CODE_OK = "0000";
}
