package mb.fw.transformation.form;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

public class MessageForm implements Serializable {

	private static final long serialVersionUID = -7651088533192308051L;

	Infomation infomation = new Infomation();

	RecordContext inContext   = new  RecordContext();
	RecordContext outContext  = new  RecordContext();

	public void setInContext(RecordContext inContext) {
		this.inContext = inContext;
	}

	public void setOutContext(RecordContext outContext) {
		this.outContext = outContext;
	}

	public RecordContext getInContext() {
		return inContext;
	}
	public RecordContext getOutContext() {
		return outContext;
	}

	public String getTransCode() {
		return infomation.getTranCode();
	}

	public String getTranDirection() {
		return infomation.getTranDirection();
	}

	public Infomation getInfomation() {
		return infomation;
	}

	public void setInfomation(Infomation infomation) {
		this.infomation = infomation;
	}



	@Override
	public String toString() {
		return "MessageForm [TranDirection : " + infomation.getTranDirection() + ", TranCode : " + infomation.getTranCode()
				+ ", infomation=" + infomation + ", inRecord size=" + inContext.size() + ", outRecord size="
				+ outContext.size() + "]";
	}

	public MessageForm copidMessageForm() {

		RecordContext inRecordClone = new RecordContext();
		RecordContext outRecordClone = new RecordContext();
		Set<Integer> keySet = null;

		keySet = inContext.keySet();

		for (Iterator<Integer> iterator = keySet.iterator(); iterator.hasNext();) {
			int key = (int) iterator.next();
			Record recordClone = null;
			try {
				recordClone = (Record) inContext.get(key).clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			inRecordClone.put(key, recordClone);
		}

		keySet = outContext.keySet();

		for (Iterator<Integer> iterator = keySet.iterator(); iterator.hasNext();) {
			int key = (int) iterator.next();
			Record recordClone = null;
			try {
				recordClone = (Record) outContext.get(key).clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			outRecordClone.put(key, recordClone);
		}

		inRecordClone.setTmpData(inContext.getTmpData());
		outRecordClone.setTmpData(outContext.getTmpData());


		MessageForm copidForm = new MessageForm();

		try {
			copidForm.setInfomation((Infomation) infomation.clone());
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}

		copidForm.setInContext(inRecordClone);
		copidForm.setOutContext(outRecordClone);
		return copidForm;
	}


}
