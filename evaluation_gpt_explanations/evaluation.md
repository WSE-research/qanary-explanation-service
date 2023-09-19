# Evaluation automatically generated explanations

## General
- Spreadsheet: https://docs.google.com/spreadsheets/d/1rfPl-nhc7O_-Pi79eJ7wICjf26Ga49Hb7dopuB53Kto/edit?usp=sharing
- the generated explanations' format isn't consistent, e.g.
	- code, like SPARQL-queries or JSON are either embedded, separated in new paragraphs or just in line
	- listings are either inline or structured as a list (numbered or "-"-annotated)

## Overview for annotation-types

### Annotation of type `AnnotationOfAnswerSPARQL`
- within the same ann.-type every generated explanation was acceptable
- cross-type ann.-types:
	- with `AnnotationOfInstance` as provided 1-shot example:
		- 1-shot: bad explanation, semantically wrong 
		- 2-shot (example of type `AnnotationOfSpotInstance`): good explanation
		- 3-shot (another example of type `AnnotationOfAnswerJSON`): quite the same as the 1-shot-approach
	- with`AnnotationOfAnswerJSON` as provided 1-shot example:
		- 1-shot: good explanation
	- with`AnnotationOfSpotInstance` as provided 1-shot example:
		- 1-shot: well, very general, semantically not 100% correct
		- 2-shot: acceptable, a bit more detailed, correct
		- 3-shot: bad, semantically wrong
### Annotation of type `AnnotationOfSpotInstance`
- within the same ann.-type every generated explanation was almost identical
- cross-type ann.-types:
	- with `AnnotationOfInstance` as provided 1-shot example:
		- 1-shot: bad, semantically wrong
		- 2-shot (example of type `AnnotationOfAnswerSPARQL`): well, missing relevant information
		- 3-shot (another example of type `AnnotationOfAnswerJSON`): well, quite the same as the 2-shot-approach
	- with`AnnotationOfAnswerSPARQL` as provided 1-shot example:
		- 1-shot: bad, semantically wrong
		- 2-shot (example of type `AnnotationOfInstance`): bad, semantically wrong
		- 3-shot (another example of type `AnnotationOfAnswerJSON`): well, ..
	- with`AnnotationOfAnswerJSON` as provided 1-shot example:
		- 1-shot: well, mixed structure, difficult to understand
		- 2-shot (example of type `AnnotationOfAnswerSPARQL` and/or `AnnotationOfInstance`): both bad, questionID or annotation-type as "entities" refered -> semantically wrong
		- 3-shot: acceptable, minimalized details
### Annotation of type `AnnotationOfInstance`
- within the same ann.-type:
	- depending on "score" exists in data:
		- if provided in example -> generated on is correct
		- if not provided -> generated one doesnt provide it as well, even though it's available in data
	- otherwise its good
- cross-type ann.-types:
	- with`AnnotationOfAnswerSPARQL` as provided 1-shot example:
		- 1-shot: well, entities are refered as instances, missing time-stamp, otherwise okay
		- 2-shot (example of type `AnnotationOfSpotInstance`): okay, missing score but a good explanation
		- 3-shot (additional example of type `AnnotationOfAnswerJSON`): similar to 2-shot-approach
	- with`AnnotationOfAnswerJSON` as provided 1-shot example:
		- 1-shot: well, referes to blank nodes
		- 2-shot (example of type `AnnotationOfAnswerSPARQL`): same as 1-shot-approach
		- 3-shot (additional example of type `AnnotationOfSpotInstance`): good
	- with`AnnotationOfSpotInstance` as provided 1-shot example:
		- 1-shot: good, even adopted the prefix from given explanation
		- 2-shot (example of type `AnnotationOfInstance` or `AnnotationOfAnswerSPARQL`): good
### Annotation of type `AnnotationOfAnswerJSON`
- withing the same ann.-type:

- cross-type ann.-types:
	- with`AnnotationOfInstance` as provided 1-shot example:
		- 1-shot:
		- 2-shot:
		- 3-shot:
	- with`AnnotationOfAnswerSPARQL` as provided 1-shot example:
		- 1-shot:
		- 2-shot:
		- 3-shot:
	- with`AnnotationOfSpotInstance` as provided 1-shot example:
		- 1-shot:
		- 2-shot:
		- 3-shot:
