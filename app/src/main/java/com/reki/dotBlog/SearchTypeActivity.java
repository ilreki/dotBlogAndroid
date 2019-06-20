package com.reki.dotBlog;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Spinner;
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

import okhttp3.Call;

//搜索
public class SearchTypeActivity extends AppCompatActivity {

    private ImageButton buttonSearch, buttonBack;
    private EditText searchInput;
    private Spinner categorySpinner;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private PopupWindowBlog popupWindowBlog;
    private JSONArray resList;
    private JSONArray newData;

    private int lastVisibleItem = 0;
    private final int PAGE_COUNT = 10;
    private GridLayoutManager mLayoutManager;
    private BlogAdapter adapter;
    private Bundle bundleReceive;
    private int spinnerChoose;
    private String searchWord;

//    private String baseUrl = "http://192.168.1.104:8080/dotBlog/";
    private String baseUrl = "http://reki.vipgz1.idcfengye.com/dotBlog/";
    private ContentResolver contentResolver;
    private Uri uri;
    private Cursor queryCursor;
    private Long user_id;
    private int user_type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_type);

        //设置toolbar
        Toolbar toolbar = findViewById(R.id.search_type_toolbar);
        setActionBar(toolbar);

        //初始化控件
        init();
    }

    //初始化控件
    private void init(){
        buttonBack = findViewById(R.id.search_type_toolbar_button_back);
        searchInput = findViewById(R.id.search_type_toolbar_input);
        buttonSearch = findViewById(R.id.search_type_toolbar_button_search);
        swipeRefreshLayout = findViewById(R.id.search_type_swipe_refresh_layout);
        recyclerView = findViewById(R.id.search_type_recycler_view);
        categorySpinner = findViewById(R.id.search_type_category_spinner);
        searchWord = "";
        bundleReceive = this.getIntent().getExtras();

        contentResolver = getContentResolver();
        uri = Uri.parse("content://com.reki.UserLoginInfoContentProvider/t_user_login_info");
        queryCursor = null;
        user_id = 0l;
        user_type = 2;

        //获取用户登录信息
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
        }

        //设置下拉选择框，并获取分类
        switch (bundleReceive.getString("type")){
            case "全部":
                spinnerChoose = 100;
                categorySpinner.setSelection(6);
                break;
            case "科技":
                spinnerChoose = 0;
                categorySpinner.setSelection(0);
                break;
            case "音乐":
                spinnerChoose = 1;
                categorySpinner.setSelection(1);
                break;
            case "影视":
                spinnerChoose = 2;
                categorySpinner.setSelection(2);
                break;
            case "动画":
                spinnerChoose = 3;
                categorySpinner.setSelection(3);
                break;
            case "生活":
                spinnerChoose = 4;
                categorySpinner.setSelection(4);
                break;
            case "游戏":
                spinnerChoose = 5;
                categorySpinner.setSelection(5);
                break;
            default:
                spinnerChoose = 100;
                categorySpinner.setSelection(6);
                break;
        }

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //搜索
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipeRefreshLayout.setRefreshing(true);
                searchWord = searchInput.getText().toString();
                if(adapter != null){
                    adapter.resetData();
                    prepareUpdateRecyclerViewData(0, PAGE_COUNT);
                } else{
                    prepareRecyclerView();
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        //切换下拉框选择
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] categorySearchList = getResources().getStringArray(R.array.category_search);
                switch (categorySearchList[position]){
                    case "全部":
                        spinnerChoose = 100;
                        break;
                    case "科技":
                        spinnerChoose = 0;
                        break;
                    case "音乐":
                        spinnerChoose = 1;
                        break;
                    case "影视":
                        spinnerChoose = 2;
                        break;
                    case "动画":
                        spinnerChoose = 3;
                        break;
                    case "生活":
                        spinnerChoose = 4;
                        break;
                    case "游戏":
                        spinnerChoose = 5;
                        break;
                    default:
                        spinnerChoose = 100;
                        break;
                }

                swipeRefreshLayout.setRefreshing(true);
                searchWord = searchInput.getText().toString();
                if(adapter != null){
                    adapter.resetData();
                    prepareUpdateRecyclerViewData(0, PAGE_COUNT);
                } else{
                    prepareRecyclerView();
                }
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing
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
                searchWord = searchInput.getText().toString();
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
        adapter = new BlogAdapter(resList, SearchTypeActivity.this, resList.length() == 10);
        mLayoutManager = new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);
        //设置分割线
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        //设置点击和长按事件的处理
        recyclerView.addOnItemTouchListener(new BlogRecyclerViewOnClickListener(SearchTypeActivity.this, recyclerView, new BlogRecyclerViewOnClickListener.OnItemClickListener() {
            @Override
            public void OnItemClick(View v, JSONObject jsonObject) {
                try{
                    Intent intent = new Intent();
                    intent.setClass(SearchTypeActivity.this, BlogBrowserActivity.class);
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
        String url = baseUrl + "SearchBlogServlet";
        try{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("category", spinnerChoose);
            jsonObject.put("searchWord", searchWord);
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
            adapter.updateList(newData, newData.length() == 10);
        } else {
            adapter.updateList(null, false);
        }
    }

    //准备更新数据
    private void prepareUpdateRecyclerViewData(int fromIndex, int toIndex){
        newData = new JSONArray();
        String url = baseUrl + "SearchBlogServlet";
        try{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("category", spinnerChoose);
            jsonObject.put("searchWord", searchWord);
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
        popupWindowBlog = new PopupWindowBlog(SearchTypeActivity.this, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //点击按钮后关闭弹窗
                popupWindowBlog.dismiss();
                String url;
                switch (v.getId()){
                    //删除的处理
                    case R.id.popup_window_blog_delete:
                        AlertDialog alertDialog = new AlertDialog.Builder(SearchTypeActivity.this)
                                .setTitle("是否删除?")
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
                                                                Toast.makeText(SearchTypeActivity.this, "已删除", Toast.LENGTH_SHORT).show();
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
                                                                Toast.makeText(SearchTypeActivity.this, "删除失败，请重试", Toast.LENGTH_SHORT).show();
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
                        url = baseUrl + "SendFavoriteServlet";
                        try{
                            JSONObject tempJson = new JSONObject();
                            tempJson.put("blogID", jsonObject.getLong("blogID"));
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
                                                Toast.makeText(SearchTypeActivity.this, "已收藏", Toast.LENGTH_SHORT).show();
                                                break;
                                            case "default":
                                                Toast.makeText(SearchTypeActivity.this, "收藏失败，请重试", Toast.LENGTH_SHORT).show();
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
                        intent.setClass(SearchTypeActivity.this, ReportActivity.class);
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
        popupWindowBlog.showAtLocation(SearchTypeActivity.this.findViewById(R.id.search_type_main), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
    }
}
