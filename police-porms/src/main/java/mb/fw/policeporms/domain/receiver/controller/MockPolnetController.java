package mb.fw.policeporms.domain.receiver.controller;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.util.crypto.Seed128Cipher;

// 로컬 polnet 개발 테스트 용
@Slf4j
@RestController
@RequestMapping("/mock/polnet")
public class MockPolnetController {

    private static final String ENCRYPTION_KEY = "DEV0009920250909"; 

    @PostMapping(value = "/onlineService", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public byte[] mockOnlineService(@RequestBody byte[] encryptedRequest) {
        try {
            byte[] keyBytes = ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8);

            byte[] decryptedRequest = Seed128Cipher.decrypt(encryptedRequest, keyBytes);
            String requestXml = new String(decryptedRequest, StandardCharsets.UTF_8);
            log.info("📩 [Mock] 수신된 폴넷 요청 전문:\n{}", requestXml);

            // QRY_CL(조회구분) 추출
            String qryCl = "1"; 
            Pattern pattern = Pattern.compile("<QRY_CL>(.*?)</QRY_CL>");
            Matcher matcher = pattern.matcher(requestXml);
            if (matcher.find()) {
                qryCl = matcher.group(1).trim();
            }

            // QRY_CL 값에 따른 완벽한 분기 처리
            String mockXmlResponse;
            if ("1".equals(qryCl) || "2".equals(qryCl) || "8".equals(qryCl)) {
                log.info("🔍 [Mock] 사용자기본(조회구분: {}) 데이터 5건 생성", qryCl);
                mockXmlResponse = generateUserResponseXml(qryCl);
            } else if ("3".equals(qryCl)) {
                log.info("🔍 [Mock] 조직(조회구분: 3) 데이터 5건 생성");
                mockXmlResponse = generateOrgResponseXml();
            } else if ("4".equals(qryCl) || "5".equals(qryCl) || "6".equals(qryCl)) {
                log.info("🔍 [Mock] 공통코드(조회구분: {}) 데이터 5건 생성", qryCl);
                mockXmlResponse = generateCodeResponseXml(qryCl);
            } else {
                log.warn("⚠️ [Mock] 지원하지 않는 조회구분({}). 빈 응답 반환", qryCl);
                mockXmlResponse = generateEmptyResponseXml(qryCl);
            }

            // 응답 전문 암호화 반환
            byte[] plainResponseBytes = mockXmlResponse.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedResponse = Seed128Cipher.encrypt(plainResponseBytes, keyBytes);
            
            return encryptedResponse;

        } catch (Exception e) {
            log.error("Mock 처리 중 오류 발생", e);
            throw new RuntimeException("Mock 서버 에러", e);
        }
    }

