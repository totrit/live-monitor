package com.totrit.livemonitor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
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
  private RowContent[] mRowContent;


  public VideoListView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initListContent();
    RowContentAdapter adapter =
        new RowContentAdapter(getContext(), R.layout.video_list_row, mRowContent);

    ListView listView = (ListView) findViewById(R.id.videoListView);
    listView.setAdapter(adapter);
  }
  
  public VideoListView(Context context) {
    super(context);
    // TODO Auto-generated constructor stub
  }
  
  private void initListContent() {
    //TODO
  }
  
  private class RowContent {
    public String mName;
    public Bitmap mIcon;
  }
  
  private class RowContentAdapter extends ArrayAdapter<RowContent> {

    Context context;
    int layoutResourceId;
    RowContent data[] = null;

    public RowContentAdapter(Context context, int layoutResourceId, RowContent[] data) {
      super(context, layoutResourceId, data);
      this.layoutResourceId = layoutResourceId;
      this.context = context;
      this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View row = convertView;
      RowContentHolder holder = null;

      if (row == null) {
        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        row = inflater.inflate(layoutResourceId, parent, false);

        holder = new RowContentHolder();
        holder.imgIcon = (ImageView) row.findViewById(R.id.rowContentIcon);
        holder.txtTitle = (TextView) row.findViewById(R.id.rowContentTxtTitle);

        row.setTag(holder);
      } else {
        holder = (RowContentHolder) row.getTag();
      }

      RowContent rowContent = data[position];
      holder.txtTitle.setText(rowContent.mName);
      holder.imgIcon.setImageBitmap(rowContent.mIcon);;

      return row;
    }

    private class RowContentHolder {
      ImageView imgIcon;
      TextView txtTitle;
    }
  }

}
