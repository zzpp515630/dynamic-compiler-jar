package me.zzpp.dynamic.core;

import me.zzpp.dynamic.core.handler.DefaultDynamicClassHandlerImpl;
import me.zzpp.dynamic.core.handler.DynamicClassHandler;
import me.zzpp.dynamic.core.utils.DynamicClassUtils;
import me.zzpp.dynamic.core.utils.FileUtils;

import java.io.File;

/**
 * test
 *
 * @author zhangpeng
 * @create 2023/5/10 15:36
 */
public class DynamicClassTest {

    private static final DynamicClassHandler dynamicClassHandler = new DefaultDynamicClassHandlerImpl(DynamicClassHandler.CompilerType.Task);

    public static void main(String[] args) throws Exception {

//        System.out.println(DynamicClassUtils.getPackageName(testJavaCode));
//        Class<?> aClass = dynamicClassHandler.loadClass("Test",testJavaCode);
//        Object invoke = dynamicClassHandler.invoke("Test", "getName", new Class[]{String.class}, new Object[]{"test"});
//        System.out.println(invoke);
        DefaultDynamicClassHandlerImpl defaultDynamicClassHandler = new DefaultDynamicClassHandlerImpl(DynamicClassHandler.CompilerType.Task);
        defaultDynamicClassHandler.setClassLibFile(new File("F:\\workspace\\java\\nas-crontab\\target\\lib"));
//        dynamicBeanHandler.getClassHandler().setClassLibFile(new File("F:\\workspace\\java\\nas-crontab\\target\\lib"));
        defaultDynamicClassHandler.loadClass("Test",code);
        defaultDynamicClassHandler.invoke("Test","test",new Class[]{String.class},new Object[]{"test"});
    }


    private static String testJavaCode = "package me.zzpp.dynamic.core.utils;import me.zzpp.dynamic.core.utils.DynamicClassUtils;" +
            "public class Test {\n" +
            "\n" +
            "    public String getName(String str){\n" +
            "        System.out.println(\"hello world \"+str);\n" +
                    "if(true) throw new RuntimeException(\"这是一个错误\");"+
            "        return str;\n" +
            "    }\n" +
            "}\n";


    static String code = "package me.service.cron.runner;\n" +
            "\n" +
            "import com.baomidou.mybatisplus.core.toolkit.Wrappers;\n" +
            "import me.service.cron.mapper.SystemMapper;\n" +
            "import me.service.cron.model.entity.SystemEntity;\n" +
            "import org.springframework.beans.factory.annotation.Autowired;\n"+
            "//import org.springframework.stereotype.Component;\n" +
            "\n" +
            "import javax.annotation.Resource;\n" +
            "\n" +
            "/**\n" +
            " * @author zzpp\n" +
            " * @create 2023/5/10 17:05\n" +
            " */\n" +
            "//@Component\n" +
            "public class Test {\n" +
            "     @Autowired\n" +
            "    private  SystemMapper systemMapper;\n" +
            "\n" +
            "    public void test(String str){\n" +
            "        SystemEntity systemEntity = systemMapper.selectOne(Wrappers.lambdaQuery(SystemEntity.class));\n" +
            "        if (null == systemEntity) {\n" +
            "            System.out.println(\"空的\"+str);\n" +
            "        }else{\n" +
            "        System.err.println(\"空的\"+str);\n"+
            "            System.err.println(systemEntity);\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "}\n";
}
