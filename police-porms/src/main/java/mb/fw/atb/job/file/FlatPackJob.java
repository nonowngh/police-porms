package mb.fw.atb.job.file;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.enums.FileType;
import net.sf.flatpack.Parser;
import net.sf.flatpack.brparse.BuffReaderParseFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * FlatPack을 이용한 File Parsing Job
 */
@Slf4j
@Component
public class FlatPackJob {

    static Map<String, FileReader> pzmapReaderMap = new HashMap();
    static Map<String, FileReader> fileReaderMap = new HashMap();
    static String separator = FileSystems.getDefault().getSeparator();

    /**
     * FileReader가 잡고 있나?
     * @param key
     */
    public static void close(String key) {
        FileReader pzmapReader = pzmapReaderMap.get(key);
        FileReader fileReader = fileReaderMap.get(key);
        try {
            if (pzmapReader != null) {
                pzmapReader.close();
            }
            if (fileReader != null) {
                fileReader.close();
            }
        } catch (IOException e) {
            log.error("FileReader close error", e);
        } finally {
            pzmapReaderMap.remove(key);
            fileReaderMap.remove(key);
        }
    }

    public static Parser getParser(String key, IFContext context, Path insertFile) throws IOException {
        Parser parser = null;
        switch (FileType.valueOf(context.getFileType())) {
            case DELIMITER: {
                FileReader insertReader = new FileReader(insertFile.toFile());
                fileReaderMap.put(key, insertReader);
                if (context.isFileFirstHeader()) {
                    parser = BuffReaderParseFactory.getInstance().newDelimitedParser(insertReader, context.getFileDelimiter(), context.getFileDelimiterQualifier());
                    parserOptionsSet(parser);
                } else {
                    File pzmap = new ClassPathResource(context.getInterfaceId() + ".pzmap").getFile();
                    FileReader pzmapReader = new FileReader(pzmap);
                    parser = BuffReaderParseFactory.getInstance().newDelimitedParser(pzmapReader, insertReader, context.getFileDelimiter(), context.getFileDelimiterQualifier(), false);
                    parserOptionsSet(parser);
                }
                break;
            }
            case FIXEDLENGTH: {

                File pzmap = new ClassPathResource(context.getInterfaceId() + ".pzmap").getFile();
                FileReader pzmapReader = new FileReader(pzmap);
                FileReader insertReader = new FileReader(insertFile.toFile());

                pzmapReaderMap.put(key, pzmapReader);
                fileReaderMap.put(key, insertReader);

                parser = BuffReaderParseFactory.getInstance().newFixedLengthParser(pzmapReader, insertReader);
                parserOptionsSet(parser);
                break;
            }
        }
        return parser;
    }

    public static void parserOptionsSet(Parser parser) {
        parser.setNullEmptyStrings(true);
        parser.setPreserveLeadingWhitespace(true);
        parser.setPreserveTrailingWhitespace(true);
    }

}
