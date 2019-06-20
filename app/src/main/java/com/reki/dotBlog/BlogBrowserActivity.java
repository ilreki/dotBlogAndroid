package com.reki.dotBlog;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.reki.dotBlog.adapter.CommentAdapter;
import com.reki.dotBlog.listener.CommentRecyclerViewOnClickListener;
import com.reki.dotBlog.popupWindow.PopupWindowBlog;
import com.reki.dotBlog.popupWindow.PopupWindowComment;
import com.reki.dotBlog.util.CallBackUtil;
import com.reki.dotBlog.util.OkhttpUtil;
import com.reki.dotBlog.util.RichTextUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;

//浏览博文
public class BlogBrowserActivity extends AppCompatActivity {

    private ImageButton buttonBack, buttonMore, buttonFavorite, buttonSend;
    private CircleImageView avatar;
    private TextView title, content, publisher, date;
    private EditText commentInput;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private PopupWindowBlog popupWindowBlog;
    private PopupWindowComment popupWindowComment;
    private JSONArray resList;
    private JSONArray newData;

    private int lastVisibleItem = 0;
    private final int PAGE_COUNT = 10;
    private GridLayoutManager mLayoutManager;
    private CommentAdapter adapter;
    private Bundle bundle;

//    private String baseUrl = "http://192.168.1.104:8080/dotBlog/";
    private String baseUrl = "http://reki.vipgz1.idcfengye.com/dotBlog/";
    private ContentResolver contentResolver;
    private Uri uri;
    private Cursor queryCursor;
    private long user_id;
    private int user_type;
    private long favoriteID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blog_browser);

        //设置toolbar
        Toolbar toolbar = findViewById(R.id.blog_browser_toolbar);
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
        buttonBack = findViewById(R.id.blog_browser_toolbar_button_back);
        buttonMore = findViewById(R.id.blog_browser_toolbar_button_more);
        buttonFavorite = findViewById(R.id.blog_browser_button_favorite);
        buttonSend = findViewById(R.id.blog_browser_button_send);
        avatar = findViewById(R.id.blog_browser_avatar);
        publisher = findViewById(R.id.blog_browser_publisher);
        date = findViewById(R.id.blog_browser_date);
        title = findViewById(R.id.blog_browser_title);
        content = findViewById(R.id.blog_browser_content);
        commentInput = findViewById(R.id.blog_browser_comment_input);
        swipeRefreshLayout = findViewById(R.id.blog_browser_swipe_refresh_layout);
        recyclerView = findViewById(R.id.blog_browser_recycler_view);
        bundle = this.getIntent().getExtras();

        queryCursor = null;
        user_id = 0l;
        user_type = 2;

        contentResolver = getContentResolver();
        uri = Uri.parse("content://com.reki.UserLoginInfoContentProvider/t_user_login_info");

        //获取用户登录信息
        String[] projections = new String[]{"user_id, user_type"};
        String selection = "is_login = ?";
        String[] selectionArgs = new String[]{"1"};
        queryCursor = contentResolver.query(uri, projections, selection, selectionArgs, null);
        if(queryCursor.getCount() != 0){
            queryCursor.moveToFirst();
            user_id = queryCursor.getLong(queryCursor.getColumnIndex("user_id"));
            user_type = queryCursor.getInt(queryCursor.getColumnIndex("user_type"));
        } else{
            //跳转到登录窗口
        }

        //检查是否已收藏
        checkIfFavorite();

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        buttonMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMyWindowBlog();
            }
        });

        //收藏时，根据之前获得的favoriteid来判断是收藏还是取消收藏
        buttonFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject jsonObject = new JSONObject();
                String url = baseUrl + "SendFavoriteServlet";
                try{
                    jsonObject.put("favoriteID", bundle.getLong("blogID"));
                    jsonObject.put("userID", user_id);
                    if(favoriteID != -1l){
                        jsonObject.put("type", "delete");
                    } else{
                        jsonObject.put("type", "add");
                    }
                    OkhttpUtil.okHttpPostJson(url, jsonObject.toString(), new CallBackUtil.CallBackJson() {
                        @Override
                        public void onFailure(Call call, Exception e) {
                            Log.e("NetworkError", e.toString());
                        }

                        @Override
                        public void onResponse(JSONObject response) {
                            try{
                                String resultFail;
                                if(favoriteID != -1l){
                                    resultFail = "收藏失败，请重试";
                                } else{
                                    resultFail = "取消收藏失败，请重试";
                                }
                                switch (response.getString("result")){
                                    case "success":
                                        checkIfFavorite();
                                        break;
                                    case "fail":
                                        Toast.makeText(BlogBrowserActivity.this, resultFail, Toast.LENGTH_SHORT).show();
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
        });

        //发送用户评论
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = commentInput.getText().toString();
                String url = baseUrl + "SendCommentServlet";
                JSONObject jsonObject = new JSONObject();
                try{
                    jsonObject.put("userID", user_id);
                    jsonObject.put("blogID", bundle.getLong("blogID"));
                    jsonObject.put("toSomeoneID", bundle.getLong("publisherID"));
                    jsonObject.put("content", content);
                    OkhttpUtil.okHttpPostJson(url, jsonObject.toString(), new CallBackUtil.CallBackJson() {
                        @Override
                        public void onFailure(Call call, Exception e) {
                            Log.e("NetworkError", e.toString());
                        }

                        @Override
                        public void onResponse(JSONObject response) {
                            try{
                                switch (response.getString("result")){
                                    case "success":
                                        commentInput.setText("");
                                        Toast.makeText(BlogBrowserActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
                                        //评论成功后刷新
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
                                        Toast.makeText(BlogBrowserActivity.this, "发送失败，请重试", Toast.LENGTH_SHORT).show();
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
        });

        //获取并初始化博文信息
        String url = baseUrl + "GetBlogServlet";
        try{
            JSONObject tempJson = new JSONObject();
            tempJson.put("blogID", bundle.getLong("blogID"));
            OkhttpUtil.okHttpPostJson(url, tempJson.toString(), new CallBackUtil.CallBackJson() {
                @Override
                public void onFailure(Call call, Exception e) {
                    Log.e("NetworkError", e.toString());
                }

                @Override
                public void onResponse(JSONObject response) {
                    try{
                        String url = baseUrl + "avatar/" + response.getString("publisherAvatar");
                        OkhttpUtil.okHttpGetBitmap(url, new CallBackBitmap() {
                            @Override
                            public void onFailure(Call call, Exception e) {
                                Log.e("NetworkError", e.toString());
                            }

                            @Override
                            public void onResponse(Bitmap response) {
                                avatar.setImageBitmap(response);
                            }
                        });

                        title.setText(response.getString("blogTitle"));
                        publisher.setText(response.getString("publisherName"));
                        date.setText(response.getString("blogDate"));
                        String jsonArrayStr = response.getString("blogContent");
                        JSONArray jsonArray = new JSONArray(jsonArrayStr);
                        JSONObject tempJSONObject = RichTextUtil.fromJson(BlogBrowserActivity.this, jsonArray);
                        content.setText((CharSequence) tempJSONObject.get("result"));
                        content.setMovementMethod(new LinkMovementMethod());
                        loadNetImage(tempJSONObject);
                    } catch (JSONException e){
                        Log.e("JSONParseError", e.toString());
                    }
                }
            });
        } catch (JSONException e){
            Log.e("JSONParseError", e.toString());
        }
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
        adapter = new CommentAdapter(resList, BlogBrowserActivity.this, resList.length() == 10);
        mLayoutManager = new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);
        //设置分割线
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        //设置点击和长按事件的处理
        recyclerView.addOnItemTouchListener(new CommentRecyclerViewOnClickListener(BlogBrowserActivity.this, recyclerView, new CommentRecyclerViewOnClickListener.OnItemClickListener() {
            @Override
            public void OnItemClick(View v, JSONObject jsonObject) {
                Intent intent = new Intent();
                intent.setClass(BlogBrowserActivity.this, CommentBrowserActivity.class);
                Bundle bundle = new Bundle();
                try{
                    bundle.putLong("commentID", jsonObject.getLong("commentID"));
                    bundle.putLong("publisherID", jsonObject.getLong("publisherID"));
                } catch (JSONException e){
                    Log.e("JSONParseError", e.toString());
                }
                intent.putExtras(bundle);
                startActivity(intent);
            }

            @Override
            public void OnLongItemPress(View v, JSONObject jsonObject) {
                popupMyWindowComment(jsonObject);
            }
        }));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
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
        String url = baseUrl + "GetBlogCommentServlet";
        try{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("blogID", bundle.getLong("blogID"));
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
            //hasMore确定是否还有数据
            adapter.updateList(newData, newData.length() == PAGE_COUNT);
        } else {
            adapter.updateList(null, false);
        }
    }

    //准备更新数据
    private void prepareUpdateRecyclerViewData(int fromIndex, int toIndex){
        newData = new JSONArray();
        String url = baseUrl + "GetBlogCommentServlet";
        try{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("blogID", bundle.getLong("blogID"));
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

    //获取要加载的图片的信息
    private void loadNetImage(JSONObject jsonObject){
        try{
            JSONArray jsonArray = jsonObject.getJSONArray("realImageArray");
            for(int i = 0; i < jsonArray.length(); i++){
                JSONObject tempJSONObject = jsonArray.getJSONObject(i);
                String param = tempJSONObject.getString("param");
                int start = tempJSONObject.getInt("start");
                int end = tempJSONObject.getInt("end");

                //设置图片
                setNetImage(param, start, end);
            }
        } catch (JSONException e){
            Log.e("JSONParseError", e.toString());
        }
    }

    //设置博文图片信息
    private void setNetImage(String netImagePath, int start, int end) {
        //请求时若height为1则为按width等比例缩放
        OkhttpUtil.okHttpGetBitmap(netImagePath, new CallBackUtil.CallBackBitmap(1080, 1) {
            @Override
            public void onFailure(Call call, Exception e) {
                Log.e("NetworkError", e.toString());
            }

            @Override
            public void onResponse(Bitmap response) {
                ImageSpan imageSpan = new ImageSpan(BlogBrowserActivity.this, response);
                if(content.getText().toString().isEmpty()){
                    Log.i("textView", "no content");
                } else{
                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(content.getText());
                    spannableStringBuilder.setSpan(imageSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    content.setText(spannableStringBuilder);
                }
            }
        });
    }

    //评论弹窗
    private void popupMyWindowComment(JSONObject jsonObject){
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
        popupWindowComment = new PopupWindowComment(BlogBrowserActivity.this, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //点击任意按钮后关闭弹窗
                popupWindowComment.dismiss();
                String url;
                switch (v.getId()){
                    //点击删除按钮后的处理
                    case R.id.popup_window_comment_delete:
                        AlertDialog alertDialog = new AlertDialog.Builder(BlogBrowserActivity.this)
                            .setTitle("是否删除?")
                            .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String  url = baseUrl + "SendDeleteServlet";
                                    try{
                                        JSONObject tempJson = new JSONObject();
                                        tempJson.put("deleteID", jsonObject.getLong("commentID"));
                                        tempJson.put("type", "comment");
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
                                                            Toast.makeText(BlogBrowserActivity.this, "已删除", Toast.LENGTH_SHORT).show();
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
                                                            Toast.makeText(BlogBrowserActivity.this, "删除失败，请重试", Toast.LENGTH_SHORT).show();
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
                            .setNegativeButton("否", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .create();
                        alertDialog.show();
                        break;
                    //举报的处理
                    case R.id.popup_window_comment_report:
                        Intent intent = new Intent();
                        intent.setClass(BlogBrowserActivity.this, ReportActivity.class);
                        Bundle bundle = new Bundle();
                        try{
                            bundle.putLong("reportContentID", jsonObject.getLong("commentID"));
                            bundle.putString("preview1", jsonObject.getString("publisherName"));
                            bundle.putString("preview2", jsonObject.getString("commentContent"));
                            bundle.putString("type", "comment");
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
        //显示时设置背景为半透明
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.4f;
        getWindow().setAttributes(lp);

        popupWindowComment.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                //弹窗关闭时恢复背景
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.alpha = 1f;
                getWindow().setAttributes(lp);
            }
        });
        //设置弹窗位置并显示
        popupWindowComment.showAtLocation(BlogBrowserActivity.this.findViewById(R.id.blog_browser_main), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
    }

    //博文弹窗
    private void popupMyWindowBlog(){
        Intent intent = getIntent();
        Bundle tempBundle = new Bundle();
        if(user_type == 0 || user_type == 1 || user_id == bundle.getLong("publisherID")){
            tempBundle.putBoolean("showDelete", true);
        } else{
            tempBundle.putBoolean("showDelete", false);
        }
        intent.replaceExtras(tempBundle);
        popupWindowBlog = new PopupWindowBlog(BlogBrowserActivity.this, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //点击按钮后关闭弹窗
                popupWindowBlog.dismiss();
                String url;
                switch (v.getId()){
                    //删除的处理
                    case R.id.popup_window_blog_delete:
                        AlertDialog alertDialog = new AlertDialog.Builder(BlogBrowserActivity.this)
                                .setTitle("是否删除?")
                                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String  url = baseUrl + "SendDeleteServlet";
                                        try{
                                            JSONObject tempJson = new JSONObject();
                                            tempJson.put("deleteID", bundle.getLong("blogID"));
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
                                                                Toast.makeText(BlogBrowserActivity.this, "已删除", Toast.LENGTH_SHORT).show();
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
                                                                Toast.makeText(BlogBrowserActivity.this, "删除失败，请重试", Toast.LENGTH_SHORT).show();
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
                                .setNegativeButton("否", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .create();
                        alertDialog.show();
                        break;
                    //收藏的处理
                    case R.id.popup_window_blog_favorite:
                        JSONObject jsonObject = new JSONObject();
                        url = baseUrl + "SendFavoriteServlet";
                        try{
                            jsonObject.put("favoriteID", bundle.getLong("blogID"));
                            jsonObject.put("userID", user_id);
                            if(favoriteID != -1l){
                                jsonObject.put("type", "delete");
                            } else{
                                jsonObject.put("type", "add");
                            }
                            OkhttpUtil.okHttpPostJson(url, jsonObject.toString(), new CallBackUtil.CallBackJson() {
                                @Override
                                public void onFailure(Call call, Exception e) {
                                    Log.e("NetworkError", e.toString());
                                }

                                @Override
                                public void onResponse(JSONObject response) {
                                    try{
                                        String resultFail;
                                        if(favoriteID != -1l){
                                            resultFail = "取消收藏失败，请重试";
                                        } else{
                                            resultFail = "收藏失败，请重试";
                                        }
                                        switch (response.getString("result")){
                                            case "success":
                                                checkIfFavorite();
                                                break;
                                            case "fail":
                                                Toast.makeText(BlogBrowserActivity.this, resultFail, Toast.LENGTH_SHORT).show();
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
                        intent.setClass(BlogBrowserActivity.this, ReportActivity.class);
                        Bundle tempBundle = new Bundle();
                        String preview2;
                        if(content.getText().toString().length() > 30){
                            preview2 = content.getText().toString().substring(0, 30) + "...";
                        } else{
                            preview2 = content.getText().toString();
                        }
                        tempBundle.putLong("reportContentID", bundle.getLong("blogID"));
                        tempBundle.putString("preview1", title.getText().toString());
                        tempBundle.putString("preview2", preview2);
                        tempBundle.putString("type", "blog");
                        intent.putExtras(tempBundle);
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
        popupWindowBlog.showAtLocation(BlogBrowserActivity.this.findViewById(R.id.blog_browser_main), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
    }

    //检查用户是否收藏了该博文
    private void checkIfFavorite(){
        String url = baseUrl + "GetFavoriteServlet";

        JSONObject jsonObject = new JSONObject();
        try{
            jsonObject.put("blogID", bundle.getLong("blogID"));
            jsonObject.put("userID", user_id);
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
                    favoriteID = response.getLong("favoriteID");
                    //若为-1l则说明没有收藏
                    if(favoriteID != -1l){
                        buttonFavorite.setImageResource(R.drawable.ic_star_red_24dp);
                    } else{
                        buttonFavorite.setImageResource(R.drawable.ic_star_white_24dp);
                    }
                } catch (JSONException e){
                    Log.e("JSONParseError", e.toString());
                }
            }
        });
    }
}
