package com.reki.dotBlog;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toolbar;

//结果显示
public class ResultTypeActivity extends AppCompatActivity {

    private TextView showResult;
    private ImageButton buttonClose;
    private Bundle bundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_type);

        //设置toolbar
        Toolbar toolbar = findViewById(R.id.result_type_toolbar);
        setActionBar(toolbar);

        //初始化控件
        init();
    }

    //初始化控件
    private void init(){
        showResult = findViewById(R.id.result_type_show_result);
        buttonClose = findViewById(R.id.result_type_button_close);
        bundle = this.getIntent().getExtras();

        //显示结果
        showResult.setText(bundle.getString("result"));
        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
