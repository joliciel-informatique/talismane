EPSILON=0.01
C=0.25
CUTOFF=5
FEATURES=none
FEATURECODE=none

CORPUS=none
CORPUSCODE=none

for k in {1..6}
do

CORPUSREADER=spmrl


if [ $k -eq 1 ]
then
	CORPUSCODE=fix_1H
elif [ $k -eq 2 ]
then
	CORPUSCODE=fix_1H+P
elif [ $k -eq 3 ]
then
	CORPUSCODE=fix_CH+P
elif [ $k -eq 4 ]
then
	CORPUSCODE=fix_PH
elif [ $k -eq 5 ]
then
	CORPUSCODE=fix_PH+P
elif [ $k -eq 6 ]
then
	CORPUSCODE=fix_PH2+P
fi

for j in {1..2}
do

if [ $j -eq 1 ]
then
	FEATURECODE=baseline
else
	FEATURECODE=coordB
	if [ $k -eq 3 ]
	then
		FEATURECODE=coord
	elif [ $k -eq 6 ]
	then
		FEATURECODE=coord
	fi
fi

CORPUS=train.French.gold.${CORPUSCODE}.tal

now=$(date)
echo "Parser train time: $now"

echo "Cutoff: $CUTOFF"
echo "C: $C"
echo "Epsilon: $EPSILON"
echo "CORPUS: $CORPUS"
echo "CORPUSCODE: $CORPUSCODE"
echo "FEATURES: $FEATURES"
echo "FEATURECODE: $FEATURECODE"

java -Xmx24G -jar talismane-fr-2.4.2b.jar command=train languagePack=frenchLanguagePack-2.4.1b.zip module=parser parserModel=models/parser_spmrl_train_${CORPUSCODE}_${FEATURECODE}_svm_C${C}_e${EPSILON}_cut${CUTOFF}.zip parserFeatures=features/${FEATURES} inFile=spmrl2013patches/${CORPUS} corpusReader=${CORPUSREADER} inputPatternFile=spmrl2013patches/spmrlInputRegex.txt algorithm=LinearSVM linearSVMEpsilon=${EPSILON} linearSVMCost=${C} cutoff=${CUTOFF} posTagSet=spmrl2013patches/spmrlTagset.txt transitionSystem=ArcEager dependencyLabels=spmrl2013patches/spmrlDependencyLabels.txt logConfigFile=conf/log4j.properties performanceConfigFile=conf/performance.properties

done
done
