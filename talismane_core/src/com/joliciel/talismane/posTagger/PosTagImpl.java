package com.joliciel.talismane.posTagger;

public class PosTagImpl implements PosTag {

	private static final long serialVersionUID = -7223891382830355170L;
	private String code;
	private String description;
	private PosTagOpenClassIndicator openClassIndicator;
	
	public PosTagImpl(String code, String description,
			PosTagOpenClassIndicator openClassIndicator) {
		super();
		this.code = code;
		this.description = description;
		this.openClassIndicator = openClassIndicator;
	}

	@Override
	public boolean isEmpty() {
		return this.code.equals(PosTag.NULL_POS_TAG_CODE);
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public PosTagOpenClassIndicator getOpenClassIndicator() {
		return openClassIndicator;
	}

	public void setOpenClassIndicator(PosTagOpenClassIndicator openClassIndicator) {
		this.openClassIndicator = openClassIndicator;
	}

	@Override
	public int hashCode() {
		return this.code.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof PosTag))
			return false;
		PosTag other = (PosTag) obj;
		if (code == null) {
			if (other.getCode() != null)
				return false;
		} else if (!code.equals(other.getCode()))
			return false;
		return true;
	}

	@Override
	public int compareTo(PosTag posTag) {
		return this.getCode().compareTo(posTag.getCode());
	}
	

	@Override
	public String toString() {
		return this.code;
	}
}
