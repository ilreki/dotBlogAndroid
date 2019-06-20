package com.reki.dotBlog;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Toast;
import android.widget.Toolbar;

import com.reki.dotBlog.adapter.BlogAdapter;
import com.reki.dotBlog.listener.BlogRecyclerViewOnClickListener;
import com.reki.dotBlog.popupWindow.PopupWindowBlog;
import com.reki.dotBlog.util.CallBackUtil;
import com.reki.dotBlog.util.OkhttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;

//主页、热门博文
public class HomeActivity extends AppCompatActivity {
    private ImageButton buttonHome, buttonCategory, buttonWriteBlog, buttonNotification, buttonMyInfo, buttonSearch;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private PopupWindowBlog popupWindowBlog;
    private JSONArray resList;
    private JSONArray newData;

    private int lastVisibleItem = 0;
    private final int PAGE_COUNT = 10;
    private GridLayoutManager mLayoutManager;
    private BlogAdapter adapter;

//    private String baseUrl = "http://192.168.1.104:8080/dotBlog/";
    private String baseUrl = "http://reki.vipgz1.idcfengye.com/dotBlog/";
    private ContentResolver contentResolver;
    private Uri uri;
    private Cursor queryCursor;
    private long user_id;
    private int user_type;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        //设置toolbar
        Toolbar toolbar = findViewById(R.id.home_toolbar);
        setActionBar(toolbar);

        //初始化控件
        init();
        //初始化refreshlayout
        initRefreshLayout();
        //初始化recyclerview
        prepareRecyclerView();
        //动态权限申请
        myRequestPermission();
    }

    //初始化控件
    private void init(){
        buttonHome = findViewById(R.id.home_nav_bar_home);
        buttonCategory = findViewById(R.id.home_nav_bar_category);
        buttonWriteBlog = findViewById(R.id.home_nav_bar_write_blog);
        buttonNotification = findViewById(R.id.home_nav_bar_notification);
        buttonMyInfo = findViewById(R.id.home_nav_bar_my_info);
        buttonSearch = findViewById(R.id.home_toolbar_button_search);
        swipeRefreshLayout = findViewById(R.id.home_swipe_refresh_layout);
        recyclerView = findViewById(R.id.home_recycler_view);

        contentResolver = getContentResolver();
        uri = Uri.parse("content://com.reki.UserLoginInfoContentProvider/t_user_login_info");
        queryCursor = null;
        user_id = 0l;
        user_type = 2;

        //获取登录用户信息
        String[] projections = new String[]{"user_id, user_type"};
        String selection = "is_login = ?";
        String[] selectionArgs = new String[]{"1"};
        queryCursor = contentResolver.query(uri, projections, selection, selectionArgs, null);
        if(queryCursor.getCount() != 0){
            queryCursor.moveToFirst();
            user_id = queryCursor.getLong(queryCursor.getColumnIndex("user_id"));
            user_type = queryCursor.getInt(queryCursor.getColumnIndex("user_type"));
            Log.i("hasLogin", "yes");
        } else{
            //跳转到登录窗口
            ContentValues values = new ContentValues();
            values.put("user_id", 1);
            values.put("username", "reki");
            values.put("password", "reki7354");
            values.put("avatar", "default.png");
            values.put("user_type", 2);
            values.put("is_login", 1);
            uri = contentResolver.insert(uri, values);
            if(ContentUris.parseId(uri) > 0){
                Log.i("Login", "success");
            }
            else{
                Log.i("Login", "fail");
            }
        }

        buttonHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //该版本该Activity内我们不需要处理buttonHome的点击事件
            }
        });

        buttonCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(HomeActivity.this, CategoryActivity.class);
                startActivity(intent);
                finish();
            }
        });

        buttonWriteBlog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("error", "can't start");
                Intent intent = new Intent();
                intent.setClass(HomeActivity.this, BlogEditActivity.class);
                startActivity(intent);
            }
        });

        buttonNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(HomeActivity.this, NotificationActivity.class);
                startActivity(intent);
                finish();
            }
        });

        buttonMyInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent();
