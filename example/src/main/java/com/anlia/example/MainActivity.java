package com.anlia.example;

import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.anlia.library.factory.FileFactory;
import com.anlia.library.model.FileModel;
import com.anlia.library.utils.FileSizeUtil;
import com.anlia.library.utils.FileUtil;
import com.anlia.library.utils.SDCardUtil;
import com.anlia.library.utils.ScanDirectoryUtil;
import com.anlia.library.worker.FileCopyWorker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    TextView textPath;
    Button btnInput;
    RecyclerView rvFile;

    private List<FileModel> fileList;
    private FileListAdapter fileListAdapter;

    private String toDir;
    private int hierarchy = 0;

    private static final String TAG = "MainActivity";
    public static final String PREFIX_OTG = "otg:/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FileFactory.init(this);

        textPath = findViewById(R.id.text_path);
        fileList = new ArrayList<>();
        fileListAdapter = new FileListAdapter(this,fileList);
        fileListAdapter.setOnItemClickListener(new FileListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if(ScanDirectoryUtil.canListFiles(fileList.get(position).getFile())){
                    toDir = fileList.get(position).getFile().getPath();
                    textPath.setText(toDir);
                    hierarchy++;
                    Log.e(TAG,"当前文件大小：" + FileSizeUtil.getFileOrFilesSize("/storage/EC95-4FBB/学习/郭静_心墙.mp3",3));
                    Log.e(TAG,"当前存储空间：" + SDCardUtil.getAvailableSize(toDir));
                    refreshFileList(fileList.get(position).getFile());
                }else {
                    Log.e(TAG, FileUtil.readFile(fileList.get(position).getFile()));
//                    Toast.makeText(MainActivity.this, "当前不支持文件查看！", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });
        rvFile = findViewById(R.id.rv_file);
        rvFile.setLayoutManager(new LinearLayoutManager(this));
        rvFile.setAdapter(fileListAdapter);

        btnInput = findViewById(R.id.btn_input);
        btnInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mkFile = new File(toDir + File.separator + "蜂背文档.txt");
                FileFactory.addJob_CopyFile(MainActivity.this)
                        .from("/storage/EC95-4FBB/学习/郭静_心墙.mp3")
                        .to(toDir + File.separator + "郭静_心墙.mp3",true)
                        .execute(new FileCopyWorker.OnFileCopyListener() {
                            @Override
                            public void onSuccess() {
                                Log.e(TAG,"创建文件成功！");
                                refreshFileList(new File(toDir));
                            }

                            @Override
                            public void onFail(int errorCode, String errorInfo) {
                                Log.e(TAG,errorInfo);
                            }
                        });
            }
        });

//        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        FileFactory.addJob_ListenOTG(new FileFactory.OnOTGListener() {
            @Override
            public void onGranted(List<FileModel> fileList) {
                Log.e(TAG,"连接成功");
                for (FileModel model:fileList){

                    Log.e(TAG,"设备列表：" + model.getFileName());
                }
                refreshSDList();
            }

            @Override
            public boolean onAttached(UsbDevice usbDevice) {
                Log.e(TAG,"设备接入");
                if(usbDevice.toString().indexOf("mSerialNumber=acegikm.")!=-1){
                    return true;
                }else {
                    return false;
                }
            }

            @Override
            public void onDetached() {
                Log.e(TAG,"设备拔出");
                initList();
            }

            @Override
            public void onError(int errorCode, String errorInfo) {
                Log.e(TAG, errorInfo);
                initList();
            }
        });
        refreshSDList();
//        for (UsbDevice usbDevice : usbManager.getDeviceList().values()) {
//            if (hasPermission(usbDevice)) {
//                //读取usbDevice里的内容
//                refreshSDList();
//            } else {
//                requestPermission(usbDevice);
//            }
//        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        FileFactory.addJob_DispatchActivityResult(requestCode,resultCode,resultData);
    }

    @Override
    public void onBackPressed() {
        if(hierarchy>1){
            hierarchy--;
            File parentFile = new File(toDir).getParentFile();
            refreshFileList(parentFile);
            toDir = parentFile.getPath();
            textPath.setText(toDir);
        }else if(hierarchy == 1){
            hierarchy--;
            textPath.setText("");
            refreshSDList();
        }else {
            textPath.setText("");
            Toast.makeText(this, "已到达最上层！", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            if(permissionList.size()>0){
//                //modeFlags可以是0或组合FLAG_GRANT_READ_URI_PERMISSION、FLAG_GRANT_WRITE_URI_PERMISSION
//                getContentResolver().releasePersistableUriPermission(permissionList.get(0).getUri(),
//                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
//            }
//        }
    }

    private void initList(){
        hierarchy = 0;
        textPath.setText("");
        fileList.clear();
        fileListAdapter.notifyDataSetChanged();
    }

    private void refreshSDList(){
        fileList.clear();
        FileModel fileModel;
        for (String s: ScanDirectoryUtil.scanSdCard(this)){
            fileModel = new FileModel();
            fileModel.setFileType(ScanDirectoryUtil.FILE_TYPE_SD);
            fileModel.setFile(new File(s));
            fileModel.setFileName(s);
            fileList.add(fileModel);
        }
        fileListAdapter.notifyDataSetChanged();
    }

    private void refreshFileList(File parentFile){
        fileList.clear();
        FileModel fileModel;
        for (File file: ScanDirectoryUtil.scanFiles(parentFile)){
            fileModel = new FileModel();
            fileModel.setFile(file);
            fileModel.setFileType(ScanDirectoryUtil.canListFiles(file)? ScanDirectoryUtil.FILE_TYPE_FOLDER: ScanDirectoryUtil.FILE_TYPE_FILE);
            fileModel.setFileName(file.getName());
            fileList.add(fileModel);
        }
        fileListAdapter.notifyDataSetChanged();
    }
}
