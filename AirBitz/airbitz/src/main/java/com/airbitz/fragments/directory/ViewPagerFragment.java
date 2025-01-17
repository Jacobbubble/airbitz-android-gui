/**
 * Copyright (c) 2014, Airbitz Inc
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms are permitted provided that
 * the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Redistribution or use of modified source code requires the express written
 *    permission of Airbitz Inc.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those
 * of the authors and should not be interpreted as representing official policies,
 * either expressed or implied, of the Airbitz Project.
 */

package com.airbitz.fragments.directory;

import android.app.Activity;
import android.app.Fragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.airbitz.R;
import com.airbitz.activities.NavigationActivity;
import com.airbitz.adapters.TouchImageViewPagerAdapter;
import com.airbitz.fragments.BaseFragment;
import com.airbitz.objects.HighlightOnPressImageButton;
import com.airbitz.widgets.TouchImageView;
import com.airbitz.api.CoreAPI;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerFragment extends BaseFragment
    implements NavigationActivity.OnBackPress {

    final String TAG = getClass().getSimpleName();

    private HighlightOnPressImageButton mQuitButton;
    private List<TouchImageView> mImageViews = new ArrayList<TouchImageView>();
    private ViewPager mViewPager;
    private ImageView mBackground;
    private ImageView mForeground;
    private SeekBar mSeekBar;
    private TextView mCounterView;
    private NavigationActivity mActivity;
    private int mPosition;
    private boolean mSeekbarReposition = false;
    private final int SEEKBAR_MAX = 100;
    private float mScale;
    private int mShortAnimationDuration = 300;
    private boolean mForegroundShowing = false;
    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View mView = inflater.inflate(R.layout.fragment_viewpager, container, false);

        mQuitButton = (HighlightOnPressImageButton) mView.findViewById(R.id.viewpager_close_button);
        mQuitButton.setVisibility(View.VISIBLE);
        mQuitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPress();
            }
        });

        mActivity = (NavigationActivity) getActivity();

        mViewPager = (ViewPager) mView.findViewById(R.id.fragment_viewpager_viewpager);
        mViewPager.setAdapter(new TouchImageViewPagerAdapter(mImageViews));
        mViewPager.setCurrentItem(mPosition);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) { }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (positionOffsetPixels == 0) {
                    mPosition = position;
                    mSeekBar.setProgress((int) (position * mScale));
                    crossfade(mImageViews.get(mPosition));
                }
            }

            public void onPageSelected(int position) {

            }
        });

        mBackground = (ImageView) mView.findViewById(R.id.fragment_viewpager_background);
        mForeground = (ImageView) mView.findViewById(R.id.fragment_viewpager_foreground);
        mBackground.setAlpha(1f);
        mForeground.setAlpha(0f);

        mCounterView = (TextView) mView.findViewById(R.id.viewpager_counter_view);

        mSeekBar = (SeekBar) mView.findViewById(R.id.viewpager_seekBar);

        if(mImageViews.size() > 1) {
            mScale = SEEKBAR_MAX / (mImageViews.size() - 1);
            mSeekBar.setMax(SEEKBAR_MAX);
            mSeekBar.setProgress(mPosition);
            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                    int nearestValue = Math.round(progress / mScale);
                    if (!mSeekbarReposition) {
                        mViewPager.setCurrentItem(nearestValue, true);
                    }
                    int val = (progress * (seekBar.getWidth() - 2 * seekBar.getThumbOffset())) / seekBar.getMax();
                    mCounterView.setText(String.valueOf(nearestValue+1)+"/"+mImageViews.size());
                    mCounterView.setX(seekBar.getX() + val + (seekBar.getThumbOffset()) - (mCounterView.getWidth() / 2));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int progress = seekBar.getProgress();
                    float nearestValue = Math.round(progress / mScale);
                    mSeekbarReposition = true;
                    mSeekBar.setProgress((int) (nearestValue * mScale));
                    mSeekbarReposition = false;
                }
            });
        } else {
            mSeekBar.setVisibility(View.INVISIBLE);
            mCounterView.setVisibility(View.INVISIBLE);
        }

        mCounterView.setX(mSeekBar.getX() - (mCounterView.getWidth() / 2));
        mCounterView.setText("1/" + mImageViews.size());

        return mView;
    }

    @Override
    public boolean onBackPress() {
        ViewPagerFragment.popFragment(mActivity);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        mHandler.postDelayed(updater, 100);
    }

    Runnable updater = new Runnable() {
        @Override
        public void run() {
            updateScrubber();
        }
    };


    private void updateScrubber() {
        if(mImageViews.size() > 1) {
            int progress = mSeekBar.getProgress();
            int nearestValue = Math.round(progress / mScale);
            int val = (progress * (mSeekBar.getWidth() - 2 * mSeekBar.getThumbOffset())) / mSeekBar.getMax();
            CoreAPI.debugLevel(1, "Seekbar progress = "+progress+", val = "+val);
            mCounterView.setText(String.valueOf(nearestValue + 1) + "/" + mImageViews.size());
            mCounterView.setX(mSeekBar.getX() + val + (mSeekBar.getThumbOffset()) - (mCounterView.getWidth() / 2));
        }
        else {
            mSeekBar.setVisibility(View.INVISIBLE);
            mCounterView.setVisibility(View.INVISIBLE);
        }
    }

    private void crossfade(final TouchImageView image) {
        Drawable drawable = image.getDrawable();
        if (drawable == null) {
            CoreAPI.debugLevel(1, "drawable null");
            return;
        }

        Bitmap bm = drawableToBitmap(drawable);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), blur(4, bm));

        if(mForegroundShowing){
            mBackground.setImageDrawable(bitmapDrawable);
            mBackground.animate().alpha(1.0f).setDuration(mShortAnimationDuration);
            mForeground.animate().alpha(0.0f).setDuration(mShortAnimationDuration);
        }else {
            mForeground.setImageDrawable(bitmapDrawable);
            mBackground.animate().alpha(0.0f).setDuration(mShortAnimationDuration);
            mForeground.animate().alpha(1.0f).setDuration(mShortAnimationDuration);
        }

        mForegroundShowing = !mForegroundShowing;

    }

    public void onAttach(Activity activity) {
        mHandler.postDelayed(loadBackground, 100);
        super.onAttach(activity);
    }

    public void setImages(List<TouchImageView> imageViews, int position) {
        mImageViews = imageViews;
        mPosition = position;
    }

    Runnable loadBackground = new Runnable() {
        @Override
        public void run() {
            if(! mImageViews.isEmpty() && mImageViews.get(mPosition).getDrawable() != null && isAdded()) {
                Bitmap bm = drawableToBitmap(mImageViews.get(mPosition).getDrawable());
                BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), blur(4, bm));

                mForeground.setImageDrawable(bitmapDrawable);
                mBackground.setImageDrawable(bitmapDrawable);
            } else {
                mHandler.postDelayed(this, 100);
            }
        }
    };

    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public Bitmap blur(int radius, Bitmap original) {
        int w = 20; //original.getWidth()/64;
        int h = 30; //original.getHeight()/64;

        Bitmap out = Bitmap.createScaledBitmap(original, w, h, true);

        int[] pix = new int[w * h];
        out.getPixels(pix, 0, w, 0, 0, w, h);

        for(int r = radius; r >= 1; r /= 2) {
            for(int i = r; i < h - r; i++) {
                for(int j = r; j < w - r; j++) {
                    int tl = pix[(i - r) * w + j - r];
                    int tr = pix[(i - r) * w + j + r];
                    int tc = pix[(i - r) * w + j];
                    int bl = pix[(i + r) * w + j - r];
                    int br = pix[(i + r) * w + j + r];
                    int bc = pix[(i + r) * w + j];
                    int cl = pix[i * w + j - r];
                    int cr = pix[i * w + j + r];

                    pix[(i * w) + j] = 0xFF000000 |
                            (((tl & 0xFF) + (tr & 0xFF) + (tc & 0xFF) + (bl & 0xFF) + (br & 0xFF) + (bc & 0xFF) + (cl & 0xFF) + (cr & 0xFF)) >> 3) & 0xFF |
                            (((tl & 0xFF00) + (tr & 0xFF00) + (tc & 0xFF00) + (bl & 0xFF00) + (br & 0xFF00) + (bc & 0xFF00) + (cl & 0xFF00) + (cr & 0xFF00)) >> 3) & 0xFF00 |
                            (((tl & 0xFF0000) + (tr & 0xFF0000) + (tc & 0xFF0000) + (bl & 0xFF0000) + (br & 0xFF0000) + (bc & 0xFF0000) + (cl & 0xFF0000) + (cr & 0xFF0000)) >> 3) & 0xFF0000;
                }
            }
        }
        out.setPixels(pix, 0, w, 0, 0, w, h);
        return out;
    }

    public static void pushFragment(NavigationActivity mActivity, List<TouchImageView> images, int currentItem) {
        FragmentTransaction transaction = mActivity.getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);

        ViewPagerFragment fragment = new ViewPagerFragment();
        fragment.setImages(images, currentItem);
        mActivity.pushFragment(fragment, transaction);
    }

    public static void popFragment(NavigationActivity mActivity) {
        FragmentTransaction transaction = mActivity.getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);

        mActivity.popFragment(transaction);
        mActivity.getFragmentManager().executePendingTransactions();
    }
}
