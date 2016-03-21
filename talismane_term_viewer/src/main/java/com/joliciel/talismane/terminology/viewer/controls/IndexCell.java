package com.joliciel.talismane.terminology.viewer.controls;

import javafx.scene.control.TableCell;

@SuppressWarnings("restriction")
public class IndexCell<S, T> extends TableCell<S, T> {
	@Override
	protected void updateItem(Object object, boolean selected) {
		setText(String.valueOf(getIndex() + 1));
	}
}
