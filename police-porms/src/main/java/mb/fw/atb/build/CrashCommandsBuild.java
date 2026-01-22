package mb.fw.atb.build;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

@Slf4j
public class CrashCommandsBuild {

    static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String resourcePath = "crash/commands";

        // Get all files in the resource directory
        URL resourceUrl = classLoader.getResource(resourcePath);
        if (resourceUrl == null) {
            throw new IOException("Resource path not found: " + resourcePath);
        }

        File resourceDir = new File(resourceUrl.getPath());
        String[] fileNames = resourceDir.list();
        if (fileNames == null) {
            throw new IOException("No files found in resource path: " + resourcePath);
        }

        List<String> writeStrList = Lists.newArrayList();
        for (String file : fileNames) {
            String writeStr = resourcePath + "/" + file;
            log.info("command : {}", writeStr);
            writeStrList.add(writeStr);
        }

        // Write the list to commands.json in the resources directory
        File outputFile = new File(classLoader.getResource("commands.json").getPath());
        log.info("outputFile : {}", outputFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(outputFile, mapper.writeValueAsString(writeStrList).getBytes());
    }
}