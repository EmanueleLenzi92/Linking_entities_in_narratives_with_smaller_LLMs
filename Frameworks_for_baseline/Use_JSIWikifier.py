import urllib.parse, urllib.request, json, os, csv, time

def aggiorna_file_json(nuovi_dati, percorso_file):
    """
    Create and update the json files of the 30 MOVING narratives with keywords retrieved by the JSI API in a root
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

def CallWikifier(text, lang="en", threshold=0.7):
    """
    call the JSI wikifier API
    """
    #create json
    new_item = {"entities": []}
    
    # Prepare the URL.
    data = urllib.parse.urlencode([
        ("text", text), ("lang", lang),
        ("support", "true"),
        ("userKey", "xvjsnkvomibidzigxwyjfihxjrkxhh"),
        ("pageRankSqThreshold", "%g" % threshold), ("applyPageRankSqThreshold", "true"),
        ("nTopDfValuesToIgnore", "200"), ("nWordsToIgnoreFromList", "200"),
        ("wikiDataClasses", "true"), ("wikiDataClassIds", "false"),
        ("support", "true"), ("ranges", "false"), ("minLinkFrequency", "2"),
        ("includeCosines", "false"), ("maxMentionEntropy", "3")
        ])
        
    url = "http://www.wikifier.org/annotate-article"
    
    # Call the Wikifier and read the response.
    req = urllib.request.Request(url, data=data.encode("utf8"), method="POST")
    with urllib.request.urlopen(req, timeout = 60) as f:
        response = f.read()
        #print(type(response.decode('utf-8')))
        response = json.loads(response.decode("utf8"))
        print(response)
    words = response.get("words", [])  # Extract words list

    # Output the annotations.
    for annotation in response["annotations"]:
        w_from = annotation["support"][0]["wFrom"]
        w_to = annotation["support"][0]["wTo"]
        original_key = " ".join(words[w_from:w_to + 1])
        entity = {
            "originalKey": original_key,
            "original_value": annotation["title"]
        }
        # add "Wikidata_ID" only if "wikiDataItemId" exists
        if "wikiDataItemId" in annotation:
            entity["Wikidata_ID"] = annotation["wikiDataItemId"]
        else:
            entity["Wikidata_ID"] = "null"
        
        
        new_item["entities"].append(entity)
        #print("%s (%s)" % (annotation["title"],  annotation["wikiDataItemId"]))
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
                    
                    # call the JSI wikifier API
                    json_data= CallWikifier(sen)
                       
                    #update and save the data
                    aggiorna_file_json(json_data, percorso_file_json_da_salvare + "/" + filename + ".json")