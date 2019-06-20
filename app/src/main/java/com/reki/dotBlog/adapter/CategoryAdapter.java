package com.reki.dotBlog.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.reki.dotBlog.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//分类适配器
public class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private JSONArray data;//存放要显示和不显示的数据
    private Context context;
    private int normalType = 0;

    public CategoryAdapter(JSONArray data, Context context) {
        this.data = data;
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == normalType) {
            return new CategoryAdapter.NormalHolder(LayoutInflater.from(context).inflate(R.layout.category_view, null));
        } else{
            return null;
        }
    }

    //绑定数据到控件上
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CategoryAdapter.NormalHolder) {
            try{
                //根据position得到对应要显示的数据
                JSONObject jsonObject = (JSONObject) data.get(position);
                ((NormalHolder) holder).categoryIcon.setImageResource(jsonObject.getInt("categoryIcon"));
                ((NormalHolder) holder).categoryName.setText(jsonObject.getString("categoryName"));
            } catch (JSONException e){
                Log.i("JSONParseError", e.toString());
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

    class NormalHolder extends RecyclerView.ViewHolder {
        private ImageView categoryIcon;
        private TextView categoryName;

        public NormalHolder(View itemView) {
            super(itemView);
            categoryIcon = itemView.findViewById(R.id.category_view_icon);
            categoryName = itemView.findViewById(R.id.category_view_name);
        }
    }

    public void resetData() {
        data = new JSONArray();
    }

    //获取特定位置的item对应的数据
    @Override
    public int getItemViewType(int position) {
        return normalType;
    }

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
