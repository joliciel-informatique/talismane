frWikiDisc corpus v1.1
-------------------
September 2014

This corpus, annotated for part-of-speech and syntactic dependencies at the CLLE-ERSS laboratory (UMR 5263) within the framework of Assaf Urieli's thesis, under supervision by Ludovic Tanguy, contains French sentences from discussion pages on the French Wikipedia: http://fr.wikipedia.org. The discussion pages were selected for topics with considerable disagreement, and downloaded from the Wikipedia website on Feb 5th, 2013. Sections of the pages citing the original Wikipedia article were removed. Original Wikipedia discussion page content is available under a Creative Commons Attribution-ShareAlike 3.0 Unported License, see http://en.wikipedia.org/wiki/Wikipedia:Text_of_Creative_Commons_Attribution-ShareAlike_3.0_Unported_License.

The corpus is freely available under the free licence LGPL-LR (Lesser General Public License For Linguistic Resources), which should have been included with this distribution (LGPLLR.txt), and can be downloaded at  http://infolingu.univ-mlv.fr/DonneesLinguistiques/Lexiques-Grammaires/lgpllr.html

If you use it, please cite the following paper:

Urieli A., 2013: "Robust French syntax analysis: reconciling statistical methods and linguistic knowledge in the Talismane toolkit", PhD Thesis, Université de Toulouse, France

The total number of sentences is around 300, and each sentence is annotated for part-of-speech and syntactic dependencies, following the guidelines included with your distribution: fr_dep_annotation_v0_8.pdf and based on the French Treebank dependency guide by Candito, Crabbé and Falco (2011) ( http://alpage.inria.fr/statgram/frdep/Publications/FTB-GuideDepSurface.pdf ) itself based on the French Treebank annotation guide ( http://www.llf.cnrs.fr/Gens/Abeille/French-Treebank-fr.php ).

The corpus was automatically analysed by Talismane, and the dependencies were manually corrected using the Brat annotation tool( http://brat.nlplab.org/ ).

Manual corrections were performed on version 0.1 by Assaf Urieli and Marjorie Raufost.
Further corrections were performed by Assaf Urieli.

The corpus is available in a Conll-X dependency format.
Non-projective phenomena in columns 7 and 8 are converted to projective dependencies in columns 9 and 10.
There is a file with extra columns for annotation comments (column 11 = token or pos-tag comments, column 12 = dependency arc comments).

Version history
---------------

V1.0 September 2014
     First public release
V1.1 November 2016
     Various corrections to enable depedency parser training using the corpus (non-projective arcs in projective column, missing depenency labels, etc.)
