package com.reki.dotBlog.popupWindow;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.reki.dotBlog.R;

//回复弹窗
public class PopupWindowReply extends PopupWindow {
    private Button buttonDelete, buttonReport, buttonCancel;
    private View menuView;

    public PopupWindowReply(Activity activity, View.OnClickListener itemOnClick){
        super(activity);
        Bundle bundle = activity.getIntent().getExtras();
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        menuView = inflater.inflate(R.layout.popup_window_reply, null);
        buttonDelete = menuView.findViewById(R.id.popup_window_reply_delete);
        buttonReport = menuView.findViewById(R.id.popup_window_reply_report);
        buttonCancel = menuView.findViewById(R.id.popup_window_reply_cancel);

        //根据传递的参数决定是否显示删除按钮
        if(bundle.getBoolean("showDelete")){
            buttonDelete.setVisibility(View.VISIBLE);
        }

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        buttonDelete.setOnClickListener(itemOnClick);
        buttonReport.setOnClickListener(itemOnClick);
        this.setContentView(menuView);
        this.setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        this.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        this.setFocusable(true);
        //设置弹窗动画
        this.setAnimationStyle(R.style.popupWindow);
        //设置点击弹窗外区域，关闭弹窗
        menuView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int height = menuView.findViewById(R.id.popup_window_reply_main).getTop();
                int y = (int) event.getY();
                if(event.getAction() == MotionEvent.ACTION_UP){
                    if(y < height){
                        dismiss();
                    }
                }
                return true;
            }
        });
    }
}
