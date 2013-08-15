package com.joliciel.talismane.machineLearning;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.utils.LogUtils;

public abstract class AbstractClassificationModel<T extends Outcome> extends
		AbstractMachineLearningModel implements ClassificationModel<T> {
	private static final Log LOG = LogFactory.getLog(AbstractClassificationModel.class);
	private DecisionFactory<T> decisionFactory;

	@Override
	protected void persistOtherEntries(ZipOutputStream zos) throws IOException {
		zos.putNextEntry(new ZipEntry("decisionFactory.obj"));
		ObjectOutputStream out = new ObjectOutputStream(zos);

		try {
			out.writeObject(decisionFactory);
		} finally {
			out.flush();
		}
		
		zos.flush();
	}	

	@Override
	public boolean loadZipEntry(ZipInputStream zis, ZipEntry ze)
			throws IOException {
		boolean loaded = true;
    	if (ze.getName().equals("decisionFactory.obj")) {
		    ObjectInputStream in = new ObjectInputStream(zis);
			try {
				@SuppressWarnings("unchecked")
				DecisionFactory<T> decisionFactory = (DecisionFactory<T>) in.readObject();
				this.setDecisionFactory(decisionFactory);
			} catch (ClassNotFoundException e) {
				LogUtils.logError(LOG, e);
				throw new RuntimeException(e);
			}
    	} else {
    		loaded = super.loadZipEntry(zis, ze);
    	}

		return loaded;
	}

	public DecisionFactory<T> getDecisionFactory() {
		return decisionFactory;
	}
	public void setDecisionFactory(DecisionFactory<T> decisionFactory) {
		this.decisionFactory = decisionFactory;
	}


}
