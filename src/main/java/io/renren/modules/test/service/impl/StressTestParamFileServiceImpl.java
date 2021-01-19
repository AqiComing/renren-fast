package io.renren.modules.test.service.impl;

import io.renren.common.exception.RRException;
import io.renren.common.utils.R;
import io.renren.modules.test.dao.*;
import io.renren.modules.test.entity.*;
import io.renren.modules.test.service.StressTestParamFileService;
import io.renren.modules.test.utils.HttpUtils;
import io.renren.modules.test.utils.SSH2Utils;
import io.renren.modules.test.utils.StressTestUtils;
import net.sf.json.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Service("stressTestParamFileService")
public class StressTestParamFileServiceImpl implements StressTestParamFileService {

    Logger logger = LoggerFactory.getLogger(getClass());

    private static final String JAVA_CLASS_PATH = "java.class.path";
    private static final String CLASSPATH_SEPARATOR = File.pathSeparator;
    private static final String OS_NAME = System.getProperty("os.name");// $NON-NLS-1$
    private static final String OS_NAME_LC = OS_NAME.toLowerCase(Locale.ENGLISH);
    private static final String JMETER_INSTALLATION_DIRECTORY;

    /**
     * 增加了一个static代码块，本身是从Jmeter的NewDriver源码中复制过来的。
     * Jmeter的api中是删掉了这部分代码的，需要从Jmeter源码中才能看到。
     * 由于源码中bug的修复很多，我也就原封保留了。
     *
     * 这段代码块的意义在于，通过Jmeter_Home的地址，找到Jmeter要加载的jar包的目录。
     * 将这些jar包中的方法的class_path，放置到JAVA_CLASS_PATH系统变量中。
     * 而Jmeter在遇到参数化的函数表达式的时候，会从JAVA_CLASS_PATH系统变量中来找到这些对应关系。
     * 而Jmeter的插件也是一个原理，来找到这些对应关系。
     * 其中配置文件还包含了这些插件的过滤配置，默认是.functions. 的必须，.gui.的非必须。
     * 配置key为  classfinder.functions.notContain
     *
     * 带来的影响：
     * 让程序和Jmeter_home外部的联系更加耦合了，这样master必带Jmeter_home才可以。
     * 不仅仅是测试报告的生成了。
     * 同时，需要在pom文件中引用ApacheJMeter_functions，这其中才包含了参数化所用的函数的实现类。
     *
     * 自己修改：
     * 1. 可以将class_path直接拼接字符串的形式添加到系统变量中，不过如果Jmeter改了命名，则这边也要同步修改很麻烦。
     * 2. 修改Jmeter源码，将JAVA_CLASS_PATH系统变量这部分的查找改掉。在CompoundVariable 类的static块中。
     *    ClassFinder.findClassesThatExtend 方法。
     *
     * 写成static代码块，也是因为类加载（第一次请求时），才会初始化并初始化一次。这也是符合逻辑的。
     */
    static {
        final List<URL> jars = new LinkedList<>();
        final String initial_classpath = System.getProperty(JAVA_CLASS_PATH);

        JMETER_INSTALLATION_DIRECTORY = StressTestUtils.getJmeterHome();

        /*
         * Does the system support UNC paths? If so, may need to fix them up
         * later
         */
        boolean usesUNC = OS_NAME_LC.startsWith("windows");// $NON-NLS-1$

        // Add standard jar locations to initial classpath
        StringBuilder classpath = new StringBuilder();
        File[] libDirs = new File[]{new File(JMETER_INSTALLATION_DIRECTORY + File.separator + "lib"),// $NON-NLS-1$ $NON-NLS-2$
                new File(JMETER_INSTALLATION_DIRECTORY + File.separator + "lib" + File.separator + "ext"),// $NON-NLS-1$ $NON-NLS-2$
                new File(JMETER_INSTALLATION_DIRECTORY + File.separator + "lib" + File.separator + "junit")};// $NON-NLS-1$ $NON-NLS-2$
        for (File libDir : libDirs) {
            File[] libJars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (libJars == null) {
                new Throwable("Could not access " + libDir).printStackTrace(); // NOSONAR No logging here
                continue;
            }
            Arrays.sort(libJars); // Bug 50708 Ensure predictable order of jars
            for (File libJar : libJars) {
                try {
                    String s = libJar.getPath();

                    // Fix path to allow the use of UNC URLs
                    if (usesUNC) {
                        if (s.startsWith("\\\\") && !s.startsWith("\\\\\\")) {// $NON-NLS-1$ $NON-NLS-2$
                            s = "\\\\" + s;// $NON-NLS-1$
                        } else if (s.startsWith("//") && !s.startsWith("///")) {// $NON-NLS-1$ $NON-NLS-2$
                            s = "//" + s;// $NON-NLS-1$
                        }
                    } // usesUNC

                    jars.add(new File(s).toURI().toURL());// See Java bug 4496398
                    classpath.append(CLASSPATH_SEPARATOR);
                    classpath.append(s);
                } catch (MalformedURLException e) { // NOSONAR
//                    EXCEPTIONS_IN_INIT.add(new Exception("Error adding jar:"+libJar.getAbsolutePath(), e));
                }
            }
        }

        // ClassFinder needs the classpath
        System.setProperty(JAVA_CLASS_PATH, initial_classpath + classpath.toString());

//        new JavassistEngine().fixJmeterStandrdEngine();
    }

