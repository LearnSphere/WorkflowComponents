package edu.cmu.cs.lti.discoursedb.remote;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class SavedQuery {
	String name;
	String querytext;
	String database;
	
	@Override
	public String toString() {
		return "<Query " + name + " on " + database + ">";
	}
	//[{"startTime":null,"endTime":null,"id":1,"propName":"ftf","propType":"query","propValue":"{\"database\":\"openfl\",\"rows\":{\"discourse_part\":[{\"dpid\":\"9\",\"name\":\"openfl/actuate\"}]}}"}]
	
	static SavedQuery parseString(String propRec) {
		JsonObject rec = javax.json.Json.createReader(new StringReader(propRec)).readObject();	
		return parseRecord(rec);
	}
	
	static SavedQuery parseRecord(JsonObject o) {
		if (o.getString("propType").equals("query")) {
			SavedQuery sq = new SavedQuery();
			sq.name = o.getString("propName");
			sq.querytext = o.getString("propValue");
			javax.json.JsonReader jr = 
				    javax.json.Json.createReader(new StringReader(sq.querytext));
			sq.database = jr.readObject().getString("database");
			return sq;
		}
		return null;
	}
	
	static List<SavedQuery> parseListing(JsonArray jlist, String onlyDb) {
		List<SavedQuery> result = new ArrayList<SavedQuery>();
		for (JsonObject o : jlist.getValuesAs(JsonObject.class)) {
			SavedQuery sq = parseRecord(o);
			if (onlyDb == null || sq.database.equals(onlyDb)) {
				result.add(sq);
			}
		}
		return result;
	}
}
