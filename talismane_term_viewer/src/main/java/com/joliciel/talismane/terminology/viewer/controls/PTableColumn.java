package com.joliciel.talismane.terminology.viewer.controls;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * This class allows to specify a percentage for the width of the column of a
 * TableView. Modified by Assaf Urieli
 * 
 * @author twasyl
 * @author Assaf Urieli
 */
@SuppressWarnings("restriction")
public class PTableColumn<S, T> extends TableColumn<S, T> {

	private final DoubleProperty percentWidth = new SimpleDoubleProperty(1);

	public PTableColumn() {
		tableViewProperty().addListener(new ChangeListener<TableView<S>>() {

			@Override
			public void changed(ObservableValue<? extends TableView<S>> ov, TableView<S> t, TableView<S> t1) {
				if (PTableColumn.this.prefWidthProperty().isBound()) {
					PTableColumn.this.prefWidthProperty().unbind();
				}

				PTableColumn.this.prefWidthProperty().bind(t1.widthProperty().multiply(percentWidth).divide(100.0));
			}
		});
	}

	public final DoubleProperty percentWidthProperty() {
		return this.percentWidth;
	}

	public final double getPercentWidth() {
		return this.percentWidthProperty().get();
	}

	public final void setPercentWidth(double value) throws IllegalArgumentException {
		if (value >= 0 && value <= 100) {
			this.percentWidthProperty().set(value);
		} else {
			throw new IllegalArgumentException(String.format(
					"The provided percentage width is not between 0.0 and 1.0. Value is: %1$s", value));
		}
	}

}