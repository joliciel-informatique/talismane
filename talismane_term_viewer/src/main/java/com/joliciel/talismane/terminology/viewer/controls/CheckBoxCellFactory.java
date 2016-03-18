package com.joliciel.talismane.terminology.viewer.controls;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

@SuppressWarnings("restriction")
public class CheckBoxCellFactory<S, T> implements Callback<TableColumn<S, T>, TableCell<S, T>> {

	@Override
	public TableCell<S, T> call(TableColumn<S, T> arg0) {
		return new CheckBoxTableCell<S, T>();
	}

}
