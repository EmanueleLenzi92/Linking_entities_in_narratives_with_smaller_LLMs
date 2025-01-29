import java.io.*;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLEncoder;

import org.apache.commons.codec.digest.DigestUtils;
import org.gcube.moving.utils.HTTPRequests;
import org.gcube.moving.utils.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;




public class WikidataExplorer2 {

	// Wikidata query URL sparql

	static String WD_URL = "https://query.wikidata.org/sparql?query=";

	// URL per la query alle api della ricerca su Wikidata
	static String URL = "https://www.wikidata.org/w/api.php";

	public String queryWikidata(String entity) throws Exception {
		return queryWikidata(entity, false);
		
	}

	public Pair getCoordinates(String entity) throws Exception{
		queryWikidata(entity);
		return wikidataPair;
	}
	

	
	
	public String queryWikidata(String entity, boolean controlmode) throws Exception {
		File sha1 = new File("wikidatacache/sha" + DigestUtils.sha1Hex(entity) + ".txt");
		String uri = "";

		if (sha1.exists() && !controlmode) {
			FileInputStream fos = new FileInputStream(sha1);
			ObjectInputStream oos = new ObjectInputStream(fos);
			WikiObj wo =  (WikiObj) oos.readObject();
			uri = wo.uri;
			this.wikidataPair = wo.coordinates;
			//System.out.println("Recovered Pair "+this.wikidataPair);
			oos.close();

		} else {

			String ent = entity.trim();
			ent = ent.replaceAll(" +", " ");
			System.out.println("Wikidata - Analysing entity written as "+ent);
			uri = analyseResponse(ent);
			if (uri.length()==0) {
				
				ent= prepareEntity(entity);
				System.out.println("Wikidata - no luck - Re-analysing entity written as "+ent);
				uri = analyseResponse(ent);
				
				if (uri.length()==0) {
					ent = entity.trim();
					ent = ent.toLowerCase();
					ent = ent.replaceAll(" +", " ");
					System.out.println("Wikidata - no luck x2 - Re-analysing entity written as "+ent);
					
					uri = analyseResponse(ent);
				}
				
			}
			
			WikiObj wo = new WikiObj(uri, wikidataPair);
			
			FileOutputStream fos = new FileOutputStream(sha1);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(wo);
			oos.close();

		}

		System.out.println("Wikidata result: " + entity + "->" + uri + " | coordinates: "+wikidataPair );
		return uri;
	}

	public String analyseResponse(String ent) throws Exception {
	    String uri = "";
	    try {
	        // Creazione della query SPARQL
	        String q = "SELECT DISTINCT ?entity ?label WHERE {?entity rdfs:label \"" + ent
	                + "\"@en . SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE],en\" }}";

	        String query = "https://query.wikidata.org/sparql?query=" + URLEncoder.encode(q, "UTF-8") + "&format=json";
	        String url = query;

	        // Esecuzione della query e parsing della risposta
	        String response = HTTPRequests.getRequest(url);
	        List<String> uris = parseResponseToList(response);

	        // Controllo tutti gli IRI trovati
	        for (String candidateUri : uris) {
	            if (isValid(ent, candidateUri)) {
	                uri = candidateUri; // Se valido, salva l'IRI e interrompi il ciclo
	                break;
	            }
	        }

	        // Se nessun IRI è valido
	        if (uri.length() == 0) {
	            System.out.println("No valid IRI found for entity: " + ent);
	        }

	    } catch (Exception e) {
	        System.out.println("Wikidata - Error as answer to the query: " + e.getLocalizedMessage());
	    }
	    return uri; // Ritorna l'IRI valido o una stringa vuota
	}
	
	public static List<String> parseResponseToList(String response) throws Exception {
	    List<String> uris = new ArrayList<>();
	    int idx = response.indexOf("\"value\"");
	    while (idx > -1) {
	        String res = response.substring(idx);
	        res = res.substring(res.indexOf(":") + 1);
	        res = res.substring(0, res.indexOf("}"));
	        uris.add(res.replace("\"", "").trim());
	        idx = response.indexOf("\"value\"", idx + 1);
	    }
	    return uris;
	}
	
