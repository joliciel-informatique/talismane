Talismane SPMRL 2013 French Treebank patches v1.0
Author: Assaf Urieli
Date: August 2015
-------------------
A set of patches to apply to the SPMRL 2013 French gold files, to fix annotation errors and improve parsing of coordination.

License: you must have a licensed copy of the original French Treebank SPMRL files to apply these patches.
These patches is freely available under the free licence LGPL-LR (Lesser General Public License For Linguistic Resources), which should have been included with this distribution (LGPLLR.txt), and can be downloaded at  http://infolingu.univ-mlv.fr/DonneesLinguistiques/Lexiques-Grammaires/lgpllr.html

To apply the patches on Linux, place the patches in a directory with the following three files:
train.French.gold.conll
test.French.gold.conll
dev.French.gold.conll

Then run the patch files as follows:
patch -o train.French.gold.fix_1H.conll < train.French.gold.fix_1H.conll.patch

The patched files are as follows:
1H: initial corrections to the gold file (including coordination, MWE heads, and "plus de" constructions)
1H+P: 1H + punctuation fix, where all punctuation depends on the previous non-punctuation token
CH+P: all coordination is changed to be conjunction headed, + punctuation fix
PH: all coordination is changed from 1st conjunct headed to previous conjunct headed
PH+P: PH + punctuation fix
PH2+P: all coordination is changed from 1st conjunct headed to previous conjunct headed, and coordinating commas are skipped + punctuation fix

In our experiments, PH2+P gives much better results than all other annotation standards for coordination, for a transition-based parser.

