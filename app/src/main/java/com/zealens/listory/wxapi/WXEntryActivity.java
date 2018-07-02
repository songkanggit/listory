package com.zealens.listory.wxapi;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.widget.Toast;

import com.zealens.listory.R;
import com.zealens.listory.activity.BaseActivity;
import com.zealens.listory.constant.DomainConst;
import com.zealens.listory.helper.HttpHelper;
import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.json.JSONException;
import org.json.JSONObject;

public class WXEntryActivity extends BaseActivity implements IWXAPIEventHandler {
	private static final String TAG = WXEntryActivity.class.getSimpleName();

    private IWXAPI api;

    protected void parseNonNullBundle(Bundle bundle){}
    protected void initDataIgnoreUi() {}
    @LayoutRes
    protected int getLayoutResourceId() { return R.layout.activity_wx_entry;}
    protected void viewAffairs(){}
    protected void assembleViewClickAffairs(){
        api = WXAPIFactory.createWXAPI(this, DomainConst.APP_ID, false);
        try {
            api.handleIntent(getIntent(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    protected void initDataAfterUiAffairs(){}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		api.handleIntent(intent, this);
	}

	@Override
	public void onReq(BaseReq req) {
		switch (req.getType()) {
		case ConstantsAPI.COMMAND_GETMESSAGE_FROM_WX:
			break;
		case ConstantsAPI.COMMAND_SHOWMESSAGE_FROM_WX:
			break;
		default:
			break;
		}
	}

	@Override
	public void onResp(BaseResp resp) {
		int result = R.string.share_success;
		switch (resp.errCode) {
		case BaseResp.ErrCode.ERR_OK: {
			switch (resp.getType()) {
				case ConstantsAPI.COMMAND_SENDAUTH:
				{
					result = R.string.errcode_success;
					final String code = ((SendAuth.Resp) resp).code;
					mCoreContext.executeAsyncTask(() -> {
						try {
							JSONObject param = new JSONObject();
							param.put("accessCode", code);
							param.put("registerPlatform", DomainConst.THIRD_PARTY_WEIXIN);
							HttpHelper.thirdPartyLoginRequest(mCoreContext, param, state -> finish());
						}  catch (JSONException e) {
							e.printStackTrace();
						}
					});
				}
					break;
				case ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX:
				{
					result = R.string.share_success;
				}
					break;
			}
        }
			break;
		case BaseResp.ErrCode.ERR_USER_CANCEL:
		case BaseResp.ErrCode.ERR_AUTH_DENIED:
		case BaseResp.ErrCode.ERR_UNSUPPORT:
			switch (resp.getType()) {
				case ConstantsAPI.COMMAND_SENDAUTH:
				{
					result = R.string.errcode_cancel;
				}
				break;
				case ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX:
				{
					result = R.string.share_failed;
				}
				break;
			}
			break;
		}
		if(result != R.string.errcode_success) {
			finish();
		}
        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
	}
}