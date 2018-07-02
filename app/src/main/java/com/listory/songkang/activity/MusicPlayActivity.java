package com.listory.songkang.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.LayoutRes;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.listory.songkang.adapter.RecyclerViewMelodyListSimpleAdapter;
import com.listory.songkang.bean.MelodyDetailBean;
import com.listory.songkang.bean.SQLDownLoadInfo;
import com.listory.songkang.constant.DomainConst;
import com.listory.songkang.constant.PreferenceConst;
import com.listory.songkang.core.download.DownLoadListener;
import com.listory.songkang.core.download.DownLoadManager;
import com.listory.songkang.dialog.MelodyListDialog;
import com.listory.songkang.helper.HttpHelper;
import com.listory.songkang.helper.WeiXinHelper;
import com.listory.songkang.R;
import com.listory.songkang.image.ImageLoader;
import com.listory.songkang.service.MediaService;
import com.listory.songkang.service.MusicPlayer;
import com.listory.songkang.service.MusicTrack;
import com.listory.songkang.utils.BitmapUtil;
import com.listory.songkang.utils.DensityUtil;
import com.listory.songkang.utils.GussBlurUtil;
import com.listory.songkang.utils.QiniuImageUtil;
import com.listory.songkang.view.CachedImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MusicPlayActivity extends BaseActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener,
        RecyclerViewMelodyListSimpleAdapter.OnItemClickListener, DownLoadListener {
    public static final String BUNDLE_DATA = "data";
    public static final String BUNDLE_DATA_PLAY = "data_play";

    private ImageView mBackImageView;
    private CachedImageView mAlbumCoverIV;
    private SeekBar mSeekBar;
    private MusicTrack mMusicTrack;
    private TextView mCurrentTime, mLastTime, mMelodyNameTV;
    private ImageView mDownloadIV, mFavoriteIV, mShareIV;
    private ImageView mRepeatRandomIV, mPreIV, mPauseResumeIV, mNextIV, mListIV;
    private MelodyListDialog mMelodyListDialog;
    private CachedImageView mBackgroundImageView;
    private Bitmap mBackgroundBitmap;
    private int mAccountId;
    private String mWxThumbUrl;

    @MediaService.RepeatMode
    private int mRepeatMode = MediaService.RepeatMode.REPEAT_ALL;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case MediaService.BUFFER_UPDATE:
                    int secondProgress = intent.getIntExtra(MediaService.BUFFER_UPDATE_PARAM_PERCENT, 0);
                    int realSecondProgress = mSeekBar.getMax() / 100 * secondProgress;
                    if (secondProgress == 99) {
                        realSecondProgress = mSeekBar.getMax();
                    }
                    mSeekBar.setSecondaryProgress(realSecondProgress);
                    break;
                case MediaService.PLAY_STATE_UPDATE:
                    int duration = (int)intent.getLongExtra(MediaService.PLAY_STATE_UPDATE_DURATION, 0);
                    int position = (int)intent.getLongExtra(MediaService.PLAY_STATE_UPDATE_POSITION, 0);
                    if(duration > 0 && position < duration) {
                        mSeekBar.setMax(duration);
                        mSeekBar.setProgress(position);
                        mCurrentTime.setText(getTimeLine(position));
                        mLastTime.setText(getTimeLine(duration));
                    }
                    MusicTrack musicTrack = intent.getParcelableExtra(MediaService.PLAY_STATE_UPDATE_DATA);
                    if(musicTrack != null) {
                        updateMusicInfo(musicTrack, false);
                    }
                    break;
            }
        }
    };

    protected void parseNonNullBundle(Bundle bundle){
        MelodyDetailBean bean = bundle.getParcelable(BUNDLE_DATA);
        if(bean != null) {
            mMusicTrack = bean.convertToMusicTrack();
        }
    }

    protected void initDataIgnoreUi() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MediaService.BUFFER_UPDATE);
        intentFilter.addAction(MediaService.PLAY_STATE_UPDATE);
        registerReceiver(mIntentReceiver, intentFilter);
    }
    @LayoutRes
    protected int getLayoutResourceId() { return R.layout.activity_music_player;}
    protected void viewAffairs(){
        mSeekBar = fvb(R.id.seek_bar);
        mBackImageView = fvb(R.id.toolbar_back);
        mCurrentTime = fvb(R.id.tv_play_left);
        mLastTime = fvb(R.id.tv_play_right);
        mAlbumCoverIV = fvb(R.id.image_cover);
        mMelodyNameTV = fvb(R.id.melody_name);

        mDownloadIV = fvb(R.id.iv_download);
        mFavoriteIV = fvb(R.id.iv_like);
        mShareIV = fvb(R.id.iv_share);

        mRepeatRandomIV = fvb(R.id.iv_random_repeat);
        mPreIV = fvb(R.id.iv_previous);
        mPauseResumeIV = fvb(R.id.iv_pause_resume);
        mNextIV = fvb(R.id.iv_next);
        mListIV = fvb(R.id.iv_list);

        mBackgroundImageView = fvb(R.id.iv_play_background);
    }
    protected void assembleViewClickAffairs(){
        mBackImageView.setOnClickListener(this);
        mDownloadIV.setOnClickListener(this);
        mFavoriteIV.setOnClickListener(this);
        mShareIV.setOnClickListener(this);

        mRepeatRandomIV.setOnClickListener(this);
        mPreIV.setOnClickListener(this);
        mPauseResumeIV.setOnClickListener(this);
        mNextIV.setOnClickListener(this);
        mListIV.setOnClickListener(this);

        mSeekBar.setOnSeekBarChangeListener(this);
    }
    protected void initDataAfterUiAffairs(){
        MusicTrack musicTrack = MusicPlayer.getInstance().getCurrentMusicTrack();
        if(musicTrack != null) {
            mMusicTrack = musicTrack;
            mRepeatMode = MusicPlayer.getInstance().getRepeatMode();
            toggleRepeatMode(false);
            updateMusicInfo(mMusicTrack ,true);
            cachedWxThumbIcon();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setNoTitleNoNotificationBar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBackgroundBitmap = null;
        if(mIntentReceiver != null) {
            unregisterReceiver(mIntentReceiver);
            mIntentReceiver = null;
        }
        mDownloadManager.removeAllDownLoadListener(this);
    }

    @Override
    public void onClick(View view) {
        final boolean isLogin = isLogin();
        switch (view.getId()) {
            case R.id.toolbar_back:
                finish();
                break;
            case R.id.iv_download:
            {
                if(isLogin) {
                    int taskState = mDownloadManager.addTask(mMusicTrack);
                    if(taskState == DownLoadManager.TaskState.TASK_OK) {
                        mDownloadManager.setSingleTaskListener(String.valueOf(mMusicTrack.mId), this);
                    } else {
                        int toastRes = R.string.already_downloaded;
                        switch (taskState) {
                            case DownLoadManager.TaskState.TASK_EXIST:
                                toastRes = R.string.download_task_exist;
                                break;
                            case DownLoadManager.TaskState.TASK_MAX:
                                toastRes = R.string.download_over_max;
                                break;
                            case DownLoadManager.TaskState.TASK_COMPLETE:
                                toastRes = R.string.already_downloaded;
                                break;
                        }
                        Toast.makeText(getApplicationContext(), toastRes, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    startLoginActivity();
                }
            }
                break;
            case R.id.iv_like:
                if(isLogin) {
                    mFavoriteIV.setEnabled(false);
                    JSONObject param = new JSONObject();
                    try {
                        param.put("accountId", mAccountId);
                        param.put("melodyId", mMusicTrack.mId);
                    } catch (JSONException e) {
                        return;
                    }
                    HttpHelper.requestLikeMelody(mCoreContext, param, responseBean -> runOnUiThread(() -> {
                        if(responseBean.isState()) {
                            mFavoriteIV.setImageResource(R.mipmap.music_player_like);
                            Toast.makeText(getApplicationContext(), R.string.favorite_success, Toast.LENGTH_LONG).show();
                        } else {
                            mFavoriteIV.setImageResource(R.mipmap.music_player_unlike);
                            Toast.makeText(getApplicationContext(), R.string.favorite_cancel, Toast.LENGTH_LONG).show();
                        }
                        mFavoriteIV.setEnabled(true);
                    }));
                } else {
                    startLoginActivity();
                }
                break;
            case R.id.iv_comment:
                if(isLogin) {

                } else {
                    startLoginActivity();
                }
                break;
            case R.id.iv_share:
                if(mMusicTrack != null) {
                    final String sharedUrl = "https://admin.liyangstory.com/share/player.html?melodyAlbum="
                            + mMusicTrack.mAlbum + "&melodyId=" + mMusicTrack.mId;
                    WeiXinHelper.getInstance().shareToWeChat(getApplicationContext(), sharedUrl, mMusicTrack.mTitle, mWxThumbUrl);
                }
                break;
            case R.id.iv_random_repeat:
                mRepeatMode = (mRepeatMode + 1) % 3;
                MusicPlayer.getInstance().setRepeatMode(mRepeatMode);
                toggleRepeatMode(true);
                break;
            case R.id.iv_previous:
                if(MusicPlayer.getInstance().goToPrevious()) {
                    enableNextAndPreviousControl(false);
                }
                break;
            case R.id.iv_pause_resume:
                togglePlayState();
                break;
            case R.id.iv_next:
                if(MusicPlayer.getInstance().goToNext()) {
                    enableNextAndPreviousControl(false);
                }
                break;
            case R.id.iv_list:
                showPopWindow();
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if(b) {
            final int keyFrameMs = seekBar.getProgress()/1000;
            final long newPosition = (long)(keyFrameMs * 1000);
            mCurrentTime.setText(getTimeLine((int)newPosition));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        final int keyFrameMs = seekBar.getProgress()/1000;
        final long newPosition = (long)(keyFrameMs * 1000);
        mCurrentTime.setText(getTimeLine((int)newPosition));
        MusicPlayer.getInstance().seek(newPosition);
    }

    @Override
    public void onItemClick(View view, int position) {
        MusicPlayer.getInstance().playAt(position);
        mMelodyListDialog.dismiss();
    }

    @Override
    public void onStart(SQLDownLoadInfo sqlDownLoadInfo) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), R.string.downloading, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onProgress(SQLDownLoadInfo sqlDownLoadInfo, boolean isSupportBreakpoint, int progress) {

    }

    @Override
    public void onStop(SQLDownLoadInfo sqlDownLoadInfo, boolean isSupportBreakpoint) {

    }

    @Override
    public void onError(SQLDownLoadInfo sqlDownLoadInfo) {

    }

    @Override
    public void onSuccess(SQLDownLoadInfo sqlDownLoadInfo) {
        runOnUiThread(() -> {
            if(Long.valueOf(sqlDownLoadInfo.getTaskID()) == mMusicTrack.mId)
                mDownloadIV.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.music_play_download_success));
        });
    }

    private void showPopWindow() {
        if(mMelodyListDialog == null) {
            mMelodyListDialog = new MelodyListDialog(MusicPlayActivity.this, MusicPlayer.getInstance().getMusicTrackList());
            mMelodyListDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mMelodyListDialog.setOnDismissListener(dialog -> setNoTitleNoNotificationBar());
        }
        List<MusicTrack> musicTrackList = MusicPlayer.getInstance().getMusicTrackList();
        int playPosition = 0;
        for(int i=0; i<musicTrackList.size(); i++) {
            if(musicTrackList.get(i).mUrl.equals(mMusicTrack.mUrl)) {
                playPosition = i;
                break;
            }
        }
        mMelodyListDialog.setPlayingPosition(playPosition);
        Window window = mMelodyListDialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.setWindowAnimations(R.style.custom_popup_window_style);
        mMelodyListDialog.show();
    }
    
    private void enableNextAndPreviousControl(boolean enable) {
        mPreIV.setEnabled(enable);
        mNextIV.setEnabled(enable);
        mDownloadIV.setEnabled(enable);
        mFavoriteIV.setEnabled(enable);
        mRepeatRandomIV.setEnabled(enable);
    }

    private void toggleRepeatMode(boolean showToast) {
        int resId = R.mipmap.music_player_repeat_all;
        int toastRes = R.string.repeat_play_all;
        switch (mRepeatMode) {
            case MediaService.RepeatMode.REPEAT_RANDOM:
                resId = R.mipmap.music_player_random;
                toastRes = R.string.random_play;
                break;
            case MediaService.RepeatMode.REPEAT_CURRENT:
                resId = R.mipmap.music_player_repeat;
                toastRes = R.string.repeat_play;
                break;
            default:
                break;
        }
        mRepeatRandomIV.setImageBitmap(BitmapFactory.decodeResource(getResources(), resId));
        if(showToast)Toast.makeText(getApplicationContext(), toastRes, Toast.LENGTH_SHORT).show();
    }

    private void updateMusicInfo(MusicTrack musicTrack, boolean force) {
        if(MusicPlayer.getInstance().isPlaying()) {
            mPauseResumeIV.setImageResource(R.mipmap.music_player_pause);
        } else {
            mPauseResumeIV.setImageResource(R.mipmap.music_player_play);
        }
        if(MusicPlayer.getInstance().isInitialized()) {
            enableNextAndPreviousControl(true);
        } else {
            enableNextAndPreviousControl(false);
        }
        if((musicTrack != null && !musicTrack.equals(mMusicTrack)) || force) {
            mMusicTrack = musicTrack;
            cachedWxThumbIcon();
            final String imageUrl = mMusicTrack.mCoverImageUrl + QiniuImageUtil.generateFixSizeImageAppender(mContext, QiniuImageUtil.ImageType.MELODY_SQUARE_L);
            mAlbumCoverIV.setImageUrl(imageUrl, bitmap -> {
                if(bitmap != null) {
                    mAlbumCoverIV.setImageBitmap(BitmapUtil.getRoundRectBitmap(bitmap, DensityUtil.dip2px(mContext, 4)));
                    mBackgroundBitmap = GussBlurUtil.rsBlur(MusicPlayActivity.this, bitmap, 18, (float)0.6);
                    mBackgroundImageView.setImageBitmap(mBackgroundBitmap);
                }
            });
            mMelodyNameTV.setText(mMusicTrack.mTitle);
            mFavoriteIV.setImageResource(R.mipmap.melody_unlike);
            mDownloadIV.setImageResource(R.mipmap.music_player_download);
            if(isLogin()) {
                mCoreContext.executeAsyncTask(() -> {
                    try {
                        JSONObject secondParam = new JSONObject();
                        secondParam.put("id", mMusicTrack.mId);
                        secondParam.put("accountId", String.valueOf(mAccountId));
                        String secondResponse = mHttpService.post(DomainConst.MELODY_ITEM_URL, secondParam.toString());
                        JSONObject secondObject = new JSONObject(secondResponse);
                        final JSONObject temp = secondObject.getJSONObject("data");
                        final boolean isFavorite = temp.getString("favorated").equals("true") ? true : false;
                        runOnUiThread(() -> {
                            if (isFavorite) {
                                mFavoriteIV.setImageResource(R.mipmap.music_player_like);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }
            ArrayList<SQLDownLoadInfo> downLoadInfoArrayList = mDownloadManager.getUserDownloadInfoList(String.valueOf(mAccountId));
            for(SQLDownLoadInfo info:downLoadInfoArrayList) {
                if(info.getTaskID().equals(String.valueOf(mMusicTrack.mId))) {
                    mDownloadIV.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.music_play_download_success));
                    break;
                }
            }
        }
    }

    private void togglePlayState() {
        if(MusicPlayer.getInstance().isPlaying()) {
            MusicPlayer.getInstance().pause();
        } else {
            MusicPlayer.getInstance().play();
        }
        updateMusicInfo(mMusicTrack, false);
    }

    private void cachedWxThumbIcon() {
        mShareIV.setEnabled(false);
        mWxThumbUrl = mMusicTrack.mCoverImageUrl + QiniuImageUtil.generateFixSizeImageAppender(mContext, QiniuImageUtil.ImageType.THUMBNAIL);
        ImageLoader.getInstance().loadImageView(null, mWxThumbUrl, url -> mShareIV.setEnabled(true));
    }

    private String getTimeLine(final int duration) {
        StringBuilder timeLine = new StringBuilder();
        timeLine.append(duration/600000);
        timeLine.append(duration%600000/60000);
        timeLine.append(":");
        int seconds = (duration%60000)/1000;
        if(seconds < 10) {
            timeLine.append("0");
        }
        timeLine.append(seconds);
        return timeLine.toString();
    }

    private void setNoTitleNoNotificationBar() {
        mRootVG.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void startLoginActivity(){
        Intent startLoginIntent = new Intent(MusicPlayActivity.this, LoginActivity.class);
        startActivity(startLoginIntent);
    }

    private boolean isLogin() {
        mAccountId = mPreferencesManager.get(PreferenceConst.ACCOUNT_ID, -1);
        return mAccountId != -1;
    }
}
