package mb.fw.transformation.form;

import mb.fw.transformation.tool.TypeConversion;
import mb.fw.transformation.util.HexDumper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;



public class Record implements Cloneable , Serializable{
	/**
	 *
	 */
	private static final long serialVersionUID = -2763778004457651638L;
	private static Logger logger = LoggerFactory.getLogger(Record.class);
	String division;
	String nameKor;
	int no;
	String rank;
	String name;
	String type;
	int length;
	int childCount;
	int countNo;
	boolean required;
	String function;
	String defaultValue;

	String agentName;
	String tranCode;
	String tranDirection;
	String mappingType;
	String nameKorPadding;

	byte[] data;

	Map<Integer,byte[]> dataMap;

	int dataIndex = 0;

	public void putData(int idx, byte[] data){
		if(dataMap == null){
			dataMap = new HashMap<Integer,byte[]>();
		}
		dataMap.put(idx, data);
	}

	public byte[] take(){
		byte[] data = dataMap.get(dataIndex);
		dataIndex++;
		return data;
	}


	public byte[] getData(int idx){
		if(dataMap == null){
			return null;
		}
		return dataMap.get(idx);
	}

	public void setData(byte[] data) {

//		logger.info("set data : " + new String(data));

		this.data = data;
	}

	public byte[] getData() {
		return data;
	}

	public void setRank(String rank) {
		this.rank = rank;
	}

	public String getRank() {
		return rank;
	}

	public void setDivision(String division) {
		this.division = division;
	}
	public String getDivision() {
		return division;
	}
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNameKor() {
		return nameKor;
	}

	public void setNameKor(String nameKor) {
		try {
			nameKorPadding = new String(TypeConversion.toTypeBytesFullChar(nameKor, 30),"KSC5601");
		}catch (Exception e) {
			nameKorPadding = nameKor;
		}
		this.nameKor = nameKor;
	}

	public int getNo() {
		return no;
	}

	public void setNo(int no) {
		this.no = no;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	/**
	 * 하위필드수
	 * @return
	 */
	public int getChildCount() {
		return childCount;
	}

	public void setChildCount(int childCount) {
		this.childCount = childCount;
	}

	/**
	 * 개수 값이 들어잇는 필드
	 * @return
	 */
	public int getCountNo() {
		return countNo;
	}

	public void setCountNo(int countNo) {
		this.countNo = countNo;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public String getTranCode() {
		return tranCode;
	}

	public void setTranCode(String tranCode) {
		this.tranCode = tranCode;
	}

	public String getTranDirection() {
		return tranDirection;
	}

	public void setTranDirection(String tranDirection) {
		this.tranDirection = tranDirection;
	}

	public String getMappingType() {
		return mappingType;
	}

	public void setMappingType(String mappingType) {
		this.mappingType = mappingType;
	}


	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public String toString() {
		return "Record [division=" + division + ", nameKor=" + nameKor + ", no=" + no + ", rank=" + rank + ", name=" + name
				+ ", type=" + type + ", length=" + length + ", childCount=" + childCount + ", countNo=" + countNo + ", required="
				+ required + ", function=" + function + ", defaultValue=" + defaultValue + ", data=" + ((data != null) ? "|"+new String(data)+"|" : "No Data")
				+ "]";
	}

	public String toString4Map() throws UnsupportedEncodingException{
		return String.format("|%1$04d | "+nameKorPadding+" | %2$-8s | %3$06d | V: ["+(data!=null ? new String(data ).replace((char)0x00, '_') : "no data") + "] , H: [" + HexDumper.dump(data)+"]", no,type,length);
	}

	public String toStringSimple() throws UnsupportedEncodingException{
		String retStr = "";
		try {
			retStr = String.format("|%1$04d | "+ nameKorPadding +" | %2$-8s | %3$06d | V: ["+(data!=null ? new String(data).replace((char)0x00, '_') : "no data") + "] , H: [" + HexDumper.dump(data)+"]", no,type,length);
		} catch (Exception e) {
			retStr = String.format("|%1$04d | "+ nameKorPadding +" | %2$-8s | %3$06d | V: [Unmarkable Data] , H: [" + HexDumper.dump(data)+"]", no,type,length);
		}
		return retStr;
	}

	public String toStringSimple(byte[] data) throws UnsupportedEncodingException{
		String retStr = "";
		try {
			retStr = String.format("|%1$04d | "+nameKorPadding+" | %2$-8s | %3$06d | V: ["+(data!=null ? new String(data).replace((char)0x00, '_') : "no data") + "] , H: [" + HexDumper.dump(data)+"]", no,type,length);
		} catch (Exception e) {
			retStr = String.format("|%1$04d | "+nameKorPadding+" | %2$-8s | %3$06d | V: [Unmarkable Data] , H: [" + HexDumper.dump(data)+"]", no,type,length);
		}
		return retStr;
	}

	public String toStringSimple(Object data) throws UnsupportedEncodingException {
		try{
			if(data instanceof String){
				return String.format("|%1$04d | "+nameKorPadding+" | %2$-8s | %3$06d | V: ["+(data!=null ? data : "no data") + "] , H: [" + HexDumper.dump(((String) data).getBytes())+"]", no,type,length);
			}else if(data instanceof byte[]){
				return String.format("|%1$04d | "+nameKorPadding+" | %2$-8s | %3$06d | V: ["+(data!=null ? new String((byte[])data).replace((char)0x00, '_') : "no data") + "] , H: [" + HexDumper.dump((byte[])data)+"]", no,type,length);
			}else if(data instanceof Integer || data instanceof Double || data instanceof Short || data instanceof Long  || data instanceof Float){
				return String.format("|%1$04d | "+nameKorPadding+" | %2$-8s | %3$06d | V: ["+(data!=null ? data : "no data") + "]", no,type,length);
			}else{
				return String.format("|%1$04d | "+nameKorPadding+" | %2$-8s | %3$06d | V: ["+(data!=null ? data : "no data") + "]", no,type,length);
			}
		}catch (Exception e) {
			return String.format("|%1$04d | "+nameKorPadding+" | %2$-8s | %3$06d | V: [Unmarkable Data] ", no,type,length);
		}

	}


		public static void main(String[] args) {
			System.out.println(new String(new byte[]{0x01}));
	}
}
