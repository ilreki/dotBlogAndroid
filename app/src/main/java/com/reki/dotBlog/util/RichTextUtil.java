package com.reki.dotBlog.util;

import android.app.Activity;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.Log;

import com.reki.dotBlog.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*
 *
 * 类名：RichTextUtil
 * 创建者：Vnshng
 * 描述：SpannableStringBuilder和JSONArray的转换工具类
 *
 */
public class RichTextUtil {
    private static String param;
    private static int start, end;
    private static JSONArray resultArray, realImageArray;
    private static SpannableStringBuilder resultSpannableString;

    /*
     *
     * 函数名：toJson
     * 创建者：Vnshng
     * 描述：将SpannableStringBuilder转换为JsonArray
     *
     */
    public static JSONArray toJson(SpannableStringBuilder spannableStringBuilder){
        int length= spannableStringBuilder.length();
        JSONObject tempObject = new JSONObject();
        resultArray = new JSONArray();

        try{
            tempObject.put("type", "total");
            tempObject.put("data", spannableStringBuilder.toString());
            resultArray.put(tempObject);

            relativeSizeSpanToJson(spannableStringBuilder, length);
            styleSpanToJson(spannableStringBuilder, length);
            urlSpanToJson(spannableStringBuilder, length);
            alignmentSpanToJson(spannableStringBuilder, length);
            imageSpanToJson(spannableStringBuilder, length);
        } catch (JSONException e){
            Log.i("JSONParseError: ", e.toString());
        }
        return  resultArray;
    }

    /*
     *
     * 函数名：relativeSizeSpanToJson
     * 创建者：Vnshng
     * 描述：将RelativeSizeSpan转换为JSONObject
     *
     */
    private static void relativeSizeSpanToJson(SpannableStringBuilder spannableStringBuilder, int length) throws JSONException{
        RelativeSizeSpan[] relativeSizeSpans =
                spannableStringBuilder.getSpans(0, length, RelativeSizeSpan.class);

        for(int i = 0; i < relativeSizeSpans.length; i++){
            int start = spannableStringBuilder.getSpanStart(relativeSizeSpans[i]);
            int end = spannableStringBuilder.getSpanEnd(relativeSizeSpans[i]);
            JSONObject tempObject = new JSONObject();

            tempObject.put("type", "relativeSizeText");
            tempObject.put("param", String.valueOf(relativeSizeSpans[i].getSizeChange()));
            tempObject.put("start", start);
            tempObject.put("end", end);

            resultArray.put(tempObject);
        }
    }

    /*
     *
     * 函数名：styleSpanToJson
     * 创建者：Vnshng
     * 描述：将StyleSpan转换为JSONObject
     *
     */
    private static void styleSpanToJson(SpannableStringBuilder spannableStringBuilder, int length) throws JSONException{
        StyleSpan[] styleSpans =
                spannableStringBuilder.getSpans(0, length, StyleSpan.class);

        for(int i = 0; i < styleSpans.length; i++){
            int start = spannableStringBuilder.getSpanStart(styleSpans[i]);
            int end = spannableStringBuilder.getSpanEnd(styleSpans[i]);
            JSONObject tempObject = new JSONObject();

            tempObject.put("type", "styleText");
            tempObject.put("param", String.valueOf(styleSpans[i].getStyle()));
            tempObject.put("start", start);
            tempObject.put("end", end);

            resultArray.put(tempObject);
        }
    }

    /*
     *
     * 函数名：urlSpanToJson
     * 创建者：Vnshng
     * 描述：将URLSpan转换为JSONObject
     *
     */
    private static void urlSpanToJson(SpannableStringBuilder spannableStringBuilder, int length) throws JSONException{
        URLSpan[] urlSpans =
                spannableStringBuilder.getSpans(0, length, URLSpan.class);

        for(int i = 0; i < urlSpans.length; i++){
            int start = spannableStringBuilder.getSpanStart(urlSpans[i]);
            int end = spannableStringBuilder.getSpanEnd(urlSpans[i]);
            JSONObject tempObject = new JSONObject();

            tempObject.put("type", "linkText");
            tempObject.put("param", String.valueOf(urlSpans[i].getURL()));
            tempObject.put("start", start);
            tempObject.put("end", end);

            resultArray.put(tempObject);
        }
    }

    /*
     *
     * 函数名：imageSpanToJson
     * 创建者：Vnshng
     * 描述：将ImageSpan转换为JSONObject
     *
     */
    private static void imageSpanToJson(SpannableStringBuilder spannableStringBuilder, int length) throws JSONException{
        ImageSpan[] imageSpans =
                spannableStringBuilder.getSpans(0, length, ImageSpan.class);

        for(int i = 0; i < imageSpans.length; i++){
            int start = spannableStringBuilder.getSpanStart(imageSpans[i]);
            int end = spannableStringBuilder.getSpanEnd(imageSpans[i]);
            JSONObject tempObject = new JSONObject();

            tempObject.put("type", "image");
            tempObject.put("param", spannableStringBuilder.subSequence(start, end).toString());
            tempObject.put("start", start);
            tempObject.put("end", end);

            resultArray.put(tempObject);
        }
    }