    @Autowired
    private StressTestFileDao stressTestFileDao;

    @Autowired
    private StressTestReportsDao stressTestReportsDao;

    @Autowired
    private DebugTestReportsDao debugTestReportsDao;

    @Autowired
    private StressTestSlaveDao stressTestSlaveDao;

    @Autowired
    private StressTestDao stressTestDao;

    @Autowired
    private StressTestUtils stressTestUtils;

    @Override
    public StressTestFileEntity queryObject(Long fileId) {
        return stressTestFileDao.queryObject(fileId);
    }

    @Override
    public List<StressTestFileEntity> queryList(Map<String, Object> map) {
        return stressTestFileDao.queryList(map);
    }

    @Override
    public List<StressTestFileEntity> queryMasterFile(Map<String, Object> map) {
        return stressTestFileDao.queryMasterFile(map);
    }

    @Override
    public List<StressTestFileEntity> querySlaveFile(Long masterFileId) {
        return stressTestFileDao.querySlaveFile(masterFileId);
    }

    @Override
    public List<StressTestFileEntity> queryParamFileList(Long caseId) {
        Map query = new HashMap<>();
        query.put("caseId", caseId.toString());
        query.put("fileType", 1);
        return stressTestFileDao.queryList(query);
    }

    @Override
    public int queryTotal(Map<String, Object> map) {
        return stressTestFileDao.queryTotal(map);
    }

    @Override
    public void save(StressTestFileEntity stressTestFile) {
        stressTestFileDao.save(stressTestFile);
    }

    /**
     * 保存用例文件及入库
     */
    @Override
    @Transactional
    public void save(MultipartFile multipartFile, String filePath, StressTestEntity stressCase, StressTestFileEntity stressTestFile) {
        // 保存文件放这里,是因为有事务.
        // 保存数据放在最前,因为当前文件重名校验是根据数据库异常得到
        try {
            String fileMd5 = DigestUtils.md5Hex(multipartFile.getBytes());
            stressTestFile.setFileMd5(fileMd5);
        } catch (IOException e) {
            throw new RRException("获取上传文件的MD5失败！", e);
        }
        if (stressTestFile.getFileId() != null && stressTestFile.getFileId() > 0L) {
            // 替换文件，同时修改添加时间，便于前端显示。
            stressTestFile.setAddTime(new Date());
            update(stressTestFile);
        } else {
            save(stressTestFile);
        }
        // 肯定存在已有的用例信息
        stressTestDao.update(stressCase);
        stressTestUtils.saveFile(multipartFile, filePath);
    }

    @Override
    public void update(StressTestFileEntity stressTestFile) {
        stressTestFileDao.update(stressTestFile);
    }

    @Override
    @Transactional
    public void update(StressTestFileEntity stressTestFile, StressTestReportsEntity stressTestReports) {
        update(stressTestFile);
        if (stressTestReports != null) {
            if (stressTestReports instanceof DebugTestReportsEntity) {
                debugTestReportsDao.update((DebugTestReportsEntity) stressTestReports);
            } else {
                stressTestReportsDao.update(stressTestReports);
            }
        }
    }

    /**
     * 更新用例文件及入库
     */
    @Override
    @Transactional
    public void update(MultipartFile multipartFile, String filePath, StressTestEntity stressCase, StressTestFileEntity stressTestFile) {
        try {
            String fileMd5 = DigestUtils.md5Hex(multipartFile.getBytes());
            stressTestFile.setFileMd5(fileMd5);
        } catch (IOException e) {
            throw new RRException("获取上传文件的MD5失败！", e);
        }
        update(stressTestFile);
        stressTestDao.update(stressCase);
        stressTestUtils.saveFile(multipartFile, filePath);
    }

