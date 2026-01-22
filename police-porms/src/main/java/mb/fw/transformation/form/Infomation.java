package mb.fw.transformation.form;

import java.io.Serializable;

public class Infomation implements Cloneable , Serializable {

	private static final long serialVersionUID = 865463849291596647L;
	String AgentName;
	String tranCode;
	String tranComment;
	String tranDirection;
	String tranMode;
	String changeTranCode;
	String redirectionAgentName;
	String inType;
	String outType;
	String serviceName;
	String reserve;

	public String getAgentName() {
		return AgentName;
	}

	public void setAgentName(String agentName) {
		AgentName = agentName;
	}

	public String getTranCode() {
		return tranCode;
	}

	public void setTranCode(String tranCode) {
		this.tranCode = tranCode;
	}

	public String getTranComment() {
		return tranComment;
	}

	public void setTranComment(String tranComment) {
		this.tranComment = tranComment;
	}

	public String getTranDirection() {
		return tranDirection;
	}

	public void setTranDirection(String tranDirection) {
		this.tranDirection = tranDirection;
	}

	public String getChangeTranCode() {
		return changeTranCode;
	}

	public void setChangeTranCode(String changeTranCode) {
		this.changeTranCode = changeTranCode;
	}

	public String getRedirectionAgentName() {
		return redirectionAgentName;
	}

	public void setRedirectionAgentName(String redirectionAgentName) {
		this.redirectionAgentName = redirectionAgentName;
	}

	public String getInType() {
		return inType;
	}

	public void setInType(String inType) {
		this.inType = inType;
	}

	public String getOutType() {
		return outType;
	}

	public void setOutType(String outType) {
		this.outType = outType;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getReserve() {
		return reserve;
	}

	public void setReserve(String reserve) {
		this.reserve = reserve;
	}

	public void setTranMode(String tranMode) {
		this.tranMode = tranMode;
	}

	public String getTranMode() {
		return tranMode;
	}

	@Override
	public String toString() {
		return "Infomation [AgentName=" + AgentName + ", tranCode=" + tranCode + ", tranComment=" + tranComment
				+ ", tranDirection=" + tranDirection + ", tranMode=" + tranMode + ", changeTranCode=" + changeTranCode
				+ ", redirectionAgentName=" + redirectionAgentName + ", inType=" + inType + ", outType=" + outType
				+ ", serviceName=" + serviceName + ", reserve=" + reserve + "]";
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}


}
