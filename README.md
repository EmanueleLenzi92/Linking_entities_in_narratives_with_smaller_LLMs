This repository contains all the necessary files for an experiment on entity linking (EL) using smaller open-source LLMs within narrative events. We created a gold-standard dataset of textual narratives, structured into events, and manually annotated with entity mentions and their corresponding Wikidata unique identifiers (QIDs). Wikidata serves as our reference knowledge base for this EL task.

To evaluate entity linking, we tested three approaches:

Direct Linking: We prompted the selected LLMs to identify entities in the text and directly link them to Wikidata QIDs.
SPARQL Querying: The LLMs identified entity mentions, and we used the Wikidata SPARQL Endpoint to retrieve their QIDs.
Wikipedia-Based Linking: The LLMs identified entity mentions and their corresponding Wikipedia titles, and we used Wikipedia APIs to obtain their Wikidata QIDs.

For all approaches, we measured precision, recall, and F1 score by comparing the model's predictions to our gold-standard annotations:

True Positive (TP): The model correctly identifies and links entities to the correct Wikidata QIDs.
False Positive (FP): The model predicts incorrect entities or links them to the wrong QIDs.
False Negative (FN): The model fails to identify or link entities present in the gold standard.
