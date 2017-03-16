package com.phonepe.merchantsdk.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.phonepe.android.sdk.api.PhonePe;
import com.phonepe.android.sdk.base.enums.CreditType;
import com.phonepe.android.sdk.base.enums.ErrorCode;
import com.phonepe.android.sdk.base.models.AccountingInfo;
import com.phonepe.android.sdk.base.models.ErrorInfo;
import com.phonepe.android.sdk.base.models.InstrumentSplitPreference;
import com.phonepe.android.sdk.base.models.PaymentInstrumentsPreference;
import com.phonepe.android.sdk.domain.builders.AccountingInfoBuilder;
import com.phonepe.android.sdk.domain.builders.CreditRequestBuilder;
import com.phonepe.android.sdk.domain.builders.DebitRequestBuilder;
import com.phonepe.android.sdk.domain.builders.OrderInfoBuilder;
import com.phonepe.android.sdk.domain.builders.PaymentInstrumentPreferenceBuilder;
import com.phonepe.android.sdk.domain.builders.SignUpRequestBuilder;
import com.phonepe.android.sdk.domain.builders.UserInfoBuilder;

import com.phonepe.android.sdk.base.listeners.TransactionCompleteListener;
import com.phonepe.android.sdk.base.models.CreditRequest;
import com.phonepe.android.sdk.base.models.DebitRequest;
import com.phonepe.android.sdk.base.models.OrderInfo;
import com.phonepe.android.sdk.base.models.PayInstrumentOption;
import com.phonepe.android.sdk.utils.CheckSumUtils;
import com.phonepe.merchantsdk.demo.utils.AppUtils;
import com.phonepe.merchantsdk.demo.utils.CacheUtils;
import com.phonepe.merchantsdk.demo.utils.Constants;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.phonepe.merchantsdk.demo.utils.AppUtils.isEmpty;

public class MainActivity extends AppCompatActivity {
    private static final long ITEM_AMOUNT = 420l;

    @BindView(R.id.id_debit_amount)
    TextView mDebitAmountTextView;

    @BindView(R.id.id_credit_amount)
    TextView mCreditAmountTextView;

    @BindView(R.id.id_result)
    TextView resultTextView;

    @BindView(R.id.id_credit_type)
    SwitchCompat mCreditTypeOption;

    @OnClick(R.id.id_install)
    void installPhonePe() {
        //PhonePe.installPhone(this);
    }

    @OnClick(R.id.id_register)
    void showRegisterDemo() {
        startLoginRegister(false);
    }

    @OnClick(R.id.id_debit)
    void showDebitDemo() {
        startDebit();
    }

    @OnClick(R.id.id_credit)
    void showCreditDemo() {
        startCredit();
    }

    @OnClick(R.id.id_flipkart)
    void showFlipkartDemo() {
        startFlipkart();
    }


    private String mMobileNo;
    private String mEmail;
    private String mName;

    @OnClick(R.id.id_account)
    void showAccountDetails() {
        String userId = CacheUtils.getInstance(this).getUserId();

        UserInfoBuilder userInfoBuilder = new UserInfoBuilder()
                .setUserId(userId);


        if (!isEmpty(mMobileNo)) {
            userInfoBuilder.setMobileNumber(mMobileNo);
        }

        if (!isEmpty(mEmail)) {
            userInfoBuilder.setEmail(mEmail);
        }

        if (!isEmpty(mName)) {
            userInfoBuilder.setShortName(mName);
        }

        PhonePe.showAccountDetails(userId, userInfoBuilder.build());
    }

