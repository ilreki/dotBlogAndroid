package com.reki.dotBlog;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toolbar;

import com.reki.dotBlog.adapter.CategoryAdapter;
import com.reki.dotBlog.listener.CategoryRecyclerViewOnClickListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//分类
public class CategoryActivity extends AppCompatActivity {

    private ImageButton buttonHome, buttonCategory, buttonWriteBlog, buttonNotification, buttonMyInfo;
    private RecyclerView recyclerView;
    private JSONArray resList;

    private GridLayoutManager mLayoutManager;
    private CategoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        //设置toolbar
        Toolbar toolbar = findViewById(R.id.category_toolbar);
        setActionBar(toolbar);

        //初始化控件
        init();
        //初始化recyclerview
        prepareRecyclerView();
    }

    //初始化控件
    private void init(){
        buttonHome = findViewById(R.id.category_nav_bar_home);
        buttonCategory = findViewById(R.id.category_nav_bar_category);
        buttonWriteBlog = findViewById(R.id.category_nav_bar_write_blog);
        buttonNotification = findViewById(R.id.category_nav_bar_notification);
        buttonMyInfo = findViewById(R.id.category_nav_bar_my_info);
        recyclerView = findViewById(R.id.category_recycler_view);

        buttonHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(CategoryActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });

        buttonCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //该版本该Activity内我们不需要处理buttonHome的点击事件
            }
        });

        buttonWriteBlog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(CategoryActivity.this, BlogEditActivity.class);
                startActivity(intent);
            }
        });

        buttonNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(CategoryActivity.this, NotificationActivity.class);
                startActivity(intent);
                finish();
            }
        });

        buttonMyInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent();
//                intent.setClass(CategoryActivity.this, MyInfoActivity.class);
//                startActivity(intent);
//                finish();
            }
        });
    }

    //准备recyclerview
    private void prepareRecyclerView(){
        prepareRecyclerViewData();
    }

    //初始化recyclerview
    private void initRecyclerView() {
        adapter = new CategoryAdapter(resList, CategoryActivity.this);
        mLayoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnItemTouchListener(new CategoryRecyclerViewOnClickListener(CategoryActivity.this, recyclerView, new CategoryRecyclerViewOnClickListener.OnItemClickListener(){
            @Override
            public void OnItemClick(View v, JSONObject jsonObject) {
                try{
                    Intent intent = new Intent();
                    intent.setClass(CategoryActivity.this, SearchTypeActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("type", jsonObject.getString("categoryName"));
                    intent.putExtras(bundle);
                    startActivity(intent);
                } catch (JSONException e){
                    Log.i("JSONParseError", e.toString());
                }
            }
        }));

        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    //准备recyclerview的数据
    private void prepareRecyclerViewData() {
        resList = new JSONArray();
        try{
            String[] categoryList = getResources().getStringArray(R.array.category);
            for (String categoryName : categoryList) {
                JSONObject jsonObject = new JSONObject();
                switch (categoryName){
                    case "科技":
                        jsonObject.put("categoryIcon", R.drawable.ic_tech);
                        jsonObject.put("categoryName", "科技");
                        break;
                    case "音乐":
                        jsonObject.put("categoryIcon", R.drawable.ic_music);
                        jsonObject.put("categoryName", "音乐");
                        break;
                    case "影视":
                        jsonObject.put("categoryIcon", R.drawable.ic_video);
                        jsonObject.put("categoryName", "影视");
                        break;
                    case "动画":
                        jsonObject.put("categoryIcon", R.drawable.ic_anim);
                        jsonObject.put("categoryName", "动画");
                        break;
                    case "生活":
                        jsonObject.put("categoryIcon", R.drawable.ic_life);
                        jsonObject.put("categoryName", "生活");
                        break;
                    case "游戏":
                        jsonObject.put("categoryIcon", R.drawable.ic_game);
                        jsonObject.put("categoryName", "游戏");
                        break;
                }
                resList.put(jsonObject);
            }
            //初始化
            initRecyclerView();
        } catch (JSONException e){
            Log.e("JSONParseError", e.toString());
        }
    }
}
