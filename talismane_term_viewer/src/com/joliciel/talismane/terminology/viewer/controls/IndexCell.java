package com.joliciel.talismane.terminology.viewer.controls;

import javafx.scene.control.TableCell;

public class IndexCell<S,T> extends TableCell<S, T> {
	protected void updateItem(Object object, boolean selected){
	    setText(String.valueOf(getIndex()+1));
	}
}
