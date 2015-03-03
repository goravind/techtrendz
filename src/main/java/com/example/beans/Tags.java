package com.example.beans;

import java.util.List;

import org.springframework.data.annotation.Id;

public class Tags {

	private String tagName;

	private Long millisec;

	private Long count;

	private Long commitCount;

	private List<String> synonyms;

	@Id
	private String id;

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Long getCount() {
		return count;
	}

	public Tags(String tagName, Long millisec, Long count, List<String> synonyms) {
		super();
		this.tagName = tagName;
		this.millisec = millisec;
		this.count = count;
		this.synonyms = synonyms;
	}

	public void setCount(Long count) {
		this.count = count;
	}

	public List<String> getSynonyms() {
		return synonyms;
	}

	public void setSynonyms(List<String> synonyms) {
		this.synonyms = synonyms;
	}

	public Long getCommitCount() {
		return commitCount;
	}

	public void setCommitCount(Long commitCount) {
		this.commitCount = commitCount;
	}

	@Override
	public String toString() {
		return "Tags [tagName=" + tagName + ", millisec=" + millisec
				+ ", count=" + count + ", commitCount=" + commitCount
				+ ", synonyms=" + synonyms + ", id=" + id + "]";
	}

}
