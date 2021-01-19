package io.renren.modules.test.controller;

import io.renren.common.annotation.SysLog;
import io.renren.common.exception.RRException;
import io.renren.common.utils.PageUtils;
import io.renren.common.utils.Query;
import io.renren.common.utils.R;
import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.entity.StressTestSlaveEntity;
import io.renren.modules.test.service.SlaveOperatorService;
import io.renren.modules.test.service.StressTestParamFileService;
import io.renren.modules.test.service.StressTestSlaveService;
import io.renren.modules.test.utils.StressTestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 压力测试用例文件
 */
@RestController
@RequestMapping("/test/stressParamFile")
public class StressTestParamFileController {
    @Autowired
    private StressTestParamFileService stressTestParamFileService;

    @Autowired
    private StressTestSlaveService stressTestSlaveService;

    @Autowired
    private SlaveOperatorService slaveOperatorService;

    /**
     * 参数化文件列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("test:stress:fileList")
    public R list(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        params.put("fileType", 1);
        //查询列表数据
        Query query = new Query(StressTestUtils.filterParms(params));

        List<StressTestFileEntity> masterList = stressTestParamFileService.queryMasterFile(query);
        int total = masterList.size();

        Map<String, Object> slaveQuery = new HashMap<>();
        List<StressTestSlaveEntity> slaveList = stressTestSlaveService.queryList(slaveQuery);
        Map<Long, String> slaveInfo = slaveList.stream().collect(Collectors.toMap(slave -> slave.getSlaveId(), slave -> slave.getSlaveName()));
        masterList.stream().forEach(masterFile -> {
            List<StressTestFileEntity> slaveFileList = stressTestParamFileService.querySlaveFile(masterFile.getFileId());
            if (!CollectionUtils.isEmpty(slaveFileList)) {
                slaveFileList.stream().forEach(slave ->
                {
                    slave.setRealName(slave.getFileName().replaceAll(".+?stressTestCases/", ""));
                    slave.setSlaveName(slaveInfo.get(slave.getSlaveId()));
                    if (slave.getStatus() < 100) {
                        if (!stressTestParamFileService.checkSlaveFileExists(slave.getSlaveId(), slave.getFileName(), request.getHeader("token"))) {
                            stressTestParamFileService.updateStatus(slave.getFileId(), 100);
                            slave.setFileName("slave机器不存在对应文件。");
                        }
                    } else {
                        slave.setFileName("slave机器不存在对应文件。");
                    }
                });
                masterFile.setChildren(slaveFileList);
            }
        });
        PageUtils pageUtil = new PageUtils(masterList, total, query.getLimit(), query.getPage());

        return R.ok().put("page", pageUtil);
    }

    /**
     * 查询具体文件信息
     */
    @RequestMapping("/info/{fileId}")
    public R info(@PathVariable("fileId") Long fileId) {
        StressTestFileEntity stressTestFile = stressTestParamFileService.queryObject(fileId);
        return R.ok().put("stressTestFile", stressTestFile);
    }

    /**
     * 删除指定文件
     */
    @SysLog("删除master机器上用例文件")
    @RequestMapping("/deleteMasterFile")
    @RequiresPermissions("test:stress:fileDelete")
    public R deleteMasterFile(@RequestBody Long fileId, HttpServletRequest request) {
        Long[] fileIds = {fileId};
        stressTestParamFileService.deleteBatch(fileIds, request.getHeader("token"));

        return R.ok();
    }

    /**
     * 删除指定文件
     */
    @SysLog("删除性能测试用例文件")
    @RequestMapping("/deleteSlaveFile")
    @RequiresPermissions("test:stress:fileDelete")
    public R deleteSlaveFile(@RequestBody Map<String, Long> slaveFileInfo, HttpServletRequest request) {
        Long fileId = slaveFileInfo.get("fileId");
        Long slaveId = slaveFileInfo.get("slaveId");
        stressTestParamFileService.deleteSlaveFile(fileId, slaveId, request.getHeader("token"));
        return R.ok();
    }

    /**
     * 将参数化文件同步到指定分布式slave节点机的指定目录下。
     */
    @SysLog("将参数化文件同步到指定分布式slave节点机的指定目录")
    @RequestMapping("/synchronizeFile")
    @RequiresPermissions("test:stress:synchronizeFile")
    public R synchronizeFile(@RequestBody Long[] fileIds, HttpServletRequest request) {
        stressTestParamFileService.synchronizeFile(fileIds, request.getHeader("token"));
        return R.ok();
    }

    /**
     * 修改slave中的参数化文件名
     * 目的：给各个slave分发的参数化文件可能不同，但需要保证文件名相同
     * @param params
     * @return
     */
    @RequestMapping("/updateSlaveFileName")
    @RequiresPermissions("test:stress:fileUpdate")
    public R updateSlaveFileName(@RequestBody Map<String,String> params, HttpServletRequest request){

        stressTestParamFileService.updateSlaveFileName(Long.valueOf(params.get("fileId")), params.get("realname"), request.getHeader("token"));
        return R.ok();
    }

    /**
     * 将参数化文件同步到指定分布式slave节点机的指定目录下。
     */
    @SysLog("将参数化文件同步到指定分布式slave节点机的指定目录")
    @RequestMapping("/synchronizeFileToSlave")
    @RequiresPermissions("test:stress:synchronizeFile")
    public R synchronizeFileToSlave(@RequestBody Map<String, Long> synchronizeInfo, HttpServletRequest request) {
        Long fileId = synchronizeInfo.get("fileId");
        Long slaveId = synchronizeInfo.get("slaveId");

        stressTestParamFileService.synchronizeFileToSlave(fileId, slaveId, request.getHeader("token"));
        return R.ok();
    }



    /**
     * 下载文件
     */
    @RequestMapping("/downloadFile/{fileId}")
    @RequiresPermissions("test:stress:fileDownLoad")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable("fileId") Long fileId) {
        StressTestFileEntity stressTestFile = stressTestParamFileService.queryObject(fileId);
        FileSystemResource fileResource = new FileSystemResource(stressTestParamFileService.getFilePath(stressTestFile));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache,no-store,must-revalidate");
        String fileNameUTF8 = new String(stressTestFile.getOriginName().getBytes(), StandardCharsets.ISO_8859_1);
        headers.add("Content-Disposition",
                "attachment;filename=" + fileNameUTF8);
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        headers.setContentType(MediaType.parseMediaType("application/octet-stream"));
        try {
            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentLength(fileResource.contentLength())
                    .body(new InputStreamResource(fileResource.getInputStream()));
        } catch (IOException e) {
            throw new RRException("找不到到文件！文件或许被删除！");
        }
    }
}