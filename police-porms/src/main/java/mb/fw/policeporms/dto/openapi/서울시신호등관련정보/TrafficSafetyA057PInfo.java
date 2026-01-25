package mb.fw.policeporms.dto.openapi.서울시신호등관련정보;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TrafficSafetyA057PInfo {

	@JsonProperty("list_total_count")
	private int listTotalCount;

	@JsonProperty("RESULT")
	private Result result;

	@JsonProperty("row")
	private List<TrafficSafety> row;

	public int getListTotalCount() {
		return listTotalCount;
	}

	public void setListTotalCount(int listTotalCount) {
		this.listTotalCount = listTotalCount;
	}

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	public List<TrafficSafety> getRow() {
		return row;
	}

	public void setRow(List<TrafficSafety> row) {
		this.row = row;
	}
}
