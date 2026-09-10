package com.example.demo;

import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.io.File;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Scanner;

public class CodeGenerator2 {
    private static final String PACKAGE_NAME = "com.example.demo";
    private static final String DB_NAME = "test";
    // 全局唯一Scanner，解决多Scanner阻塞输入
    private static final Scanner SCANNER = new Scanner(System.in);

    public static String scanner(String tip) {
        System.out.print("请输入" + tip + "：");
        String ipt = SCANNER.nextLine().trim();
        if (StringUtils.isNotBlank(ipt)) {
            return ipt;
        }
        throw new MybatisPlusException("请输入正确的" + tip + "！");
    }

    public static void main(String[] args) throws Exception {
        // 修复：当前类是CodeGenerator2，不是CodeGenerator
        String classPath = CodeGenerator2.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        classPath = URLDecoder.decode(classPath, "UTF-8");
        File classDir = new File(classPath);
        String classAbsPath = classDir.getAbsolutePath();

        if (!classAbsPath.contains("target" + File.separator + "classes")) {
            throw new MybatisPlusException("仅支持IDE正常编译后运行，未识别到target/classes目录");
        }

        // 拼接源码根
        String srcMainJava = classAbsPath
                .replace(File.separator + "target" + File.separator + "classes", "")
                + File.separator + "src" + File.separator + "main" + File.separator + "java";

        File javaSrcDir = new File(srcMainJava);
        if (!javaSrcDir.exists()) {
            throw new MybatisPlusException("源码目录不存在：" + srcMainJava);
        }

        // xml目录
        String srcMainResources = classAbsPath
                .replace(File.separator + "target" + File.separator + "classes", "")
                + File.separator + "src" + File.separator + "main" + File.separator + "resources";
        String xmlOutputDir = srcMainResources + File.separator + "mappers";
        new File(xmlOutputDir).mkdirs();

        System.out.println("\n===== 路径信息 =====");
        System.out.println("class编译目录：" + classAbsPath);
        System.out.println("Java源码输出根：" + srcMainJava);
        System.out.println("Mapper XML输出目录：" + xmlOutputDir);
        System.out.println("====================\n");

        // 这里正常调用scanner方法，会阻塞等待你输入表名
        String tableStr = scanner("表名，多个英文逗号分割");
        String[] tableArr = tableStr.split(",");

        FastAutoGenerator.create(
                        "jdbc:mysql://localhost:3306/" + DB_NAME + "?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true",
                        "root",
                        "As.123456")
                .globalConfig(builder -> builder
                        .outputDir(srcMainJava)
                        .author("laohan")
                        .disableOpenDir()
                )
                .packageConfig(builder -> builder
                        .parent(PACKAGE_NAME)
                        .moduleName("")
                        .pathInfo(Collections.singletonMap(OutputFile.xml, xmlOutputDir))
                )
                .strategyConfig(builder -> builder
                        .addInclude(tableArr)
                        .entityBuilder()
                        .enableLombok()
                        .naming(NamingStrategy.underline_to_camel)
                        .columnNaming(NamingStrategy.underline_to_camel)
                        .mapperBuilder()
                        .enableMapperAnnotation()
                        .controllerBuilder()
                        .enableRestStyle()
                )
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();

        System.out.println("代码生成完成！文件已写入源代码目录");
        SCANNER.close();
    }
}