    /**
     * 批量删除
     * 删除所有缓存 + 方法只要调用即删除所有缓存。
     */
    @Override
    @Transactional
    public void deleteBatch(Object[] fileIds, String token) {

        Arrays.asList(fileIds).stream().forEach(fileId -> {
            StressTestFileEntity stressTestFile = queryObject((Long) fileId);
            String casePath = stressTestUtils.getCasePath();
            String filePath = casePath + File.separator + stressTestFile.getFileName();

//            String jmxDir = filePath.substring(0, filePath.lastIndexOf("."));
            // jmxDir不在这里删除，删除报告那里会有一个兜底的代码。要不然会造成脚本删除，测试报告由于缺失源文件生成失败。
//            FileUtils.deleteQuietly(new File(jmxDir));

            //给已经删除脚本的测试报告一个提示
            Map<String, Object> params = new HashMap<>();
            params.put("fileId", fileId + "");
            List<StressTestReportsEntity> stressTestReportsList = stressTestReportsDao.queryList(params);
            for (StressTestReportsEntity report : stressTestReportsList) {
                if (StringUtils.isBlank(report.getRemark())) {
                    report.setRemark("源脚本被删除过");
                    stressTestReportsDao.update(report);
                }
            }

            FileUtils.deleteQuietly(new File(filePath));

            //删除缓存
            StressTestUtils.samplingStatCalculator4File.invalidate(fileId);
            StressTestUtils.jMeterEntity4file.remove(fileId);

            //删除远程节点的同步文件，如果远程节点比较多，网络不好，执行时间会比较长。
            deleteSlaveFile((Long) fileId, token);
        });

        stressTestFileDao.deleteBatch(fileIds);
    }

    /**
     * 向子节点同步参数化文件
     */
    @Override
    @Transactional
    public void synchronizeFile(Long[] fileIds, String token) {
        //当前是向所有的分布式节点推送这个，阻塞操作+轮询，并非多线程，因为本地同步网卡会是瓶颈。
        Map query = new HashMap<>();
        query.put("status", StressTestUtils.ENABLE);
        List<StressTestSlaveEntity> stressTestSlaveList = stressTestSlaveDao.queryList(query);
        //使用for循环传统写法
        //采用了先给同一个节点机传送多个文件的方式，因为数据库的连接消耗优于节点机的链接消耗
        for (StressTestSlaveEntity slave : stressTestSlaveList) {

            // 不向本地节点传送文件
            if ("127.0.0.1".equals(slave.getIp().trim())) {
                continue;
            }

//            SSH2Utils ssh2Util = new SSH2Utils(slave.getIp(), slave.getUserName(),
//                    slave.getPasswd(), Integer.parseInt(slave.getSshPort()));
            for (Long fileId : fileIds) {
                StressTestFileEntity stressTestFile = queryObject(fileId);
//                putFileToSlave(slave, ssh2Util, stressTestFile);
                putFileToSlaveByApi(slave, stressTestFile, token);
                stressTestFile.setStatus(StressTestUtils.RUN_SUCCESS);
                //由于事务性，这个地方不好批量更新。
                update(stressTestFile);
            }
        }
    }

    /**
     * 向指定子节点同步参数化文件
     */
    @Override
    @Transactional
    public void synchronizeFileToSlave(Long fileId, Long slaveId, String token) {
        StressTestSlaveEntity slave = stressTestSlaveDao.queryObject(slaveId);

        // 不向本地节点传送文件
        if ("127.0.0.1".equals(slave.getIp().trim())) {
            R.error("不能向本地同步文件");
        }

        StressTestFileEntity stressTestFile = queryObject(fileId);
        putFileToSlaveByApi(slave, stressTestFile, token);

        stressTestFile.setStatus(StressTestUtils.RUN_SUCCESS);
    }