//                intent.setClass(HomeActivity.this, MyInfoActivity.class);
//                startActivity(intent);
//                finish();
            }
        });

        //点击搜索按钮
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(HomeActivity.this, SearchTypeActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("type", "全部");
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
    }

    //初始化refreshlayout
    private void initRefreshLayout() {
        swipeRefreshLayout.setBackgroundResource(R.color.white);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);
        //设置下拉刷新
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                if(adapter != null){
                    adapter.resetData();
                    prepareUpdateRecyclerViewData(0, PAGE_COUNT);
                } else{
                    prepareRecyclerView();
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    //准备recyclerview
    private void prepareRecyclerView(){
        prepareRecyclerViewData(0, PAGE_COUNT);
    }

    //初始化recyclerview
    private void initRecyclerView() {
        if(resList == null){
            return;
        }
        adapter = new BlogAdapter(resList, HomeActivity.this, resList.length() == 10);
        mLayoutManager = new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);
        //设置分割线
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        //设置点击和长按事件的处理
        recyclerView.addOnItemTouchListener(new BlogRecyclerViewOnClickListener(HomeActivity.this, recyclerView, new BlogRecyclerViewOnClickListener.OnItemClickListener() {
            @Override
            public void OnItemClick(View v, JSONObject jsonObject) {
                try{
                    Intent intent = new Intent();
                    intent.setClass(HomeActivity.this, BlogBrowserActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putLong("blogID", jsonObject.getLong("blogID"));
                    bundle.putLong("publisherID", jsonObject.getLong("publisherID"));
                    intent.putExtras(bundle);
                    startActivity(intent);
                } catch (JSONException e){
                    Log.i("JSONParseError", e.toString());
                }
            }

            @Override
            public void OnLongItemPress(View v, JSONObject jsonObject) {
                popupMyWindowBlog(jsonObject);
            }
        }));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        //滑动时判断是否到达底部，并处理
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (!adapter.isFadeTips() && lastVisibleItem + 1 == adapter.getItemCount()) {
                        prepareUpdateRecyclerViewData(adapter.getRealLastPosition(), adapter.getRealLastPosition() + PAGE_COUNT);
                    }

                    if (adapter.isFadeTips() && lastVisibleItem + 2 == adapter.getItemCount()) {
                        prepareUpdateRecyclerViewData(adapter.getRealLastPosition(), adapter.getRealLastPosition() + PAGE_COUNT);
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                lastVisibleItem = mLayoutManager.findLastVisibleItemPosition();
            }
        });
    }

    //准备要显示的数据
    private void prepareRecyclerViewData(final int firstIndex, final int lastIndex) {
        resList = new JSONArray();
        String url = baseUrl + "GetTopBlogServlet";
        try{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("start", firstIndex);
            jsonObject.put("count", lastIndex - firstIndex);
            OkhttpUtil.okHttpPostJson(url, jsonObject.toString(), new CallBackUtil.CallBackJsonArray() {
                @Override
                public void onFailure(Call call, Exception e) {
                    Log.e("NetworkError", e.toString());
                }

                @Override
                public void onResponse(JSONArray response) {
                    resList = response;
                    //得到数据后初始化
                    initRecyclerView();
                }
            });
        } catch (JSONException e){
            Log.e("JSONParseError", e.toString());
        }
    }

    //更新recyclerview（上拉加载）
    private void updateRecyclerView() {
        if (newData != null) {
            adapter.updateList(newData, newData.length() == PAGE_COUNT);
        } else {
            adapter.updateList(null, false);
        }
    }

    //准备更新数据
    private void prepareUpdateRecyclerViewData(int fromIndex, int toIndex){
        newData = new JSONArray();
        String url = baseUrl + "GetTopBlogServlet";
        try{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("start", fromIndex);
            jsonObject.put("count", toIndex - fromIndex);
            OkhttpUtil.okHttpPostJson(url, jsonObject.toString(), new CallBackUtil.CallBackJsonArray() {
                @Override
                public void onFailure(Call call, Exception e) {
                    Log.e("NetworkError", e.toString());
                }

                @Override
                public void onResponse(JSONArray response) {
                    newData = response;
                    //获取到更新数据后更新
                    updateRecyclerView();
                }
            });
        } catch (JSONException e){
            Log.e("JSONParseError", e.toString());
        }
    }

    //博文弹窗
    private void popupMyWindowBlog(JSONObject jsonObject){
        Intent intent = getIntent();
        Bundle tempBundle = new Bundle();
        try{
            if(user_type == 0 || user_type == 1 || user_id == jsonObject.getLong("publisherID")){
                tempBundle.putBoolean("showDelete", true);
            } else{
                tempBundle.putBoolean("showDelete", false);
            }
        } catch (JSONException e){
            Log.e("JSONParseError", e.toString());
        }
        intent.replaceExtras(tempBundle);
        popupWindowBlog = new PopupWindowBlog(HomeActivity.this, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //点击按钮后关闭弹窗
                popupWindowBlog.dismiss();
                String url;
                switch (v.getId()){
                    //删除的处理
                    case R.id.popup_window_blog_delete:
                        AlertDialog alertDialog = new AlertDialog.Builder(HomeActivity.this)
                                .setTitle("是否删除?")
                                .setNegativeButton("否", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String  url = baseUrl + "SendDeleteServlet";
                                        try{
                                            JSONObject tempJson = new JSONObject();
                                            tempJson.put("deleteID", jsonObject.getLong("blogID"));
                                            tempJson.put("type", "blog");
                                            OkhttpUtil.okHttpPostJson(url, tempJson.toString(), new CallBackUtil.CallBackJson() {
                                                @Override
                                                public void onFailure(Call call, Exception e) {
                                                    Log.e("NetworkError", e.toString());
                                                }

                                                @Override
                                                public void onResponse(JSONObject response) {
                                                    try{
                                                        switch (response.getString("result")){
                                                            case "success":
                                                                Toast.makeText(HomeActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                                                                swipeRefreshLayout.setRefreshing(true);
                                                                if(adapter != null){
                                                                    adapter.resetData();
                                                                    prepareUpdateRecyclerViewData(0, PAGE_COUNT);
                                                                } else{
                                                                    prepareRecyclerView();
                                                                }
                                                                swipeRefreshLayout.setRefreshing(false);
                                                                break;
                                                            case "default":
                                                                Toast.makeText(HomeActivity.this, "删除失败，请重试", Toast.LENGTH_SHORT).show();
                                                                break;
                                                        }
                                                    } catch (JSONException e){
                                                        Log.e("JSONParseError", e.toString());
                                                    }
                                                }
                                            });
                                        } catch (JSONException e){
                                            Log.e("JSONParseError", e.toString());
                                        }
                                    }
                                })
                                .create();
                        alertDialog.show();
                        break;
                    //收藏的处理
                    case R.id.popup_window_blog_favorite:
                        url = baseUrl + "SendFavoriteServlet";
                        try{
                            JSONObject tempJson = new JSONObject();
                            tempJson.put("favoriteID", jsonObject.getLong("blogID"));
                            tempJson.put("userID", user_id);
                            OkhttpUtil.okHttpPostJson(url, tempJson.toString(), new CallBackUtil.CallBackJson() {
                                @Override
                                public void onFailure(Call call, Exception e) {
                                    Log.e("NetworkError", e.toString());
                                }

                                @Override
                                public void onResponse(JSONObject response) {
                                    try{
                                        switch (response.getString("result")){
                                            case "success":
                                                Toast.makeText(HomeActivity.this, "已收藏", Toast.LENGTH_SHORT).show();
                                                break;
                                            case "default":
                                                Toast.makeText(HomeActivity.this, "收藏失败，请重试", Toast.LENGTH_SHORT).show();
                                                break;
                                        }
                                    } catch (JSONException e){
                                        Log.e("JSONParseError", e.toString());
                                    }
                                }
                            });
                        } catch (JSONException e){
                            Log.e("JSONParseError", e.toString());
                        }
                        break;
                    //举报的处理
                    case R.id.popup_window_blog_report:
                        Intent intent = new Intent();
                        intent.setClass(HomeActivity.this, ReportActivity.class);
                        Bundle bundle = new Bundle();
                        try{
                            bundle.putLong("reportContentID", jsonObject.getLong("blogID"));
                            bundle.putString("preview1", jsonObject.getString("blogTitle"));
                            bundle.putString("preview2", jsonObject.getString("blogContent"));
                            bundle.putString("type", "blog");
                        } catch (JSONException e){
                            Log.e("JSONParseError", e.toString());
                        }
                        intent.putExtras(bundle);
                        startActivity(intent);
                        break;
                    default:
                        break;
                }
            }
        });
        //弹出时背景为半透明
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.4f;
        getWindow().setAttributes(lp);

        popupWindowBlog.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                //关闭时恢复背景
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.alpha = 1f;
                getWindow().setAttributes(lp);
            }
        });
        //设置弹窗位置并显示
        popupWindowBlog.showAtLocation(HomeActivity.this.findViewById(R.id.home_main), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
    }

    //动态获取权限
    protected void myRequestPermission(){
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
            //大于安卓M时才需要动态获取
            //要请求的权限
            List<String> permissions = new ArrayList<String>();
            //已经请求了的权限
            List<String> removePerm = new ArrayList<String>();

            //添加权限
            permissions.add(Manifest.permission.CAMERA);
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            //判断是否已经请求
            for(String permission : permissions){
                int permissionOk = ActivityCompat.checkSelfPermission(getApplication(), permission);
                if(permissionOk == PackageManager.PERMISSION_GRANTED){
                    //请求了的加入要去掉的
                    removePerm.add(permission);
                }
            }
            if(!removePerm.isEmpty()){
                //如果不需要申请的权限，则从请求权限列表中删除
                for(String perm : removePerm){
                    permissions.remove(perm);
                }
            }
            if(!permissions.isEmpty()){
                //请求权限
                ActivityCompat.requestPermissions(HomeActivity.this,
                        permissions.toArray(new String[permissions.size()]), 1);
            }
        }
    }

    //获取并处理请求权限后的结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
