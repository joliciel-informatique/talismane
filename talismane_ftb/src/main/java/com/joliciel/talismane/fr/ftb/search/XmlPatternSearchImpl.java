///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.fr.ftb.search;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.joliciel.talismane.fr.ftb.Category;
import com.joliciel.talismane.fr.ftb.Entity;
import com.joliciel.talismane.fr.ftb.Function;
import com.joliciel.talismane.fr.ftb.Phrase;
import com.joliciel.talismane.fr.ftb.PhraseType;
import com.joliciel.talismane.fr.ftb.PhraseUnit;
import com.joliciel.talismane.fr.ftb.Sentence;
import com.joliciel.talismane.fr.ftb.SubCategory;
import com.joliciel.talismane.fr.ftb.TreebankException;
import com.joliciel.talismane.fr.ftb.TreebankService;
import com.joliciel.talismane.fr.ftb.Word;
import com.joliciel.talismane.fr.ftb.util.UnicodeReader;
import com.joliciel.talismane.utils.LogUtils;

public class XmlPatternSearchImpl implements XmlPatternSearch {
    private static final Logger LOG = LoggerFactory.getLogger(XmlPatternSearchImpl.class);

    String xmlPattern = "";
    TreebankService treebankService;
    private static final String NULL_STRING = "[null]";
    int phraseCounter = 0;
    int phraseUnitCounter = 0;
    int phraseSubunitCounter = 0;

    public TreebankService getTreebankService() {
        return treebankService;
    }

    public void setTreebankService(TreebankService treebankService) {
        this.treebankService = treebankService;
    }

