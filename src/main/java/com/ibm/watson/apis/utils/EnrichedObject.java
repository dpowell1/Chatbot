package com.ibm.watson.apis.utils;

import com.google.gson.annotations.SerializedName;

public class EnrichedObject {
	@SerializedName("text")
	private String text;
	@SerializedName("relevance")
	private Double relevance;
	@SerializedName("dbpedia")      //change if a different link is desired
	private String link;
	
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public Double getRelevance() {
		return relevance;
	}
	
	public void setRelevance(Double relevance) {
		this.relevance = relevance;
	}
	
	public String getLink() {
		return link;
	}
	
	public void setLink(String dbpedia) {
		this.link = dbpedia;
	}
	
}