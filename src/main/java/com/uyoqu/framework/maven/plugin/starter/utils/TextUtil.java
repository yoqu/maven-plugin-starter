package com.uyoqu.framework.maven.plugin.starter.utils;

import com.uyoqu.framework.maven.plugin.starter.BinCreateMojo;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.Iterator;

public class TextUtil
{
    /**
     * 将TXT中的内容以流的形式读入Java的string中
     * @param sourcePath 要读取的TXT文件的路径
     * @return 读取到的内容字符串
     * @throws IOException
     */
    public static String readFileAsStream( String sourcePath ) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader( BinCreateMojo.class.getClassLoader().getResourceAsStream( sourcePath ) ) );
        StringBuffer buffer = new StringBuffer();
        String line;
        while ( ( line = in.readLine() ) != null )
        {
            buffer.append( line ).append( "\n" );
        }
        in.close();
        return buffer.toString();
    }

    /**
     * tar打包压缩
     *
     * @param filesPathArray
     *            要压缩的文件的全路径(数组)
     * @param resultFilePath
     *            压缩后的文件全文件名(.tar)
     * @throws Exception
     * @DATE 2018年9月25日 下午12:39:28
     */
    public static boolean tarCompression(String[] filesPathArray, String resultFilePath) throws Exception {
        System.out.println(" tarCompression -> Compression start!");
        FileOutputStream fos = null;
        TarArchiveOutputStream taos = null;
        try {
            fos = new FileOutputStream(new File(resultFilePath));
            taos = new TarArchiveOutputStream(fos);
            for (String filePath : filesPathArray) {
                BufferedInputStream bis = null;
                FileInputStream fis = null;
                try {
                    File file = new File(filePath);
                    TarArchiveEntry tae = new TarArchiveEntry(file);
                    // 此处指明 每一个被压缩文件的名字,以便于解压时TarArchiveEntry的getName()方法获取到的直接就是这里指定的文件名
                    // 以(左边的)GBK编码将file.getName()“打碎”为序列,再“组装”序列为(右边的)GBK编码的字符串
                    tae.setName(new String(file.getName().getBytes("UTF8"), "UTF8"));
                    taos.putArchiveEntry(tae);
                    fis = new FileInputStream(file);
                    bis = new BufferedInputStream(fis);
                    int count;
                    byte data[] = new byte[1024];
                    while ((count = bis.read(data, 0, 1024)) != -1) {
                        taos.write(data, 0, count);
                    }
                } finally {
                    taos.closeArchiveEntry();
                    if (bis != null)
                        bis.close();
                    if (fis != null)
                        fis.close();
                }
            }
        } finally {
            if (taos != null)
                taos.close();
            if (fos != null)
                fos.close();
        }
        System.out.println(" tarCompression -> Compression end!");
        return true;
    }
    /**
     * 将指定路径中的所有文件打成war包
     * @param zipDir 要打成的war包的名称
     * @param contextPath 要进行打war包的根目录的路径名
     */
    public static void zip(String zipDir,String contextPath) {
        if (StringUtils.isBlank(zipDir) || StringUtils.isBlank(contextPath) || !contextPath.endsWith(".war")){
            return;
        }
        File outFile = new File(contextPath);
        contextPath = contextPath.substring(0, contextPath.length()-4);
        String[] strings = contextPath.split("/");
        if (1 == strings.length){
            strings = contextPath.split("\\\\");
        }
        contextPath = strings[strings.length-1];
        try {
        if (outFile.exists()){
            outFile.delete();
        }
        outFile.createNewFile();
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(outFile));
        ArchiveOutputStream out = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.JAR,bufferedOutputStream);
        if (zipDir.endsWith(File.separator)) {
            zipDir = zipDir + File.separator;
        }
        Iterator files = FileUtils.iterateFiles(new File(zipDir), null, true);
        while (files.hasNext()) {
        File file = (File) files.next();
        String path = file.getPath();
        String[] split = path.split(contextPath);
        split[1] = split[1].substring(1);
        ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(file, path.substring(path.length()-split[1].length(), path.length()));
        out.putArchiveEntry(zipArchiveEntry);
        IOUtils.copy(new FileInputStream(file), out);
        out.closeArchiveEntry();
        }
        out.finish();
        out.close();
    } catch (IOException e) {
        System.out.println(e.getMessage());
        System.err.println("创建文件失败");
        } catch (ArchiveException e) {
        System.err.println("不支持的压缩格式");
        }
    }

}

