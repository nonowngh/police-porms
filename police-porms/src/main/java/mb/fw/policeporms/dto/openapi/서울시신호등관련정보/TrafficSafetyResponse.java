package mb.fw.policeporms.dto.openapi.서울시신호등관련정보;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TrafficSafetyResponse {

	@JsonProperty("trafficSafetyA057PInfo")
	private TrafficSafetyA057PInfo trafficSafetyA057PInfo;

	public TrafficSafetyA057PInfo getTrafficSafetyA057PInfo() {
		return trafficSafetyA057PInfo;
	}

	public void setTrafficSafetyA057PInfo(TrafficSafetyA057PInfo trafficSafetyA057PInfo) {
		this.trafficSafetyA057PInfo = trafficSafetyA057PInfo;
	}
}
