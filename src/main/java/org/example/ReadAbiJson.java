package org.example;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReadAbiJson {

    // 测试网地址  your test net rpc address, not necessary
    private static final String testNet = "your test net rpc address";
    // 合约地址 not necessary
    private static final String contractAddress = "contract address";
    // 私钥  not necessary
    private static final String privateKey = "your private key";

    // 文件夹地址 your folder
    private static final String abiFilePath = "E:\\EvmProject\\ConnectEther\\src\\main\\resources\\DonateNft.json";

    public static void main(String[] args) {
        JSONArray abiArray = readAbiFile(abiFilePath);
        generateJsFiles(abiArray);
    }

    public static JSONArray readAbiFile(String path) {
        try {
            String jsonStr = FileUtil.readUtf8String(new File(path));
            return JSONUtil.parseObj(jsonStr).getJSONArray("abi");
        } catch (Exception e) {
            System.err.println("Error reading ABI file: " + e.getMessage());
            return new JSONArray(); // 返回空的 JSONArray 以避免后续错误
        }
    }

    public static void generateJsFiles(JSONArray abiArray) {
        for (Object obj : abiArray) {
            JSONObject json = (JSONObject) obj;
            String methodName = json.getStr("name");

            if (methodName != null) {
                methodName = sanitizeFileName(capitalizeFirstLetter(methodName));
                String content = generateJsContent(json, methodName);
                if (ObjectUtil.isNotEmpty(content)) {
                    saveToFile(methodName, content);
                }
            }
        }
    }

    public static String generateJsContent(JSONObject json, String methodName) {
        String type = json.getStr("type");
        // 跳过不需要处理的类型
        if ("error".equals(type) || "event".equals(type) || "constructor".equals(type) || "receive".equals(type)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        // 添加导入代码
        sb.append("const { Web3 } = require('web3');\n")
                .append("const fs = require('fs');\n")
                .append("const path = require('path');\n\n");

        // 添加 Web3 实例
        sb.append("const web3 = new Web3('").append(testNet).append("');\n")
                .append("const contractAddress = '").append(contractAddress).append("';\n")
                .append("const privateKey = '").append(privateKey).append("';\n\n");

        // 添加 ABI 方法调用
        sb.append("const contractJson = JSON.parse(fs.readFileSync('")
                .append(abiFilePath.replace("\\", "/")).append("', 'utf8'));\n")
                .append("const contractABI = contractJson.abi;\n\n")
                .append("const contract = new web3.eth.Contract(contractABI, contractAddress);\n");

        // 生成方法调用代码
        sb.append("async function call").append(methodName).append("(");
        List<String> paramList = setInputParam(json, sb);
        sb.append(") {\n     try {\n");

        List<String> outputParamList = getOutPutParms(json);
        if (!outputParamList.isEmpty()) {
            sb.append("         const ").append(methodName).append(" = await contract.methods.")
                    .append(json.getStr("name")).append("(");
        } else {
            sb.append("         await contract.methods.").append(json.getStr("name")).append("(");
        }

        setInputParam(json, sb);
        sb.append(").call();\n");

        if (!outputParamList.isEmpty()) {
            sb.append("         return ").append(methodName).append(";\n");
        }

        sb.append("     } catch (error) {\n")
                .append("         console.error('Error fetching ").append(methodName).append(":', error);\n")
                .append("         throw error;\n")
                .append("     }\n };\n\n");

        // 生成参数声明
        for (String param : paramList) {
            sb.append("const ").append(param).append(" = nil;\n");
        }

        sb.append("\ncall").append(methodName).append("(");
        setInputParam(json, sb);
        sb.append(")\n     .then(info => {\n")
                .append("           console.log('Token Info:', info);\n")
                .append("       })\n")
                .append("       .catch(err => {\n")
                .append("           console.error('Error:', err);\n")
                .append("       });\n");

        return sb.toString();
    }

    private static List<String> getOutPutParms(JSONObject json) {
        List<String> param = new ArrayList<>();
        JSONArray inputs = json.getJSONArray("inputs");
        for (Object o : inputs) {
            JSONObject jsonObject = JSONUtil.parseObj(JSONUtil.toJsonStr(o));
            param.add(jsonObject.getStr("name"));
        }
        return param;
    }

    private static List<String> setInputParam(JSONObject json, StringBuilder sb) {
        List<String> param = new ArrayList<>();
        JSONArray inputs = json.getJSONArray("inputs");
        for (int i = 0; i < inputs.size(); i++) {
            if (i > 0) sb.append(", ");
            JSONObject jsonObject = JSONUtil.parseObj(JSONUtil.toJsonStr(inputs.get(i)));
            String name = jsonObject.getStr("name");
            sb.append(name);
            param.add(name);
        }
        return param;
    }

    public static void saveToFile(String methodName, String content) {
        try {
            String fileName = methodName + ".js";
            FileUtil.writeUtf8String(content, fileName);
            System.out.println("File saved: " + new File(fileName).getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error saving file: " + e.getMessage());
        }
    }

    // 清理文件名的特殊字符
    private static String sanitizeFileName(String fileName) {
        return (fileName == null) ? "defaultFileName" : fileName.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    // 首字母大写
    public static String capitalizeFirstLetter(String input) {
        return (input == null || input.isEmpty()) ? input : input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
