package mb.fw.policeporms.dto.openapi.서울시신호등관련정보;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class TrafficSafety {

	@JsonProperty("ATCH_MNG_NO1")
	private String atchMngNo1;

	@JsonProperty("STTS_CD")
	private String sttsCd;

	@JsonProperty("ATCH_MTHD")
	private String atchMthd;

	@JsonProperty("ATCH_LEN")
	private String atchLen;

	@JsonProperty("HLVLRD")
	private String hlvlrd;

	@JsonProperty("ATCH_DRCT")
	private String atchDrct;

	@JsonProperty("TRFC_LGHT_CNT")
	private String trfcLghtCnt;

	@JsonProperty("TLGHT_CNT")
	private String tlghtCnt;

	@JsonProperty("TRFC_LGHT_KND")
	private String trfcLghtKnd;

	@JsonProperty("TLGHT_KND")
	private String tlghtKnd;

	@JsonProperty("INSTL_YMD")
	private String instlYmd;

	@JsonProperty("RPLC_YMD")
	private String rplcYmd;

	@JsonProperty("PLR_MNG_NO")
	private String plrMngNo;

	@JsonProperty("SGN_LMN_SE")
	private String sgnLmnSe;

	@JsonProperty("MKR")
	private String mkr;

	@JsonProperty("JOB_SE_CD")
	private String jobSeCd;

	@JsonProperty("EXPRS_SE")
	private String exprsSe;

	@JsonProperty("NEW_NMLZ_ID")
	private String newNmlzId;

	@JsonProperty("CSTRN_MNG_NO")
	private String cstrnMngNo;

	@JsonProperty("ATCH_MNG_NO2")
	private String atchMngNo2;

	@JsonProperty("HSTRY_ID")
	private String hstryId;

	@JsonProperty("PSTN_INFO")
	private String pstnInfo;

	@JsonProperty("XCRD")
	private String xcrd;

	@JsonProperty("YCRD")
	private String ycrd;

	@JsonProperty("CSTRN_FORM")
	private String cstrnForm;

}
