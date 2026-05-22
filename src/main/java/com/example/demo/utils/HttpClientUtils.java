package com.example.demo.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP客户端工具类，用于调用第三方接口
 */
@Component
public class HttpClientUtils {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtils.class);

    private final RestTemplate restTemplate;

    public HttpClientUtils(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 发送GET请求
     * 
     * @param url 请求URL
     * @param responseType 响应类型
     * @param <T> 响应泛型
     * @return 响应结果
     */
    public <T> T get(String url, Class<T> responseType) {
        return exchange(url, HttpMethod.GET, null, responseType, null);
    }

    /**
     * 发送GET请求（带请求头）
     * 
     * @param url 请求URL
     * @param headers 请求头
     * @param responseType 响应类型
     * @param <T> 响应泛型
     * @return 响应结果
     */
    public <T> T get(String url, HttpHeaders headers, Class<T> responseType) {
        return exchange(url, HttpMethod.GET, null, responseType, headers);
    }

    /**
     * 发送GET请求（带URL参数）
     * 
     * @param url 请求URL
     * @param params URL参数
     * @param responseType 响应类型
     * @param <T> 响应泛型
     * @return 响应结果
     */
    public <T> T get(String url, Map<String, String> params, Class<T> responseType) {
        String fullUrl = buildUrlWithParams(url, params);
        return get(fullUrl, responseType);
    }

    /**
     * 发送GET请求（带URL参数和请求头）
     * 
     * @param url 请求URL
     * @param params URL参数
     * @param headers 请求头
     * @param responseType 响应类型
     * @param <T> 响应泛型
     * @return 响应结果
     */
    public <T> T get(String url, Map<String, String> params, HttpHeaders headers, Class<T> responseType) {
        String fullUrl = buildUrlWithParams(url, params);
        return get(fullUrl, headers, responseType);
    }

    /**
     * 发送POST请求
     * 
     * @param url 请求URL
     * @param body 请求体
     * @param responseType 响应类型
     * @param <T> 响应泛型
     * @return 响应结果
     */
    public <T> T post(String url, Object body, Class<T> responseType) {
        return exchange(url, HttpMethod.POST, body, responseType, null);
    }

    /**
     * 发送POST请求（带请求头）
     * 
     * @param url 请求URL
     * @param body 请求体
     * @param headers 请求头
     * @param responseType 响应类型
     * @param <T> 响应泛型
     * @return 响应结果
     */
    public <T> T post(String url, Object body, HttpHeaders headers, Class<T> responseType) {
        return exchange(url, HttpMethod.POST, body, responseType, headers);
    }

    /**
     * 发送PUT请求
     * 
     * @param url 请求URL
     * @param body 请求体
     * @param responseType 响应类型
     * @param <T> 响应泛型
     * @return 响应结果
     */
    public <T> T put(String url, Object body, Class<T> responseType) {
        return exchange(url, HttpMethod.PUT, body, responseType, null);
    }

    /**
     * 发送PUT请求（带请求头）
     * 
     * @param url 请求URL
     * @param body 请求体
     * @param headers 请求头
     * @param responseType 响应类型
     * @param <T> 响应泛型
     * @return 响应结果
     */
    public <T> T put(String url, Object body, HttpHeaders headers, Class<T> responseType) {
        return exchange(url, HttpMethod.PUT, body, responseType, headers);
    }

    /**
     * 发送DELETE请求
     * 
     * @param url 请求URL
     * @param responseType 响应类型
     * @param <T> 响应泛型
     * @return 响应结果
     */
    public <T> T delete(String url, Class<T> responseType) {
        return exchange(url, HttpMethod.DELETE, null, responseType, null);
    }

    /**
     * 发送DELETE请求（带请求头）
     * 
     * @param url 请求URL
     * @param headers 请求头
     * @param responseType 响应类型
     * @param <T> 响应泛型
     * @return 响应结果
     */
    public <T> T delete(String url, HttpHeaders headers, Class<T> responseType) {
        return exchange(url, HttpMethod.DELETE, null, responseType, headers);
    }

    /**
     * 发送请求（通用方法）
     * 
     * @param url 请求URL
     * @param method HTTP方法
     * @param body 请求体
     * @param responseType 响应类型
     * @param headers 请求头
     * @param <T> 响应泛型
     * @return 响应结果
     */
    public <T> T exchange(String url, HttpMethod method, Object body, Class<T> responseType, HttpHeaders headers) {
        try {
            HttpHeaders requestHeaders = headers != null ? headers : new HttpHeaders();
            HttpEntity<?> requestEntity = new HttpEntity<>(body, requestHeaders);

            logger.info("Sending {} request to {}", method, url);
            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    method,
                    requestEntity,
                    responseType
            );

            logger.info("Received response with status: {}", response.getStatusCode());
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error during {} request to {}: {}", method, url, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 发送请求（通用方法，支持复杂泛型返回类型）
     * 
     * @param url 请求URL
     * @param method HTTP方法
     * @param body 请求体
     * @param typeReference 参数化类型引用
     * @param headers 请求头
     * @param <T> 响应泛型
     * @return 响应结果
     */
    public <T> T exchange(String url, HttpMethod method, Object body, 
                          ParameterizedTypeReference<T> typeReference, HttpHeaders headers) {
        try {
            HttpHeaders requestHeaders = headers != null ? headers : new HttpHeaders();
            HttpEntity<?> requestEntity = new HttpEntity<>(body, requestHeaders);

            logger.info("Sending {} request to {}", method, url);
            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    method,
                    requestEntity,
                    typeReference
            );

            logger.info("Received response with status: {}", response.getStatusCode());
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error during {} request to {}: {}", method, url, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 构建带参数的URL
     * 
     * @param url 基础URL
     * @param params 参数Map
     * @return 构建完成的URL
     */
    private String buildUrlWithParams(String url, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }

        StringBuilder urlBuilder = new StringBuilder(url);
        if (!url.contains("?")) {
            urlBuilder.append("?");
        } else if (!url.endsWith("&") && !url.endsWith("?")) {
            urlBuilder.append("&");
        }

        int i = 0;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (i > 0) {
                urlBuilder.append("&");
            }
            urlBuilder.append(entry.getKey()).append("=").append(entry.getValue());
            i++;
        }

        return urlBuilder.toString();
    }
}
