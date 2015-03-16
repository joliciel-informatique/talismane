package com.joliciel.talismane.machineLearning;

import java.io.IOException;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractClassificationModel extends
		AbstractMachineLearningModel implements ClassificationModel {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(AbstractClassificationModel.class);

	@Override
	protected void persistOtherEntries(ZipOutputStream zos) throws IOException {
	}	

}
