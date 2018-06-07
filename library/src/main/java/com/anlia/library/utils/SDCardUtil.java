package com.anlia.library.utils;

import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.text.TextUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * SD卡公共工具类
 */
public class SDCardUtil {
	public static final String TAG = "SDCardUtil";


	/**
	 * 获取所有SD卡根路径列表
	 * @return
	 */
	public static List<String> getSDCardRootPathList(Context context){
		List<String> pathList = getWritableVolumePaths(context);
		final String defaultPath = getDefaultSDCardRootPath(context);

		if (pathList.isEmpty()) {
			if (!TextUtils.isEmpty(defaultPath)) {
				pathList.add(defaultPath);
			}
		}
		return pathList;
	}
	
	/**
	 * 获取所有SD卡根路径列表
	 * @return
	 */
	public static List<String> getSDCardRootPathList(Context context, String filePath){
		List<String> pathList = getWritableVolumePaths(context,filePath);
		final String defaultPath = getDefaultSDCardRootPath(context);
		
		if (pathList.isEmpty()) {
			if (!TextUtils.isEmpty(defaultPath)) {
				pathList.add(defaultPath);
			}
		}
		return pathList;
	}
	
	/**
	 * 获取默认SD卡根路径
	 * @return
	 */
	@SuppressWarnings("unused")
	public static String getDefaultSDCardRootPath(Context context) {
		File file = new File(Environment
				.getExternalStorageDirectory().getAbsolutePath() + "/");
		if (file != null) {
			List<String> pathList = getWritableVolumePaths(context);
			String path = file.getAbsolutePath();
			for (String aPath : pathList) {
				if (path.startsWith(aPath)) {
					return aPath;
				}
			}
		} else {
			if (Environment.getExternalStorageDirectory() == null) {
				return "";
			} else {
				return Environment.getExternalStorageDirectory().getPath();
			}
		}
		return "";
	}

