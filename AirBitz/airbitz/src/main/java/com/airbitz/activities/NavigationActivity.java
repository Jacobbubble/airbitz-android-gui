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

package com.airbitz.activities;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.airbitz.AirbitzApplication;
import com.airbitz.R;
import com.airbitz.adapters.AccountsAdapter;
import com.airbitz.api.AccountSettings;
import com.airbitz.api.CoreAPI;
import com.airbitz.api.DirectoryWrapper;
import com.airbitz.api.directory.DirectoryApi;
import com.airbitz.fragments.BaseFragment;
import com.airbitz.fragments.HelpFragment;
import com.airbitz.fragments.directory.BusinessDirectoryFragment;
import com.airbitz.fragments.login.LandingFragment;
import com.airbitz.fragments.login.SetupUsernameFragment;
import com.airbitz.fragments.login.SetupWriteItDownFragment;
import com.airbitz.fragments.login.SignUpFragment;
import com.airbitz.fragments.request.AddressRequestFragment;
import com.airbitz.fragments.request.OnAddressRequestListener;
import com.airbitz.fragments.request.RequestFragment;
import com.airbitz.fragments.send.SendConfirmationFragment;
import com.airbitz.fragments.send.SendFragment;
import com.airbitz.fragments.send.SuccessFragment;
import com.airbitz.fragments.settings.ImportFragment;
import com.airbitz.fragments.settings.PasswordRecoveryFragment;
import com.airbitz.fragments.settings.SettingFragment;
import com.airbitz.fragments.settings.twofactor.TwoFactorScanFragment;
import com.airbitz.fragments.wallet.TransactionListFragment;
import com.airbitz.fragments.wallet.WalletsFragment;
import com.airbitz.models.AirbitzNotification;
import com.airbitz.models.Transaction;
import com.airbitz.models.Wallet;
import com.airbitz.objects.AirbitzAlertReceiver;
import com.airbitz.objects.AudioPlayer;
import com.airbitz.objects.Disclaimer;
import com.airbitz.objects.PasswordCheckReceiver;
import com.airbitz.objects.RememberPasswordCheck;
import com.airbitz.objects.UserReview;
import com.airbitz.plugins.BuySellFragment;
import com.airbitz.plugins.GiftCardFragment;
import com.airbitz.plugins.PluginFragment;
import com.airbitz.plugins.PluginCheck;
import com.airbitz.utils.Common;
import com.airbitz.utils.ListViewUtility;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class NavigationActivity extends ActionBarActivity
        implements View.OnTouchListener,
        OnAddressRequestListener,
        TwoFactorScanFragment.OnTwoFactorQRScanResult,
        AccountsAdapter.OnButtonTouched {
    private final int DIALOG_TIMEOUT_MILLIS = 120000;

    public final String INCOMING_COUNT = "com.airbitz.navigation.incomingcount";

    public static final String LAST_MESSAGE_ID = "com.airbitz.navigation.LastMessageID";
    private Map<Integer, AirbitzNotification> mNotificationMap;

    public static final String URI_DATA = "com.airbitz.navigation.uri";
    public static final String URI_SOURCE = "URI";
    public static Typeface latoBlackTypeFace;
    public static Typeface latoRegularTypeFace;

    private final String TAG = getClass().getSimpleName();
    BroadcastReceiver ConnectivityChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();

            if (extras != null) {
                if (networkIsAvailable()) {
                    CoreAPI.debugLevel(1, "Connection available");
                    mCoreAPI.restoreConnectivity();
                    mConnectivityNotified = false;
                } else { // has connection
                    CoreAPI.debugLevel(1, "Connection NOT available");
                    mCoreAPI.lostConnectivity();
                    if (!mConnectivityNotified) {
                        ShowOkMessageDialog(getString(R.string.string_no_connection_title), getString(R.string.string_no_connection_message));
                    }
                    mConnectivityNotified = true;
                }
            }
        }
    };
    ViewGroup.LayoutParams mFragmentLayoutParams;
    int mNavBarStart;
    String mUUID, mTxId;
    Handler mHandler = new Handler();
    private CoreAPI mCoreAPI;
    private Uri mDataUri;
    private boolean keyBoardUp = false;
    private boolean mCalcLocked = false;
    private boolean mConnectivityNotified = false;
    private View mFragmentContainer;
    public LinearLayout mFragmentLayout;
    private LinearLayout mLandingLayout;
    private int mNavThreadId;
    private Fragment[] mNavFragments = {
            new BusinessDirectoryFragment(),
            new RequestFragment(),
            new SendFragment(),
            new TransactionListFragment(),
            new WalletsFragment(),
            new SettingFragment(),
            new ImportFragment(),
            new BuySellFragment(),
            new GiftCardFragment(),
    };
    // These stacks are the five "threads" of fragments represented in mNavFragments
    private Stack<Fragment>[] mNavStacks = null;
    private List<Fragment> mOverlayFragments = new ArrayList<Fragment>();
    // Callback interface when a wallet could be updated
    private Dialog mIncomingDialog;
    private LandingFragment mLandingFragment;
    final Runnable dialogKiller = new Runnable() {
        @Override
        public void run() {
            if (mIncomingDialog != null) {
                updateWalletListener();
                mIncomingDialog.dismiss(); // hide dialog
            }
        }
    };

    public Stack<AsyncTask> mAsyncTasks = new Stack<AsyncTask>();

    private DrawerLayout mDrawer;
    private FrameLayout mDrawerView;
    private View mDrawerLogin;
    private View mDrawerLayoutAccount;
    private RelativeLayout mDrawerBuySellLayout;
    private TextView mDrawerAccount;
    private ImageView mDrawerAccountArrow;
    private TextView mDrawerExchange;
    private Button mDrawerDirectory;
    private Button mDrawerRequest;
    private Button mDrawerSend;
    private Button mDrawerTxs;
    private Button mDrawerWallets;
    private Button mDrawerBuySell;
    private Button mDrawerShop;
    private Button mDrawerImport;
    private Button mDrawerSettings;
    private Button mDrawerLogout;
    private ListView mOtherAccountsListView;
    private AccountsAdapter mOtherAccountsAdapter;
    private List<String> mOtherAccounts;

    private RememberPasswordCheck mPasswordCheck;

    private boolean activityInForeground = false;

    private View mActionButton;
    private FloatingActionMenu mActionMenu;

    private ViewGroup mRoot;
    private int mTouchDown;

    private int mMenuPadding;
    private int mMenuWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        mCoreAPI = initiateCore(this);
        setContentView(R.layout.activity_navigation);


        Resources r = getResources();
        mMenuPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, r.getDisplayMetrics());
        mMenuWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 75, r.getDisplayMetrics());
        FrameLayout.LayoutParams menuLayout = new FrameLayout.LayoutParams(mMenuWidth, mMenuWidth);

        mActionButton = findViewById(R.id.action_button);

        ImageView requestButton = new ImageView(this);
        ImageView sendButton = new ImageView(this);
        ImageView txButton = new ImageView(this);
        requestButton.setImageResource(R.drawable.ic_receive_dark);
        requestButton.setPadding(mMenuPadding, mMenuPadding, mMenuPadding, mMenuPadding);
        sendButton.setImageResource(R.drawable.ic_send_dark);
        sendButton.setPadding(mMenuPadding, mMenuPadding, mMenuPadding, mMenuPadding);
        txButton.setImageResource(R.drawable.ic_transactions_dark);
        txButton.setPadding(mMenuPadding, mMenuPadding, mMenuPadding, mMenuPadding);

        SubActionButton.Builder itemBuilder = new SubActionButton.Builder(this);

        SubActionButton receiveAction = itemBuilder.setLayoutParams(menuLayout).setContentView(requestButton).build();
        SubActionButton sendAction = itemBuilder.setLayoutParams(menuLayout).setContentView(sendButton).build();
        SubActionButton txAction = itemBuilder.setLayoutParams(menuLayout).setContentView(txButton).build();

        mActionMenu =
            new FloatingActionMenu.Builder(this)
                                  .addSubActionView(receiveAction)
                                  .addSubActionView(sendAction)
                                  .addSubActionView(txAction)
                                  .attachTo(mActionButton)
                                  .contentView(findViewById(R.id.action_menu_container))
                                  .build();

        receiveAction.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onNavBarSelected(Tabs.REQUEST.ordinal());
            }
        });

        sendAction.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onNavBarSelected(Tabs.SEND.ordinal());
            }
        });

        txAction.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onNavBarSelected(Tabs.WALLET.ordinal());
            }
        });

        mFragmentContainer = findViewById(R.id.fragment_container);
        mFragmentLayout = (LinearLayout) findViewById(R.id.activityLayout);

        Common.addStatusBarPadding(this, mFragmentContainer);

        setTypeFaces();

        mNavStacks = AirbitzApplication.getFragmentStack();
        AirbitzApplication.setFragmentStack(null);
        if(mNavStacks == null) {
            mNavStacks = new Stack[mNavFragments.length];
            for (int i = 0; i < mNavFragments.length; i++) {
                mNavStacks[i] = new Stack<Fragment>();
                mNavStacks[i].push(mNavFragments[i]);
            }
        }

        mLandingFragment = new LandingFragment();

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.fade_in, 0);
        transaction.replace(R.id.landing_overlay, mLandingFragment);
        transaction.commitAllowingStateLoss();

        mLandingLayout = (LinearLayout) findViewById(R.id.landing_overlay);

        // Navigation Drawer slideout
        setupDrawer();
        updateDrawer(false);

        mRoot = (ViewGroup)findViewById(R.id.activity_navigation_root);

        IntentFilter filter = new IntentFilter();
        filter.addAction(CoreAPI.WALLET_LOADING_START_ACTION);
        filter.addAction(CoreAPI.WALLET_LOADING_STATUS_ACTION);
        filter.addAction(CoreAPI.WALLETS_ALL_LOADED_ACTION);
        filter.addAction(CoreAPI.WALLETS_LOADING_BITCOIN_ACTION);
        filter.addAction(CoreAPI.WALLETS_LOADED_BITCOIN_ACTION);
        mWalletsLoadedReceiver = new WalletReceiver();
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mWalletsLoadedReceiver, filter);
        manager.registerReceiver(mExchangeReceiver, new IntentFilter(CoreAPI.EXCHANGE_RATE_UPDATED_ACTION));
        manager.registerReceiver(mBlockHeightReceiver, new IntentFilter(CoreAPI.BLOCKHEIGHT_CHANGE_ACTION));
        manager.registerReceiver(mIncomingBitcoinReceiver, new IntentFilter(CoreAPI.INCOMING_BITCOIN_ACTION));
        manager.registerReceiver(mRemotePasswordChange, new IntentFilter(CoreAPI.REMOTE_PASSWORD_CHANGE_ACTION));
        manager.registerReceiver(mDataSyncReceiver, new IntentFilter(CoreAPI.DATASYNC_UPDATE_ACTION));
        manager.registerReceiver(mOtpErrorReceiver, new IntentFilter(CoreAPI.OTP_ERROR_ACTION));
        manager.registerReceiver(mOtpResetReceiver, new IntentFilter(CoreAPI.OTP_RESET_ACTION));

        // Let's see what plugins are enabled
        PluginCheck.checkEnabledPlugins();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(!getResources().getBoolean(R.bool.portrait_only)){
            // store the fragment stack in case of an orientation change
            AirbitzApplication.setFragmentStack(mNavStacks);
            AirbitzApplication.setLastNavTab(mNavThreadId);
        }
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.unregisterReceiver(mWalletsLoadedReceiver);
        manager.unregisterReceiver(mExchangeReceiver);
        manager.unregisterReceiver(mBlockHeightReceiver);
        manager.unregisterReceiver(mIncomingBitcoinReceiver);
        manager.unregisterReceiver(mRemotePasswordChange);
        manager.unregisterReceiver(mDataSyncReceiver);
        manager.unregisterReceiver(mOtpErrorReceiver);
        manager.unregisterReceiver(mOtpResetReceiver);
    }

    public boolean onTouch(View view, MotionEvent event) {

        int X = (int) event.getRawX();
        int deltaX = X - mTouchDown;

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mTouchDown = X;
                Log.d("", "ACTION_DOWN: " + String.valueOf((int) X));
                break;
            case MotionEvent.ACTION_UP:
                Log.d("", "ACTION_UP: " + String.valueOf((int) X));
                if (deltaX > view.getWidth() / 2) {
                    DisplayLoginOverlay(false, true, false);
                    hideSoftKeyboard(view);
                } else {
                    DisplayLoginOverlay(true, true, false);
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d("", "ACTION_MOVE: " + String.valueOf((int) X) + " mTouchDown:" + mTouchDown);
                if (deltaX < 0)
                    deltaX = 0;

                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mLandingLayout.getLayoutParams();
                layoutParams.topMargin = 0;
                layoutParams.leftMargin = deltaX;
                layoutParams.rightMargin = -layoutParams.leftMargin;
                layoutParams.bottomMargin = 0;
                mLandingLayout.setLayoutParams(layoutParams);

                // Move the bizdir inward as the Landing fragment moves outward
                layoutParams = (RelativeLayout.LayoutParams) mFragmentLayout.getLayoutParams();
                layoutParams.topMargin = 0;
                layoutParams.leftMargin = -mFragmentLayout.getWidth() + (deltaX);
                layoutParams.rightMargin = -layoutParams.leftMargin;
                layoutParams.bottomMargin = 0;

                mFragmentLayout.setLayoutParams(layoutParams);
                float alpha = (float) (deltaX) / (float) mFragmentLayout.getWidth();
                mFragmentLayout.setAlpha(alpha);

                break;
        }
        mRoot.invalidate();
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Fragment frag = mNavStacks[mNavThreadId].peek();
        if (frag != null) {
            frag.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void setCoreListeners(NavigationActivity activity) {
    }

    public static CoreAPI initiateCore(Context context) {
        CoreAPI api = CoreAPI.getApi(context);
        String seed = CoreAPI.getSeedData();
        String airbitzApiKey = AirbitzApplication.getContext().getString(R.string.airbitz_api_key);
        String hiddenbitzKey = AirbitzApplication.getContext().getString(R.string.hiddenbitz_key);
        api.Initialize(context, airbitzApiKey, hiddenbitzKey, seed, seed.length());
        return api;
    }

    public void DisplayLoginOverlay(boolean overlay) {
        DisplayLoginOverlay(overlay, false);
    }

    public void DisplayLoginOverlay(boolean overlay, boolean animate) {
        DisplayLoginOverlay(overlay, animate, true);
    }

    public void DisplayLoginOverlay(boolean overlay, boolean animate, boolean fullRefresh) {

        if (!overlay) {
            // Show FragmentLayout
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mLandingLayout.getLayoutParams();
            layoutParams.topMargin = 0;
            layoutParams.leftMargin = mLandingLayout.getWidth();
            layoutParams.rightMargin = -layoutParams.leftMargin;
            layoutParams.bottomMargin = 0;
            mLandingLayout.setLayoutParams(layoutParams);
            mLandingLayout.setVisibility(View.INVISIBLE);

            // Move the bizdir inward as the Landing fragment moves outward
            layoutParams = (RelativeLayout.LayoutParams) mFragmentLayout.getLayoutParams();
            layoutParams.topMargin = 0;
            layoutParams.leftMargin = 0;
            layoutParams.rightMargin = 0;
            layoutParams.bottomMargin = 0;
            mFragmentLayout.setLayoutParams(layoutParams);
            mFragmentLayout.setAlpha(1.0f);
        } else {
            // Go back to showing Landing
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mLandingLayout.getLayoutParams();
            layoutParams.topMargin = 0;
            layoutParams.leftMargin = 0;
            layoutParams.rightMargin = 0;
            layoutParams.bottomMargin = 0;
            mLandingLayout.setLayoutParams(layoutParams);
            mLandingLayout.setVisibility(View.VISIBLE);

            layoutParams = (RelativeLayout.LayoutParams) mFragmentLayout.getLayoutParams();
            layoutParams.topMargin = 0;
            layoutParams.leftMargin = -mFragmentLayout.getWidth();
            layoutParams.rightMargin = -layoutParams.leftMargin;
            layoutParams.bottomMargin = 0;
            mFragmentLayout.setLayoutParams(layoutParams);
            mFragmentLayout.setAlpha(0.0f);
            hideNavBar();
            mDrawer.closeDrawer(mDrawerView);
            if (fullRefresh) {
                mLandingFragment.refreshViewAndUsername();
            } else {
                mLandingFragment.refreshView();
            }
        }
    }

    private void setTypeFaces() {
        latoBlackTypeFace = Typeface.createFromAsset(getAssets(), "font/Lato-Bla.ttf");
        latoRegularTypeFace = Typeface.createFromAsset(getAssets(), "font/Lato-Regular.ttf");
    }

    /*
        Implements interface to receive navigation changes from the bottom nav bar
     */
    public void onNavBarSelected(int position) {
        if (AirbitzApplication.isLoggedIn()) {
            hideSoftKeyboard(mFragmentLayout);
            if (position != mNavThreadId) {
                if(findViewById(R.id.modal_indefinite_progress).getVisibility() == View.VISIBLE) {
                    showModalProgress(false);
                }
                AirbitzApplication.setLastNavTab(position);
                switchFragmentThread(position);
            } else if (position == Tabs.MORE.ordinal()) {
                openDrawer();
            } else if (!isAtNavStackEntry()) {
                onBackPressed();
            }
        } else {
            if (position != Tabs.BD.ordinal()) {
                AirbitzApplication.setLastNavTab(position);
                DisplayLoginOverlay(true, true);
            }
        }
        resetDrawerButtons();
    }

    public void switchFragmentThread(int id) {
        switchFragmentThread(id, true);
    }

    public void switchFragmentThread(int id, boolean animation) {
        Fragment frag = mNavStacks[id].peek();
        Fragment fragShown = getFragmentManager().findFragmentById(R.id.activityLayout);
        if (fragShown != null)
            CoreAPI.debugLevel(1, "switchFragmentThread frag, fragShown is " + frag.getClass().getSimpleName() + ", " + fragShown.getClass().getSimpleName());
        else
            CoreAPI.debugLevel(1, "switchFragmentThread no fragment showing yet ");

        CoreAPI.debugLevel(1, "switchFragmentThread pending transactions executed ");

        FragmentTransaction transaction = getFragmentManager().beginTransaction().disallowAddToBackStack();
        if (frag.isAdded()) {
            CoreAPI.debugLevel(1, "Fragment already added, detaching and attaching");
            transaction.detach(mNavStacks[mNavThreadId].peek());
            transaction.attach(frag);
        } else {
            if (animation) {
                transaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);
            }
            transaction.replace(R.id.activityLayout, frag);
            CoreAPI.debugLevel(1, "switchFragmentThread replace executed.");
        }
        transaction.commit();
        CoreAPI.debugLevel(1, "switchFragmentThread transactions committed.");
        fragShown = getFragmentManager().findFragmentById(R.id.activityLayout);
        if (fragShown != null) {
            CoreAPI.debugLevel(1, "switchFragmentThread showing frag is " + fragShown.getClass().getSimpleName());
        } else {
            CoreAPI.debugLevel(1, "switchFragmentThread showing frag is null");
        }
        AirbitzApplication.setLastNavTab(id);
        mNavThreadId = id;

        CoreAPI.debugLevel(1, "switchFragmentThread switch to threadId " + mNavThreadId);

        getFragmentManager().executePendingTransactions();
        resetDrawerButtons();
    }

    public void switchFragmentThread(int id, Bundle bundle) {
        if (bundle != null)
            mNavStacks[id].peek().setArguments(bundle);
        switchFragmentThread(id);
    }

    public void pushFragment(Fragment fragment, FragmentTransaction transaction) {
        mNavStacks[mNavThreadId].push(fragment);
        transaction.replace(R.id.activityLayout, fragment);
        transaction.commitAllowingStateLoss();

        resetDrawerButtons();
    }

    public void pushFragment(Fragment fragment) {
        pushFragment(fragment, mNavThreadId);
        resetDrawerButtons();
    }

    public void pushFragment(Fragment fragment, int threadID) {
        mNavStacks[threadID].push(fragment);

        // Only show visually if we're displaying the thread
        if (mNavThreadId == threadID) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            if (mNavStacks[threadID].size() != 0) {
                if (fragment instanceof HelpFragment) {
                    transaction.setCustomAnimations(R.animator.fade_in, 0);
                } else {
                    transaction.setCustomAnimations(R.animator.slide_in_from_right, R.animator.slide_out_left);
                }
            }
            transaction.replace(R.id.activityLayout, fragment);
            transaction.commitAllowingStateLoss();
        }
        resetDrawerButtons();
    }

    public void pushFragmentNoAnimation(Fragment fragment, int threadID) {
        mNavStacks[threadID].push(fragment);

        // Only show visually if we're displaying the thread
        if (mNavThreadId == threadID) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.activityLayout, fragment);
            transaction.commitAllowingStateLoss();
        }
        getFragmentManager().executePendingTransactions();
        resetDrawerButtons();
    }

    public void popFragment(FragmentTransaction transaction) {
        hideSoftKeyboard(mFragmentLayout);
        Fragment fragment = mNavStacks[mNavThreadId].pop();
        getFragmentManager().executePendingTransactions();

        transaction.replace(R.id.activityLayout, mNavStacks[mNavThreadId].peek());
        transaction.commitAllowingStateLoss();
    }

    public void popFragment() {
        hideSoftKeyboard(mFragmentLayout);
        Fragment fragment = mNavStacks[mNavThreadId].pop();
        getFragmentManager().executePendingTransactions();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (mNavStacks[mNavThreadId].size() != 0) {
            if (fragment instanceof HelpFragment) {
                transaction.setCustomAnimations(0, R.animator.fade_out);
            } else {
                transaction.setCustomAnimations(R.animator.slide_in_from_left, R.animator.slide_out_right);
            }
        }
        transaction.replace(R.id.activityLayout, mNavStacks[mNavThreadId].peek());
        transaction.commitAllowingStateLoss();
    }

    private ViewGroup.LayoutParams getFragmentLayoutParams() {
        if (mFragmentLayoutParams == null) {
            mFragmentLayoutParams = mFragmentLayout.getLayoutParams();
        }
        return mFragmentLayoutParams;
    }

    private boolean mNavBarAnimating = false;
    static final int NAV_BAR_ANIMATE = 250;

    public void hideNavBar() {
        hideNavBar(NAV_BAR_ANIMATE);
    }

    public void hideNavBar(int duration) {
        final float bottom = mFragmentContainer.getBottom();
        if (!mNavBarAnimating && mActionButton.getY() != bottom) {
            ObjectAnimator key = ObjectAnimator.ofFloat(mActionButton, "y", mActionButton.getY(), bottom);
            key.setDuration(duration);
            key.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator aniamtor) {
                    mActionButton.setVisibility(View.INVISIBLE);
                    mNavBarAnimating = false;
                }

                @Override
                public void onAnimationStart(Animator animator) {
                    mActionButton.setVisibility(View.VISIBLE);
                    mNavBarAnimating = true;
                }
            });
            key.start();
        }
    }

    public View getFabView() {
        return mActionButton;
    }

    public float getFabHeight() {
        return mActionButton.getHeight() + mMenuPadding;
    }

    public float getFabTop() {
        return getBottom() - Common.getStatusBarHeight(this) - getFabHeight();
    }

    public void showNavBar() {
        showNavBar(getFabTop());
    }

    public void showNavBar(float animateTo) {
        if (!AirbitzApplication.isLoggedIn()) {
            hideNavBar(0);
            return;
        }
        if (!mNavBarAnimating && mActionButton.getY() != animateTo) {
            final ValueAnimator val = ValueAnimator.ofFloat(1f, 0f);
            val.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    Fragment fragment = mNavStacks[mNavThreadId].peek();
                    if (fragment instanceof BaseFragment) {
                        ((BaseFragment) fragment).finishFabAnimation();
                    }
                }
            });

            ObjectAnimator key = ObjectAnimator.ofFloat(mActionButton, "y", mActionButton.getY(), animateTo);
            key.setDuration(NAV_BAR_ANIMATE);
            key.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator aniamtor) {
                    mActionButton.setVisibility(View.VISIBLE);
                    mNavBarAnimating = false;
                }

                @Override
                public void onAnimationStart(Animator animator) {
                    mActionButton.setVisibility(View.VISIBLE);
                    mNavBarAnimating = true;
                }
            });

            AnimatorSet set = new AnimatorSet();
            set.setDuration(NAV_BAR_ANIMATE);
            set.playTogether(key, val);
            set.start();
        }
    }

    private float getBottom() {
        return getWindow().getDecorView().findViewById(android.R.id.content).getBottom();
    }

    @Override
    public void onBackPressed() {
        if (mLandingLayout.getVisibility() == View.VISIBLE) {
            View v = findViewById(R.id.modal_indefinite_progress);
            if (v.getVisibility() != View.VISIBLE) {
                DisplayLoginOverlay(false, true);
            }
            return;
        }

        // If fragments want the back key, they can have it
        Fragment fragment = mNavStacks[mNavThreadId].peek();
        if (fragment instanceof OnBackPress) {
            boolean handled = ((OnBackPress) fragment).onBackPress();
            if (handled)
                return;
        }

        showModalProgress(false);

        if (isAtNavStackEntry()) {
            if (AirbitzApplication.isLoggedIn() && Tabs.WALLET.ordinal() != mNavThreadId) {
                onNavBarSelected(Tabs.WALLET.ordinal());
            } else {
                ShowExitMessageDialog("", String.format(
                    getString(R.string.string_exit_app_question),
                    getString(R.string.app_name)));
            }
        } else {
            popFragment();
        }

        showNavBar();
    }

    public boolean isAtNavStackEntry() {
        return mNavStacks[mNavThreadId].size() == 1;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11 in Support Package
    }

    @Override
    public void onResume() {
        //******************* HockeyApp support
        // Always check for crashes and send to Hockey if user chooses to
        String hockeyKey = getString(R.string.hockey_key);
        CrashManager.register(this, hockeyKey);

        // Only allow updates for debug builds
        if (AirbitzApplication.isDebugging()) {
            UpdateManager.register(this, hockeyKey);
        }
        //******************* end HockeyApp support

        //Look for Connection change events
        registerReceiver(ConnectivityChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        mNavThreadId = AirbitzApplication.getLastNavTab();

        if (loginExpired() || !AirbitzApplication.isLoggedIn()) {
            DisplayLoginOverlay(true);
            mNavThreadId = Tabs.BD.ordinal();
        } else {
            DisplayLoginOverlay(false);
            mCoreAPI.restoreConnectivity();
        }
        updateDrawer(AirbitzApplication.isLoggedIn());
        switchFragmentThread(mNavThreadId);

        AirbitzAlertReceiver.CancelNextAlertAlarm(this, AirbitzAlertReceiver.ALERT_NOTIFICATION_CODE);
        AirbitzAlertReceiver.CancelNextAlertAlarm(this, AirbitzAlertReceiver.ALERT_NEW_BUSINESS_CODE);

        checkNotifications();

        if (SettingFragment.getNFCPref()) {
            setupNFCForegrounding();
        }

        Intent intent = getIntent();
        if (intent != null) {
            if (PasswordCheckReceiver.USER_SWITCH.equals(intent.getAction())) {
                saveCachedLoginName(intent.getStringExtra(PasswordCheckReceiver.USERNAME));
                mLandingFragment.refreshViewAndUsername();
            } else {
                Uri data = intent.getData();
                if (data != null) {
                    processUri(data);
                }
            }
            // XXX: replace intent that launched the app
            // it is a result of this dirty "god" activity
            setIntent(new Intent());
        }

        if (null != mPasswordCheck) {
            mPasswordCheck.onResume();
        }

        setCoreListeners(this);

        activityInForeground = true;

        hideSoftKeyboard(mFragmentContainer);

        super.onResume();

        checkDisclaimer();
    }

    private void checkDisclaimer() {
        new Thread(new Runnable() {
            public void run() {
                if (!Disclaimer.hasAgreedDisclaimer(NavigationActivity.this)) {
                    showDisclaimer();
                }
            }
        }).start();
    }

    private void showDisclaimer() {
        mHandler.post(new Runnable() {
            public void run() {
                Disclaimer.showDisclaimer(NavigationActivity.this);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        setCoreListeners(null);

        activityInForeground = false;
        unregisterReceiver(ConnectivityChangeReceiver);
        mCoreAPI.lostConnectivity();
        AirbitzApplication.setBackgroundedTime(System.currentTimeMillis());
        AirbitzAlertReceiver.SetAllRepeatingAlerts(this);
        PasswordCheckReceiver.setup(this);
        if(SettingFragment.getNFCPref()) {
            disableNFCForegrounding();
        }
        if (null != mPasswordCheck) {
            mPasswordCheck.onPause();
        }
        mOTPResetRequestDialog = null; // To allow the message again if foregrounding
    }

    /*
     * this only gets called from sent funds, or a request comes through
     */
    public void switchToWallets(Bundle bundle) {
        Fragment frag = new TransactionListFragment();
        bundle.putBoolean(WalletsFragment.CREATE, true);
        frag.setArguments(bundle);
        mNavStacks[Tabs.WALLET.ordinal()].clear();
        mNavStacks[Tabs.WALLET.ordinal()].add(frag);

        switchFragmentThread(Tabs.WALLET.ordinal());
    }

    public void clearBD() {
        Fragment top = mNavStacks[Tabs.BD.ordinal()].peek();
        while (!(top instanceof BusinessDirectoryFragment)) {
            mNavStacks[Tabs.BD.ordinal()].pop();
            top = mNavStacks[Tabs.BD.ordinal()].peek();
        }
    }

    //********************** NFC support
    @Override
    protected void onNewIntent(final Intent intent)
    {
        final String action = intent.getAction();
        final Uri intentUri = intent.getData();
        final String type = intent.getType();
        final String scheme = intentUri != null ? intentUri.getScheme() : null;

        CoreAPI.debugLevel(1, "New Intent action=" + action + ", data=" + intentUri + ", type=" + type + ", scheme=" + scheme);

        if (PasswordCheckReceiver.USER_SWITCH.equals(action)) {
            saveCachedLoginName(intent.getStringExtra(PasswordCheckReceiver.USERNAME));
            mLandingFragment.refreshViewAndUsername();
        } else if (intentUri != null && action != null && (Intent.ACTION_VIEW.equals(action) ||
                (SettingFragment.getNFCPref() && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)))) {
            processUri(intentUri);
        } else if(type != null && type.equals(AirbitzAlertReceiver.ALERT_NOTIFICATION_TYPE)) {
            CoreAPI.debugLevel(1, "Notification type found");
                mNotificationTask = new NotificationTask();
                mNotificationTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public void setupNFCForegrounding() {
        // Create a generic PendingIntent that will be deliver to this activity. The NFC stack
        // will fill in the intent with the details of the discovered tag before delivering to
        // this activity.
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        ndef.addDataScheme("bitcoin");
        IntentFilter[] filters = new IntentFilter[] { ndef };

        final NfcManager nfcManager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        NfcAdapter nfcAdapter = nfcManager.getDefaultAdapter();

        if (nfcAdapter != null && nfcAdapter.isEnabled() && SettingFragment.getNFCPref()) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, null);
        }
    }

    public void disableNFCForegrounding() {
        final NfcManager nfcManager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        NfcAdapter nfcAdapter = nfcManager.getDefaultAdapter();

        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    private void processUri(Uri uri) {
        if(uri == null || uri.getScheme() == null) {
            CoreAPI.debugLevel(1, "Null uri or uri.scheme");
            return;
        }

        if (!AirbitzApplication.isLoggedIn()) {
            mDataUri = uri;
            return;
        }

        String scheme = uri.getScheme();
        if ("airbitz".equals(scheme) && "plugin".equals(uri.getHost())) {
            List<String> path = uri.getPathSegments();
            if (2 <= path.size()) {
                CoreAPI.debugLevel(1, uri.toString());
                launchBuySell(path.get(1), path.get(0), uri);
            }
        } else if ("bitcoin".equals(scheme)
                || "airbitz".equals(scheme)
                || "bitid".equals(scheme)) {
            handleBitcoinUri(uri);
        }
        else if("bitcoin-ret".equals(scheme)
                || "x-callback-url".equals(scheme)
                || "airbitz-ret".equals(scheme)) {
            handleRequestForPaymentUri(uri);
        }
        else if (ImportFragment.getHiddenBitsToken(uri.toString()) != null) {
            gotoImportNow(uri);
        }
    }

    /*
     * Handle bitcoin-ret or x-callback-url Uri's coming from OS
     */
    public void handleRequestForPaymentUri(Uri uri) {
        AddressRequestFragment fragment = new AddressRequestFragment();
        fragment.setOnAddressRequestListener(this);
        Bundle bundle = new Bundle();
        bundle.putString(AddressRequestFragment.URI, uri.toString());
        fragment.setArguments(bundle);
        pushFragment(fragment);
    }

    @Override
    public void onAddressRequest() {
        popFragment();
        mDataUri = null;
    }

    private void launchBuySell(String country, String provider, Uri uri) {
        resetFragmentThreadToBaseFragment(Tabs.BUYSELL.ordinal());

        BuySellFragment buySell = (BuySellFragment) mNavStacks[Tabs.BUYSELL.ordinal()].peek();
        switchFragmentThread(Tabs.BUYSELL.ordinal());
        mDrawer.closeDrawer(mDrawerView);

        FragmentManager manager = getFragmentManager();
        if (manager != null) {
            manager.executePendingTransactions();
        }
        buySell.launchPluginByCountry(country, provider, uri);
    }

    /*
     * Handle bitcoin:<address> Uri's coming from OS
     */
    private void handleBitcoinUri(Uri dataUri) {
        CoreAPI.debugLevel(1, "Received onBitcoin with uri = " + dataUri.toString());
        resetFragmentThreadToBaseFragment(Tabs.SEND.ordinal());

        Bundle bundle = new Bundle();
        bundle.putString(WalletsFragment.FROM_SOURCE, URI_SOURCE);
        bundle.putString(URI_DATA, dataUri.toString());
        switchFragmentThread(Tabs.SEND.ordinal(), bundle);
    }

    public void onIncomingBitcoin(String walletUUID, String txId) {
        CoreAPI.debugLevel(1, "onIncomingBitcoin uuid, txid = " + walletUUID + ", " + txId);
        mUUID = walletUUID;
        mTxId = txId;

        // If in merchant donation mode, stay on QR screen and show amount
        RequestFragment fragment = requestMatchesDonation();
        if(fragment != null) {
            AudioPlayer.play(this, R.raw.bitcoin_received);
            fragment.showDonation(mUUID, mTxId);
            return;
        }

        /* If showing QR code, launch receiving screen*/
        RequestFragment f = requestMatchesQR(mUUID, mTxId);
        CoreAPI.debugLevel(1, "RequestFragment? " + f);
        if (f != null) {
            long diff = f.requestDifference(mUUID, mTxId);
            if (diff <= 0) {
                // sender paid exact amount
                AudioPlayer.play(this, R.raw.bitcoin_received);
                handleReceiveFromQR();
            }
            else {
                // Request the remainder of the funds
                f.updateWithAmount(diff);
                AudioPlayer.play(this, R.raw.bitcoin_received_partial);
            }
        } else {
            Transaction tx = mCoreAPI.getTransaction(walletUUID, txId);
            if (null != tx) {
                if (tx.getAmountSatoshi() > 0) {
                    AudioPlayer.play(this, R.raw.bitcoin_received);
                    showIncomingBitcoinDialog();
                    updateWalletListener();
                }
            }
        }
    }

    private void handleReceiveFromQR() {
        if (!SettingFragment.getMerchantModePref()) {
            Bundle bundle = new Bundle();
            bundle.putString(WalletsFragment.FROM_SOURCE, SuccessFragment.TYPE_REQUEST);
            bundle.putString(Transaction.TXID, mTxId);
            bundle.putString(Wallet.WALLET_UUID, mUUID);
            switchToWallets(bundle);
            resetFragmentThreadToBaseFragment(Tabs.REQUEST.ordinal());
        }
        else {
            hideSoftKeyboard(mFragmentLayout);
            Bundle bundle = new Bundle();
            bundle.putString(RequestFragment.MERCHANT_MODE, "merchant");
            resetFragmentThreadToBaseFragment(NavigationActivity.Tabs.REQUEST.ordinal());
            switchFragmentThread(NavigationActivity.Tabs.REQUEST.ordinal(), bundle);
        }
        showIncomingDialog(mUUID, mTxId, true);
    }

    private void showIncomingDialog(String uuid, String txId, boolean withTeaching) {
        Wallet wallet = mCoreAPI.getWalletFromUUID(uuid);
        Transaction transaction = mCoreAPI.getTransaction(uuid, txId);
        String coinValue = mCoreAPI.formatSatoshi(transaction.getAmountSatoshi(), true);
        String currencyValue = null;
        // If no value set, then calculate it
        if (transaction.getAmountFiat() == 0.0) {
            currencyValue = mCoreAPI.FormatCurrency(transaction.getAmountSatoshi(), wallet.getCurrencyNum(),
                    false, true);
        } else {
            currencyValue = mCoreAPI.formatCurrency(transaction.getAmountFiat(),
                    wallet.getCurrencyNum(), true);
        }
        String message = String.format(getString(R.string.received_bitcoin_fading_message), coinValue, currencyValue);
        if(withTeaching) {
            SharedPreferences prefs = AirbitzApplication.getContext().getSharedPreferences(AirbitzApplication.PREFS, Context.MODE_PRIVATE);
            int count = prefs.getInt(INCOMING_COUNT, 1);
            if(count <= 2 && !SettingFragment.getMerchantModePref()) {
                count++;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(INCOMING_COUNT, count);
                editor.apply();
                message += " " + getString(R.string.received_bitcoin_fading_message_teaching);
            }
        }

        ShowFadingDialog(message, getResources().getInteger(R.integer.alert_hold_time_payment_received));
    }

    private RequestFragment requestMatchesQR(String uuid, String txid) {
        Fragment f = mNavStacks[mNavThreadId].peek();
        if (!(f instanceof RequestFragment)) {
            return null;
        }
        RequestFragment qr = (RequestFragment) f;
        if (qr.isShowingQRCodeFor(uuid, txid)) {
            return qr;
        } else {
            return null;
        }
    }

    private RequestFragment requestMatchesDonation() {
        Fragment f = mNavStacks[mNavThreadId].peek();
        if (!(f instanceof RequestFragment)) {
            return null;
        }
        RequestFragment qr = (RequestFragment) f;
        if (qr.isMerchantDonation()) {
            return qr;
        } else {
            return null;
        }
    }

    public void onSentFunds(String walletUUID, String txId, String returnUrl) {
        CoreAPI.debugLevel(1, "onSentFunds uuid, txid = " + walletUUID + ", " + txId);

        FragmentManager manager = getFragmentManager();
        if(manager != null) {
            manager.executePendingTransactions();
        }

        Bundle bundle = new Bundle();
        bundle.putString(WalletsFragment.FROM_SOURCE, SuccessFragment.TYPE_SEND);
        bundle.putBoolean(WalletsFragment.CREATE, true);
        bundle.putString(Transaction.TXID, txId);
        bundle.putString(Wallet.WALLET_UUID, walletUUID);
        bundle.putString(SendFragment.RETURN_URL, returnUrl);

        CoreAPI.debugLevel(1, "onSentFunds calling switchToWallets");
        switchToWallets(bundle);

        while (mNavStacks[Tabs.SEND.ordinal()].size() > 0) {
            CoreAPI.debugLevel(1, "Send thread removing " + mNavStacks[Tabs.SEND.ordinal()].peek().getClass().getSimpleName());
            mNavStacks[Tabs.SEND.ordinal()].pop();
        }
        Fragment frag = getNewBaseFragement(Tabs.SEND.ordinal());
        mNavStacks[Tabs.SEND.ordinal()].push(frag); // Set first fragment but don't show
    }

    private void updateWalletListener() {
        mCoreAPI.reloadWallets();
    }

    private void gotoDetailsNow() {
        Bundle bundle = new Bundle();
        bundle.putString(WalletsFragment.FROM_SOURCE, SuccessFragment.TYPE_REQUEST);
        bundle.putString(Transaction.TXID, mTxId);
        bundle.putString(Wallet.WALLET_UUID, mUUID);
        switchToWallets(bundle);

        resetFragmentThreadToBaseFragment(Tabs.REQUEST.ordinal());
    }

    private void gotoImportNow(Uri uri) {
        switchFragmentThread(Tabs.IMPORT.ordinal());

        ImportFragment fragment = (ImportFragment) mNavStacks[Tabs.IMPORT.ordinal()].peek();
        fragment.processAddress(uri.toString());
    }

    public void resetFragmentThreadToBaseFragment(int threadId) {
        mNavStacks[threadId].clear();
        mNavStacks[threadId].add(getNewBaseFragement(threadId));
    }

    private void showIncomingBitcoinDialog() {
        if (!this.isFinishing()) {
            AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
            builder.setMessage(getResources().getString(R.string.received_bitcoin_message))
                    .setTitle(getResources().getString(R.string.received_bitcoin_title))
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.received_bitcoin_positive),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    gotoDetailsNow();
                                }
                            }
                    )
                    .setNegativeButton(getResources().getString(R.string.received_bitcoin_negative),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    resetFragmentThreadToBaseFragment(Tabs.REQUEST.ordinal());
                                    updateWalletListener();
                                    dialog.cancel();
                                }
                            }
                    );
            mIncomingDialog = builder.create();
            mIncomingDialog.show();
            mHandler.postDelayed(dialogKiller, getResources().getInteger(R.integer.alert_hold_time_payment_received));
        }
    }

    Dialog mRemoteChange = null;
    private void showRemotePasswordChangeDialog() {
        if (mRemoteChange == null && !this.isFinishing()) {
            AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
            builder.setMessage(getResources().getString(R.string.remote_password_change_message))
                    .setTitle(getResources().getString(R.string.remote_password_change_title))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.string_ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Logout();
                                    dialog.cancel();
                                    mRemoteChange = null;
                                }
                            }
                    );
            mRemoteChange = builder.create();
            mRemoteChange.show();
        }
    }

    public void UserJustLoggedIn(boolean passwordLogin, boolean showLoadingMessages) {
        mWalletsLoadedReceiver.mShowMessages = showLoadingMessages;
        UserJustLoggedIn(passwordLogin);
    }

    public void UserJustLoggedIn(boolean passwordLogin) {
        showNavBar();
        checkDailyLimitPref();
        mCoreAPI.setupAccountSettings();
        mCoreAPI.startAllAsyncUpdates();
        if (mDataUri != null) {
            processUri(mDataUri);
            mDataUri = null;
        } else {
            resetFragmentThreadToBaseFragment(mNavThreadId);
            AirbitzApplication.setLastNavTab(Tabs.WALLET.ordinal());
            resetFragmentThreadToBaseFragment(Tabs.WALLET.ordinal());
            switchFragmentThread(Tabs.WALLET.ordinal(), false);
        }
        checkFirstWalletSetup();
        AccountSettings settings = mCoreAPI.coreSettings();
        if (settings != null) {
            if (!settings.getBDisablePINLogin() && passwordLogin) {
                mCoreAPI.PinSetup();
            }
        }

        List<String> cats = mCoreAPI.loadCategories();
        if (cats.size() == 0)
            createDefaultCategories();

        DisplayLoginOverlay(false, true);

        boolean checkPassword = false;
        // if the user has a password, increment PIN login count
        if (mCoreAPI.PasswordExists()) {
            checkPassword = mCoreAPI.incrementPinCount();
        }

        if(!passwordLogin && !mCoreAPI.PasswordExists()) {
            showPasswordSetAlert();
        }
        else if (!passwordLogin && checkPassword) {
            mPasswordCheck = new RememberPasswordCheck(this);
            mPasswordCheck.showPasswordCheckAlert();
        }
        else {
            new UserReviewTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        updateDrawer(true);
        resetDrawerButtons();
    }

    public class UserReviewTask extends AsyncTask<Void, Void, Boolean> {

        UserReviewTask() { }

        @Override
        protected Boolean doInBackground(Void... params) {
            return UserReview.offerUserReview();
        }

        @Override
        protected void onPostExecute(final Boolean offerReview) {
            if(offerReview) {
                UserReview.ShowUserReviewDialog(NavigationActivity.this);
            }
        }
    }

    public void startRecoveryQuestions(String questions, String username) {
        hideNavBar();
        Bundle bundle = new Bundle();
        bundle.putInt(PasswordRecoveryFragment.MODE, PasswordRecoveryFragment.FORGOT_PASSWORD);
        bundle.putString(PasswordRecoveryFragment.QUESTIONS, questions);
        bundle.putString(PasswordRecoveryFragment.USERNAME, username);
        Fragment frag = new PasswordRecoveryFragment();
        frag.setArguments(bundle);
        pushFragmentNoAnimation(frag, mNavThreadId);
        DisplayLoginOverlay(false, true);
    }

    public void startSignUp(String userName) {
        hideSoftKeyboard(mFragmentLayout);
        hideNavBar();
        Bundle bundle = new Bundle();
        bundle.putString(SetupWriteItDownFragment.USERNAME, userName);
        Fragment frag = new SetupUsernameFragment();
        frag.setArguments(bundle);
        pushFragmentNoAnimation(frag, mNavThreadId);
        DisplayLoginOverlay(false, true);
    }

    public void noSignup() {
        popFragment();
        showNavBar();
        DisplayLoginOverlay(true);
    }

    public void finishSignup() {
        showNavBar();
        switchFragmentThread(AirbitzApplication.getLastNavTab());
    }

    public void Logout() {
        if(mNavStacks[mNavThreadId].size()>1) { // ensure onPause called
            Fragment fragment = mNavStacks[mNavThreadId].peek();
            getFragmentManager().executePendingTransactions();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.remove(fragment);
            transaction.commitAllowingStateLoss();
        }
        // mHandler.postDelayed(mAttemptLogout, 100);
        updateDrawer(false);

        mLogoutTask = new LogoutTask();
        mLogoutTask.execute();
    }

    private LogoutTask mLogoutTask;
    public class LogoutTask extends AsyncTask<Void, Void, Boolean> {

        LogoutTask() { }

        @Override
        protected void onPreExecute() {
            NavigationActivity.this.ShowFadingDialog(
                    String.format(
                        getString(R.string.logout_message),
                        getString(R.string.app_name)),
                    getResources().getInteger(R.integer.alert_hold_time_forever), false);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            AirbitzApplication.Logout();
            mCoreAPI.logout();
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (isCancelled()) {
                return;
            }
            NavigationActivity.this.DismissFadingDialog();
            DisplayLoginOverlay(true);
            resetApp();
            switchFragmentThread(Tabs.BD.ordinal());
            mLogoutTask = null;
        }
    }

    Runnable mAttemptLogout = new Runnable() {
        @Override
        public void run() {
            if(mAsyncTasks.isEmpty()) {
                startActivity(new Intent(NavigationActivity.this, NavigationActivity.class));
            }
            else {
                mHandler.postDelayed(this, 100);
            }
        }
    };

    private void resetApp() {
        Fragment frag = mNavStacks[Tabs.BUYSELL.ordinal()].peek();
        if (frag instanceof PluginFragment) {
            PluginFragment plugin = (PluginFragment) frag;
            plugin.cleanup();
        }
        for(int i=0; i<mNavFragments.length; i++) {
            resetFragmentThreadToBaseFragment(i);
        }
    }

    private Fragment getNewBaseFragement(int id) {
        switch (id) {
            case 0:
                return new BusinessDirectoryFragment();
            case 1:
                return new RequestFragment();
            case 2:
                return new SendFragment();
            case 3:
                return new TransactionListFragment();
            case 4:
                return new WalletsFragment();
            case 5:
                return new SettingFragment();
            case 6:
                return new ImportFragment();
            case 7:
                return new BuySellFragment();
            case 8:
                return new GiftCardFragment();
            default:
                return null;
        }
    }

    public boolean networkIsAvailable() {
        return mCoreAPI.hasConnectivity();
    }

    private boolean loginExpired() {
        if (AirbitzApplication.getmBackgroundedTime() == 0 || !AirbitzApplication.isLoggedIn())
            return true;

        long milliDelta = (System.currentTimeMillis() - AirbitzApplication.getmBackgroundedTime());

        CoreAPI.debugLevel(1, "delta logout time = " + milliDelta);
        AccountSettings settings = mCoreAPI.coreSettings();
        if (settings != null) {
            if (milliDelta > settings.getMinutesAutoLogout() * 60 * 1000) {
                Logout();
                return true;
            }
        }
        return false;
    }

    public enum Tabs {BD, REQUEST, SEND, WALLET, WALLETS, MORE, IMPORT, BUYSELL, SHOP}

    //************************ Connectivity support

    // For Fragments to implement if they need to customize on back presses
    public interface OnBackPress {
        public boolean onBackPress();
    }

    public void LoginNow(String username, String password, boolean newDevice) {
        AirbitzApplication.Login(username, password);
        UserJustLoggedIn(password != null, newDevice);
        mDrawerAccount.setText(username);
    }

    Runnable mProgressDialogKiller = new Runnable() {
        @Override
        public void run() {
            findViewById(R.id.modal_indefinite_progress).setVisibility(View.INVISIBLE);
            ShowOkMessageDialog(getResources().getString(R.string.string_connection_problem_title), getResources().getString(R.string.string_no_connection_response));
        }
    };

    Dialog mMessageDialog;
    Runnable mMessageDialogKiller = new Runnable() {
        @Override
        public void run() {
            if (mMessageDialog.isShowing()) {
                mMessageDialog.dismiss();
            }
        }
    };

    public void showModalProgress(final boolean show) {
        View v = findViewById(R.id.modal_indefinite_progress);
        if (show) {
            v.setVisibility(View.VISIBLE);
            v.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return true; // intercept all touches
                }
            });
            if (mHandler == null)
                mHandler = new Handler();
            mHandler.postDelayed(mProgressDialogKiller, DIALOG_TIMEOUT_MILLIS);
        } else {
            mHandler.removeCallbacks(mProgressDialogKiller);
            v.setVisibility(View.INVISIBLE);
        }
    }

    public void ShowOkMessageDialog(String title, String message) {
        if (!this.isFinishing()) {
            if (mMessageDialog != null) {
                mMessageDialog.dismiss();
            }
            AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
            builder.setMessage(message)
                    .setTitle(title)
                    .setCancelable(false)
                    .setNeutralButton(getResources().getString(R.string.string_ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            }
                    );
            mMessageDialog = builder.create();
            mMessageDialog.show();
        }
    }

    public void ShowOkMessageDialog(String title, String message, int timeoutMillis) {
        mHandler.postDelayed(mMessageDialogKiller, timeoutMillis);
        ShowOkMessageDialog(title, message);
    }

    private Dialog mExitDialog;
    public void ShowExitMessageDialog(String title, String message) {
        if (!this.isFinishing() && mExitDialog == null) {
            AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
            builder.setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.string_yes),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                    finish();
                                }
                            })
                    .setNegativeButton(getResources().getString(R.string.string_no),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    mExitDialog = null;
                                    dialog.cancel();
                                }
                            });
            mExitDialog = builder.create();

            if (title != null) {
                mExitDialog.setTitle(title);
            }
            mExitDialog.show();
        }
    }

    public void ShowMessageDialogBackPress(String title, String reason) {
        if (!this.isFinishing()) {
            AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
            builder.setMessage(reason)
                    .setCancelable(false)
                    .setNeutralButton(getResources().getString(R.string.string_ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    NavigationActivity.this.onBackPressed();
                                }
                            }
                    );
            Dialog alert = builder.create();
            if (title != null) {
                alert.setTitle(title);
            }
            alert.show();
        }
    }

    //**************** Fading Dialog

    public interface OnFadingDialogFinished { public void onFadingDialogFinished();
    }

    private OnFadingDialogFinished mOnFadingDialogFinished;
    public void setFadingDialogListener(OnFadingDialogFinished listener) {
        mOnFadingDialogFinished = listener;
    }

    private void updateFadingDialogFinished() {
        if (mOnFadingDialogFinished != null)
            mOnFadingDialogFinished.onFadingDialogFinished();
    }

    public void DismissFadingDialog() {
        ShowFadingDialog("", 0);
    }

    public void ShowFadingDialog(String message) {
        ShowFadingDialog(message, getResources().getInteger(R.integer.alert_hold_time_default));
    }

    public void ShowFadingDialog(String message, int timeout) {
        ShowFadingDialog(message, timeout, true);
    }

    public void ShowFadingDialog(String message, int timeout, boolean cancelable) {
        ShowFadingDialog(message, null, timeout, cancelable);
    }

    public void ShowOrUpdateDialog(String message, int timeout, boolean cancelable) {
        if (mFadingDialog == null || !mFadingDialog.isShowing()) {
            ShowFadingDialog(message, null, timeout, cancelable);
        } else {
            mFadingDialog.setMessage(message);
        }
    }

    private MaterialDialog mFadingDialog = null;
    public void ShowFadingDialog(final String message, final String thumbnail, final int timeout, final boolean cancelable) {
        ShowFadingDialog(null, message, thumbnail, timeout, cancelable);
    }

    public void ShowFadingDialog(final String title, final String message, final String thumbnail, final int timeout, final boolean cancelable) {
        if (!this.isFinishing()) {
            NavigationActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (timeout == 0) {
                        if (mFadingDialog != null) {
                            mFadingDialog.dismiss();
                        }
                        return;
                    }
                    if (mFadingDialog != null) {
                        mFadingDialog.dismiss();
                    }
                    MaterialDialog.Builder builder =
                        new MaterialDialog.Builder(NavigationActivity.this)
                                .content(message)
                                .contentColorRes(android.R.color.white)
                                .theme(Theme.LIGHT)
                                .backgroundColorRes(R.color.colorPrimary)
                                .widgetColorRes(android.R.color.white);
                    if (!cancelable) {
                        builder.progress(true, 0);
                    }
                    mFadingDialog = builder.build();
                    mFadingDialog.setCancelable(cancelable);
                    mFadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                    TextView tv = mFadingDialog.getContentView();
                    tv.setTypeface(NavigationActivity.latoRegularTypeFace);

                    mFadingDialog.show();

                    View view = mFadingDialog.getView();
                    if (cancelable) {
                        View.OnClickListener dismiss = new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (mFadingDialog != null) {
                                    mFadingDialog.dismiss();
                                }
                            }
                        };
                        view.setOnClickListener(dismiss);
                        tv.setOnClickListener(dismiss);
                    }
                    AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
                    fadeOut.setStartOffset(timeout);
                    fadeOut.setDuration(getResources().getInteger(R.integer.alert_fadeout_time_default));
                    fadeOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            mFadingDialog.dismiss();
                            updateFadingDialogFinished();
                        }

                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });

                    view.setAnimation(fadeOut);
                    view.startAnimation(fadeOut);
                }
            });
        }
    }

    public void showPrivateKeySweepTransaction(String txid, String uuid, long amount) {
        if (amount > 0 && !txid.isEmpty()) {
            onSentFunds(uuid, txid, "");
            ShowOkMessageDialog(getString(R.string.import_wallet_swept_funds_title),
                    getString(R.string.import_wallet_swept_funds_message));
        }
        else if (amount == 0) {
            ShowOkMessageDialog(getString(R.string.import_wallet_hidden_bits_error_title),
                    getString(R.string.import_wallet_hidden_bits_error_message));
        }

    }

    public void showHiddenBitsTransaction(String txid, String uuid, long amount,
                String message, String zeroMessage, String tweet) {
        if(txid != null) {
            onSentFunds(uuid, txid, "");
        }

        if (amount == 0 && !zeroMessage.isEmpty()) {
            ShowHiddenBitsTweet(getString(R.string.import_wallet_hidden_bits_claimed), zeroMessage, tweet);
        }
        else if (!message.isEmpty()) {
            ShowHiddenBitsTweet(getString(R.string.import_wallet_hidden_bits_not_claimed), message, tweet);
        }
    }

    public void ShowHiddenBitsTweet(String title, String reason, final String tweet) {
        if (!this.isFinishing()) {
            if (mMessageDialog != null) {
                mMessageDialog.dismiss();
            }
            AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
            builder.setMessage(reason)
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.string_ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // invoke Twitter to send tweet
                                    Intent i = new Intent(Intent.ACTION_VIEW);
                                    i.setData(Uri.parse("http://twitter.com/post?message=" + Uri.encode(tweet)));
                                    startActivity(i);
                                    dialog.dismiss();
                                }
                            })
                    .setNegativeButton(getResources().getString(R.string.string_no),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });
            mMessageDialog = builder.create();
            if (title != null) {
                mMessageDialog.setTitle(title);
            }
            mMessageDialog.show();
        }
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
        builder.setMessage(reason)
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.string_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // invoke Twitter to send tweet
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse("http://twitter.com/post?message=" + Uri.encode(tweet)));
                                startActivity(i);
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.string_no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
        mMessageDialog = builder.create();
        if (title != null) {
            mMessageDialog.setTitle(title);
        }
        mMessageDialog.show();
    }

    public void hideSoftKeyboard(View v) {
        if (null != v) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    public void showSoftKeyboard(View v) {
        if (null != v) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void checkFirstWalletSetup() {
        List<String> wallets = mCoreAPI.loadWalletUUIDs();
        if (wallets.size() <= 0) {
            mWalletSetup = new SetupFirstWalletTask();
            mWalletSetup.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
        }
    }

    /**
     * Represents an asynchronous creation of the first wallet
     */
    private SetupFirstWalletTask mWalletSetup;
    public class SetupFirstWalletTask extends AsyncTask<Void, Void, Boolean> {

        SetupFirstWalletTask() { }

        @Override
        protected void onPreExecute() {
            NavigationActivity.this.ShowFadingDialog(
                    getString(R.string.fragment_signup_creating_wallet),
                    getResources().getInteger(R.integer.alert_hold_time_forever), false);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Set default currency
            mCoreAPI.SetupDefaultCurrency();

            // Create the Wallet
            String walletName =
                getResources().getString(R.string.activity_recovery_first_wallet_name);
            AccountSettings settings = mCoreAPI.coreSettings();
            if (settings != null) {
                return mCoreAPI.createWallet(
                        AirbitzApplication.getUsername(),
                        AirbitzApplication.getPassword(),
                        walletName, settings.getCurrencyNum());
            } else {
                return mCoreAPI.createWallet(
                        AirbitzApplication.getUsername(),
                        AirbitzApplication.getPassword(),
                        walletName, mCoreAPI.defaultCurrencyNum());
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mWalletSetup = null;
            if (!success) {
                NavigationActivity.this.ShowFadingDialog(
                    getResources().getString(R.string.activity_signup_create_wallet_fail));
            } else {
                // Update UI
                updateWalletListener();
                // Add categories
                createDefaultCategories();
                // Dismiss dialog
                NavigationActivity.this.DismissFadingDialog();
            }
            mCoreAPI.setupAccountSettings();
            mCoreAPI.startAllAsyncUpdates();
        }

        @Override
        protected void onCancelled() {
            mWalletSetup = null;
            NavigationActivity.this.DismissFadingDialog();
        }
    }

    private void createDefaultCategories() {
        String[] defaults =
            getResources().getStringArray(R.array.category_defaults);

        for (String cat : defaults)
            mCoreAPI.addCategory(cat);

        List<String> cats = mCoreAPI.loadCategories();
        if (cats.size() == 0 || cats.get(0).equals(defaults)) {
            CoreAPI.debugLevel(1, "Category creation failed");
        }
    }

    //************** Notification support

    private void checkNotifications() {
        if(mNotificationTask == null) {
            mNotificationTask = new NotificationTask();
            mNotificationTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private NotificationTask mNotificationTask;
    public class NotificationTask extends AsyncTask<Void, Void, String> {
        String mMessageId;
        String mBuildNumber;

        NotificationTask() { }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(Void... params) {
            DirectoryApi api = DirectoryWrapper.getApi();
            PackageInfo pInfo;
            try {
                pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                mMessageId = String.valueOf(getMessageIDPref());
                mBuildNumber = String.valueOf(pInfo.versionCode);
                return api.getMessages(mMessageId, mBuildNumber);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return null;
            }

        }

        @Override
        protected void onPostExecute(final String response) {
            CoreAPI.debugLevel(1, "Notification response of "+mMessageId+","+mBuildNumber+": " + response);
            if(response != null && response.length() != 0) {
                mNotificationMap = getAndroidMessages(response);
                if(mNotificationMap.size() > 0) {
                    showNotificationAlert();
                }
            }
            else {
                CoreAPI.debugLevel(1, "No Notification response");
            }
            mNotificationTask = null;
        }

        @Override
        protected void onCancelled() {
            mNotificationTask = null;
        }
    }

    public int getMessageIDPref() {
        SharedPreferences prefs = getSharedPreferences(AirbitzApplication.PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(LAST_MESSAGE_ID, 0); // default to Automatic
    }

    private void saveMessageIDPref(int id) {
        SharedPreferences.Editor editor = getSharedPreferences(AirbitzApplication.PREFS, Context.MODE_PRIVATE).edit();
        editor.putInt(LAST_MESSAGE_ID, id);
        editor.apply();
    }

    Map<Integer, AirbitzNotification> getAndroidMessages(String input) {
        Map<Integer, AirbitzNotification> map = new HashMap<Integer, AirbitzNotification>();
        try {
            JSONObject json = new JSONObject(input);
            int count = json.getInt("count");
            if(count > 0) {
                JSONArray notifications = json.getJSONArray("results");
                for(int i=0; i<count; i++) {
                    JSONObject notification = notifications.getJSONObject(i);

                    int id = Integer.valueOf(notification.getString("id"));
                    String title = notification.getString("title");
                    String message = notification.getString("message");

                    map.put(id, new AirbitzNotification(title, message));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return map;
    }

    private Dialog mAlertNotificationDialog;
    private void showNotificationAlert() {
        if (!this.isFinishing()) {
            if (mNotificationMap == null || mNotificationMap.size() == 0)
                return;

            if (mAlertNotificationDialog != null)
                mAlertNotificationDialog.dismiss();

            StringBuilder s = new StringBuilder();
            int max = -1;
            for (Integer i : mNotificationMap.keySet()) {
                if (max < i) {
                    max = i;
                }
                String title = mNotificationMap.get(i).mTitle;
                String message = mNotificationMap.get(i).mMessage;

                s.append("<div style=\"font-weight: bold; text-align: center\">").append(title).append("</div><br />");
                s.append("<div>").append(message).append("</div><br /><br />");
            }

            final int saveInt = max;
            saveMessageIDPref(saveInt);

            mAlertNotificationDialog = new Dialog(this);
            mAlertNotificationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mAlertNotificationDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            mAlertNotificationDialog.setContentView(R.layout.dialog_notification);

            WebView wv = (WebView) mAlertNotificationDialog.findViewById(R.id.dialog_notification_webview);
            wv.setVisibility(View.VISIBLE);
            wv.loadData(s.toString(), "text/html; charset=UTF-8", null);

            Button ok = (Button) mAlertNotificationDialog.findViewById(R.id.dialog_notification_ok_button);
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mAlertNotificationDialog.cancel();
                }
            });
            mAlertNotificationDialog.show();
        }
    }

    private Dialog mPasswordSetDialog;
    private void showPasswordSetAlert() {
            if (!NavigationActivity.this.isFinishing() && mPasswordSetDialog == null) {
                AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(NavigationActivity.this);
                builder.setMessage(getString(R.string.password_set_message))
                        .setTitle(getString(R.string.password_set_title))
                        .setCancelable(false)
                        .setPositiveButton(getResources().getString(R.string.password_set_skip),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.dismiss();
                                        mPasswordSetDialog = null;
                                    }
                                })
                        .setNegativeButton(getResources().getString(R.string.string_ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.dismiss();
                                        mPasswordSetDialog = null;
                                        launchChangePassword();
                                    }
                                });
                mPasswordSetDialog = builder.create();
                mPasswordSetDialog.show();
        }
    }

    private void launchChangePassword() {
        Bundle bundle = new Bundle();

        Fragment frag = new SettingFragment();
        bundle.putBoolean(SettingFragment.START_CHANGE_PASSWORD, true);
        frag.setArguments(bundle);
        mNavStacks[Tabs.MORE.ordinal()].clear();
        mNavStacks[Tabs.MORE.ordinal()].add(frag);
        switchFragmentThread(Tabs.MORE.ordinal());
    }

    private void checkDailyLimitPref() {
        SharedPreferences prefs = AirbitzApplication.getContext().getSharedPreferences(AirbitzApplication.PREFS, Context.MODE_PRIVATE);

        // On first install/load, copy synchronized to local setting
        if(!prefs.contains(CoreAPI.DAILY_LIMIT_SETTING_PREF + AirbitzApplication.getUsername())) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(CoreAPI.DAILY_LIMIT_PREF + AirbitzApplication.getUsername(), mCoreAPI.GetDailySpendLimit());
            editor.putBoolean(CoreAPI.DAILY_LIMIT_SETTING_PREF + AirbitzApplication.getUsername(), mCoreAPI.GetDailySpendLimitSetting());
            editor.apply();
        }
    }

    Dialog mOTPAlertDialog;
    final Runnable mShowOTPRequired = new Runnable() {
        @Override
        public void run() {
            if (!NavigationActivity.this.isFinishing() && mOTPAlertDialog == null) {
                    AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(NavigationActivity.this);
                    builder.setMessage(getString(R.string.twofactor_required_message))
                            .setTitle(getString(R.string.twofactor_required_title))
                            .setCancelable(false)
                            .setPositiveButton(getResources().getString(R.string.twofactor_enable),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.dismiss();
                                            launchTwoFactorScan();
                                        }
                                    })
                            .setNegativeButton(getResources().getString(R.string.twofactor_remind_later),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                    mOTPAlertDialog = builder.create();
                    mOTPAlertDialog.show();
            }
        }
    };

    final Runnable mShowOTPSkew = new Runnable() {
        @Override
        public void run() {
            if (!NavigationActivity.this.isFinishing() && mOTPAlertDialog == null) {
                    AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(NavigationActivity.this);
                    builder.setMessage(getString(R.string.twofactor_invalid_message))
                            .setTitle(getString(R.string.twofactor_invalid_title))
                            .setCancelable(false)
                            .setPositiveButton(getResources().getString(R.string.string_ok),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                            mOTPAlertDialog = null;
                                        }
                                    });
                    mOTPAlertDialog = builder.create();
                    mOTPAlertDialog.show();
            }
        }
    };

    private void launchTwoFactorScan() {
        Fragment fragment = new TwoFactorScanFragment();
        ((TwoFactorScanFragment)fragment).setOnTwoFactorQRScanResult(this);
        Bundle bundle = new Bundle();
        bundle.putBoolean(TwoFactorScanFragment.STORE_SECRET, true);
        bundle.putBoolean(TwoFactorScanFragment.TEST_SECRET, true);
        bundle.putString(TwoFactorScanFragment.USERNAME, AirbitzApplication.getUsername());
        fragment.setArguments(bundle);
        pushFragment(fragment);
    }

    @Override
    public void onTwoFactorQRScanResult(boolean success, String result) {
        mOTPAlertDialog = null;
    }

    Dialog mOTPResetRequestDialog;
    public void onOTPResetRequest() {
        if (!NavigationActivity.this.isFinishing() && mOTPResetRequestDialog == null) {
            String message = String.format(getString(R.string.twofactor_reset_message), AirbitzApplication.getUsername());
            AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(NavigationActivity.this);
            builder.setMessage(message)
                    .setTitle(getString(R.string.twofactor_reset_title))
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.string_ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });
            mOTPResetRequestDialog = builder.create();
            mOTPResetRequestDialog.show();
        }
    }

    boolean mDrawerExchangeUpdated = false;
    // Navigation Drawer (right slideout)
    private void setupDrawer() {
        mDrawer = (DrawerLayout) findViewById(R.id.activityDrawer);

        mDrawerView = (FrameLayout) findViewById(R.id.activityDrawerView);
        Common.addStatusBarPadding(this, mDrawerView);

        mDrawerLayoutAccount = findViewById(R.id.layout_account);
        mDrawerLogin = findViewById(R.id.item_drawer_login);
        mDrawerLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawer.closeDrawer(mDrawerView);
                DisplayLoginOverlay(true);
            }
        });
        mDrawerDirectory = (Button) findViewById(R.id.item_drawer_directory);
        mDrawerDirectory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavBarSelected(Tabs.BD.ordinal());
                mDrawer.closeDrawer(mDrawerView);
            }
        });

        mDrawerRequest = (Button) findViewById(R.id.item_drawer_request);
        mDrawerRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavBarSelected(Tabs.REQUEST.ordinal());
                mDrawer.closeDrawer(mDrawerView);
            }
        });

        mDrawerSend = (Button) findViewById(R.id.item_drawer_send);
        mDrawerSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavBarSelected(Tabs.SEND.ordinal());
                mDrawer.closeDrawer(mDrawerView);
            }
        });

        mDrawerTxs = (Button) findViewById(R.id.item_drawer_txs);
        mDrawerTxs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavBarSelected(Tabs.WALLET.ordinal());
                mDrawer.closeDrawer(mDrawerView);
            }
        });

        mDrawerWallets = (Button) findViewById(R.id.item_drawer_wallets);
        mDrawerWallets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavBarSelected(Tabs.WALLETS.ordinal());
                mDrawer.closeDrawer(mDrawerView);
            }
        });

        mDrawerExchange = (TextView) findViewById(R.id.item_drawer_exchange_rate);
        mDrawerBuySellLayout = (RelativeLayout) findViewById(R.id.layout_drawer_bottom_buttons);
        mDrawerBuySell = (Button) findViewById(R.id.item_drawer_buy_sell);
        mDrawerBuySell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavBarSelected(Tabs.BUYSELL.ordinal());
                mDrawer.closeDrawer(mDrawerView);
            }
        });
        if (!getResources().getBoolean(R.bool.include_buysell)) {
            mDrawerBuySell.setVisibility(View.GONE);
        } else {
            mDrawerBuySell.setVisibility(View.VISIBLE);
        }

        mDrawerShop = (Button) findViewById(R.id.item_drawer_shop);
        mDrawerShop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavBarSelected(Tabs.SHOP.ordinal());
                mDrawer.closeDrawer(mDrawerView);
            }
        });
        if (!getResources().getBoolean(R.bool.include_shop)) {
            mDrawerShop.setVisibility(View.GONE);
        } else {
            mDrawerShop.setVisibility(View.VISIBLE);
        }

        mDrawerImport = (Button) findViewById(R.id.item_drawer_import);
        mDrawerImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFragmentThreadToBaseFragment(Tabs.IMPORT.ordinal());
                onNavBarSelected(Tabs.IMPORT.ordinal());
                mDrawer.closeDrawer(mDrawerView);
            }
        });

        mDrawerLogout = (Button) findViewById(R.id.item_drawer_logout);
        mDrawerLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawer.closeDrawer(mDrawerView);
                Logout();
            }
        });

        mDrawerSettings = (Button) findViewById(R.id.item_drawer_settings);
        mDrawerSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int tmp = mNavThreadId;
                resetFragmentThreadToBaseFragment(Tabs.MORE.ordinal());
                onNavBarSelected(Tabs.MORE.ordinal());
                if (Tabs.MORE.ordinal() == tmp) {
                    switchFragmentThread(Tabs.MORE.ordinal());
                }
                mDrawer.closeDrawer(mDrawerView);
            }
        });

        mDrawerAccount = (TextView) findViewById(R.id.item_drawer_account);
        mDrawerAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!otherAccounts(AirbitzApplication.getUsername()).isEmpty()) {
                    if (mOtherAccountsListView.getVisibility() != View.VISIBLE) {
                        showOthersList(AirbitzApplication.getUsername(), true);
                    } else {
                        showOthersList(AirbitzApplication.getUsername(), false);
                    }
                }
            }
        });
        mDrawerAccountArrow = (ImageView) findViewById(R.id.item_drawer_account_arrow);
        mDrawerAccountArrow.setVisibility(View.INVISIBLE);

        mOtherAccounts = new ArrayList<String>();
        mOtherAccountsAdapter = new AccountsAdapter(this, mOtherAccounts, true);
        mOtherAccountsAdapter.setButtonTouchedListener(this); // for close account button
        mOtherAccountsListView = (ListView) findViewById(R.id.drawer_account_list);
        mOtherAccountsListView.setAdapter(mOtherAccountsAdapter);
        mOtherAccountsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String username = mOtherAccounts.get(position);
                saveCachedLoginName(username);
                mDrawer.closeDrawer(mDrawerView);
                Logout();
            }
        });

        mDrawer.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                mDrawerExchangeUpdated = false;
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                if(!mDrawerExchangeUpdated) {
                    AccountSettings settings = mCoreAPI.coreSettings();
                    if (settings != null) {
                        mDrawerExchange.setText(mCoreAPI.BTCtoFiatConversion(settings.getCurrencyNum()));
                        mDrawerExchangeUpdated = true;
                    } else {
                        mDrawerExchange.setText(mCoreAPI.BTCtoFiatConversion(mCoreAPI.defaultCurrencyNum()));
                        mDrawerExchangeUpdated = true;
                    }
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {}

            @Override
            public void onDrawerStateChanged(int newState) {
                mActionMenu.close(true);
            }
        });
    }

    private void resetDrawerButtons() {
        Fragment frag = mNavStacks[mNavThreadId].peek();
        if (mNavThreadId == Tabs.BD.ordinal()) {
            resetDrawerButtons(mDrawerDirectory);
        } else if (mNavThreadId == Tabs.WALLETS.ordinal()) {
                resetDrawerButtons(mDrawerWallets);
        } else if (mNavThreadId == Tabs.WALLET.ordinal()) {
                resetDrawerButtons(mDrawerTxs);
        } else if (mNavThreadId == Tabs.SEND.ordinal()) {
            resetDrawerButtons(mDrawerSend);
        } else if (mNavThreadId == Tabs.REQUEST.ordinal()) {
            resetDrawerButtons(mDrawerRequest);
        } else if (mNavThreadId == Tabs.MORE.ordinal()) {
            resetDrawerButtons(mDrawerSettings);
        } else if (mNavThreadId == Tabs.IMPORT.ordinal()) {
            resetDrawerButtons(mDrawerImport);
        } else if (mNavThreadId == Tabs.BUYSELL.ordinal()) {
            resetDrawerButtons(mDrawerBuySell);
        } else if (mNavThreadId == Tabs.SHOP.ordinal()) {
            resetDrawerButtons(mDrawerShop);
        }
    }

    private void resetDrawerButtons(Button button) {
        mDrawerDirectory.setSelected(false);
        mDrawerRequest.setSelected(false);
        mDrawerSend.setSelected(false);
        mDrawerTxs.setSelected(false);
        mDrawerWallets.setSelected(false);
        mDrawerBuySell.setSelected(false);
        mDrawerShop.setSelected(false);
        mDrawerImport.setSelected(false);
        mDrawerSettings.setSelected(false);
        mDrawerLogout.setSelected(false);
        if (button != null) {
            button.setSelected(true);
        }
        mActionMenu.close(true);
    }

    private void updateDrawer(boolean loggedIn) {
        if (loggedIn) {
            mDrawerAccount.setText(AirbitzApplication.getUsername());
            mDrawerLogin.setVisibility(View.GONE);
            mDrawerExchange.setVisibility(View.VISIBLE);
            mDrawerLayoutAccount.setVisibility(View.VISIBLE);
            mDrawerLogout.setVisibility(View.VISIBLE);
            List<String> users = otherAccounts(AirbitzApplication.getUsername());
            if (users.size() > 0) {
                mDrawerAccountArrow.setVisibility(View.VISIBLE);
            } else {
                mDrawerAccountArrow.setVisibility(View.INVISIBLE);
            }
        } else {
            mDrawerLogin.setVisibility(View.VISIBLE);
            mDrawerExchange.setVisibility(View.INVISIBLE);
            mDrawerLayoutAccount.setVisibility(View.GONE);
            mDrawerLogout.setVisibility(View.GONE);
        }
        showOthersList(AirbitzApplication.getUsername(), false);
    }

    public void lockDrawer() {
        mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    public void unlockDrawer() {
        mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    public void openDrawer() {
        mDrawerAccount.setText(AirbitzApplication.getUsername());
        showOthersList(AirbitzApplication.getUsername(), false);
        mDrawer.openDrawer(mDrawerView);
    }

    public void closeDrawer() {
        mDrawer.closeDrawer(mDrawerView);
    }

    public boolean isDrawerOpen() {
        return mDrawer.isDrawerOpen(mDrawerView);
    }

    private void showOthersList(String username, boolean show)
    {
        mOtherAccounts.clear();
        mOtherAccounts.addAll(otherAccounts(username));
        mOtherAccountsAdapter.notifyDataSetChanged();
        ListViewUtility.setListViewHeightBasedOnChildren(mOtherAccountsListView);
        if(show && !mOtherAccounts.isEmpty()) {
            mOtherAccountsListView.setVisibility(View.VISIBLE);
            mDrawerBuySellLayout.setVisibility(View.GONE);
            mDrawerAccountArrow.animate()
                    .rotation(180)
                    .start();
        }
        else {
            mOtherAccountsListView.setVisibility(View.GONE);
            mDrawerBuySellLayout.setVisibility(View.VISIBLE);
            mDrawerAccountArrow.animate()
                    .rotation(0)
                    .start();
        }
    }

    private List<String> otherAccounts(String username) {
        List<String> accounts = mCoreAPI.listAccounts();
        List<String> others = new ArrayList<String>();
        for(int i=0; i< accounts.size(); i++) {
            if(!accounts.get(i).equals(username)) {
                others.add(accounts.get(i));
            }
        }
        return others;
    }

    private void saveCachedLoginName(String name) {
        SharedPreferences.Editor editor = getSharedPreferences(AirbitzApplication.PREFS, Context.MODE_PRIVATE).edit();
        editor.putString(AirbitzApplication.LOGIN_NAME, name);
        editor.apply();
    }

    @Override
    public void onButtonTouched(final String account) {
        String message = String.format(getString(R.string.fragment_landing_account_delete_message), account);
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
        builder.setMessage(message)
                .setTitle(getString(R.string.fragment_landing_account_delete_title))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.string_yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (!mCoreAPI.deleteAccount(account)) {
                                    ShowFadingDialog("Account could not be deleted.");
                                }
                                showOthersList("", false);
                                showOthersList(AirbitzApplication.getUsername(), true);
                                updateDrawer(true);
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.string_no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                showOthersList("", false);
                                dialog.dismiss();
                            }
                        });
        Dialog confirmDialog = builder.create();
        confirmDialog.show();
    }

    private WalletReceiver mWalletsLoadedReceiver;
    class WalletReceiver extends BroadcastReceiver {
        public boolean mDataLoaded = false;
        public boolean mShowMessages = false;

        private void showMessage(String message) {
            if (mShowMessages) {
                NavigationActivity.this.ShowOrUpdateDialog(message,
                    NavigationActivity.this.getResources().getInteger(R.integer.alert_hold_time_forever), false);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (CoreAPI.WALLET_LOADING_START_ACTION.equals(intent.getAction())) {
                if (mShowMessages) {
                    showMessage(context.getString(R.string.loading_wallets));
                }
            } else if (CoreAPI.WALLET_LOADING_STATUS_ACTION.equals(intent.getAction())) {
                int complete = intent.getIntExtra(CoreAPI.WALLETS_LOADED_TOTAL, -1);
                int total = intent.getIntExtra(CoreAPI.WALLETS_TOTAL, -1);
                if (total >= 0) {
                    showMessage(context.getString(R.string.loading_n_wallets, complete, total));
                } else {
                    showMessage(context.getString(R.string.loading_wallets));
                }
            } else if (CoreAPI.WALLETS_ALL_LOADED_ACTION.equals(intent.getAction())) {
                mDataLoaded = true;
                showMessage(context.getString(R.string.loading_transactions));
            } else if (CoreAPI.WALLETS_LOADING_BITCOIN_ACTION.equals(intent.getAction())) {
                if (mDataLoaded) {
                    showMessage(context.getString(R.string.loading_transactions));
                }
            } else if (CoreAPI.WALLETS_LOADED_BITCOIN_ACTION.equals(intent.getAction())) {
                NavigationActivity.this.DismissFadingDialog();
            }
        }
    };

    private BroadcastReceiver mBlockHeightReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            CoreAPI.debugLevel(1, "Block Height received");
            updateWalletListener();
        }
    };

    private BroadcastReceiver mDataSyncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            CoreAPI.debugLevel(1, "Data Sync received");
            updateWalletListener();
        }
    };

    private BroadcastReceiver mIncomingBitcoinReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String uuid = intent.getStringExtra(CoreAPI.WALLET_UUID);
            String txId = intent.getStringExtra(CoreAPI.WALLET_TXID);
            onIncomingBitcoin(uuid, txId);
        };
    };

    private BroadcastReceiver mExchangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AccountSettings settings = mCoreAPI.coreSettings();
            if (settings != null) {
                mDrawerExchange.setText(mCoreAPI.BTCtoFiatConversion(settings.getCurrencyNum()));
            } else {
                mDrawerExchange.setText(mCoreAPI.BTCtoFiatConversion(mCoreAPI.defaultCurrencyNum()));
            }

        }
    };

    private BroadcastReceiver mRemotePasswordChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            CoreAPI.debugLevel(1, "Remote Password received");
            if (!(mNavStacks[mNavThreadId].peek() instanceof SignUpFragment)) {
                showRemotePasswordChangeDialog();
            }
        }
    };

    private BroadcastReceiver mOtpErrorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String secret = intent.getStringExtra(CoreAPI.OTP_SECRET);
            if (secret != null) {
                mHandler.post(mShowOTPSkew);
            } else {
                mHandler.post(mShowOTPRequired);
            }
        }
    };

    private BroadcastReceiver mOtpResetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onOTPResetRequest();
        }
    };
}
