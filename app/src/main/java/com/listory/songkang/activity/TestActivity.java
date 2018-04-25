package com.listory.songkang.activity;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.listory.songkang.listory.R;
import com.listory.songkang.transformer.ScaleInTransformer;

public class TestActivity extends AppCompatActivity {
    private int[] imgRes = {R.mipmap.view_page_01, R.mipmap.view_page_02, R.mipmap.view_page_03, R.mipmap.view_page_01,
            R.mipmap.view_page_02, R.mipmap.view_page_03};
    private PagerAdapter mAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        mViewPager = findViewById( R.id.viewPager);
        mViewPager.setPageMargin(20);
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(mAdapter = new HomePageAdapter());
        mViewPager.setPageTransformer(true, new ScaleInTransformer());
    }

    private class HomePageAdapter extends PagerAdapter {
        @Override
        public Object instantiateItem(ViewGroup container, int position)
        {
            View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.view_page_item, null);
            final int realPosition = getRealPosition(position);
            container.addView(view);
            ImageView content = view.findViewById(R.id.iv_pager);
            content.setBackgroundResource(imgRes[realPosition]);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });
            return view;
        }


        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object)
        {
            container.removeView((View) object);
        }

        @Override
        public int getCount()
        {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isViewFromObject(View view, Object o)
        {
            return view == o;
        }

        @Override
        public void startUpdate(ViewGroup container) {
            super.startUpdate(container);
            ViewPager viewPager = (ViewPager) container;
            int position = viewPager.getCurrentItem();
            if (position == 0) {
                position = getFirstItemPosition();
            } else if (position == getCount() - 1) {
                position = getLastItemPosition();
            }
            viewPager.setCurrentItem(position, false);

        }

        @Override
        public float getPageWidth(int position) {
            return (float)1;
        }

        private int getRealCount() {
            return imgRes.length;
        }

        private int getRealPosition(int position) {
            return position % getRealCount();
        }

        private int getFirstItemPosition() {
            return Integer.MAX_VALUE / getRealCount() / 2 * getRealCount();
        }

        private int getLastItemPosition() {
            return Integer.MAX_VALUE / getRealCount() / 2 * getRealCount() - 1;
        }
    }
}
