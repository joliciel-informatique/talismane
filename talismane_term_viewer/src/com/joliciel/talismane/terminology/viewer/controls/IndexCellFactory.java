package com.joliciel.talismane.terminology.viewer.controls;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import com.joliciel.talismane.terminology.Term;

@SuppressWarnings("restriction")
public class IndexCellFactory implements Callback<TableColumn<Term, Integer>, TableCell<Term, Integer>> {

	@Override
	public TableCell<Term, Integer> call(TableColumn<Term, Integer> arg0) {
		return new IndexCell<Term, Integer>();
	}

}