    /**
     * 将文件上传到节点机目录上。
     */
    public void putFileToSlaveByApi(StressTestSlaveEntity slave, StressTestFileEntity stressTestFile, String token) {
        String casePath = stressTestUtils.getCasePath();
        String fileNameSave = stressTestFile.getFileName();
        String filePath = casePath + File.separator + fileNameSave;
        String fileSaveMD5 = "";
        try {
            fileSaveMD5 = stressTestUtils.getMd5ByFile(filePath);
        } catch (IOException e) {
            throw new RRException(stressTestFile.getOriginName() + "生成MD5失败！", e);
        }

        // 避免跨系统的问题，远端由于都时linux服务器，则文件分隔符统一为/，不然同步文件会报错。
        String slaveFilePath = getSlaveFileName(stressTestFile, slave);
        HttpUtils httpUtils = new HttpUtils();
        httpUtils.uploadFile(filePath, slaveFilePath, slave.getIp(), token);

        Map fileQuery = new HashMap<>();
        fileQuery.put("originName", stressTestFile.getOriginName() + "_slaveId" + slave.getSlaveId());
        fileQuery.put("slaveId", slave.getSlaveId().toString());
        StressTestFileEntity newStressTestFile = stressTestFileDao.queryObjectForClone(fileQuery);
        if (newStressTestFile == null) {
            newStressTestFile = stressTestFile.clone();
            newStressTestFile.setStatus(2);
            newStressTestFile.setFileName(slaveFilePath);
            newStressTestFile.setOriginName(stressTestFile.getOriginName() + "_slaveId" + slave.getSlaveId());
            newStressTestFile.setFileMd5(fileSaveMD5);
            // 最重要是保存分布式子节点的ID
            newStressTestFile.setSlaveId(slave.getSlaveId());
            newStressTestFile.setFileType(stressTestFile.getFileType());
            newStressTestFile.setRelatedId(stressTestFile.getFileId());
            save(newStressTestFile);
        } else {
            newStressTestFile.setFileMd5(fileSaveMD5);
            update(newStressTestFile);
        }
    }

    @Override
    public String getFilePath(StressTestFileEntity stressTestFile) {
        String casePath = stressTestUtils.getCasePath();
        String FilePath = casePath + File.separator + stressTestFile.getFileName();
        return FilePath;
    }

    /**
     * 将文件上传到节点机目录上。
     */
    public void putFileToSlave(StressTestSlaveEntity slave, SSH2Utils ssh2Util, StressTestFileEntity stressTestFile) {
        String casePath = stressTestUtils.getCasePath();
        String fileNameSave = stressTestFile.getFileName();
        String filePath = casePath + File.separator + fileNameSave;
        String fileSaveMD5 = "";
        try {
            fileSaveMD5 = stressTestUtils.getMd5ByFile(filePath);
        } catch (IOException e) {
            throw new RRException(stressTestFile.getOriginName() + "生成MD5失败！", e);
        }

        // 避免跨系统的问题，远端由于都时linux服务器，则文件分隔符统一为/，不然同步文件会报错。
        String caseFileHome = slave.getHomeDir() + "/bin/stressTestCases";
        String MD5 = ssh2Util.runCommand("md5sum " + getSlaveFileName(stressTestFile, slave) + "|cut -d ' ' -f1");
        if (fileSaveMD5.equals(MD5)) {//说明目标服务器已经存在相同文件不再重复上传
            return;
        }

        //上传文件
        ssh2Util.scpPutFile(filePath, caseFileHome);

        Map fileQuery = new HashMap<>();
        fileQuery.put("originName", stressTestFile.getOriginName() + "_slaveId" + slave.getSlaveId());
        fileQuery.put("slaveId", slave.getSlaveId().toString());
        StressTestFileEntity newStressTestFile = stressTestFileDao.queryObjectForClone(fileQuery);
        if (newStressTestFile == null) {
            newStressTestFile = stressTestFile.clone();
            newStressTestFile.setStatus(2);
            newStressTestFile.setFileName(getSlaveFileName(stressTestFile, slave));
            newStressTestFile.setOriginName(stressTestFile.getOriginName() + "_slaveId" + slave.getSlaveId());
            newStressTestFile.setFileMd5(fileSaveMD5);
            // 最重要是保存分布式子节点的ID
            newStressTestFile.setSlaveId(slave.getSlaveId());
            newStressTestFile.setFileType(stressTestFile.getFileType());
            newStressTestFile.setRelatedId(stressTestFile.getFileId());
            save(newStressTestFile);
        } else {
            newStressTestFile.setFileMd5(fileSaveMD5);
            update(newStressTestFile);
        }
    }

