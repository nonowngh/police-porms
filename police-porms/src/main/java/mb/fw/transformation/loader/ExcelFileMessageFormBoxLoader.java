package mb.fw.transformation.loader;

import mb.fw.transformation.form.*;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author clupine
 *
 */
public class ExcelFileMessageFormBoxLoader implements MessageFormBoxLoader {

	private static Logger logger = LoggerFactory.getLogger(ExcelFileMessageFormBoxLoader.class);

	
	//0부터 계산 
	
	final int INFOMATION_START_INDEX_ROW = 0;
	final int INFOMATION_COUNT = 9;

	final int FIELD_START_INDEX_ROW = 11;

	// Infomation
	final int AGENT_NAME_IDX = 0;
	final int TRAN_CODE_IDX = 1;
	final int TRAN_COMMENT_IDX = 3;
	final int TRAN_DIRECTION_IDX = 7;
//	final int TRAN_MODE_IDX = 4;
	final int CHANGE_TRAN_CODE_IDX = 2;
//	final int REDIRECTION_AGENT_NAME_IDX = 6;
	final int INFO_IN_TYPE_IDX = 5;
	final int INFO_OUT_TYPE_IDX =6;
	final int SERVICE_NAME_IDX = 8;
	final int RESERVE_IDX = 4;
	

	final int VALUE_CELL_IDX = 1;

	// IN
//	final int IN_DIVISION_IDX = 0;
	final int IN_NO_IDX = 0;
	final int IN_NAME_KOR_IDX = 1; 
	final int IN_NAME_IDX = 2;
	final int IN_TYPE_IDX = 3;
	final int IN_LENGTH_IDX = 4;
	final int IN_CHILD_COUNT_IDX = 5;
	final int IN_COUNT_NO_IDX = 6;
	final int IN_FUNCTION_IDX = 7;
	final int IN_DEFAULT_VALUE_IDX = 8;
	
	
//	final int IN_REQUIRED_IDX = 9;
//	final int IN_RANK_IDX = 3;
//	final int IN_TPDATA_IDX = 24;

	// OUT
//	final int OUT_DIVISION_IDX = 12;
	final int OUT_NO_IDX = 9;
	final int OUT_NAME_KOR_IDX = 10; 
	final int OUT_RANK_IDX = 11;
	final int OUT_NAME_IDX = 12;
	final int OUT_TYPE_IDX = 13;
	final int OUT_LENGTH_IDX = 14;
	final int OUT_CHILD_COUNT_IDX = 15;
	final int OUT_COUNT_NO_IDX = 16;
	final int OUT_FUNCTION_IDX = 17;
	final int OUT_DEFAULT_VALUE_IDX = 18;
	
//	final int OUT_REQUIRED_IDX = 21;
//	final int OUT_TPDATA_IDX = 35;

	final String INFO_TYPE = "VM-XML";

	Set<File> fileSet = new LinkedHashSet<File>();

	public void setFile(Set<File> filePathSet) {
		this.fileSet = filePathSet;
	}

	public void setFile(File file) {
		fileSet.add(file);
	}

