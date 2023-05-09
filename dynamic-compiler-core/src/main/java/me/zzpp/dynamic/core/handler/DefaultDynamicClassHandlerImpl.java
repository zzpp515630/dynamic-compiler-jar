package me.zzpp.dynamic.core.handler;

import lombok.Setter;
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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态编译实现接口实现
 *
 * @author zzpp
 */
@Slf4j
public class DefaultDynamicClassHandlerImpl implements DynamicClassHandler {

    private String cmd;

    private final CompilerType compilerType;

    @Setter
    private boolean isCache = true;

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
    public Class<?> loadClass(List<String> classLibPaths, String javaCode) {
        String className = getClassName(javaCode);
        return loadClass(className, classLibPaths, javaCode);
    }

    @Override
    public Class<?> loadClass(File classLibFile, String javaCode) {
        String className = getClassName(javaCode);
        return loadClass(className, classLibFile, javaCode);
    }

    @Override
    public Class<?> loadClass(String className, String javaCode) {
        return loadClass(className, new ArrayList<>(), javaCode);
    }

    @SneakyThrows
    @Override
    public Class<?> loadClass(String className, List<String> classLibPaths, String javaCode) {
        Compiler compiler;
        if (CompilerType.Javac == compilerType) {
            compiler = new JavacCompiler();
        } else if (CompilerType.Task == compilerType) {
            compiler = new ClassPathCompiler(classLibPaths);
        } else if (CompilerType.Cmd == compilerType) {
            compiler = new CmdCompiler(cmd, classLibPaths);
        } else {
            throw new RuntimeException("不支持的类型");
        }
        return loadClass(compiler, className, javaCode);
    }


    @SneakyThrows
    @Override
    public Class<?> loadClass(String className, File cleasFile, String javaCode) {
        Compiler compiler;
        if (CompilerType.Javac == compilerType) {
            compiler = new JavacCompiler();
        } else if (CompilerType.Task == compilerType) {
            compiler = new ClassPathCompiler(cleasFile);
        } else if (CompilerType.Cmd == compilerType) {
            compiler = new CmdCompiler(cmd, cleasFile);
        } else {
            throw new RuntimeException("不支持的类型");
        }
        return loadClass(compiler, className, javaCode);
    }

    private synchronized Class<?> loadClass(Compiler compiler, String className, String javaCode) throws IOException {
        log.info("loadClass，compile {},start", className);
        log.debug("loadClass，compile code: \n{}", javaCode);
        String javaName = getClassName(javaCode);
        if (!javaName.equals(className)) {
            javaCode = replaceClassName(className, javaCode);
        }
        String packageName = getPackageName(javaCode);
        File file = FileUtils.createTempFileWithFileNameAndContent(packageName, className, ".java", javaCode.getBytes());
        //删除缓存
        if (isCache) cacheClass.remove(className);
        //获取新的包名
        String newClassName = getClassName(packageName, className);
        //编译class
        compiler.compiler(newClassName, file);
        //加载class
        Class<?> aClass = loadClass(newClassName, file);
        //写入缓存
        if (isCache) cacheClass.put(className, aClass);
        return aClass;
    }

