package com.phonepe.merchantsdk.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.phonepe.android.sdk.api.PhonePe;
import com.phonepe.android.sdk.api.TransactionCompleteListener;
import com.phonepe.android.sdk.domain.DataListenerContract;
import com.phonepe.android.sdk.domain.builders.CreditRequestBuilder;
import com.phonepe.android.sdk.domain.builders.DebitRequestBuilder;
import com.phonepe.android.sdk.domain.builders.OrderInfoBuilder;
import com.phonepe.android.sdk.domain.builders.SignUpRequestBuilder;
import com.phonepe.android.sdk.domain.builders.UserInfoBuilder;
import com.phonepe.android.sdk.models.ErrorInfo;
import com.phonepe.android.sdk.models.api.CreditRequest;
import com.phonepe.android.sdk.models.api.DebitRequest;
import com.phonepe.android.sdk.models.api.OrderInfo;
import com.phonepe.android.sdk.models.api.SignUpRequest;
import com.phonepe.android.sdk.models.api.UserInfo;
import com.phonepe.android.sdk.models.enums.CreditType;
import com.phonepe.android.sdk.models.enums.PayInstrumentOption;
import com.phonepe.android.sdk.models.enums.WalletState;
import com.phonepe.android.sdk.models.networking.response.DebitSuggestion;
import com.phonepe.android.sdk.models.networking.response.TransactionStatus;
import com.phonepe.android.sdk.utils.CheckSumUtils;
import com.phonepe.merchantsdk.demo.utils.CacheUtils;
import com.phonepe.merchantsdk.demo.utils.Constants;

