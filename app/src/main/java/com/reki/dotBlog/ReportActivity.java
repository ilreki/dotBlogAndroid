package com.reki.dotBlog;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.reki.dotBlog.util.CallBackUtil;
import com.reki.dotBlog.util.OkhttpUtil;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;

//举报
public class ReportActivity extends AppCompatActivity {

    private RadioGroup radioGroupReportReasons;
    private EditText reportReasonInput;
    private ImageButton buttonClose, buttonSend;
    private TextView preview1, preview2;
    private Long reportContentID;
    private String reportChosenReason, type;

//    private String baseUrl = "http://192.168.1.104:8080/dotBlog/";
    private String baseUrl = "http://reki.vipgz1.idcfengye.com/dotBlog/";
    private ContentResolver contentResolver;
    private Uri uri;
    private Cursor queryCurosr;
    private Long user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        //初始化toolbar
        Toolbar toolbar = findViewById(R.id.report_toolbar);
        setActionBar(toolbar);

        //初始化控件
        init();
    }

    //初始化控件
    private void init(){
        radioGroupReportReasons = findViewById(R.id.report_reason_group);
        reportReasonInput = findViewById(R.id.report_add_report_reason);
        buttonClose = findViewById(R.id.report_toolbar_button_close);
        buttonSend = findViewById(R.id.report_toolbar_button_send);
        preview1 = findViewById(R.id.report_preview_1);
        preview2 = findViewById(R.id.report_preview_2);
        queryCurosr = null;
        user_id = null;

        contentResolver = getContentResolver();
        uri = Uri.parse("content://com.reki.UserLoginInfoContentProvider/t_user_login_info");

        //获取登录用户信息
        String[] projections = new String[]{"user_id"};
        String selection = "is_login = ?";
        String[] selectionArgs = new String[]{"1"};
        queryCurosr = contentResolver.query(uri, projections, selection, selectionArgs, null);
        if(queryCurosr.getCount() != 0){
            queryCurosr.moveToFirst();
            user_id = queryCurosr.getLong(queryCurosr.getColumnIndex("user_id"));
        } else{
            //跳转到登录窗口
        }

        //获取要显示的预览信息
        Bundle bundle = this.getIntent().getExtras();
        preview1.setText(bundle.getString("preview1"));
        preview2.setText(bundle.getString("preview2"));
        reportContentID = bundle.getLong("reportContentID");
        //获取举报类型
        type = bundle.getString("type");

        //获得单选按钮组的选择
        radioGroupReportReasons.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.report_contain_ads:
                        reportChosenReason = "含有广告。";
                        break;
                    case R.id.report_contain_offensive_language:
                        reportChosenReason = "辱骂攻击。";
                        break;
                    case R.id.report_contain_r18:
                        reportChosenReason = "低俗色情。";
                        break;
                    case R.id.report_content_useless:
                        reportChosenReason = "内容无意义。";
                        break;
                }
            }
        });

        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //发送举报信息
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = baseUrl + "SendReportServlet";
                String reportReason = reportChosenReason + reportReasonInput.getText().toString();
                JSONObject jsonObject = new JSONObject();
                try{
                    jsonObject.put("reportContentID", reportContentID);
                    jsonObject.put("type", type);
                    jsonObject.put("userID", user_id);
                    jsonObject.put("reportReason", reportReason);
                } catch (JSONException e){
                    Log.e("JSONParseError", e.toString());
                }
                OkhttpUtil.okHttpPostJson(url, jsonObject.toString(), new CallBackUtil.CallBackJson() {
                    @Override
                    public void onFailure(Call call, Exception e) {
                        Log.e("NetworkError", e.toString());
                    }

                    @Override
                    public void onResponse(JSONObject response) {
                        try{
                            String result = response.getString("result");
                            switch (result){
                                case "success":
                                    //转到结果页面并显示结果
                                    Intent intent = new Intent();
                                    intent.setClass(ReportActivity.this, ResultTypeActivity.class);
                                    Bundle bundle1 = new Bundle();
                                    bundle1.putString("result", "举报成功，我们会尽快处理");
                                    intent.putExtras(bundle1);
                                    startActivity(intent);
                                    finish();
                                    break;
                                case "fail":
                                    Toast.makeText(ReportActivity.this, "举报失败，请重试",Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        } catch (JSONException e){
                            Log.e("JSONParseError", e.toString());
                        }
                    }
                });
            }
        });
    }
}
