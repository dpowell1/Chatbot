package com.ibm.watson.apis.utils;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class JsonTest {

	@Test
	public void test() {
		//Get content from file
		InputStream in = this.getClass().getResourceAsStream("/test_response.json");
		StringBuffer fileContent = new StringBuffer();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String line;
			while ((line = reader.readLine()) != null) {
				fileContent.append(line + "\n");
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Import to JSON
		JsonParser parser = new JsonParser();
		JsonObject jsonObj = parser.parse(fileContent.toString()).getAsJsonObject();
		
		//Parse into object
		Gson gson = new Gson();
		Type listType = new TypeToken<EnrichedTextObject>() {}.getType();
		JsonObject enriched_text = jsonObj.get(Constants.SCHEMA_FIELD_ENRICHED_TEXT).getAsJsonObject();
		EnrichedTextObject enrichedText = gson.fromJson(enriched_text, listType);
		String text = enrichedText.getEntities().get(0).getText();
		if (text == null) {
			fail("Could not parse JSON correctly");
		} else {
			System.out.println(text);
			System.out.println(enrichedText.getConcepts().get(1).getLink());
		}
	}

}