	public Pair wikidataPair = null;
	
	
	public double dmsToDecimal(String DMS) throws Exception {
		
		DMS = DMS.replaceAll( "[^0-9\\.'°NSWE]+" , "");
		
		int degIdx = DMS.indexOf("°");
		int minIdx = DMS.indexOf("'");
		int len = DMS.length();
		
		double deg = Integer.parseInt(DMS.substring(0,degIdx));
		
		if (minIdx>-1) {
			double minutes = Double.parseDouble(DMS.substring(degIdx+1,minIdx));
			//minutes = minutes/60d;
			String secondsS = DMS.substring(minIdx+1,len-1);
			double seconds = 0;
			if (secondsS.length()>0) {
				seconds = Double.parseDouble(secondsS);
				seconds = seconds/60d;
			}
			minutes = minutes+seconds;
			minutes = minutes/60d;

			deg = deg+minutes;
		}
		
		if (DMS.charAt(len-1) == 'S' || DMS.charAt(len-1) == 'W')
			deg = -deg;
		
		
		return deg;
	}
	
	
	public void extractCoordinates(String response) throws Exception {
		
		try {
		response = response.substring(response.indexOf(">coordinate location<") + 1);
		response = response.substring(response.indexOf("wikibase-kartographer-caption\">") + 1);
		response = response.substring(response.indexOf(">")+1,response.indexOf("<"));
		response = response.trim();
		System.out.println("COORDINATE String "+response);
		
		String latString = response.substring(0,response.indexOf(","));
		double lat = dmsToDecimal(latString);
		String lonString = response.substring(response.indexOf(",")+1);
		double lon = dmsToDecimal(lonString);
		
		System.out.println("COORDINATES "+lon+";"+lat);
		wikidataPair = new Pair(lon,lat);
		}catch(Exception e ) {
			System.out.println("UNPARSABLE COORDINATES");
		}
		System.out.println("Estimated Pair "+wikidataPair);
	}
	
	
	public boolean isValid(String entity, String uri) throws Exception {

		try {
			String response = HTTPRequests.getRedirectedPage(uri);
			String wikipage = new String(response);
			
			//System.out.println("wikidata check:\n" + response);

			if (response.toLowerCase().contains("wikimedia disambiguation page")) {
				System.out.println("Term "+entity+" corresponds to disambiguation page -> INVALID");
				return false;
			} else {
				try {
					
					response = response.substring(response.indexOf("wikibase-sitelinkgrouplistview") + 1);
					response = response.substring(response.indexOf("title=\"English\">enwiki</span>") + 1);
					response = response.substring(response.indexOf("title=\"") + "title=\"".length());
					response = response.substring(0, response.indexOf("\""));
					response = response.trim();
					System.out.println("Wikidata - there is a NAME IN ENGLISH on WIKIPEDIA:" + response);
				} catch (Exception e) {
					System.out.println("Error finding the wikipedia en page:" + e.getLocalizedMessage()+"->INVALID");

				}
				if (entity.equalsIgnoreCase(response)) {
					System.out.println("Wikidata - THE NAME IN ENGLISH on WIKIPEDIA CORRESPONDS TO THE ENTITY "+entity+"->VALID");
					extractCoordinates(wikipage);
					return true;
				}
				else {
					System.out.println("Wikidata - THE NAME IN ENGLISH on WIKIPEDIA DOES NOT EXIST OR DOES NOT CORRESPOND TO THE ENTITY "+entity+"->INVALID");
					return false;
				}
			}

		} catch (Exception e) {
			System.out.println("Issue on the page " + uri + " : " + e.getLocalizedMessage());
			e.printStackTrace();
			System.exit(0); 
			return false;
		}
	}


	public List<String> getAliasesFromWikidata(String uri) throws Exception {
	    List<String> aliases = new ArrayList<>();

	    try {
	        // Costruzione della query SPARQL per ottenere gli alias
	        String q = "SELECT DISTINCT ?alias WHERE { <" + uri + "> skos:altLabel ?alias . FILTER (lang(?alias) = \"en\") }";
	        String query = "https://query.wikidata.org/sparql?query=" + URLEncoder.encode(q, "UTF-8") + "&format=json";

	        // Esecuzione della richiesta
	        String response = HTTPRequests.getRequest(query);

	        // Parsing della risposta JSON
	        int idx = response.indexOf("\"value\"");
	        while (idx > -1) {
	            String res = response.substring(idx);
	            res = res.substring(res.indexOf(":") + 1);
	            res = res.substring(0, res.indexOf("}"));
	            aliases.add(res.replace("\"", "").trim());
	            idx = response.indexOf("\"value\"", idx + 1);
	        }

	    } catch (Exception e) {
	        System.out.println("Error retrieving aliases for " + uri + ": " + e.getLocalizedMessage());
	    }

	    return aliases;
	}

	public static String prepareEntity(String entity) {

			String e = new String(entity);
			e = e.trim();
			e = e.toLowerCase();
			e = e.replaceAll(" +", " ");
			String[] es = e.split(" ");
			StringBuffer sb = new StringBuffer();
			System.out.println(e);
			for (int i = 0; i < es.length; i++) {

				String ent = es[i];
				if (ent.trim().length() > 0) {
					String c1 = ("" + ent.charAt(0)).toUpperCase();
					String c2 = ent.substring(1);

					sb.append(c1 + c2);
					if (i < es.length - 1)
						sb.append(" ");
				}
			}
			System.out.println(sb);

			return sb.toString();
		
	}

