package io.renren.modules.test.service;

import io.renren.modules.test.entity.StressTestEntity;
import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.entity.StressTestReportsEntity;
import io.renren.modules.test.jmeter.JmeterRunEntity;
import io.renren.modules.test.jmeter.JmeterStatEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 性能测试压测文件
 */
public interface StressTestParamFileService {

    /**
     * 根据ID，查询文件
     */
    StressTestFileEntity queryObject(Long fileId);

    /**
     * 查询文件列表
     */
    List<StressTestFileEntity> queryList(Map<String, Object> map);

    /**
     * 查询master机器上文件列表
     */
    List<StressTestFileEntity> queryMasterFile(Map<String, Object> map);

    /**
     * 查询master机器上文件列表
     */
    List<StressTestFileEntity> querySlaveFile(Long masterId);

    /**
     * 查询文件列表
     */
    List<StressTestFileEntity> queryParamFileList(Long caseId);

    /**
     * 查询总数
     */
    int queryTotal(Map<String, Object> map);

    /**
     * 保存性能测试用例文件
     */
    void save(StressTestFileEntity stressCaseFile);

    /**
     * 保存性能测试用例文件
     */
    void save(MultipartFile file, String filePath, StressTestEntity stressCase, StressTestFileEntity stressCaseFile);

    /**
     * 更新性能测试用例信息
     */
    void update(StressTestFileEntity stressTestFile);

    /**
     * 更新性能测试用例信息
     */
    void update(StressTestFileEntity stressTestFile, StressTestReportsEntity stressTestReports);

    /**
     * 更新性能测试用例信息
     */
    void update(MultipartFile file, String filePath, StressTestEntity stressCase, StressTestFileEntity stressCaseFile);

    /**
     * 批量删除
     */
    void deleteBatch(Object[] fileIds, String token);

    /**
     * 删除指定节点文件
     */
    void deleteSlaveFile(Long fileId, Long slaveId, String token);

    /**
     * 同步参数化文件到节点机
     */
    void synchronizeFile(Long[] fileIds, String token);

    /**
     * 同步参数化文件到指定节点机
     */
    void synchronizeFileToSlave(Long fileId, Long slaveId, String token);

    /**
     * 获取文件路径，是文件的真实绝对路径
     */
    String getFilePath(StressTestFileEntity stressTestFile);

    /**
     * slave 参数化文件重命名
     * @param fileId
     * @param finalName
     */
    void updateSlaveFileName(long fileId, String finalName, String token);

    boolean checkSlaveFileExists(long slaveId, String path, String token);

    void updateStatus(long fileId, int status);
}