    /*
     *
     * 函数名：alignmentSpanToJson
     * 创建者：Vnshng
     * 描述：将AlignmentSpan转换为JSONObject
     *
     */
    private static void alignmentSpanToJson(SpannableStringBuilder spannableStringBuilder, int length) throws JSONException{
        AlignmentSpan[] alignmentSpans =
                spannableStringBuilder.getSpans(0, length, AlignmentSpan.class);

        for(int i = 0; i < alignmentSpans.length; i++){
            int start = spannableStringBuilder.getSpanStart(alignmentSpans[i]);
            int end = spannableStringBuilder.getSpanEnd(alignmentSpans[i]);
            JSONObject tempObject = new JSONObject();

            tempObject.put("type", "alignmentText");
            switch (alignmentSpans[i].getAlignment()){
                case ALIGN_CENTER:
                    tempObject.put("param", "center");
                    break;
                case ALIGN_NORMAL:
                    tempObject.put("param", "normal");
                    break;
                case ALIGN_OPPOSITE:
                    tempObject.put("param", "opposite");
                    break;
                default:
                    return;
            }
            tempObject.put("start", start);
            tempObject.put("end", end);

            resultArray.put(tempObject);
        }
    }

    /*
     *
     * 函数名：fromJson
     * 创建者：Vnshng
     * 描述：将JsonArray转换为SpannableStringBuilder
     *
     */
    public static JSONObject fromJson(Activity activity, JSONArray jsonArray){
        JSONObject resultObject = new JSONObject();
        realImageArray = new JSONArray();
        try{
            JSONObject total = jsonArray.getJSONObject(0);
            String type = total.getString("type");
            if(type.equals("total")){
                resultSpannableString = new SpannableStringBuilder(total.getString("data"));
                Log.i("total", "data: " + resultSpannableString.toString());
            }
            for(int i = 1; i < jsonArray.length(); i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                type = jsonObject.getString("type");
                switch (type){
                    case "relativeSizeText":
                        applyRelativeSizeSpanFromJson(jsonObject);
                        break;
                    case "styleText":
                        applyStyleSpanFromJson(jsonObject);
                        break;
                    case "linkText":
                        applyURLSpanFromJson(jsonObject);
                        break;
                    case "alignmentText":
                        applyAlignmentSpanFromJson(jsonObject);
                        break;
                    case "image":
                        applyLoadingImageSpanFromJson(activity, jsonObject);
                        break;
                    default:
                        break;
                }
            }
            resultObject.put("result", resultSpannableString);
            resultObject.put("realImageArray", realImageArray);
        } catch (JSONException e){
            Log.e("JSONParseError", e.toString());
        }
        return resultObject;
    }

    /*
     *
     * 函数名：applyRelativeSizeSpanFromJson
     * 创建者：Vnshng
     * 描述：将JSONObject转换为RelativeSizeSpan，并应用到resultSpannableString
     *
     */
    private static void applyRelativeSizeSpanFromJson(JSONObject jsonObject) throws JSONException{
        float relativeSize;
        param = jsonObject.getString("param");
        start = jsonObject.getInt("start");
        end = jsonObject.getInt("end");

        relativeSize = Float.parseFloat(param);
        RelativeSizeSpan relativeSizeSpan = new RelativeSizeSpan(relativeSize);
        resultSpannableString.setSpan(relativeSizeSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /*
     *
     * 函数名：applyStyleSpanFromJson
     * 创建者：Vnshng
     * 描述：将JSONObject转换为StyleSpan，并应用到resultSpannableString
     *
     */
    private static void applyStyleSpanFromJson(JSONObject jsonObject) throws JSONException{
        int style;
        param = jsonObject.getString("param");
        start = jsonObject.getInt("start");
        end = jsonObject.getInt("end");

        style = Integer.parseInt(param);
        StyleSpan styleSpan = new StyleSpan(style);
        resultSpannableString.setSpan(styleSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /*
     *
     * 函数名：applyURLSpanFromJson
     * 创建者：Vnshng
     * 描述：将JSONObject转换为URLSpan，并应用到resultSpannableString
     *
     */
    private static void applyURLSpanFromJson(JSONObject jsonObject) throws JSONException{
        String url;
        param = jsonObject.getString("param");
        start = jsonObject.getInt("start");
        end = jsonObject.getInt("end");

        url = param;
        URLSpan urlSpan = new URLSpan(url);
        resultSpannableString.setSpan(urlSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /*
     *
     * 函数名：applyImageSpanFromJson
     * 创建者：Vnshng
     * 描述：将JSONObject转换为ImageSpan，并应用到resultSpannableString
     * 说明：实际图片应在UI线程更新，以获得更好的用户体验
     *
     */
    private static void applyLoadingImageSpanFromJson(Activity activity, JSONObject jsonObject) throws JSONException{
        start = jsonObject.getInt("start");
        end = jsonObject.getInt("end");

        setLoadingImage(activity, start, end);
        realImageArray.put(jsonObject);
    }

    /*
     *
     * 函数名：getNetImage
     * 创建者：Vnshng
     * 描述：设置默认图片
     *
     */
    private static void setLoadingImage(Activity activity, int start, int end) {
        ImageSpan imageSpan = new ImageSpan(activity.getDrawable(R.drawable.ic_image_green_200dp));
        resultSpannableString.setSpan(imageSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /*
     *
     * 函数名：applyAlignmentSpanFromJson
     * 创建者：Vnshng
     * 描述：将JSONObject转换为AlignmentSpan，并应用到resultSpannableString
     *
     */
    private static void applyAlignmentSpanFromJson(JSONObject jsonObject) throws JSONException{
        param = jsonObject.getString("param");
        start = jsonObject.getInt("start");
        end = jsonObject.getInt("end");

        AlignmentSpan alignmentSpan;
        switch (param){
            case "center":
                alignmentSpan = new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER);
                break;
            case "normal":
                alignmentSpan = new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL);
                break;
            case "opposite":
                alignmentSpan = new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE);
                break;
            default:
                return;
        }
        resultSpannableString.setSpan(alignmentSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
