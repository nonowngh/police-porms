package mb.fw.atb.strategy;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.config.Specifications;

@Slf4j
@Data
public class ATBPattern {

    public Map<String, String> patternInfo = new HashMap<>();
    public Map<String, Specifications> strategyMap = new HashMap();

    //patternName , List<String>
    public Map<String, List<String>> patternSubClassName = new HashMap();


    public ATBPattern() {
        patternInfo.put("pubsub", "PUB/SUB");
        patternInfo.put("batch", "배치");
        patternInfo.put("provider", "프로 바이더");
        patternInfo.put("na", "TCP/IP 연동");

    }

    /**
     * {
     * "batch": "배치",
     * "na": "TCP/IP 연동",
     * "provider": "프로 바이더",
     * "pubsub": "PUB/SUB",
     * }
     */
    //json값을 맵에 포함한다
    public void load(String packageName) throws IOException {

        String doller = "$";
        patternSubClassName = (HashMap<String, List<String>>) getClassNamesByDirectory(packageName);
        log.info("patternSubClassName : {}", patternSubClassName);

        patternSubClassName.forEach((k, v) -> {
            List<String> jarEntries = (List<String>) v;
            jarEntries.forEach(className -> {
                //log.info("className : {}", className);
                //className 의 instance를 생성하여 메소드를 호출한다
                try {
                    Class<?> clazz = Class.forName(className);
                    ATBStrategy instance = (ATBStrategy) clazz.newInstance();
                    Specifications specifications = instance.specifications();
                    //log.info("specifications : {}", specifications);
                    String subClassName = className.substring(className.lastIndexOf(".") + 1);
                    if(specifications != null){
                        strategyMap.put(k + "." + subClassName, specifications);
                    }
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            });
        });

    }

    public static Map<String, List<String>> getClassNamesByDirectory(String directoryPath) {
        Map<String, List<String>> classNamesMap = new HashMap<>();
        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        String pkg = directoryPath.replace("/", ".").replace("src.main.java.", "");
                        List<String> classNames = findClasses(file, pkg + "." + file.getName());
                        classNamesMap.put(file.getName(), classNames);
                    }
                }
            }
        }
        return classNamesMap;
    }

    private static List<String> findClasses(File directory, String packageName) {
        List<String> classNames = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    classNames.addAll(findClasses(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".java")) {
                    classNames.add(packageName + '.' + file.getName().substring(0, file.getName().length() - 5));
                }
            }
        }
        return classNames;
    }
}
