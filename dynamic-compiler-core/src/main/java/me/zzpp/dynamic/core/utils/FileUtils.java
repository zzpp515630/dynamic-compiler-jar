package me.zzpp.dynamic.core.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 创建java文件到临时目录
 *
 * @author zhangpeng
 * @create 2023/1/18 17:13
 */
@Slf4j
public class FileUtils {

    /**
     * 创建java文件
     *
     * @param packageName
     * @param className
     * @param suffix
     * @param content
     * @return
     * @throws IOException
     */
    public static File createTempFileWithFileNameAndContent(String packageName, String className, String suffix, byte[] content) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        File file;
        if (null != packageName && !"".equals(packageName)) {
            String packagePath = packageName.replace(".", "/");
            File fileDes = new File(tempDir, packagePath);
            boolean mkdirs = fileDes.mkdirs();
            log.info("create package directory {}", mkdirs);
            file = new File(fileDes, className.concat(suffix));
        } else {
            file = new File(tempDir, className.concat(suffix));
        }
        OutputStream os = Files.newOutputStream(file.toPath());
        os.write(content, 0, content.length);
        os.flush();
        os.close();
        return file;
    }

    /**
     * 从jar中提取lib包并存放到指定位置
     * @param jarPath
     * @param destinationDir
     * @return
     * @throws IOException
     */
    public static File jarToLib(String jarPath, String destinationDir) throws IOException {
        File jarFile = new File(jarPath);

        File desFile = new File(destinationDir);
        String tempDir = System.getProperty("java.io.tmpdir");
        File tempFileDir = new File(tempDir, UUID.randomUUID().toString());
        boolean tempMkdirs = tempFileDir.mkdirs();
        log.info("jarToLib temp path:【{}】 mkdirs:【{}】", tempFileDir.getAbsolutePath(), tempMkdirs);
        if (desFile.exists()) {
//            boolean delete = desFile.delete();
//            log.info("jarToLib destinationDir  path:【{}】 delete【{}】", desFile.getAbsolutePath(), delete);
        }
        boolean mkdirs = desFile.mkdirs();
        log.info("jarToLib destinationDir path:【{}】 mkdirs【{}】", desFile.getAbsolutePath(), mkdirs);
        //复制jar
        File tempJarFile = new File(tempDir, jarFile.getName());
        File newJarFile = new File(desFile, jarFile.getName());
        Files.copy(jarFile.toPath(), tempJarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        log.debug("jarToLib copy {} to path:【{}】 success", tempJarFile.getName(), tempJarFile.getAbsolutePath());
        //解压jar
        unzipJar(tempJarFile.getAbsolutePath(), tempFileDir.getAbsolutePath());
        //压缩原项目
        File jarSourceFile = new File(tempFileDir, "BOOT-INF/classes");
        toZip(jarSourceFile.getAbsolutePath(), newJarFile.getAbsolutePath());
        //复制lib中的jar
        File file = new File(tempFileDir, "BOOT-INF/lib");
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            if (null != files) {
                for (File listFile : files) {
                    File newFile = new File(desFile, listFile.getName());
                    Files.move(listFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    log.debug("jarToLib copy {} to path:【{}】 success", newFile.getName(), newFile.getAbsolutePath());
                }
            }
        }
        boolean delete = tempFileDir.delete();
        log.info("jarToLib copy lib to path:【{}】 success", desFile.getAbsolutePath());
        return desFile;
    }

    /**
     * 解压jar文件到指定目录
     *
     * @param jarPath
     * @param destinationDir
     * @throws IOException
     */
    public static void unzipJar(String jarPath, String destinationDir) throws IOException {
        try (JarFile jar = new JarFile(new File(jarPath));) {
            // fist get all directories,
            // then make those directory on the destination Path
            for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements(); ) {
                JarEntry entry = (JarEntry) enums.nextElement();
                String fileName = destinationDir + File.separator + entry.getName();
                File f = new File(fileName);
                if (fileName.endsWith("/")) {
                    boolean mkdirs = f.mkdirs();
                    log.debug("unzipJar mkdir path {}", mkdirs);
                }
            }
            //now create all files
            for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements(); ) {
                JarEntry entry = (JarEntry) enums.nextElement();
                String fileName = destinationDir + File.separator + entry.getName();
                File f = new File(fileName);
                if (!fileName.endsWith("/")) {

                    try (BufferedInputStream bis = new BufferedInputStream(jar.getInputStream(entry));
                         BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(f.toPath()));) {
                        byte[] buffer = new byte[1024 * 1024];
                        int len;
                        while ((len = bis.read(buffer)) != -1) {
                            bos.write(buffer, 0, len);
                        }
                    }
                }
            }
        }
    }


    /**
     * 压缩成ZIP 方法1
     *
     * @param zipFileName    压缩文件夹路径
     * @param sourceFileName 要压缩的文件路径
     * @throws RuntimeException 压缩失败会抛出运行时异常
     */
    public static void toZip(String sourceFileName, String zipFileName) {
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(Paths.get(zipFileName))))) {
            File sourceFile = new File(sourceFileName);
            long start = System.currentTimeMillis();//开始
            File[] listFiles = sourceFile.listFiles();
            if (null != listFiles) {
                for (File file : listFiles) {
                    compress(file, zos, file.getName());
                }
            } else {
                compress(sourceFile, zos, sourceFile.getName());
            }
            long end = System.currentTimeMillis();//结束
           log.debug("压缩完成，耗时：" + (end - start) + " 毫秒");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 递归压缩方法
     *
     * @param sourceFile 源文件
     * @param zos        zip输出流
     * @param name       压缩后的名称
     * @throws Exception
     */
    private static void compress(File sourceFile, ZipOutputStream zos, String name) throws Exception {
        if (sourceFile.isFile()) {
            // 向zip输出流中添加一个zip实体，构造器中name为zip实体的文件的名字
            zos.putNextEntry(new ZipEntry(name));
            // copy文件到zip输出流中
            try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(sourceFile.toPath()))) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = bis.read(buffer)) != -1) {
                    zos.write(buffer, 0, len);
                }
            }
        } else {
            File[] listFiles = sourceFile.listFiles();
            if (listFiles == null || listFiles.length == 0) {
                // 需要保留原来的文件结构时,需要对空文件夹进行处理
                // 空文件夹的处理
                zos.putNextEntry(new ZipEntry(name + "/"));
                // 没有文件，不需要文件的copy
                zos.closeEntry();
            } else {
                for (File file : listFiles) {
                    // 判断是否需要保留原来的文件结构
                    // 注意：file.getName()前面需要带上父文件夹的名字加一斜杠,
                    // 不然最后压缩包中就不能保留原来的文件结构,即：所有文件都跑到压缩包根目录下了
                    compress(file, zos, name + "/" + file.getName());
                }
            }
        }
    }

//    public static void main(String[] args) throws IOException {
//        String java = "F:\\workspace\\wxws\\order-center\\order-center-server\\target\\order-center-server.jar";
//        String des = "d:\\Desktop\\lib";
//        jarToLib(java, des);
//    }

}
