import urllib.parse, urllib.request, json, os, csv, time, requests

def aggiorna_file_json(nuovi_dati, percorso_file):
    """
    Create and update the json files of the 30 MOVING narratives with keywords retrieved by the Falcon API in a root
    """
    if os.path.exists(percorso_file):
        with open(percorso_file, 'r', encoding='utf-8') as file:
            dati_esistenti = json.load(file)
    else:
        dati_esistenti = []

    # add new data in the json
    dati_esistenti.append(nuovi_dati)

    # save the json
    with open(percorso_file, 'w', encoding='utf-8') as file:
        json.dump(dati_esistenti, file, ensure_ascii=False, indent=4)


def CallFalconAPI(text):
    """
    Call the Falcon API and find the keywords in a text
    """
    
    #create json
    new_item = {"entities": []}
    

    # URL dell'API
    url = "https://labs.tib.eu/falcon/falcon2/api?mode=long"
    
    # Header request
    headers = {
        "Content-Type": "application/json"
    }
    
    # Dati da inviare
    data = {
        "text": text
    }

    # POST request
    response = requests.post(url, headers=headers, json=data)

    # print the answer
    if response.status_code == 200:
        print("Risposta:", response.json())

        for annotation in response.json()["entities_wikidata"]:
            entity = {
                "originalKey": annotation["surface form"],
                "original_value": annotation["surface form"],   
                "Wikidata_ID": annotation["URI"]
            }
            
            new_item["entities"].append(entity)
    else:
        print(f"Errore {response.status_code}: {response.text}")

    time.sleep(30)

    return new_item


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
                    
                    # call the Falcon 2.0 APIs
                    json_data= CallFalconAPI(sen)
                    print(json_data)
                   
                   #update and save the json with the keywords found by Falcon 2.0
                    aggiorna_file_json(json_data, percorso_file_json_da_salvare + "/" + filename + ".json")