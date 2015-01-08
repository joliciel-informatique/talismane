package com.joliciel.talismane.lexicon;

abstract class AbstractLexicalEntry implements LexicalEntry {
	private static final long serialVersionUID = 1L;

	@Override
	public String getMorphologyForCoNLL() {
		String morphologyForCoNLL = "";
		if (this.hasAttribute(LexicalAttribute.SubCategory) && this.getSubCategory().length()>0) {
			morphologyForCoNLL+= "s=" + this.getSubCategory() + "|";
		}
		if (this.hasAttribute(LexicalAttribute.Case) && this.getCase().size()>0) {
			morphologyForCoNLL+= "c=";
			boolean first = true;
			for (String aCase : this.getCase()) {
				if (!first)
					morphologyForCoNLL+= ",";
				morphologyForCoNLL += aCase;
			}
			morphologyForCoNLL += "|";
		}
		if (this.hasAttribute(LexicalAttribute.Number) &&this.getNumber().size()>0) {
			morphologyForCoNLL+= "n=";
			boolean first = true;
			for (String aNumber : this.getNumber()) {
				if (!first)
					morphologyForCoNLL+= ",";
				morphologyForCoNLL += aNumber;
			}
			morphologyForCoNLL += "|";
		}
		if (this.hasAttribute(LexicalAttribute.Gender)&& this.getGender().size()>0) {
			morphologyForCoNLL+= "g=";
			boolean first = true;
			for (String aGender : this.getGender()) {
				if (!first)
					morphologyForCoNLL+= ",";
				morphologyForCoNLL += aGender;
			}
			morphologyForCoNLL += "|";
		}
		if (this.hasAttribute(LexicalAttribute.Tense) && this.getTense().size()>0) {
			morphologyForCoNLL+= "t=";
			boolean first = true;
			for (String aTense : this.getTense()) {
				if (!first)
					morphologyForCoNLL+= ",";
				morphologyForCoNLL += aTense;
			}
			morphologyForCoNLL += "|";
		}
		if (this.hasAttribute(LexicalAttribute.Mood) && this.getMood().size()>0) {
			morphologyForCoNLL+= "m=";
			boolean first = true;
			for (String aMood : this.getMood()) {
				if (!first)
					morphologyForCoNLL+= ",";
				morphologyForCoNLL += aMood;
			}
			morphologyForCoNLL += "|";
		}
		if (this.hasAttribute(LexicalAttribute.Aspect) && this.getAspect().size()>0) {
			morphologyForCoNLL+= "a=";
			boolean first = true;
			for (String anAspect : this.getAspect()) {
				if (!first)
					morphologyForCoNLL+= ",";
				morphologyForCoNLL += anAspect;
			}
			morphologyForCoNLL += "|";
		}
		if (this.hasAttribute(LexicalAttribute.Person) && this.getPerson().size()>0) {
			morphologyForCoNLL+= "p=";
			boolean first = true;
			for (String aPerson : this.getPerson()) {
				if (!first)
					morphologyForCoNLL+= ",";
				morphologyForCoNLL += aPerson;
			}
			morphologyForCoNLL += "|";
		}
		if (this.hasAttribute(LexicalAttribute.PossessorNumber)&& this.getPossessorNumber().size()>0) {
			morphologyForCoNLL+= "poss=";
			boolean first = true;
			for (String aPossessorNumber : this.getPossessorNumber()) {
				if (!first)
					morphologyForCoNLL+= ",";
				morphologyForCoNLL += aPossessorNumber;
			}
			morphologyForCoNLL += "|";
		}

		return morphologyForCoNLL;
	}

}
