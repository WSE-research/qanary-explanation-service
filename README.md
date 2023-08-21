# Webservice to explain QA components

### Context
QA component mostly create outputs (e.g. query builder create SPARQL queries) and store them as new annotation to the origin graphID in the triplestore. To understand components it's helpful to understand the output they generate, while acknowledge that this is only a first step in explaining the component or even QA systems.

### Functionality
Currently there are the following GET-Requests available:

`/explanation` requires graphID::String
```
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
PREFIX qa: <http://www.wdaqua.eu/qa#>
SELECT *
FROM ?graphURI

WHERE {
  	?annotationId a qa:AnnotationOfInstance.
    ?annotationId rdf:type ?type.
    ?annotationId oa:hasBody ?body.
    ?annotationId oa:hasTarget [
	a oa:SpecificResource;
                  oa:hasSource ?source;
                  oa:hasSelector [
    	a oa:TextPositionSelector;
                  oa:start ?start;
                  oa:end ?end;
    ]
].
    ?annotationId oa:annotatedBy $createdBy .
    ?annotationId oa:annotatedAt $createdAt .
    ?annotationId qa:score ?score .
}
```
- returns a list of objects of all annotations which have the structure above like [qanary-component-NED-DBpediaSpotlight](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary-component-NED-DBpediaSpotlight)
---
`/explanationforquerybuilder` requires graphID:String
```
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
PREFIX qa: <http://www.wdaqua.eu/qa#>
SELECT *
FROM ?graphURI

WHERE {
    ?annotationId a qa:AnnotationOfAnswerSPARQL .
    ?annotationId oa:hasBody ?body .
    ?annotationId oa:annotatedBy $createdBy .
    ?annotationId oa:annotatedAt $createdAt .
    ?annotationId qa:score ?score .
}
```
- returns a textual explanation including all created SPARQL queries (if the graphID has any annotations created by a QB-component)
---
`/getannotations` requires graphID::String
```
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
PREFIX qa: <http://www.wdaqua.eu/qa#>
SELECT *
FROM ?graphURI
WHERE {
    ?annotationId rdf:type ?type.
    ?annotationId oa:hasBody ?body.
    ?annotationId oa:hasTarget ?target.
    ?annotationId oa:annotatedBy $createdBy .
    ?annotationId oa:annotatedAt $createdAt .
}
```
- returns a list of objects with all annotations and the properties from the SPARQL query above
---
`/explainspecificcomponent` requires graphID::String, componentURI::String 
```
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
PREFIX qa: <http://www.wdaqua.eu/qa#>
SELECT *
FROM ?graphURI

WHERE {
    ?annotationId oa:annotatedBy ?componentURI .
    ?annotationId oa:hasBody ?body .
    ?annotationId oa:annotatedAt $createdAt .
    ?annotationId qa:score ?score .
}
```
- depending on the Accept-Header it returns a explanation as RDF/XML, Turtle or JSON-LD
- working for the majority of components since the properties hasBody, annotatedAt and score are mostly provided

### Using the webservice

#### Settings
Change the following settings for your needs:

*applications.properties*
```
server.port = 4000
sparqlEndpoint = http://demos.swe.htwk-leipzig.de:40111/sparql
```
#### Building
`mvn clean install`
#### Usage
- when accessing the described REST endpoints make sure to pass the required variables as **parameter**
  - e.g. when working with curl: `curl http://localhost:4000/explanation?graphID=<whateverGraphidYouHave>` for one parameter
  - or `curl "http://localhost:4000/explanation?graphID=<whateverGraphidYouHave>&<anotherParameter>=<ValueOfTheOtherParameter>"` for more than one parameter
  - please note the ' "" ' for the second approach
