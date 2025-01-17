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

package com.airbitz.fragments.send;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.airbitz.AirbitzApplication;
import com.airbitz.R;
import com.airbitz.activities.NavigationActivity;
import com.airbitz.api.AirbitzException;
import com.airbitz.api.CoreAPI;
import com.airbitz.fragments.HelpFragment;
import com.airbitz.fragments.WalletBaseFragment;
import com.airbitz.fragments.settings.CurrencyFragment;
import com.airbitz.fragments.wallet.WalletsFragment;
import com.airbitz.models.Wallet;
import com.airbitz.objects.AudioPlayer;
import com.airbitz.objects.Calculator;
import com.airbitz.utils.Common;

import java.lang.reflect.Method;

/**
 * Created on 2/21/14.
 */
public class SendConfirmationFragment extends WalletBaseFragment implements
        CoreAPI.OnPasswordCheckListener,
        Calculator.OnCalculatorKey,
        CurrencyFragment.OnCurrencySelectedListener {
    private CoreAPI mCoreAPI;

    private final String TAG = getClass().getSimpleName();

    private final int INVALID_ENTRY_COUNT_MAX = 3;
    private final int INVALID_ENTRY_WAIT_MILLIS = 30000;
    private final int CALC_SEND_FEES_DELAY_MILLIS = 400;
    private static final String INVALID_ENTRY_PREF = "fragment_send_confirmation_invalid_entries";

    private TextView mToEdittext;
    private EditText mAuthorizationEdittext;
    String mDelayedMessage;

    private TextView mToTextView;
    private TextView mSlideTextView;
    private TextView mAuthorizationTextView;
    private View mAuthorizationLayout;
    private TextView mBTCSignTextview;
    private TextView mBTCDenominationTextView;
    private TextView mFiatDenominationTextView;
    private TextView mFiatSignTextView;
    private TextView mConversionTextView;
    private Button mMaxButton;
    private Button mChangeFiatButton;
    private int mCurrencyNum;
    private boolean mSendConfirmationOverrideCurrencyMode = false;
    private int mSendConfirmationCurrencyNumOverride;

    private Bundle bundle;

    private EditText mFiatField;
    private EditText mBitcoinField;
    private long mSavedBitcoin = -1;

    private ImageButton mConfirmSwipeButton;

    private float mSlideHalfWidth;
    private float moveX = 0;

    private Calculator mCalculator;

    private RelativeLayout mSlideLayout;

    private RelativeLayout mParentLayout;

    private int mRightThreshold;
    private int mLeftThreshold;

    private String mUUIDorURI;
    private String mLabel;
    private String mCategory;
    private String mNotes;
    private Boolean mLocked = false;
    private Boolean mSignOnly = false;
    private Boolean mIsUUID;
    private long mAmountMax;
    private long mAmountToSendSatoshi = -1;
    private double mAmountFiat = -1;
    private long mFees;
    private int mInvalidEntryCount = 0;
    private long mInvalidEntryStartMillis = 0;
    private boolean mFundsSent = false;

    private String _sendTo;
    private String _destUUID;

    private boolean mPasswordRequired = false;
    private boolean mPinRequired = false;
    private boolean mMaxLocked = false;
    private boolean mBtcMode = false;

    private Wallet mToWallet;

    private boolean mAutoUpdatingTextFields = false;

    private Typeface mBitcoinTypeface;

    private View mView;
    /**
     * Wrap the fee calculation in an AsyncTask
     */
    private MaxAmountTask mMaxAmountTask;
    private CalculateFeesTask mCalculateFeesTask;
    /**
     * Represents an asynchronous send or transfer
     */
    private SendOrTransferTask mSendOrTransferTask;
    private Handler mHandler = new Handler();
    static final int KEYBOARD_ANIM = 250;

    private CoreAPI.SpendTarget mSpendTarget = null;

    public interface OnExitHandler {
        public void error();
        public void success(String txId);
        public void back();
    }

    private OnExitHandler exitHandler;

    public SendConfirmationFragment() {}

    public void setSpendTarget(CoreAPI.SpendTarget target) {
        mSpendTarget = target;
    }

    @Override
    protected String getSubtitle() {
        return mActivity.getString(R.string.fragment_send_subtitle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bundle = this.getArguments();

        mCoreAPI = CoreAPI.getApi();

        mAutoUpdatingTextFields = true;
        mPositionNavBar = false;
        setHomeEnabled(true);
        if (null != mSpendTarget && mSpendTarget.isTransfer()) {
            setDropdownEnabled(false);
        }
        if (null != mSpendTarget && mSpendTarget.getSpendAmount() > 0) {
            mBtcMode = true;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_send_confirmation, container, false);

        mBitcoinTypeface = Typeface.createFromAsset(getActivity().getAssets(), "font/Lato-Regular.ttf");

        mConfirmSwipeButton = (ImageButton) mView.findViewById(R.id.button_confirm_swipe);

        mBitcoinField = (EditText) mView.findViewById(R.id.button_bitcoin_balance);
        mFiatField = (EditText) mView.findViewById(R.id.button_dollar_balance);

        mCalculator = (Calculator) mActivity.findViewById(R.id.navigation_calculator_layout);
        mCalculator.setCalculatorKeyListener(this);
        mCalculator.setEditText(mBitcoinField);

        mToTextView = (TextView) mView.findViewById(R.id.textview_to);
        mSlideTextView = (TextView) mView.findViewById(R.id.textview_slide);
        mConversionTextView = (TextView) mView.findViewById(R.id.textview_conversion);
        mBTCSignTextview = (TextView) mView.findViewById(R.id.send_confirmation_btc_sign);
        mBTCDenominationTextView = (TextView) mView.findViewById(R.id.send_confirmation_btc_denomination);
        mFiatDenominationTextView = (TextView) mView.findViewById(R.id.send_confirmation_fiat_denomination);
        mFiatSignTextView = (TextView) mView.findViewById(R.id.send_confirmation_fiat_sign);
        mMaxButton = (Button) mView.findViewById(R.id.button_max);
        mChangeFiatButton = (Button) mView.findViewById(R.id.button_change_fiat);

        mToEdittext = (TextView) mView.findViewById(R.id.textview_to_name);
        mAuthorizationEdittext = (EditText) mView.findViewById(R.id.edittext_pin);

        mSlideLayout = (RelativeLayout) mView.findViewById(R.id.layout_slide);
        mSlideLayout.setVisibility(View.INVISIBLE);

        mToEdittext.setTypeface(NavigationActivity.latoBlackTypeFace, Typeface.NORMAL);

        mToTextView.setTypeface(NavigationActivity.latoBlackTypeFace);
        mConversionTextView.setTypeface(NavigationActivity.latoRegularTypeFace);
        mSlideTextView.setTypeface(NavigationActivity.latoBlackTypeFace, Typeface.NORMAL);

        mAuthorizationTextView = (TextView) mView.findViewById(R.id.textview_pin);
        mAuthorizationTextView.setTypeface(NavigationActivity.latoBlackTypeFace, Typeface.NORMAL);

        mAuthorizationLayout = mView.findViewById(R.id.fragment_send_confirmation_layout_authorization);

        mParentLayout = (RelativeLayout) mView.findViewById(R.id.layout_root);

        final TextWatcher mPINTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (mPinRequired && editable.length() >= 4) {
                    mActivity.hideSoftKeyboard(mAuthorizationEdittext);
                    mConfirmSwipeButton.requestFocus();
                }
            }
        };
        mAuthorizationEdittext.addTextChangedListener(mPINTextWatcher);

        mAuthorizationEdittext.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                CoreAPI.debugLevel(1, "PIN field focus changed");
                if (hasFocus) {
                    mAutoUpdatingTextFields = true;
                    mActivity.showSoftKeyboard(mAuthorizationEdittext);
                    hideCalculator();
                } else {
                    mAutoUpdatingTextFields = false;
                }
            }
        });

        mBitcoinField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!mAutoUpdatingTextFields) {
                    mBtcMode = true;
                    updateTextFieldContents(mBtcMode);
                    mBitcoinField.setSelection(mBitcoinField.getText().toString().length());
                }
            }
        });

        mBitcoinField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CoreAPI.debugLevel(1, "Bitcoin field clicked");
                mCalculator.setEditText(mBitcoinField);
                showCalculator();
            }
        });

        mFiatField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!mAutoUpdatingTextFields) {
                    mBtcMode = false;
                    updateTextFieldContents(mBtcMode);
                    mFiatField.setSelection(mFiatField.getText().toString().length());
                }
            }
        });
        mFiatField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCalculator.setEditText(mFiatField);
                showCalculator();
            }
        });

        TextView.OnEditorActionListener tvListener = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                        keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                        mAuthorizationLayout.getVisibility()==View.VISIBLE) {
                    mAuthorizationEdittext.requestFocus();
                    return true;
                }
                else {
                    mView.requestFocus();
                    return true;
                }
            }
        };

        View.OnTouchListener setCursorAtEnd = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                CoreAPI.debugLevel(1, "Prevent OS keyboard");
                final EditText edittext = (EditText) v;
                edittext.onTouchEvent(event);
                edittext.post(new Runnable() {
                    @Override
                    public void run() {
                        edittext.setSelection(edittext.getText().length()); // set cursor at end
                    }
                });
                return true;
            }
        };

        // Prevent OS keyboard from showing
        try {
            final Method method = EditText.class.getMethod(
                    "setShowSoftInputOnFocus"
                    , new Class[] { boolean.class });
            method.setAccessible(true);
            method.invoke(mBitcoinField, false);
            method.invoke(mFiatField, false);
        } catch (Exception e) {
            // ignore
        }

        mBitcoinField.setOnTouchListener(setCursorAtEnd);
        mFiatField.setOnTouchListener(setCursorAtEnd);
        mBitcoinField.setOnEditorActionListener(tvListener);
        mFiatField.setOnEditorActionListener(tvListener);

        mConfirmSwipeButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mSlideHalfWidth = mConfirmSwipeButton.getWidth() / 2;
                        mLeftThreshold = (int) (mSlideLayout.getX());
                        mRightThreshold = (int) (mSlideLayout.getX() + mSlideLayout.getWidth() - mConfirmSwipeButton.getWidth() - mLeftThreshold);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        moveX = event.getRawX() - mLeftThreshold;
                        float leftSlide = moveX - mSlideHalfWidth;
                        CoreAPI.debugLevel(1, "Move data: leftThreshold, rightThreshold, leftSlide, slideWidth, = "
                                + mLeftThreshold + ", " + mRightThreshold + ", " + leftSlide + ", " + mConfirmSwipeButton.getWidth());
                        if (leftSlide < 0) {
                            mConfirmSwipeButton.setX(0);
                        } else if (leftSlide > mRightThreshold) {
                            mConfirmSwipeButton.setX(mRightThreshold);
                        } else {
                            mConfirmSwipeButton.setX(leftSlide);
                        }
                        return false;
                    case MotionEvent.ACTION_UP:
                        touchEventsEnded();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        resetSlider();
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        break;
                    default:
                        break;
                }

                return false;
            }
        });

        mMaxButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mWallet != null && !mMaxLocked) {
                    mMaxLocked = true;
                    if (mMaxAmountTask != null)
                        mMaxAmountTask.cancel(true);
                    mMaxAmountTask = new MaxAmountTask();
                    mMaxAmountTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        });

        mChangeFiatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String code = mCoreAPI.getCurrencyCode(mCurrencyNum);

                CurrencyFragment fragment = new CurrencyFragment();
                fragment.setSelected(code);
                fragment.setOnCurrencySelectedListener(SendConfirmationFragment.this);

                ((NavigationActivity) getActivity()).pushFragment(fragment, NavigationActivity.Tabs.SEND.ordinal());
            }
        });

        mConversionTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;

                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    if(mConversionTextView.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {
                        if (event.getRawX() >= (mConversionTextView.getRight() - mConversionTextView.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                            // your action here
                            mActivity.pushFragment(new HelpFragment(HelpFragment.SEND_CONFIRMATION_INSUFFICIENT_FUNDS), NavigationActivity.Tabs.SEND.ordinal());
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        return mView;
    }

    @Override
    public boolean onBackPress() {
        if (mCalculator.getVisibility() == View.VISIBLE) {
            hideCalculator();
            return true;
        }
        if (super.onBackPress()) {
            return true;
        } else {
            if (null != exitHandler) {
                exitHandler.back();
            }
            mActivity.popFragment();
            return true;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_standard, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPress();
                return true;
            case R.id.action_help:
                mActivity.pushFragment(
                    new HelpFragment(HelpFragment.SEND_CONFIRMATION),
                        NavigationActivity.Tabs.SEND.ordinal());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCurrencySelected(int num) {
        mSendConfirmationCurrencyNumOverride = num;
        mSendConfirmationOverrideCurrencyMode = true;
        checkFields();
    }

    public void setExitHandler(OnExitHandler handler) {
        this.exitHandler = handler;
    }

    @Override
    protected void onExchangeRatesChange() {
        super.onExchangeRatesChange();
        checkFields();
        checkAuthorization();
    }


    @Override
    public void onWalletsLoaded() {
        super.onWalletsLoaded();
        checkFields();
        checkAuthorization();
    }

    private void resetFiatAndBitcoinFields() {
        mAutoUpdatingTextFields = true;
        mAmountToSendSatoshi = 0;
        mAmountFiat = 0.0;
        mFiatField.setText("");
        mBitcoinField.setText("");
        mConversionTextView.setTextColor(getResources().getColor(R.color.dark_text));
        mConversionTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        mBitcoinField.setTextColor(getResources().getColor(R.color.dark_text));
        mFiatField.setTextColor(getResources().getColor(R.color.dark_text));
        mAutoUpdatingTextFields = false;
        checkAuthorization();
        calculateFees();
    }

    public void touchEventsEnded() {
        int successThreshold = (mSlideLayout.getWidth() / 8);
        if (mConfirmSwipeButton.getX() <= successThreshold) {
            attemptInitiateSend();
        } else {
            resetSlider();
        }
    }

    private void updateTextFieldContents(boolean btc) {
        double currency;
        long satoshi;

        mAutoUpdatingTextFields = true;
        if (mSendConfirmationOverrideCurrencyMode) {
            mCurrencyNum = mSendConfirmationCurrencyNumOverride;
        } else {
            mCurrencyNum = mWallet.getCurrencyNum();
        }

        mFiatSignTextView.setText(mCoreApi.getCurrencyDenomination(mCurrencyNum));
        mConversionTextView.setText(mCoreApi.BTCtoFiatConversion(mCurrencyNum));

        if (!mLocked) {
            if (btc) {
                mAmountToSendSatoshi = mCoreApi.denominationToSatoshi(mBitcoinField.getText().toString());
                mSpendTarget.setSpendAmount(mAmountToSendSatoshi);
                mFiatField.setText(mCoreApi.FormatCurrency(mAmountToSendSatoshi, mCurrencyNum, false, false));
            } else {
                satoshi = mCoreApi.parseFiatToSatoshi(mFiatField.getText().toString(), mCurrencyNum);
                mAmountToSendSatoshi = satoshi;
                mSpendTarget.setSpendAmount(satoshi);
                mBitcoinField.setText(mCoreApi.formatSatoshi(mAmountToSendSatoshi, false));
            }
        }
        mAutoUpdatingTextFields = false;
        checkAuthorization();
        calculateFees();
    }

    final Runnable delayCalcFees = new Runnable() {
        @Override
        public void run() {
            if (mCalculateFeesTask != null) {
                mCalculateFeesTask.cancel(true);
            }
            mCalculateFeesTask = new CalculateFeesTask();
            mCalculateFeesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    };

    @Override
    public void OnCalculatorKeyPressed(String tag) {
        if (tag.equals("done")) {
            hideCalculator();
        }
    }

    private void showCalculator() {
        mActivity.hideSoftKeyboard(getView());
        mCalculator.showCalculator();
    }

    private void hideCalculator() {
        mCalculator.hideCalculator();
    }

    private void calculateFees() {
        mHandler.removeCallbacks(delayCalcFees);
        mHandler.postDelayed(delayCalcFees, CALC_SEND_FEES_DELAY_MILLIS);
    }

    private void UpdateFeeFields(Long fees, AirbitzException error) {
        mAutoUpdatingTextFields = true;
        int color;
        if (mSendConfirmationOverrideCurrencyMode) {
            mCurrencyNum = mSendConfirmationCurrencyNumOverride;
        } else {
            mCurrencyNum = mWallet.getCurrencyNum();
        }

        mFiatSignTextView.setText(mCoreApi.getCurrencyDenomination(mCurrencyNum));

        if (error == null || 0 == mAmountToSendSatoshi) {
            if (mAmountMax > 0 && mAmountToSendSatoshi == mAmountMax) {
                color = getResources().getColor(R.color.max_orange);
                mMaxButton.setBackgroundResource(R.drawable.bg_button_orange);
            } else {
                color = getResources().getColor(R.color.dark_text);
                mMaxButton.setBackgroundResource(R.drawable.bg_button_green);
            }
            if ((fees + mAmountToSendSatoshi) <= mWallet.getBalanceSatoshi()) {
                mConversionTextView.setTextColor(color);
                mConversionTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                mConversionTextView.setBackgroundResource(android.R.color.transparent);
                mBitcoinField.setTextColor(color);
                mFiatField.setTextColor(color);

                String coinFeeString = "+ " + mCoreApi.formatSatoshi(fees, false);
                mBTCDenominationTextView.setText(coinFeeString + " " + mCoreApi.getDefaultBTCDenomination());

                double fiatFee = mCoreApi.SatoshiToCurrency(fees, mCurrencyNum);
                String fiatFeeString = "+ " + mCoreApi.formatCurrency(fiatFee, mCurrencyNum, false);
                mFiatDenominationTextView.setText(fiatFeeString + " " + mCoreApi.getCurrencyAcronym(mCurrencyNum));
                mConversionTextView.setText(mCoreApi.BTCtoFiatConversion(mCurrencyNum));

                mSlideLayout.setVisibility(View.VISIBLE);
                if (0 == mAmountToSendSatoshi) {
                    mBTCDenominationTextView.setText(mCoreApi.getDefaultBTCDenomination());
                    mFiatDenominationTextView.setText(mCoreApi.getCurrencyAcronym(mCurrencyNum));
                }
            }
        } else {
            mConversionTextView.setText(error.getMessage());
            mConversionTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.btn_help, 0);
            mConversionTextView.setCompoundDrawablePadding(10);
            mConversionTextView.setBackgroundResource(R.color.white_haze);
            mBTCDenominationTextView.setText(mCoreApi.getDefaultBTCDenomination());
            mFiatDenominationTextView.setText(mCoreApi.getCurrencyAcronym(mCurrencyNum));
            mConversionTextView.setTextColor(Color.RED);
            mBitcoinField.setTextColor(Color.RED);
            mFiatField.setTextColor(Color.RED);
            mSlideLayout.setVisibility(View.INVISIBLE);
        }
        mAutoUpdatingTextFields = false;
    }

    private void attemptInitiateSend() {
        // If a send is currently executing, don't send again
        if (mSendOrTransferTask != null) {
            return;
        }
        float remaining = (mInvalidEntryStartMillis + INVALID_ENTRY_WAIT_MILLIS - System.currentTimeMillis()) / 1000;
        // check if invalid entry timeout still active
        if(mInvalidEntryStartMillis > 0) {
            if(mPinRequired) {
                String message = String.format(getString(R.string.fragment_send_confirmation_pin_remaining), remaining);
                mActivity.ShowFadingDialog(message);
            } else {
                String message = String.format(getString(R.string.fragment_send_confirmation_password_remaining), remaining);
                mActivity.ShowFadingDialog(message);
            }
            resetSlider();
            return;
        }

        String enteredPIN = mAuthorizationEdittext.getText().toString();
        if(mPinRequired && enteredPIN.isEmpty()) {
            mActivity.ShowFadingDialog(getString(R.string.fragment_send_confirmation_please_enter_pin), getResources().getInteger(R.integer.alert_hold_time_default));
            mAuthorizationEdittext.requestFocus();
            resetSlider();
            return;
        }

        String userPIN = mCoreApi.GetUserPIN();
         if (mPinRequired && enteredPIN != null && userPIN != null && !userPIN.equals(enteredPIN)) {
             mInvalidEntryCount += 1;
             saveInvalidEntryCount(mInvalidEntryCount);
             if(mInvalidEntryCount >= INVALID_ENTRY_COUNT_MAX) {
                 if(mInvalidEntryStartMillis == 0) {
                     mInvalidEntryStartMillis = System.currentTimeMillis();
                     mHandler.postDelayed(invalidEntryTimer, INVALID_ENTRY_WAIT_MILLIS);
                 }
                 remaining = (mInvalidEntryStartMillis + INVALID_ENTRY_WAIT_MILLIS - System.currentTimeMillis()) / 1000;
                 String message = String.format(getString(R.string.fragment_send_confirmation_pin_remaining), remaining);
                 mActivity.ShowFadingDialog(message);
             } else {
                 mActivity.ShowFadingDialog(getResources().getString(R.string.fragment_send_incorrect_pin_message));
             }
             mAuthorizationEdittext.requestFocus();
            resetSlider();
        } else if (mPasswordRequired) {
             mActivity.showModalProgress(true);
             mCoreApi.SetOnPasswordCheckListener(this, mAuthorizationEdittext.getText().toString());
        } else {
             continueChecks();
         }
    }

    @Override
    public void onPasswordCheck(boolean passwordOkay) {
        mActivity.showModalProgress(false);

        if(passwordOkay) {
            continueChecks();
        }
        else {
            mActivity.ShowFadingDialog(getResources().getString(R.string.fragment_send_incorrect_password_title));
            mAuthorizationEdittext.requestFocus();
            resetSlider();
        }
    }

    private void continueChecks() {
        if (mAmountToSendSatoshi == 0) {
            mActivity.ShowFadingDialog(getResources().getString(R.string.fragment_send_no_satoshi_message));
        } else {
            // show the sending screen
            SuccessFragment mSuccessFragment = new SuccessFragment();
            Bundle bundle = new Bundle();
            bundle.putString(WalletsFragment.FROM_SOURCE, SuccessFragment.TYPE_SEND);
            mSuccessFragment.setArguments(bundle);
            if (null != exitHandler) {
                mActivity.pushFragment(mSuccessFragment);
            } else {
                mActivity.pushFragment(mSuccessFragment, NavigationActivity.Tabs.SEND.ordinal());
            }

            mSpendTarget.setAmountFiat(mAmountFiat);
            mSendOrTransferTask = new SendOrTransferTask(mWallet);
            mSendOrTransferTask.execute();
            hideCalculator();
        }
        resetSlider();
    }

    final Runnable invalidEntryTimer = new Runnable() {
        @Override
        public void run() {
            mInvalidEntryStartMillis = 0;
        }
    };

    private void resetSlider() {
        Animator animator = ObjectAnimator.ofFloat(mConfirmSwipeButton, "translationX", -(mRightThreshold - mConfirmSwipeButton.getX()), 0);
        animator.setDuration(300);
        animator.setStartDelay(0);
        animator.start();
    }

    private void checkAuthorization()
    {
        mPasswordRequired = false;
        mPinRequired = false;

        long dailyLimit = mCoreApi.GetDailySpendLimit();
        boolean dailyLimitSetting = mCoreApi.GetDailySpendLimitSetting();

        if (mToWallet == null && dailyLimitSetting
            && (mAmountToSendSatoshi + mCoreApi.GetTotalSentToday(mWallet) >= dailyLimit)) {
            // Show password
            mPasswordRequired = true;
            mAuthorizationLayout.setVisibility(View.VISIBLE);
            mAuthorizationTextView.setText(getString(R.string.send_confirmation_enter_send_password));
            mAuthorizationEdittext.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else if (mToWallet == null && mCoreApi.GetPINSpendLimitSetting() && mAmountToSendSatoshi >= mCoreApi.GetPINSpendLimit() && !AirbitzApplication.recentlyLoggedIn()) {
            // Show PIN pad
            mPinRequired = true;
            mAuthorizationLayout.setVisibility(View.VISIBLE);
            mAuthorizationTextView.setText(getString(R.string.send_confirmation_enter_send_pin));
            mAuthorizationEdittext.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        } else {
            mAuthorizationLayout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFundsSent) {
            return;
        }
        // NOTE: We set focus listeners here to prevent the keyboard from
        // showing after a send when the fragment are being popped from the stack
        mBitcoinField.setOnFocusChangeListener(mAmountFocusListener);
        mFiatField.setOnFocusChangeListener(mAmountFocusListener);
//        mView.requestFocus();

        mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        bundle = this.getArguments();
        if (bundle == null) {
            CoreAPI.debugLevel(1, "Send confirmation bundle is null");
        } else {
            mUUIDorURI = bundle.getString(SendFragment.UUID);
            mLabel = bundle.getString(SendFragment.LABEL, "");
            mCategory = bundle.getString(SendFragment.CATEGORY, "");
            mNotes = bundle.getString(SendFragment.NOTES, "");
            mAmountToSendSatoshi = bundle.getLong(SendFragment.AMOUNT_SATOSHI);
            mAmountFiat = bundle.getDouble(SendFragment.AMOUNT_FIAT);
            mIsUUID = bundle.getBoolean(SendFragment.IS_UUID);
            mLocked = bundle.getBoolean(SendFragment.LOCKED);
            mSignOnly = bundle.getBoolean(SendFragment.SIGN_ONLY);
            if (mIsUUID) {
                mToWallet = mCoreApi.getWalletFromUUID(mUUIDorURI);
            }
        }

        if(mSpendTarget != null) {
            _sendTo = mSpendTarget.getSpend().getSzName();
            mIsUUID = false;
            _destUUID = mSpendTarget.getSpend().getSzDestUUID();
            if (_destUUID != null) {
                mIsUUID = true;
                mToWallet = mCoreApi.getWalletFromUUID(_destUUID);
            }
            mAmountToSendSatoshi = mSpendTarget.getSpendAmount();
            mLocked = !mSpendTarget.getSpend().getAmountMutable();
        }

        mBitcoinField.setEnabled(!mLocked);
        mBitcoinField.setFocusable(!mLocked);
        mFiatField.setEnabled(!mLocked);
        mFiatField.setFocusable(!mLocked);
        if (mLocked) {
            mMaxButton.setVisibility(View.INVISIBLE);
        } else {
            mMaxButton.setVisibility(View.VISIBLE);
        }

        if (mToWallet != null) {
            mToEdittext.setText(mToWallet.getName());
        } else {
            String temp = _sendTo;
            if (_sendTo.length() > 20) {
                temp = _sendTo.substring(0, 5) + "..." + _sendTo.substring(_sendTo.length() - 5, _sendTo.length());
            }
            mToEdittext.setText(temp);
        }

        mBitcoinField = (EditText) mView.findViewById(R.id.button_bitcoin_balance);
        mFiatField = (EditText) mView.findViewById(R.id.button_dollar_balance);
        mAuthorizationEdittext = (EditText) mView.findViewById(R.id.edittext_pin);

        checkFields();
        checkAuthorization();
    }

    private void checkFields() {
        mAutoUpdatingTextFields = true;

        if (mSavedBitcoin > 0) {
            mAmountToSendSatoshi = mSavedBitcoin;
        }

        if (mSendConfirmationOverrideCurrencyMode) {
            mCurrencyNum = mSendConfirmationCurrencyNumOverride;
        } else {
            mCurrencyNum = mWallet.getCurrencyNum();
        }

        if (mAmountToSendSatoshi > 0) {
            mBitcoinField.setText(mCoreApi.formatSatoshi(mAmountToSendSatoshi, false));
            if (mWallet != null) {
                mFiatField.setText(mCoreApi.FormatCurrency(mAmountToSendSatoshi, mCurrencyNum, false, false));
            }
            calculateFees();

            if (mAuthorizationLayout.getVisibility() == View.VISIBLE) {
                mAuthorizationEdittext.requestFocus();
            }
        } else {
            mFiatField.setText("");
            mBitcoinField.setText("");
            if (mBtcMode) {
                mBitcoinField.requestFocus();
                mCalculator.setEditText(mBitcoinField);
            } else {
                mFiatField.requestFocus();
                mCalculator.setEditText(mFiatField);
            }
            showCalculator();
        }

        mBTCSignTextview.setTypeface(mBitcoinTypeface);
        mBTCSignTextview.setText(mCoreApi.getUserBTCSymbol());
        mBTCDenominationTextView.setText(mCoreApi.getDefaultBTCDenomination());
        mFiatDenominationTextView.setText(mCoreApi.getCurrencyAcronym(mCurrencyNum));
        mFiatSignTextView.setText(mCoreApi.getCurrencyDenomination(mCurrencyNum));
        mConversionTextView.setText(mCoreApi.BTCtoFiatConversion(mCurrencyNum));

        mAutoUpdatingTextFields = false;

        mMaxLocked = false;

        mInvalidEntryCount = getInvalidEntryCount();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSavedBitcoin = mAmountToSendSatoshi;
        if (null != mCalculateFeesTask) {
            mCalculateFeesTask.cancel(true);
        }
        if (null != mMaxAmountTask) {
            mMaxAmountTask.cancel(true);
        }
        hideCalculator();
    }

    @Override
    protected void walletChanged(Wallet newWallet) {
        super.walletChanged(newWallet);

        mWallet = newWallet;
        updateTextFieldContents(mBtcMode);
    }

    private void saveInvalidEntryCount(int entries) {
        SharedPreferences.Editor editor = mActivity.getSharedPreferences(AirbitzApplication.PREFS, Context.MODE_PRIVATE).edit();
        editor.putInt(INVALID_ENTRY_PREF, entries);
        editor.apply();
    }

    static public int getInvalidEntryCount() {
        SharedPreferences prefs = AirbitzApplication.getContext().getSharedPreferences(AirbitzApplication.PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(INVALID_ENTRY_PREF, 0); // default to Automatic
    }



    public class MaxAmountTask extends AsyncTask<Void, Void, Long> {

        MaxAmountTask() {
        }

        @Override
        protected void onPreExecute() {
            CoreAPI.debugLevel(1, "Max calculation called");
        }

        @Override
        protected Long doInBackground(Void... params) {
            CoreAPI.debugLevel(1, "Max calculation started");
            return mSpendTarget.maxSpendable(mWallet.getUUID());
        }

        @Override
        protected void onPostExecute(final Long max) {
            CoreAPI.debugLevel(1, "Max calculation finished");
            mMaxAmountTask = null;
            if (isAdded()) {
                if (max < 0) {
                    CoreAPI.debugLevel(1, "Max calculation error");
                }
                mMaxLocked = false;
                mAmountMax = max;
                mAmountToSendSatoshi = max;
                mSpendTarget.setSpendAmount(max);
                mAutoUpdatingTextFields = true;
                mFiatField.setText(mCoreApi.FormatCurrency(mAmountToSendSatoshi, mCurrencyNum, false, false));
                mFiatSignTextView.setText(mCoreApi.getCurrencyDenomination(mCurrencyNum));
                mConversionTextView.setText(mCoreApi.BTCtoFiatConversion(mCurrencyNum));
                mBitcoinField.setText(mCoreApi.formatSatoshi(mAmountToSendSatoshi, false));

                checkAuthorization();
                calculateFees();
            }
            mAutoUpdatingTextFields = false;
        }

        @Override
        protected void onCancelled() {
            mMaxAmountTask = null;
        }
    }

    public class CalculateFeesTask extends AsyncTask<Void, Void, Long> {
        AirbitzException mFailureException = null;

        @Override
        protected void onPreExecute() {
            mSlideLayout.setEnabled(false);
        }

        @Override
        protected Long doInBackground(Void... params) {
            CoreAPI.debugLevel(1, "Fee calculation started");
            String dest = mIsUUID ? mWallet.getUUID() : mUUIDorURI;
            try {
                return mSpendTarget.calcSendFees(mWallet.getUUID());
            } catch (AirbitzException e) {
                mFailureException = e;
                return 0L;
            }
        }

        @Override
        protected void onPostExecute(final Long fees) {
            CoreAPI.debugLevel(1, "Fee calculation ended");
            if (isAdded()) {
                mCalculateFeesTask = null;
                mFees = fees;
                UpdateFeeFields(fees, mFailureException);
                mSlideLayout.setEnabled(true);
            }
        }

        @Override
        protected void onCancelled() {
            mCalculateFeesTask = null;
        }
    }

    public class SendOrTransferTask extends AsyncTask<Void, Void, String> {
        private Wallet mFromWallet;

        SendOrTransferTask(Wallet fromWallet) {
            mFromWallet = fromWallet;
        }

        @Override
        protected void onPreExecute() {
            CoreAPI.debugLevel(1, "SEND called");
        }

        @Override
        protected String doInBackground(Void... params) {
            CoreAPI.debugLevel(1, "Initiating SEND");
            try {
                // Hack: Give the fragment manager time to finish
                Thread.sleep(1000);
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
            if (mSignOnly) {
                // Returns raw tx
                return mSpendTarget.signTx(mFromWallet.getUUID());
            } else {
                // Returns txid
                return mSpendTarget.approve(mFromWallet.getUUID());
            }
        }

        @Override
        protected void onPostExecute(final String txResult) {
            CoreAPI.debugLevel(1, "SEND done");
            mSendOrTransferTask = null;
            if (txResult == null) {
                CoreAPI.debugLevel(1, "Error during send ");
                if (mActivity != null) {
                    mActivity.popFragment(); // stop the sending screen
                    if (null == exitHandler) {
                        mDelayedMessage = mActivity.getResources().getString(R.string.fragment_send_confirmation_send_error_title);
                        mHandler.postDelayed(mDelayedErrorMessage, 500);
                    } else {
                        mActivity.popFragment(); // stop the sending screen
                        exitHandler.error();
                    }
                }
            } else {
                if (mActivity != null) {
                    saveInvalidEntryCount(0);
                    AudioPlayer.play(mActivity, R.raw.bitcoin_sent);
                    mActivity.popFragment(); // stop the sending screen
                    if (null != exitHandler) {
                        mActivity.popFragment();
                        exitHandler.success(txResult);
                    } else {
                        mFundsSent = true;
                        String returnUrl = mSpendTarget.getSpend().getSzRet();
                        mActivity.onSentFunds(mFromWallet.getUUID(), txResult, returnUrl);
                    }
                }
            }
        }

        @Override
        protected void onCancelled() {
            mActivity.popFragment(); // stop the sending screen
            mSendOrTransferTask = null;
        }
    }

    Runnable mDelayedErrorMessage = new Runnable() {
        @Override
        public void run() {
            if (mDelayedMessage != null) {
                mActivity.ShowFadingDialog(mDelayedMessage);
            }
        }
    };

    final View.OnFocusChangeListener mAmountFocusListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            if (view == mBitcoinField || view == mFiatField) {
                if (hasFocus) {
                    EditText edittext = (EditText) view;
                    edittext.selectAll();
                    mCalculator.setEditText(edittext);
                    showCalculator();
                }
            } else {
                hideCalculator();
            }
        }
    };

}
