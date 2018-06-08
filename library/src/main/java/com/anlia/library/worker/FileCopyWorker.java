package com.anlia.library.worker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import com.anlia.library.factory.FileFactory;
import com.anlia.library.utils.FileDeleteUtil;
import com.anlia.library.utils.FileSizeUtil;
import com.anlia.library.utils.FileUtil;
import com.anlia.library.utils.SDCardUtil;

import java.io.File;
import java.net.URLDecoder;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by anlia on 2018/6/1.
 */

public class FileCopyWorker {
    private Activity mActivity;
    private Uri usbFileTreeUri;
    private File mFromFile;
    private File mToFile;
    private FileFactory.OnFileCopyListener mListener;

    private String mFromPath;
    private String mToDir;
    private String mToPath;
    private boolean mIsCover;//是否覆盖文件
    private boolean mIsDelete = false;//是否成功删除文件

    public static FileCopyWorker getInstance() {
        return SingletonHolder.instance;
    }

    private static class SingletonHolder {
        private static final FileCopyWorker instance = new FileCopyWorker();
    }

    public void init(Activity activity){
        getInstance().mActivity = activity;
    }

    /**
     * 设置源文件
     * @param fromPath 源文件路径
     * @return
     */
    public FileCopyWorker from(String fromPath){
        mFromPath = fromPath;
        return getInstance();
    }

    /**
     * @see #toDir(String, boolean)
     */
    public FileCopyWorker toDir(String toDir){
        return toDir(toDir,false);
    }

    /**
     * 设置复制目标文件夹
     * @param toDir
     * @param isCover 若存在同名文件时是否覆盖
     * @return
     */
    public FileCopyWorker toDir(String toDir, boolean isCover){
        mToDir = toDir;
        mToPath = null;
        mIsCover = isCover;
        return getInstance();
    }

    /**
     * @see #to(String, boolean)
     */
    public FileCopyWorker to(String toPath){
        return to(toPath,false);
    }

    /**
     * 设置复制目标文件
     * @param toPath
     * @param isCover 若存在同名文件时是否覆盖
     * @return
     */
    public FileCopyWorker to(String toPath, boolean isCover){
        mToPath = toPath;
        mToDir = toPath.substring(0,toPath.lastIndexOf(File.separator));
        mIsCover = isCover;
        return getInstance();
    }

    public void execute(FileFactory.OnFileCopyListener listener){
        mListener = listener;
        if(mFromPath == null){
            mListener.onFail(-1,"需调用from方法指定源文件!");
            return;
        }

        if(mToDir == null){
            mListener.onFail(-2,"需调用toDir方法设置目标目录!");
            return;
        }

        mFromFile = new File(mFromPath);
        if (!mFromFile.exists()) {
            mListener.onFail(-3,"源文件不存在!");
            return;
        }

        try {
            if(FileSizeUtil.getFileOrFilesSize(mFromPath,3) > SDCardUtil.getAvailableSize(mToDir)){
                mListener.onFail(-4,"空间不足!");
                return;
            }
        }catch (Exception e){
            e.printStackTrace();
            mListener.onFail(-99,"发生未知错误!");
            return;
        }

        // 获取待复制文件的文件名
        if(mToPath == null){
            String fromName = mFromPath.substring(mFromPath.lastIndexOf(File.separator));
            mToPath = mToDir + File.separator + fromName;
        }
        mToFile = new File(mToPath);
        if (FileUtil.isFileExists(mToFile) && !mIsCover) {
            mListener.onFail(-5,"目标目录下已存在同名文件!");
            return;
        }

        Observable.create(
                new ObservableOnSubscribe<Boolean>() {
                    @Override
                    public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                        if (FileUtil.isFileExists(mToFile) && mIsCover) {
                            mIsDelete = FileDeleteUtil.deleteFile(mToPath);
                        }
                        e.onNext(FileUtil.copyByFile(mFromFile,mToDir,mToFile));
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean isComplete) throws Exception {
                        if(isComplete){
                            mListener.onSuccess();
                        }else {
                            // 5.0以上需要用户对指定的文件夹进行访问授权，获取权限后使用DocumentFile对文件进行操作
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && FileUtil.isOnExtSdCard(mToFile, mActivity)) {
                                if(usbFileTreeUri != null){
                                    executeOverLollipop(usbFileTreeUri);
                                }else {
                                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                                    mActivity.startActivityForResult(intent, FileFactory.CODE_REQUEST_OPEN_DOCUMENT_TREE);
                                }
                            }
                        }
                    }
                });
    }

    /**
     * 兼容Android 5.0以上
     * @param uri
     * {@hide}
     */
    public void executeOverLollipop(final Uri uri){
        if(mListener == null){
            return;
        }

        String toDirString = mToDir.replace("/storage/","")
                .replace("/","");

        String uriString = URLDecoder.decode(uri.toString()).replace("content://com.android.externalstorage.documents/tree/","");

        String rootDir = uriString.substring(0,uriString.indexOf(":"));

        uriString = uriString.replace("content://com.android.externalstorage.documents/tree/","")
                .replace(":","")
                .replace("/","")
                + toDirString.replace(rootDir,"");

        if(!uriString.equals(toDirString)){
            switch (mListener.onPathDifference()){
                case FileFactory.EVENT_TO_FAIL:
                    mListener.onFail(-6,"创建文件失败！");
                    return;
                case FileFactory.EVENT_TO_RESELECT:
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    mActivity.startActivityForResult(intent, FileFactory.CODE_REQUEST_OPEN_DOCUMENT_TREE);
                    return;
                case FileFactory.EVENT_TO_CONTINUE:
                    break;
                default:
                    mListener.onFail(-7,"动作错误！");
                    return;
            }
        }

        usbFileTreeUri = uri;//保存TreeUri，下次无需再申请
        Observable.create(
                new ObservableOnSubscribe<Boolean>() {
                    @Override
                    public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                        if (FileUtil.isFileExists(mToFile) && mIsCover && !mIsDelete) {
                            mIsDelete = FileDeleteUtil.deleteDocumentFile(mToFile, mActivity,uri);
                        }
                        e.onNext(FileUtil.copyByDocumentFile(mFromFile,mToFile, mActivity,uri));
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean isComplete) throws Exception {
                        if(isComplete){
                            mListener.onSuccess();
                        }else {
                            mListener.onFail(-8,"创建文件失败！");
                        }
                    }
                });
    }
}