	public MessageFormBox formload() {

		MessageFormBox box = new MessageFormBox();
		for (File file : fileSet) {

			POIFSFileSystem fs;
			int numberSheets = 0;
			HSSFWorkbook workbook = null;

			logger.debug("MessageFormLoad File : " + file.getName());

			try {

				fs = new POIFSFileSystem(new FileInputStream(file));
				workbook = new HSSFWorkbook(fs);
				numberSheets = workbook.getNumberOfSheets();
				logger.debug("ExcelFile Sheets Count : " + numberSheets);

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			for (int k = 0; k < numberSheets; k++) {
				logger.debug("Sheet Info : " + k + " , " + workbook.getSheetName(k));

				HSSFSheet sheet = workbook.getSheetAt(k);
				Infomation infomation = createInfomation(sheet);

//				 logger.debug("created infomation : " + infomation);

				MessageForm form = createRecordForm(sheet);

				form.setInfomation(infomation);

//				 logger.debug("message form : " + form.getTpContext());

				box.put(form);
			}
		}
		return box;
	}

	private Infomation createInfomation(HSSFSheet sheet) {

		Infomation infomation = new Infomation();

		try {

			HSSFRow AgentNameRow = sheet.getRow(INFOMATION_START_INDEX_ROW + AGENT_NAME_IDX);
			HSSFRow tranCodeRow = sheet.getRow(INFOMATION_START_INDEX_ROW + TRAN_CODE_IDX);
			HSSFRow tranCommentRow = sheet.getRow(INFOMATION_START_INDEX_ROW + TRAN_COMMENT_IDX);
			HSSFRow tranDirectionRow = sheet.getRow(INFOMATION_START_INDEX_ROW + TRAN_DIRECTION_IDX);
//			HSSFRow tranModeRow = sheet.getRow(INFOMATION_START_INDEX_ROW + TRAN_MODE_IDX);
			HSSFRow changeTranCodeRow = sheet.getRow(INFOMATION_START_INDEX_ROW + CHANGE_TRAN_CODE_IDX);
//			HSSFRow redirectionAgentNameRow = sheet.getRow(INFOMATION_START_INDEX_ROW + REDIRECTION_AGENT_NAME_IDX);
			HSSFRow inTypeRow = sheet.getRow(INFOMATION_START_INDEX_ROW + INFO_IN_TYPE_IDX);
			HSSFRow outTypeRow = sheet.getRow(INFOMATION_START_INDEX_ROW + INFO_OUT_TYPE_IDX);
			HSSFRow serviceNameRow = sheet.getRow(INFOMATION_START_INDEX_ROW + SERVICE_NAME_IDX);
			HSSFRow reserveRow = sheet.getRow(INFOMATION_START_INDEX_ROW + RESERVE_IDX);

			infomation.setAgentName(AgentNameRow.getCell(VALUE_CELL_IDX).getStringCellValue());
			infomation.setTranCode(tranCodeRow.getCell(VALUE_CELL_IDX).getStringCellValue());
			infomation.setTranComment(tranCommentRow.getCell(VALUE_CELL_IDX).getStringCellValue());
			infomation.setTranDirection(tranDirectionRow.getCell(VALUE_CELL_IDX).getStringCellValue());
//			infomation.setTranMode(tranModeRow.getCell(VALUE_CELL_IDX).getStringCellValue());
			infomation.setChangeTranCode(changeTranCodeRow.getCell(VALUE_CELL_IDX).getStringCellValue());
//			infomation.setRedirectionAgentName(redirectionAgentNameRow.getCell(VALUE_CELL_IDX).getStringCellValue());
			infomation.setInType(inTypeRow.getCell(VALUE_CELL_IDX).getStringCellValue());
			infomation.setOutType(outTypeRow.getCell(VALUE_CELL_IDX).getStringCellValue());
			infomation.setServiceName(serviceNameRow.getCell(VALUE_CELL_IDX).getStringCellValue());
			infomation.setReserve(reserveRow.getCell(VALUE_CELL_IDX).getStringCellValue());

		} catch (Exception e) {
			logger.error("createInfomation Fail , ", e);
			return null;
		}

		return infomation;
	}

	private MessageForm createRecordForm(HSSFSheet sheet) {

		MessageForm form = new MessageForm();
		RecordContext inRecordContext = new RecordContext();
		RecordContext outRecordContext = new RecordContext();
		int lastRownum = sheet.getLastRowNum();

		for (int i = FIELD_START_INDEX_ROW; i < lastRownum; i++) {
			
			HSSFRow row = sheet.getRow(i);

			if (row.getCell(IN_NO_IDX).getCellType() != HSSFCell.CELL_TYPE_BLANK) {
				Record inRecord = new Record();
				Infomation infomation = new Infomation();

				HSSFRow inTypeRow = sheet.getRow(INFOMATION_START_INDEX_ROW + INFO_IN_TYPE_IDX);
				infomation.setInType(inTypeRow.getCell(VALUE_CELL_IDX).getStringCellValue());

//				if (infomation.getInType().equals(INFO_TYPE)
//						&& !(row.getCell(IN_TPDATA_IDX).getStringCellValue().isEmpty())) {
//					inRecordContext.setTmpData(row.getCell(IN_TPDATA_IDX).getStringCellValue());
//				}
//				inRecord.setDivision(row.getCell(IN_DIVISION_IDX).getStringCellValue());
				inRecord.setNameKor(row.getCell(IN_NAME_KOR_IDX).getStringCellValue());
				inRecord.setNo((int) row.getCell(IN_NO_IDX).getNumericCellValue());

//				if (row.getCell(IN_RANK_IDX).getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
//					inRecord.setRank(((int) row.getCell(IN_RANK_IDX).getNumericCellValue()) + "");
//				} else if (row.getCell(IN_RANK_IDX).getCellType() == HSSFCell.CELL_TYPE_STRING) {
//					inRecord.setRank(row.getCell(IN_RANK_IDX).getStringCellValue());
//				}

				inRecord.setName(row.getCell(IN_NAME_IDX).getStringCellValue());
				inRecord.setType(row.getCell(IN_TYPE_IDX).getStringCellValue());
				inRecord.setLength((int) row.getCell(IN_LENGTH_IDX).getNumericCellValue());
				inRecord.setChildCount((int) row.getCell(IN_CHILD_COUNT_IDX).getNumericCellValue());
//				inRecord.setRequired(row.getCell(IN_REQUIRED_IDX).getStringCellValue().equals("Y") ? true : false);
				inRecord.setFunction(row.getCell(IN_FUNCTION_IDX).getStringCellValue());
				inRecord.setDefaultValue(row.getCell(IN_DEFAULT_VALUE_IDX).getStringCellValue());
				inRecord.setCountNo((int) row.getCell(IN_COUNT_NO_IDX).getNumericCellValue());

				inRecordContext.put(inRecord.getNo(), inRecord);

			}

			
			if (row.getCell(OUT_NO_IDX) != null && (row.getCell(OUT_NO_IDX).getCellType() != HSSFCell.CELL_TYPE_BLANK)) {
				Record outRecord = new Record();
				Infomation infomation = new Infomation();
				HSSFRow outTypeRow = sheet.getRow(INFOMATION_START_INDEX_ROW + INFO_OUT_TYPE_IDX);
				infomation.setOutType(outTypeRow.getCell(VALUE_CELL_IDX).getStringCellValue());

//				if (infomation.getOutType().equals(INFO_TYPE)
//						&& !(row.getCell(OUT_TPDATA_IDX).getStringCellValue().isEmpty())) {
//					outRecord.setData(row.getCell(OUT_TPDATA_IDX).getStringCellValue().getBytes());
//					outRecordContext.setTmpData(row.getCell(OUT_TPDATA_IDX).getStringCellValue());
//				}
//				outRecord.setDivision(row.getCell(OUT_DIVISION_IDX).getStringCellValue());
				outRecord.setNameKor(row.getCell(OUT_NAME_KOR_IDX).getStringCellValue());
				outRecord.setNo((int) row.getCell(OUT_NO_IDX).getNumericCellValue());

				if (row.getCell(OUT_RANK_IDX).getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
					outRecord.setRank(((int) row.getCell(OUT_RANK_IDX).getNumericCellValue()) + "");
				} else if (row.getCell(OUT_RANK_IDX).getCellType() == HSSFCell.CELL_TYPE_STRING) {
					outRecord.setRank(row.getCell(OUT_RANK_IDX).getStringCellValue());
				}
				outRecord.setName(row.getCell(OUT_NAME_IDX).getStringCellValue());
				outRecord.setType(row.getCell(OUT_TYPE_IDX).getStringCellValue());
				outRecord.setLength((int) row.getCell(OUT_LENGTH_IDX).getNumericCellValue());
				outRecord.setChildCount((int) row.getCell(OUT_CHILD_COUNT_IDX).getNumericCellValue());
//				outRecord.setRequired(row.getCell(OUT_REQUIRED_IDX).getStringCellValue().equals("Y") ? true : false);
				outRecord.setFunction(row.getCell(OUT_FUNCTION_IDX).getStringCellValue() == "" ? null
						: row.getCell(OUT_FUNCTION_IDX).getStringCellValue());
				outRecord.setDefaultValue(row.getCell(OUT_DEFAULT_VALUE_IDX).getStringCellValue());
				outRecord.setCountNo((int) row.getCell(OUT_COUNT_NO_IDX).getNumericCellValue());

				outRecordContext.put(outRecord.getNo(), outRecord);
			}

			// logger.debug("IN Record : " + inRecord);
			// logger.debug("OUT Record : " + outRecord);

		}
		// logger.debug("IN Data : " + inRecordMap);
		// logger.debug("OUT Data : " + outRecordMap);

		form.setInContext(inRecordContext);
		form.setOutContext(outRecordContext);

		return form;
	}

	public static void main(String[] args) throws Exception {
		Set<File> fileSet = new HashSet<File>();
		// fileSet.add(new File("doc/Sample.xls"));
		fileSet.add(new File("C:\\work\\frameworkTeam\\mci-commons2\\testsample\\Upload_sample.xls"));
		// fileSet.add(new File("doc/MappingFormSample2.xls"));
		ExcelFileMessageFormBoxLoader loader = new ExcelFileMessageFormBoxLoader();
		loader.setFile(fileSet);
		MessageFormBox box = loader.formload();
		logger.debug("mapping data : \n" + box.toString());
		RecordContext rc =  box.get("Q8001100").getInContext();
		logger.info(rc.toString());
		for (int i = 1; i <= rc.size(); i++) {
			logger.info(rc.get(i).toStringSimple());
		}
		
	}

	@Override
	public void setGroupId(String groupId) {
	}

}
