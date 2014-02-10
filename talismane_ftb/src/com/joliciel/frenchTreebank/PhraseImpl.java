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
package com.joliciel.frenchTreebank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

class PhraseImpl extends EntityImpl implements PhraseInternal {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1906638863648461475L;
	Phrase parent;
    int parentId;
    PhraseType phraseType;
    int phraseTypeId;
    List<PhraseUnitInternal> phraseUnits;
    List<PhraseUnit> allPhraseUnits;
    int position;
    int functionId;
    Function function;
    int depth;
    boolean dirty = true;
    boolean finalised = false;
    
    List<PhraseInternal> children;
    int currentPositionInPhrase = 0;
    TreebankServiceInternal treebankServiceInternal;
    private String text = null;

    public Phrase getParent() {
    	if (this.parent==null && this.parentId!=0) {
    		this.parent = this.treebankServiceInternal.loadPhrase(this.parentId);
    	}
        return parent;
    }
    
    public void setParent(Phrase parent) {
        if (this.parent==null||!this.parent.equals(parent)) {
            this.parent = parent;
            this.setParentId(parent==null? 0 : parent.getId());
        }
    }
    public int getParentId() {
        return parentId;
    }
    public void setParentId(int parentId) {
    	if (this.parentId!=parentId) {
    		this.parentId = parentId;
    		this.dirty=true;
    	}
    }
    public PhraseType getPhraseType() {
    	if (this.phraseType==null && this.phraseTypeId!=0) {
    		this.phraseType = this.treebankServiceInternal.loadPhraseType(this.phraseTypeId);
    	}
        return phraseType;
    }
    public void setPhraseType(PhraseType phraseType) {
        if (this.phraseType==null||!this.phraseType.equals(phraseType)) {
            this.phraseType = phraseType;
            this.setPhraseTypeId(phraseType.getId());
        }
    }
    public int getPhraseTypeId() {
        return phraseTypeId;
    }
    public void setPhraseTypeId(int phraseTypeId) {
    	if (this.phraseTypeId!=phraseTypeId) {
    		this.phraseTypeId = phraseTypeId;
    		this.dirty=true;
    	}
    }
    
    List<PhraseUnitInternal> getPhraseUnitsInternal() {
        if (this.isNew() && this.phraseUnits==null) {
            this.phraseUnits = new ArrayList<PhraseUnitInternal>();
        } else if (this.phraseUnits==null) {
        	this.phraseUnits = this.treebankServiceInternal.findPhraseUnits(this);
        }
        return phraseUnits;
    }
    
    public List<PhraseUnitInternal> getPhraseUnitsDB() {
        if (this.phraseUnits==null) {
            this.phraseUnits = new ArrayList<PhraseUnitInternal>();
        }
        return phraseUnits;
    }
    
    public List<PhraseUnit> getPhraseUnits() {
        List<PhraseUnit> phraseUnitsExternal = new ArrayList<PhraseUnit>();
        phraseUnitsExternal.addAll(this.getPhraseUnitsInternal());
        return phraseUnitsExternal;        
    }
    
    public List<PhraseUnit> getAllPhraseUnits() {
        if (!this.isNew() && this.allPhraseUnits==null) {
            this.allPhraseUnits = this.treebankServiceInternal.findAllPhraseUnits(this);
        }
        return allPhraseUnits;
    }
    
    public List<PhraseUnit> getAllPhraseUnitsDB() {
    	if (this.allPhraseUnits==null) {
    		this.allPhraseUnits = new ArrayList<PhraseUnit>();
    	}
    	return allPhraseUnits;
    }
    
    public void addPhraseUnit(PhraseUnitInternal phraseUnit) {
        this.getPhraseUnitsInternal().add(phraseUnit);
        phraseUnit.setPhrase(this);
        phraseUnit.setPositionInPhrase(currentPositionInPhrase++);
    }

    public int getPositionInPhrase() {
        return position;
    }
    public void setPositionInPhrase(int position) {
    	if (this.position!=position) {
    		this.position = position;
    		this.dirty=true;
    	}
    }

    public List<PhraseInternal> getChildrenDB()  {
        if (this.children==null)
            this.children = new ArrayList<PhraseInternal>();
        return children;
    }
    
    List<PhraseInternal> getChildrenInternal()  {
        if (this.isNew() && this.children==null)
            this.children = new ArrayList<PhraseInternal>();
        else if (this.children==null)
        	this.children = this.treebankServiceInternal.findChildren(this);
        return children;
    }
    
    public List<Phrase> getChildren() {       
        List<Phrase> childrenExternal = new ArrayList<Phrase>();
        childrenExternal.addAll(this.getChildrenInternal());
        return childrenExternal;
    }
    public PhraseInternal newChild() {
        PhraseInternal child = this.treebankServiceInternal.newPhrase(this);
        child.setPositionInPhrase(currentPositionInPhrase++);
        child.setDepth(this.getDepth()+1);
        this.getChildrenInternal().add(child);
        return child;
    }
    public TreebankServiceInternal getTreebankServiceInternal() {
        return treebankServiceInternal;
    }
    public void setTreebankServiceInternal(
            TreebankServiceInternal treebankServiceInternal) {
        this.treebankServiceInternal = treebankServiceInternal;
    }
    public Function getFunction() {
        return function;
    }
    public void setFunction(Function function) {
        if (this.function==null||!this.function.equals(function)) {
            this.function = function;
            this.setFunctionId(function==null ? 0 : function.getId());
        }
     }
    public int getFunctionId() {
        return functionId;
    }
    public void setFunctionId(int functionId) {
    	if (this.functionId!=functionId) {
    		this.functionId = functionId;
    		this.dirty=true;
    	}
    }
    
