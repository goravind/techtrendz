package com.example.beans;

import org.springframework.data.annotation.Id;

public class Tech {

	@Id
	private String techName;

	private Long millisec;

	public String getTechName() {
		return techName;
	}

	public void setTechName(String techName) {
		this.techName = techName;
	}

	public Long getMillisec() {
		return millisec;
	}

	public void setMillisec(Long millisec) {
		this.millisec = millisec;
	}

}
