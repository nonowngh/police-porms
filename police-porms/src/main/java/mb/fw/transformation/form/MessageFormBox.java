package mb.fw.transformation.form;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

public class MessageFormBox implements Serializable{

	private static final long serialVersionUID = -1449456252222642260L;

	LinkedHashMap<String,MessageForm> map = new LinkedHashMap<String,MessageForm>();

	public int size(){
		return map.size();
	}

	public Set<String> keySet(){
		return map.keySet();
	}

	//변경
	public void put(MessageForm form){
		map.put(form.getTransCode(), form);
	}
//	public void put(MessageForm form){
//		map.put(form.getTranDirection()+form.getTransCode(), form);
//	}

	public boolean contains(MessageForm form){
		return map.containsKey(form.getTranDirection()+form.getTransCode());
	}

	public boolean containsKey(String key){
		return map.containsKey(key);
	}

	public MessageForm remove(String key){
		return map.remove(key);
	}

	public MessageForm remove(MessageForm form){
		return map.remove(form.getTranDirection()+form.getTransCode());
	}

	public MessageForm getClone(String key){
		return map.get(key).copidMessageForm();
	}

	public MessageForm get(String key){
		return map.get(key);
	}
	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		Set<String> keySet = map.keySet();
		if(keySet.size()<=0){
			return "no data";
		}
		for (Iterator<String> iterator = keySet.iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			buff.append(map.get(key).toString()).append("\n");
		}

		return buff.toString();
	}

}
