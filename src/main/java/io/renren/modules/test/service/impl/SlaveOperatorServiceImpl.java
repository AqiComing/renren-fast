package io.renren.modules.test.service.impl;

import io.renren.common.exception.RRException;
import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.service.SlaveOperatorService;
import io.renren.modules.test.service.StressTestParamFileService;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Service("slaveOperatorService")
public class SlaveOperatorServiceImpl implements SlaveOperatorService {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    StressTestParamFileService stressTestParamFileService;

    @Override
    public void updateFileName(long fileId, String newFileName) {
        StressTestFileEntity file = stressTestParamFileService.queryObject(fileId);
        String filePath = file.getFileName();
        File currentFile = new File(filePath);

        if (!currentFile.exists()) {
            throw new RRException("文件不存在。");
        }

        currentFile.renameTo(new File(newFileName));
    }

    @Override
    public void deleteFile(long fileId) {
        StressTestFileEntity file = stressTestParamFileService.queryObject(fileId);
        String filePath = file.getFileName();
        File currentFile = new File(filePath);

        if (!currentFile.exists()) {
            return;
        } else {
            FileUtils.deleteQuietly(currentFile);
        }
    }

    @Override
    public boolean checkFileExists(String path) {
        File file = new File(path);
        return file.exists();
    }
}