    /**
     * 根据fileId 删除对应所有同步过slave节点的文件。
     */
    public void deleteSlaveFile(Long fileId, String token) {
        // 获取参数化文件同步到哪些分布式子节点的记录
        Map fileQuery = new HashMap<>();
        fileQuery.put("relatedId", fileId);
        List<StressTestFileEntity> fileDeleteList = stressTestFileDao.queryList(fileQuery);

        if (fileDeleteList.isEmpty()) {
            return;
        }
        // 将同步过的分布式子节点的ID收集起来，用于查询子节点对象集合。
        ArrayList fileDeleteIds = new ArrayList();
        for (StressTestFileEntity stressTestFile4Slave : fileDeleteList) {
            if (stressTestFile4Slave.getSlaveId() == null) {
                continue;
            }

            StressTestSlaveEntity slaveEntity = stressTestSlaveDao.queryObject(stressTestFile4Slave.getSlaveId());
            if (Objects.isNull(slaveEntity)) {
                // 系统中已经不维护该节点，则跳过
                continue;
            }
            // 跳过本地节点
            if ("127.0.0.1".equals(slaveEntity.getIp().trim())) {
                continue;
            }
            deleteSlaveFile(stressTestFile4Slave.getFileId(), slaveEntity.getSlaveId(), token);
//            SSH2Utils ssh2Util = new SSH2Utils(slaveEntity.getIp(), slaveEntity.getUserName(),
//                    slaveEntity.getPasswd(), Integer.parseInt(slaveEntity.getSshPort()));
//            ssh2Util.runCommand("rm -f " + getSlaveFileName(stressTestFile, slaveEntity));

//            if (slaveIds.isEmpty()) {
//                slaveIds = stressTestFile4Slave.getSlaveId().toString();
//            } else {
//                slaveIds += "," + stressTestFile4Slave.getSlaveId().toString();
//            }
            fileDeleteIds.add(stressTestFile4Slave.getFileId());
        }

//        if (slaveIds.isEmpty()) {
//            return;
//        }

        // 每一个参数化文件，会对应多个同步子节点slave的记录。
//        Map slaveQuery = new HashMap<>();
//        slaveQuery.put("slaveIds", slaveIds);
//        // 每一个被同步过的记录，都要执行删除操作。
//        List<StressTestSlaveEntity> stressTestSlaveList = stressTestSlaveDao.queryList(slaveQuery);
//        for (StressTestSlaveEntity slave : stressTestSlaveList) {
//            // 跳过本地节点
//            if ("127.0.0.1".equals(slave.getIp().trim())) {
//                continue;
//            }
//
//            SSH2Utils ssh2Util = new SSH2Utils(slave.getIp(), slave.getUserName(),
//                    slave.getPasswd(), Integer.parseInt(slave.getSshPort()));
//            ssh2Util.runCommand("rm -f " + getSlaveFileName(stressTestFile, slave));
//        }
        if (CollectionUtils.isNotEmpty(fileDeleteIds)) {
            stressTestFileDao.deleteBatch(fileDeleteIds.toArray());
        }
    }

    /**
     * 删除指定slave节点指定fileId的文件。
     */
    @Override
    @Transactional
    public void deleteSlaveFile(Long fileId, Long slaveId, String token) {
        StressTestSlaveEntity slaveEntity = stressTestSlaveDao.queryObject(slaveId);
        if (Objects.isNull(slaveEntity)) {
            // 系统中已经不维护该节点，则跳过
            return;
        }
        // 跳过本地节点
        if ("127.0.0.1".equals(slaveEntity.getIp().trim())) {
            return;
        }

        HttpUtils httpUtils = new HttpUtils();
        String host = slaveEntity.getIp() + ":8080";
        String path = "/renren-fast/test/slaveOperator/deleteFile";

        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.add("fileId", fileId);

        httpUtils.doPost(params, host, path, token);
//        SSH2Utils ssh2Util = new SSH2Utils(slaveEntity.getIp(), slaveEntity.getUserName(),
//                slaveEntity.getPasswd(), Integer.parseInt(slaveEntity.getSshPort()));
//        ssh2Util.runCommand("rm -f " + stressTestFile.getFileName());

        stressTestFileDao.delete(fileId);
    }

    /**
     * 获取slave节点上的参数化文件具体路径
     */
    public String getSlaveFileName(StressTestFileEntity stressTestFile, StressTestSlaveEntity slave) {
        // 避免跨系统的问题，远端由于都时linux服务器，则文件分隔符统一为/，不然同步文件会报错。
        String caseFileHome = slave.getHomeDir() + "/bin/stressTestCases";
        String fileNameUpload = stressTestFile.getOriginName();
        return caseFileHome + File.separator + fileNameUpload;
    }

