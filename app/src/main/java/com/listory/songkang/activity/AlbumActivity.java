package com.listory.songkang.activity;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import com.listory.songkang.container.NavigationTabStrip;
import com.listory.songkang.listory.R;

public class AlbumActivity extends BaseActivity implements View.OnClickListener {
    private NavigationTabStrip mNavigationTab;
    private ImageView mBackView;

    protected void parseNonNullBundle(Bundle bundle){

    }
    protected void initDataIgnoreUi() {

    }
    @LayoutRes
    protected int getLayoutResourceId() { return R.layout.activity_album;}
    protected void viewAffairs(){
        mNavigationTab = fvb(R.id.navigation_tab_strip);
        mBackView = fvb(R.id.toolbar_back);
    }
    protected void assembleViewClickAffairs(){
        mBackView.setOnClickListener(this);
    }
    protected void initDataAfterUiAffairs(){

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
