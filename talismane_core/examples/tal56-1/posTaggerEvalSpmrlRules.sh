PosTaggerModel=none
TagModelType=none
BEAM=1
CORPUS=none
TAGSET=none
CorpusReader=conll
RULES=posTagger_fr_rules_que
EPSILON=0.01
C=0.5
CUTOFF=3

for k in {1..7}
do

if [ $k -eq 1 ]
then
	CORPUS=spmrl2013patches/dev.French.gold.fix_1H.tal
	CORPUSCODE=spmrlDev
	CorpusReader=spmrl
elif [ $k -eq 2 ]
then
	CORPUS=spmrl2013patches/test.French.gold.fix_1H.tal
	CORPUSCODE=spmrlTest
	CorpusReader=spmrl
elif [ $k -eq 3 ]
then
	CORPUS=sequoia/annodis.er.np_conll
	CORPUSCODE=annodis
	CorpusReader=conll
elif [ $k -eq 4 ]
then
	CORPUS=sequoia/emea-fr-dev.np_conll
	CORPUSCODE=emea_dev
	CorpusReader=conll
elif [ $k -eq 5 ]
then
	CORPUS=sequoia/emea-fr-test.np_conll
	CORPUSCODE=emea_test
	CorpusReader=conll
elif [ $k -eq 6 ]
then
	CORPUS=sequoia/Europar.550.np_conll
	CORPUSCODE=europar
	CorpusReader=conll
elif [ $k -eq 7 ]
then
	CORPUS=frWikiDisc/frWikiDisc_v1.0-full.conll
	CORPUSCODE=frWikiDisc
	CorpusReader=conll
fi

for j in {1..2}
do

if [ $j -eq 1 ]
then
	PosTaggerModel=postag_spmrl_train_svm_C${C}_e${EPSILON}_cut${CUTOFF}_base
	TagModelType=Baseline
elif [ $j -eq 2 ]
then
	PosTaggerModel=postag_spmrl_train_svm_C${C}_e${EPSILON}_cut${CUTOFF}_que
	TagModelType=Que
fi


now=$(date)
echo "Eval date: $now"

echo "PosTaggerModel=$PosTaggerModel"
echo "TagModelType=$TagModelType"
echo "CORPUS=$CORPUS"
echo "BEAM=$BEAM"
echo "RULES=$RULES"

java -Xmx16G -jar talismane-fr-2.4.2b.jar languagePack=frenchLanguagePack-2.4.1b.zip command=evaluate startModule=posTagger endModule=posTagger posTaggerModel=models/${PosTaggerModel}.zip inFile=${CORPUS} corpusReader=${CorpusReader} outDir=eval/posTagger/${CORPUSCODE} beamWidth=${BEAM} suffix=_Pos${TagModelType}_QueRules_beam${BEAM} maxParseAnalysisTime=0 predictTransitions=false posTagSet=spmrl2013patches/spmrlTagset.txt posTaggerRules=features/${RULES}.txt logConfigFile=conf/log4j.properties performanceConfigFile=conf/performance.properties

done
done

