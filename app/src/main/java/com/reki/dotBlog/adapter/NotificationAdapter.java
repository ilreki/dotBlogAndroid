package com.reki.dotBlog.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.reki.dotBlog.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//消息提醒适配器
public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private JSONArray data;//存放要显示和不显示的数据
    private Context context;
    private int normalType = 0;
    private int footType = 1;
    private boolean hasMore = true;
    private boolean fadeTips = false;//用于判断是否显示页脚提示信息

    public NotificationAdapter(JSONArray data, Context context, boolean hasMore) {
        this.data = data;
        this.context = context;
        this.hasMore = hasMore;
    }

    //不同的类型显示也不同
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == normalType) {
            return new NormalHolder(LayoutInflater.from(context).inflate(R.layout.notification_view, null));
        } else {
            return new FootHolder(LayoutInflater.from(context).inflate(R.layout.footer_view, null));
        }
    }

    //绑定数据到控件上
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof NormalHolder) {
            try{
                //根据position得到对应要显示的数据
                JSONObject jsonObject = (JSONObject) data.get(position);
                Log.i("notification", jsonObject.toString());
                ((NormalHolder) holder).name.setText(jsonObject.getString("notificationName"));
                ((NormalHolder) holder).date.setText(jsonObject.getString("notificationDate"));
                ((NormalHolder) holder).content.setText(jsonObject.getString("notificationContent"));
            } catch (JSONException e){
                Log.i("JSONParseError", e.toString());
            }
        } else {
            //显示页脚
            ((FootHolder) holder).tips.setVisibility(View.VISIBLE);
            //根据是否还有更多数据来显示不同的提示信息
            if (hasMore) {
                fadeTips = false;
                ((FootHolder) holder).tips.setText("正在加载数据...");
            } else {
                if (data.length() > 0) {
                    ((FootHolder) holder).tips.setText("没有更多数据了");
                    ((FootHolder) holder).tips.setVisibility(View.GONE);
                    fadeTips = true;
                    hasMore = true;
                } else{
                    ((FootHolder) holder).tips.setText("没有更多数据了");
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return data.length() + 1;
    }

    public int getRealLastPosition() {
        return data.length();
    }

    //获取新数据后更新view
    public void updateList(JSONArray newData, boolean hasMore) {
        //判断是否有新数据
        if(newData != null){
            if(data != null){
                try{
                    for(int i =0; i < newData.length(); i++){
                        data.put(newData.get(i));
                    }
                } catch (JSONException e){
                    Log.i("JSONParseError", e.toString());
                }

            } else{
                data = newData;
            }
        }
        this.hasMore = hasMore;
        //调用notifyDataSetChanged来更新
        notifyDataSetChanged();
    }

    class NormalHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView content;
        private TextView date;

        public NormalHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.notification_view_name);
            date = itemView.findViewById(R.id.notification_view_date);
            content = itemView.findViewById(R.id.notification_view_content);
        }
    }

    class FootHolder extends RecyclerView.ViewHolder {
        private TextView tips;

        public FootHolder(View itemView) {
            super(itemView);
            tips = itemView.findViewById(R.id.load_more);
        }
    }

    public boolean isFadeTips() {
        return fadeTips;
    }

    public void resetData() {
        data = new JSONArray();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1) {
            return footType;
        } else {
            return normalType;
        }
    }

    //获取特定位置的item对应的数据
    public JSONObject getItemViewData(int position){
        JSONObject itemViewData = null;
        try{
            if(position >= 0 && position < data.length()){
                itemViewData = (JSONObject) data.get(position);
            }
        } catch (JSONException e){
            Log.i("JSONParseError", e.toString());
        }
        return itemViewData;
    }
}
