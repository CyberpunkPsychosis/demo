#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试二维码品规识别
Test cigarette brand recognition from QR codes
"""

import sys
import os
import cv2
import requests
from pathlib import Path
from pyzbar import pyzbar
import hashlib
from urllib.parse import urlparse
import json
import time

# 添加香烟识别项目的backend路径
cigarette_project_path = Path.home() / 'smallapp' / '香烟识别' / 'backend'
sys.path.insert(0, str(cigarette_project_path))

from detector import CigaretteDetector

def test_desktop_image():
    """测试桌面图片的二维码检测和品规识别"""

    # 桌面图片路径
    desktop_image_path = Path.home() / 'Desktop' / '0bea9cdad277a6e4ab1dbdcaaa0c45dd.jpg'

    if not desktop_image_path.exists():
        print(f"❌ 图片不存在: {desktop_image_path}")
        return

    print(f"📷 测试图片: {desktop_image_path}")
    print("=" * 80)

    # 使用现有的detector检测二维码
    detector = CigaretteDetector()
    image = cv2.imread(str(desktop_image_path))

    if image is None:
        print("❌ 无法读取图片")
        return

    print(f"图片尺寸: {image.shape[1]} x {image.shape[0]}")

    # 检测二维码
    print("\n🔍 开始检测二维码...")
    results = detector.process_image(image)

    qr_codes = results.get('qr_codes', [])
    print(f"\n✅ 检测到 {len(qr_codes)} 个二维码")
    print("=" * 80)

    # 分析二维码数据
    url_codes = []
    non_url_codes = []

    for idx, qr in enumerate(qr_codes, 1):
        data = qr.get('data', '')
        print(f"\n[{idx}] 二维码数据:")
        print(f"    完整内容: {data[:100]}{'...' if len(data) > 100 else ''}")

        # 判断是否为URL (处理大写HTTPS)
        data_lower = data.lower()
        if data_lower.startswith('http://') or data_lower.startswith('https://'):
            url_codes.append(data)
            parsed = urlparse(data_lower)
            print(f"    类型: URL")
            print(f"    域名: {parsed.netloc}")
        else:
            non_url_codes.append(data)
            print(f"    类型: 非URL数据")

    print("\n" + "=" * 80)
    print(f"📊 统计:")
    print(f"   URL类型: {len(url_codes)} 个")
    print(f"   非URL类型: {len(non_url_codes)} 个")

    # 测试访问URL
    if url_codes:
        print("\n" + "=" * 80)
        print("🌐 开始测试URL访问...")
        print("=" * 80)

        for idx, url in enumerate(url_codes[:5], 1):  # 只测试前5个，避免请求过多
            print(f"\n[{idx}] 访问: {url}")
            test_url_access(url)
            time.sleep(2)  # 避免请求过快

    # 保存结果
    print("\n" + "=" * 80)
    print("💾 保存测试结果...")

    # 保存标注后的图片
    annotated_image = results.get('annotated_image')
    if annotated_image is not None:
        output_path = Path.home() / 'Desktop' / 'qr_detection_result.jpg'
        cv2.imwrite(str(output_path), annotated_image)
        print(f"✅ 标注图片已保存: {output_path}")

    # 保存详细报告
    report_path = Path.home() / 'Desktop' / 'qr_detection_report.json'
    report = {
        'image': str(desktop_image_path),
        'total_qr_codes': len(qr_codes),
        'url_count': len(url_codes),
        'non_url_count': len(non_url_codes),
        'urls': url_codes,
        'qr_codes': qr_codes
    }

    with open(report_path, 'w', encoding='utf-8') as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    print(f"✅ 详细报告已保存: {report_path}")

def test_url_access(url):
    """测试访问URL并尝试提取品规信息"""

    try:
        # 设置请求头，模拟浏览器
        headers = {
            'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.38',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
            'Accept-Language': 'zh-CN,zh;q=0.9',
        }

        print(f"   ⏳ 请求中...")
        response = requests.get(url, headers=headers, timeout=10, allow_redirects=True)

        print(f"   ✅ 状态码: {response.status_code}")
        print(f"   📄 Content-Type: {response.headers.get('Content-Type', 'unknown')}")
        print(f"   📏 响应大小: {len(response.content)} bytes")

        # 检查是否重定向
        if response.history:
            print(f"   🔀 重定向链:")
            for resp in response.history:
                print(f"      {resp.status_code} -> {resp.url}")
            print(f"      最终URL: {response.url}")

        # 尝试从响应中提取品牌信息
        content_type = response.headers.get('Content-Type', '')

        if 'application/json' in content_type:
            # JSON响应
            try:
                data = response.json()
                print(f"   📋 JSON响应:")
                print(f"      {json.dumps(data, ensure_ascii=False, indent=6)[:500]}")
                extract_brand_from_json(data)
            except:
                print(f"   ⚠️  JSON解析失败")

        elif 'text/html' in content_type:
            # HTML响应
            html = response.text
            print(f"   📋 HTML响应 (前500字符):")
            print(f"      {html[:500]}")
            extract_brand_from_html(html)

        else:
            print(f"   ℹ️  其他类型响应")

    except requests.exceptions.Timeout:
        print(f"   ⏱️  请求超时 (10秒)")
    except requests.exceptions.ConnectionError:
        print(f"   ❌ 连接失败")
    except requests.exceptions.TooManyRedirects:
        print(f"   ❌ 重定向次数过多")
    except Exception as e:
        print(f"   ❌ 错误: {e}")

def extract_brand_from_json(data):
    """从JSON数据中提取品牌信息"""

    # 常见的品牌相关字段
    brand_fields = ['brand', 'brandName', '品牌', 'productName', '产品名称', 'name', '名称']
    spec_fields = ['spec', 'specification', '品规', '规格', 'model', '型号']

    found_info = {}

    def search_dict(d, prefix=''):
        """递归搜索字典"""
        if not isinstance(d, dict):
            return

        for key, value in d.items():
            # 检查是否是品牌字段
            for field in brand_fields:
                if field.lower() in key.lower():
                    found_info['品牌'] = value

            # 检查是否是规格字段
            for field in spec_fields:
                if field.lower() in key.lower():
                    found_info['规格'] = value

            # 递归搜索
            if isinstance(value, dict):
                search_dict(value, f"{prefix}{key}.")
            elif isinstance(value, list) and value and isinstance(value[0], dict):
                for item in value:
                    search_dict(item, f"{prefix}{key}[].")

    search_dict(data)

    if found_info:
        print(f"   🏷️  提取到的信息:")
        for key, value in found_info.items():
            print(f"      {key}: {value}")
    else:
        print(f"   ℹ️  未找到明显的品牌信息字段")

def extract_brand_from_html(html):
    """从HTML中提取品牌信息"""

    # 查找常见的品牌关键词
    keywords = ['品牌', '产品名称', '香烟', '卷烟', '规格', '型号']

    found = []
    for keyword in keywords:
        if keyword in html:
            # 找到关键词前后的内容
            idx = html.find(keyword)
            context = html[max(0, idx-50):min(len(html), idx+100)]
            found.append(f"{keyword}: {context}")

    if found:
        print(f"   🏷️  HTML中找到的关键词:")
        for item in found[:3]:  # 只显示前3个
            clean = item.replace('\n', ' ').replace('\r', ' ')
            print(f"      {clean[:100]}")
    else:
        print(f"   ℹ️  HTML中未找到品牌相关关键词")

    # 尝试查找常见的中国香烟品牌
    cigarette_brands = [
        '中华', '黄鹤楼', '芙蓉王', '利群', '玉溪', '红塔山',
        '南京', '云烟', '苏烟', '娇子', '七匹狼', '黄金叶',
        '白沙', '红河', '兰州', '钻石', '泰山', '长白山'
    ]

    found_brands = []
    for brand in cigarette_brands:
        if brand in html:
            found_brands.append(brand)

    if found_brands:
        print(f"   🎯 识别到的品牌: {', '.join(found_brands)}")
    else:
        print(f"   ℹ️  未识别到已知品牌")

if __name__ == '__main__':
    print("🚀 香烟品规识别测试")
    print("=" * 80)
    test_desktop_image()
    print("\n" + "=" * 80)
    print("✅ 测试完成!")