    // [조회구분: 1, 2, 8] 사용자기본, 사용자기본(민원), 사용자기본(안보)
    private String generateUserResponseXml(String qryCl) {
        // 8(안보)일 경우 USER_CPOS_CD가 빈 값으로 오고 USER_NM이 변환
        String cposCd = "8".equals(qryCl) ? "" : "00000";
        String userNm1 = "8".equals(qryCl) ? "A+경기남부교환+1234" : "홍길동";
        
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<RESPONSE>\n" +
               "  <HEADER>\n" +
               "    <TLGR_CD>PN02</TLGR_CD>\n" +
               "    <REQ_CL>0</REQ_CL>\n" +
               "  </HEADER>\n" +
               "  <DATA>\n" +
               "    <QRY_CL>" + qryCl + "</QRY_CL>\n" +
               "    <RESP_CD>00</RESP_CD>\n" + 
               "    <REQ_CNT>5</REQ_CNT>\n" +
               "    <PAGE>1</PAGE>\n" +
               "    <CHG_START_DTTM>20230101100000</CHG_START_DTTM>\n" +
               "    <CHG_END_DTTM>20231231105959</CHG_END_DTTM>\n" +
               "    <LOOP>\n" +
               "      <RECORD>\n" +
               "        <USER_NO>100000001</USER_NO>\n" +
               "        <USER_ID>test01</USER_ID>\n" +
               "        <USER_PWD>pwd01</USER_PWD>\n" +
               "        <USER_NM>" + userNm1 + "</USER_NM>\n" +
               "        <USER_BTHD>19800101</USER_BTHD>\n" +
               "        <GND_CL_CD>M</GND_CL_CD>\n" +
               "        <USER_OPOS_CD>OP001</USER_OPOS_CD>\n" +
               "        <USER_CPOS_CD>" + cposCd + "</USER_CPOS_CD>\n" +
               "        <USER_STAT_CD>01</USER_STAT_CD>\n" +
               "        <USER_GVOF_CD>P0000001</USER_GVOF_CD>\n" +
               "      </RECORD>\n" +
               "      <RECORD>\n" +
               "        <USER_NO>100000002</USER_NO>\n" +
               "        <USER_ID>test02</USER_ID>\n" +
               "        <USER_PWD>pwd02</USER_PWD>\n" +
               "        <USER_NM>김철수</USER_NM>\n" +
               "        <USER_BTHD>19820505</USER_BTHD>\n" +
               "        <GND_CL_CD>M</GND_CL_CD>\n" +
               "        <USER_OPOS_CD>OP002</USER_OPOS_CD>\n" +
               "        <USER_CPOS_CD>" + cposCd + "</USER_CPOS_CD>\n" +
               "        <USER_STAT_CD>01</USER_STAT_CD>\n" +
               "        <USER_GVOF_CD>P0000002</USER_GVOF_CD>\n" +
               "      </RECORD>\n" +
               "      <RECORD>\n" +
               "        <USER_NO>100000003</USER_NO>\n" +
               "        <USER_ID>test03</USER_ID>\n" +
               "        <USER_PWD>pwd03</USER_PWD>\n" +
               "        <USER_NM>이영희</USER_NM>\n" +
               "        <USER_BTHD>19901010</USER_BTHD>\n" +
               "        <GND_CL_CD>F</GND_CL_CD>\n" +
               "        <USER_OPOS_CD>OP003</USER_OPOS_CD>\n" +
               "        <USER_CPOS_CD>" + cposCd + "</USER_CPOS_CD>\n" +
               "        <USER_STAT_CD>01</USER_STAT_CD>\n" +
               "        <USER_GVOF_CD>P0000003</USER_GVOF_CD>\n" +
               "      </RECORD>\n" +
               "      <RECORD>\n" +
               "        <USER_NO>100000004</USER_NO>\n" +
               "        <USER_ID>test04</USER_ID>\n" +
               "        <USER_PWD>pwd04</USER_PWD>\n" +
               "        <USER_NM>박민수</USER_NM>\n" +
               "        <USER_BTHD>19950303</USER_BTHD>\n" +
               "        <GND_CL_CD>M</GND_CL_CD>\n" +
               "        <USER_OPOS_CD>OP004</USER_OPOS_CD>\n" +
               "        <USER_CPOS_CD>" + cposCd + "</USER_CPOS_CD>\n" +
               "        <USER_STAT_CD>01</USER_STAT_CD>\n" +
               "        <USER_GVOF_CD>P0000004</USER_GVOF_CD>\n" +
               "      </RECORD>\n" +
               "      <RECORD>\n" +
               "        <USER_NO>100000005</USER_NO>\n" +
               "        <USER_ID>test05</USER_ID>\n" +
               "        <USER_PWD>pwd05</USER_PWD>\n" +
               "        <USER_NM>정수진</USER_NM>\n" +
               "        <USER_BTHD>19981212</USER_BTHD>\n" +
               "        <GND_CL_CD>F</GND_CL_CD>\n" +
               "        <USER_OPOS_CD>OP005</USER_OPOS_CD>\n" +
               "        <USER_CPOS_CD>" + cposCd + "</USER_CPOS_CD>\n" +
               "        <USER_STAT_CD>01</USER_STAT_CD>\n" +
               "        <USER_GVOF_CD>P0000005</USER_GVOF_CD>\n" +
               "      </RECORD>\n" +
               "    </LOOP>\n" +
               "  </DATA>\n" +
               "</RESPONSE>";
    }

