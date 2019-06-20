package com.reki.dotBlog.listener;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.reki.dotBlog.adapter.NotificationAdapter;

import org.json.JSONObject;

//消息提示item点击监听器
public class NotificationRecyclerViewOnClickListener implements RecyclerView.OnItemTouchListener {
    private GestureDetector gestureDetector;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener{
        void OnItemClick(View v, JSONObject jsonObject);
    }

    public NotificationRecyclerViewOnClickListener(Context context, final RecyclerView recyclerView, OnItemClickListener onItemClickListener){
        this.onItemClickListener = onItemClickListener;
        //通过gestureDetector来处理点击事件
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){
            //单次点击
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                View childView = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if(childView != null){
                    int position = recyclerView.getChildLayoutPosition(childView);
                    NotificationAdapter adapter = (NotificationAdapter) recyclerView.getAdapter();
                    JSONObject jsonObject = adapter.getItemViewData(position);
                    if(onItemClickListener != null && jsonObject != null){
                        onItemClickListener.OnItemClick(childView, jsonObject);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    //拦截触摸事件，并交由gestureDetector处理
    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent motionEvent) {
        if(gestureDetector.onTouchEvent(motionEvent)){
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent motionEvent) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean b) {

    }
}
