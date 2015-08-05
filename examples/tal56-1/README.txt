Scripts and resources for running the experiments described in "Stratégies pour l'étiquetage et l'analyse syntaxique statistique de phénomènes difficiles en français~: études de cas avec Talismane", Revue TAL, 56-1.
Author: Assaf Urieli
Date: August 2015
===============================================================================
The training and evaluation scripts are given the .sh files in the present folder.

In order to run these scripts.
- Install Java v1.7+

- Download Talismane v2.4.2 and place the JAR files in the current folder.
https://github.com/urieli/talismane/releases/tag/v2.4.2b

- Download the various required corpora as described below.

SPMRL:
The SPMRL 2013 French gold files files must be requested from the copyright holders and placed in the "spmrl2013patches" folder.
A series of patches to apply to the files are found in the "spmrl2013patches" sub-folder.

Corpus Sequoia:
Download the corpus at the following link.
https://www.rocq.inria.fr/alpage-wiki/tiki-index.php?page=CorpusSequoia
The present experiments were run on v4.0.
Place the files in the "sequoia" sub-folder.

FrWikiDisc:
Download the files from:
https://github.com/urieli/talismane/tree/master/talismane_core/examples/french/corpus
Place the files in the "frWikiDisc" sub-folder.
