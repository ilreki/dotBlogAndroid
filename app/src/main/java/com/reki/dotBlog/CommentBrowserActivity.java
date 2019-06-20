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
import android.text.Editable;
import android.text.TextWatcher;
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

import com.reki.dotBlog.adapter.ReplyAdapter;
import com.reki.dotBlog.listener.ReplyRecyclerViewOnClickListener;
import com.reki.dotBlog.popupWindow.PopupWindowComment;
import com.reki.dotBlog.popupWindow.PopupWindowReply;
import com.reki.dotBlog.util.CallBackUtil;
import com.reki.dotBlog.util.OkhttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;

//浏览评论
public class CommentBrowserActivity extends AppCompatActivity {

    private ImageButton buttonClose, buttonMore, buttonSend;
    private CircleImageView avatar;
    private TextView content, publisher, date;
    private EditText commentReplyInput;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private PopupWindowComment popupWindowComment;
    private PopupWindowReply popupWindowReply;
    private JSONArray resList;
    private JSONArray newData;

    private int lastVisibleItem = 0;
    private final int PAGE_COUNT = 10;
    private GridLayoutManager mLayoutManager;
    private ReplyAdapter adapter;
    private Bundle bundle;

//    private String baseUrl = "http://192.168.1.104:8080/dotBlog/";
    private String baseUrl = "http://reki.vipgz1.idcfengye.com/dotBlog/";
    private ContentResolver contentResolver;
    private Uri uri;
    private Cursor queryCursor;
    private long user_id;
    private int user_type;
    private long toSomeoneID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment_browser);

        //设置toolbar
        Toolbar toolbar = findViewById(R.id.comment_browser_toolbar);
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
        buttonClose = findViewById(R.id.comment_browser_toolbar_button_close);
        buttonMore = findViewById(R.id.comment_browser_toolbar_button_more);
        buttonSend = findViewById(R.id.comment_browser_button_send);
        avatar = findViewById(R.id.comment_browser_comment_avatar);
        publisher = findViewById(R.id.comment_browser_comment_publisher);
        date = findViewById(R.id.comment_browser_comment_date);
        content = findViewById(R.id.comment_browser_comment_content);
        commentReplyInput = findViewById(R.id.comment_browser_reply_input);
        swipeRefreshLayout = findViewById(R.id.comment_browser_swipe_refresh_layout);
        recyclerView = findViewById(R.id.comment_browser_recycler_view);
        bundle = this.getIntent().getExtras();
        queryCursor = null;
        user_id = -1l;
        user_type = 2;
        toSomeoneID = -1l;

        contentResolver = getContentResolver();
        uri = Uri.parse("content://com.reki.UserLoginInfoContentProvider/t_user_login_info");

        //获得登录用户信息
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

        commentReplyInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(s.toString().equals("")){
                    //当文字改变时，改变之前的文字为空
                    //则说明是回复评论的
                    toSomeoneID = bundle.getLong("publisherID");
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        buttonMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMyWindowComment();
            }
        });

        //发送回复
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = commentReplyInput.getText().toString();
                String url = baseUrl + "SendReplyServlet";
                JSONObject jsonObject = new JSONObject();
                try{
                    jsonObject.put("userID", user_id);
                    jsonObject.put("commentID", bundle.getLong("commentID"));
                    jsonObject.put("toSomeoneID", toSomeoneID);
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
                                        //回复成功后刷新
                                        commentReplyInput.setText("");
                                        swipeRefreshLayout.setRefreshing(true);
                                        if(adapter != null){
                                            adapter.resetData();
                                            prepareUpdateRecyclerViewData(0, PAGE_COUNT);
                                        } else{
                                            prepareRecyclerView();
                                        }
                                        swipeRefreshLayout.setRefreshing(false);
                                        Toast.makeText(CommentBrowserActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
                                        break;
                                    case "default":
                                        Toast.makeText(CommentBrowserActivity.this, "发送失败，请重试", Toast.LENGTH_SHORT).show();
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

        //获取评论信息
        String url = baseUrl + "GetCommentServlet";
        try{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("commentID", bundle.getLong("commentID"));
            OkhttpUtil.okHttpPostJson(url, jsonObject.toString(), new CallBackUtil.CallBackJson() {
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

                        publisher.setText(response.getString("publisherName"));
                        date.setText(response.getString("commentDate"));
                        content.setText(response.getString("commentContent"));
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
        adapter = new ReplyAdapter(resList, CommentBrowserActivity.this, resList.length() == 10);
        mLayoutManager = new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);
        //设置分割线
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        //设置点击和长按事件的处理
        recyclerView.addOnItemTouchListener(new ReplyRecyclerViewOnClickListener(CommentBrowserActivity.this, recyclerView, new ReplyRecyclerViewOnClickListener.OnItemClickListener() {
            @Override
            public void OnItemClick(View v, JSONObject jsonObject) {
                try{
                    //先清空文字
                    commentReplyInput.setText("");
                    //设置提示信息
                    commentReplyInput.setText("回复 " + jsonObject.getString("publisherName") + ": ");
                    //设置光标位置
                    commentReplyInput.setSelection(commentReplyInput.getText().toString().length());
                    //在文字改变后再改变tosomeoneid，避免又被修改回去
                    toSomeoneID = jsonObject.getLong("publisherID");
                } catch (JSONException e){
                    Log.e("JSONParseError", e.toString());
                }
            }

            @Override
            public void OnLongItemPress(View v, JSONObject jsonObject) {
                popupMyWindowReply(jsonObject);
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
        String url = baseUrl + "GetCommentReplyServlet";
        try{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("commentID", bundle.getLong("commentID"));
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
            //hasMore确定是否还有数据
            adapter.updateList(newData, newData.length() == PAGE_COUNT);
        } else {
            adapter.updateList(null, false);
        }
    }

    //准备更新数据
    private void prepareUpdateRecyclerViewData(int fromIndex, int toIndex){
        newData = new JSONArray();
        String url = baseUrl + "GetCommentReplyServlet";
        try{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("commentID", bundle.getLong("commentID"));
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

    //回复弹窗
    private void popupMyWindowReply(JSONObject jsonObject){
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
        popupWindowReply = new PopupWindowReply(CommentBrowserActivity.this, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //点击任意按钮后关闭弹窗
                popupWindowReply.dismiss();
                String url;
                switch (v.getId()){
                    //点击删除按钮后的处理
                    case R.id.popup_window_reply_delete:
                        AlertDialog alertDialog = new AlertDialog.Builder(CommentBrowserActivity.this)
                                .setTitle("是否删除?")
                                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String  url = baseUrl + "SendDeleteServlet";
                                        try{
                                            JSONObject tempJson = new JSONObject();
                                            tempJson.put("deleteID", jsonObject.getLong("replyID"));
                                            tempJson.put("type", "reply");
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
                                                                Toast.makeText(CommentBrowserActivity.this, "已删除", Toast.LENGTH_SHORT).show();
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
                                                                Toast.makeText(CommentBrowserActivity.this, "删除失败，请重试", Toast.LENGTH_SHORT).show();
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
                    //举报处理
                    case R.id.popup_window_reply_report:
                        Intent intent = new Intent();
                        intent.setClass(CommentBrowserActivity.this, ReportActivity.class);
                        Bundle bundle = new Bundle();
                        try{
                            bundle.putLong("reportContentID", jsonObject.getLong("replyID"));
                            bundle.putString("preview1", jsonObject.getString("publisherName"));
                            bundle.putString("preview2", jsonObject.getString("replyContent"));
                            bundle.putString("type", "reply");
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

        popupWindowReply.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                //弹窗关闭时恢复背景
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.alpha = 1f;
                getWindow().setAttributes(lp);
            }
        });
        //设置弹窗位置并显示
        popupWindowReply.showAtLocation(CommentBrowserActivity.this.findViewById(R.id.comment_browser_main), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
    }

    //评论弹窗
    private void popupMyWindowComment(){
        Intent intent = getIntent();
        Bundle tempBundle = new Bundle();
        if(user_type == 0 || user_type == 1 || user_id == bundle.getLong("publisherID")){
            tempBundle.putBoolean("showDelete", true);
        } else{
            tempBundle.putBoolean("showDelete", false);
        }
        intent.replaceExtras(tempBundle);
        popupWindowComment = new PopupWindowComment(CommentBrowserActivity.this, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //点击按钮后关闭弹窗
                popupWindowComment.dismiss();
                String url;
                switch (v.getId()){
                    //删除的处理
                    case R.id.popup_window_comment_delete:
                        AlertDialog alertDialog = new AlertDialog.Builder(CommentBrowserActivity.this)
                                .setTitle("是否删除?")
                                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String  url = baseUrl + "SendDeleteServlet";
                                        try{
                                            JSONObject tempJson = new JSONObject();
                                            tempJson.put("deleteID", bundle.getLong("commentID"));
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
                                                                Toast.makeText(CommentBrowserActivity.this, "已删除", Toast.LENGTH_SHORT).show();
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
                                                                Toast.makeText(CommentBrowserActivity.this, "删除失败，请重试", Toast.LENGTH_SHORT).show();
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
                        intent.setClass(CommentBrowserActivity.this, ReportActivity.class);
                        Bundle tempBundle = new Bundle();
                        String preview2;
                        if(content.getText().toString().length() > 30){
                            preview2 = content.getText().toString().substring(0, 30) + "...";
                        } else{
                            preview2 = content.getText().toString();
                        }
                        tempBundle.putLong("reportContentID", bundle.getLong("commentID"));
                        tempBundle.putString("preview1", publisher.getText().toString());
                        tempBundle.putString("preview2", preview2);
                        tempBundle.putString("type", "comment");
                        intent.replaceExtras(tempBundle);
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

        popupWindowComment.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                //关闭时恢复背景
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.alpha = 1f;
                getWindow().setAttributes(lp);
            }
        });
        //设置弹窗位置并显示
        popupWindowComment.showAtLocation(CommentBrowserActivity.this.findViewById(R.id.comment_browser_main), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
    }
}
