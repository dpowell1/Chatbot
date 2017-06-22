package com.ibm.watson.apis.utils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

public class EnrichedTextObject {

	@SerializedName("entities")
	private List<EnrichedObject> entities;
	@SerializedName("concepts")
	private List<EnrichedObject> concepts;

	/**
	 * Gets the EnrichedText object representation of the JsonObject,
	 * enriched_text.
	 * 
	 * @param enriched_text
	 *            The JsonObject obtained by parsing "enriched_text"
	 * @return
	 */
	public static EnrichedTextObject getEnrichedTextObject(JsonObject enriched_text) {
		// Declare array type
		Gson gson = new Gson();
		Type listType = new TypeToken<EnrichedTextObject>() {}.getType();
		EnrichedTextObject enrichedText = gson.fromJson(enriched_text, listType);
		return enrichedText;
	}

	/**
	 * Gets a concept whose title is contained inside the query string.
	 * 
	 * @param enrichedText
	 *            EnrichedTextObject containing the parsed enriched_text
	 * @param query
	 *            User input that is scanned for a concept
	 * @return
	 */
	public static List<EnrichedObject> searchConcepts(EnrichedTextObject enrichedText, String query) {
		List<EnrichedObject> foundConcepts = new ArrayList<>();
		List<EnrichedObject> concepts = enrichedText.getConcepts();
		for (EnrichedObject concept : concepts) {
			String title = concept.getText().toLowerCase();
			if (query.toLowerCase().contains(title)) {
				foundConcepts.add(concept);
			}
		}
		return foundConcepts;
	}

	public List<EnrichedObject> getEntities() {
		return entities;
	}

	public void setEntities(List<EnrichedObject> entities) {
		this.entities = entities;
	}

	public List<EnrichedObject> getConcepts() {
		return concepts;
	}

	public void setConcepts(List<EnrichedObject> concepts) {
		this.concepts = concepts;
	}

}