    @Override
    public Object invoke(String className, String methodName) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (find(className)) return null;
        Class<?> aClass = cacheClass.get(className);
        return invoke(aClass, methodName, InvokeArgs.builder().build(), null);
    }

    @Override
    public Object invoke(String className, String methodName, Object[] args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (find(className)) return null;
        Class<?> aClass = cacheClass.get(className);
        return invoke(aClass, methodName, args);
    }

    @Override
    public Object invoke(String className, String methodName, Class<?>[] parameterTypes, Object[] args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (find(className)) return null;
        Class<?> aClass = cacheClass.get(className);
        return invoke(aClass, methodName, parameterTypes, args);
    }

    @Override
    public Object invoke(Class<?> clz, String methodName) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        return invoke(clz, methodName, InvokeArgs.builder().build(), null);
    }

    @Override
    public Object invoke(Class<?> clz, String methodName, Object... args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Method[] declaredMethods = clz.getDeclaredMethods();
        Optional<Class<?>[]> first = Arrays.stream(declaredMethods)
                .filter(x -> x.getName().equals(methodName))
                .filter(x -> {
                    Class<?>[] parameterTypes = x.getParameterTypes();
                    if (parameterTypes.length == args.length) {
                        for (int i = 0; i < args.length; i++) {
                            Object arg = args[i];
                            Object parameterType = parameterTypes[i];
                            if (!arg.getClass().equals(parameterType.getClass())) {
                                return true;
                            }
                        }
                        return true;
                    }
                    return false;
                }).map(Method::getParameterTypes).findFirst();
        Class<?>[] parameterTypes = first.orElse(new Class[0]);
        InvokeArgs invokeArgs = InvokeArgs.builder().parameterTypes(parameterTypes).args(args).build();
        return invoke(clz, methodName, null, invokeArgs);
    }

    @Override
    public Object invoke(Class<?> clz, String methodName, Class<?>[] parameterTypes, Object[] args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        InvokeArgs invokeArgs = InvokeArgs.builder().parameterTypes(parameterTypes).args(args).build();
        return invoke(clz, methodName, null, invokeArgs);
    }

    @Override
    public Object invoke(Class<?> clz, String methodName, InvokeArgs constructorArgs, InvokeArgs methodArgs) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        try {
            InvokeArgs invokeMethodArgs = Optional.ofNullable(methodArgs).orElse(InvokeArgs.builder().build());
            Method method = clz.getDeclaredMethod(methodName, invokeMethodArgs.getParameterTypes());
            int modifiers = method.getModifiers();
            if (Modifier.isStatic(modifiers)) {
                return MethodUtils.invokeExactStaticMethod(clz, methodName, invokeMethodArgs.getArgs(), invokeMethodArgs.getParameterTypes());
            } else {
                InvokeArgs invokeConstructorArgs = Optional.ofNullable(constructorArgs).orElse(InvokeArgs.builder().build());
                Object obj = clz.getDeclaredConstructor(invokeConstructorArgs.getParameterTypes()).newInstance(invokeConstructorArgs.getArgs());
                return MethodUtils.invokeExactMethod(obj, methodName, invokeMethodArgs.getArgs(), invokeMethodArgs.getParameterTypes());
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
            return c;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    abstract static class Compiler {

        protected final List<String> classLibPaths;

        protected final File classLibFile;

        private Compiler(File classLibFile, List<String> classLibPaths) {
            this.classLibPaths = classLibPaths;
            this.classLibFile = classLibFile;
        }

        public Compiler(List<String> classLibPaths) {
            this(null, classLibPaths);
        }

        public Compiler(File classLibFile) {
            this(classLibFile, null);
        }

        /**
         * 编译
         * @param className
         * @param file
         */
        abstract void compiler(String className, File file);

    }

    private static class ClassPathCompiler extends Compiler {

        public ClassPathCompiler(List<String> classLibPaths) {
            super(classLibPaths);
        }

        public ClassPathCompiler(File classLibFile) {
            super(classLibFile);
        }


        @Override
        public void compiler(String className, File file) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
            Iterable<? extends JavaFileObject> javaFileObjects = fileManager.getJavaFileObjects(file);
            List<String> options;
            if (null != classLibFile) {
                options = Arrays.asList("-encoding", "utf-8", "-Djava.ext.dirs=", classLibFile.getAbsolutePath());
            } else {
                String classPath;
                if (Platform.isWindows()) {
                    classPath = String.join(";", classLibPaths);
                } else {
                    classPath = String.join(":", classLibPaths);
                }
                options = Arrays.asList("-encoding", "utf-8", "-cp", classPath);
            }
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

    private static class JavacCompiler extends Compiler {

        public JavacCompiler() {
            super(null, null);
        }

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

    private static class CmdCompiler extends Compiler {

        private final String cmdPath;

        private CmdCompiler(String cmdPath, File classLibFile, List<String> classLibPaths) {
            super(classLibFile, classLibPaths);
            if (null == cmdPath || "".equals(cmdPath)) {
                this.cmdPath = "javac";
            } else {
                File file = new File(cmdPath);
                if (file.isDirectory()) {
                    this.cmdPath = new File(file, "javac").getAbsolutePath();
                } else {
                    this.cmdPath = file.getAbsolutePath();
                }
            }
        }

        public CmdCompiler(String cmdPath, List<String> classLibPaths) {
            this(cmdPath, null, classLibPaths);
        }

        public CmdCompiler(String cmdPath, File classLibFile) {
            this(cmdPath, classLibFile, null);
        }

        @Override
        public void compiler(String className, File file) {
            File classFile = new File(file.getAbsolutePath().replace(".java", ".class"));
            boolean delete = classFile.delete();
            log.info("java compiler init delete {} is {}", classFile.getAbsolutePath(), delete);
            String execute;
            if ((null == classLibPaths || classLibPaths.isEmpty()) && classLibFile == null) {
                execute = cmdPath + " -encoding utf-8 " + file.getAbsolutePath();
            } else if (null != classLibFile) {
                execute = cmdPath + " -encoding utf-8 -Djava.ext.dirs=" + classLibFile.getAbsolutePath() + " " + file.getAbsolutePath();
            } else {
                String classPath;
                if (Platform.isWindows()) {
                    classPath = String.join(";", classLibPaths);
                } else {
                    classPath = String.join(":", classLibPaths);
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

