package io.renren.modules.test.entity;

import io.renren.modules.test.utils.StressTestUtils;

import javax.validation.constraints.Min;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 性能测试用例文件
 */
public class StressTestFileEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    private Long fileId;

    /**
     * 批量更新时，用于更新的fileId集合
     */
    private Long[] fileIdList;

    /**
     * 用例id
     */
    @Min(value = 1)
    private Long caseId;

    /**
     * 用例名称
     */
    private String caseName;

    /**
     * slave机器id
     */
    private Long slaveId;

    /**
     * 用例文件名
     */
//    @NotBlank(message="文件名不能为空")
    private String originName;

    /**
     * 目标slave机器上的真实名字
     */
    private String realName;

    /**
     * 用例保存文件名，唯一化
     */
    private String fileName;

    /**
     * 文件的MD5对于参数化文件有效主要用于节点的参数化文件校验
     */
    private String fileMd5;

    /**
     * 任务状态  0：初始状态  1：正在运行  2：成功执行  3：运行出现异常
     */
    private Integer status = 0;

    /**
     * 状态  0：保存测试报告原始文件  1：不需要测试报告
     * 默认0
     */
    private Integer reportStatus;

    /**
     * 状态  0：需要前端监控  1：不需要前端监控
     * 默认0
     */
    private Integer webchartStatus;

    /**
     * 状态 0：关闭debug  1：开始debug调试模式
     * 默认 0
     */
    private Integer debugStatus;

    /**
     * 脚本定时执行多少秒，默认是3小时
     */
    private Integer duration = StressTestUtils.getScriptSchedulerDuration();

    /**
     * 脚本启动时间，默认10s
     */
    private Integer rampUp;

    /**
     * 单机线程数，默认10
     */
    private Integer numThread;

    /**
     * 提交的用户
     */
    private String addBy;

    /**
     * 修改的用户
     */
    private String updateBy;

    /**
     * 提交的时间
     */
    private Date addTime;

    /**
     * 更新的时间
     */
    private Date updateTime;

    /**
     * 文件类型(0:脚本文件，1：数据文件)
     */
    private Integer fileType;

    /**
     * slave机文件关联的master机器文件id
     */
    private Long relatedId;

    /**
     * slave机器名字
     */
    private String slaveName;

    private List<StressTestFileEntity> children;

    private boolean dubboCase;
    /**
     * 分布式节点的字符串，可以用来判断是否使用分布式。
     * 不入库。
     */
    private String slaveStr;

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getOriginName() {
        return originName;
    }

    public void setOriginName(String originName) {
        this.originName = originName;
    }

    public String getAddBy() {
        return addBy;
    }

    public void setAddBy(String addBy) {
        this.addBy = addBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getAddTime() {
        return addTime;
    }

    public void setAddTime(Date addTime) {
        this.addTime = addTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getReportStatus() {
        return reportStatus;
    }

    public void setReportStatus(Integer reportStatus) {
        this.reportStatus = reportStatus;
    }

    public Integer getWebchartStatus() {
        return webchartStatus;
    }

    public void setWebchartStatus(Integer webchartStatus) {
        this.webchartStatus = webchartStatus;
    }

    public String getSlaveStr() {
        return slaveStr;
    }

    public void setSlaveStr(String slaveStr) {
        this.slaveStr = slaveStr;
    }

    public String getFileMd5() {
        return fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    public Long getSlaveId() {
        return slaveId;
    }

    public void setSlaveId(Long slaveId) {
        this.slaveId = slaveId;
    }

    public String getCaseName() {
        return caseName;
    }

    public void setCaseName(String caseName) {
        this.caseName = caseName;
    }

    public Integer getDebugStatus() {
        return debugStatus;
    }

    public void setDebugStatus(Integer debugStatus) {
        this.debugStatus = debugStatus;
    }

    public Long[] getFileIdList() {
        return fileIdList;
    }

    public void setFileIdList(Long[] fileIdList) {
        this.fileIdList = fileIdList;
    }

    public Integer getDuration() {
        return duration;
    }

    public Integer getDuration(Integer durationDefault) {
        if (duration > 0) {
            return duration;
        } else {
            return durationDefault;
        }
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    /**
     * 深度clone
     */
    public StressTestFileEntity clone() {
        StressTestFileEntity clone = new StressTestFileEntity();
        clone.setCaseId(this.getCaseId());
        clone.setStatus(this.getStatus());
        clone.setFileMd5(this.getFileMd5());
        clone.setFileName(this.getFileName());
        clone.setOriginName(this.getOriginName());
        clone.setSlaveStr(this.getSlaveStr());
        clone.setReportStatus(this.getReportStatus());
        clone.setWebchartStatus(this.getWebchartStatus());
        clone.setDebugStatus(this.getDebugStatus());
        clone.setSlaveId(this.getSlaveId());
        clone.setFileIdList(this.getFileIdList());
        clone.setFileType(this.getFileType());
        return clone;
    }

    public Integer getFileType() {
        return fileType;
    }

    public void setFileType(Integer fileType) {
        this.fileType = fileType;
    }

    public Integer getRampUp() {
        return this.rampUp;
    }

    public Integer getRampUp(Integer rampUpDefault) {
        if (rampUp > 0) {
            return rampUp;
        } else {
            return rampUpDefault;
        }
    }

    public void setRampUp(Integer rampUp) {
        this.rampUp = rampUp;
    }

    public Integer getNumThread() {
        return this.numThread;
    }

    public Integer getNumThread(Integer numThreadDefault) {
        if (numThread > 0) {
            return numThread;
        } else {
            return numThreadDefault;
        }
    }

    public void setNumThread(Integer numThread) {
        this.numThread = numThread;
    }

    public List<StressTestFileEntity> getChildren() {
        return children;
    }

    public void setChildren(List<StressTestFileEntity> children) {
        this.children = children;
    }

    public Long getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(Long relatedId) {
        this.relatedId = relatedId;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getSlaveName() {
        return slaveName;
    }

    public void setSlaveName(String slaveName) {
        this.slaveName = slaveName;
    }

    public boolean isDubboCase() {
        return dubboCase;
    }

    public void setDubboCase(boolean dubboCase) {
        this.dubboCase = dubboCase;
    }
}
