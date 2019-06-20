package com.reki.dotBlog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toolbar;

import com.hss01248.glidepicker.GlideIniter;
import com.hss01248.photoouter.PhotoCallback;
import com.hss01248.photoouter.PhotoUtil;
import com.reki.dotBlog.util.CallBackUtil;
import com.reki.dotBlog.util.OkhttpUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

public class TestOkHttpActivity extends AppCompatActivity {

    EditText userNameInput;
    EditText passwordInput;
    ImageButton buttonSend;
    TextView result;
    ImageView imageView;
    ImageButton imageButton;
    Button buttonChangeToTestSpanActivity;
    String baseUrl = "http://192.168.1.104:8080/dotBlog/";
    final int REQUEST_IMAGE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_ok_http);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);

        init();
        myRequestPermission();
    }

    public void init() {

        userNameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        buttonSend = findViewById(R.id.button_send);
        result = findViewById(R.id.result);
        imageView = findViewById(R.id.image_view);
        imageButton = findViewById(R.id.upload_image_button);
        buttonChangeToTestSpanActivity = findViewById(R.id.button_change_to_test_span_activity);

        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                String url = "http://reki.free.idcfengye.com/testAndroid/loginServlet";
                String url = baseUrl + "loginServlet";
                String jsonStr = "";
                JSONObject jsonObject = new JSONObject();

                try{
                    jsonObject.put("name", userNameInput.getText().toString());
                    jsonObject.put("password", passwordInput.getText().toString());
                }catch (JSONException e){
                    Log.e("JSONError", e.toString());
                }

                jsonStr = jsonObject.toString();

                OkhttpUtil.okHttpPostJson(url, jsonStr, new CallBackUtil.CallBackJson() {
                    @Override
                    public void onFailure(Call call, Exception e) {
                        Log.e("NetworkError", e.toString());
                    }

                    @Override
                    public void onResponse(JSONObject response) {
                        String resultStr = "";
                        try{
                            resultStr = response.getString("info");
                        } catch (JSONException e){
                            Log.e("JSONError", e.toString());
                        }
                        result.setText(resultStr);
                    }
                });
            }
        });

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                MultiImageSelector.create()
//                        .showCamera(false)
//                        .single()
//                        .start(TestOkHttpActivity.this, REQUEST_IMAGE);
                PhotoUtil.init(getApplicationContext(),new GlideIniter());
                PhotoUtil.begin()
                        .setFromCamera(false)
                        .setMaxSelectCount(1)
                        .setNeedCropWhenOne(true)
                        .setNeedCompress(true)
                        .setCropMuskOval()
                        .start(TestOkHttpActivity.this, REQUEST_IMAGE, new PhotoCallback() {
                            @Override
                            public void onFail(String s, Throwable throwable, int i) {
                                Log.e("PhotoError", "failed to select photo/r/n" + throwable.toString());
                            }

                            @Override
                            public void onSuccessSingle(String s, String s1, int i) {
                                Log.i("PhotoSelectSingle", "compressed path: " + s1);
                                uploadImage(s1);
                            }

                            @Override
                            public void onSuccessMulti(List<String> list, List<String> list1, int i) {
                                Log.i("PhotoSelectMulti", "compressed paths: " + list1.toString());
                                for(String path : list1){
                                    uploadImage(path);
                                }
                            }

                            @Override
                            public void onCancel(int i) {
                                Log.i("PhotoCancel", "cancel photo select");
                            }
                        });
            }
        });

        buttonChangeToTestSpanActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(TestOkHttpActivity.this, BlogEditActivity.class);
                startActivity(intent);
            }
        });
    }

    protected void myRequestPermission(){
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
            List<String> permissions = new ArrayList<String>();
            List<String> removePerm = new ArrayList<String>();

            permissions.add(Manifest.permission.CAMERA);
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            for(String permission : permissions){
                int permissionOk = ActivityCompat.checkSelfPermission(getApplication(), permission);
                    if(permissionOk == PackageManager.PERMISSION_GRANTED){
                        removePerm.add(permission);
                }
            }
            if(!removePerm.isEmpty()){
                for(String perm : removePerm){
                    permissions.remove(perm);
                }
            }
            if(!permissions.isEmpty()){
                ActivityCompat.requestPermissions(TestOkHttpActivity.this,
                        permissions.toArray(new String[permissions.size()]), 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        PhotoUtil.onActivityResult(TestOkHttpActivity.this, requestCode, resultCode, data);
//        if(requestCode == REQUEST_IMAGE){
//            if(resultCode == RESULT_OK){
//
//                // 获取返回的图片列表
////                List<String> paths = data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT);
////                for(String path : paths){
////                    uploadImage(path);
////                }
//            }
//        }
    }

    public void uploadImage(String path){
        String url = baseUrl + "imageUploadServlet";
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
        params.put("name","reki");

        OkhttpUtil.okHttpUploadFile(url, image, "image", fileType, params, new CallBackUtil.CallBackJson() {
            @Override
            public void onFailure(Call call, Exception e) {
                Log.e("NetworkError", e.toString());
            }

            @Override
            public void onResponse(JSONObject response) {
                String resultStr = "";
                imageView.setImageURI(Uri.fromFile(image));
                try{
                    resultStr = response.getString("path");
                } catch (JSONException e){
                    Log.e("JSONError", e.toString());
                }
                result.setText(resultStr);
            }
        });
    }
}
