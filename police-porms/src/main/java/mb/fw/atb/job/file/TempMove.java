package mb.fw.atb.job.file;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.config.sub.IFContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TempMove {

    static String separator = FileSystems.getDefault().getSeparator();

    /**
     * SEND디렉토리에 있는 파일을 TEMP 디렉토리 위치로 옮긴다
     *
     * @param pollList
     * @param context
     * @param txId
     * @return List<Path> TEMP/TXID/ 파일 PATH
     * @throws IOException
     */
    public List<Path> tempMove(List<Path> pollList, IFContext context, String txId) throws IOException {

        List<Path> tempList = new ArrayList<Path>();

        Path tempPath = Paths.get(context.getFileTempPath() + separator + txId);

        if (Files.notExists(tempPath)) {
            tempPath = Files.createDirectory(tempPath);
        }

        for (Path path : pollList) {
            Path beforeMovePath = Paths.get(context.getFileTempPath() + separator + txId + separator + path.getFileName());

            //존재하는 파일이면 삭제
            if (Files.exists(beforeMovePath)) {
                Files.delete(beforeMovePath);
            }

            Path movedPath = Files.move(path, beforeMovePath);
            log.info("MOVED TEMP FILE ==> {}", movedPath);
            tempList.add(movedPath);
        }

        return tempList;
    }

}
