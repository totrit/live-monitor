package com.totrit.livemonitor.core;

import com.totrit.livemonitor.util.Controller;

import android.app.Service;
import android.content.Intent;
import android.graphics.Path;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

public class ProcessService extends Service {

  private static final String LOG_TAG = "ProcessService";

  /**
   * Messages that sent from activity.
   */
  private final static int MSG_BASE_ACTIVITY = 0;
  public final static int MSG_START_OBSERVE_PREVIEW = MSG_BASE_ACTIVITY + 1;
  public final static int MSG_START_DETECT_AND_RECORD = MSG_BASE_ACTIVITY + 2;
  public final static int MSG_STOP_ALL = MSG_BASE_ACTIVITY + 3;

  /**
   * Messages that sent from other parts of the core.
   */
  private final static int MSG_BASE_CORE = 100;
  public final static int MSG_MOTION_DETECTED = MSG_BASE_CORE + 1;

  private Handler mHandler = null;
  private Messenger mMessengerFromActivityToService = null;
  private Messenger mMessengerFromServiceToActivity = null;
  private Phase mPhase = Phase.PHASE_NONE;
  private MotionDetector mMotionDetector = null;

  private enum Phase {
    PHASE_NONE, PHASE_PREVIEWING, PHASE_DETECTING, PHASE_RECORDING
  }

  @Override
  public IBinder onBind(Intent intent) {
    // TODO Auto-generated method stub
    init();
    return mMessengerFromActivityToService.getBinder();
  }

  private void init() {
    if (mHandler != null) {
      return;
    }
    HandlerThread handlerThread = new HandlerThread("ServiceHandler");
    handlerThread.start();
    mHandler = new Handler(handlerThread.getLooper()) {
      @Override
      public void handleMessage(Message msg) {
        switch (msg.what) {
          case MSG_START_OBSERVE_PREVIEW:
            mPhase = Phase.PHASE_PREVIEWING;
            mMessengerFromServiceToActivity = (Messenger) msg.replyTo;
            mMotionDetector =
                new MotionDetector(msg.arg1, msg.arg2, mMessengerFromActivityToService);
            break;

          case MSG_START_DETECT_AND_RECORD:
            // TODO
            mMessengerFromServiceToActivity = (Messenger) msg.replyTo;
            break;

          case MSG_STOP_ALL:
            if (mMotionDetector != null) {
              mMotionDetector.destroy();
              mMotionDetector = null;
            }
            mHandler = null;
            mMessengerFromServiceToActivity = null;
            mMessengerFromActivityToService = null;
            break;

          case MSG_MOTION_DETECTED:
            Path path = (Path) msg.obj;
            if (Controller.logEnabled()) {
              Log.d(LOG_TAG, "motion detected, path:" + path);
            }
            motionCallback(path);
            break;

        }
      }
    };
    mMessengerFromActivityToService = new Messenger(mHandler);
  }

  public void motionCallback(Path path) {

  }

}
