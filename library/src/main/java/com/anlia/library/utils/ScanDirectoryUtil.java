package com.anlia.library.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.os.Build.VERSION.SDK_INT;

/**
 * Created by anlia on 2018/4/23.
 */

public class ScanDirectoryUtil {
    public final static String TAG = "ScanDirectoryUtil";

    public final static String FILE_TYPE_SD = "FILE_TYPE_SD";
    public final static String FILE_TYPE_FOLDER = "FILE_TYPE_FOLDER";
    public final static String FILE_TYPE_FILE = "FILE_TYPE_FILE";

    public static boolean canListFiles(File f) {
        return f.canRead() && f.isDirectory();
    }

    public static File[] scanFiles(File file) {
        return file.listFiles();
    }

    public static List<String> scanSdCard(Context context){
        List<String> list = new ArrayList<>();
        if (SDK_INT >= Build.VERSION_CODES.M && checkStoragePermission(context)){
            list.clear();
        }
        if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String strings[] = scanExtSdCardPaths(context);
            for (String s : strings) {
                File f = new File(s);
                if (!list.contains(s) && canListFiles(f)) {
                    list.add(s);
                }
            }
        }
        if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (isUsbDeviceConnected(context)) {
//                list.add(PREFIX_OTG + "/");
            }
        }
        return list;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String[] scanExtSdCardPaths(Context context) {
        List<String> paths = new ArrayList<>();
        for (File file : context.getExternalFilesDirs("external")) {
            if (file != null && !file.equals(context.getExternalFilesDir("external"))) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index < 0) {
                    Log.w(TAG, "Unexpected external file dir: " + file.getAbsolutePath());
                } else {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    } catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(path);
                }
            }
        }
        if (paths.isEmpty()) paths.add("/storage/sdcard1");
        return paths.toArray(new String[0]);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String[] scanExtSdCardPathsForActivity(Activity activity) {
        List<String> paths = new ArrayList<>();
        for (File file : activity.getExternalFilesDirs("external")) {
            if (file != null) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index < 0) {
                    Log.w(TAG, "Unexpected external file dir: " + file.getAbsolutePath());
                } else {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    } catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(path);
                }
            }
        }
        if (paths.isEmpty()) paths.add("/storage/sdcard1");
        return paths.toArray(new String[0]);
    }

    public static boolean isUsbDeviceConnected(Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager.getDeviceList().size()!=0) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean checkStoragePermission(Context context) {
        // Verify that all required contact permissions have been granted.
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }
}
