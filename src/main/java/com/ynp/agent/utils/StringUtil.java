package com.ynp.agent.utils;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.List;
import java.util.Objects;

public class StringUtil {
    //路径比较器，可以按三类占位符统配 ?：匹配任意单个字符。例 /files/log?.txt 可匹配 /files/log1.txt 或 /files/logA.txt，但不匹配 /files/log10.txt。
    //  - *：匹配 0 到任意多个字符（不跨 /）。例 /img/*.png 可匹配 /img/logo.png、/img/.png，但不匹配 /img/icons/logo.png。
    //  - **：跨目录匹配任意多段路径。例 /public/** 可匹配 /public/index.html、/public/js/app.js、/public/，也能匹配更深层级 /public/assets/fonts/icon.ttf。
    private static final PathMatcher PATH_MATCHER = new AntPathMatcher();

    public static boolean match(String sourceStr, List<String> targetList) {
        if (isBlank(sourceStr) || targetList == null || targetList.isEmpty()) {
            return false;
        }
        for (String pattern : targetList) {
            if (isBlank(pattern)) {
                continue;
            }
            if (Objects.equals(sourceStr, pattern) || PATH_MATCHER.match(pattern, sourceStr)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBlank(String str){
        return io.netty.util.internal.StringUtil.isNullOrEmpty(str);
    }

    public static boolean equal(String str1, String str2) {
        if(isBlank(str1)){
            return false;
        }
        return str1.equals(str2);
    }
}
