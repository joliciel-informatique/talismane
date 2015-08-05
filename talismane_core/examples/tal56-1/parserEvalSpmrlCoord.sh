EPSILON=0.01
C=0.25
CUTOFF=5

TESTCORPUS=test

for l in {1..2}
do

if [ $l -eq 1 ]
then
	TESTCORPUS=test
elif [ $l -eq 2 ]
then
	TESTCORPUS=dev
fi

BEAM=1

for i in {1..3}
do

if [ $i -eq 1 ]
then
	BEAM=1
elif [ $i -eq 2 ]
then
	BEAM=2
elif [ $i -eq 3 ]
then
	BEAM=5
fi


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

CORPUS=${TESTCORPUS}.French.gold.${CORPUSCODE}.tal

now=$(date)
echo "Eval date: $now"

echo "CORPUSCODE: $CORPUSCODE"
echo "TESTCORPUS: $TESTCORPUS"
echo "SUFFIX: $SUFFIX"
echo "FEATURECODE: $FEATURECODE"
echo "CORPUS: $CORPUS"
echo "CORPUSREADER=$CORPUSREADER"
echo "BEAM=$BEAM"

java -Xmx24G -jar talismane-fr-2.4.2b.jar languagePack=frenchLanguagePack-2.4.1b.zip command=evaluate startModule=parser endModule=parser parserModel=models/parser_spmrl_train_${CORPUSCODE}_${FEATURECODE}_svm_C${C}_e${EPSILON}_cut${CUTOFF}.zip  inFile=spmrl2013patches/${CORPUS} corpusReader=${CORPUSREADER} outDir=eval/parser/${TESTCORPUS} beamWidth=${BEAM} suffix=_${CORPUSCODE}_${FEATURECODE}_beam${BEAM} maxParseAnalysisTime=0 predictTransitions=false posTagSet=spmrl2013patches/spmrlTagset.txt dependencyLabels=spmrl2013patches/spmrlDependencyLabels.txt logConfigFile=conf/log4j.properties performanceConfigFile=conf/performance.properties includeTimePerToken=true

done
done
done
done