    // [조회구분: 3] 조직
    private String generateOrgResponseXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<RESPONSE>\n" +
               "  <HEADER>\n" +
               "    <TLGR_CD>PN02</TLGR_CD>\n" +
               "    <REQ_CL>0</REQ_CL>\n" +
               "  </HEADER>\n" +
               "  <DATA>\n" +
               "    <QRY_CL>3</QRY_CL>\n" +
               "    <RESP_CD>00</RESP_CD>\n" + 
               "    <REQ_CNT>5</REQ_CNT>\n" +
               "    <PAGE>1</PAGE>\n" +
               "    <CHG_START_DTTM>20230101100000</CHG_START_DTTM>\n" +
               "    <CHG_END_DTTM>20231231105959</CHG_END_DTTM>\n" +
               "    <LOOP>\n" +
               "      <RECORD>\n" +
               "        <ESRM_GVOF_CD>G30000000000001</ESRM_GVOF_CD>\n" +
               "        <GVOF_NM>경찰청</GVOF_NM>\n" +
               "        <ORG_TREE_STRC_SORT_ORD>A001</ORG_TREE_STRC_SORT_ORD>\n" +
               "        <USE_YN>Y</USE_YN>\n" +
               "        <GVOF_ALL_NM>경찰청</GVOF_ALL_NM>\n" +
               "        <GVOF_CD>P0000001</GVOF_CD>\n" +
               "        <HGHR_GVOF_CD>P0000001</HGHR_GVOF_CD>\n" +
               "        <ORCT_LVL_VAL>1</ORCT_LVL_VAL>\n" +
               "      </RECORD>\n" +
               "      <RECORD>\n" +
               "        <ESRM_GVOF_CD>G30000000000002</ESRM_GVOF_CD>\n" +
               "        <GVOF_NM>서울청</GVOF_NM>\n" +
               "        <ORG_TREE_STRC_SORT_ORD>B001</ORG_TREE_STRC_SORT_ORD>\n" +
               "        <USE_YN>Y</USE_YN>\n" +
               "        <GVOF_ALL_NM>경찰청 서울특별시경찰청</GVOF_ALL_NM>\n" +
               "        <GVOF_CD>P0000002</GVOF_CD>\n" +
               "        <HGHR_GVOF_CD>P0000001</HGHR_GVOF_CD>\n" +
               "        <ORCT_LVL_VAL>2</ORCT_LVL_VAL>\n" +
               "      </RECORD>\n" +
               "      <RECORD>\n" +
               "        <ESRM_GVOF_CD>G30000000000003</ESRM_GVOF_CD>\n" +
               "        <GVOF_NM>경기남부청</GVOF_NM>\n" +
               "        <ORG_TREE_STRC_SORT_ORD>B002</ORG_TREE_STRC_SORT_ORD>\n" +
               "        <USE_YN>Y</USE_YN>\n" +
               "        <GVOF_ALL_NM>경찰청 경기남부경찰청</GVOF_ALL_NM>\n" +
               "        <GVOF_CD>P0000003</GVOF_CD>\n" +
               "        <HGHR_GVOF_CD>P0000001</HGHR_GVOF_CD>\n" +
               "        <ORCT_LVL_VAL>2</ORCT_LVL_VAL>\n" +
               "      </RECORD>\n" +
               "      <RECORD>\n" +
               "        <ESRM_GVOF_CD>G30000000000004</ESRM_GVOF_CD>\n" +
               "        <GVOF_NM>수원남부서</GVOF_NM>\n" +
               "        <ORG_TREE_STRC_SORT_ORD>C001</ORG_TREE_STRC_SORT_ORD>\n" +
               "        <USE_YN>Y</USE_YN>\n" +
               "        <GVOF_ALL_NM>경찰청 경기남부경찰청 수원남부경찰서</GVOF_ALL_NM>\n" +
               "        <GVOF_CD>P0000004</GVOF_CD>\n" +
               "        <HGHR_GVOF_CD>P0000003</HGHR_GVOF_CD>\n" +
               "        <ORCT_LVL_VAL>3</ORCT_LVL_VAL>\n" +
               "      </RECORD>\n" +
               "      <RECORD>\n" +
               "        <ESRM_GVOF_CD>G30000000000005</ESRM_GVOF_CD>\n" +
               "        <GVOF_NM>화성동탄서</GVOF_NM>\n" +
               "        <ORG_TREE_STRC_SORT_ORD>C002</ORG_TREE_STRC_SORT_ORD>\n" +
               "        <USE_YN>Y</USE_YN>\n" +
               "        <GVOF_ALL_NM>경찰청 경기남부경찰청 화성동탄경찰서</GVOF_ALL_NM>\n" +
               "        <GVOF_CD>P0000005</GVOF_CD>\n" +
               "        <HGHR_GVOF_CD>P0000003</HGHR_GVOF_CD>\n" +
               "        <ORCT_LVL_VAL>3</ORCT_LVL_VAL>\n" +
               "      </RECORD>\n" +
               "    </LOOP>\n" +
               "  </DATA>\n" +
               "</RESPONSE>";
    }

    // [조회구분: 4, 5, 6] 직위, 직급, 공통코드
    private String generateCodeResponseXml(String qryCl) {
        // 직위: CM000100, 직급: CM000101, 공통(대외직명): US000010
        String grpCd;
        String[] codeNames;
        if ("4".equals(qryCl)) {
            grpCd = "CM000100"; 
            codeNames = new String[]{"청장", "차장", "국장", "과장", "계장"};
        } else if ("5".equals(qryCl)) {
            grpCd = "CM000101";
            codeNames = new String[]{"치안총감", "치안정감", "총경", "경정", "경감"};
        } else {
            grpCd = "US000010";
            codeNames = new String[]{"주무관", "행정관", "검시조사관", "연구사", "연구관"};
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<RESPONSE>\n" +
               "  <HEADER>\n" +
               "    <TLGR_CD>PN02</TLGR_CD>\n" +
               "    <REQ_CL>0</REQ_CL>\n" +
               "  </HEADER>\n" +
               "  <DATA>\n" +
               "    <QRY_CL>" + qryCl + "</QRY_CL>\n" +
               "    <RESP_CD>00</RESP_CD>\n" + 
               "    <REQ_CNT>5</REQ_CNT>\n" +
               "    <PAGE>1</PAGE>\n" +
               "    <CHG_START_DTTM>20230101100000</CHG_START_DTTM>\n" +
               "    <CHG_END_DTTM>20231231105959</CHG_END_DTTM>\n" +
               "    <LOOP>\n" +
               "      <RECORD>\n" +
               "        <CMN_CD>10</CMN_CD>\n" +
               "        <CMN_CD_NM>" + codeNames[0] + "</CMN_CD_NM>\n" +
               "        <SORT_ORD>1</SORT_ORD>\n" +
               "        <CD_USE_YN>Y</CD_USE_YN>\n" +
               "        <GRP_CD>" + grpCd + "</GRP_CD>\n" +
               "        <CHG_DTTM>2023-01-25 20:59:31.0</CHG_DTTM>\n" +
               "      </RECORD>\n" +
               "      <RECORD>\n" +
               "        <CMN_CD>20</CMN_CD>\n" +
               "        <CMN_CD_NM>" + codeNames[1] + "</CMN_CD_NM>\n" +
               "        <SORT_ORD>2</SORT_ORD>\n" +
               "        <CD_USE_YN>Y</CD_USE_YN>\n" +
               "        <GRP_CD>" + grpCd + "</GRP_CD>\n" +
               "        <CHG_DTTM>2023-01-25 20:59:31.0</CHG_DTTM>\n" +
               "      </RECORD>\n" +
               "      <RECORD>\n" +
               "        <CMN_CD>30</CMN_CD>\n" +
               "        <CMN_CD_NM>" + codeNames[2] + "</CMN_CD_NM>\n" +
               "        <SORT_ORD>3</SORT_ORD>\n" +
               "        <CD_USE_YN>Y</CD_USE_YN>\n" +
               "        <GRP_CD>" + grpCd + "</GRP_CD>\n" +
               "        <CHG_DTTM>2023-01-25 20:59:31.0</CHG_DTTM>\n" +
               "      </RECORD>\n" +
               "      <RECORD>\n" +
               "        <CMN_CD>40</CMN_CD>\n" +
               "        <CMN_CD_NM>" + codeNames[3] + "</CMN_CD_NM>\n" +
               "        <SORT_ORD>4</SORT_ORD>\n" +
               "        <CD_USE_YN>Y</CD_USE_YN>\n" +
               "        <GRP_CD>" + grpCd + "</GRP_CD>\n" +
               "        <CHG_DTTM>2023-01-25 20:59:31.0</CHG_DTTM>\n" +
               "      </RECORD>\n" +
               "      <RECORD>\n" +
               "        <CMN_CD>50</CMN_CD>\n" +
               "        <CMN_CD_NM>" + codeNames[4] + "</CMN_CD_NM>\n" +
               "        <SORT_ORD>5</SORT_ORD>\n" +
               "        <CD_USE_YN>Y</CD_USE_YN>\n" +
               "        <GRP_CD>" + grpCd + "</GRP_CD>\n" +
               "        <CHG_DTTM>2023-01-25 20:59:31.0</CHG_DTTM>\n" +
               "      </RECORD>\n" +
               "    </LOOP>\n" +
               "  </DATA>\n" +
               "</RESPONSE>";
    }

    private String generateEmptyResponseXml(String qryCl) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<RESPONSE>\n" +
               "  <HEADER>\n" +
               "    <TLGR_CD>PN02</TLGR_CD>\n" +
               "    <REQ_CL>0</REQ_CL>\n" +
               "  </HEADER>\n" +
               "  <DATA>\n" +
               "    <QRY_CL>" + qryCl + "</QRY_CL>\n" +
               "    <RESP_CD>01</RESP_CD>\n" + 
               "    <REQ_CNT>0</REQ_CNT>\n" +
               "    <PAGE>1</PAGE>\n" +
               "    <CHG_START_DTTM>20230101100000</CHG_START_DTTM>\n" +
               "    <CHG_END_DTTM>20231231105959</CHG_END_DTTM>\n" +
               "    <LOOP></LOOP>\n" +
               "  </DATA>\n" +
               "</RESPONSE>";
    }
}