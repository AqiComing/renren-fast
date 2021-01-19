package io.renren.modules.test.controller;

import io.renren.common.annotation.SysLog;
import io.renren.common.utils.R;
import io.renren.modules.test.service.SlaveOperatorService;
import io.renren.modules.test.service.StressTestSlaveService;
import io.renren.modules.test.utils.StressTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 性能测试
 */
@RestController
@RequestMapping("/test/slaveOperator")
public class SlaveOperatorController {

    @Autowired
    private StressTestUtils stressTestUtils;

    @Autowired
    private StressTestSlaveService stressTestSlaveService;

    @Autowired
    private SlaveOperatorService slaveOperatorService;

    /**
     * 将参数化文件同步到指定分布式slave节点机的指定目录下。
     */
    @SysLog("保存文件到slave机器")
    @RequestMapping("/saveFile")
    public R saveFile(@RequestParam("file") MultipartFile file, @RequestParam("filePath") String filePath) {
        stressTestUtils.saveFile(file, filePath);
        return R.ok();
    }

    @SysLog("更新文件名字")
    @RequestMapping("/updateFileName")
    public R updateFileName(@RequestParam("fileId") long fileId, @RequestParam("newFileName") String newFileName) {
        slaveOperatorService.updateFileName(fileId, newFileName);
        return R.ok();
    }

    @SysLog("删除文件")
    @RequestMapping("/deleteFile")
    public R updateFileName(@RequestParam("fileId") long fileId) {
        slaveOperatorService.deleteFile(fileId);
        return R.ok();
    }

    @SysLog("校验文件是否存在")
    @RequestMapping("/checkFileExists")
    public R checkFileExists(@RequestParam("filePath") String filePath) {
        boolean result = slaveOperatorService.checkFileExists(filePath);
        return R.ok(Boolean.toString(result));
    }
}
