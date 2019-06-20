package com.reki.dotBlog;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.AlignmentSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.Toolbar;

import com.hss01248.glidepicker.GlideIniter;
import com.hss01248.photoouter.PhotoCallback;
import com.hss01248.photoouter.PhotoUtil;
import com.reki.dotBlog.myView.MyEditText;
import com.reki.dotBlog.util.CallBackUtil;
import com.reki.dotBlog.util.OkhttpUtil;
import com.reki.dotBlog.util.RichTextUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

//博文编辑
public class BlogEditActivity extends AppCompatActivity {

    private MyEditText content;
    private EditText title;
    private ImageButton buttonTitle, buttonBold, buttonInsertPhoto,buttonInsertLink, buttonBack, buttonSend;
    private Spinner spinnerCategory;
    private LinearLayout editBar;
    private int spinnerChoose;
    private boolean titleOn, boldOn, codeToTextChange, styleButtonClicked;
    private int cursorLast, cursorNow, contentLengthLast, contentLengthNow, contentChangeStart, setBackStart;
    private float relativeSize;

//    private String baseUrl = "http://192.168.1.104:8080/dotBlog/";
    private String baseUrl = "http://reki.vipgz1.idcfengye.com/dotBlog/";
    private final int REQUEST_IMAGE = 200;
    private ContentResolver contentResolver;
    private Uri uri;
    private Cursor queryCursor;
    private Long user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blog_edit);

        //设置toolbar
        Toolbar toolbar = findViewById(R.id.edit_page_toolbar);
        setActionBar(toolbar);

        //初始化控件
        init();
    }

    protected void init(){
        content = findViewById(R.id.edit_page_content_input);
        title = findViewById(R.id.edit_page_title_input);
        buttonTitle = findViewById(R.id.edit_page_button_title);
        buttonBold = findViewById(R.id.edit_page_button_bold);
        buttonInsertPhoto = findViewById(R.id.edit_page_button_insert_photo);
        buttonInsertLink = findViewById(R.id.edit_page_button_insert_link);
        buttonBack = findViewById(R.id.edit_page_button_back);
        buttonSend = findViewById(R.id.edit_page_button_send);
        spinnerCategory = findViewById(R.id.edit_page_category_spinner);
        editBar = findViewById(R.id.edit_page_edit_bar);
        codeToTextChange = false;
        styleButtonClicked = false;
        titleOn = false;
        boldOn = false;
        relativeSize = 2.0f;
        cursorLast = 0;
        cursorNow = 0;
        setBackStart = 0;
        spinnerChoose = 0;
        queryCursor = null;
        user_id = 0l;

        contentResolver = getContentResolver();
        uri = Uri.parse("content://com.reki.UserLoginInfoContentProvider/t_user_login_info");

        //获取用户信息
        String[] projections = new String[]{"user_id"};
        String selection = "is_login = ?";
        String[] selectionArgs = new String[]{"1"};
        queryCursor = contentResolver.query(uri, projections, selection, selectionArgs, null);
        if(queryCursor.getCount() != 0){
            queryCursor.moveToFirst();
            user_id = queryCursor.getLong(queryCursor.getColumnIndex("user_id"));
        } else{
            //跳转到登录窗口
        }

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //发送博文
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(content.getText().toString().isEmpty() || title.getText().toString().isEmpty()){
                    return;
                } else{
                    String url = baseUrl + "SendBlogServlet";
                    try{
                        JSONObject jsonObject = new JSONObject();
                        //转换富文本信息为json格式
                        JSONArray result = RichTextUtil.toJson(new SpannableStringBuilder(content.getText()));
                        jsonObject.put("user_id", user_id);
                        jsonObject.put("title", title.getText().toString());
                        jsonObject.put("content", result.toString());
                        jsonObject.put("category", spinnerChoose);
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
                                            //跳转到结果页面并提示发送成功
                                            Intent intent = new Intent();
                                            intent.setClass(BlogEditActivity.this, ResultTypeActivity.class);
                                            Bundle bundle = new Bundle();
                                            bundle.putString("result", "发送成功，等待审核中");
                                            intent.putExtras(bundle);
                                            startActivity(intent);
                                            finish();
                                            break;
                                        case "fail":
                                            Toast.makeText(BlogEditActivity.this, "发送失败，请重试", Toast.LENGTH_LONG).show();
                                            break;
                                        default:
                                            break;
                                    }
                                } catch (JSONException e){
                                    Log.e("JSONError", e.toString());
                                }
                            }
                        });
                    } catch (JSONException e){
                        Log.i("JSONParseError", e.toString());
                    }
                }
            }
        });

        //设置博文内容的改变时的监听器
        content.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //获取上一次文本的长度
                contentLengthLast = s.length();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need to watch
            }

            @Override
            public void afterTextChanged(Editable s) {
                //弃用
                /*contentLengthLast = content.getText().length();
                if(!codeToTextChange){
                    SpannableString spannableString = new SpannableString(s);
                    RelativeSizeSpan relativeSizeSpan = new RelativeSizeSpan(relativeSize);
                    StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
                    cursorNow = content.getSelectionStart();
                    if(cursorLast < cursorNow){
                        if(titleOn){
                            if(boldOn){
                                spannableString.setSpan(relativeSizeSpan, cursorLast, cursorNow, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                spannableString.setSpan(styleSpan, cursorLast, cursorNow, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            } else{
                                spannableString.setSpan(relativeSizeSpan, cursorLast, cursorNow, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                        } else {
                            if(boldOn){
                                spannableString.setSpan(styleSpan, cursorLast, cursorNow, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                        }
                        codeToTextChange = true;
                        content.setText(spannableString);
                        content.setSelection(cursorNow);
                        cursorLast = cursorNow;
                        codeToTextChange = false;
                    } else if(cursorLast > cursorNow){
                        Editable contentText = content.getText();
                        ImageSpan[] beforeImageSpan = contentText.getSpans(0, cursorNow, ImageSpan.class);
                        for(int i = 0; i < beforeImageSpan.length; i++){
                            int removeStart = contentText.getSpanStart(beforeImageSpan[i]);
                            int removeEnd = contentText.getSpanEnd(beforeImageSpan[i]);
                            if(cursorNow == removeEnd){
                                codeToTextChange = true;
                                contentText.replace(removeStart, removeEnd, "");
                                cursorNow = cursorNow - (removeEnd - removeStart);
                                content.setSelection(cursorNow);
                                codeToTextChange = false;
                            }
                        }
                        cursorLast = cursorNow;
                    }
                }*/

                //获取当前文本的长度
                contentLengthNow = s.length();
                //删除时，不需要更改文字
                if(contentLengthNow > contentLengthLast){
                    RelativeSizeSpan relativeSizeSpan = new RelativeSizeSpan(relativeSize);
                    StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
                    //当titlebutton和boldbutton点击过并且输入文本时执行
                    if(styleButtonClicked){
                        //如果是要应用样式则应用，并且设置为后续添加的文本也要应用这些样式
                        if(titleOn){
                            s.setSpan(relativeSizeSpan, contentChangeStart, contentChangeStart + (contentLengthNow - contentLengthLast), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                        }
                        if(boldOn){
                            s.setSpan(styleSpan, contentChangeStart, contentChangeStart + (contentLengthNow - contentLengthLast), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                        }
                        //设置添加的文本之前的文本的后续插入文本也要应用样式
                        setSpanFlagToIn(setBackStart, setBackStart);
                    }
                    //设置未点击过titlebutton和boldbutton
                    styleButtonClicked = false;
                }
            }
        });

        //设置当博文内容编辑框获取到焦点时显示富文本编辑的工具条
        content.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus){
                    editBar.setVisibility(View.VISIBLE);
                } else{
                    editBar.setVisibility(View.GONE);
                }
            }
        });

        //设置博文内容点击时的监听器
        //由于MyEditText在光标选择改变时会触发点击事件，所以也会跳转到这里来执行代码
        //并且点击时其实也是光标改变的时候，所以以下统称光标改变时的事件
        content.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //获取光标改变时选择的开始位置和结束位置
                int selectionStart = content.getSelectionStart();
                int selectionEnd = content.getSelectionEnd();
                //获取博文内容
                Editable contentText = content.getText();
                //弃用
                /*if(content.performClicked) {
                    cursorNow = selectionStart;
                    if(contentLengthLast == contentText.length()){
                        cursorLast = selectionStart;
                    }
                    if (cursorLast + 1 < cursorNow) {
                        updateButtonWhenCursorMove(contentText, cursorLast - 1, cursorLast);
                        content.performClicked = false;
                        cursorLast = selectionStart;
                        return;
                    }
                }
                cursorLast = selectionStart;*/
                //获得博文即将输入或删除的开始位置
                contentChangeStart = selectionStart;
                if(selectionStart < selectionEnd){
                    //选择的一段文字
                    //更新editbar的信息
                    updateButtonWhenCursorMove(contentText, selectionStart, selectionEnd);
                } else if(selectionStart >= 1){//before: >=2
                    //只是移动了光标并且不是移动到开头
                    //更新editbar的信息
                    updateButtonWhenCursorMove(contentText, selectionStart - 1, selectionStart);
                } else if(selectionEnd == 0){
                    //光标移动到开头
                    //关闭所有效果
                    titleOn = false;
                    buttonTitle.setImageResource(R.drawable.ic_title_white_24dp);
                    boldOn = false;
                    buttonBold.setImageResource(R.drawable.ic_format_bold_white_24dp);
                    buttonInsertLink.setImageResource(R.drawable.ic_insert_link_white_24dp);
                }
            }
        });

        //设置文字大小
        buttonTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //每点击一次，状态就会改变
                titleOn = !titleOn;
                //设置点击了文字样式按钮
                styleButtonClicked = true;
                //获得博文即将输入或删除的位置
                contentChangeStart = content.getSelectionStart();
                Editable contentText = content.getText();
                RelativeSizeSpan relativeSizeSpan = new RelativeSizeSpan(relativeSize);
                int selectionStart = content.getSelectionStart();
                int selectionEnd = content.getSelectionEnd();
                if(titleOn){
                    //设置样式
                    buttonTitle.setImageResource(R.drawable.ic_title_red_24dp);
                    if(selectionStart < selectionEnd){
                        contentText.setSpan(relativeSizeSpan, selectionStart, selectionEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                    } else if(selectionStart == selectionEnd){
                        //后续输入时要将前面一段文字的样式设置为不会影响后续文字的样式
                        //获取设置和设置回去的开始
                        setBackStart = selectionStart;
                        //设置添加的文本之前的文本的后续插入文本不应用样式
                        setSpanFlagToEx(selectionStart, selectionEnd);
                    }
                } else{
                    //取消样式
                    buttonTitle.setImageResource(R.drawable.ic_title_white_24dp);
                    if(selectionStart < selectionEnd){
                        RelativeSizeSpan[] removeSpans = contentText.getSpans(selectionStart, selectionEnd, RelativeSizeSpan.class);
                        for(int i = 0; i < removeSpans.length; i++){
                            contentText.removeSpan(removeSpans[i]);
                        }
                    } else if(selectionStart == selectionEnd){
                        //同设置样式中的相同部分
                        setBackStart = selectionStart;
                        setSpanFlagToEx(selectionStart, selectionEnd);
                    }
                }
            }
        });

        //设置文字加粗
        //同设置文字大小
        buttonBold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boldOn = !boldOn;
                styleButtonClicked = true;
                contentChangeStart = content.getSelectionStart();
                Editable contentText = content.getText();
                StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
                int selectionStart = content.getSelectionStart();
                int selectionEnd = content.getSelectionEnd();
                if(boldOn){
                    buttonBold.setImageResource(R.drawable.ic_format_bold_red_24dp);
                    if(selectionStart < selectionEnd){
                        contentText.setSpan(styleSpan, selectionStart, selectionEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                    } else if(selectionStart == selectionEnd){
                        setBackStart = selectionStart;
                        setSpanFlagToEx(selectionStart, selectionEnd);
                    }
                } else{
                    Log.e("bold_start_end", "se:" + selectionStart + " " + selectionEnd);
                    buttonBold.setImageResource(R.drawable.ic_format_bold_white_24dp);
                    if(selectionStart < selectionEnd){
                        StyleSpan[] removeSpans = contentText.getSpans(selectionStart, selectionEnd, StyleSpan.class);
                        for(int i = 0; i < removeSpans.length; i++){
                            contentText.removeSpan(removeSpans[i]);
                        }
                    } else if(selectionStart == selectionEnd){
                        setBackStart = selectionStart;
                        setSpanFlagToEx(selectionStart, selectionEnd);
                    }
                }
            }
        });

        //插入图片
        buttonInsertPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //打开开源的图片选择器
                PhotoUtil.init(getApplicationContext(),new GlideIniter());
                PhotoUtil.begin()
                        .setMaxSelectCount(1)
                        .setFromCamera(false)
                        .setNeedCompress(true)
                        .start(BlogEditActivity.this, REQUEST_IMAGE, new PhotoCallback() {
                            @Override
                            public void onFail(String s, Throwable throwable, int i) {
                                Log.e("PhotoError", "failed to select photo\r\n" + s);
                            }

                            @Override
                            public void onSuccessSingle(String s, String s1, int i) {
                                Log.i("PhotoSelectSingle", "compressed path: " + s1);
                                uploadImage(s1);//上传图片
                            }

                            @Override
                            public void onSuccessMulti(List<String> list, List<String> list1, int i) {
                                Log.i("PhotoSelectMulti", "compressed paths: " + list1.toString());
                                for(String path : list1){
                                    uploadImage(path);//上传图片
                                }
                            }

                            @Override
                            public void onCancel(int i) {
                                Log.i("PhotoCancel", "cancel photo select");
                            }
                        });
            }
        });

        //为文字插入链接
        buttonInsertLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonInsertLink.setImageResource(R.drawable.ic_insert_link_white_24dp);
                Editable contentText = content.getText();
                int selectionStart = content.getSelectionStart();
                int selectionEnd = content.getSelectionEnd();
                if(selectionStart < selectionEnd){
                    URLSpan[] removeSpans = contentText.getSpans(selectionStart, selectionEnd, URLSpan.class);
                    if(removeSpans.length != 0){
                        for(int i = 0; i < removeSpans.length; i++){
                            contentText.removeSpan(removeSpans[i]);
                        }
                    } else{
                        //弹出dialog
                        View alertDialogView = View.inflate(BlogEditActivity.this, R.layout.alert_dialog_insert_link_view, null);
                        EditText editText = alertDialogView.findViewById(R.id.alter_dialog_input_link);
                        AlertDialog alertDialog = new AlertDialog.Builder(BlogEditActivity.this)
                                .setTitle("插入链接")
                                .setView(alertDialogView)
                                .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setPositiveButton(R.string.button_insert, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String url = editText.getText().toString();

                                        if(url.isEmpty()){
                                            Toast.makeText(BlogEditActivity.this, "未输入链接", Toast.LENGTH_LONG).show();
                                        } else{
                                            URLSpan urlSpan = new URLSpan(url);
                                            contentText.setSpan(urlSpan, selectionStart, selectionEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                            dialog.dismiss();
                                        }
                                    }
                                })
                                .create();
                        alertDialog.show();
                    }
                } else{
                    Toast.makeText(BlogEditActivity.this, "请选择文字后再插入链接", Toast.LENGTH_LONG).show();
                }
            }
        });

        //选择分类
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] categoryList = getResources().getStringArray(R.array.category);
                switch (categoryList[position]){
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
                        spinnerChoose = 0;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing
            }
        });
    }

    //更新editbar信息
    protected void updateButtonWhenCursorMove(Editable contentText, int start, int end){
        RelativeSizeSpan[] relativeSizeSpan = contentText.getSpans(start, end, RelativeSizeSpan.class);
        if(relativeSizeSpan.length != 0){
            if(relativeSizeSpan[0].getSizeChange() == relativeSize){
                titleOn = true;
                buttonTitle.setImageResource(R.drawable.ic_title_red_24dp);
            }
        } else{
            titleOn = false;
            buttonTitle.setImageResource(R.drawable.ic_title_white_24dp);
        }

        StyleSpan[] beforeStyleSpan = contentText.getSpans(start, end, StyleSpan.class);
        if(beforeStyleSpan.length != 0){
            if(beforeStyleSpan[0].getStyle() == Typeface.BOLD){
                boldOn = true;
                buttonBold.setImageResource(R.drawable.ic_format_bold_red_24dp);
            }
        } else{
            boldOn = false;
            buttonBold.setImageResource(R.drawable.ic_format_bold_white_24dp);
        }

        URLSpan[] beforeURLSpan = contentText.getSpans(start, end, URLSpan.class);
        if(start - 1 >= 0 && start < content.length()){
            URLSpan[] afterBeforeURLSpan = contentText.getSpans(start + 1, end + 1, URLSpan.class);
            if(beforeURLSpan.length != 0 && afterBeforeURLSpan.length != 0){
                buttonInsertLink.setImageResource(R.drawable.ic_insert_link_red_24dp);
            } else{
                buttonInsertLink.setImageResource(R.drawable.ic_insert_link_white_24dp);
            }
        } else if(start == content.length() || start == end){
            buttonInsertLink.setImageResource(R.drawable.ic_insert_link_white_24dp);
        }
    }

    //设置SpanFlag为SPAN_EXCLUSIVE_EXCLUSIVE
    public void setSpanFlagToEx(int selectionStart, int selectionEnd){
        Editable contentText = content.getText();
        StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
        RelativeSizeSpan relativeSizeSpan = new RelativeSizeSpan(relativeSize);
        int spanStart, spanEnd;

        RelativeSizeSpan[] removeRelativeSpans = contentText.getSpans(selectionStart - 1, selectionEnd, RelativeSizeSpan.class);
        if(removeRelativeSpans.length > 0){
            spanStart = contentText.getSpanStart(removeRelativeSpans[0]);
            spanEnd = contentText.getSpanEnd(removeRelativeSpans[0]);
            contentText.removeSpan(removeRelativeSpans[0]);
            Log.e("start_end", "se:" + spanStart + " " + spanEnd);
            contentText.setSpan(relativeSizeSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        StyleSpan[] removeStyleSpans = contentText.getSpans(selectionStart - 1, selectionEnd, StyleSpan.class);
        if(removeStyleSpans.length > 0){
            spanStart = contentText.getSpanStart(removeStyleSpans[0]);
            spanEnd = contentText.getSpanEnd(removeStyleSpans[0]);
            contentText.removeSpan(removeStyleSpans[0]);
            Log.e("start_end", "se:" + spanStart + " " + spanEnd);
            contentText.setSpan(styleSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    //设置文字样式为SPAN_EXCLUSIVE_INCLUSIVE
    public void setSpanFlagToIn(int selectionStart, int selectionEnd){
        Editable contentText = content.getText();
        StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
        RelativeSizeSpan relativeSizeSpan = new RelativeSizeSpan(relativeSize);
        int spanStart, spanEnd;

        RelativeSizeSpan[] removeRelativeSpans = contentText.getSpans(selectionStart - 1, selectionEnd, RelativeSizeSpan.class);
        if(removeRelativeSpans.length > 0){
            spanStart = contentText.getSpanStart(removeRelativeSpans[0]);
            spanEnd = contentText.getSpanEnd(removeRelativeSpans[0]);
            contentText.removeSpan(removeRelativeSpans[0]);
            contentText.setSpan(relativeSizeSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        }

        StyleSpan[] removeStyleSpans = contentText.getSpans(selectionStart - 1, selectionEnd, StyleSpan.class);
        if(removeStyleSpans.length > 0){
            spanStart = contentText.getSpanStart(removeStyleSpans[0]);
            spanEnd = contentText.getSpanEnd(removeStyleSpans[0]);
            contentText.removeSpan(removeStyleSpans[0]);
            contentText.setSpan(styleSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        }
    }

    //上传图片并设置图片
    public void uploadImage(String path){
        String url = baseUrl + "ImageUploadServlet";
        String fileType = "";
        Log.i("path", path);
        final File image = new File(path);

        if(path.contains(".png") || path.contains(".PNG")){
            fileType = "image/png";
        } else if(path.contains(".jpg")|| path.contains(".JPG")
                || path.contains(".jpeg") || path.contains(".JPEG")){
            fileType = "image/jpeg";
        } else if(path.contains(".gif") || path.contains(".GIF")){
            fileType = "image/gif";
        } else{
            Log.e("ImageError", "图片选择错误");
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("id", String.valueOf(user_id));

        OkhttpUtil.okHttpUploadFile(url, image, "image", fileType, params, new CallBackUtil.CallBackJson() {
            @Override
            public void onFailure(Call call, Exception e) {
                Log.e("NetworkError", e.toString());
            }

            @Override
            public void onResponse(JSONObject response) {
                String netImagePath = "";
                try{
                    netImagePath = baseUrl + "img/" + user_id + "/" + response.getString("path");
                    Log.i("imagePath", netImagePath);
                } catch (JSONException e){
                    Log.e("JSONError", e.toString());
                }
                //获取并设置图片
                setNetImage(netImagePath);
            }
        });
    }

    //获取并设置图片
    protected void setNetImage(String netImagePath){
        //请求图片，targetheight为1，则为按targetwidth等比例缩放
        OkhttpUtil.okHttpGetBitmap(netImagePath, new CallBackUtil.CallBackBitmap(1080, 1) {
            @Override
            public void onFailure(Call call, Exception e) {
                Log.e("NetworkError", e.toString());
            }

            @Override
            public void onResponse(Bitmap response) {
                Editable contentText = content.getText();
                ImageSpan imageSpan = new ImageSpan(BlogEditActivity.this, response);
                //图片设置为居中
                AlignmentSpan alignmentSpan = new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER);
                SpannableString spannableString = new SpannableString(netImagePath);
                spannableString.setSpan(imageSpan, 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableString.setSpan(alignmentSpan, 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                Log.i("spannableString", spannableString.toString());
                if(contentText.toString().isEmpty()){
                    contentText.append(spannableString);
                    contentText.append("\r\n");
                    content.setText(contentText);
                    content.setSelection(contentText.length());
                } else{
                    contentText.append("\r\n");
                    contentText.append(spannableString);
                    contentText.append("\r\n");
                    content.setText(contentText);
                    content.setSelection(contentText.length());
                }
            }
        });
    }

    //处理图片返回结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        PhotoUtil.onActivityResult(BlogEditActivity.this, requestCode, resultCode, data);
    }
}
