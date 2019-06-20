package com.reki.dotBlog;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toolbar;

import com.reki.dotBlog.adapter.NotificationAdapter;
import com.reki.dotBlog.listener.NotificationRecyclerViewOnClickListener;
import com.reki.dotBlog.popupWindow.PopupWindowBlog;
import com.reki.dotBlog.util.CallBackUtil;
import com.reki.dotBlog.util.OkhttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;

//消息提示
public class NotificationActivity extends AppCompatActivity {

    private ImageButton buttonHome, buttonCategory, buttonWriteBlog, buttonNotification, buttonMyInfo;
    private Button tabComment, tabReply;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private JSONArray resList;
    private JSONArray newData;

    private int lastVisibleItem = 0;
    private final int PAGE_COUNT = 10;
    private GridLayoutManager mLayoutManager;
    private NotificationAdapter adapter;

    private ContentResolver contentResolver;
    private Uri uri;

//    private String baseUrl = "http://192.168.1.104:8080/dotBlog/";
    private String baseUrl = "http://reki.vipgz1.idcfengye.com/dotBlog/";
    private Cursor queryCurosr;
    private Long user_id;

    private boolean isCommentNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        //设置toolbar
        Toolbar toolbar = findViewById(R.id.notification_toolbar);
        setActionBar(toolbar);

        //初始化控件
        init();
        //初始化refreshlayout
        initRefreshLayout();
        //初始化recyclerview
        prepareRecyclerView();
    }

    //初始化控件
    private void init(){
        buttonHome = findViewById(R.id.notification_nav_bar_home);
        buttonCategory = findViewById(R.id.notification_nav_bar_category);
        buttonWriteBlog = findViewById(R.id.notification_nav_bar_write_blog);
        buttonNotification = findViewById(R.id.notification_nav_bar_notification);
        buttonMyInfo = findViewById(R.id.notification_nav_bar_my_info);
        tabComment = findViewById(R.id.notification_tab_comment);
        tabReply = findViewById(R.id.notification_tab_reply);
        swipeRefreshLayout = findViewById(R.id.notification_swipe_refresh_layout);
        recyclerView = findViewById(R.id.notification_recycler_view);
        contentResolver = getContentResolver();
        uri = Uri.parse("content://com.reki.UserLoginInfoContentProvider/t_user_login_info");
        queryCurosr = null;
        user_id = null;
        isCommentNotification = true;

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

        buttonHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(NotificationActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });

        buttonCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(NotificationActivity.this, CategoryActivity.class);
                startActivity(intent);
                finish();
            }
        });

        buttonWriteBlog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(NotificationActivity.this, BlogEditActivity.class);
                startActivity(intent);
            }
        });

        buttonNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //该版本该Activity内我们不需要处理buttonNotification的点击事件
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

        //切换到评论
        tabComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipeRefreshLayout.setRefreshing(true);
                isCommentNotification = true;
                Drawable drawableColorPrimary = getDrawable(R.color.colorPrimary);
                Drawable drawableColorAccent = getDrawable(R.color.colorAccent);
                tabComment.setBackground(drawableColorAccent);
                tabReply.setBackground(drawableColorPrimary);
                if(adapter != null){
                    adapter.resetData();
                    prepareUpdateRecyclerViewData(0, PAGE_COUNT);
                } else{
                    prepareRecyclerView();
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        //切换到回复
        tabReply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipeRefreshLayout.setRefreshing(true);
                isCommentNotification = false;
                Drawable drawableColorPrimary = getDrawable(R.color.colorPrimary);
                Drawable drawableColorAccent = getDrawable(R.color.colorAccent);
                tabComment.setBackground(drawableColorPrimary);
                tabReply.setBackground(drawableColorAccent);
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

    //初始化refreshlayout
    private void initRefreshLayout() {
        swipeRefreshLayout.setBackgroundResource(R.color.white);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent);
        //设置下拉刷新
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                if(resList != null){
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
        adapter = new NotificationAdapter(resList, NotificationActivity.this, resList.length() == 10);
        mLayoutManager = new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);
        //设置分割线
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        //设置点击和长按事件的处理
        recyclerView.addOnItemTouchListener(new NotificationRecyclerViewOnClickListener(NotificationActivity.this, recyclerView, new NotificationRecyclerViewOnClickListener.OnItemClickListener() {
            @Override
            public void OnItemClick(View v, JSONObject jsonObject) {
                try{
                    Intent intent = new Intent();
                    Bundle bundle = new Bundle();
                    if(isCommentNotification){
                        intent.setClass(NotificationActivity.this, BlogBrowserActivity.class);
                        bundle.putLong("blogID", jsonObject.getLong("notificationID"));
                    } else{
                        intent.setClass(NotificationActivity.this, CommentBrowserActivity.class);
                        bundle.putLong("commentID", jsonObject.getLong("notificationID"));
                    }
                    intent.putExtras(bundle);
                    startActivity(intent);
                } catch (JSONException e){
                    Log.i("JSONParseError", e.toString());
                }
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
        String url = baseUrl + "GetNotificationServlet";
        try{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userID", user_id);
            if(isCommentNotification){
                jsonObject.put("type", "comment");
            } else{
                jsonObject.put("type", "reply");
            }
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
        if (newData.length() > 0) {
            adapter.updateList(newData, newData.length() == PAGE_COUNT);
        } else {
            adapter.updateList(null, false);
        }
    }

    //准备更新数据
    private void prepareUpdateRecyclerViewData(int fromIndex, int toIndex){
        newData = new JSONArray();
        String url = baseUrl + "GetNotificationServlet";
        try{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userID", user_id);
            if(isCommentNotification){
                jsonObject.put("type", "comment");
            } else{
                jsonObject.put("type", "reply");
            }
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
                    updateRecyclerView();
                }
            });
        } catch (JSONException e){
            Log.e("JSONParseError", e.toString());
        }
    }
}
