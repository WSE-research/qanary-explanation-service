## Explanations evaluation

### TL;DR
As we want to evaluate the quality of generated explanations we have to find a evaluation schema as well as specific measurable metrics to rate the generated explanations.
To respect the nature of explanation, which is the need of being understandable - or a good semantic - we want to include a Crowd-Source metric in the following approach.

### The approach
After evaluating the first examples and the preparation experiments we have found the following, sometimes changing, measurable metrics:

1. Number of Annotations
	- As every explanation starts with the number of annotations this is the easiest one here. We have the decision between an exact evaluation (3/5 equals 60%) or wrong/correct.
	- TODO
2. Explanation's quality
	-  **Quality for each annotation**
		- we want to rate the quality of each annotation for one explanation, therefore we need a macro scale here:
			1. Regarding the provided information (not important how many): 
				- Missing / Added information: -1/0
			2. Regarding the recognition (not important how many):
				- Wrong / Correct value recognition: -1/0
		- This results in a 3-point-scale:
			- Best-Case: 3
			- Worst-Case: 3-1-1=1
		- Attention: The semantics of the information (if something is added, for example) isn't to be rated, as it is not measurable and depends on different factors
	- **Quality for explanation**
		- Here, we can only rate if a "prefix" or "introduction" exists. In some cases ChatGPT adds information at the end. As this isn't part of the *Golden standard* we cannot rate the absence.
			1. Regarding the Prefix:
				- Missing / Existing Prefix: -1/0


	#### Final Quality: $\frac{\Sigma_{1}^{Number~of~Annotations}~~ Quality_{Annotation_n}}{Number~of~Annotations} ~~+~~Quality_{Explanation}$

3. Crowd Sourcing
	- TODO


---
### Open questions
1. Wrong value recognition is always rated with 0 or -1, no matter what. Since some wrong values may not be as impactful for a explanation like others I asked myself if that should be respected? On the other hand changing that, the generalization would break (Some say it's import, some say it's not // Different Types means different values, means we have to list all possible values -> Generalization?)

2. About crowd-sourcing: How many (all is obv. not possible) examples can be served? 
	- some of all cases, like: Nearly perfect examples, medium perfect examples, bad examples

### Offene Fragen
1. Wrong value recognition ist aktuell immer mit 0 oder -1 bewertet, egal welches value es betrifft. Da allerdings einige für die Erklärung "wichtiger" als andere erscheinen/sein können, frage ich mich, ob der Umstand berücksichtigt werden sollte? Andererseits würde eine Änderung dahingehend dafür sorgen, dass eine Art Generalisierung verloren geht, da die Wichtigkeit für verschiedene Personen unterschiedlich sein kann und außerdem alle möglich Attribute aufgelistet und gewertet werden müssten (Aufwand/Nutzen?)

2. Wegen des Crowd-Sourcing: Wie viele Beispiele kann bzw. sollte man bereitstellen (alle ist logischerweise utopisch)? 
	- Ich dachte, dass "perfekte" Beispiele lediglich als Vergleich dienen sollten und dann
		- x gute, aber mit Fehlern / schlechte 
		- bzw. x Beispiele mit Gesamt-Qualität 4/3/2/1