    /**
     * 拼装分布式节点，当前还没有遇到分布式节点非常多的情况。
     *
     * @return 分布式节点的IP地址拼装，不包含本地127.0.0.1的IP
     */
    public String getSlaveIPPort() {
        Map query = new HashMap<>();
        query.put("status", StressTestUtils.ENABLE);
        List<StressTestSlaveEntity> stressTestSlaveList = stressTestSlaveDao.queryList(query);

        StringBuilder stringBuilder = new StringBuilder();
        for (StressTestSlaveEntity slave : stressTestSlaveList) {
            // 本机不包含在内
            if ("127.0.0.1".equals(slave.getIp().trim())) {
                continue;
            }

            if (stringBuilder.length() != 0) {
                stringBuilder.append(",");
            }
            stringBuilder.append(slave.getIp().trim()).append(":").append(slave.getJmeterPort().trim());
        }
        return stringBuilder.toString();
    }

    /**
     * master节点是否被使用为压力节点
     */
    public boolean checkSlaveLocal() {
        Map query = new HashMap<>();
        query.put("status", StressTestUtils.ENABLE);
        List<StressTestSlaveEntity> stressTestSlaveList = stressTestSlaveDao.queryList(query);

        for (StressTestSlaveEntity slave : stressTestSlaveList) {
            // 本机配置IP为127.0.0.1，没配置localhost
            if ("127.0.0.1".equals(slave.getIp().trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 拼装分布式节点，当前还没有遇到分布式节点非常多的情况。
     *
     * @return 分布式节点的IP地址拼装，不包含本地127.0.0.1的IP
     */
    public Map<String, Integer> getSlaveAddrWeight() {
        Map query = new HashMap<>();
        query.put("status", StressTestUtils.ENABLE);
        List<StressTestSlaveEntity> stressTestSlaveList = stressTestSlaveDao.queryList(query);

        Map<String, Integer> resultMap = new HashMap<String, Integer>();
        for (StressTestSlaveEntity slave : stressTestSlaveList) {
            // 本机不包含在内
            if ("127.0.0.1".equals(slave.getIp().trim())) {
                continue;
            }

            resultMap.put(slave.getIp().trim() + ":" + slave.getJmeterPort().trim(), Integer.parseInt(slave.getWeight()));
        }
        return resultMap;
    }

    /**
     * 修改slave中的参数化文件名
     * 目的：给各个slave分发的参数化文件可能不同，但需要保证文件名相同
     */
    @Override
    @Transactional
    public void updateSlaveFileName(long fileId, String newName, String token) {

        StressTestFileEntity stressTestFileEntity = stressTestFileDao.queryObject(fileId);

        //查询slave信息
        StressTestSlaveEntity slave = stressTestSlaveDao.queryObject(stressTestFileEntity.getSlaveId());
        String originalPath = stressTestFileEntity.getFileName();
        String finalFilePath = originalPath.substring(0, originalPath.lastIndexOf("/")) + "/" + newName;

        HttpUtils httpUtils = new HttpUtils();
        String host = slave.getIp() + ":8080";
        String path = "/renren-fast/test/slaveOperator/updateFileName";

        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.add("fileId", fileId);
        params.add("newFileName", finalFilePath);
        httpUtils.doPost(params, host, path, token);

        //修改sql对应的fileName(删除文件是依靠该字段)
        Map<String, Object> map = new HashMap<>();
        map.put("fileName", finalFilePath);
        map.put("fileId", stressTestFileEntity.getFileId());
        stressTestFileDao.update(map);
    }

    @Override
    public boolean checkSlaveFileExists(long slaveId, String filePath, String token) {
        StressTestSlaveEntity slave = stressTestSlaveDao.queryObject(slaveId);

        if (slave == null) {
            return false;
        } else {
            HttpUtils httpUtils = new HttpUtils();
            String host = slave.getIp() + ":8080";
            String path = "/renren-fast/test/slaveOperator/checkFileExists";
            MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
            params.add("filePath", filePath);
            ResponseEntity responseEntity = httpUtils.doPost(params, host, path, token);
            logger.info(responseEntity.getBody().toString());
            return JSONObject.fromObject(responseEntity.getBody()).getBoolean("msg");
        }
    }

    @Override
    public void updateStatus(long fileId, int status) {
        Map query = new HashMap();
        query.put("fileId", fileId);
        query.put("status", status);
        stressTestFileDao.update(query);
    }
}