    @Override
	public void assignVerbSubcategories() {
        // look for auxiliary verbs & mark them as such
        // these will be saved when the sentence enclosing them is saved
        if (this.phraseUnits!=null) {
            List<PhraseUnitInternal> verbs = new ArrayList<PhraseUnitInternal>();
            for (PhraseUnitInternal phraseUnit : this.phraseUnits) {
                if (phraseUnit.getCategory().getCode().equals("V")) {
                    verbs.add(phraseUnit);
                }
                if (verbs.size()>1) {
                    Category verbCat = this.treebankServiceInternal.loadOrCreateCategory("V");
                    SubCategory auxVerbSubCat = this.treebankServiceInternal.loadOrCreateSubCategory(verbCat, "aux");
                    SubCategory causVerbSubCat = this.treebankServiceInternal.loadOrCreateSubCategory(verbCat, "caus");
                    int i = 0;
                    for (PhraseUnitInternal verb : verbs) {
                        // don't touch the last verb
                        if (i == verbs.size()-1)
                            break;
                        if (verb.getLemma().getText().equals("avoir")||verb.getLemma().getText().equals("être"))
                            verb.setSubCategory(auxVerbSubCat);
                        else if  (verb.getLemma().getText().equals("faire")||verb.getLemma().getText().equals("laisser"))
                            verb.setSubCategory(causVerbSubCat);
                        i++;
                    }
                }
            }
        }
        
        // do this recursively
        if (this.children!=null)
        	for (PhraseInternal child : this.children)
                child.assignVerbSubcategories();
    }
    
    public void saveInternal() {
    	if (this.dirty)
    		this.treebankServiceInternal.savePhraseInternal(this);
        if (this.children!=null)
            for (PhraseInternal child : this.children)
                child.save();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Phrase) {
            return ((Phrase) obj).getId()==this.getId();
        }
        return false;
    }
    @Override
    public int hashCode() {
        if (this.getId()!=0)
            return this.getId();
        else
            return super.hashCode();
    }
    
    public int getDepth() {
        return depth;
    }
    public void setDepth(int depth) {
        this.depth = depth;
    }
    
    public List<PhraseInternal> savePhraseDescendentMapping() {
        List<PhraseInternal> descendants = new ArrayList<PhraseInternal>();
        descendants.add(this);
        if (this.children!=null)
            for (PhraseInternal child : this.children)
                descendants.addAll(child.savePhraseDescendentMapping());
        
        for (PhraseInternal descendant : descendants)
            treebankServiceInternal.savePhraseDescendantMapping(this, descendant);
        
        return descendants;
     }
    
    public String getText() {
        if (text == null && !this.isNew()) {
            text = "";
            List<Word> words = treebankServiceInternal.findWords(this);
            String lastText = "'";
            for (Word word : words) {
                if (word.getText()!=null && word.getText().length()>0) {
                    if (lastText.endsWith("'") || word.getText()=="." || word.getText()==",") {
                        text += word.getText();
                    } else {
                        text += " " + word.getText();
                    }
                    lastText = word.getText();
                }
            }
        }
        return text;
    }
	@Override
	public boolean isDirty() {
		return dirty;
	}
	@Override
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
	@Override
	public boolean isPhrase() {
		return true;
	}
	
	@Override
	public List<PhraseElement> getElements() {
		TreeMap<Integer, PhraseElement> elementMap = new TreeMap<Integer, PhraseElement>();
		for (PhraseUnit punit : this.getPhraseUnits()) {
			elementMap.put(punit.getPositionInPhrase(), punit);
		}
		for (Phrase child : this.getChildren()) {
			elementMap.put(child.getPositionInPhrase(), child);
		}
		List<PhraseElement> elements = new ArrayList<PhraseElement>(elementMap.values());
		return elements;
	}

	@Override
	public void finalisePhrase() {
		if (!finalised) {
			if (this.getParent()!=null) {
				PhraseInternal parentInternal = (PhraseInternal) this.getParent();
				parentInternal.finalisePhrase();
			} else {
				// top-level phrase
				Map<Integer, Integer> splitCompoundMap = new HashMap<Integer, Integer>();

				for (PhraseUnit phraseUnit : this.getAllPhraseUnits()) {
					if (phraseUnit.getNextCompoundPartId()!=0) {
						splitCompoundMap.put(phraseUnit.getId(), phraseUnit.getNextCompoundPartId());
					}
				}
				
				for (Entry<Integer, Integer> splitCompoundEntry : splitCompoundMap.entrySet()) {
					for (PhraseUnit phraseUnit : this.getAllPhraseUnits()) {
						if (phraseUnit.getId()==splitCompoundEntry.getValue()) {
							PhraseUnitInternal phraseUnitInternal = (PhraseUnitInternal) phraseUnit;
							phraseUnitInternal.setPreviousCompoundPartId(splitCompoundEntry.getKey());
							break;
						}
					}
				}
			}
			this.finalised = true;
		}
	}
    
    
}
