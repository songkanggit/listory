package com.listory.songkang.activity;

import android.support.annotation.LayoutRes;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.listory.songkang.listory.R;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MusicPlayerActivity extends BaseActivity implements View.OnClickListener {
    private ImageView mBackImageView;

    protected void parseNonNullBundle(Bundle bundle){

    }
    protected void initDataIgnoreUi() {
    }
    @LayoutRes
    protected int getLayoutResourceId() { return R.layout.activity_music_player;}
    protected void viewAffairs(){
        mBackImageView = fvb(R.id.toolbar_back);
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
}
