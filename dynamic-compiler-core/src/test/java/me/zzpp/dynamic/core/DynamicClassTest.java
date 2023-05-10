package me.zzpp.dynamic.core;

import me.zzpp.dynamic.core.handler.DefaultDynamicClassHandlerImpl;
import me.zzpp.dynamic.core.handler.DynamicClassHandler;

/**
 * test
 *
 * @author zhangpeng
 * @create 2023/5/10 15:36
 */
public class DynamicClassTest {

    private static final DynamicClassHandler dynamicClassHandler = new DefaultDynamicClassHandlerImpl(DynamicClassHandler.CompilerType.Cmd);

    public static void main(String[] args) throws Exception {
        Class<?> aClass = dynamicClassHandler.loadClass("Test",testJavaCode);
        Object invoke = dynamicClassHandler.invoke("Test", "getName", new Class[]{String.class}, new Object[]{"test"});
        System.out.println(invoke);
    }


    private static String testJavaCode = "" +
            "public class Test {\n" +
            "\n" +
            "    public String getName(String str){\n" +
            "        System.out.println(\"hello world \"+str);\n" +
                    "if(true) throw new RuntimeException(\"这是一个错误\");"+
            "        return str;\n" +
            "    }\n" +
            "}\n";
}
