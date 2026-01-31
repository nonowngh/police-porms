package mb.fw.policeporms.common.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingUtils {

	public static void printInsertProgress(String txId, int total, int current) {
		double progress = (total > 0) ? ((double) current / total) * 100 : 0;
		log.info("[{}] ⏳ 처리 중: {}/{}건 적재 완료 ({})", txId, String.format("%,d", current), String.format("%,d", total),
				String.format("%.1f%%", progress));
	}

	public static void printRequestApiProgress(String txId, String serviceId, int total, int current) {
		double progress = (total > 0) ? ((double) current / total) * 100 : 0;
		log.info("[{}] ⏳ 처리 중: '{}' API 호출 증 현재 {}/총{} 중 ({})", txId, serviceId, String.format("%,d", current),
				String.format("%,d", total), String.format("%.1f%%", progress));
	}

	public static void printWriteFileProgress(String txId, int row, int current, int total) {
		double progress = (total > 0) ? ((double) current / total) * 100 : 0;
		log.info("[{}] ⏳ 처리 중:  {} 건 파일 저장. {}/{}건 완료 ({})", txId, String.format("%,d", row), String.format("%,d", current), String.format("%,d", total),
				String.format("%.1f%%", progress));
	}
}
