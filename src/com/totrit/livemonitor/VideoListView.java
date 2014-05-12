package com.totrit.livemonitor;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class VideoListView extends LinearLayout {
  private ArrayList<RowContent> mRowContent = new ArrayList<RowContent>();


  public VideoListView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
  
  public VideoListView(Context context) {
    super(context);
    // TODO Auto-generated constructor stub
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
			
		}

		@Override
		protected void onPostExecute(Integer result) {
			initView();
		}

		private void initListContent() {

		}
		
		private void initView() {
			RowContentAdapter adapter = new RowContentAdapter(getContext(),
					R.layout.video_list_row, mRowContent);

			ListView listView = (ListView) findViewById(R.id.videoListView);
			listView.setAdapter(adapter);
		}
  }
  
  private class RowContent {
    public String mName;
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

        row.setTag(rowView);
      } else {
        rowView = (RowView) row.getTag();
      }

      RowContent rowContent = data.get(position);
      rowView.mTitleView.setText(rowContent.mName);
      rowView.mIconView.setImageBitmap(rowContent.mIcon);;

      return row;
    }

    private class RowView {
      ImageView mIconView;
      TextView mTitleView;
    }
  }

}
