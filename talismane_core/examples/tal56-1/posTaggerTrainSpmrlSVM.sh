EPSILON=0.01
C=0.5
CUTOFF=3

now=$(date)
echo "Train time: $now"

echo "Cutoff: $CUTOFF"
echo "C: $C"
echo "Epsilon: $EPSILON"

java -Xmx16G -jar talismane-fr-2.4.2b.jar languagePack=frenchLanguagePack-2.4.1b.zip module=posTagger command=train posTaggerModel=models/postag_spmrl_train_svm_C${C}_e${EPSILON}_cut${CUTOFF}_base.zip posTaggerFeatures=features/posTagger_fr_baseline_features.txt algorithm=LinearSVM linearSVMEpsilon=${EPSILON} linearSVMCost=${C} cutoff=${CUTOFF} inFile=spmrl2013patches/train.French.gold.fix_1H.tal corpusReader=spmrl inputPatternFile=spmrl2013patches/spmrlInputRegex.txt posTagSet=spmrl2013patches/spmrlTagset.txt logConfigFile=conf/log4j.properties performanceConfigFile=conf/performance.properties