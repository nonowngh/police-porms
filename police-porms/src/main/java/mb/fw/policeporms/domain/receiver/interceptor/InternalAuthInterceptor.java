package mb.fw.policeporms.domain.receiver.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import lombok.extern.slf4j.Slf4j;
import mb.fw.policeporms.common.constant.InterfaceAuthConstants;

@Component
@Slf4j
public class InternalAuthInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		String clientKey = request.getHeader(InterfaceAuthConstants.AUTH_HEADER);

		if (InterfaceAuthConstants.AUTH_KEY.equals(clientKey)) {
			return true; // 인증 성공: 컨트롤러로 진행
		}

		// 인증 실패: 401 에러 반환 및 로그 기록
		log.warn("미인증 접근 시도됨 - IP: {}, Path: {}", request.getRemoteAddr(), request.getRequestURI());
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized Access");
		return false;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) {
		MDC.clear();
	}
}
