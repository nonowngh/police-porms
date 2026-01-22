package mb.fw.atb.build;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.strategy.ATBPattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

@Slf4j
public class ATBSpecBuild {
    ObjectMapper mapper = new ObjectMapper();

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        ATBSpecBuild atbSpecTest = new ATBSpecBuild();
        log.info("ATBSpecBuild start");
        atbSpecTest.writeResourceFile();
        log.info("ATBSpecBuild end");
    }

    public void writeResourceFile() throws IOException {
        String packageName = "src/main/java/mb/fw/atb/strategy";
        ATBPattern atbPattern = new ATBPattern();
        atbPattern.load(packageName);

        //atbPattern to json String
        try {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            String json = mapper.writeValueAsString(atbPattern);
            log.info("json : {}", json);
            FileUtils.writeByteArrayToFile(new File("src/main/resources/atbPattern.json"), json.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ATBPattern loadFilePattern(String jarName) throws IOException {
        JarFile jarFile = new JarFile(jarName);
        Stream<JarEntry> stream = jarFile.stream();
        //stream JarEntry에서  atbPattern.json 라는  파일을 찾아라
        JarEntry entry = stream.filter(jarEntry -> jarEntry.getName().equals("atbPattern.json")).findFirst().get();
        log.info(entry.getName());
        InputStream inputStream = jarFile.getInputStream(entry);

        String json = IOUtils.toString(inputStream);
        log.info("json : {}", json);

        return mapper.readValue(json, ATBPattern.class);
    }
}