package com.uyoqu.framework.maven.plugin.starter;

import com.uyoqu.framework.maven.plugin.starter.utils.ClassUtil;
import com.uyoqu.framework.maven.plugin.starter.utils.TextUtil;
import com.uyoqu.framework.maven.plugin.starter.utils.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.loader.tools.MainClassFinder;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Mojo(name = "bin", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class BinCreateMojo extends AbstractMojo {
    private static final Logger logger = LoggerFactory.getLogger(BinCreateMojo.class);

    private static final String SPRING_BOOT_APPLICATION_CLASS_NAME = "org.springframework.boot.autoconfigure.SpringBootApplication";

    @Parameter
    private String mainClass;

    @Parameter(property = "pom.build.finalName")
    private String serverName;

    @Parameter
    private List<String> jvms;

    @Parameter
    private String matchClass;

    @Parameter(property = "basedir")
    private String projectDir;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;


    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File outputDirectory;


    public void execute() throws MojoFailureException {
        try {
            configMainClass();
        } catch (IOException e) {
            throw new MojoFailureException("configMainClass异常");
        }
        createStarterBin();
        zip();
    }

    private void configMainClass() throws IOException {
        if (StringUtils.isNotBlank(mainClass)) {
            return;
        }
        try {
            String mainClassScanRootPath = genFileByName("").getParentFile().getAbsolutePath() + "/WEB-INF/classes";
            File rootFile = new File(mainClassScanRootPath);
            if (StringUtils.isBlank(matchClass)) {
                logger.info("正在通过判断@SpringBootApplication注解寻找启动类");
                mainClass = MainClassFinder.findSingleMainClass(rootFile,
                        SPRING_BOOT_APPLICATION_CLASS_NAME);
            }
            if (StringUtils.isBlank(mainClass) && StringUtils.isNotBlank(matchClass)) {
                logger.info("正在通过类名匹配寻找启动类");
                mainClass = ClassUtil.findSingleMainClass(rootFile, matchClass);
            }
            if (StringUtils.isBlank(mainClass)) {
                throw new IllegalArgumentException("用户未配置启动类且插件未自动找到启动类");
            } else {
                logger.info("启动类为:{}", mainClass);
            }
        } catch (IOException e) {
            logger.error("获取启动类时产生IO异常", e);
            if (StringUtils.isBlank(mainClass)) {
                throw e;
            }
        }
    }

    /**
     * 对工程进行重新打包
     */
    private void zip() {
        String targetWarFilePath = new File(this.outputDirectory, serverName + ".war").getAbsolutePath();
        String sourceDirPath = new File(this.outputDirectory, serverName).getAbsolutePath() + File.separator;
        logger.info("压缩目录：{},压缩目标文件{}", sourceDirPath, targetWarFilePath);
        ZipUtil.zip(sourceDirPath, targetWarFilePath);
//        TextUtil.zip(targetWarFilePath, sourceDirPath);
    }

    /**
     * 创建脚本
     */
    public void createStarterBin() {
        for (String s : Arrays.asList("start", "stop", "restart")) {
            logger.info("auto generate {} sh", s);
            copyFile(s);
        }
    }

    /**
     * 根据传入脚本名返回对应的shell脚本文件
     *
     * @param fileName 脚本名称
     * @return 生成的脚本文件
     */
    private File genFileByName(String fileName) throws IOException {
        fileName = outputDirectory + "/" + serverName + "/bin/" + fileName;
        File file = new File(fileName);
        FileUtils.forceMkdir(file.getParentFile());
        if (file.exists()) {
            file.delete();
        }
        return file;
    }

    /**
     * 根据文件名生成对应脚本文件
     *
     * @param fileName 文件名
     */
    private void copyFile(String fileName) {
        try {
            String content = TextUtil.readFileAsStream(fileName + ".txt");
            content = doFilter(content);
            File targetFile = genFileByName(fileName + ".sh");
            targetFile.setExecutable(true);
            FileUtils.writeStringToFile(targetFile, content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 对脚本内容进行过滤，替换站占位符成真实参数
     *
     * @param content 脚本内容
     * @return 过滤后的脚本内容
     * @throws IOException
     */
    private String doFilter(String content) throws IOException {
        if (content.contains("{main}")) {
            content = content.replace("{main}", mainClass);
        }
        if (content.contains("{serverName}")) {
            content = content.replace("{serverName}", serverName);
        }
        if (content.contains("{jvms}")) {
            String jvmString = formatJvmString();
            content = content.replace("{jvms}", jvmString);
        }
        return content;
    }

    /**
     * 将用户配置的jvm参数格式化成字符串
     *
     * @return 格式化成字符串后的jvm参数
     * @throws IOException
     */
    private String formatJvmString() throws IOException {
        StringBuffer jvmString = new StringBuffer();
        if (CollectionUtils.isEmpty(jvms)) {
            //用户无jvm参数配置时使用默认参数
            jvmString.append(TextUtil.readFileAsStream("jvms.txt"));
        } else {
            jvmString.append("JAVA_MEM_OPTS=\" ");
            for (String jvm : jvms) {
                jvmString.append(jvm + " ");
            }
            jvmString.append("\"");
        }
        return jvmString.toString();
    }
}

