package com.joliciel.talismane.machineLearning.maxent;

import java.io.StringReader;

import com.joliciel.talismane.machineLearning.maxent.custom.TwoPassRealValueDataIndexer;

import junit.framework.TestCase;
import opennlp.maxent.GIS;
import opennlp.maxent.PlainTextByLineDataStream;
import opennlp.maxent.RealBasicEventStream;
import opennlp.model.EventStream;
import opennlp.model.MaxentModel;
import opennlp.model.OnePassRealValueDataIndexer;
import opennlp.model.RealValueFileEventStream;


public class TwoPassRealValueDataIndexerTest extends TestCase {

        /**
         * This test sets out to prove that the scale you use on real valued predicates
         * doesn't matter when it comes the probability assigned to each outcome.
         * Strangely, if we use (1,2) and (10,20) there's no difference.
         * If we use (0.1,0.2) and (10,20) there is a difference.
         * @throws Exception
         */
        public void testDataIndexers() throws Exception {
                String smallValues = "predA=0.1 predB=0.2 A\n" +
                                "predB=0.3 predA=0.1 B\n";
                
                String smallTest = "predA=0.2 predB=0.2";
                
                StringReader smallReader = new StringReader(smallValues);
                EventStream smallEventStream = new RealBasicEventStream(new PlainTextByLineDataStream(smallReader));

                MaxentModel smallModel = GIS.trainModel(100, new OnePassRealValueDataIndexer(smallEventStream,0), false);
                String[] contexts = smallTest.split(" ");
                float[] values = RealValueFileEventStream.parseContexts(contexts);
                double[] smallResults = smallModel.eval(contexts, values);
                
                String smallResultString = smallModel.getAllOutcomes(smallResults);
                System.out.println("smallResults: " + smallResultString);
                
                StringReader smallReaderTwoPass = new StringReader(smallValues);
                EventStream smallEventStreamTwoPass = new RealBasicEventStream(new PlainTextByLineDataStream(smallReaderTwoPass));

                MaxentModel smallModelTwoPass = GIS.trainModel(100, new TwoPassRealValueDataIndexer(smallEventStreamTwoPass,0), false);
                contexts = smallTest.split(" ");
                values = RealValueFileEventStream.parseContexts(contexts);
                double[] smallResultsTwoPass = smallModelTwoPass.eval(contexts, values);
                
                String smallResultTwoPassString = smallModel.getAllOutcomes(smallResults);
                System.out.println("smallResults2: " + smallResultTwoPassString);
                
            assertEquals(smallResults.length, smallResultsTwoPass.length);
            for(int i=0; i<smallResults.length; i++) {
              System.out.println(String.format("classify with smallModel: %1$s = %2$f", smallModel.getOutcome(i), smallResults[i]));
              System.out.println(String.format("classify with smallModelTwoPass: %1$s = %2$f", smallModelTwoPass.getOutcome(i), smallResultsTwoPass[i]));
              assertEquals(smallResults[i], smallResultsTwoPass[i], 0.01f);      
            }

                
        }
}