package com.tinklabs.yhh.simpinsettings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

// implement based on http://www.aichengxu.com/wangluo/24603478.htm
public class MainActivity extends AppCompatActivity implements CustomPinDialogFragment.Callback {

    private static final Object NULL_OBJECT = new Object();

    private static final String TAG = "QQQQ";

    private static final String DEFAULT_PIN = "1234";

    private Object mPhone;
    private Object mIccCard;

    private Button setSim;
    private Button clearLog;
    private TextView logView;

    private String customPin = null;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            log("receive callback: " + msg.what + ", obj: " + msg.obj);

            // retrieve android.os.AsyncResult
            try {
                Field fieldUserObj = msg.obj.getClass().getDeclaredField("userObj");
                Object userObj = fieldUserObj.get(msg.obj);

                log("userObj: " + userObj);

                Field fieldException = msg.obj.getClass().getDeclaredField("exception");
                Throwable exception = (Throwable) fieldException.get(msg.obj);

                if (exception != null) {
                    log("exception: " + exception.toString());
                }

                Field fieldResult = msg.obj.getClass().getDeclaredField("result");
                Object result = fieldResult.get(msg.obj);

                log("result: " + result);
            } catch (Exception e) {
                log(e.getMessage());
            }
        }
    };

    private TelephonyManager telephonyManager;

    private StringBuilder logStringBuilder = new StringBuilder();

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        init();
    }

    private void initView() {
        setSim = findViewById(R.id.set_sim);
        setSim.setOnClickListener(view -> startSetPin());
        clearLog = findViewById(R.id.clear_log);
        clearLog.setOnClickListener(view -> clearLog());
        logView = findViewById(R.id.log);
    }

    private void clearLog() {
        logView.setText(null);
        logStringBuilder.setLength(0);
    }

    private void startSetPin() {
        changeIccLockPassword("", customPin == null ? DEFAULT_PIN : customPin);
    }

    private void init() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        compositeDisposable.add(initPhone().subscribe(iccCard -> {
            if (iccCard != NULL_OBJECT) {
                mIccCard = iccCard;
                setSim.setEnabled(true);
                log("load iccCard succeed");
                compositeDisposable.add(checkTelephoneState().subscribe(simState -> {
                    String stateString;
                    switch (simState) {
                        case TelephonyManager.SIM_STATE_ABSENT:
                            stateString = "SIM_STATE_ABSENT";
                            break;
                        case TelephonyManager.SIM_STATE_CARD_IO_ERROR:
                            stateString = "SIM_STATE_CARD_IO_ERROR";
                            break;
                        case TelephonyManager.SIM_STATE_CARD_RESTRICTED:
                            stateString = "SIM_STATE_CARD_RESTRICTED";
                            break;
                        case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                            stateString = "SIM_STATE_NETWORK_LOCKED";
                            break;
                        case TelephonyManager.SIM_STATE_NOT_READY:
                            stateString = "SIM_STATE_NOT_READY";
                            break;
                        case TelephonyManager.SIM_STATE_PERM_DISABLED:
                            stateString = "SIM_STATE_PERM_DISABLED";
                            break;
                        case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                            stateString = "SIM_STATE_PIN_REQUIRED";
                            break;
                        case TelephonyManager.SIM_STATE_READY:
                            stateString = "SIM_STATE_READY";
                            break;
                        case TelephonyManager.SIM_STATE_UNKNOWN:
                            stateString = "SIM_STATE_UNKNOWN";
                            break;
                        default:
                            stateString = "Failed to retrieve sim state: " + simState;
                    }
                    log("sim state: " + stateString);
                }));
            } else {
                log("load iccCard failed");
            }
        }));
    }

    private Single<Integer> checkTelephoneState() {
        return Single.create((SingleOnSubscribe<Integer>) e ->
                e.onSuccess(telephonyManager.getSimState())).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io());
    }

    private Single<Object> initPhone() {
        return Single.create(e -> e.onSuccess(loadPhoneObject() == null ? NULL_OBJECT : loadPhoneObject()))
                .flatMap(phone -> {
                    if (phone != NULL_OBJECT) {
                        mPhone = phone;
                        log("load phone succeed");
                        return Single.create(e -> e.onSuccess(loadIccCardObject(phone) == null ? NULL_OBJECT : loadIccCardObject(phone)));
                    } else {
                        log("load phone failed");
                        return Single.just(NULL_OBJECT);
                    }
                }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io());
    }

    private void log(String log) {
        Log.v(TAG, log);
        logStringBuilder.append(log).append("\n");
        logView.setText(logStringBuilder.toString());
    }

    public void changeIccLockPassword(String oldPassWord, String newPassword) {
        try {
            Method changeIccLockPassword = mIccCard.getClass().getMethod("changeIccLockPassword", new Class[]{String.class, String.class, Message.class});
            changeIccLockPassword.invoke(mIccCard, new Object[]{oldPassWord, newPassword, handler.obtainMessage(1000)});
            log("#### change pin success, old pin: " + oldPassWord + ", new pin: " + newPassword + " ####");
        } catch (Exception e) {
            log(e.toString());
            log("#### change pin fail ####");
        }
    }

    private Object loadPhoneObject() {
        try {
            Class<?> forName = Class.forName("com.android.internal.telephony.PhoneFactory");
            Method getDefaultPhone = forName.getMethod("getDefaultPhone", new Class[]{});
            return getDefaultPhone.invoke(null, new Object[]{});
        } catch (Exception e) {
            log(e.toString());
        }
        return null;
    }

    private Object loadIccCardObject(Object phone) {
        try {
            Method getIccCard = phone.getClass().getMethod("getIccCard", new Class[]{});
            return getIccCard.invoke(phone, new Object[]{});
        } catch (Exception e) {
            log(e.toString());
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_change_pin) {
            CustomPinDialogFragment dialogFragment = new CustomPinDialogFragment();
            dialogFragment.show(getFragmentManager(), "CustomPinDialogFragment");
            return true;
        } else if (id == R.id.menu_clear_pin) {
            customPin = null;
            log("clear custom pin");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(String pin) {
        if (reasonablePin(pin)) {
            customPin = pin;
            log("set custom pin to: " + pin);
        } else {
            log("invalid pin: " + pin);
        }
    }

    private boolean reasonablePin(String pin) {
        return !(pin == null || pin.length() < 4 || pin.length() > 8);
    }
}
