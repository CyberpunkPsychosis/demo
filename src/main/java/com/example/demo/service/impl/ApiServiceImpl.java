package com.example.demo.service.impl;

import com.example.demo.model.ApiResponse;
import com.example.demo.service.ApiService;
import com.example.demo.utils.HttpClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * 第三方API服务实现类
 */
@Service
public class ApiServiceImpl implements ApiService {

    private static final Logger logger = LoggerFactory.getLogger(ApiServiceImpl.class);

    private final HttpClientUtils httpClientUtils;

    public ApiServiceImpl(HttpClientUtils httpClientUtils) {
        this.httpClientUtils = httpClientUtils;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> ApiResponse<T> callGetApi(String url, Map<String, String> params, Class<T> responseType) {
        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

            // 调用接口
            T result = httpClientUtils.get(url, params, headers, responseType);
            return ApiResponse.success(result);
        } catch (RestClientException e) {
            logger.error("调用GET接口失败: {}", e.getMessage(), e);
            return ApiResponse.error("调用第三方接口失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("处理GET请求时发生异常: {}", e.getMessage(), e);
            return ApiResponse.error("处理请求时发生异常: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> ApiResponse<T> callPostApi(String url, Object requestBody, Class<T> responseType) {
        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

            // 调用接口
            T result = httpClientUtils.post(url, requestBody, headers, responseType);
            return ApiResponse.success(result);
        } catch (RestClientException e) {
            logger.error("调用POST接口失败: {}", e.getMessage(), e);
            return ApiResponse.error("调用第三方接口失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("处理POST请求时发生异常: {}", e.getMessage(), e);
            return ApiResponse.error("处理请求时发生异常: " + e.getMessage());
        }
    }
}
