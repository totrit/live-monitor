package com.totrit.livemonitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class DecorationView extends View {
  private final static String LOG_TAG = "DecorationView";
  private Rect mAdditionalRectToDraw = null;

  public DecorationView(Context context) {
    super(context);
    // TODO Auto-generated constructor stub
  }
  
  
  @Override
  protected void onDraw(Canvas canvas){
    super.onDraw(canvas);
    if (mAdditionalRectToDraw != null) {
      Log.d(LOG_TAG, "motion rect not null, painting it: " + mAdditionalRectToDraw);
      Paint p = new Paint();
      p.setAntiAlias(true);
      p.setColor(Color.RED);
      p.setStyle(Style.STROKE);
      canvas.drawRect(mAdditionalRectToDraw, p);
    }
  }

  public void setAdditionalDrawingRect(Rect rect) {
    mAdditionalRectToDraw = rect;
  }
}
