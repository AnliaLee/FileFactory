package com.anlia.library.factory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;

import com.anlia.library.model.FileModel;
import com.anlia.library.receiver.OTGReceiver;
import com.anlia.library.utils.ScanDirectoryUtil;
import com.anlia.library.worker.FileCopyWorker;
import com.anlia.library.worker.OTGWorker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static android.app.Activity.RESULT_OK;

/**
 * Created by anlia on 2018/6/1.
 */

public class FileFactory {
    private Context mContext;

    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";//自定义U盘读写权限
    public static final int CODE_REQUEST_OPEN_DOCUMENT_TREE = 10001;

    public static FileFactory getInstance() {
        return SingletonHolder.instance;
    }

    private static class SingletonHolder {
        private static final FileFactory instance = new FileFactory();
    }

    public static void init(Context context){
        getInstance().mContext = context;
    }

    public static void addJob_ListenOTG(final OnOTGListener listener){
        if(getInstance().mContext == null){
            throw new NullPointerException("请先初始化FileFactory!");
        }
        OTGWorker.registerReceiver(getInstance().mContext,new OTGReceiver(getInstance().mContext, new OTGReceiver.OnReceiverListener() {
            @Override
            public void onGranted() {
                Observable.create(
                        new ObservableOnSubscribe<List<FileModel>>() {
                            @Override
                            public void subscribe(ObservableEmitter<List<FileModel>> e) throws Exception {
                                List<FileModel> fileList = new ArrayList<>();
                                FileModel fileModel;
                                for (String s: ScanDirectoryUtil.scanSdCard(getInstance().mContext)){
                                    fileModel = new FileModel();
                                    fileModel.setFileType(ScanDirectoryUtil.FILE_TYPE_SD);
                                    fileModel.setFile(new File(s));
                                    fileModel.setFileName(s);
                                    fileList.add(fileModel);
                                }
                                e.onNext(fileList);
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<List<FileModel>>() {
                            @Override
                            public void accept(List<FileModel> list) throws Exception {
                                listener.onGranted(list);
                            }
                        });
            }

            @Override
            public boolean onAttached(UsbDevice usbDevice) {
                return listener.onAttached(usbDevice);
            }

            @Override
            public void onDetached() {
                listener.onDetached();
            }

            @Override
            public void onError(int errorCode, String errorInfo) {
                listener.onError(errorCode,errorInfo);
            }
        }));
    }

    public static FileCopyWorker addJob_CopyFile(Activity context){
        FileCopyWorker.getInstance().init(context);
        return FileCopyWorker.getInstance();
    }

    public static void addJob_DispatchActivityResult(int requestCode, int resultCode, Intent resultData){
        if (resultCode == RESULT_OK) {
            if (requestCode == CODE_REQUEST_OPEN_DOCUMENT_TREE) {
                //授权持久化，对读写权限有进一步要求时使用
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                    getInstance().mContext.getContentResolver().takePersistableUriPermission(resultData.getData(),
//                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
//                }
                FileCopyWorker.getInstance().executeOverLollipop(resultData.getData());
            }
        }
    }

    public interface OnOTGListener {
        /**
         * 设备连接成功时的回调
         * @param fileList 外置存储目录列表
         */
        void onGranted(List<FileModel> fileList);

        /**
         * 设备接入时的回调
         * @param usbDevice 接入的设备
         * @return true：允许该设备连接，继续连接请求的操作
         *         false：不允许该设备连接，进入onError回调
         */
        boolean onAttached(UsbDevice usbDevice);

        /**
         * 设备拔出时的回调
         */
        void onDetached();

        /**
         * 发生错误时的回调
         * @param errorCode
         * @param errorInfo
         */
        void onError(int errorCode, String errorInfo);
    }
}
