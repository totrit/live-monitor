package com.totrit.livemonitor;

import com.totrit.livemonitor.core.ProcessService;
import com.totrit.livemonitor.util.Controller;
import com.totrit.livemonitor.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e. status bar and
 * navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class MainActivity extends Activity {
  private final static String LOG_TAG = "MainActivity";
  /**
   * Whether or not the system UI should be auto-hidden after {@link #AUTO_HIDE_DELAY_MILLIS}
   * milliseconds.
   */
  private static final boolean AUTO_HIDE = true;

  /**
   * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after user interaction before
   * hiding the system UI.
   */
  private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

  /**
   * If set, will toggle the system UI visibility upon interaction. Otherwise, will show the system
   * UI visibility upon interaction.
   */
  private static final boolean TOGGLE_ON_CLICK = true;

  /**
   * The flags to pass to {@link SystemUiHider#getInstance}.
   */
  private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

  /**
   * The instance of the {@link SystemUiHider} for this activity.
   */
  private SystemUiHider mSystemUiHider;

  /**
   * The main view that the activity contains.
   */
  private MainView mSurfaceView = null;
  
  /**
   * The decoration view that is used to show motion rect, etc.
   */
  private DecorationView mDecorationView = null;

  /**
   * The messenger that connects the activity with the service.
   */
  ServiceMessenger mMessenger = null;
  
  /**
   * Manage the UI controls' state, etc.
   */
  private ControlsGroup mUIControlls = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //TODO don't hide the title, because option menu is triggered through the title.
//    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.activity_main);
    mSurfaceView = (MainView)findViewById(R.id.mainView);
    mDecorationView = (DecorationView)findViewById(R.id.decorationView);
    mUIControlls = new ControlsGroup();
//    setupHiderFeature();
    mMessenger = new ServiceMessenger();
  }
  
  /**
   * Set up option menu.
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.layout.option_menu, menu);
    return true;
  }

  private VideoListView mVideoListView = null;
  
  /**
   * Handling menu click.
   */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {
      case R.id.menu_item_video_list:
        ViewGroup rootView = (ViewGroup) (findViewById(android.R.id.content).getRootView());
        if (mVideoListView == null) {
          LayoutInflater inflater = getLayoutInflater();
          mVideoListView = (VideoListView) inflater.inflate(R.layout.video_list_view, rootView, false);
          rootView.addView(mVideoListView);
        }
        
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mMessenger.sendMessage(Message.obtain(null, ProcessService.MSG_RELEASE_ALL));
    mMessenger.releaseConnection();
  }
  
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
      if ((keyCode == KeyEvent.KEYCODE_BACK)) {
          if (mVideoListView != null) {
            mVideoListView.destroy();
            mVideoListView = null;
            return true;
          }
      }
      return super.onKeyDown(keyCode, event);
  }

  private void setupHiderFeature() {
    final View controlsView = findViewById(R.id.fullscreen_content_controls);

    // Set up an instance of SystemUiHider to control the system UI for
    // this activity.
    // TODO Should replace mSurfaceView with some other low-placed view that
    // can hide with the system menu bar.
    mSystemUiHider = SystemUiHider.getInstance(this, mSurfaceView, HIDER_FLAGS);
    mSystemUiHider.setup();
    mSystemUiHider.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
      // Cached values.
      int mControlsHeight;
      int mShortAnimTime;

      @Override
      @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
      public void onVisibilityChange(boolean visible) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
          // If the ViewPropertyAnimator API is available
          // (Honeycomb MR2 and later), use it to animate the
          // in-layout UI controls at the bottom of the
          // screen.
          if (mControlsHeight == 0) {
            mControlsHeight = controlsView.getHeight();
          }
          if (mShortAnimTime == 0) {
            mShortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
          }
          // TODO: uncomment these when has a custom menu-bar.
          // controlsView
          // .animate()
          // .translationY(visible ? 0 : mControlsHeight)
          // .setDuration(mShortAnimTime);
        } else {
          // If the ViewPropertyAnimator APIs aren't
          // available, simply show or hide the in-layout UI
          // controls.
          controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }

        if (visible && AUTO_HIDE) {
          // Schedule a hide().
          delayedHide(AUTO_HIDE_DELAY_MILLIS);
        }
      }
    });

    // Set up the user interaction to manually show or hide the system UI.
    mSurfaceView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (TOGGLE_ON_CLICK) {
          mSystemUiHider.toggle();
        } else {
          mSystemUiHider.show();
        }
      }
    });
  }

  private class ServiceMessenger {
    private Messenger mMessengerFromActivityToService;
    private Messenger mMessengerFromServiceToActivity;

    private ServiceConnection mServiceConnection = new ServiceConnection() {

      @Override
      public void onServiceConnected(ComponentName name, IBinder service) {
        mMessengerFromActivityToService = new Messenger(service);
        mMessenger.sendMessage(Message.obtain(null, ProcessService.MSG_START_OBSERVE_PREVIEW,
            Camera.CameraInfo.CAMERA_FACING_BACK, 50));
      }

      @Override
      public void onServiceDisconnected(ComponentName name) {
        /** TODO */
      }
    };

    public ServiceMessenger() {
      boolean res = doBindService();
      if (Controller.logEnabled() && !res) {
        Log.d(LOG_TAG, "bind service failed.");
      }
      mMessengerFromServiceToActivity = new Messenger(mPrimeHandler);
    }

    public boolean sendMessage(int what) {
      try {
        Message msg = Message.obtain(null, what);
        msg.replyTo = mMessengerFromServiceToActivity;
        mMessengerFromActivityToService.send(msg);
        return true;
      } catch (RemoteException e) {
        if (Controller.logEnabled()) {
          e.printStackTrace();
        }
        return false;
      }
    }

    public boolean sendMessage(Message msg) {
      msg.replyTo = mMessengerFromServiceToActivity;
      msg.obj = mPrimeHandler;
      try {
        mMessengerFromActivityToService.send(msg);
        return true;
      } catch (RemoteException e) {
        e.printStackTrace();
        return false;
      }
    }

    private boolean doBindService() {
      return bindService(new Intent(MainActivity.this, ProcessService.class), mServiceConnection,
          Context.BIND_AUTO_CREATE);
    }

    public void releaseConnection() {
      unbindService(mServiceConnection);
    }

  }
  
  private class ControlsGroup {
    private Button mBtnRecord = null;
    private boolean mBtnRecordState = false;

    public ControlsGroup() {
      mBtnRecord = (Button) findViewById(R.id.btnRecord);
      mBtnRecord.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          if (!mBtnRecordState) {
            mBtnRecord.setText(R.string.btn_record_on_text);
            mBtnRecord.setTextColor(getResources().getColor(R.color.hint_color_recording));
            mMessenger.sendMessage(Message.obtain(null, ProcessService.MSG_START_RECORD, Camera.CameraInfo.CAMERA_FACING_BACK, 0));
            // Stop from rotating when recording, because the video will be corrupted if resolution changed during recording.
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
            Toast.makeText(MainActivity.this, R.string.tip_no_rotation_when_recording, Toast.LENGTH_SHORT).show();
          } else {
            mBtnRecord.setText(R.string.btn_record_off_text);
            mBtnRecord.setTextColor(getResources().getColor(R.color.hint_color_not_recording));
            mMessenger.sendMessage(Message.obtain(null, ProcessService.MSG_STOP_RECORD));
            // Restore auto rotating when recording finished.
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
          }
          mBtnRecordState = !mBtnRecordState;
        }
      });
    }
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    // Trigger the initial hide() shortly after the activity has been
    // created, to briefly hint to the user that UI controls
    // are available.
    delayedHide(100);
  }

  //
  // @Override
  // protected void onStart() {
  // super.onStart();
  // CameraManager.getInstance().startPreview(mSurfaceView.getHolder());
  // }
  //
  // @Override
  // protected void onPause() {
  // super.onPause();
  // CameraManager.getInstance().releaseCarema();
  // }
  //
  // @Override
  // protected void onResume() {
  // super.onResume();
  // CameraManager.getInstance().startPreview(mSurfaceView.getHolder());
  // }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    mSurfaceView.onConfigurationChanged(newConfig);
  }

  /**
   * Touch listener to use for in-layout UI controls to delay hiding the system UI. This is to
   * prevent the jarring behavior of controls going away while interacting with activity UI.
   */
  View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
      if (AUTO_HIDE) {
        delayedHide(AUTO_HIDE_DELAY_MILLIS);
      }
      return false;
    }
  };

  Handler mPrimeHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case ProcessService.MSG_SHOW_MOTION_RECT:
          Rect rect = (Rect) msg.obj;
          mDecorationView.setAdditionalDrawingRect(rect);
          mDecorationView.invalidate();
          break;
      }
    }
  };

  Handler mHideHandler = new Handler();
  Runnable mHideRunnable = new Runnable() {
    @Override
    public void run() {
      //TODO
//      mSystemUiHider.hide();
    }
  };

  /**
   * Schedules a call to hide() in [delay] milliseconds, canceling any previously scheduled calls.
   */
  private void delayedHide(int delayMillis) {
    mHideHandler.removeCallbacks(mHideRunnable);
    mHideHandler.postDelayed(mHideRunnable, delayMillis);
  }

}
