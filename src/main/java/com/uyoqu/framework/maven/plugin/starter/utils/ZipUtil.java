package com.uyoqu.framework.maven.plugin.starter.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.FastByteArrayOutputStream;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.zip.*;

public class ZipUtil {

    /**
     * 默认编码，使用平台相关编码
     */
    private static final Charset DEFAULT_CHARSET = Charset.defaultCharset();

    /**
     * 打包到当前目录，使用默认编码UTF-8
     *
     * @param srcPath 源文件路径
     * @return 打包好的压缩文件
     * @ IO异常
     */
    public static File zip(String srcPath) throws IOException {
        return zip(srcPath, DEFAULT_CHARSET);
    }

    /**
     * 打包到当前目录
     *
     * @param srcPath 源文件路径
     * @param charset 编码
     * @return 打包好的压缩文件
     * @ IO异常
     */
    public static File zip(String srcPath, Charset charset) throws IOException {
        return zip(FileUtil.file(srcPath), charset);
    }

    /**
     * 打包到当前目录，使用默认编码UTF-8
     *
     * @param srcFile 源文件或目录
     * @return 打包好的压缩文件
     * @ IO异常
     */
    public static File zip(File srcFile) throws IOException {
        return zip(srcFile, DEFAULT_CHARSET);
    }

    /**
     * 打包到当前目录
     *
     * @param srcFile 源文件或目录
     * @param charset 编码
     * @return 打包好的压缩文件
     * @ IO异常
     */
    public static File zip(File srcFile, Charset charset) throws IOException {
        File zipFile = FileUtil.file(srcFile.getParentFile(), FileUtil.mainName(srcFile) + ".zip");
        zip(zipFile, charset, false, srcFile);
        return zipFile;
    }

    /**
     * 对文件或文件目录进行压缩<br>
     * 不包含被打包目录
     *
     * @param srcPath 要压缩的源文件路径。如果压缩一个文件，则为该文件的全路径；如果压缩一个目录，则为该目录的顶层目录路径
     * @param zipPath 压缩文件保存的路径，包括文件名。注意：zipPath不能是srcPath路径下的子文件夹
     * @return 压缩好的Zip文件
     * @ IO异常
     */
    public static File zip(String srcPath, String zipPath) {
        return zip(srcPath, zipPath, false);
    }

    /**
     * 对文件或文件目录进行压缩<br>
     *
     * @param srcPath    要压缩的源文件路径。如果压缩一个文件，则为该文件的全路径；如果压缩一个目录，则为该目录的顶层目录路径
     * @param zipPath    压缩文件保存的路径，包括文件名。注意：zipPath不能是srcPath路径下的子文件夹
     * @param withSrcDir 是否包含被打包目录
     * @return 压缩文件
     * @ IO异常
     */
    public static File zip(String srcPath, String zipPath, boolean withSrcDir) {
        return zip(srcPath, zipPath, DEFAULT_CHARSET, withSrcDir);
    }

    /**
     * 对文件或文件目录进行压缩<br>
     *
     * @param srcPath    要压缩的源文件路径。如果压缩一个文件，则为该文件的全路径；如果压缩一个目录，则为该目录的顶层目录路径
     * @param zipPath    压缩文件保存的路径，包括文件名。注意：zipPath不能是srcPath路径下的子文件夹
     * @param charset    编码
     * @param withSrcDir 是否包含被打包目录
     * @return 压缩文件
     * @ IO异常
     */
    public static File zip(String srcPath, String zipPath, Charset charset, boolean withSrcDir) {
        File srcFile = FileUtil.file(srcPath);
        File zipFile = FileUtil.file(zipPath);
        zip(zipFile, charset, withSrcDir, srcFile);
        return zipFile;
    }

    /**
     * 对文件或文件目录进行压缩<br>
     * 使用默认UTF-8编码
     *
     * @param zipFile    生成的Zip文件，包括文件名。注意：zipPath不能是srcPath路径下的子文件夹
     * @param withSrcDir 是否包含被打包目录，只针对压缩目录有效。若为false，则只压缩目录下的文件或目录，为true则将本目录也压缩
     * @param srcFiles   要压缩的源文件或目录。
     * @return 压缩文件
     * @ IO异常
     */
    public static File zip(File zipFile, boolean withSrcDir, File... srcFiles) {
        return zip(zipFile, DEFAULT_CHARSET, withSrcDir, srcFiles);
    }

