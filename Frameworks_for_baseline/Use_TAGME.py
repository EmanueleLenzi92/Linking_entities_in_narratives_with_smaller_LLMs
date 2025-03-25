import requests
import os
import json
import urllib.parse, urllib.request, csv, time

def aggiorna_file_json(nuovi_dati, percorso_file):
    """
    Create and update the json files of the 30 MOVING narratives with keywords retrieved by the TAGME API in a root
    """
    
    if os.path.exists(percorso_file):
        with open(percorso_file, 'r', encoding='utf-8') as file:
            dati_esistenti = json.load(file)
    else:
        dati_esistenti = []

    # Aggiungi i nuovi dati ai dati esistenti
    dati_esistenti.append(nuovi_dati)

    # Salva il file aggiornato
    with open(percorso_file, 'w', encoding='utf-8') as file:
        json.dump(dati_esistenti, file, ensure_ascii=False, indent=4)

def get_wikidata_entity_from_wikipedia_title(language, title):
    """
    Get Wikidata id from a Wikipedia title usind WIkpiedia API
    """
    
    url = f"https://{language}.wikipedia.org/w/api.php"
    
    # parameters request
    params = {
        "action": "query",
        "prop": "pageprops",
        "titles": title,
        "format": "json",
        "redirects": 1  
    }
    
    # HTTP GET request to Wikipedia API
    response = requests.get(url, params=params)
    
    data = response.json()
    

    pages = data.get("query", {}).get("pages", {})
    if pages:
        page = next(iter(pages.values())) 
        if "pageprops" in page and "wikibase_item" in page["pageprops"]:
            return page["pageprops"]["wikibase_item"]
        else:
            return None  
    else:
        return None 

    time.sleep(50)


# parameters
directory= "../selected_MOVING_narratives"
percorso_file_json_da_salvare= "baseline_data_output"



# for all the CSV in the "selected_MOVING_narratives" folder
for filename in os.listdir(directory):

    if filename.endswith(".csv"):
        filepath = os.path.join(directory, filename)
      
   
        # open the CSV
        with open(filepath, newline='', encoding='utf-8') as csvfile:
            csvreader = csv.reader(csvfile)
                    
            # skip the first row
            next(csvreader)

            for row in csvreader:
                
                # get the value of the second column (textual description)
                if len(row) > 1:
                    sen = row[1]
                    new_item = {"entities": []}

                    # parameters for request to TAGME API
                    textI= sen
                    
                    params = {
                        "text": textI,
                        "lang": "en",
                        "gcube-token": "a470fac7-cba8-498e-8a3f-d68baa3515da-843339462"
                    }
                    
                    link= "https://tagme.d4science.org/tagme/tag"
                    
                    response1 = requests.post(link, params=params)

                    data = response1.json()
                    

                    # iterate on the annotations of the answer
                    annotations = data.get("annotations", [])  
                    for annotation in annotations:
                        title = annotation.get("title")  
                        if title:
                            #call function to get wikidata id from a wikipedia title found by TAGME
                            wikidata_entity = get_wikidata_entity_from_wikipedia_title("en", title)
                            spot = annotation.get("spot")
                            rho = annotation.get("rho")
                            entity = {
                                "originalKey": spot,
                                "original_value": title,
                                 "Wikidata_ID": wikidata_entity,
                                "rho": rho
                            }

                            new_item["entities"].append(entity)
                            #print(wikidata_entity)
                    
                    #save and update the file
                    aggiorna_file_json(new_item, percorso_file_json_da_salvare + "/" + filename + ".json")
                    time.sleep(50)