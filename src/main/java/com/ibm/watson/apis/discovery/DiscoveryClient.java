/*
 * Copyright 2015 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.ibm.watson.apis.discovery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.watson.apis.payload.DocumentPayload;
import com.ibm.watson.apis.utils.Constants;
import com.ibm.watson.apis.utils.Messages;
import com.ibm.watson.developer_cloud.discovery.v1.model.query.QueryResponse;

/**
 * DiscoveryClient.
 */
public class DiscoveryClient {

  private static final Logger logger = LogManager.getLogger(DiscoveryClient.class.getName());

  private static final int SNIPPET_LENGTH = 150;

  private Pattern pattern = Pattern.compile("((.+?)</p>){1,2}");
  
  /**
   * This method uses the Query object to send the user's query (the <code>input</code> param) to the discovery service.
   *
   * @param input The user's query to be sent to the discovery service
   * @return A list of DocumentPayload objects, each representing a single document the discovery service believes is a
   *         possible answer to the user's query
   * @throws Exception the exception
   */
  public List<DocumentPayload> getDocuments(String input) throws Exception {
    DiscoveryQuery discoveryQuery = new DiscoveryQuery();
    QueryResponse output = discoveryQuery.query(input);
    List<Map<String, Object>> results = output.getResults();
    String jsonRes = new Gson().toJson(results);
    JsonElement jelement = new JsonParser().parse(jsonRes);
    
    return createPayload(jelement);
  }

  /**
   * Helper Method to include highlighting information along with the Discovery response so the final payload
   * includes id,title,body,sourceUrl as json key value pairs.
   *
   * @param resultsElement the results element
   * @return A list of DocumentPayload objects, each representing a single document the discovery service believes is a
   *         possible answer to the user's query
   */
  private List<DocumentPayload> createPayload(JsonElement resultsElement) {
    logger.info(Messages.getString("Service.CREATING_DISCOVERY_PAYLOAD"));
    List<DocumentPayload> payload = new ArrayList<DocumentPayload>();
    JsonArray jarray = resultsElement.getAsJsonArray();

    if (jarray.size() > 0) {
      for (int i = 0; (i < jarray.size()) && (i < Constants.DISCOVERY_MAX_SEARCH_RESULTS_TO_SHOW); i++) {
        JsonObject jsonObj = jarray.get(i).getAsJsonObject();
        DocumentPayload documentPayload = new DocumentPayload();
        String id = jsonObj.get(Constants.DISCOVERY_FIELD_ID).toString().replaceAll("\"", "");
        documentPayload.setId(id);
        
        //Get title either through "title" or "extracted_metadata->title
        String title;
        if (jsonObj.get(Constants.DISCOVERY_FIELD_TITLE) == null) {
        	JsonElement metadata = jsonObj.get("extracted_metadata");
        	title = metadata.getAsJsonObject().get(Constants.DISCOVERY_FIELD_TITLE).toString();
        } else {
        	title = jsonObj.get(Constants.DISCOVERY_FIELD_TITLE).toString();
        }
        documentPayload.setTitle(title.replaceAll("\"", ""));
        
        //body
        String body;
        if (jsonObj.get(Constants.DISCOVERY_FIELD_BODY) != null) {
          body = jsonObj.get(Constants.DISCOVERY_FIELD_BODY).toString();
        } else if (jsonObj.get("enriched_text") != null){
          body = getEnrichedData(jsonObj.get("enriched_text").getAsJsonObject());
        } else {
        	body = "empty";
        	documentPayload.setBody(body);
        }
        if (body != "empty") {
        	body = body.replaceAll("\"", "");
        	documentPayload.setBody(body);
            documentPayload.setBodySnippet(getSniplet(body));
        }
        
        //Source URL of document
        String url;
        if (jsonObj.get(Constants.DISCOVERY_FIELD_SOURCE_URL) != null) {
        	documentPayload.setSourceUrl(jsonObj.get(Constants.DISCOVERY_FIELD_SOURCE_URL)
                    .toString().replaceAll("\"", ""));
        } else if (jsonObj.get("enriched_text").getAsJsonObject().get(Constants.DISCOVERY_FIELD_SOURCE_URL) != null) {
        	documentPayload.setSourceUrl(jsonObj.get("enriched_text").getAsJsonObject()
        			.get(Constants.DISCOVERY_FIELD_SOURCE_URL).toString().replaceAll("\"", ""));
        } else {
        	documentPayload.setSourceUrl("empty");
        }
        
        //Confidence of document
        if (jsonObj.get(Constants.DISCOVERY_FIELD_CONFIDENCE) != null) {
        	documentPayload.setConfidence(jsonObj.get(Constants.DISCOVERY_FIELD_CONFIDENCE)
        			.toString().replaceAll("\"", ""));
        } else if (jsonObj.get("extracted_metadata").getAsJsonObject().get(Constants.DISCOVERY_FIELD_CONFIDENCE) != null) {
        	documentPayload.setConfidence(jsonObj.get("extracted_metadata").getAsJsonObject()
        			.get(Constants.DISCOVERY_FIELD_CONFIDENCE).toString().replaceAll("\"", ""));
        } else {
          documentPayload.setConfidence("0.0");
        }
        payload.add(i, documentPayload);
      }
    } else {
      DocumentPayload documentPayload = new DocumentPayload();
      documentPayload.setTitle("No results found");
      documentPayload.setBody("empty");
      documentPayload.setSourceUrl("empty");
      documentPayload.setBodySnippet("empty");
      documentPayload.setConfidence("0.0");
      payload.add(documentPayload);
    }
    logger.info(Messages.getString("Payload finished"));

    return payload;
  }
  
