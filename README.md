![Talismane Logo](https://raw.githubusercontent.com/wiki/joliciel-informatique/talismane/pics/TalismaneLogo300px.png)

[![Build Status](https://travis-ci.org/joliciel-informatique/talismane.png?branch=master)](https://travis-ci.org/joliciel-informatique/talismane)

Talismane is a natural language processing framework with sentence detector, tokeniser, pos-tagger and dependency syntax parser. Current available language packs include French (standard and [Universal Dependencies](https://github.com/joliciel-informatique/talismane/wiki/Analysing-French-Universal-Dependencies)) and English.

**Sample input:**
> Les amoureux qui se bécotent sur les bancs publics ont des petites gueules bien sympathiques.

**Sample output:** a syntax tree, shown below in CoNLL-X format, also available as a Java object for manipulation in code.
<pre>
1	Les	les	DET	DET	n=p|	2	det	2	det
2	amoureux	amoureux	NC	NC	g=m|	10	suj	10	suj
3	qui	qui	PROREL	PROREL	n=s|	5	suj	5	suj
4	se	se	CLR	CLR	n=p|p=3|	5	aff	5	aff
5	bécotent	bécoter	V	V	n=p|t=PS|p=3|	2	mod_rel	2	mod_rel
6	sur	sur	P	P		5	mod	5	mod
7	les	les	DET	DET	n=p|	8	det	8	det
8	bancs	banc	NC	NC	n=p|g=m|	6	prep	6	prep
9	publics	public	ADJ	ADJ	n=p|g=m|	8	mod	8	mod
10	ont	avoir	V	V	n=p|t=P|p=3|	0	root	0	root
11	des	des	DET	DET	n=p|	13	det	13	det
12	petites	petit	ADJ	ADJ	n=p|g=f|	13	mod	13	mod
13	gueules	gueule	NC	NC	n=p|	10	obj	10	obj
14	bien	bien	ADV	ADV		15	mod	15	mod
15	sympathiques	sympathique	ADJ	ADJ	n=p|	13	mod	13	mod
16	.	.	PONCT	PONCT		15	ponct	15	ponct
</pre>

**Downloads**: The latest release and language packs can be downloaded on the [releases pages](https://github.com/joliciel-informatique/talismane/releases).

**Wiki**: Simple instructions for use can be found on the [Talismane wiki](https://github.com/joliciel-informatique/talismane/wiki).

**Command-line usage**: follow the [setup instructions](https://github.com/joliciel-informatique/talismane/wiki/Setup), and then run a command similar to the following:
<pre>java -Xmx1G -Dconfig.file=talismane-fr-X.X.X.conf -jar talismane-core-X.X.X.jar --analyse --sessionId=fr --encoding=UTF8 --inFile=data/frTest.txt --outFile=data/frTest.tal</pre>

**Calling from Java**: For syntax analysis within Java code via the API, see this [Java code example](https://github.com/joliciel-informatique/talismane/blob/master/talismane_examples/src/main/java/com/joliciel/talismane/examples/TalismaneAPIExamples.java).

**JavaDoc API**: You may also consult the [full JavaDoc API](http://joliciel-informatique.github.io/talismane/api/) online.

**User's manual**: An out-of-date users's manual can be found on the [GitHub Talismane project page](http://joliciel-informatique.github.io/talismane/). For up-to-date documentation, you're far better off consulting the [ wiki](https://github.com/joliciel-informatique/talismane/wiki) or the [JavaDoc API](http://joliciel-informatique.github.io/talismane/api/) .           

**Additional information** on the project can be found on the [CLLE-ERSS laboratory Talismane project home page](http://redac.univ-tlse2.fr/applications/talismane.html).

**Language pack usage**

* The **French language pack** can be used for research purposes provided that you have a license for the French Treebank.
The model included is not optimised as it uses a Maximum Entropy model (which only requires about 1G of RAM) rather than a Linear SVM model (which requires about 24G RAM).
If you would like the more optimised Linear SVM model, please contact [Assaf Urieli](mailto:assaf.urieli@gmail.com "Assaf Urieli").

* The **English language pack** can be used for research purposes provided that you have a license for the Penn Treebank.
WARNING: the English model is only an initial version, with no attempts at optimisation.
