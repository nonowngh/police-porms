package mb.fw.policeporms.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GzipUtils {

//	public static boolean isGzipFileValid(MultipartFile file) {
//	    log.debug("파일 무결성 검증 시작...");
//	    try (InputStream is = file.getInputStream();
//	         GZIPInputStream gzis = new GZIPInputStream(is)) {
//	        
//	        byte[] buffer = new byte[8192];
//	        // 실제로 데이터를 쓰지는 않고 끝까지 읽어서 압축 해제에 문제가 없는지만 확인
//	        while (gzis.read(buffer) != -1) { }
//	        
//	        log.debug("파일 무결성 검증 완료: 정상");
//	        return true;
//	    } catch (ZipException e) {
//	        log.error("파일이 손상되었거나 Gzip 형식이 아닙니다: {}", e.getMessage());
//	    } catch (IOException e) {
//	        log.error("파일 읽기 중 오류 발생 (네트워크 전송 중단 가능성): {}", e.getMessage());
//	    }
//	    return false;
//	}
	
	public static boolean isGzipFileValid(MultipartFile file) {
	    if (file == null || file.isEmpty()) {
	        log.warn("검증할 파일이 비어있습니다.");
	        return false;
	    }

	    String fileName = file.getOriginalFilename();
	    boolean isEncrypt = fileName != null && fileName.toLowerCase().endsWith(".enc");
	    
	    log.debug("파일 무결성 검증 시작... (파일명: {}, 암호화여부: {})", fileName, isEncrypt);

	    try (InputStream is = file.getInputStream()) {
	        if (isEncrypt) {
	            // [case 1] 암호화 파일: 최소 12바이트(IV) 이상의 크기인지 확인
	            // 복호화 전에는 내부를 볼 수 없으므로 크기로 1차 검증
	            if (file.getSize() < 12) {
	                log.error("암호화 파일 크기가 유효하지 않음 (12바이트 미만)");
	                return false;
	            }
	        } else {
	            // [case 2] 일반 GZIP 파일: 헤더의 매직 넘버만 확인 (빠른 검증)
	            byte[] header = new byte[2];
	            int readCount = is.read(header);
	            
	            if (readCount < 2) return false;

	            // GZIP 매직 넘버 확인 (0x1f, 0x8b)
	            int magic = ((header[1] & 0xFF) << 8) | (header[0] & 0xFF);
	            if (magic != GZIPInputStream.GZIP_MAGIC) {
	                log.error("파일이 Gzip 형식이 아닙니다. (Magic Number 불일치)");
	                return false;
	            }
	        }
	        
	        log.debug("파일 무결성 검증 통과");
	        return true;

	    } catch (IOException e) {
	        log.error("파일 읽기 중 오류 발생: {}", e.getMessage());
	        return false;
	    }
	}
}
