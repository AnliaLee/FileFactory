package com.anlia.library.worker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;

import com.anlia.library.factory.FileFactory;


/**
 * Created by anlia on 2018/6/1.
 */

public class OTGWorker {
    public static void registerReceiver(Context context, BroadcastReceiver receiver){
        IntentFilter otgFilter = new IntentFilter(FileFactory.ACTION_USB_PERMISSION);
        otgFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        otgFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(receiver, otgFilter);
    }
}
