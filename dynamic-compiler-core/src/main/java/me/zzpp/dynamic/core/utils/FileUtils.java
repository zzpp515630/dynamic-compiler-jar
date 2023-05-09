package me.zzpp.dynamic.core.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * @author zhangpeng
 * @create 2023/1/18 17:13
 */
@Slf4j
public class FileUtils {

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

}
