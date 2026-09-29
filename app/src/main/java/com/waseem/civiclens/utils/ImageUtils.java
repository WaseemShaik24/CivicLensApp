package com.waseem.civiclens.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageUtils {

    public static Bitmap getBitmapFromUri(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap scaleBitmap(Bitmap bitmap, int maxDimension) {
        if (bitmap == null) return null;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float max = Math.max(width, height);
        if (max <= maxDimension) return bitmap;

        float ratio = maxDimension / max;
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";
        Bitmap scaled = scaleBitmap(bitmap, 1024);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    public static Bitmap base64ToBitmap(String base64String) {
        if (base64String == null || base64String.isEmpty()) return null;
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
