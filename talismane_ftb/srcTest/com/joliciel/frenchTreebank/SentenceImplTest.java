/*
 * Created on 14 Jan 2010
 */
package com.joliciel.frenchTreebank;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;

import junit.framework.TestCase;

public class SentenceImplTest extends TestCase {
    private  Mockery mockContext = new Mockery();
    private SentenceImpl sentence;
    private TreebankServiceInternal treebankService;
    final PhraseInternal phrase1 = mockContext.mock(PhraseInternal.class, "phrase1");
    final PhraseInternal phrase2 = mockContext.mock(PhraseInternal.class, "phrase2");
    
    final PhraseTypeInternal phraseTypeNP = mockContext.mock(PhraseTypeInternal.class, "phraseTypeNP");
    final PhraseTypeInternal phraseTypeAP = mockContext.mock(PhraseTypeInternal.class, "phraseTypeAP");
    
    final FunctionInternal functionSUJ = mockContext.mock(FunctionInternal.class, "functionSUJ");

    final PhraseUnitInternal phraseUnit1 = mockContext.mock(PhraseUnitInternal.class, "phraseUnit1");
    final PhraseUnitInternal phraseUnit2 = mockContext.mock(PhraseUnitInternal.class, "phraseUnit2");
    
    final CategoryInternal categoryN = mockContext.mock(CategoryInternal.class, "categoryN");
    final SubCategoryInternal subCategoryN_C = mockContext.mock(SubCategoryInternal.class, "subCategoryN_C");
    final MorphologyInternal morphology_fs = mockContext.mock(MorphologyInternal.class, "morphology_fs");
    final WordInternal lemma_filale = mockContext.mock(WordInternal.class, "lemma_filale");

    final CategoryInternal categoryA = mockContext.mock(CategoryInternal.class, "categoryA");
    final SubCategoryInternal subCategoryA_qual = mockContext.mock(SubCategoryInternal.class, "subCategoryA_qual");
    final WordInternal lemma_espagnol = mockContext.mock(WordInternal.class, "lemma_espagnol");

