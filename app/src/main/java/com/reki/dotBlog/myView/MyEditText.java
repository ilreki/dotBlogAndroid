package com.reki.dotBlog.myView;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;

//自定义EditText
public class MyEditText extends AppCompatEditText {
    //弃用
//    public boolean performClicked = false;
//    public boolean onSelectionChangedFirst = true;

    public MyEditText(Context context) {
        super(context);
    }

    public MyEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    //重写onselectionChanged
    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        //弃用
        /*if(!onSelectionChangedFirst){
            if(!styleButtonClicked){
                performClicked = true;
                performClick();
            }
            styleButtonClicked = false;
        }
        onSelectionChangedFirst = !onSelectionChangedFirst;*/

        //触发点击事件
        performClick();
    }
}