	/**
	 * 反射调用api，获取所有存储器路径列表
	 * 获取sdcard的路径：外置和内置
	 */
	private static List<String> getWritableVolumePaths(Context context) {
		String[] paths = null;
		try {
			StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
			paths = (String[]) sm.getClass().getMethod("getVolumePaths", new Class[0]).invoke(sm, new Object[]{});
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {

		}

		List<String> writablePaths = new ArrayList<String>();
		if (paths != null && paths.length > 0) {
			for (String path : paths) {
				File file = new File(path);
				if (canWrite(file) && file.canRead() && getTotalSize(path) > 0) {
					writablePaths.add(path);
				}
			}
		}
		return writablePaths;
	}
	
	/**
	 * 反射调用api，获取所有存储器路径列表
	 * 获取sdcard的路径：外置和内置
	 */
	private static List<String> getWritableVolumePaths(Context context, String filePath) {
		String[] paths = null;
		try {
			StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
			paths = (String[]) sm.getClass().getMethod("getVolumePaths", new Class[0]).invoke(sm, new Object[]{});
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {

		}

		List<String> writablePaths = new ArrayList<String>();
		if (paths != null && paths.length > 0) {
			for (String path : paths) {
				String path2 = path + filePath;
				File file = new File(path2);
				if (canWrite(file) && file.canRead() && getTotalSize(path2) > 0) {
					writablePaths.add(path);
				}
			}
		}
		return writablePaths;
	}
	
	public static boolean canWrite(final File file) {
		if (file == null || !file.exists()) {
			return false;
		}

		boolean result;
		result = file.canWrite();
		return result;
	}


	/**
	 * 获取磁盘空间信息
	 * @param stringFormat  总共%s，剩余%s
	 * @param formatType    类型-1--总共%s,2--剩余%s,3--总共+剩余，4--剩余+总共
	 * @return
	 */
	public static String getDiskInfo(final String pathRoot, final String stringFormat, final int formatType) {
		if (TextUtils.isEmpty(stringFormat)) {
			return "";
		}
		
		String info = "";
		try {
			android.os.StatFs statfs = new android.os.StatFs(pathRoot);
			// 获取SDCard上每个block的SIZE
			long nBlocSize = statfs.getBlockSize();
			// 获取可供程序使用的Block的数量
			long nAvailaBlock = statfs.getAvailableBlocks();
			// 计算 SDCard 剩余大小B
			double strSDFreeSize = (nAvailaBlock * nBlocSize)/(1024.0*1024*1024);
			// 获取所有Block的数量
			long nTotalBlock = statfs.getBlockCount();
			// 计算 SDCard 总大小B
			double strSDTotalSize = (nTotalBlock * nBlocSize)/(1024.0*1024*1024);
			
			switch (formatType) {
			case 1:
				info = String.format(stringFormat, String.format("%.1f", strSDTotalSize));
				break;
			case 2:
				info = String.format(stringFormat, String.format("%.1f", strSDFreeSize));
				break;
			case 3:
				info = String.format(stringFormat, String.format("%.1f", strSDTotalSize), String.format("%.1f", strSDFreeSize));
				break;
			case 4:
				info = String.format(stringFormat, String.format("%.1f", strSDFreeSize), String.format("%.1f", strSDTotalSize));
				break;
			default:
				return "";
			}
		} catch (Exception e) {
			
		}
		return info;
	}

	/**
	 * 获取磁盘总大小
	 * @param pathRoot
	 * @return
	 */
	public static long getTotalSize(final String pathRoot) {
		try {
			android.os.StatFs statfs = new android.os.StatFs(pathRoot);
			// 获取SDCard上每个block的SIZE
			long nBlocSize = statfs.getBlockSize();
			// 获取总共的Block的数量
			long nTotalBlock = statfs.getBlockCount();
			return (nTotalBlock * nBlocSize);
		} catch (Exception e) {
			
		}
		return 0;
	}

	/**
	 * 获取文件夹大小
	 * @param file File实例
	 * @return long 单位为M
	 * @throws Exception
	 */
	public static long getFolderSizeMB(File file)throws Exception {
		long size = 0;
		File[] fileList = file.listFiles();
		for (int i = 0; i < fileList.length; i++)
		{
			if (fileList[i].isDirectory())
			{
				size = size + getFolderSizeMB(fileList[i]);
			} else
			{
				size = size + fileList[i].length();
			}
		}
		return size/1048576;
	}

	/**
	 * 获取剩余空间
	 * @param pathRoot
	 * @return
	 */
	public static double getAvailableSize(final String pathRoot) {
		try {
			android.os.StatFs statfs = new android.os.StatFs(pathRoot);
			long nBlocSize = statfs.getBlockSize();
			long nAvailaBlock = statfs.getAvailableBlocks();
			// 计算 SDCard 剩余大小MB
			long left = (nAvailaBlock * nBlocSize)/1024/1024;
			return left;
		} catch (Exception e) {
			return 0;
		}
	}


	/**
	 * SD卡是否可用
	 * @return
	 */
	public static boolean isSDCardMounted() {
		String sDStateString = Environment.getExternalStorageState();
		if (sDStateString.equals(Environment.MEDIA_MOUNTED)) {
			return true;
		} else {
			return false;
		}
	}


	/**
	 * SD卡是否只读
	 * @return
	 */
	public static boolean isSDCardMountedReadOnly() {
		String sDStateString = Environment.getExternalStorageState();
		if (sDStateString
				.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 获取SDCard总大小和剩余大小,单位MB
	 * @return
	 */
	public static HashMap<String, Long> getSDCardSizeInfo() {

		HashMap<String, Long> map = new HashMap<String, Long>();

		String sDcString = Environment.getExternalStorageState();
		if (sDcString.equals(Environment.MEDIA_MOUNTED)) {
			File pathFile = Environment
					.getExternalStorageDirectory();
			android.os.StatFs statfs = new android.os.StatFs(pathFile.getPath());
			long nTotalBlocks = statfs.getBlockCount();
			long nBlocSize = statfs.getBlockSize();
			long nAvailaBlock = statfs.getAvailableBlocks();
			long nSDTotalSize = nTotalBlocks * nBlocSize / 1024 / 1024;
			long nSDFreeSize = nAvailaBlock * nBlocSize / 1024 / 1024;
			map.put("totalsize", nSDTotalSize);
			map.put("freesize", nSDFreeSize);
		}
		return map;

	}

	public static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			//递归删除目录中的子目录下
			for (int i=0; i<children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		// 目录此时为空，可以删除
		return dir.delete();
	}
	
}