    protected void setUp() throws Exception {
        super.setUp();
        sentence = new SentenceImpl();
        treebankService = mockContext.mock(TreebankServiceInternal.class);
        sentence.setTreebankServiceInternal(treebankService);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public final void testSaveInternal() {
        this.testClosePhrase();
        final Sequence seq = mockContext.sequence("seq");

        mockContext.checking(new Expectations() {{
            oneOf(treebankService).savePhraseInternal(sentence); inSequence(seq);
            oneOf(treebankService).saveSentenceInternal(sentence); inSequence(seq);
            
            oneOf(phrase1).save(); inSequence(seq);
            oneOf(phraseUnit1).save(); inSequence(seq);
            oneOf(phraseUnit2).save(); inSequence(seq);
            oneOf(phrase1).savePhraseDescendentMapping();  inSequence(seq);
            oneOf(treebankService).savePhraseDescendantMapping(sentence, sentence);  inSequence(seq);
            oneOf(treebankService).savePhraseDescendantMapping(sentence, phrase1);  inSequence(seq);
        }});
        sentence.saveInternal();
    }

    public final void testOpenPhrase() {
        
        mockContext.checking(new Expectations() {{
            oneOf(treebankService).newPhrase(sentence); will (returnValue(phrase1));
            oneOf(treebankService).loadOrCreatePhraseType("NP"); will (returnValue(phraseTypeNP));
            oneOf(treebankService).loadOrCreateFunction("SUJ");  will (returnValue(functionSUJ));
            oneOf(phrase1).setPhraseType(phraseTypeNP);
            oneOf(phrase1).setFunction(functionSUJ);
            oneOf(phrase1).setPositionInPhrase(0);
            oneOf(phrase1).setDepth(1);
            oneOf(phrase1).newChild(); will (returnValue(phrase2));
            oneOf(treebankService).loadOrCreatePhraseType("AP"); will (returnValue(phraseTypeAP));
            oneOf(treebankService).loadOrCreateFunction(null); will (returnValue(null));
            oneOf(phrase2).setPhraseType(phraseTypeAP);
            oneOf(phrase2).setFunction(null);
       }});
         sentence.openPhrase("NP", "SUJ");
         sentence.openPhrase("AP", null);
         
         assertEquals(1, sentence.children.size());
    }

    public final void testClosePhrase() {
        this.testOpenPhrase();
 
        mockContext.checking(new Expectations() {{
            oneOf(treebankService).newPhraseUnit(); will (returnValue(phraseUnit1));
            oneOf(treebankService).loadOrCreateCategory("N"); will (returnValue(categoryN));
            oneOf(treebankService).loadOrCreateSubCategory(categoryN, "C"); will (returnValue(subCategoryN_C));
            oneOf(treebankService).loadOrCreateMorphology("fs"); will (returnValue(morphology_fs));
            oneOf(treebankService).loadOrCreateWord("filiale","filiale"); will (returnValue(lemma_filale));
            oneOf(phraseUnit1).setPositionInSentence(0);
            oneOf(phraseUnit1).setCategory(categoryN);
            oneOf(phraseUnit1).setSubCategory(subCategoryN_C);
            oneOf(phraseUnit1).setMorphology(morphology_fs);
            oneOf(phraseUnit1).setLemma(lemma_filale);
            oneOf (phrase2).addPhraseUnit(phraseUnit1);
            
            
            allowing(phrase2).getParent(); will (returnValue(phrase1));
            
            oneOf(treebankService).newPhraseUnit(); will (returnValue(phraseUnit2));
            oneOf(phraseUnit2).setPositionInSentence(1);
            oneOf(treebankService).loadOrCreateCategory("A"); will (returnValue(categoryA));
            oneOf(treebankService).loadOrCreateSubCategory(categoryA, "qual"); will (returnValue(subCategoryA_qual));
            oneOf(treebankService).loadOrCreateMorphology("fs"); will (returnValue(morphology_fs));
            oneOf(treebankService).loadOrCreateWord("espagnol","espagnol"); will (returnValue(lemma_espagnol));
            oneOf(phraseUnit2).setCategory(categoryA);
            oneOf(phraseUnit2).setSubCategory(subCategoryA_qual);
            oneOf(phraseUnit2).setMorphology(morphology_fs);
            oneOf(phraseUnit2).setLemma(lemma_espagnol);
            oneOf (phrase1).addPhraseUnit(phraseUnit2);

            allowing(phrase1).getParent(); will (returnValue(null));
        }});
        sentence.newPhraseUnit("N", "C", "fs", "filiale");
        sentence.closePhrase();
        sentence.newPhraseUnit("A", "qual", "fs", "espagnol");
        sentence.closePhrase();
    }

    public final void testNewPhraseUnit() {

        mockContext.checking(new Expectations() {{
            oneOf(treebankService).newPhraseUnit(); will (returnValue(phraseUnit1));
            oneOf(treebankService).loadOrCreateCategory("N"); will (returnValue(categoryN));
            oneOf(treebankService).loadOrCreateSubCategory(categoryN, "C"); will (returnValue(subCategoryN_C));
            oneOf(treebankService).loadOrCreateMorphology("fs"); will (returnValue(morphology_fs));
            oneOf(treebankService).loadOrCreateWord("filiale","filiale"); will (returnValue(lemma_filale));
            oneOf(phraseUnit1).setPositionInSentence(0);
            oneOf(phraseUnit1).setPhrase(null);
            oneOf(phraseUnit1).setCategory(categoryN);
            oneOf(phraseUnit1).setSubCategory(subCategoryN_C);
            oneOf(phraseUnit1).setMorphology(morphology_fs);
            oneOf(phraseUnit1).setLemma(lemma_filale);
            oneOf(phraseUnit1).setPositionInPhrase(0);
            
            oneOf(treebankService).newPhraseUnit(); will (returnValue(phraseUnit2));
            oneOf(phraseUnit2).setPositionInSentence(1);
            oneOf(phraseUnit2).setPhrase(null);
            oneOf(treebankService).loadOrCreateCategory("A"); will (returnValue(categoryA));
            oneOf(treebankService).loadOrCreateSubCategory(categoryA, "qual"); will (returnValue(subCategoryA_qual));
            oneOf(treebankService).loadOrCreateMorphology("fs"); will (returnValue(morphology_fs));
            oneOf(treebankService).loadOrCreateWord("espagnol","espagnol"); will (returnValue(lemma_espagnol));
            oneOf(phraseUnit2).setCategory(categoryA);
            oneOf(phraseUnit2).setSubCategory(subCategoryA_qual);
            oneOf(phraseUnit2).setMorphology(morphology_fs);
            oneOf(phraseUnit2).setLemma(lemma_espagnol);
            oneOf(phraseUnit2).setPositionInPhrase(1);

        }});

        sentence.newPhraseUnit("N", "C", "fs", "filiale");
        sentence.newPhraseUnit("A", "qual", "fs", "espagnol");
    }

    public final void testSentenceElementOrdering() {
        final PhraseUnitInternal phraseUnit3 = mockContext.mock(PhraseUnitInternal.class, "phraseUnit3");
        final PhraseUnitInternal phraseUnit4 = mockContext.mock(PhraseUnitInternal.class, "phraseUnit4");
        
        mockContext.checking(new Expectations() {{
            oneOf(treebankService).newPhraseUnit(); will (returnValue(phraseUnit1));
            oneOf(treebankService).loadOrCreateCategory("N"); will (returnValue(categoryN));
            oneOf(treebankService).loadOrCreateSubCategory(categoryN, "C"); will (returnValue(subCategoryN_C));
            oneOf(treebankService).loadOrCreateMorphology("fs"); will (returnValue(morphology_fs));
            oneOf(treebankService).loadOrCreateWord("filiale","filiale"); will (returnValue(lemma_filale));
            oneOf(phraseUnit1).setPositionInSentence(0);
            oneOf(phraseUnit1).setPhrase(null);
            oneOf(phraseUnit1).setCategory(categoryN);
            oneOf(phraseUnit1).setSubCategory(subCategoryN_C);
            oneOf(phraseUnit1).setMorphology(morphology_fs);
            oneOf(phraseUnit1).setLemma(lemma_filale);
            oneOf(phraseUnit1).setPositionInPhrase(0);
            
            oneOf(treebankService).newPhrase(sentence); will (returnValue(phrase1));
            oneOf(treebankService).loadOrCreatePhraseType("NP"); will (returnValue(phraseTypeNP));
            oneOf(treebankService).loadOrCreateFunction("SUJ");  will (returnValue(functionSUJ));
            oneOf(phrase1).setPhraseType(phraseTypeNP);
            oneOf(phrase1).setFunction(functionSUJ);
            oneOf(phrase1).setPositionInPhrase(1);
            oneOf(phrase1).setDepth(1);
            allowing(phrase1).getParent(); will (returnValue(null));
           
            oneOf(treebankService).newPhraseUnit(); will (returnValue(phraseUnit2));
            oneOf(phraseUnit2).setPositionInSentence(1);
            oneOf(phraseUnit2).setPhrase(null);
            oneOf(treebankService).loadOrCreateCategory("A"); will (returnValue(categoryA));
            oneOf(treebankService).loadOrCreateSubCategory(categoryA, "qual"); will (returnValue(subCategoryA_qual));
            oneOf(treebankService).loadOrCreateMorphology("fs"); will (returnValue(morphology_fs));
            oneOf(treebankService).loadOrCreateWord("espagnol","espagnol"); will (returnValue(lemma_espagnol));
            oneOf(phraseUnit2).setCategory(categoryA);
            oneOf(phraseUnit2).setSubCategory(subCategoryA_qual);
            oneOf(phraseUnit2).setMorphology(morphology_fs);
            oneOf(phraseUnit2).setLemma(lemma_espagnol);
            oneOf (phrase1).addPhraseUnit(phraseUnit2);

            oneOf(treebankService).newPhrase(sentence); will (returnValue(phrase2));
            oneOf(treebankService).loadOrCreatePhraseType("AP"); will (returnValue(phraseTypeAP));
            oneOf(treebankService).loadOrCreateFunction(null);  will (returnValue(null));
            oneOf(phrase2).setPhraseType(phraseTypeAP);
            oneOf(phrase2).setFunction(null);
            oneOf(phrase2).setPositionInPhrase(2);
            oneOf(phrase2).setDepth(1);
            allowing(phrase2).getParent(); will (returnValue(null));
            
            oneOf(treebankService).newPhraseUnit(); will (returnValue(phraseUnit3));
            oneOf(phraseUnit3).setPositionInSentence(2);
            oneOf(phraseUnit3).setPhrase(null);
            oneOf(treebankService).loadOrCreateCategory("A"); will (returnValue(categoryA));
            oneOf(treebankService).loadOrCreateSubCategory(categoryA, "qual"); will (returnValue(subCategoryA_qual));
            oneOf(treebankService).loadOrCreateMorphology("fs"); will (returnValue(morphology_fs));
            oneOf(treebankService).loadOrCreateWord("espagnol","espagnol"); will (returnValue(lemma_espagnol));
            oneOf(phraseUnit3).setCategory(categoryA);
            oneOf(phraseUnit3).setSubCategory(subCategoryA_qual);
            oneOf(phraseUnit3).setMorphology(morphology_fs);
            oneOf(phraseUnit3).setLemma(lemma_espagnol);
            oneOf (phrase2).addPhraseUnit(phraseUnit3);
           
            oneOf(treebankService).newPhraseUnit(); will (returnValue(phraseUnit4));
            oneOf(treebankService).loadOrCreateCategory("N"); will (returnValue(categoryN));
            oneOf(treebankService).loadOrCreateSubCategory(categoryN, "C"); will (returnValue(subCategoryN_C));
            oneOf(treebankService).loadOrCreateMorphology("fs"); will (returnValue(morphology_fs));
            oneOf(treebankService).loadOrCreateWord("filiale","filiale"); will (returnValue(lemma_filale));
            oneOf(phraseUnit4).setPositionInSentence(3);
            oneOf(phraseUnit4).setPhrase(null);
            oneOf(phraseUnit4).setCategory(categoryN);
            oneOf(phraseUnit4).setSubCategory(subCategoryN_C);
            oneOf(phraseUnit4).setMorphology(morphology_fs);
            oneOf(phraseUnit4).setLemma(lemma_filale);
            oneOf(phraseUnit4).setPositionInPhrase(3);
        }});
        
        sentence.newPhraseUnit("N", "C", "fs", "filiale");
        sentence.openPhrase("NP", "SUJ");
        sentence.newPhraseUnit("A", "qual", "fs", "espagnol");
        sentence.closePhrase();
        sentence.openPhrase("AP", null);
        sentence.newPhraseUnit("A", "qual", "fs", "espagnol");
        sentence.closePhrase();
        sentence.newPhraseUnit("N", "C", "fs", "filiale");
    }

}