    public List<SearchResult> perform() {
        DOMParser parser = new DOMParser();
        StringReader reader = new StringReader(this.xmlPattern);
        InputSource inputSource = new InputSource(reader);
        try {
            parser.parse(inputSource);
        } catch (SAXException e) {
            LogUtils.logError(LOG, e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            LogUtils.logError(LOG, e);
            throw new RuntimeException(e);
        }
        
        Document document = parser.getDocument();
        Element firstPhraseTag = document.getDocumentElement();
        PhraseNode phraseNode = (PhraseNode) this.traverse(firstPhraseTag, 0);
        
        List<String> tablesToReturn = new ArrayList<String>();
        List<String> tables = new ArrayList<String>();
        List<String> conditions = new ArrayList<String>();
        List<String> orderBy = new ArrayList<String>();

        this.getSQLElements(phraseNode, tablesToReturn, tables, conditions, orderBy);
        List<List<Entity>> stuff = this.treebankService.findStuff(tablesToReturn, tables, conditions, orderBy);
        
        List<SearchResult> searchResults = new ArrayList<SearchResult>();
        for (List<Entity> oneRow : stuff) {
            SearchResultImpl searchResult = new SearchResultImpl();
            for (Entity entity : oneRow) {
                if (entity instanceof Sentence)
                    searchResult.setSentence((Sentence) entity);
                else if (entity instanceof Phrase)
                    searchResult.setPhrase((Phrase) entity);
                else if (entity instanceof PhraseUnit)
                    searchResult.getPhraseUnits().add((PhraseUnit)entity);
            }
            for (Entity entity : oneRow) {
                if (entity instanceof Word) {
                    Word lemma = (Word) entity;
                    for (PhraseUnit punit : searchResult.getPhraseUnits()) {
                        if (punit.getLemmaId() == lemma.getId()) {
                            punit.setLemma(lemma);
                        }
                    }
                }
                
            }
            searchResults.add(searchResult);
        }
        
        return searchResults;
    }
    
    
    private void getSQLElements(PhraseMemberNode node, List<String> tablesToReturn, List<String> tables, List<String> conditions, List<String> orderBy) {
        List<String> localConditions = conditions;
        List<String> localTables = tables;
        List<String> localTablesToReturn = tablesToReturn;
        List<String> localOrderBy = orderBy;
        
        // if the node doesn't exist, we'll need to put all of its conditions into a NOT EXISTS clause
        // hence we store them in a separate List
        if (!node.exists()) {
            localConditions = new ArrayList<String>();
            localTables = new ArrayList<String>();
            localTablesToReturn = new ArrayList<String>();
            localOrderBy = new ArrayList<String>();
        }
        
        if (node instanceof PhraseNode) {
            PhraseNode phraseNode = (PhraseNode) node;
            localTables.add("ftb_phrase as " + phraseNode.getAlias());

            
            if (phraseNode.getParent()!=null) {
                localConditions.add(phraseNode.getAlias() + ".phrase_parent_id = " + phraseNode.getParent().getAlias() + ".phrase_id");
            } else {
                // this is the top-level phrase node
                if (!phraseNode.exists)
                    throw new TreebankException("The top-level phrase has to exist, otherwise what are searching for?");
                
                // want to return the parent phrase englobing all the other ones
                localTablesToReturn.add("ftb_phrase as " +  phraseNode.getAlias());
                
                // include sentence englobing this phrase
                String sentenceAlias = "s";
                localTables.add("ftb_sentence as " + sentenceAlias);
    
                String sentencePhraseAlias = "sp";
                localTables.add("ftb_phrase as " + sentencePhraseAlias);
                
                String phraseChildAlias = "pc";
                localTables.add("ftb_phrase_child as " + phraseChildAlias);
                
                localConditions.add(sentenceAlias + ".sentence_id = " + phraseChildAlias + ".pchild_phrase_id");
                localConditions.add(phraseNode.getAlias() + ".phrase_id = " + phraseChildAlias + ".pchild_child_id");
                localConditions.add(sentencePhraseAlias + ".phrase_id =" + sentenceAlias + ".sentence_id");
                localConditions.add(sentencePhraseAlias + ".phrase_id = " + phraseChildAlias + ".pchild_phrase_id");
                localTablesToReturn.add("ftb_sentence as " + sentenceAlias);
                localTablesToReturn.add("ftb_phrase as " + sentencePhraseAlias);
                
                localOrderBy.add(sentenceAlias + "_sentence_file_id");
                localOrderBy.add(sentenceAlias + "_sentence_id");
            }
            
            this.addConditions(phraseNode, localConditions);

            for (PhraseMemberNode child : phraseNode.getChildNodes()) {
                this.getSQLElements(child, localTablesToReturn, localTables, localConditions, localOrderBy);
            } // next child node
            // Phrase node
        } else {
            // Word node
            WordNode wordNode = (WordNode) node;
            String phraseUnitAlias = wordNode.getAlias();
            localTables.add("ftb_phrase_unit as " + phraseUnitAlias);
            boolean wordNodeExists = wordNode.exists();
            PhraseNode parent = wordNode.getParent();
            while (parent!=null) {
                if (!parent.exists()) {
                    wordNodeExists = false;
                    break;
                }
                parent = parent.getParent();
            }
            if (wordNodeExists) {
                // want to return the phrase units
                localTablesToReturn.add("ftb_phrase_unit as " + phraseUnitAlias);
                
                // add the lemma
                String lemmaAlias = "w" + wordNode.getNodeIndex();
                localTables.add("ftb_word as " + lemmaAlias);
                localTablesToReturn.add("ftb_word as " + lemmaAlias);
                
                localConditions.add(lemmaAlias + ".word_id = " + phraseUnitAlias + ".punit_lemma_id");
           }
            localConditions.add(phraseUnitAlias + ".punit_phrase_id = " +  wordNode.getParent().getAlias() + ".phrase_id");
                             
            this.addConditions(wordNode, localConditions);
            
            // Let's see if there are any sub-words
            if (wordNode.getChildNodes().size()>0) {
                for (ComponentWordNode subWordNode : wordNode.getChildNodes()) {
                    localTables.add("ftb_phrase_subunit as " + subWordNode.getAlias());
                    localConditions.add(subWordNode.getAlias() + ".psubunit_punit_id = " + wordNode.getAlias() + ".punit_id");
                    
                    this.addConditions(subWordNode, localConditions);
                }
                
                // ordering of subwords
                ComponentWordNode previousNode = null;
                for (ComponentWordNode subWordNode : wordNode.getChildNodes()) {
                    if (previousNode!=null)
                        localConditions.add(previousNode.getAlias() + ".psubunit_position < " + subWordNode.getAlias() + ".psubunit_position");
                    previousNode = subWordNode;
                }
            }
            // Word node
        }
        
        if (node.getParent()!=null) {
            // Ordering condition
            PhraseMemberNode previousNode = null;
            PhraseMemberNode nextNode = null;
            boolean foundCurrent = false;
            for (PhraseMemberNode child : node.getParent().getChildNodes()) {
                if (child.equals(node))
                    foundCurrent = true;
                else if (child.exists()) {
                    if (foundCurrent && nextNode==null) {
                        nextNode = child;
                        break;
                    }
                    if (!foundCurrent)
                        previousNode = child;
                }
            }
            String myPositionColumn = ".phrase_position";
            if (node instanceof WordNode)
                myPositionColumn = ".punit_pos_in_phrase";
            
            if (previousNode!=null) {
                if (previousNode instanceof PhraseNode) {
                    localConditions.add(previousNode.getAlias() + ".phrase_position < " + node.getAlias() + myPositionColumn);
                } else {
                    localConditions.add(previousNode.getAlias() + ".punit_pos_in_phrase < " + node.getAlias() + myPositionColumn);
                }   
            }
            if (!node.exists()) {
                // in this case, we also have to add a condition concerning the next node
                if (nextNode != null) {
                    if (nextNode instanceof PhraseNode) {
                        localConditions.add(node.getAlias()  + myPositionColumn + " < " + nextNode.getAlias() + ".phrase_position");
                    } else {
                        localConditions.add(node.getAlias() + myPositionColumn + " < " + nextNode.getAlias() + ".punit_pos_in_phrase");
                    }
                }
            }
            
            // if the node doesn't exist, add the non existence condition, based on all of the local conditions
            if (!node.exists()) {
                String nonExistenceCondition;
                if (node instanceof PhraseNode)
                    nonExistenceCondition = " NOT EXISTS (SELECT " + node.getAlias() + ".phrase_id";
                else
                    nonExistenceCondition = " NOT EXISTS (SELECT " + node.getAlias() + ".punit_id";
                boolean firstOne = true;
                for (String table : localTables) {
                    if (firstOne) {
                        nonExistenceCondition += " FROM " + table;
                        firstOne = false;
                    } else {
                        nonExistenceCondition += ", " + table;
                    }
                }
                firstOne = true;
                for (String condition : localConditions) {
                    if (firstOne) {
                        nonExistenceCondition += " WHERE " + condition;
                        firstOne = false;
                    } else {
                        nonExistenceCondition += " AND " + condition;
                    }
                }
                nonExistenceCondition += ")";
                conditions.add(nonExistenceCondition);
            }
        }
    }
    
    private void addConditions(PhraseNode phraseNode, List<String> conditions) {
        // add the phrase type condition
        if (phraseNode.getTypeCodes().size()>0) {
            String condition = phraseNode.getAlias() + ".phrase_ptype_id ";
            List<PhraseType> phraseTypes = new ArrayList<PhraseType>();
            for (String phraseTypeCode : phraseNode.getTypeCodes()) {
                PhraseType phraseType = this.treebankService.loadPhraseType(phraseTypeCode);
                phraseTypes.add(phraseType);
            }
            if (phraseTypes.size()==1)
                condition += " = " + phraseTypes.get(0).getId();
            else {
                condition += " IN (";
                boolean firstOne = true;
                for (PhraseType phraseType : phraseTypes) {
                    if (firstOne) {
                        condition += phraseType.getId();
                        firstOne = false;
                    }   else {
                        condition += ", " + phraseType.getId();
                    }
                }
                condition += ")";
            }
            conditions.add(condition);
        } 
        
        // add the function condition
        if (phraseNode.getFunctionCodes().size()>0) {
            String condition = phraseNode.getAlias() + ".phrase_function_id ";
            List<Function> functions = new ArrayList<Function>();
            for (String functionCode : phraseNode.getFunctionCodes()) {
                Function function = this.treebankService.loadFunction(functionCode);
                functions.add(function);
            }
            if (functions.size()==1)
                condition += " = " + functions.get(0).getId();
            else {
                condition += " IN (";
                boolean firstOne = true;
                for (Function function : functions) {
                    if (firstOne) {
                        condition += function.getId();
                        firstOne = false;
                    }   else {
                        condition += ", " + function.getId();
                    }
                }
                condition += ")";
            }
            conditions.add(condition);
        }        
    }
    
    private void addConditions(WordNode wordNode, List<String> wordConditions) {
        // add the category condition
        if (wordNode.getCategoryCodes().size()>0) {
            String condition = wordNode.getAlias() + ".punit_cat_id ";
            List<Category> categories = new ArrayList<Category>();
            for (String categoryCode : wordNode.getCategoryCodes()) {
                Category category = this.treebankService.loadCategory(categoryCode);
                categories.add(category);
            }
            if (categories.size()==1)
                condition += " = " + categories.get(0).getId();
            else {
                condition += " IN (";
                boolean firstOne = true;
                for (Category category : categories) {
                    if (firstOne) {
                        condition += category.getId();
                        firstOne = false;
                    }   else {
                        condition += ", " + category.getId();
                    }
                }
                condition += ")";
            }
            wordConditions.add(condition);
        }
        
        // add the subCategory condition
        if (wordNode.getSubCategoryCodes().size()>0) {
            if (wordNode.getCategoryCodes().size()!=1)
                throw new TreebankException("Subcategories can only be used if exactly one category has been specified.");
            Category category = this.treebankService.loadCategory(wordNode.getCategoryCodes().get(0));
            String condition = wordNode.getAlias() + ".punit_subcat_id ";
            if (wordNode.getSubCategoryCodes().size()==1 && wordNode.getSubCategoryCodes().get(0).equals(NULL_STRING))
                    condition += " is null";
            else {
                List<SubCategory> subCategories = new ArrayList<SubCategory>();
                for (String subCategoryText : wordNode.getSubCategoryCodes()) {
                    SubCategory subCategory = this.treebankService.loadSubCategory(category, subCategoryText);
                    subCategories.add(subCategory);
                }
                if (subCategories.size()==1) {
                    condition += " = " + subCategories.get(0).getId();
                } else {
                    condition += " IN (";
                    boolean firstOne = true;
                    for (SubCategory subCategory : subCategories) {
                        if (firstOne) {
                            condition += subCategory.getId();
                            firstOne = false;
                        }   else {
                            condition += ", " + subCategory.getId();
                        }
                    }
                    condition += ")";
                }
            }
            wordConditions.add(condition);
        }
        
        // add the lemma condition
        if (wordNode.getLemmas().size()>0) {
            String condition = wordNode.getAlias() + ".punit_lemma_id ";
            List<Word> lemmas = new ArrayList<Word>();
            for (String lemmaText : wordNode.getLemmas()) {
            	List<Word> lemmasForText = this.treebankService.findWords(lemmaText);
            	lemmas.addAll(lemmasForText);
            }
            if (lemmas.size()==1)
                condition += " = " + lemmas.get(0).getId();
            else {
                condition += " IN (";
                boolean firstOne = true;
                for (Word lemma : lemmas) {
                    if (firstOne) {
                        condition += lemma.getId();
                        firstOne = false;
                    }   else {
                        condition += ", " + lemma.getId();
                    }
                }
                condition += ")";
            }
            wordConditions.add(condition);
        }
        
        // add the word condition
        if (wordNode.getWords().size()>0) {
            String condition = wordNode.getAlias() + ".punit_word_id ";
            List<Word> words = new ArrayList<Word>();
            for (String wordText : wordNode.getWords()) {
            	List<Word> wordsForText = this.treebankService.findWords(wordText);
            	words.addAll(wordsForText);
            }
            if (words.size()==1)
                condition += " = " + words.get(0).getId();
            else {
                condition += " IN (";
                boolean firstOne = true;
                for (Word word : words) {
                    if (firstOne) {
                        condition += word.getId();
                        firstOne = false;
                    }   else {
                        condition += ", " + word.getId();
                    }
                }
                condition += ")";
            }
            wordConditions.add(condition);
        }        
    }
    
    private void addConditions(ComponentWordNode subWordNode, List<String> wordConditions) {
        // add the category condition
        if (subWordNode.getCategoryCodes().size()>0) {
            String condition = subWordNode.getAlias() + ".psubunit_cat_id ";
            if (subWordNode.getCategoryCodes().size()==1 && subWordNode.getCategoryCodes().get(0).equals(NULL_STRING))
                    condition += " is null";
            else {
                List<Category> categories = new ArrayList<Category>();
                for (String categoryCode : subWordNode.getCategoryCodes()) {
                    Category category = this.treebankService.loadCategory(categoryCode);
                    categories.add(category);
                }
                if (categories.size()==1)
                    condition += " = " + categories.get(0).getId();
                else {
                    condition += " IN (";
                    boolean firstOne = true;
                    for (Category category : categories) {
                        if (firstOne) {
                            condition += category.getId();
                            firstOne = false;
                        }   else {
                            condition += ", " + category.getId();
                        }
                    }
                    condition += ")";
                }
            }
            wordConditions.add(condition);
        }
        
        // add the word condition
        if (subWordNode.getWords().size()>0) {
            String condition = subWordNode.getAlias() + ".psubunit_word_id ";
            List<Word> words = new ArrayList<Word>();
            for (String wordText : subWordNode.getWords()) {
            	List<Word> wordsForText = this.treebankService.findWords(wordText);
            	words.addAll(wordsForText);
            }
            if (words.size()==1)
                condition += " = " + words.get(0).getId();
            else {
                condition += " IN (";
                boolean firstOne = true;
                for (Word word : words) {
                    if (firstOne) {
                        condition += word.getId();
                        firstOne = false;
                    }   else {
                        condition += ", " + word.getId();
                    }
                }
                condition += ")";
            }
            wordConditions.add(condition);
        }        
    }
    
    private PhraseMemberNode traverse(Node node, int depth) {
         {
            LOG.debug(depth + " " +node.getNodeName());
            PhraseMemberNode xmlPatternNode = null;
            Element element = (Element) node;
            String tag = node.getNodeName();
            if (tag=="phrase") {
                String phraseTypes = element.getAttribute("type");
                String functionCodes = element.getAttribute("fct");
                String existsString = element.getAttribute("exists");
                boolean exists = existsString==null||!(existsString.equals("no"));
                LOG.debug("Phrase type: " + phraseTypes + ", fct = " + functionCodes + ", exists = " + existsString);
                xmlPatternNode = new PhraseNode(phraseTypes, functionCodes, exists, phraseCounter++);
            } else if (tag=="w") {
                String categoryCode = element.getAttribute("cat");
                String subCategoryCode = element.getAttribute("subcat");
                String lemma = element.getAttribute("lemma");
                String word = element.getTextContent();
                String existsString = element.getAttribute("exists");
                boolean exists = existsString==null||!(existsString.equals("no"));
                LOG.debug("w: cat=" + categoryCode + ", subcat = " + subCategoryCode + ", lemma=" + lemma + ", word=" + word + ", exists = " + existsString);
                xmlPatternNode = new WordNode(categoryCode, subCategoryCode, lemma, word, exists, phraseUnitCounter++);
            } else {
                String phraseTypes = node.getNodeName();
                String functionCodes = element.getAttribute("fct");
                String existsString = element.getAttribute("exists");
                boolean exists = existsString==null||!(existsString.equals("no"));
                LOG.debug("Phrase type: " + phraseTypes + ", fct = " + functionCodes + ", exists = " + existsString);
                xmlPatternNode = new PhraseNode(phraseTypes, functionCodes, exists, phraseCounter++);
             }

            if (xmlPatternNode instanceof PhraseNode) {
                if (node.hasChildNodes()) {
                    Node child = node.getFirstChild();
                    while (child!=null) {
                        if (child.getNodeType()==Node.ELEMENT_NODE) {
                            PhraseNode phraseNode = (PhraseNode) xmlPatternNode;
                            PhraseMemberNode childNode = this.traverse(child, depth+1);
                            phraseNode.addNode(childNode);
                            childNode.setParent(phraseNode);
                        }
                        child = child.getNextSibling();
                    }
                }
            } else if (xmlPatternNode instanceof WordNode) {
                if (node.hasChildNodes()) {
                    Node child = node.getFirstChild();
                    while (child!=null) {
                        if (child.getNodeType()==Node.ELEMENT_NODE) {
                            WordNode wordNode = (WordNode) xmlPatternNode;
                            ComponentWordNode childNode = this.getComponentWordNode(child);
                            wordNode.addNode(childNode);
                            childNode.setParent(wordNode);
                        }
                        child = child.getNextSibling();
                    }
                }
            }
            return xmlPatternNode;
        }
    }
    
    private ComponentWordNode getComponentWordNode(Node node) {
        Element element = (Element) node;
        String tag = node.getNodeName();
        if (tag!="w")
            throw new TreebankException("Only w elements are allowed beneath a w element");
        String categoryCode = element.getAttribute("cat");
        String word = element.getTextContent();
        LOG.debug("component w: cat=" + categoryCode + ", word=" + word);
        ComponentWordNode componentWordNode = new ComponentWordNode(categoryCode, word, phraseSubunitCounter++);
        return componentWordNode;
    }

    public String getXmlPattern() {
        return xmlPattern;
    }

    public void setXmlPattern(String xmlPattern) {
        this.xmlPattern = xmlPattern;
    }

    private  interface XmlPatternNode {
        public String getAlias();
        public int getNodeIndex();
   }
    
    private interface PhraseMemberNode extends XmlPatternNode {
        public PhraseNode getParent();
        public void setParent(PhraseNode parent);        
        public boolean exists() ;
    }
    
    private static final class PhraseNode implements PhraseMemberNode {
        private List<String> typeCodes = new ArrayList<String>();
        private  List<String> functionCodes = new ArrayList<String>();
        private List<PhraseMemberNode> childNodes = new ArrayList<PhraseMemberNode>();
        private PhraseNode parent = null;
        private String alias = "";
        private int nodeIndex;
        private boolean exists = true;
        
        public PhraseNode(String typeString, String functionString, boolean exists, int nodeIndex) {
            StringTokenizer st = new StringTokenizer(typeString, ",", false);
            while (st.hasMoreTokens())
                typeCodes.add(st.nextToken().trim());
            
            st = new StringTokenizer(functionString, ",", false);
            while (st.hasMoreTokens())
                functionCodes.add(st.nextToken().trim());
            
            this.exists = exists;
            this.alias = "p" + nodeIndex;
            this.nodeIndex = nodeIndex;
        }
        
        public List<String> getTypeCodes() { return typeCodes; }
        
        public List<String> getFunctionCodes() {
            return functionCodes;
        }

        public boolean exists() { return exists; }

        public void addNode(PhraseMemberNode node) { childNodes.add(node); }
        
        public List<PhraseMemberNode> getChildNodes() { return this.childNodes; }

        public PhraseNode getParent() {
            return parent;
        }


        public void setParent(PhraseNode parent) {
            this.parent = parent;
        }

        public String getAlias() {
            return alias;
        }

        public int getNodeIndex() {
            return nodeIndex;
        }
        
    }
    
    private static final class WordNode implements PhraseMemberNode {
        private List<String> subCategoryCodes = new ArrayList<String>();
        private List<String> categoryCodes = new ArrayList<String>();
        private List<String> lemmas = new ArrayList<String>();
        private List<String> words = new ArrayList<String>();
        private List<ComponentWordNode> childNodes = new ArrayList<ComponentWordNode>();
        private  PhraseNode parent = null;
        private String alias = "";
        private int nodeIndex;
        private boolean exists = true;
        
        public WordNode(String categoryString, String subCategoryString, String lemmaString, String wordString, boolean exists, int nodeIndex) {
            StringTokenizer st = new StringTokenizer(categoryString, ",", false);
            while (st.hasMoreTokens())
                categoryCodes.add(st.nextToken().trim());
            st = new StringTokenizer(subCategoryString, ",", false);
            while (st.hasMoreTokens())
                subCategoryCodes.add(st.nextToken().trim());
            st = new StringTokenizer(lemmaString, ",", false);
            while (st.hasMoreTokens())
                lemmas.add(st.nextToken().trim());
            st = new StringTokenizer(wordString, ",", false);
            while (st.hasMoreTokens())
                words.add(st.nextToken().trim());
            
            this.exists = exists;
            this.alias = "pu" + nodeIndex;
            this.nodeIndex = nodeIndex;
        } 
        
        public List<String> getCategoryCodes() { return categoryCodes; }
        public List<String> getSubCategoryCodes() { return subCategoryCodes; }
        public List<String> getLemmas() { return lemmas; }
        public List<String> getWords() { return words; }
        public boolean exists() { return exists; }

        public PhraseNode getParent() {
            return parent;
        }

        public void setParent(PhraseNode parent) {
            this.parent = parent;
        }

        public String getAlias() {
            return alias;
        }
        public int getNodeIndex() {
            return nodeIndex;
        }
        public void addNode(ComponentWordNode node) { childNodes.add(node); }
        
        public List<ComponentWordNode> getChildNodes() { return this.childNodes; }

    }
    
    private static final class ComponentWordNode implements XmlPatternNode {
        private List<String> categoryCodes = new ArrayList<String>();
        private List<String> words = new ArrayList<String>();
        private  WordNode parent = null;
        private String alias = "";
        private int nodeIndex;
        
        public ComponentWordNode(String categoryString, String wordString,  int nodeIndex) {
            StringTokenizer st = new StringTokenizer(categoryString, ",", false);
            while (st.hasMoreTokens())
                categoryCodes.add(st.nextToken().trim());
            st = new StringTokenizer(wordString, ",", false);
            while (st.hasMoreTokens()){
                String word = st.nextToken().trim();
                if (word.length()>0)
                    words.add(word);
            }
            this.alias = "psu" + nodeIndex;
            this.nodeIndex = nodeIndex;
        } 
        
        public List<String> getCategoryCodes() { return categoryCodes; }
        public List<String> getWords() { return words; }

        @SuppressWarnings("unused")
		public WordNode getParent() {
            return parent;
        }

        public void setParent(WordNode parent) {
            this.parent = parent;
        }

        public String getAlias() {
            return alias;
        }
        public int getNodeIndex() {
            return nodeIndex;
        }

    }

    public void setXmlPatternFile(String xmlPatternFile) {
        try {
            StringBuffer fileData = new StringBuffer(1000);
            Reader reader = new UnicodeReader(new FileInputStream(xmlPatternFile),"UTF-8");

            char[] buf = new char[1024];
            int numRead=0;
            while((numRead=reader.read(buf)) != -1){
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
                buf = new char[1024];
            }
            reader.close();
            
            xmlPattern = fileData.toString();
            LOG.debug(xmlPattern);
        } catch (FileNotFoundException e) {
            LogUtils.logError(LOG, e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            LogUtils.logError(LOG, e);
            throw new RuntimeException(e);
        }
        
    }
    
}
