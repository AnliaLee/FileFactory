package com.anlia.library.model;

import java.io.File;

/**
 * Created by anlia on 2018/4/23.
 */

public class FileModel {
    public File file;
    public String fileType;
    public String fileName;

    public File getFile() {
        return file;
    }

    public void setFile(File mFile) {
        this.file = mFile;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