    /**
     * 对文件或文件目录进行压缩
     *
     * @param zipFile    生成的Zip文件，包括文件名。注意：zipPath不能是srcPath路径下的子文件夹
     * @param charset    编码
     * @param withSrcDir 是否包含被打包目录，只针对压缩目录有效。若为false，则只压缩目录下的文件或目录，为true则将本目录也压缩
     * @param srcFiles   要压缩的源文件或目录。如果压缩一个文件，则为该文件的全路径；如果压缩一个目录，则为该目录的顶层目录路径
     * @return 压缩文件
     * @ IO异常
     */
    public static File zip(File zipFile, Charset charset, boolean withSrcDir, File... srcFiles) {
        validateFiles(zipFile, srcFiles);

        try (ZipOutputStream out = getZipOutputStream(zipFile, charset)) {
            String srcRootDir;
            for (File srcFile : srcFiles) {
                if (null == srcFile) {
                    continue;
                }
                // 如果只是压缩一个文件，则需要截取该文件的父目录
                srcRootDir = srcFile.getCanonicalPath();
                if (srcFile.isFile() || withSrcDir) {
                    //若是文件，则将父目录完整路径都截取掉；若设置包含目录，则将上级目录全部截取掉，保留本目录名
                    srcRootDir = srcFile.getCanonicalFile().getParentFile().getCanonicalPath();
                }
                // 调用递归压缩方法进行目录或文件压缩
                zip(srcFile, srcRootDir, out);
                out.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return zipFile;
    }

    // ---------------------------------------------------------------------------------------------- Unzip

    /**
     * 解压到文件名相同的目录中，默认编码UTF-8
     *
     * @param zipFilePath 压缩文件路径
     * @return 解压的目录
     * @ IO异常
     */
    public static File unzip(String zipFilePath) throws IOException {
        return unzip(zipFilePath, DEFAULT_CHARSET);
    }

    /**
     * 解压到文件名相同的目录中
     *
     * @param zipFilePath 压缩文件路径
     * @param charset     编码
     * @return 解压的目录
     * @ IO异常
     * @since 3.2.2
     */
    public static File unzip(String zipFilePath, Charset charset) throws IOException {
        return unzip(FileUtil.file(zipFilePath), charset);
    }

    /**
     * 解压到文件名相同的目录中，使用UTF-8编码
     *
     * @param zipFile 压缩文件
     * @return 解压的目录
     * @ IO异常
     * @since 3.2.2
     */
    public static File unzip(File zipFile) throws IOException {
        return unzip(zipFile, DEFAULT_CHARSET);
    }

    /**
     * 解压到文件名相同的目录中
     *
     * @param zipFile 压缩文件
     * @param charset 编码
     * @return 解压的目录
     * @ IO异常
     * @since 3.2.2
     */
    public static File unzip(File zipFile, Charset charset) throws IOException {
        return unzip(zipFile, FileUtil.file(zipFile.getParentFile(), FileUtil.mainName(zipFile)), charset);
    }

    /**
     * 解压，默认UTF-8编码
     *
     * @param zipFilePath 压缩文件的路径
     * @param outFileDir  解压到的目录
     * @return 解压的目录
     * @ IO异常
     */
    public static File unzip(String zipFilePath, String outFileDir) {
        return unzip(zipFilePath, outFileDir, DEFAULT_CHARSET);
    }

    /**
     * 解压
     *
     * @param zipFilePath 压缩文件的路径
     * @param outFileDir  解压到的目录
     * @param charset     编码
     * @return 解压的目录
     * @ IO异常
     */
    public static File unzip(String zipFilePath, String outFileDir, Charset charset) {
        return unzip(FileUtil.file(zipFilePath), FileUtil.mkdir(outFileDir), charset);
    }

    /**
     * 解压，默认使用UTF-8编码
     *
     * @param zipFile zip文件
     * @param outFile 解压到的目录
     * @return 解压的目录
     * @ IO异常
     */
    public static File unzip(File zipFile, File outFile) {
        return unzip(zipFile, outFile, DEFAULT_CHARSET);
    }

    /**
     * 解压
     *
     * @param zipFile zip文件
     * @param outFile 解压到的目录
     * @param charset 编码
     * @return 解压的目录
     * @ IO异常
     * @since 3.2.2
     */
    @SuppressWarnings("unchecked")
    public static File unzip(File zipFile, File outFile, Charset charset) {
        charset = (null == charset) ? DEFAULT_CHARSET : charset;

        ZipFile zipFileObj = null;
        try {
            zipFileObj = new ZipFile(zipFile, charset);
            final Enumeration<ZipEntry> em = (Enumeration<ZipEntry>) zipFileObj.entries();
            ZipEntry zipEntry = null;
            File outItemFile = null;
            while (em.hasMoreElements()) {
                zipEntry = em.nextElement();
                //FileUtil.file会检查slip漏洞，漏洞说明见http://blog.nsfocus.net/zip-slip-2/
                outItemFile = FileUtil.file(outFile, zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    outItemFile.mkdirs();
                } else {
                    FileUtil.touch(outItemFile);
                    copy(zipFileObj, zipEntry, outItemFile);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IoUtil.close(zipFileObj);
        }
        return outFile;
    }

    /**
     * 从Zip文件中提取指定的文件为bytes
     *
     * @param zipFilePath Zip文件
     * @param name        文件名，如果存在于子文件夹中，此文件名必须包含目录名，例如images/aaa.txt
     * @return 文件内容bytes
     * @since 4.1.8
     */
    public static byte[] unzipFileBytes(String zipFilePath, String name) {
        return unzipFileBytes(zipFilePath, DEFAULT_CHARSET, name);
    }

    /**
     * 从Zip文件中提取指定的文件为bytes
     *
     * @param zipFilePath Zip文件
     * @param charset     编码
     * @param name        文件名，如果存在于子文件夹中，此文件名必须包含目录名，例如images/aaa.txt
     * @return 文件内容bytes
     * @since 4.1.8
     */
    public static byte[] unzipFileBytes(String zipFilePath, Charset charset, String name) {
        return unzipFileBytes(FileUtil.file(zipFilePath), charset, name);
    }

    /**
     * 从Zip文件中提取指定的文件为bytes
     *
     * @param zipFile Zip文件
     * @param name    文件名，如果存在于子文件夹中，此文件名必须包含目录名，例如images/aaa.txt
     * @return 文件内容bytes
     * @since 4.1.8
     */
    public static byte[] unzipFileBytes(File zipFile, String name) {
        return unzipFileBytes(zipFile, DEFAULT_CHARSET, name);
    }

    /**
     * 从Zip文件中提取指定的文件为bytes
     *
     * @param zipFile Zip文件
     * @param charset 编码
     * @param name    文件名，如果存在于子文件夹中，此文件名必须包含目录名，例如images/aaa.txt
     * @return 文件内容bytes
     * @since 4.1.8
     */
    @SuppressWarnings("unchecked")
    public static byte[] unzipFileBytes(File zipFile, Charset charset, String name) {
        ZipFile zipFileObj = null;
        try {
            zipFileObj = new ZipFile(zipFile, charset);
            final Enumeration<ZipEntry> em = (Enumeration<ZipEntry>) zipFileObj.entries();
            ZipEntry zipEntry = null;
            while (em.hasMoreElements()) {
                zipEntry = em.nextElement();
                if (zipEntry.isDirectory()) {
                    continue;
                } else if (name.equals(zipEntry.getName())) {
                    return IoUtil.readBytes(zipFileObj.getInputStream(zipEntry));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IoUtil.close(zipFileObj);
        }
        return null;
    }

    // ----------------------------------------------------------------------------- Gzip

    /**
     * Gzip压缩处理
     *
     * @param content 被压缩的字符串
     * @param charset 编码
     * @return 压缩后的字节流
     * @throws UnsupportedEncodingException IO异常
     */
    public static byte[] gzip(String content, String charset) throws UnsupportedEncodingException {
        return gzip(content.getBytes(charset));
    }

    /**
     * Gzip压缩处理
     *
     * @param val 被压缩的字节流
     * @return 压缩后的字节流
     * @ IO异常
     */
    public static byte[] gzip(byte[] val) {
        FastByteArrayOutputStream bos = new FastByteArrayOutputStream(val.length);
        GZIPOutputStream gos = null;
        try {
            gos = new GZIPOutputStream(bos);
            gos.write(val, 0, val.length);
            gos.finish();
            gos.flush();
            val = bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IoUtil.close(gos);
        }
        return val;
    }

    /**
     * Gzip压缩文件
     *
     * @param file 被压缩的文件
     * @return 压缩后的字节流
     * @ IO异常
     */
    public static byte[] gzip(File file) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length());
        GZIPOutputStream gos = null;
        BufferedInputStream in;
        try {
            gos = new GZIPOutputStream(bos);
            in = FileUtil.getInputStream(file);
            IoUtil.copy(in, gos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IoUtil.close(gos);
        }
    }

    /**
     * Gzip解压缩处理
     *
     * @param buf     压缩过的字节流
     * @param charset 编码
     * @return 解压后的字符串
     * @throws UnsupportedEncodingException IO异常
     */
    public static String unGzip(byte[] buf, String charset) throws UnsupportedEncodingException {
        return new String(unGzip(buf), charset);
    }

    /**
     * Gzip解压处理
     *
     * @param buf buf
     * @return bytes
     * @ IO异常
     */
    public static byte[] unGzip(byte[] buf) {
        GZIPInputStream gzi = null;
        ByteArrayOutputStream bos = null;
        try {
            gzi = new GZIPInputStream(new ByteArrayInputStream(buf));
            bos = new ByteArrayOutputStream(buf.length);
            IoUtil.copy(gzi, bos);
            buf = bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IoUtil.close(gzi);
        }
        return buf;
    }

    // ----------------------------------------------------------------------------- Zlib

    /**
     * Zlib压缩处理
     *
     * @param content 被压缩的字符串
     * @param charset 编码
     * @param level   压缩级别，1~9
     * @return 压缩后的字节流
     * @since 4.1.4
     */
    public static byte[] zlib(String content, String charset, int level) throws UnsupportedEncodingException {
        return zlib(content.getBytes(charset), level);
    }

    /**
     * Zlib压缩文件
     *
     * @param file  被压缩的文件
     * @param level 压缩级别
     * @return 压缩后的字节流
     * @since 4.1.4
     */
    public static byte[] zlib(File file, int level) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedInputStream in = null;
        try {
            in = FileUtil.getInputStream(file);
            deflater(in, out, level);
        } finally {
            IoUtil.close(in);
        }
        return out.toByteArray();
    }

    /**
     * 打成Zlib压缩包
     *
     * @param buf   数据
     * @param level 压缩级别，0~9
     * @return 压缩后的bytes
     * @since 4.1.4
     */
    public static byte[] zlib(byte[] buf, int level) {
        final ByteArrayInputStream in = new ByteArrayInputStream(buf);
        final ByteArrayOutputStream out = new ByteArrayOutputStream(buf.length);
        deflater(in, out, level);
        return out.toByteArray();
    }

    /**
     * Zlib解压缩处理
     *
     * @param buf     压缩过的字节流
     * @param charset 编码
     * @return 解压后的字符串
     * @since 4.1.4
     */
    public static String unZlib(byte[] buf, String charset) throws UnsupportedEncodingException {
        return new String(unZlib(buf), charset);
    }

    /**
     * 解压缩zlib
     *
     * @param buf 数据
     * @return 解压后的bytes
     * @since 4.1.4
     */
    public static byte[] unZlib(byte[] buf) {
        final ByteArrayInputStream in = new ByteArrayInputStream(buf);
        final ByteArrayOutputStream out = new ByteArrayOutputStream(buf.length);
        inflater(in, out);
        return out.toByteArray();
    }

    // ---------------------------------------------------------------------------------------------- Private method start

    /**
     * 获得 {@link ZipOutputStream}
     *
     * @param zipFile 压缩文件
     * @param charset 编码
     * @return {@link ZipOutputStream}
     */
    private static ZipOutputStream getZipOutputStream(File zipFile, Charset charset) {
        return getZipOutputStream(FileUtil.getOutputStream(zipFile), charset);
    }

    private static ZipOutputStream getZipOutputStream(OutputStream out, Charset charset) {
        charset = (null == charset) ? DEFAULT_CHARSET : charset;
        return new ZipOutputStream(out, charset);
    }

    /**
     * 递归压缩文件夹<br>
     * srcRootDir决定了路径截取的位置，例如：<br>
     * file的路径为d:/a/b/c/d.txt，srcRootDir为d:/a/b，则压缩后的文件与目录为结构为c/d.txt
     *
     * @param out        压缩文件存储对象
     * @param srcRootDir 被压缩的文件夹根目录
     * @param file       当前递归压缩的文件或目录对象
     * @ IO异常
     */
    private static void zip(File file, String srcRootDir, ZipOutputStream out) {
        if (file == null) {
            return;
        }

        final String subPath = FileUtil.subPath(srcRootDir, file); // 获取文件相对于压缩文件夹根目录的子路径
        if (file.isDirectory()) {// 如果是目录，则压缩压缩目录中的文件或子目录
            final File[] files = file.listFiles();
            if ((files == null || files.length == 0) && StringUtils.isNotEmpty(subPath)) {
                // 加入目录，只有空目录时才加入目录，非空时会在创建文件时自动添加父级目录
                addDir(subPath, out);
            }
            // 压缩目录下的子文件或目录
            for (File childFile : files) {
                zip(childFile, srcRootDir, out);
            }
        } else {// 如果是文件或其它符号，则直接压缩该文件
            addFile(file, subPath, out);
        }
    }

    /**
     * 添加文件到压缩包
     *
     * @param file 需要压缩的文件
     * @param path 在压缩文件中的路径
     * @param out  压缩文件存储对象
     * @ IO异常
     * @since 4.0.5
     */
    private static void addFile(File file, String path, ZipOutputStream out) {
        BufferedInputStream in = null;
        try {
            in = FileUtil.getInputStream(file);
            addFile(in, path, out);
        } finally {
            IoUtil.close(in);
        }
    }

    /**
     * 添加文件流到压缩包，不关闭输入流
     *
     * @param in   需要压缩的输入流
     * @param path 压缩的路径
     * @param out  压缩文件存储对象
     * @ IO异常
     */
    private static void addFile(InputStream in, String path, ZipOutputStream out) {
        if (null == in) {
            return;
        }
        try {
            out.putNextEntry(new ZipEntry(path));
            IoUtil.copy(in, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeEntry(out);
        }
    }

    /**
     * 在压缩包中新建目录
     *
     * @param path 压缩的路径
     * @param out  压缩文件存储对象
     * @ IO异常
     */
    private static void addDir(String path, ZipOutputStream out) {
        path = addSuffixIfNot(path, "/");
        try {
            out.putNextEntry(new ZipEntry(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeEntry(out);
        }
    }

    /**
     * 如果给定字符串不是以suffix结尾的，在尾部补充 suffix
     *
     * @param str    字符串
     * @param suffix 后缀
     * @return 补充后的字符串
     */
    public static String addSuffixIfNot(CharSequence str, CharSequence suffix) {
        if (StringUtils.isEmpty(str) || StringUtils.isEmpty(suffix)) {
            return null == str ? null : str.toString();
        }

        final String str2 = str.toString();
        final String suffix2 = suffix.toString();
        if (false == str2.endsWith(suffix2)) {
            return str2.concat(suffix2);
        }
        return str2;
    }

    /**
     * 判断压缩文件保存的路径是否为源文件路径的子文件夹，如果是，则抛出异常（防止无限递归压缩的发生）
     *
     * @param zipFile 压缩后的产生的文件路径
     */
    private static void validateFiles(File zipFile, File... srcFiles) {

        for (File srcFile : srcFiles) {
            if (null == srcFile) {
                continue;
            }
            if (false == srcFile.exists()) {
                throw new RuntimeException(String.format("File %s not exist!", srcFile.getAbsolutePath()));
            }

//            try {
//                // 压缩文件不能位于被压缩的目录内
////                if (srcFile.isDirectory() && zipFile.getCanonicalPath().contains(srcFile.getCanonicalPath())) {
////                    throw new RuntimeException("[zipPath] must not be the child directory of [srcPath]!");
////                }
//
//                if (false == zipFile.exists()) {
//                    FileUtil.touch(zipFile);
//                }
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
        }
    }

    /**
     * 关闭当前Entry，继续下一个Entry
     *
     * @param out ZipOutputStream
     */
    private static void closeEntry(ZipOutputStream out) {
        try {
            out.closeEntry();
        } catch (IOException e) {
            // ignore
        }
    }

    /**
     * 从Zip文件流中拷贝文件出来
     *
     * @param zipFile     Zip文件
     * @param zipEntry    zip文件中的子文件
     * @param outItemFile 输出到的文件
     * @throws IOException IO异常
     */
    private static void copy(ZipFile zipFile, ZipEntry zipEntry, File outItemFile) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = zipFile.getInputStream(zipEntry);
            out = FileUtil.getOutputStream(outItemFile);
            IoUtil.copy(in, out);
        } finally {
            IoUtil.close(out);
            IoUtil.close(in);
        }
    }

    /**
     * 将Zlib流解压到out中
     *
     * @param in  zlib数据流
     * @param out 输出
     */
    private static void inflater(InputStream in, OutputStream out) {
        final InflaterOutputStream ios = (out instanceof InflaterOutputStream) ? (InflaterOutputStream) out : new InflaterOutputStream(out, new Inflater(true));
        IoUtil.copy(in, ios);
    }

    /**
     * 将普通数据流压缩成zlib到out中
     *
     * @param in    zlib数据流
     * @param out   输出
     * @param level 压缩级别，0~9
     */
    private static void deflater(InputStream in, OutputStream out, int level) {
        final DeflaterOutputStream ios = (out instanceof DeflaterOutputStream) ? (DeflaterOutputStream) out : new DeflaterOutputStream(out, new Deflater(level, true));
        IoUtil.copy(in, ios);
    }
    // ---------------------------------------------------------------------------------------------- Private method end

}
