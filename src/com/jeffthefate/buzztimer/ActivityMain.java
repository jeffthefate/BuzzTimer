package com.jeffthefate.buzztimer;

import net.simonvt.widget.NumberPicker;
import net.simonvt.widget.NumberPicker.Formatter;
import net.simonvt.widget.NumberPicker.OnValueChangeListener;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jeffthefate.buzztimer.TimerService.TimerBinder;
import com.jeffthefate.buzztimer.TimerService.UiCallback;

public class ActivityMain extends FragmentActivity implements UiCallback {
    
    private static TextView minText;
    private NumberPicker minPicker;
    private static TextView secText;
    private static TextView msecText;
    private NumberPicker secPicker;
    private RelativeLayout timeLayout;
    private TextView colonText;
    
    private CheckBox loopCheck;
    private Button doneButton;
    
    private SharedPreferences sharedPrefs;
    
    private boolean bound;
    
    private int newMin = 1;
    private int newSec = 0;
    
    private TranslateAnimation screenSaver;
    
    private UiCallback uiCallback = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        minText = (TextView) findViewById(R.id.MinuteText);
        minPicker = (NumberPicker) findViewById(R.id.MinutePicker);
        minPicker.setFormatter(new TimerFormatter());
        minPicker.setMaxValue(59);
        minPicker.setMinValue(0);
        minPicker.setOnValueChangedListener(new OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal,
                    int newVal) {
                newMin = newVal;
                setTime(newMin, newSec);
            }
        });
        secText = (TextView) findViewById(R.id.SecondText);
        secPicker = (NumberPicker) findViewById(R.id.SecondPicker);
        secPicker.setFormatter(new TimerFormatter());
        secPicker.setMaxValue(59);
        secPicker.setMinValue(0);
        secPicker.setOnValueChangedListener(new OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal,
                    int newVal) {
                newSec = newVal;
                if (oldVal == 0 && newVal == 59)
                	minPicker.setValue(--newMin);
                else if (oldVal == 59 && newVal == 0)
                	minPicker.setValue(++newMin);
                newMin = minPicker.getValue();
                setTime(newMin, newSec);
            }
        });
        msecText = (TextView) findViewById(R.id.MillisecondText);
        timeLayout = (RelativeLayout) findViewById(R.id.TimeLayout);
        timeLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ApplicationEx.getMsecs() < 5000) {
                    ApplicationEx.mToast.setText("Must be at least 5 seconds");
                    ApplicationEx.mToast.show();
                }
                else {
                    if (mService != null) {
                        updateTime(ApplicationEx.getMsecs());
                        if (!mService.isTimerRunning()) {
                            mService.setTime(loopCheck.isChecked());
                            mService.startTimer();
                        }
                        else {
                            mService.stopTimer();
                            mService.setTime(loopCheck.isChecked());
                        }
                    }
                }
            }
        });
        timeLayout.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (minPicker.getVisibility() == View.GONE &&
                        secPicker.getVisibility() == View.GONE) {
                    colonText.setVisibility(View.INVISIBLE);
                    minPicker.setVisibility(View.VISIBLE);
                    secPicker.setVisibility(View.VISIBLE);
                    loopCheck.setVisibility(View.VISIBLE);
                    doneButton.setVisibility(View.VISIBLE);
                    minText.setVisibility(View.INVISIBLE);
                    secText.setVisibility(View.INVISIBLE);
                    msecText.setVisibility(View.INVISIBLE);
                    newMin = minPicker.getValue();
                    newSec = secPicker.getValue();
                }
                return true;
            }
        });
        colonText = (TextView) findViewById(R.id.ColonText);
        loopCheck = (CheckBox) findViewById(R.id.LoopCheck);
        loopCheck.setVisibility(View.INVISIBLE);
        loopCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                sharedPrefs.edit().putBoolean(getString(R.string.loop_key),
                        isChecked).commit();
            }
        });
        doneButton = (Button) findViewById(R.id.DoneButton);
        doneButton.setVisibility(View.INVISIBLE);
        doneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (newMin < 1 && newSec < 5) {
                    ApplicationEx.mToast.setText("Must be at least 5 seconds");
                    ApplicationEx.mToast.show();
                }
                else if (newMin == 60 && newSec > 0) {
                    ApplicationEx.mToast.setText("Must be at most 60 minutes");
                    ApplicationEx.mToast.show();
                }
                else {
                	sharedPrefs.edit().putInt(getString(R.string.msec_key),
                            ApplicationEx.getMsecs()).commit();
                    setTime(newMin, newSec);
                    if (mService != null) {
                        if (mService.isTimerRunning())
                            mService.stopTimer();
                        mService.setTime(loopCheck.isChecked());
                    }
                    minText.setVisibility(View.VISIBLE);
                    secText.setVisibility(View.VISIBLE);
                    msecText.setVisibility(View.VISIBLE);
                    minPicker.setVisibility(View.GONE);
                    secPicker.setVisibility(View.GONE);
                    loopCheck.setVisibility(View.INVISIBLE);
                    doneButton.setVisibility(View.INVISIBLE);
                    colonText.setVisibility(View.VISIBLE);
                }
            }
        });
        if (savedInstanceState != null) {
            minText.setText(savedInstanceState.getString("minText"));
            secText.setText(savedInstanceState.getString("secText"));
            msecText.setText(savedInstanceState.getString("msecText"));
            newMin = savedInstanceState.getInt("newMin");
            newSec = savedInstanceState.getInt("newSec");
            colonText.setVisibility(savedInstanceState.getInt("colonTextVis"));
            minPicker.setVisibility(savedInstanceState.getInt("minPickerVis"));
            secPicker.setVisibility(savedInstanceState.getInt("secPickerVis"));
            loopCheck.setVisibility(savedInstanceState.getInt("loopCheckVis"));
            doneButton.setVisibility(
                    savedInstanceState.getInt("doneButtonVis"));
            minText.setVisibility(savedInstanceState.getInt("minTextVis"));
            secText.setVisibility(savedInstanceState.getInt("secTextVis"));
            msecText.setVisibility(savedInstanceState.getInt("msecTextVis"));
        }
        else
            updateTime(ApplicationEx.getMsecs());
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(
                ApplicationEx.getApp());
        minPicker.setValue(newMin);
        secPicker.setValue(newSec);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("minText", minText.getText().toString());
        outState.putString("secText", secText.getText().toString());
        outState.putString("msecText", msecText.getText().toString());
        outState.putInt("newMin", newMin);
        outState.putInt("newSec", newSec);
        outState.putInt("colonTextVis", colonText.getVisibility());
        outState.putInt("minPickerVis", minPicker.getVisibility());
        outState.putInt("secPickerVis", secPicker.getVisibility());
        outState.putInt("loopCheckVis", loopCheck.getVisibility());
        outState.putInt("doneButtonVis", doneButton.getVisibility());
        outState.putInt("minTextVis", minText.getVisibility());
        outState.putInt("secTextVis", secText.getVisibility());
        outState.putInt("msecTextVis", msecText.getVisibility());
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (mService == null) {
            Intent timerIntent = new Intent(getApplicationContext(),
                    TimerService.class);
            getApplicationContext().startService(timerIntent);
        }
        bindService(new Intent(getApplicationContext(), TimerService.class),
                mConnection, 0);
        ApplicationEx.setActive();
    }
    
    @Override
    public void onPause() {
        if (bound) {
            unbindService(mConnection);
            bound = false;
        }
        ApplicationEx.setInactive();
        super.onPause();
    }
    
    private static TimerService mService;
    
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TimerBinder binder = (TimerBinder) service;
            mService = binder.getService();
            mService.setUiCallback(uiCallback);
            bound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };
    
    @Override
    public void updateTime(int mSecs) {
        int mins = (int)((mSecs-(mSecs%60000))/60000);
        int secs = (int)((mSecs%60000)/1000);
        int millis = (int)((mSecs%1000)/100);
        minText.setText(mins < 10 ? "0" + mins : Integer.toString(mins));
        secText.setText(secs < 10 ? "0" + secs : Integer.toString(secs));
        msecText.setText(Integer.toString(millis));
    }
    
    private void setTime(int minutes, int seconds) {
    	Log.i(Constants.LOG_TAG, "setTime minutes: " + minutes);
    	Log.i(Constants.LOG_TAG, "setTime seconds: " + seconds);
        ApplicationEx.setMsecs((minutes*60000)+(seconds*1000));
        updateTime((minutes*60000)+(seconds*1000));
    }
    
    private class TimerFormatter implements Formatter {
        @Override
        public String format(int value) {
            if (value < 10)
                return "0" + value;
            else
                return Integer.toString(value);
        }
    }

}
