package io.renren.modules.test.service;

public interface SlaveOperatorService {

    /**
     * 更新slave机器文件名字
     * @param fileId
     * @param newFileName
     */
    void updateFileName(long fileId, String newFileName);

    void deleteFile(long fileId);

    boolean checkFileExists(String path);
}
