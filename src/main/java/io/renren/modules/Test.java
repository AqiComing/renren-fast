package io.renren.modules;

import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.visualizers.backend.BackendListener;
import org.apache.jmeter.visualizers.backend.BackendListenerClient;
import org.apache.jmeter.visualizers.backend.UserMetric;
import org.apache.jorphan.collections.HashTree;

import java.io.*;
import java.util.*;

public class Test {
//    public static void main(String[] args) {
//        GoogleAuthenticator gAuth = new GoogleAuthenticator();
//        int test = gAuth.getTotpPassword("P4OUDLJO6Z5TF5ZF");
//
//        Connection connection = new Connection("10.111.3.165", 2222);
//        try {
//            File keyfile = new File("C:\\Users\\chenjianghui\\.ssh\\coco");
//            if (keyfile.exists()) {
//                System.out.println(123);
//            }
//            connection.connect();
//            connection.authenticateWithPublicKey("chenjianghui", keyfile, "Cjh0408inxdu");
//            Session session = connection.openSession();
//            session.execCommand("1");
//
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        System.out.println(123);
//    }

    public static void main(String[] args) throws IOException {
        Properties prop = new Properties();
        InputStream in = new FileInputStream(new File("F:\\jmeter\\jmeter.properties"));
        prop.load(in);
        String test = prop.getProperty("remote_hosts");
        prop.setProperty("remote_hosts", test + "1234");
        OutputStream out = new FileOutputStream(new File("F:\\jmeter\\jmeter.properties"));
        prop.store(out, "test1234");
        out.flush();
        out.close();
        System.out.println(123);
    }

}