	/**
	 * Extracts desired information from Discovery's "enriched_text" tag.
	 * 
	 * @param data
	 *            JsonObject containing the enriched_text JsonObject
	 * @return String containing the information to be displayed
	 */
	private String getEnrichedData(JsonObject data) {
		return getEnrichedEntities(data);
	}
	
	/**
	 * Extracts desired information from the enriched entities that
	 * have a relevance level of >= 0.5.
	 * 
	 * @param data
	 * 			JsonObject containing the entriched_text JsonObject
	 * @return String containing the information to be displayed
	 */
	private String getEnrichedEntities(JsonObject data) {
		Set<String> choices = new HashSet<>();
		JsonArray entitiesArray = data.get("entities").getAsJsonArray();
		// entities are already sorted in order of decreasing relevance
		for (int i = 0; i < entitiesArray.size(); i++) {
			JsonObject entity = entitiesArray.get(i).getAsJsonObject();
			double relevance = entity.get("relevance").getAsDouble();
			// only include relevant results
			if (relevance >= 0.5) {
				choices.add(entity.get("text").toString().replaceAll("\"", ""));
			} else {
				break;
			}
		}
		if (choices.size() == 1) {
			return "Did you mean " + choices.toArray()[0] + "?";
		} else if (choices.size() > 1) {
			return "Did you mean:<br/>" + String.join("<br/>", choices);
		} else {
			return "I'm not sure what you meant";
		}
	}

  /**
   * get first <code>SNIPPET_LENGTH</code> characters of body response.
   *
   * @param body discovery response
   * @return
   */
  private String getSniplet(String body) {
    if (body == null) {
      return "";
    }

    int len = body.length();
    if (len > SNIPPET_LENGTH) {
      body = body.substring(0, SNIPPET_LENGTH - 3) + "...";
    }
    return body;
  }

  /**
   * This method limits the response text in this sample app to two paragraphs. For your own application, you can
   * comment out the method to allow the full text to be returned.
   *
   * @param body
   * @return string
   */
  private String limitParagraph(String body) {
    String returnString = body;

    Matcher matcher = pattern.matcher(body);
    if (matcher.find()) {
      returnString = matcher.group(0);
    }

    return returnString;
  }
}
