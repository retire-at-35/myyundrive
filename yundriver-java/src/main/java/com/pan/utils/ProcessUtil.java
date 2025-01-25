package com.pan.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class ProcessUtil {

    /**
     * 执行 CMD 命令，并返回执行结果
     * 
     * @param command CMD 命令
     * @return 执行结果（标准输出 + 错误输出）
     * @throws IOException 如果执行命令失败
     */
    public static String executeCommand(String command) throws IOException {
        StringBuilder output = new StringBuilder();
        Process process = null;
        BufferedReader reader = null;
        BufferedReader errorReader = null;

        try {
            // 启动 CMD 命令
            process = Runtime.getRuntime().exec(command);

            // 获取命令的标准输出
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }

            // 获取命令的错误输出
            errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
            while ((line = errorReader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }

            // 等待命令执行完成
            process.waitFor();
        } catch (Exception e) {
            throw new IOException("执行命令失败: " + command, e);
        } finally {
            // 关闭流和进程
            if (reader != null) {
                reader.close();
            }
            if (errorReader != null) {
                errorReader.close();
            }
            if (process != null) {
                process.destroy();
            }
        }

        return output.toString();
    }


}
