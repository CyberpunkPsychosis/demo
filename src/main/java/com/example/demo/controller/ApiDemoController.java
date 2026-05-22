package com.example.demo.controller;

import com.example.demo.model.ApiResponse;
import com.example.demo.service.ApiService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * API调用示例控制器
 */
@RestController
@RequestMapping("/api/demo")
public class ApiDemoController {

    private final ApiService apiService;

    public ApiDemoController(ApiService apiService) {
        this.apiService = apiService;
    }

    /**
     * 调用第三方GET接口示例
     * 
     * @param url 接口URL
     * @return API响应
     */
    @GetMapping("/call-get")
    public ApiResponse<String> callGetApi(@RequestParam("url") String url) {
        // 构建请求参数示例
        Map<String, String> params = new HashMap<>();
        params.put("param1", "value1");
        params.put("param2", "value2");

        // 调用第三方接口
        return apiService.callGetApi(url, params, String.class);
    }

    /**
     * 调用第三方POST接口示例
     * 
     * @param url 接口URL
     * @param requestBody 请求体
     * @return API响应
     */
    @PostMapping("/call-post")
    public ApiResponse<String> callPostApi(
            @RequestParam("url") String url, 
            @RequestBody Object requestBody) {
        // 调用第三方接口
        return apiService.callPostApi(url, requestBody, String.class);
    }
}
