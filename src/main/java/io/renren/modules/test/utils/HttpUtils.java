package io.renren.modules.test.utils;

import io.renren.common.exception.RRException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;


public class HttpUtils {

    @Autowired
    private RestTemplate restTemplate;

    private ResponseEntity doGet(String uri, MultiValueMap<String, String> headers) {
        HttpEntity<byte[]> httpEntity = new HttpEntity<>(headers);
        return this.restTemplate.exchange(uri, HttpMethod.GET, httpEntity, byte[].class);
    }

    private ResponseEntity<byte[]> doPost(byte[] input2byte, String uri, MultiValueMap<String, String> headers) {
        HttpEntity<byte[]> httpEntity = new HttpEntity<>(input2byte, headers);

        return this.restTemplate.exchange(uri, HttpMethod.POST, httpEntity,byte[].class);
    }

    public void uploadFile(String filePath, String slaveFilePath, String slaveIp, String token) {
        RestTemplate restTemplate = new RestTemplate();
        final String url = "http://" + slaveIp + ":8080/renren-fast/test/slaveOperator/saveFile";

        //设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("token",token);

        //设置请求体，注意是LinkedMultiValueMap
        org.springframework.core.io.FileSystemResource fileSystemResource = new FileSystemResource(filePath);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", fileSystemResource);
        form.add("filePath", slaveFilePath);

        //用HttpEntity封装整个请求报文
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(form, headers);

        ResponseEntity responseEntity = restTemplate.postForEntity(url, entity, String.class);
        if (responseEntity.getStatusCode().value() == 200) {
            return;
        } else {
            throw new RRException("文件上传失败。目标服务器：" + slaveIp);
        }
    }

    public ResponseEntity doPost(MultiValueMap<String, Object> params, String host, String path, String token) {
        RestTemplate restTemplate = new RestTemplate();
        final String url = "http://" + host + path;

        //设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("token",token);

        //用HttpEntity封装整个请求报文
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(params, headers);

        ResponseEntity responseEntity = restTemplate.postForEntity(url, entity, String.class);
        if (responseEntity.getStatusCode().value() == 200) {
            return responseEntity;
        } else {
            throw new RRException("请求失败。" + host + path + params.toString());
        }
    }
}
