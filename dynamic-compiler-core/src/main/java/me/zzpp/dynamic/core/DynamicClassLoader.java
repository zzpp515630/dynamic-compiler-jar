package me.zzpp.dynamic.core;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;

@Slf4j
public class DynamicClassLoader extends URLClassLoader {

    private String classPath;

    public DynamicClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
//        classPath = urls[0].getPath();
    }

//    @Override
//    protected Class<?> findClass(String name) throws ClassNotFoundException {
//        try {
//            return super.findClass(name);
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//        }
//        String className = name.replace('.', '/').concat(".class");
//        try {
//            byte[] classByte = getClassByte(className);
//            return defineClass(name, classByte, 0, classByte.length);
//        } catch (Exception e) {
//            throw new ClassNotFoundException(name, e);
//        }
//    }

    private byte[] getClassByte(String className) throws ClassNotFoundException {
        String realPath = this.classPath + className;
        File f = new File(realPath);
        if (f.exists()) {
            byte[] bytes = new byte[0];
            try {
                FileInputStream fis = new FileInputStream(f);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int len;
                byte[] buffer = new byte[1024];
                while ((len = fis.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                bytes = baos.toByteArray();
                fis.close();
                baos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            log.info("class loader read file:{} success", realPath);
            return bytes;
        } else {
            log.error("class loader read file:{} error", realPath);
            throw new ClassNotFoundException(className);
        }
    }
}