    //*********************************************************************
    // Life cycles
    //*********************************************************************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_new);
        ButterKnife.bind(this);
        setDefaults();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == 100) {
            setDefaults();
        }
    }


    //*********************************************************************
    // Menu related
    //*********************************************************************

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), 100);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //*********************************************************************
    // Private class
    //*********************************************************************

    private void startFlipkart()
    {
        Intent intent = new Intent(MainActivity.this, FlipkartActivity.class);
        startActivity(intent);
    }

    private void setDefaults() {
        mDebitAmountTextView.setText("Rs. " + CacheUtils.getInstance(this).getAmountForTransaction());
        mCreditAmountTextView.setText("Rs. " + CacheUtils.getInstance(this).getAmountForTransaction());

        mMobileNo = CacheUtils.getInstance(this).getMobile();
        mEmail = CacheUtils.getInstance(this).getEmail();
        mName = CacheUtils.getInstance(this).getName();
    }

    private void startLoginRegister(boolean isLogin) {
        String sampleUSerId = UUID.randomUUID().toString().substring(0, 15);
        if (isLogin) {
            sampleUSerId = CacheUtils.getInstance(this).getUserId();
        }

        final String txnId = UUID.randomUUID().toString().substring(0, 15);

        String checksum = CheckSumUtils.getCheckSumForRegister(Constants.MERCHANT_ID, txnId, Constants.SALT, Constants.SALT_KEY_INDEX);

        SignUpRequestBuilder signUpRequestBuilder = new SignUpRequestBuilder()
                .setUserId(sampleUSerId)
                .setTransactionId(txnId)
                .setChecksum(checksum);


        if (!AppUtils.isEmpty(mMobileNo)) {
            signUpRequestBuilder.setMobileNumber(mMobileNo);
        }

        if (!AppUtils.isEmpty(mEmail)) {
            signUpRequestBuilder.setEmail(mEmail);
        }

        if (!AppUtils.isEmpty(mName)) {
            signUpRequestBuilder.setShortName(mName);
        }

        startActivityForResult(PhonePe.getRegisterIntent(this, Constants.MERCHANT_ID, CacheUtils.getInstance(this).getUserId(), signUpRequestBuilder.build(), true), 400);
    }

    private void getWalletBalance() {
/*        resultTextView.setText("Fetching wallet balance ...");
        String userId = CacheUtils.getInstance(this).getUserId();
        String checksum = CheckSumUtils.getCheckSumForNonTransaction(Constants.MERCHANT_ID, userId, Constants.SALT, Constants.SALT_KEY_INDEX);

        PhonePe.fetchDebitSuggestion(checksum, userId, new DataListener<DebitSuggestion>() {
            @Override
            public void onSuccess(DebitSuggestion debitSuggestion) {
                if (debitSuggestion != null) {
                    if (debitSuggestion.getWalletState().equals(WalletState.UNKNOWN)) {
                        resultTextView.setText("Wallet Balance: Unknown");
                    } else {
                        Long amountInRs = debitSuggestion.getAvailableBalanceInWallet() / 100;
                        resultTextView.setText("Wallet Balance:" + amountInRs + "Rs.");
                    }
                }
            }


            @Override
            public void onFailure(ErrorInfo errorInfo) {
                resultTextView.setText("Failed to fetch wallet balance:" + errorInfo.getCode());
            }

        });*/
    }

    private void startDebit() {
        Long amount = CacheUtils.getInstance(this).getAmountForTransaction();
        final String txnId = UUID.randomUUID().toString().substring(0, 35);
        String userId = CacheUtils.getInstance(this).getUserId();

        UserInfoBuilder userInfoBuilder = new UserInfoBuilder()
                .setUserId(userId);


        if (!isEmpty(mMobileNo)) {
            userInfoBuilder.setMobileNumber(mMobileNo);
        }

        if (!isEmpty(mEmail)) {
            userInfoBuilder.setEmail(mEmail);
        }

        if (!isEmpty(mName)) {
            userInfoBuilder.setShortName(mName);
        }


        OrderInfo orderInfo = new OrderInfoBuilder()
                .setOrderId("OD139924923")
                .setMessage("Payment towards order No. OD139924923.")
                .build();

        AccountingInfo accountingInfo = new AccountingInfoBuilder().setSubMerchant("xMerchantId").build();

        PaymentInstrumentsPreference paymentInstrumentsPreference = new PaymentInstrumentPreferenceBuilder()
                .setWalletAllowed(false)
                .setAccountAllowed(true)
                .setCreditCardAllowed(false)
                .setDebitCardAllowed(false)
                .setNetBankingAllowed(false)
                .setInstrumentSplitPreference(InstrumentSplitPreference.MULTI_INSTRUMENT_MODE)
                .build();


        DebitRequest debitRequest = new DebitRequestBuilder()
                .setTransactionId(txnId)
                .setAmount(amount * 100)
                .setPaymentInstrumentsPreference(paymentInstrumentsPreference)
                .setAccountingInfo(accountingInfo)
                .setOrderInfo(orderInfo)
                .setUserInfo(userInfoBuilder.build())
                .build();

        startActivityForResult(PhonePe.getDebitIntent(this, Constants.MERCHANT_ID, debitRequest), 300);
    }

    void startCredit() {
        Long amount = ITEM_AMOUNT;
        final String txnId = UUID.randomUUID().toString().substring(0, 15);
        String userId = CacheUtils.getInstance(this).getUserId();

        CreditType creditType = CreditType.INSTANT;
        /*  if (mCreditTypeOption.isChecked()) {
            creditType = CreditType.DEFERRED;
        }*/

        String checksum = CheckSumUtils.getCheckSumForPayment(Constants.MERCHANT_ID, txnId, amount * 100, Constants.SALT, Constants.SALT_KEY_INDEX);

        UserInfoBuilder userInfoBuilder = new UserInfoBuilder()
                .setUserId(userId);

        if (!isEmpty(mMobileNo)) {
            userInfoBuilder.setMobileNumber(mMobileNo);
        }

        if (!isEmpty(mEmail)) {
            userInfoBuilder.setEmail(mEmail);
        }

        if (!isEmpty(mName)) {
            userInfoBuilder.setShortName(mName);
        }

        OrderInfo orderInfo = new OrderInfoBuilder()
                .setOrderId("OD139924923")
                .setMessage("Payment towards 'Batman : Arkham Origins' (Order No. OD139924923)")
                .build();

        CreditRequest creditRequest = new CreditRequestBuilder()
                .setTransactionId(txnId)
                .setAmount(amount * 100)
                .setOrderInfo(orderInfo)
                .setCreditType(creditType)
                .setUserInfo(userInfoBuilder.build())
                .setChecksum(checksum)
                .build();

        Toast.makeText(MainActivity.this, "Not supported in v1", Toast.LENGTH_SHORT).show();

    /*    PhonePe.initiateCredit(creditRequest, new TransactionCompleteListener() {
            @Override
            public void onTransactionComplete() {
                trackTxnStatus(txnId, false);
            }

            @Override
            public void onTransactionFailed(ErrorInfo errorInfo) {
                trackTxnStatus(txnId, errorInfo.getCode() == ErrorCode.ERROR_CANCELED);
            }
        });*/
    }

    private void trackTxnStatus(final String txnId, final boolean wascanceled) {
        startActivity(ResultActivity.getInstance(this, txnId, wascanceled));
        overridePendingTransition(0, 0);
    }

    //*********************************************************************
    // End of the class
    //*********************************************************************
}
