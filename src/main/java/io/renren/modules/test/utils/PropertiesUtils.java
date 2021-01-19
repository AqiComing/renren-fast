package io.renren.modules.test.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

public class PropertiesUtils {
    private Logger logger = LoggerFactory.getLogger(getClass());

    public void updateProperties(String filePath, Map<String, String> updateInfo) {
        try {
            File file = new File(filePath);
            Properties prop = new Properties();
            if (file.exists()) {
                InputStream in = new FileInputStream(new File(filePath));
                prop.load(in);
            }
            updateInfo.keySet().stream().forEach(key -> prop.setProperty(key, updateInfo.get(key)));
            OutputStream out = new FileOutputStream(file);
            Enumeration<?> e = prop.propertyNames();
            while (e.hasMoreElements()){
                String key = (String) e.nextElement();
                String value = prop.getProperty(key);
                String s = key + "=" + value+"\n";
                out.write(s.getBytes());
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            logger.info(filePath + "文件编辑失败。\n" + e.getMessage());
            throw new RuntimeException("property文件更新失败");
        }
    }
}
