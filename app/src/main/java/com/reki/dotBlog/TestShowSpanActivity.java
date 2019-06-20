package com.reki.dotBlog;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.util.Log;
import android.widget.TextView;

import com.reki.dotBlog.util.CallBackUtil;
import com.reki.dotBlog.util.OkhttpUtil;
import com.reki.dotBlog.util.RichTextUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

import okhttp3.Call;

public class TestShowSpanActivity extends AppCompatActivity {

    public TextView showResult;
    Bundle fromTestSpanActivity;
    MyHandler myHandler;

    private static class MyHandler extends Handler {
        private final WeakReference<TestShowSpanActivity> mActivity;
        private final int SHOW_RESULT = 1;

        private MyHandler(TestShowSpanActivity activity) {
            mActivity = new WeakReference<TestShowSpanActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            TestShowSpanActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what){
                    case SHOW_RESULT:
                        try{
                            SpannableStringBuilder result = new SpannableStringBuilder(msg.getData().getCharSequence("result"));
                            activity.showResult.setText(result);
                            activity.showResult.setMovementMethod(LinkMovementMethod.getInstance());
                            JSONArray jsonArray = new JSONArray(msg.getData().getString("imageArray"));
                            for(int i = 0; i < jsonArray.length(); i++){
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String param = jsonObject.getString("param");
                                int start = jsonObject.getInt("start");
                                int end = jsonObject.getInt("end");

                                activity.setNetImage(param, start, end);
                            }
                        } catch (JSONException e){
                            Log.i("JSONParseError", e.toString());
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_show_span);

        init();
    }

    protected void init(){
        showResult = findViewById(R.id.showResult);
        fromTestSpanActivity = this.getIntent().getExtras();
        myHandler = new MyHandler(TestShowSpanActivity.this);
        String contentText = fromTestSpanActivity.getString("data");

        myHandler.post(new Runnable() {
            @Override
            public void run() {
                try{
                    JSONArray jsonArray = new JSONArray(contentText);
                    JSONObject resultObject =
                            RichTextUtil.fromJson(TestShowSpanActivity.this, jsonArray);
                    Message msg = new Message();
                    msg.what = myHandler.SHOW_RESULT;
                    Bundle bundle = new Bundle();
                    bundle.putCharSequence("result", (CharSequence) resultObject.get("result"));
                    bundle.putString("imageArray", resultObject.get("realImageArray").toString());
                    msg.setData(bundle);
                    myHandler.sendMessage(msg);
                } catch (JSONException e){
                    Log.i("JSONParseError", e.toString());
                }
            }
        });
    }

    private void setNetImage(String netImagePath, int start, int end) {
        OkhttpUtil.okHttpGetBitmap(netImagePath, new CallBackUtil.CallBackBitmap(1080, 1) {
            @Override
            public void onFailure(Call call, Exception e) {
                Log.i("NetworkError", e.toString());
            }

            @Override
            public void onResponse(Bitmap response) {
                ImageSpan imageSpan = new ImageSpan(TestShowSpanActivity.this, response);
                if(showResult.getText().toString().isEmpty()){
                    Log.i("textView", "no content");
                } else{
                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(showResult.getText());
                    spannableStringBuilder.setSpan(imageSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    showResult.setText(spannableStringBuilder);
                }
            }
        });
    }
}