import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Bind(R.id.id_debit_amount)
    TextView mDebitAmountTextView;

    @Bind(R.id.id_credit_amount)
    TextView mCreditAmountTextView;

    @Bind(R.id.id_result)
    TextView resultTextView;

    @Bind(R.id.id_credit_type)
    SwitchCompat mCreditTypeOption;

    @OnClick(R.id.id_install)
    void installPhonePe() {
        PhonePe.installPhone(this);
    }

    @OnClick(R.id.id_register)
    void showRegisterDemo() {
        startRegister();
    }

    @OnClick(R.id.id_debit)
    void showDebitDemo() {
        startDebit();
    }

    @OnClick(R.id.id_credit)
    void showCreditDemo() {
        startCredit();
    }

    @OnClick(R.id.id_account)
    void showAccountDetails() {
        String userId = CacheUtils.getInstance(this).getUserId();
        String checksum = CheckSumUtils.getCheckSumForNonTransaction(Constants.MERCHANT_ID, userId, Constants.SALT, Constants.SALT_KEY_INDEX);
        PhonePe.showAccountDetails(userId, checksum);
    }

    private String mMobileNo;
    private String mEmail;
    private String mName;

    //*********************************************************************
    // Life cycles
    //*********************************************************************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_new);
        ButterKnife.bind(this);
        setDefaults();
        getWalletBalance();
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

    private void setDefaults() {
        mDebitAmountTextView.setText("Rs. " + CacheUtils.getInstance(this).getAmountForTransaction());
        mCreditAmountTextView.setText("Rs. " + CacheUtils.getInstance(this).getAmountForTransaction());

        mMobileNo = CacheUtils.getInstance(this).getMobile();
        mEmail = CacheUtils.getInstance(this).getEmail();
        mName = CacheUtils.getInstance(this).getName();
    }

    private void startRegister() {
        String sampleUSerId = UUID.randomUUID().toString().substring(0, 15);
        final String txnId = UUID.randomUUID().toString().substring(0, 15);

        String checksum = CheckSumUtils.getCheckSumForRegister(Constants.MERCHANT_ID, txnId, Constants.SALT, Constants.SALT_KEY_INDEX);

        SignUpRequestBuilder signUpRequestBuilder = new SignUpRequestBuilder()
                .setUserId(sampleUSerId)
                .setTransactionId(txnId)
                .setChecksum(checksum);


        if (!isEmpty(mMobileNo)) {
            signUpRequestBuilder.setMobileNumber(mMobileNo);
        }

        if (!isEmpty(mEmail)) {
            signUpRequestBuilder.setEmail(mEmail);
        }

        if (!isEmpty(mName)) {
            signUpRequestBuilder.setShortName(mName);
        }

        PhonePe.initiateRegister(signUpRequestBuilder.build(), new TransactionCompleteListener() {
            @Override
            public void onTransactionComplete() {
                trackTxnStatus(txnId, false);
            }

            @Override
            public void onTransactionCanceled() {
                trackTxnStatus(txnId, true);
            }
        });
    }

    private void getWalletBalance() {
        resultTextView.setText("Fetching wallet balance ...");
        String userId = CacheUtils.getInstance(this).getUserId();
        String checksum = CheckSumUtils.getCheckSumForNonTransaction(Constants.MERCHANT_ID, userId, Constants.SALT, Constants.SALT_KEY_INDEX);

        PhonePe.fetchDebitSuggestion(checksum, userId, new DataListenerContract<DebitSuggestion>() {
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
            public void onFailure(ErrorInfo error) {
                resultTextView.setText("Failed to fetch wallet balance ...");
            }
        });
    }

    private void startDebit() {
        Long amount = CacheUtils.getInstance(this).getAmountForTransaction();
        PayInstrumentOption instrumentOption = PayInstrumentOption.ANY;
        final String txnId = UUID.randomUUID().toString().substring(0, 15);
        String userId = CacheUtils.getInstance(this).getUserId();
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
                .setOrderId("someOrderId")
                .setMessage("Pay me for this order.")
                .build();

        DebitRequest debitRequest = new DebitRequestBuilder()
                .setTransactionId(txnId)
                .setAmount(amount * 100)
                .setPaymentInstrumentOption(instrumentOption)
                .setOrderInfo(orderInfo)
                .setUserInfo(userInfoBuilder.build())
                .setChecksum(checksum)
                .build();

        PhonePe.initiateDebit(debitRequest, new TransactionCompleteListener() {
            @Override
            public void onTransactionComplete() {
                trackTxnStatus(txnId, false);
            }

            @Override
            public void onTransactionCanceled() {
                trackTxnStatus(txnId, true);
            }
        });
    }

    void startCredit() {
        Long amount = CacheUtils.getInstance(this).getAmountForTransaction();
        final String txnId = UUID.randomUUID().toString().substring(0, 15);
        String userId = CacheUtils.getInstance(this).getUserId();
        CreditType creditType = CreditType.INSTANT;
        if (mCreditTypeOption.isChecked()) {
            creditType = CreditType.DEFERRED;
        }

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
                .setOrderId("someOrderId")
                .setMessage("Pay me for this order.")
                .build();

        CreditRequest creditRequest = new CreditRequestBuilder()
                .setTransactionId(txnId)
                .setAmount(amount * 100)
                .setOrderInfo(orderInfo)
                .setCreditType(creditType)
                .setUserInfo(userInfoBuilder.build())
                .setChecksum(checksum)
                .build();

        PhonePe.initiateCredit(creditRequest, new TransactionCompleteListener() {
            @Override
            public void onTransactionComplete() {
                trackTxnStatus(txnId, false);
            }

            @Override
            public void onTransactionCanceled() {
                Log.v("test", "onTransactionCanceled called");
                trackTxnStatus(txnId, true);
            }
        });
    }

    private void trackTxnStatus(String txnId, boolean wascanceled) {
        startActivity(ResultActivity.getInstance(this, txnId, wascanceled));
    }

    protected boolean isEmpty(String string) {
        if (string == null || string.trim().equals("")) {
            return true;
        }
        return false;
    }

    //*********************************************************************
    // End of the class
    //*********************************************************************
}
