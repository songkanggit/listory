package com.listory.songkang.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.support.annotation.LayoutRes;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.listory.songkang.adapter.RecyclerViewMelodyListSimpleAdapter;
import com.listory.songkang.bean.MelodyDetailBean;
import com.listory.songkang.dialog.MelodyListDialog;
import com.listory.songkang.listory.R;
import com.listory.songkang.service.MediaService;
import com.listory.songkang.service.MusicPlayer;
import com.listory.songkang.service.MusicTrack;
import com.listory.songkang.utils.PermissionUtil;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MusicPlayerActivity extends BaseActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, RecyclerViewMelodyListSimpleAdapter.OnItemClickListener {
    public static final String BUNDLE_DATA = "data";
    public static final String BUNDLE_DATA_PLAY = "data_play";

    private ImageView mBackImageView, mAlbumCoverIV;
    private SeekBar mSeekBar;
    private MusicTrack mMusicTrack;
    private TextView mCurrentTime, mLastTime, mMelodyNameTV;
    private ImageView mDownloadIV, mLikeIV, mCommentIV, mShareIV;
    private ImageView mRandomIV, mPreIV, mPauseResumeIV, mNextIV, mListIV;
    private MelodyListDialog mMelodyListDialog;
    private boolean mIsLike;
    private boolean mIsAutoPlay;

    @MediaService.RepeatMode
    private int mRepeatMode = MediaService.RepeatMode.REPEAT_ALL;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case MediaService.BUFFER_UPDATE:
                    int secondProgress = intent.getIntExtra(MediaService.BUFFER_UPDATE_PARAM_PERCENT, 0);
                    mSeekBar.setSecondaryProgress(secondProgress);
                    break;
                case MediaService.PLAY_STATE_UPDATE:
                    int duration = intent.getIntExtra(MediaService.PLAY_STATE_UPDATE_DURATION, 0);
                    long position = intent.getLongExtra(MediaService.PLAY_STATE_UPDATE_POSITION, 0);
                    if(duration > 0 && position < duration) {
                        mSeekBar.setMax(duration);
                        mSeekBar.setProgress((int)position);
                        mCurrentTime.setText(getTimeLine((int)position));
                        mLastTime.setText(getTimeLine(duration - (int)position));
                    }
                    break;
                case MediaService.MUSIC_CHANGE_ACTION:
                    MusicTrack musicTrack = intent.getParcelableExtra(MediaService.MUSIC_CHANGE_ACTION_PARAM);
                    if(musicTrack != null) {
                        mMusicTrack = musicTrack;
                    }
                    updateMusicInfo();
                    break;
            }
        }
    };

    protected void parseNonNullBundle(Bundle bundle){
        MelodyDetailBean melody = bundle.getParcelable(BUNDLE_DATA);
        if(melody != null) {
            mMusicTrack = melody.convertToMusicTrack();
        }
        mIsAutoPlay = bundle.getBoolean(BUNDLE_DATA_PLAY);
    }
    protected void initDataIgnoreUi() {
        PermissionUtil.verifyStoragePermissions(MusicPlayerActivity.this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MediaService.BUFFER_UPDATE);
        intentFilter.addAction(MediaService.PLAY_STATE_UPDATE);
        intentFilter.addAction(MediaService.MUSIC_CHANGE_ACTION);
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
        mLikeIV = fvb(R.id.iv_like);
        mCommentIV = fvb(R.id.iv_comment);
        mShareIV = fvb(R.id.iv_share);

        mRandomIV = fvb(R.id.iv_random_repeat);
        mPreIV = fvb(R.id.iv_previous);
        mPauseResumeIV = fvb(R.id.iv_pause_resume);
        mNextIV = fvb(R.id.iv_next);
        mListIV = fvb(R.id.iv_list);
    }
    protected void assembleViewClickAffairs(){
        mBackImageView.setOnClickListener(this);
        mDownloadIV.setOnClickListener(this);
        mLikeIV.setOnClickListener(this);
        mCommentIV.setOnClickListener(this);
        mShareIV.setOnClickListener(this);

        mRandomIV.setOnClickListener(this);
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
        } else {
            mIsAutoPlay = true;
        }
        if(mIsAutoPlay) {
            MusicPlayer.getInstance().play();
        }
        updateMusicInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRootVG.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.toolbar_back:
                finish();
                break;
            case R.id.iv_download:
                break;
            case R.id.iv_like:
                if(mIsLike) {
                    mIsLike = false;
                    mLikeIV.setImageResource(R.mipmap.music_player_unlike);
                } else {
                    mIsLike = true;
                    mLikeIV.setImageResource(R.mipmap.music_player_like);
                }
                break;
            case R.id.iv_comment:
                break;
            case R.id.iv_share:
                break;
            case R.id.iv_random_repeat:
                mRepeatMode = (mRepeatMode + 1) % 3;
                MusicPlayer.getInstance().setRepeatMode(mRepeatMode);
                changeRepeatMode();
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
            MusicPlayer.getInstance().seek(i);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onItemClick(View view, int position) {
        MusicPlayer.getInstance().playAt(position);
    }

    private void showPopWindow() {
        if(mMelodyListDialog == null) {
            mMelodyListDialog = new MelodyListDialog(MusicPlayerActivity.this, MusicPlayer.getInstance().getMusicTrackList());
            mMelodyListDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mMelodyListDialog.setOnCancelListener(dialogInterface -> mRootVG.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION));
        }
        Window window = mMelodyListDialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.setWindowAnimations(R.style.custom_popup_window_style);
        mMelodyListDialog.show();
    }
    
    private void enableNextAndPreviousControl(boolean enable) {
        if(enable) {
            mPreIV.setEnabled(true);
            mNextIV.setEnabled(true);
        } else {
            mPreIV.setEnabled(false);
            mNextIV.setEnabled(false);
        }
    }

    private void changeRepeatMode() {
        int resId = R.mipmap.music_player_repeat_all;
        switch (mRepeatMode) {
            case MediaService.RepeatMode.REPEAT_RANDOM:
                resId = R.mipmap.music_player_random;
                break;
            case MediaService.RepeatMode.REPEAT_CURRENT:
                resId = R.mipmap.music_player_repeat;
                break;
            default:
                break;
        }
        mRandomIV.setImageBitmap(BitmapFactory.decodeResource(getResources(), resId));
    }

    private void updateMusicInfo() {
        if(mMusicTrack != null) {
            mAlbumCoverIV.setImageBitmap(BitmapFactory.decodeFile(mMusicTrack.mCoverImageUrl.split(";")[0]));
            mMelodyNameTV.setText(mMusicTrack.mTitle);
            mIsLike = true;
        }
        if(MusicPlayer.getInstance().isPlaying()) {
            mPauseResumeIV.setImageResource(R.mipmap.music_player_pause);
        } else {
            mPauseResumeIV.setImageResource(R.mipmap.music_player_play);
        }
        enableNextAndPreviousControl(true);
    }

    private void togglePlayState() {
        if(MusicPlayer.getInstance().isPlaying()) {
            MusicPlayer.getInstance().pause();
            mPauseResumeIV.setImageResource(R.mipmap.music_player_play);
        } else {
            MusicPlayer.getInstance().play();
            mPauseResumeIV.setImageResource(R.mipmap.music_player_pause);
        }
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
}
