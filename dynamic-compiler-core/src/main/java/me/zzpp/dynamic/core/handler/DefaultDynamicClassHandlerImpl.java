package me.zzpp.dynamic.core.handler;

import com.sun.tools.javac.resources.compiler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.zzpp.dynamic.core.DynamicClassLoader;
import me.zzpp.dynamic.core.utils.CommandProcess;
import me.zzpp.dynamic.core.utils.FileUtils;
import me.zzpp.dynamic.core.utils.Platform;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DefaultDynamicClassHandlerImpl implements DynamicClassHandler {

    private String cmd;

    private final CompilerType compilerType;

    private final Map<String, Class<?>> cacheClass = new ConcurrentHashMap<>();

    public DefaultDynamicClassHandlerImpl() {
        this.compilerType = CompilerType.Task;
    }

    public DefaultDynamicClassHandlerImpl(CompilerType compilerType) {
        this.compilerType = compilerType;
    }

    public DefaultDynamicClassHandlerImpl(CompilerType compilerType, String cmdPath) {
        this.cmd = cmdPath;
        this.compilerType = compilerType;
    }


    @Override
    public Class<?> loadClass(String javaCode) {
        String className = getClassName(javaCode);
        return loadClass(className, javaCode);
    }

    @Override
    public Class<?> loadClass(List<String> classPaths, String javaCode) {
        String className = getClassName(javaCode);
        return loadClass(className, classPaths, javaCode);
    }

    @SneakyThrows
    @Override
    public Class<?> loadClass(String className, String javaCode) {
        return loadClass(className, null, javaCode);
    }

    @SneakyThrows
    @Override
    public Class<?> loadClass(String className, List<String> classPaths, String javaCode) {
        log.info("loadClass，compile {},start", className);
        log.info("loadClass，compile code: \n{}", javaCode);
        File file = FileUtils.createTempFileWithFileNameAndContent(className, ".java", javaCode.getBytes());
        cacheClass.remove(className);
        Compiler compiler;
        if (CompilerType.Javac == compilerType) {
            compiler = new JavacCompiler();
        } else if (CompilerType.Task == compilerType) {
            compiler = new ClassPathCompiler(classPaths);
        } else if (CompilerType.Cmd == compilerType) {
            compiler = new CmdCompiler(cmd, classPaths);
        } else {
            throw new RuntimeException("不支持的类型");
        }
        compiler.compiler(className, file);
        return loadClass(className, file);
    }


    @Override
    public Object invoke(String className, String methodName) {
        if (find(className)) return null;
        Class<?> aClass = cacheClass.get(className);
        return invoke(aClass, methodName);
    }

    @Override
    public Object invoke(String className, String methodName, Class<?>[] parameterTypes, Object[] args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (find(className)) return null;
        Class<?> aClass = cacheClass.get(className);
        return invoke(aClass, methodName, parameterTypes, args);
    }

    @Override
    public Object invoke(Class<?> clz, String methodName) {
        try {
            Method method = clz.getDeclaredMethod(methodName);
            int modifiers = method.getModifiers();
            if (Modifier.isStatic(modifiers)) {
                return MethodUtils.invokeExactStaticMethod(clz, methodName);
            } else {
                Object obj = clz.getDeclaredConstructor().newInstance();
                return MethodUtils.invokeExactMethod(obj, methodName);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.info("execute method failed，{}::{}", clz.getSimpleName(), methodName);
            log.error("execute method errMsg : {}", e.getMessage(), e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public Object invoke(Class<?> clz, String methodName, Class<?>[] parameterTypes, Object[] args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        try {
            Method method = clz.getDeclaredMethod(methodName);
            int modifiers = method.getModifiers();
            if (Modifier.isStatic(modifiers)) {
                return MethodUtils.invokeExactStaticMethod(clz, methodName, args, parameterTypes);
            } else {
                Object obj = clz.getDeclaredConstructor(parameterTypes).newInstance(args);
                return MethodUtils.invokeExactMethod(obj, methodName, methodName, args, parameterTypes);
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            log.info("execute method failed，{}::{}", clz.getSimpleName(), methodName);
            log.error("execute method errMsg : {}", e.getMessage(), e);
            throw e;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean find(String className) {
        if (!cacheClass.containsKey(className)) {
            return true;
        }
        log.error("the error is class {} not found !!", className);
        return false;
    }

    private Class<?> loadClass(String className, File file) throws MalformedURLException {
        log.info("loadClass {} loader start, to {}", className, file.getParent());
        URL[] urls = new URL[]{file.getParentFile().toURI().toURL()};
        try (URLClassLoader loader = new DynamicClassLoader(urls, Thread.currentThread().getContextClassLoader())) {
            Class<?> c = loader.loadClass(className);
            log.info("loadClass {} loader end", className);
            cacheClass.put(className, c);
            return c;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    interface Compiler {

        void compiler(String className, File file);

    }

    private static class ClassPathCompiler implements Compiler {

        private final List<String> classPaths;

        public ClassPathCompiler(List<String> classPaths) {
            this.classPaths = classPaths;
        }

        @Override
        public void compiler(String className, File file) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
            Iterable<? extends JavaFileObject> javaFileObjects = fileManager.getJavaFileObjects(file);
            String classPath;
            if (Platform.isWindows()) {
                classPath = String.join(";", classPaths);
            } else {
                classPath = String.join(":", classPaths);
            }
            List<String> options = Arrays.asList("-encoding", "utf-8", "-cp", classPath);
            log.debug("loadClass，compile options:\n{}", options);
            javax.tools.JavaCompiler.CompilationTask compilationTask = compiler.getTask(null, fileManager, null, options, null, javaFileObjects);
            Boolean call = compilationTask.call();
            if (BooleanUtils.isTrue(call)) {
                log.info("{} {}", className, "-编译成功");
            } else {
                throw new RuntimeException(String.format("动态编译失败，className %s ,path:%s", className, file.getParentFile()));
            }
        }
    }

    private static class JavacCompiler implements Compiler {

        @Override
        public void compiler(String className, File file) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            int result = compiler.run(null, null, null, file.getAbsolutePath());
            if (result == 0) {
                log.info("{} {}", className, "-编译成功");
            } else {
                throw new RuntimeException(String.format("动态编译失败，className %s ,path:%s", className, file.getParentFile()));
            }
        }
    }

    private static class CmdCompiler implements Compiler {

        private final String cmdPath;

        private final List<String> classPaths;

        public CmdCompiler(String cmdPath, List<String> classPaths) {
            this.cmdPath = (null == cmdPath || "".equals(cmdPath)) ? "javac" : (cmdPath + "/javac");
            this.classPaths = classPaths;
        }


        @Override
        public void compiler(String className, File file) {
            File classFile = new File(file.getParent(), className + ".class");
            classFile.delete();
            String execute;
            if (null == classPaths || classPaths.isEmpty()) {
                execute = cmdPath + " -encoding utf-8 " + file.getAbsolutePath();
            } else {
                String classPath;
                if (Platform.isWindows()) {
                    classPath = String.join(";", classPaths);
                } else {
                    classPath = String.join(":", classPaths);
                }
                execute = cmdPath + " -encoding utf-8 -cp " + classPath + " " + file.getAbsolutePath();
            }
            CommandProcess commandProcess = new CommandProcess();
            Integer integer = commandProcess.executeStepping(execute);
            log.info("java compiler result:{}", integer);
            boolean exists = classFile.exists();
            if (!exists) {
                throw new RuntimeException(String.format("动态编译失败，className %s ,path:%s", className, file.getParentFile()));
            }
            log.info("{} {}", className, "-编译成功");
        }
    }
}

