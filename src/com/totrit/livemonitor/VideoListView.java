package com.totrit.livemonitor;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import com.totrit.livemonitor.core.VideoRecorder;
import com.totrit.livemonitor.util.Controller;
import com.totrit.livemonitor.util.Utilities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class VideoListView extends FrameLayout {
  private static final String TAG = "VideoListView";
  private ArrayList<RowContent> mRowContent = new ArrayList<RowContent>();
  private ProgressBar mProgressbar = null;

  public VideoListView(Context context, AttributeSet attrs) {
    super(context, attrs);
    mProgressbar = new ProgressBar(context, null, android.R.attr.progressBarStyleSmall);
    LayoutParams params = new LayoutParams(100, 100);
    params.gravity = Gravity.CENTER;
    mProgressbar.setLayoutParams(params);
    this.addView(mProgressbar);
    mProgressbar.setVisibility(VISIBLE);
  }
  
  public VideoListView(Context context) {
    super(context);
    // TODO Auto-generated constructor stub
  }
  
  public void destroy() {
    ((ViewGroup)this.getRootView()).removeView(this);
  }
  
  @Override
  protected void onFinishInflate() {
	  super.onFinishInflate();
	  new ListInitTask().execute();
  }
  
  /**
   * Asynchronously show the list.
   * @author totrit
   *
   */
  private class ListInitTask extends AsyncTask<Void, Integer, Integer> {

      @Override
      protected Integer doInBackground(Void... params) {
          initListContent();
          return 0;
      }
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
		  mProgressbar.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(Integer result) {
			initView();
		}

		private void initListContent() {
		  String[] exts = {".mp4"};
		  File[] list = Utilities.listFiles(VideoRecorder.getSaveRootDir(), exts, true);
		  for (int i = 0, count = list.length; i < count; i ++) {
	        Bitmap thumb = ThumbnailUtils.createVideoThumbnail(list[i].getPath(),
	              MediaStore.Images.Thumbnails.MICRO_KIND);
		    RowContent item = new RowContent();
		    item.mName = list[i].getName();
		    item.mFullPath = list[i].getPath();
		    item.mIcon = thumb;
		    item.mDate = new Date(list[i].lastModified()).toLocaleString();
		    item.mSize = list[i].length();
		    mRowContent.add(item);
		  }
		}
		
		private void initView() {
			RowContentAdapter adapter = new RowContentAdapter(getContext(),
					R.layout.video_list_row, mRowContent);

			ListView listView = (ListView) findViewById(R.id.videoListView);
			// Set click listner
            listView.setOnItemClickListener(new OnItemClickListener() {
              @Override
              public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (Controller.logEnabled()) {
                  Log.d(TAG, "list item clicked: index = " + position);
                }
                if (position >= mRowContent.size()) {
                  return; // Unexpectedly
                } else {
                  String videoPath = mRowContent.get(position).mFullPath;
//                  play(videoPath);
                  Intent intent = new Intent(Intent.ACTION_VIEW);
                  intent.setDataAndType(Uri.parse("file://" + videoPath), "video/*");
                  getContext().startActivity(intent);
                }
              }
            });
			listView.setAdapter(adapter);
			invalidate();
			mProgressbar.setProgress(100);
			mProgressbar.setVisibility(GONE);
		}
  }
  
  private class RowContent {
    public String mName;
    public String mFullPath;
    public String mDate;
    public long mSize;
    public Bitmap mIcon;
  }
  
  private class RowContentAdapter extends ArrayAdapter<RowContent> {

    Context context;
    int layoutResourceId;
    ArrayList<RowContent> data = null;

    public RowContentAdapter(Context context, int layoutResourceId, ArrayList<RowContent> data) {
      super(context, layoutResourceId, data);
      this.layoutResourceId = layoutResourceId;
      this.context = context;
      this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View row = convertView;
      RowView rowView = null;

      if (row == null) {
    	  LayoutInflater inflater = (LayoutInflater)context.getSystemService
    		      (Context.LAYOUT_INFLATER_SERVICE);
        row = inflater.inflate(layoutResourceId, parent, false);

        rowView = new RowView();
        rowView.mIconView = (ImageView) row.findViewById(R.id.rowContentIcon);
        rowView.mTitleView = (TextView) row.findViewById(R.id.rowContentTxtTitle);
        rowView.mDate = (TextView) row.findViewById(R.id.rowContentDate);
        rowView.mSize = (TextView) row.findViewById(R.id.rowContentSize);

        row.setTag(rowView);
      } else {
        rowView = (RowView) row.getTag();
      }

      RowContent rowContent = data.get(position);
      rowView.mIconView.setImageBitmap(rowContent.mIcon);
      rowView.mTitleView.setText(rowContent.mName);
      rowView.mDate.setText(rowContent.mDate);
      rowView.mSize.setText("" + Utilities.getBestExpreOfFileSize(rowContent.mSize));

      return row;
    }

    private class RowView {
      ImageView mIconView;
      TextView mTitleView;
      TextView mDate;
      TextView mSize;
    }
  }

}