	public static String parseResponse(String response) throws Exception {
		System.out.println(response);
		int idx = response.indexOf("\"value\"");
		System.out.println(idx);
		String minimalURI = "";
		while (idx > -1) {

			String res = response.substring(idx);
			res = res.substring(res.indexOf(":") + 1);
			res = res.substring(0, res.indexOf("}"));

			String uri = res.replace("\"", "");
			uri = uri.trim();
			//System.out.println("URI->" + uri);

			if (uri.startsWith("http")) {
				if (minimalURI.length() == 0)
					minimalURI = uri;
				else if (minimalURI.length() > uri.length()) {
					minimalURI = uri;
				}

			}
			response = response.substring(idx + 1);

			idx = response.indexOf("\"value\"");

		}
		System.out.println(minimalURI);
		return minimalURI;

	}


	public static void main(String[] args) throws Exception {
	    // Configura il mapper e le cartelle di input/output
	    ObjectMapper mapper = new ObjectMapper();
	    File inputFolder = new File("predictions/LLMsOriginals/gemma22b");  // Cambia il percorso se necessario
	    File outputFolder = new File("new_folder_to_create");
	    if (!outputFolder.exists()) {
	        outputFolder.mkdir();
	    }

	    // Legge i file JSON nella cartella di input
	    File[] files = inputFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
	    if (files == null || files.length == 0) {
	        System.out.println("Nessun file JSON trovato nella cartella: " + inputFolder.getAbsolutePath());
	        return;
	    }

	    for (File file : files) {
	        System.out.println("Processing file: " + file.getAbsolutePath());

	        try {
	            // Legge il contenuto del file JSON
	            JsonNode rootNode = mapper.readTree(file);
	            if (!rootNode.isArray()) {
	                System.out.println("Il file non contiene un array JSON: " + file.getName());
	                continue;
	            }

	            ArrayNode jsonArray = (ArrayNode) rootNode;
	            //System.out.println("Loaded JSON: " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonArray));

	            // Itera sugli oggetti dell'array
	            for (JsonNode node : jsonArray) {
	                //System.out.println("Current node: " + node);

	                JsonNode entitiesNode = node.path("entities");
	                //System.out.println("Entities Node: " + entitiesNode);

	                // Controlla se "entities" è un array
	                if (!entitiesNode.isArray()) {
	                    System.out.println("'entities' is not an array or is missing in node: " + node);
	                    continue;
	                }

	                // Itera sugli elementi di "entities"
	                Iterator<JsonNode> entities = entitiesNode.elements();
	                if (!entities.hasNext()) {
	                    System.out.println("'entities' array is empty in node: " + node);
	                    continue;
	                }

	                while (entities.hasNext()) {
	                    JsonNode entityNode = entities.next();
	                    //String entityText = entityNode.path("entity_in_the_text").asText();
	                    //String entityText2 = entityNode.path("wikipedia_title").asText();
	                    String entityText = entityNode.path("text_span").asText();
	                    String entityText2 = entityNode.path("text_span").asText();

	                    if (entityText == null || entityText.isEmpty()) {
	                        System.out.println("Skipping entity with empty or missing 'text_span': " + entityNode);
	                        continue;
	                    }

	                    //System.out.println("Processing wikipedia_label: " + entityText);
	                    ((ObjectNode) entityNode).put("originalKey", entityText);
	                    ((ObjectNode) entityNode).put("original_value", entityText2);

	                    // Chiama la funzione queryWikidata
	                    String result;
	                    try {
	                        result = new WikidataExplorer2().queryWikidata(entityText2, true);
	                        String[] parts = result.split("/");
	                        String lastElement = parts[parts.length - 1];
	                        System.out.println("Wikidata result for " + entityText2 + ": " + lastElement);
	                        
	                        ((ObjectNode) entityNode).put("Wikidata_ID", lastElement);
	                    } catch (Exception e) {
	                        System.out.println("Errore durante la query per: " + entityText2);
	                        e.printStackTrace();
	                        ((ObjectNode) entityNode).put("Wikidata_ID", "ERROR");
	                    }
	                }
	            }

	            // Scrive il JSON aggiornato nella cartella di output
	            String originalFileName = file.getName();
	            //String newFileName = originalFileName.replaceFirst("\\.json$", ".csv.json");
	            File outputFile = new File(outputFolder, originalFileName);
	            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, jsonArray);
	            System.out.println("File salvato in: " + outputFile.getAbsolutePath());

	        } catch (Exception e) {
	            System.out.println("Errore durante l'elaborazione del file: " + file.getName());
	            e.printStackTrace();
	        }
	    }
	}

	
 
}