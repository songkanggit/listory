package com.listory.songkang.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.LayoutRes;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.listory.songkang.listory.R;
import com.listory.songkang.service.MediaService;
import com.listory.songkang.service.MusicTrack;
import com.listory.songkang.utils.PermissionUtil;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MusicPlayerActivity extends BaseActivity implements View.OnClickListener {
    private ImageView mBackImageView;
    private SeekBar mSeekBar;
    private MusicTrack mMusicTrack;
    private TextView mCurrentTime, mLastTime;

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
                        mLastTime.setText(getTimeLine(duration));
                    }
                    break;
                case MediaService.MUSIC_CHANGE_ACTION:
                    MusicTrack musicTrack = intent.getParcelableExtra(MediaService.MUSIC_CHANGE_ACTION_PARAM);
                    if(musicTrack != null) {
                        mMusicTrack = musicTrack;
                    }
                    initDataAfterUiAffairs();
                    break;
            }
        }
    };

    protected void parseNonNullBundle(Bundle bundle){

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
    }
    protected void assembleViewClickAffairs(){
        mBackImageView.setOnClickListener(this);
    }
    protected void initDataAfterUiAffairs(){
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
        }
    }

    private String getTimeLine(final int duration) {
        StringBuilder timeLine = new StringBuilder();
        timeLine.setLength(0);
        timeLine.append(duration/600000);
        timeLine.append(duration/60000);
        timeLine.append(":");
        int seconds = (duration%60000)/1000;
        if(seconds < 10) {
            timeLine.append("0");
        }
        timeLine.append(seconds);
        return timeLine.toString();
    }
}
