package me.zzpp.dynamic.core.utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * @author zhangpeng
 * @create 2023/1/18 17:13
 */
public class FileUtils {

    public static File createTempFileWithFileNameAndContent(String beanName, String suffix, byte[] content) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        File file = new File(tempDir + "/" + beanName + suffix);
        OutputStream os = Files.newOutputStream(file.toPath());
        os.write(content, 0, content.length);
        os.flush();
        os.close();
        return file;
    }

}
