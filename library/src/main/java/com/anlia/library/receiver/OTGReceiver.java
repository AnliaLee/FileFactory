package com.anlia.library.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.anlia.library.factory.FileFactory;

/**
 * Created by anlia on 2018/6/1.
 */

public class OTGReceiver extends BroadcastReceiver {
    private Context mContext;
    private UsbManager usbManager;
    private OnReceiverListener mListener;

    private boolean isGranted = true;

    public OTGReceiver(Context context, OnReceiverListener listener) {
        mContext = context;
        usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(FileFactory.ACTION_USB_PERMISSION)) {
            if(!isGranted){
                return;
            }
            synchronized (this) {
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {//获取设备权限
                    if (usbDevice != null) {
                        mListener.onGranted();
                    }else {
                        mListener.onError(-1,"获取USB设备失败");
                    }
                } else {
                    mListener.onError(-2,"获取USB设备权限失败");
                }
            }
        } else if (action.equals(UsbManager.ACTION_USB_ACCESSORY_DETACHED)) {//设备连接中断
            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (usbDevice != null) {
                mListener.onError(-3,"USB设备连接中断");
            }
        }else if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)){//设备接入
            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if(mListener.onAttached(usbDevice)){
                if (hasPermission(usbDevice)) {
                    mListener.onGranted();
                } else {
                    requestPermission(usbDevice);
                }
            }else {
                isGranted = false;
                mListener.onError(-4,"连接的设备不符合要求");
            }
        }else if(action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)){//设备拔出
            mListener.onDetached();
        }
    }

    public boolean hasPermission(UsbDevice device) {
        return usbManager.hasPermission(device);
    }

    public void requestPermission(UsbDevice usbDevice) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(FileFactory.ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(usbDevice, pendingIntent);
    }

    public interface OnReceiverListener {
        void onGranted();
        boolean onAttached(UsbDevice usbDevice);
        void onDetached();
        void onError(int errorCode, String errorInfo);
    }
}
