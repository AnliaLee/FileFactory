package com.anlia.library.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.support.v4.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by anlia on 2018/4/24.
 */

public class FileUtil {
    private static final String TAG = "FileUtil";

    public static String readFile(File file) {
        StringBuilder result = new StringBuilder("");
        try {
            FileInputStream input = new FileInputStream(file.getPath());
            byte[] temp = new byte[1024];

            int len = 0;
            //读取文件内容:
            while ((len = input.read(temp)) > 0) {
                result.append(new String(temp, 0, len));
            }
            //关闭输入流
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public static boolean mkdirByFile(final File file, Activity context) {
        if (file == null)
            return false;
        if (file.exists()) {
            // nothing toDir create.
            return file.isDirectory();
        }

        // Try the normal way
        if (file.mkdirs()) {
            return true;
        }

        // Try the Kitkat workaround.
//        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
//            try {
//                return MediaStoreHack.mkdirByFile(context, file);
//            } catch (IOException e) {
//                return false;
//            }
//        }
        return false;
    }

    /**
     * 使用DocumentFile进行文件创建，为了兼容Android 5.0以上写入外置sd卡或otg设备
     * @param file
     * @param context
     * @param treeUri
     * @return
     */
    public static boolean mkdirByDocumentFile(final File file, Activity context, Uri treeUri) {
        String baseFolder = getExtSdCardFolder(file, context);
        boolean originalDirectory = false;
        if (baseFolder == null) {
            return false;
        }

        String relativePath = null;
        try {
            String fullPath = file.getCanonicalPath();
            if (!baseFolder.equals(fullPath))
                relativePath = fullPath.substring(baseFolder.length() + 1);
            else originalDirectory = true;
        } catch (IOException e) {
            return false;
        } catch (Exception f) {
            originalDirectory = true;
            //continue
        }

        // start with root of SD card and then parse through document tree.
        DocumentFile document = DocumentFile.fromTreeUri(context, treeUri);
        if (originalDirectory) {
            return document.exists();
        }
        String[] parts = relativePath.split("\\/");
        for (int i = 0; i < parts.length; i++) {
            DocumentFile nextDocument = document.findFile(parts[i]);

            if (nextDocument == null) {
                if ((i < parts.length - 1) || file.isDirectory()) {
                    nextDocument = document.createDirectory(parts[i]);
                } else {
                    nextDocument = document.createFile(getMimeType(file.getPath()), parts[i]);
                }
            }
            document = nextDocument;
        }
        if (document != null) {
            return document.exists();
        } else {
            return false;
        }
    }

    /**
     * 复制文件
     * @param fromFile 源文件
     * @param toDir 目标文件所在目录
     * @param toFile 目标文件
     * @return
     */
    public static boolean copyByFile(File fromFile, String toDir, File toFile) {
        boolean reFlag = false;

        File toDirFile = new File(toDir);
        toDirFile.mkdirs();
        try {
            FileInputStream fis = new FileInputStream(fromFile);
            FileOutputStream fos = new FileOutputStream(toFile);
            byte[] buf = new byte[1024];
            int c;
            while ((c = fis.read(buf)) != -1) {
                fos.write(buf, 0, c);
            }
            fis.close();
            fos.close();
            reFlag = true;
        } catch (IOException e) {
            return false;
        }
        return reFlag;
    }

    /**
     * 使用DocumentFile进行拷贝文件的操作，为了兼容Android 5.0以上写入外置sd卡或otg设备
     * @param fromFile
     * @param toFile
     * @param context
     * @param treeUri
     * @return
     */
    public static boolean copyByDocumentFile(File fromFile, File toFile, Activity context, Uri treeUri) {
        String baseFolder = getExtSdCardFolder(toFile, context);
        boolean originalDirectory = false;
        if (baseFolder == null) {
            return false;
        }

        String relativePath = null;
        try {
            String fullPath = toFile.getCanonicalPath();
            if (!baseFolder.equals(fullPath)) {
                relativePath = fullPath.substring(baseFolder.length() + 1);
            } else {
                originalDirectory = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (Exception f) {
            originalDirectory = true;
            //continue
        }

        // start with root of SD card and then parse through document tree.
        DocumentFile document = DocumentFile.fromTreeUri(context, treeUri);
        if (originalDirectory) {
            return document.exists();
        }
        String[] parts = relativePath.split("\\/");
        for (int i = 0; i < parts.length; i++) {
            DocumentFile nextDocument = document.findFile(parts[i]);

            if (nextDocument == null) {
                if ((i < parts.length - 1) || toFile.isDirectory()) {
                    nextDocument = document.createDirectory(parts[i]);
                } else {
                    nextDocument = document.createFile(getMimeType(fromFile.getPath()), parts[i]);
                    try {
                        FileInputStream fis = new FileInputStream(fromFile);
                        OutputStream out = context.getContentResolver().openOutputStream(nextDocument.getUri());
                        byte[] buf = new byte[1024];
                        int c;
                        while ((c = fis.read(buf)) != -1) {
                            out.write(buf, 0, c);
                        }
                        fis.close();
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
            document = nextDocument;
        }

        if (document != null) {
            return document.exists();
        } else {
            return false;
        }
    }

    public static DocumentFile getDocumentFile(final File file, Activity context, Uri treeUri) {
        String baseFolder = getExtSdCardFolder(file,context);
        boolean originalDirectory=false;
        if (baseFolder == null) {
            return null;
        }

        String relativePath = null;
        try {
            String fullPath = file.getCanonicalPath();
            if(!baseFolder.equals(fullPath))
                relativePath = fullPath.substring(baseFolder.length() + 1);
            else originalDirectory=true;
        }
        catch (IOException e) {
            return null;
        }
        catch (Exception f){
            originalDirectory=true;
            //continue
        }

        // start with root of SD card and then parse through document tree.
        DocumentFile document = DocumentFile.fromTreeUri(context, treeUri);
        if(originalDirectory)return document;
        String[] parts = relativePath.split("\\/");
        for (int i = 0; i < parts.length; i++) {
            DocumentFile nextDocument = document.findFile(parts[i]);
            if (nextDocument == null) {
                if ((i < parts.length - 1) || file.isDirectory()) {
                    if( document.createDirectory(parts[i])==null){
                        return null;
                    }
                    nextDocument = document.createDirectory(parts[i]);
                }
                else {
                    nextDocument = document.createFile(getMimeType(file.getPath()), parts[i]);
                }
            }
            document = nextDocument;
        }

        return document;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isOnExtSdCard(final File file, Context c) {
        return getExtSdCardFolder(file, c) != null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String getExtSdCardFolder(final File file, Context context) {
        String[] extSdPaths = ScanDirectoryUtil.scanExtSdCardPaths(context);
        try {
            for (int i = 0; i < extSdPaths.length; i++) {
                if (file.getCanonicalPath().startsWith(extSdPaths[i])) {
                    return extSdPaths[i];
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    /**
     * 获取文件内容类型
     *
     * @param filePath
     * @return
     */
    public static String getMimeType(String filePath) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        String mime = "text/plain";
        if (filePath != null) {
            try {
                mmr.setDataSource(filePath);
                mime = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            } catch (IllegalStateException e) {
                return mime;
            } catch (IllegalArgumentException e) {
                return mime;
            } catch (RuntimeException e) {
                return mime;
            }
        }
        return mime;
    }

    /**
     * 检测文件是否存在
     * @param file
     * @return
     */
    public static boolean isFileExists(File file){
        if (file.exists() && file.isFile()) {
            return true;
        }else {
            return false;
        }
    }
}
