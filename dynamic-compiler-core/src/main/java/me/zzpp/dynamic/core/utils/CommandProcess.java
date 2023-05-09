package me.zzpp.dynamic.core.utils;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 执行脚本工具类
 *
 * @author zzpp
 */
@Slf4j
public class CommandProcess {

    private final String[] commandPrefix;

    private String charsets;

    public CommandProcess() {
        if (isWindows()) {
            this.commandPrefix = new String[]{"cmd", "/C"};
        } else {
            this.commandPrefix = new String[]{"/bin/bash", "-c"};
        }
        setCharsets();
    }

    public String[] getCommandPrefix() {
        return commandPrefix;
    }

    public CommandProcess(String[] commandPrefix) {
        this.commandPrefix = commandPrefix;
        setCharsets();
    }

    private void setCharsets() {
        if (isWindows()) {
            this.charsets = "GBK";
        } else {
            this.charsets = "UTF-8";
        }
    }

    public Pair<Integer, List<String>> execute(String[] env, String command) {
        Process process = null;
        try {
            log.debug("execute starting command:{}", command);
            //执行终端命令
            process = Runtime.getRuntime().exec(analysisCommand(command), env);
            List<String> result = streamExport(process);
            int i = process.waitFor();
            log.debug("execute completed command:{}", command);
            return Pair.of(i, result);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(e.getMessage(), e);
        } finally {
            if (null != process) {
                process.destroy();
            }
        }
        log.error("execute fail command:{}", command);
        return Pair.of(-1, new ArrayList<>());
    }

    public Integer executeStepping(String[] env, String command) {
        Process process = null;
        try {
            log.debug("execute starting command:{}", command);
            //执行终端命令
            process = Runtime.getRuntime().exec(analysisCommand(command), env);
            streamExportStepping(process);
            int i = process.waitFor();
            log.debug("execute completed command:{}", command);
            return i;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(e.getMessage(), e);
        } finally {
            if (null != process) {
                process.destroy();
            }
        }
        log.error("execute fail command:{}", command);
        return -1;
    }

    public Pair<Integer, String> execute(String command) {
        return execute(null, "\n", command);
    }

    public Pair<Integer, String> execute(String delimiter, String command) {
        return execute(null, delimiter, command);
    }


    public Pair<Integer, String> execute(String[] env, String delimiter, String command) {
        Pair<Integer, List<String>> execute = execute(env, command);
        return Pair.of(execute.getKey(), String.join(delimiter, execute.getValue()));
    }


    public Integer executeStepping(String command) {
        return executeStepping(null, command);
    }

    private String[] analysisCommand(String cmd) {
        List<String> command = commandPrefix != null && commandPrefix.length > 0 ?
                new ArrayList<>(Arrays.asList(commandPrefix)) : new ArrayList<>();
        command.add(cmd);
        return command.toArray(new String[0]);
    }

    private boolean isWindows() {
        return System.getProperties().getProperty("os.name").toUpperCase().contains("WINDOWS");
    }

    private synchronized List<String> streamExport(Process process) {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        List<String> result = Collections.synchronizedList(new ArrayList<String>());
        executorService.execute(() -> {
            try (BufferedReader read = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName(charsets)))) {
                String line;
                while ((line = read.readLine()) != null) {
                    result.add(line);
                }
            } catch (Exception ignore) {
            }
        });
        executorService.execute(() -> {
            try (BufferedReader readError = new BufferedReader(new InputStreamReader(process.getErrorStream(), Charset.forName(charsets)));) {
                String lineError;
                while ((lineError = readError.readLine()) != null) {
                    result.add(lineError);
                }
            } catch (Exception ignore) {
            }
        });
        return result;
    }

    private synchronized void streamExportStepping(Process process) {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(() -> {
            try (BufferedReader read = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName(charsets)))) {
                String line;
                while ((line = read.readLine()) != null) {
                    System.out.println("process sout:"+line);
                    log.info("process:{}",line);
                }

            } catch (Exception ignore) {
            }
        });
        executorService.execute(() -> {
            try (BufferedReader readError = new BufferedReader(new InputStreamReader(process.getErrorStream(), Charset.forName(charsets)));) {
                String lineError;
                while ((lineError = readError.readLine()) != null) {
                    System.err.println("process serr:"+lineError);
                    log.error("process:{}",lineError);
                }
            } catch (Exception ignore) {
            }
        });
    }
}
