package mb.fw.transformation.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class JsonUtil {
	
	static JsonParser jp = new JsonParser();
	static Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	public static String jsonPretty(String uglyJSONString){
		JsonElement je = jp.parse(uglyJSONString);
		return gson.toJson(je);
	}

}
