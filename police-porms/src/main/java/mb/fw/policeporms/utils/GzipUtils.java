package mb.fw.policeporms.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GzipUtils {

	public static boolean isGzipFileValid(MultipartFile file) {
	    log.debug("파일 무결성 검증 시작...");
	    try (InputStream is = file.getInputStream();
	         GZIPInputStream gzis = new GZIPInputStream(is)) {
	        
	        byte[] buffer = new byte[8192];
	        // 실제로 데이터를 쓰지는 않고 끝까지 읽어서 압축 해제에 문제가 없는지만 확인
	        while (gzis.read(buffer) != -1) { }
	        
	        log.debug("파일 무결성 검증 완료: 정상");
	        return true;
	    } catch (ZipException e) {
	        log.error("파일이 손상되었거나 Gzip 형식이 아닙니다: {}", e.getMessage());
	    } catch (IOException e) {
	        log.error("파일 읽기 중 오류 발생 (네트워크 전송 중단 가능성): {}", e.getMessage());
	    }
	    return false;
	}
}
