package com.joliciel.talismane.terminology.viewer;

import java.util.List;
import java.util.Set;

import javafx.beans.property.SimpleBooleanProperty;

import com.joliciel.talismane.terminology.Context;
import com.joliciel.talismane.terminology.Term;

public class TermWrapper implements Term {
	private Term wrappedTerm;
	private AutoUpdateBooleanProperty markedProperty;
	
	public TermWrapper(Term wrappedTerm) {
		this.wrappedTerm = wrappedTerm;
		this.markedProperty = new AutoUpdateBooleanProperty(wrappedTerm);
	}
	
	@Override
	public String getText() {
		return wrappedTerm.getText();
	}

	@Override
	public Set<Term> getHeads() {
		return wrappedTerm.getHeads();
	}

	@Override
	public Set<Term> getExpansions() {
		return wrappedTerm.getExpansions();
	}

	@Override
	public List<Context> getContexts() {
		return wrappedTerm.getContexts();
	}

	@Override
	public void addHead(Term head) {
		wrappedTerm.addHead(head);
	}

	@Override
	public void addExpansion(Term expansion) {
		wrappedTerm.addExpansion(expansion);
	}

	@Override
	public void addContext(Context context) {
		wrappedTerm.addContext(context);
	}

	@Override
	public int getFrequency() {
		return wrappedTerm.getFrequency();
	}

	@Override
	public boolean isMarked() {
		return wrappedTerm.isMarked();
	}

	@Override
	public void setMarked(boolean marked) {
		markedProperty.set(marked);
		wrappedTerm.setMarked(marked);
	}

	public Term getWrappedTerm() {
		return wrappedTerm;
	}

	public SimpleBooleanProperty markedProperty() {
        return markedProperty;
    }

	@Override
	public int getExpansionCount() {
		return wrappedTerm.getExpansionCount();
	}
	
	private static final class AutoUpdateBooleanProperty extends SimpleBooleanProperty {
		private Term wrappedTerm;
		private boolean initialized = false;
		
		public AutoUpdateBooleanProperty(Term wrappedTerm) {
			super(wrappedTerm.isMarked());
			this.wrappedTerm = wrappedTerm;
			this.initialized = true;
		}

		@Override
		public void set(boolean marked) {
			super.set(marked);
			if (this.initialized) {
				wrappedTerm.setMarked(marked);
				wrappedTerm.save();
			}
		}
	}

	@Override
	public void save() {
		this.wrappedTerm.save();
	}

	@Override
	public Set<Term> getParents() {
		return this.wrappedTerm.getParents();
	}

	@Override
	public int getHeadCount() {
		return this.wrappedTerm.getHeadCount();
	}

	@Override
	public int getLexicalWordCount() {
		return this.wrappedTerm.getLexicalWordCount();
	}

	@Override
	public void setLexicalWordCount(int lexicalWordCount) {
		this.wrappedTerm.setLexicalWordCount(lexicalWordCount);
	}

	@Override
	public boolean isNew() {
		return this.wrappedTerm.isNew();
	}
	
}
