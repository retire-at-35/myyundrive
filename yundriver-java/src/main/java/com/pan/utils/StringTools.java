package com.pan.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;


public class StringTools {

    public static void checkParam(Object param) {
        try {
            Field[] fields = param.getClass().getDeclaredFields();
            boolean notEmpty = false;
            for (Field field : fields) {
                String methodName = "get" + StringTools.upperCaseFirstLetter(field.getName());
                Method method = param.getClass().getMethod(methodName);
                Object object = method.invoke(param);
                if (object != null && object instanceof String && !StringTools.isEmpty(object.toString())
                        || object != null && !(object instanceof String)) {
                    notEmpty = true;
                    break;
                }
            }
            if (!notEmpty) {
                throw new RuntimeException("多参数更新，删除，必须有非空条件");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("校验参数是否为空失败");
        }
    }

    public static String getFileSuffix(String fileName) {
        Integer index = fileName.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        String suffix = fileName.substring(index);
        return suffix;
    }

    public static String upperCaseFirstLetter(String field) {
        if (isEmpty(field)) {
            return field;
        }
        //如果第二个字母是大写，第一个字母不大写
        if (field.length() > 1 && Character.isUpperCase(field.charAt(1))) {
            return field;
        }
        return field.substring(0, 1).toUpperCase() + field.substring(1);
    }

    public static boolean isEmpty(String str) {
        if (null == str || "".equals(str) || "null".equals(str) || "\u0000".equals(str)) {
            return true;
        } else if ("".equals(str.trim())) {
            return true;
        }
        return false;
    }

    public static String encodeByMD5(String originString) {
        return StringTools.isEmpty(originString) ? null : DigestUtils.md5Hex(originString);
    }


    public static String getRandomString(Integer count) {
        return RandomStringUtils.random(count, true, true);
    }

    public static boolean pathIsOk(String path) {
        if (StringTools.isEmpty(path)) {
            return true;
        }
        if (path.contains("../") || path.contains("..\\")) {
            return false;
        }
        return true;
    }

    /**
     * 获取文件的后缀名
     * @param fileName 文件名
     * @return 文件后缀名（不包含"."），如果没有后缀名，返回空字符串
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        // 如果点不存在，或者点是最后一个字符，说明没有后缀名
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }


    /**
     * 获取文件名（不带后缀）
     * @param fileName 文件名
     * @return 文件名（不包括后缀），如果文件名无效则返回空字符串
     */
    public static String getFileNameWithoutExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        int lastSeparatorIndex = Math.max(fileName.lastIndexOf("/"), fileName.lastIndexOf("\\"));

        // 如果没有"."，或者"."在路径分隔符之前，认为没有后缀
        if (lastDotIndex == -1 || lastDotIndex < lastSeparatorIndex) {
            return fileName.substring(lastSeparatorIndex + 1);
        }

        // 返回从最后一个路径分隔符到最后一个"."之间的部分
        return fileName.substring(lastSeparatorIndex + 1, lastDotIndex);
    }


    public static String extractFilePath(String input) {
        // 查找最后一个 '.' 出现的位置
        int dotIndex = input.lastIndexOf('.');

        // 截取从开始到 '.' 前的部分
        if (dotIndex != -1) {
            return input.substring(0, dotIndex);
        }

        // 如果没有找到 '.'，返回原字符串
        return input;
    }

    public static String rename(String fileName){
        String fileNameReal = getFileNameWithoutExtension(fileName);
        String suffix = getFileExtension(fileName);
        return fileNameReal + "_" + getRandomString(5)+"."+suffix;
    }
}
