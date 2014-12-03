package com.totrit.livemonitor;

import com.totrit.livemonitor.core.ProcessService;
import com.totrit.livemonitor.util.Controller;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
  private final static String LOG_TAG = "MainActivity";

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
  @SuppressWarnings("unused")
  private ControlsGroup mUIControlls = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mSurfaceView = (MainView)findViewById(R.id.mainView);
    mDecorationView = (DecorationView)findViewById(R.id.decorationView);
    mUIControlls = new ControlsGroup();
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

  /**
   * Handling menu click.
   */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {
      case R.id.menu_item_video_list:
        Intent newIntent = new Intent(this, VideoListActivity.class);
        this.startActivity(newIntent);
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

    @SuppressWarnings("unused")
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
            setRotationEnabled(false);
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            Toast.makeText(MainActivity.this, R.string.tip_no_rotation_when_recording, Toast.LENGTH_SHORT).show();
          } else {
            mBtnRecord.setText(R.string.btn_record_off_text);
            mBtnRecord.setTextColor(getResources().getColor(R.color.hint_color_not_recording));
            mMessenger.sendMessage(Message.obtain(null, ProcessService.MSG_STOP_RECORD));
            // Restore auto rotating when recording finished.
            setRotationEnabled(true);
          }
          mBtnRecordState = !mBtnRecordState;
        }
      });
    }
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
  
  private void setRotationEnabled(boolean enabled) {
    if (enabled) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
      return;
    }
    int rotation = getWindowManager().getDefaultDisplay().getRotation();

    switch(rotation) {
    case Surface.ROTATION_180:
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        break;
    case Surface.ROTATION_270:
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);         
        break;
    case  Surface.ROTATION_0:
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        break;
    case Surface.ROTATION_90:
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        break;
    }
  }

  @SuppressLint("HandlerLeak")
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

}
