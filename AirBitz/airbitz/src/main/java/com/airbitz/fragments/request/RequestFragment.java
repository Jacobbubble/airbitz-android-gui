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

package com.airbitz.fragments.request;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.NfcManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.airbitz.AirbitzApplication;
import com.airbitz.R;
import com.airbitz.activities.NavigationActivity;
import com.airbitz.api.AccountSettings;
import com.airbitz.api.CoreAPI;
import com.airbitz.bitbeacon.BeaconRequest;
import com.airbitz.bitbeacon.BleUtil;
import com.airbitz.fragments.HelpFragment;
import com.airbitz.fragments.WalletBaseFragment;
import com.airbitz.fragments.settings.SettingFragment;
import com.airbitz.models.Contact;
import com.airbitz.models.Transaction;
import com.airbitz.models.Wallet;
import com.airbitz.objects.Calculator;
import com.airbitz.objects.DessertView;
import com.airbitz.utils.Common;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class RequestFragment extends WalletBaseFragment implements
        ContactPickerFragment.ContactSelection,
        NfcAdapter.CreateNdefMessageCallback,
        Calculator.OnCalculatorKey,
        BeaconRequest.BeaconRequestListener
{

    public static final String FROM_UUID = "com.airbitz.request.from_uuid";
    public static final String MERCHANT_MODE = "com.airbitz.request.merchant_mode";

    private final String FIRST_USAGE_COUNT = "com.airbitz.fragments.requestqr.firstusagecount";
    private final int READVERTISE_REPEAT_PERIOD = 1000 * 60 * 2;

    private final String TAG = getClass().getSimpleName();
    private EditText mAmountField;
    private boolean mAutoUpdatingTextFields = false;
    private boolean mInPartialPayment = false;

    private TextView mConverterTextView;
    private TextView mDenominationTextView;
    private TextView mOtherDenominationTextView;
    private TextView mOtherAmountTextView;
    private Calculator mCalculator;
    private CoreAPI mCoreAPI;
    private View mView;
    private View mBottomButtons;

    private Long mSavedSatoshi;
    private String mSavedCurrency;
    private boolean mAmountIsBitcoin = false;

    private NavigationActivity mActivity;
    private Handler mHandler;
    private BeaconRequest mBeaconRequest;
    private TextView mBitcoinAddress;
    private long mAmountSatoshi;
    private long mOriginalAmountSatoshi;
    private String mId;
    private String mAddress;
    private String mContentURL;
    private String mRequestURI;
    private NfcAdapter mNfcAdapter;
    private ImageView mNFCImageView;
    private ImageView mBLEImageView;
    private Button mSMSButton;
    private Button mEmailButton;
    private Button mCopyButton;
    private DessertView mDessertView;
    private TextView mReceivedTextView;
    private TextView mRemainingTextView;

    private boolean emailType = false;
    private ImageView mQRView;
    private View mQRProgress;
    private Bitmap mQRBitmap;
    private CreateBitmapTask mCreateBitmapTask;

    private float mOrigQrHeight;
    private float mQrPadding;
    private int mFabCoords[] = new int[2];
    private int mQrCoords[] = new int[2];
    private int mCalcCoords[] = new int[2];

    // currency swap variables
    static final int SWAP_DURATION = 200;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        mCoreAPI = CoreAPI.getApi();
        mActivity = (NavigationActivity) getActivity();
        mBeaconRequest = new BeaconRequest(mActivity);
        mBeaconRequest.setRequestListener(this);
    }

    @Override
    public void advertiseStartFailed() {
        mActivity.ShowFadingDialog(getString(R.string.request_qr_ble_advertise_start_failed));
    }

    @Override
    public void invalidService() {
        mActivity.ShowFadingDialog(
                String.format(
                    mActivity.getString(R.string.request_qr_ble_invalid_service),
                    mActivity.getString(R.string.app_name)));
    }

    @Override
    public void receivedConnection(String text) {
        Contact nameInContacts = findMatchingContact(text);
        text += "\nConnected";
        if (nameInContacts != null) {
            mActivity.ShowFadingDialog(text, nameInContacts.getThumbnail(), getResources().getInteger(R.integer.alert_hold_time_default), true);
        } else {
            mActivity.ShowFadingDialog(text, "", getResources().getInteger(R.integer.alert_hold_time_default), true);
        }
    }

    @Override
    protected String getSubtitle() {
        return mActivity.getString(R.string.fragment_request_subtitle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Resources r = getResources();

        mView = inflater.inflate(R.layout.fragment_request, container, false);
        mAmountField = (EditText) mView.findViewById(R.id.request_amount);
        mAmountField.setTypeface(NavigationActivity.latoRegularTypeFace);
        final TextWatcher mAmountChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                mAmountField.setSelection(mAmountField.getText().toString().length());
                mInPartialPayment = false;
                if (!mAutoUpdatingTextFields) {
                    mReceivedTextView.setText(getResources().getString(R.string.request_qr_waiting_for_payment));
                    mRemainingTextView.setVisibility(View.GONE);
                    mRemainingTextView.setText("");

                    amountChanged();
                }
            }
        };

        mAmountField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAmountField.post(new Runnable() {
                    @Override
                    public void run() {
                        mAmountField.setSelection(mAmountField.getText().length());
                    }
                });
                if (mCalculator.getVisibility() != View.VISIBLE) {
                    showCalculator();
                }
            }
        });
        mAmountField.addTextChangedListener(mAmountChangedListener);
        mAmountField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    mAmountField.setText("");
                    showCalculator();
                    mCalculator.setEditText(mAmountField);
                } else {
                    hideCalculator();
                }
            }
        });

        TextView.OnEditorActionListener amountEditorListener = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                        keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    amountChanged();
                    return true;
                }
                else {
                    return false;
                }
            }
        };

        mAmountField.setOnEditorActionListener(amountEditorListener);

        mCalculator = (Calculator) mActivity.findViewById(R.id.navigation_calculator_layout);
        mCalculator.setCalculatorKeyListener(this);
        mCalculator.setEditText(mAmountField);

        mQRProgress = mView.findViewById(R.id.progress_horizontal);
        mQRView = (ImageView) mView.findViewById(R.id.qr_code_view);
        mQRView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCalculator.getVisibility() == View.VISIBLE) {
                    hideCalculator();
                } else {
                    showCalculator();
                }
            }
        });

        mDessertView = (DessertView) mView.findViewById(R.id.dropdown_alert);
        mReceivedTextView = (TextView) mView.findViewById(R.id.amount_requested);
        mRemainingTextView = (TextView) mView.findViewById(R.id.amount_received);
        if (SettingFragment.getMerchantModePref()) {
            showCalculator();
        } else {
            mCalculator.forceHide();
        }

        mConverterTextView = (TextView) mView.findViewById(R.id.textview_converter);
        mConverterTextView.setTypeface(NavigationActivity.latoRegularTypeFace);
        mDenominationTextView = (TextView) mView.findViewById(R.id.request_selected_denomination);
        mOtherDenominationTextView = (TextView) mView.findViewById(R.id.request_not_selected_denomination);
        mOtherAmountTextView = (TextView) mView.findViewById(R.id.request_not_selected_value);
        mBitcoinAddress = (TextView) mView.findViewById(R.id.request_bitcoin_address);

        mBottomButtons = mView.findViewById(R.id.request_bottom_buttons);
        mCopyButton = (Button) mView.findViewById(R.id.fragment_triple_selector_left);
        mCopyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyToClipboard();
            }
        });
        mEmailButton = (Button) mView.findViewById(R.id.fragment_triple_selector_center);
        mEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startEmail();
            }
        });
        mSMSButton = (Button) mView.findViewById(R.id.fragment_triple_selector_right);
        mSMSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSMS();
            }
        });

        mDenominationTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                swapStart();
            }
        });

        mOtherAmountTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                swapStart();
            }
        });
        mOtherDenominationTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                swapStart();
            }
        });

        updateMode();

        // Prevent OS keyboard from showing
        try {
            final Method method = EditText.class.getMethod(
                    "setShowSoftInputOnFocus"
                    , new Class[] { boolean.class });
            method.setAccessible(true);
            method.invoke(mAmountField, false);
        } catch (Exception e) {
            // ignore
        }

        mQrPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, r.getDisplayMetrics());
        return mView;
    }

    @Override
    protected float getFabTop() {
        return mActivity.getFabTop()
             - mBottomButtons.getHeight()
             - mBitcoinAddress.getHeight();
    }

    @Override
    public boolean onBackPress() {
        if (mCalculator.getVisibility() == View.VISIBLE) {
            hideCalculator();
            return true;
        } else {
            return super.onBackPress();
        }
    }

    @Override
    protected void walletChanged(Wallet newWallet) {
        super.walletChanged(newWallet);

        updateAmount();
    }

    private void updateConversion() {
        if (null != mWallet){
            if (mAmountIsBitcoin) {
                long satoshi = mCoreAPI.denominationToSatoshi(mAmountField.getText().toString());
                String currency = mCoreAPI.FormatCurrency(mAmountSatoshi, mWallet.getCurrencyNum(), false, false);
                mDenominationTextView.setText(mCoreApi.getDefaultBTCDenomination());
                mOtherDenominationTextView.setText(mCoreApi.currencyCodeLookup(mWallet.getCurrencyNum()));
                mOtherAmountTextView.setText(currency);
            } else {
                long satoshi = mCoreApi.parseFiatToSatoshi(mAmountField.getText().toString(), mWallet.getCurrencyNum());
                mDenominationTextView.setText(mCoreApi.currencyCodeLookup(mWallet.getCurrencyNum()));
                mOtherDenominationTextView.setText(mCoreApi.getDefaultBTCDenomination());
                mOtherAmountTextView.setText(mCoreAPI.formatSatoshi(satoshi, false));
            }
            if (TextUtils.isEmpty(mAmountField.getText())) {
                mOtherAmountTextView.setText("");
            }
            int currencyNum = mWallet.getCurrencyNum();
            mConverterTextView.setText(mCoreAPI.BTCtoFiatConversion(currencyNum));
            updateMode();
        }
    }

    private void updateMode() {
        mDenominationTextView.setVisibility(View.VISIBLE);
    }

    private void updateAmount() {
        Wallet wallet = mWallet;
        if (mAmountIsBitcoin) {
            String bitcoin = mAmountField.getText().toString();
            if (!mCoreAPI.TooMuchBitcoin(bitcoin)) {
                mAmountSatoshi = mCoreAPI.denominationToSatoshi(bitcoin);
            } else {
                CoreAPI.debugLevel(1, "Too much bitcoin");
            }
        } else {
            String fiat = mAmountField.getText().toString();
            try {
                if (!mCoreAPI.TooMuchFiat(fiat, wallet.getCurrencyNum())) {
                    mAmountSatoshi = mCoreApi.parseFiatToSatoshi(fiat, mWallet.getCurrencyNum());
                } else {
                    CoreAPI.debugLevel(1, "Too much fiat");
                }
            } catch (NumberFormatException e) {
                //not a double, ignore
            }
        }
        updateConversion();
        createNewQRBitmap();
    }

    @Override
    protected void onAddOptions(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_request, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (isMenuExpanded()) {
            return super.onOptionsItemSelected(item);
        }
        switch (item.getItemId()) {
        case R.id.action_refresh:
            onRefresh();
            return true;
        case R.id.action_help:
            mActivity.pushFragment(
                new HelpFragment(HelpFragment.REQUEST),
                    NavigationActivity.Tabs.REQUEST.ordinal());
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AccountSettings settings = mCoreAPI.coreSettings();
        if (settings != null && settings.getBNameOnPayments()) {
            String name = settings.getSzFullName();
            mBeaconRequest.setBroadcastName(name);
        } else {
            mBeaconRequest.setBroadcastName(
                getResources().getString(R.string.request_qr_unknown));
        }
        mInPartialPayment = false;
        mActivity.hideSoftKeyboard(mAmountField);
        mHandler.postDelayed(new Runnable() {
            public void run() {
                checkFirstUsage();
            }
        }, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        if (TextUtils.isEmpty(mAmountField.getText().toString())) {
            mSavedSatoshi = null;
            mSavedCurrency = null;
        } else {
            if (mAmountIsBitcoin) {
                mSavedSatoshi = mCoreAPI.denominationToSatoshi(mAmountField.getText().toString());
                mSavedCurrency = null;
            } else {
                mSavedCurrency = mAmountField.getText().toString();
                mSavedSatoshi = null;
            }
        }
        if (!SettingFragment.getMerchantModePref()) {
            hideCalculator();
        }

        mBeaconRequest.stop();
    }

    @Override
    protected void onExchangeRatesChange() {
        if (mInPartialPayment)
            return;

        if (mWallet != null) {
            updateAmount();
        }
    }

    @Override
    public void onWalletsLoaded() {
        super.onWalletsLoaded();

        mAutoUpdatingTextFields = true;
        if (mSavedSatoshi != null) {
            mAmountField.setText(mCoreAPI.formatSatoshi(mSavedSatoshi, false));
        } else if (mSavedCurrency != null) {
            mAmountField.setText(mSavedCurrency);
        }
        mSavedSatoshi = null;
        mSavedCurrency = null;

        mAutoUpdatingTextFields = false;

        updateConversion();
        createNewQRBitmap();
    }

    private void checkFirstUsage() {
        new Thread(new Runnable() {
            public void run() {
                SharedPreferences prefs = AirbitzApplication.getContext().getSharedPreferences(AirbitzApplication.PREFS, Context.MODE_PRIVATE);
                int count = prefs.getInt(FIRST_USAGE_COUNT, 1);
                if(count <= 2) {
                    count++;
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(FIRST_USAGE_COUNT, count);
                    editor.apply();

                    notifyFirstUsage();
                }
            }
        }).start();
    }

    private void notifyFirstUsage() {
        mHandler.post(new Runnable() {
            public void run() {
                mActivity.ShowFadingDialog(getString(R.string.request_qr_first_usage), getResources().getInteger(R.integer.alert_hold_time_help_popups));
            }
        });
    }

    private void checkNFC() {
        final NfcManager nfcManager = (NfcManager) mActivity.getSystemService(Context.NFC_SERVICE);
        mNfcAdapter = nfcManager.getDefaultAdapter();

        if (mNfcAdapter != null && mNfcAdapter.isEnabled() && SettingFragment.getNFCPref()) {
//            mNFCImageView.setVisibility(View.VISIBLE);
            mNfcAdapter.setNdefPushMessageCallback(this, mActivity);
        }
    }

    @Override
    public void onContactSelection(Contact contact) {
        if (emailType) {
            if (mQRBitmap != null) {
                mContentURL = MediaStore.Images.Media.insertImage(mActivity.getContentResolver(), mQRBitmap, mAddress, null);
                if (mContentURL != null) {
                    finishEmail(contact, Uri.parse(mContentURL));
                } else {
                    showNoQRAttached(contact);
                }
            } else {
                mActivity.ShowOkMessageDialog("", getString(R.string.request_qr_bitmap_error));
            }
        } else {
            finishSMS(contact);
        }
    }

    private void copyToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.request_qr_title), mAddress);
        clipboard.setPrimaryClip(clip);
        mActivity.ShowFadingDialog(getString(R.string.request_qr_ble_copied));
    }

    private void startSMS() {
        emailType = false;
        Bundle bundle = new Bundle();
        bundle.putString(ContactPickerFragment.TYPE, ContactPickerFragment.SMS);
        ContactPickerFragment.pushFragment(mActivity, bundle, this);
    }

    private void finishSMS(Contact contact) {
        String defaultName = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            defaultName = Telephony.Sms.getDefaultSmsPackage(mActivity); // Android 4.4 and up
        }

        String name = getString(R.string.request_qr_unknown);
        AccountSettings settings = mCoreAPI.coreSettings();
        if (settings != null) {
            if (settings.getBNameOnPayments()) {
                name = settings.getSzFullName();
                if (name == null) {
                    name = getString(R.string.request_qr_unknown);
                }
            }
        }
        String textToSend = fillTemplate(R.raw.sms_template, name);

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        if (defaultName != null) {
            intent.setPackage(defaultName);
        }
        intent.setData(Uri.parse("smsto:" + contact.getPhone()));  // This ensures only SMS apps respond
        intent.putExtra("sms_body", textToSend);

        startActivity(Intent.createChooser(intent, "SMS"));

        mCoreAPI.finalizeRequest(contact, "SMS", mId, mWallet);
    }

    private void startEmail() {
        emailType = true;
        Bundle bundle = new Bundle();
        bundle.putString(ContactPickerFragment.TYPE, ContactPickerFragment.EMAIL);
        ContactPickerFragment.pushFragment(mActivity, bundle, this);
    }

    private void finishEmail(Contact contact, Uri uri) {
        ArrayList<Uri> uris = new ArrayList<Uri>();

        if (uri != null) {
            uris.add(Uri.parse(mContentURL));
        }

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{contact.getEmail()});
        intent.putExtra(Intent.EXTRA_SUBJECT,
                String.format(getString(R.string.request_qr_email_title),
                        getString(R.string.app_name)));

        String name = getString(R.string.request_qr_unknown);
        AccountSettings settings = mCoreAPI.coreSettings();
        if (settings != null) {
            if (settings.getBNameOnPayments()) {
                name = settings.getSzFullName();
            }
        }

        String html = fillTemplate(R.raw.email_template, name);
        intent.putExtra(Intent.EXTRA_STREAM, uris);
        intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(html));
        startActivity(Intent.createChooser(intent, "email"));

        mCoreAPI.finalizeRequest(contact, "Email", mId, mWallet);
    }

    private void showNoQRAttached(final Contact contact) {
        getString(R.string.request_qr_image_store_error);
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));
        builder.setMessage(getString(R.string.request_qr_image_store_error))
                .setTitle("")
                .setCancelable(false)
                .setNeutralButton(getResources().getString(R.string.string_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finishEmail(contact, null);
                                dialog.cancel();
                            }
                        }
                );
        builder.create().show();
    }

    private String fillTemplate(int id, String fullName) {
        String amountBTC = mCoreAPI.formatSatoshi(mAmountSatoshi, false, 8);
        String amountBits = mCoreAPI.formatSatoshi(mAmountSatoshi, false, 2);

        String bitcoinURL = "bitcoin://";
        String redirectURL = mRequestURI;

        if (mRequestURI.contains("bitcoin:")) {
            String[] typeAddress = mRequestURI.split(":");
            String address = typeAddress[1];

            bitcoinURL += address;
            redirectURL = "https://airbitz.co/blf/?address=" + address;
        }

        String content = Common.evaluateTextFile(getActivity(), id);

        List<String> searchList = new ArrayList<String>();
        searchList.add("[[abtag FROM]]");
        searchList.add("[[abtag BITCOIN_URL]]");
        searchList.add("[[abtag REDIRECT_URL]]");
        searchList.add("[[abtag BITCOIN_URI]]");
        searchList.add("[[abtag ADDRESS]]");
        searchList.add("[[abtag AMOUNT_BTC]]");
        searchList.add("[[abtag AMOUNT_BITS]]");

        List<String> replaceList = new ArrayList<String>();
        if (fullName == null)
            replaceList.add("");
        else
            replaceList.add(fullName);
        replaceList.add(bitcoinURL);
        replaceList.add(redirectURL);
        replaceList.add(mRequestURI);
        replaceList.add(mAddress);
        replaceList.add(amountBTC);
        replaceList.add(amountBits);

        for (int i = 0; i < searchList.size(); i++) {
            content = content.replace(searchList.get(i), replaceList.get(i));
        }
        return content;
    }

    public boolean isShowingQRCodeFor(String walletUUID, String txId) {
        CoreAPI.debugLevel(1, "isShowingQRCodeFor: " + walletUUID + " " + txId);
        Transaction tx = mCoreAPI.getTransaction(walletUUID, txId);
        if (null == tx)
            return false;

        if (tx.getOutputs() == null || mAddress == null) {
            return false;
        }
        CoreAPI.debugLevel(1, "isShowingQRCodeFor: hasOutputs");
        for (CoreAPI.TxOutput output : tx.getOutputs()) {
            CoreAPI.debugLevel(1, output.getmInput() + " " + mAddress + " " + output.getAddress());
            if (!output.getmInput() && mAddress.equals(output.getAddress())) {
                return true;
            }
        }
        CoreAPI.debugLevel(1, "isShowingQRCodeFor: noMatch");
        return false;
    }

    public boolean isMerchantDonation() {
        return (mAmountSatoshi == 0 && SettingFragment.getMerchantModePref());
    }

    public long requestDifference(String walletUUID, String txId) {
        CoreAPI.debugLevel(1, "requestDifference: " + walletUUID + " " + txId);
        if (mAmountSatoshi > 0) {
            Transaction tx = mCoreAPI.getTransaction(walletUUID, txId);
            return mAmountSatoshi - tx.getAmountSatoshi();
        } else {
            return 0;
        }
    }

    public void showDonation(String uuid, String txId) {
        Transaction tx = mCoreApi.getTransaction(uuid, txId);
        showDonation(tx.getAmountSatoshi());
    }

    public void showDonation(long amount) {
        mDessertView.setOkIcon();
        mDessertView.getLine1().setText(R.string.string_payment_received);
        mDessertView.getLine2().setText(mCoreAPI.formatSatoshi(amount, true));
        mDessertView.getLine3().setVisibility(View.VISIBLE);
        mDessertView.getLine3().setText(
            mCoreApi.FormatCurrency(amount, mWallet.getCurrencyNum(), false, true));
        mDessertView.show();

        createNewQRBitmap();
    }

    public void updateWithAmount(long newAmount) {
        mAutoUpdatingTextFields = true;
        mInPartialPayment = true;
        if (mOriginalAmountSatoshi == 0) {
            mOriginalAmountSatoshi = mAmountSatoshi;
        }
        mAmountSatoshi = newAmount;

        mReceivedTextView.setText(
                String.format(getResources().getString(R.string.bitcoin_received),
                        mCoreAPI.formatSatoshi(mOriginalAmountSatoshi - mAmountSatoshi, true))
        );
        mRemainingTextView.setVisibility(View.VISIBLE);
        mRemainingTextView.setText(
                String.format(getResources().getString(R.string.bitcoin_remaining),
                        mCoreAPI.formatSatoshi(mAmountSatoshi, true))
        );

        mDessertView.setWarningIcon();
        mDessertView.getLine1().setText(R.string.received_partial_bitcoin_title);
        mDessertView.getLine2().setText(R.string.received_partial_bitcoin_message);
        mDessertView.getLine3().setVisibility(View.GONE);
        mDessertView.show();

        createNewQRBitmap();
        mAutoUpdatingTextFields = false;
    }

    public void amountChanged() {
        if (mWallet != null) {
            if (mAmountIsBitcoin) {
                mAmountSatoshi = mCoreAPI.denominationToSatoshi(mAmountField.getText().toString());
            } else {
                mAmountSatoshi = mCoreApi.parseFiatToSatoshi(mAmountField.getText().toString(), mWallet.getCurrencyNum());
            }
            updateConversion();
            createNewQRBitmap();
        }
    }

    private void createNewQRBitmap() {
        if (mCreateBitmapTask != null) {
            mCreateBitmapTask.cancel(true);
        }
        if (null != mWallet) {
            // Create a new request and qr code
            mCreateBitmapTask = new CreateBitmapTask(mWallet, mAmountSatoshi);
            mCreateBitmapTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /*
     * Send an Ndef message when a device with NFC is detected
     */
    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
        if(mRequestURI != null) {
            CoreAPI.debugLevel(1, "Creating NFC request: " + mRequestURI);
            return new NdefMessage(NdefRecord.createUri(mRequestURI));
        }
        else
            return null;
    }

    @Override
    public void OnCalculatorKeyPressed(String tag) {
        if (tag.equals("done")) {
            hideCalculator();
        }
    }

    public class CreateBitmapTask extends AsyncTask<Void, Void, Boolean> {

        private Wallet wallet;
        private long satoshis;
        private String requestId;
        private String address;
        private String uri;
        private Bitmap qrBitmap;

        public CreateBitmapTask(Wallet wallet, long satoshis) {
            this.satoshis = satoshis;
            this.wallet = wallet;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            CoreAPI.debugLevel(1, "Starting Receive Request at:" + System.currentTimeMillis());
            requestId = mCoreAPI.createReceiveRequestFor(wallet, "", "", satoshis);
            address = mCoreAPI.getRequestAddress(wallet.getUUID(), requestId);
            try {
                // data in barcode is like bitcoin:address?amount=0.001
                CoreAPI.debugLevel(1, "Starting QRCodeBitmap at:" + System.currentTimeMillis());
                qrBitmap = mCoreAPI.getQRCodeBitmap(wallet.getUUID(), requestId);
                qrBitmap = Common.AddWhiteBorder(qrBitmap);
                CoreAPI.debugLevel(1, "Ending QRCodeBitmap at:" + System.currentTimeMillis());
                uri = mCoreAPI.getRequestURI();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mId = requestId;
            mAddress = address;
            mQRBitmap = qrBitmap;
            mRequestURI = uri;
            if (isAdded()) {
                onCancelled();
                if(success) {
                    checkNFC();
                    checkBle();
                    mBitcoinAddress.setText(mAddress);
                    if (mQRBitmap != null) {
                        mQRView.setImageBitmap(mQRBitmap);
                        mQRProgress.setVisibility(View.GONE);
                    }
                    mCoreAPI.prioritizeAddress(mAddress, mWallet.getUUID());
                    alignQrCode();
                }
            }
        }

        @Override
        protected void onCancelled() {
            mCreateBitmapTask = null;
//            mActivity.showModalProgress(false);
        }
    }

    private void checkBle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && SettingFragment.getBLEPref() &&
                BleUtil.isBleAdvertiseAvailable(mActivity)) {
            mBeaconRequest.stop();
            mBeaconRequest.startRepeated(mRequestURI);
        }
    }


    private Contact findMatchingContact(String displayName) {
        String id = null;
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI,Uri.encode(displayName.trim()));
        Cursor mapContact = mActivity.getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI}, null, null, null);
        if(mapContact.moveToNext())
        {
            id = mapContact.getString(mapContact.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI));
            return new Contact(displayName, null, null, id);
        }
        else {
            return null;
        }
    }

    private Interpolator mCalcInterpolator = new AccelerateInterpolator() {
        @Override
        public float getInterpolation(float input) {
            mHandler.post(new Runnable() {
                public void run() {
                    mView.invalidate();
                }
            });
            return super.getInterpolation(input);
        }
    };

    @Override
    public void finishFabAnimation() {
        alignQrCode();
    }

    private void alignQrCode() {
        mQRView.getLocationOnScreen(mQrCoords);
        mCalculator.getLocationOnScreen(mCalcCoords);
        if (mOrigQrHeight == 0.0f) {
            mOrigQrHeight = mQRView.getHeight();
        }

        View fab = mActivity.getFabView();
        fab.getLocationOnScreen(mFabCoords);
        if (mQrCoords[1] + mOrigQrHeight > mFabCoords[1]) {
            mOrigQrHeight = mFabCoords[1] - mQrCoords[1];
        }

        float qrY = mQrCoords[1];
        float calcY = mCalcCoords[1];
        float diff = calcY - qrY;
        int newHeight = (int) (diff - mQrPadding);
        if (newHeight > mOrigQrHeight) {
            newHeight = (int) mOrigQrHeight;
        }
        if (mQRView.getHeight() != newHeight) {
            mQRView.getLayoutParams().height = newHeight;
            mQRView.requestLayout();
        }
    }

    private ValueAnimator.AnimatorUpdateListener mUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
            alignQrCode();
            if (animation.getAnimatedFraction() == 1.0f) {
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        CoreAPI.debugLevel(1, "Last");
                        alignQrCode();
                    }
                }, 100);
            }
        }
    };

    private void showCalculator() {
        mCalculator.showCalculator(mUpdateListener);
    }

    private void hideCalculator() {
        mCalculator.hideCalculator(mUpdateListener);
    }

    private void swapAmount() {
        mAmountIsBitcoin = !mAmountIsBitcoin;
        if (mAmountIsBitcoin) {
            String fiat = mAmountField.getText().toString();
            try {
                if (!mCoreAPI.TooMuchFiat(fiat, mWallet.getCurrencyNum())) {
                    mAmountSatoshi = mCoreApi.parseFiatToSatoshi(fiat, mWallet.getCurrencyNum());
                    if (mAmountSatoshi == 0) {
                        mAmountField.setText("");
                    } else {
                        mAmountField.setText(mCoreAPI.formatSatoshi(mAmountSatoshi, false));
                    }
                } else {
                    CoreAPI.debugLevel(1, "Too much fiat");
                }
            } catch (NumberFormatException e) {
                //not a double, ignore
            }
        } else {
            String bitcoin = mAmountField.getText().toString();
            if (!mCoreAPI.TooMuchBitcoin(bitcoin)) {
                mAmountSatoshi = mCoreAPI.denominationToSatoshi(bitcoin);
                if (mAmountSatoshi == 0) {
                    mAmountField.setText("");
                } else {
                    mAmountField.setText(mCoreAPI.FormatCurrency(mAmountSatoshi, mWallet.getCurrencyNum(), false, false));
                }
            } else {
                CoreAPI.debugLevel(1, "Too much bitcoin");
            }
        }
        updateConversion();
    }

    private void swapStart() {
        final boolean animateAmount = !TextUtils.isEmpty(mAmountField.getText());
        mDenominationTextView.setPivotY(mDenominationTextView.getHeight());
        mAmountField.setPivotY(mAmountField.getHeight());
        mOtherDenominationTextView.setPivotY(0f);
        mOtherAmountTextView.setPivotY(0f);

        Animator anim = AnimatorInflater.loadAnimator(mActivity, R.animator.scale_down);
        Animator top = anim.clone();
        top.setTarget(mDenominationTextView);

        Animator bot = anim.clone();
        bot.setTarget(mOtherDenominationTextView);

        List<Animator> list = new LinkedList<>();
        list.add(top);
        list.add(bot);

        if (animateAmount) {
            Animator amt = anim.clone();
            amt.setTarget(mAmountField);
            list.add(amt);

            amt = anim.clone();
            amt.setTarget(mOtherAmountTextView);
            list.add(amt);
        }

        AnimatorSet set = new AnimatorSet();
        set.setDuration(SWAP_DURATION / 2);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                swapAmount();
                swapEnd(animateAmount);
            }
        });
        set.playTogether(list.toArray(new Animator[list.size()]));
        set.start();
    }

    private void swapEnd(boolean animateAmount) {
        Animator anim = AnimatorInflater.loadAnimator(mActivity, R.animator.scale_up);

        Animator top = anim.clone();
        top.setTarget(mDenominationTextView);

        Animator bot = anim.clone();
        bot.setTarget(mOtherDenominationTextView);

        List<Animator> list = new LinkedList<>();
        list.add(top);
        list.add(bot);

        if (animateAmount) {
            Animator amt = anim.clone();
            amt.setTarget(mAmountField);
            list.add(amt);

            amt = anim.clone();
            amt.setTarget(mOtherAmountTextView);
            list.add(amt);
        }

        AnimatorSet set = new AnimatorSet();
        set.setDuration(SWAP_DURATION / 2);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.playTogether(list.toArray(new Animator[list.size()]));
        set.start();
    }

    public void onRefresh() {
        if (mWallet != null) {
            mCoreAPI.connectWatcher(mWallet.getUUID());
        }
        mActivity.showModalProgress(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mActivity.showModalProgress(false);
            }
        }, 1000);
    }
}
