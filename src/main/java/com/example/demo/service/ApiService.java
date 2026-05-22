package com.example.demo.service;

import com.example.demo.model.ApiResponse;

import java.util.Map;

/**
 * 第三方API服务接口
 */
public interface ApiService {

    /**
     * 调用第三方GET接口
     * 
     * @param url 接口URL
     * @param params 请求参数
     * @param responseType 响应类型
     * @param <T> 响应数据类型
     * @return API响应
     */
    <T> ApiResponse<T> callGetApi(String url, Map<String, String> params, Class<T> responseType);

    /**
     * 调用第三方POST接口
     * 
     * @param url 接口URL
     * @param requestBody 请求体
     * @param responseType 响应类型
     * @param <T> 响应数据类型
     * @return API响应
     */
    <T> ApiResponse<T> callPostApi(String url, Object requestBody, Class<T> responseType);
}
