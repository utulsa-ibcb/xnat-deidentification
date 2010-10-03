/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.pipeline.launchers;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.pipeline.XnatPipelineLauncher;
import org.nrg.pipeline.utils.FileUtils;
import org.nrg.pipeline.utils.PipelineUtils;
import org.nrg.pipeline.xmlbeans.ParameterData;
import org.nrg.pipeline.xmlbeans.ParameterData.Values;
import org.nrg.pipeline.xmlbeans.ParametersDocument.Parameters;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.om.XnatMrsessiondataI;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;

public class GenericBoldPreProcessingLauncher extends PipelineLauncher{

	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(GenericBoldPreProcessingLauncher.class);
	
	public static final String  NAME = "GenericBoldPreprocessing.xml";
	public static final String  LOCATION = "build-tools";
	public static final	String STDBUILDTEMPLATE = "PipelineScreen_GenericBoldPreProcessing.vm";

	
	public static final	String MPRAGE = "MPRAGE";
	public static final String MPRAGE_PARAM = "mprs";
	public static final	String TSE = "TSE";
	public static final String TSE_PARAM = "tse";
	public static final	String T1W = "T1W";
	public static final String T1W_PARAM = "t1w";
	public static final	String PDT2 = "PDT2";
	public static final	String PDT2_PARAM = "pdt2";
	public static final	String EPI = "BOLD";
	public static final	String EPI_PARAM = "fstd";

	
	public boolean launch(RunData data, Context context) {
		boolean rtn = false;
		try {
		ItemI data_item = TurbineUtils.GetItemBySearch(data);
		XnatMrsessiondata mr = new XnatMrsessiondata(data_item);
		XnatPipelineLauncher xnatPipelineLauncher = XnatPipelineLauncher.GetLauncher(data, context, mr);
		String pipelineName = data.getParameters().get("pipelinename");
		String cmdPrefix = data.getParameters().get("cmdprefix");
		xnatPipelineLauncher.setPipelineName(pipelineName);
		String buildDir = FileUtils.getBuildDir(mr.getProject(), true);
		buildDir +=   "stdb"  ;
		xnatPipelineLauncher.setBuildDir(buildDir);
		xnatPipelineLauncher.setNeedsBuildDir(false);
		
		Parameters parameters = Parameters.Factory.newInstance();
		
        ParameterData param = parameters.addNewParameter();
    	param.setName("rm_prev_folder");
    	param.addNewValues().setUnique("0");
		
		param = parameters.addNewParameter();
    	param.setName("sessionId");
    	param.addNewValues().setUnique(mr.getLabel());

		boolean build = false;

		String target = data.getParameters().get("target");
		param = parameters.addNewParameter();
    	param.setName("target");
    	param.addNewValues().setUnique(target);
    	
		param = parameters.addNewParameter();
    	param.setName("TR_vol");
    	param.addNewValues().setUnique(data.getParameters().get("TR_vol"));
    	
		param = parameters.addNewParameter();
    	param.setName("skip");
    	param.addNewValues().setUnique(data.getParameters().get("skip"));

		if (TurbineUtils.HasPassedParameter(PipelineUtils.CROSS_DAY_REGISTER, data)) {
			String cross_day_register = data.getParameters().get(PipelineUtils.CROSS_DAY_REGISTER);
			if (!cross_day_register.equals("-1")) {
				XnatMrsessiondataI crossMr = XnatMrsessiondata.getXnatMrsessiondatasById(cross_day_register, TurbineUtils.getUser(data), false);
				if (crossMr != null)
					param = parameters.addNewParameter();
				   param.setName("day1_archivedir");
	    	       param.addNewValues().setUnique(((XnatMrsessiondata)crossMr).getArchivePath());

					param = parameters.addNewParameter();
				   param.setName("day1_sessionId");
	    	       param.addNewValues().setUnique(crossMr.getLabel());
			}
		}


		ArrayList<String> mprs = getCheckBoxSelections(data,mr,MPRAGE);
		ArrayList<String> t2s = getCheckBoxSelections(data,mr,TSE);
		ArrayList<String> t1w = getCheckBoxSelections(data,mr,T1W);
		ArrayList<String> pdt2 = getCheckBoxSelections(data,mr,PDT2);

		ArrayList<String> bold = getCheckBoxSelections(data,mr,StdBuildLauncher.EPI);

		if (TurbineUtils.HasPassedParameter("build_" + MPRAGE, data)) {
//			xnatPipelineLauncher.setParameter("mprs",mprs);
		      param = parameters.addNewParameter();
		      param.setName("mprs");
		      Values values = param.addNewValues();
		      if (mprs.size() == 1) {
		    	  values.setUnique(mprs.get(0));
		      }else {
			       for (int i = 0; i < mprs.size(); i++) {
			        	values.addList(mprs.get(i));
			        }
		      }
			build = true;
		}
		if (TurbineUtils.HasPassedParameter("build_" + TSE, data)) {
		      param = parameters.addNewParameter();
		      param.setName("tse");
		      Values values = param.addNewValues();
		      if (t2s.size() == 1) {
		    	  values.setUnique(t2s.get(0));
		      }else {
			      for (int i = 0; i < t2s.size(); i++) {
			        	values.addList(t2s.get(i));
			        }
		      }
			build = true;
		}

		if (TurbineUtils.HasPassedParameter("build_" + T1W, data)) {
		      param = parameters.addNewParameter();
		      param.setName("t1w");
		      Values values = param.addNewValues();
		      if (t1w.size() == 1) {
		    	  values.setUnique(t1w.get(0));
		      }else {
			      for (int i = 0; i < t1w.size(); i++) {
			        	values.addList(t1w.get(i));
			        }
		      }
			build = true;
		}

		if (TurbineUtils.HasPassedParameter("build_" + PDT2, data)) {
		      param = parameters.addNewParameter();
		      param.setName("pdt2");
		      Values values = param.addNewValues();
		      if (pdt2.size() == 1) {
		    	  values.setUnique(pdt2.get(0));
		      }else {
			      for (int i = 0; i < pdt2.size(); i++) {
			        	values.addList(pdt2.get(i));
			        }
		      }
			build = true;
		}
		
		if (TurbineUtils.HasPassedParameter("build_" + EPI, data)) {
//			xnatPipelineLauncher.setParameter("fstd",bold);
		      param = parameters.addNewParameter();
		      param.setName("fstd");
		      Values values = param.addNewValues();
		      if (bold.size() == 1) {
		    	  values.setUnique(bold.get(0));
		      }else {
			      for (int i = 0; i < bold.size(); i++) {
			        	values.addList(bold.get(i));
			        }
		      }

			if (bold.size() > 0) {
				ArrayList<String> irun = getRunLabels(bold);
//	    		xnatPipelineLauncher.setParameter("irun",irun);
			      param = parameters.addNewParameter();
			      param.setName("irun");
			       values = param.addNewValues();
			      if (irun.size() == 1) {
			    	  values.setUnique(irun.get(0));
			      }else {
				      for (int i = 0; i < irun.size(); i++) {
				        	values.addList(irun.get(i));
				        }
			      }

				String epidir = data.getParameters().get("epidir");
				param = parameters.addNewParameter();
		    	param.setName("epidir");
		    	param.addNewValues().setUnique(epidir);

				String epi2atl = data.getParameters().get("epi2atl");
				param = parameters.addNewParameter();
		    	param.setName("epi2atl");
		    	param.addNewValues().setUnique(epi2atl);

				String normode = data.getParameters().get("normode");
				param = parameters.addNewParameter();
		    	param.setName("normode");
		    	param.addNewValues().setUnique(normode);

				String economy = data.getParameters().get("economy");
				param = parameters.addNewParameter();
		    	param.setName("economy");
		    	param.addNewValues().setUnique(economy);
		    	
				ArrayList<String> functionalScans = getCheckBoxSelections(data,mr,"functional_"+StdBuildLauncher.EPI, data.getParameters().getInt(StdBuildLauncher.EPI+"_rowcount"));
	            if (functionalScans.size() > 0) {
	            	ArrayList<String> functional = getRunLabels(bold,irun,functionalScans);
//	        		xnatPipelineLauncher.setParameter("fcbolds",functional);
				      param = parameters.addNewParameter();
				      param.setName("fcbolds");
				       values = param.addNewValues();
				      if (functional.size() == 1) {
				    	  values.setUnique(functional.get(0));
				      }else {
					      for (int i = 0; i < functional.size(); i++) {
					        	values.addList(functional.get(i));
					        }
				      }
//	        		xnatPipelineLauncher.setParameter("preprocessFunctional","1");
					param = parameters.addNewParameter();
			    	param.setName("preprocessFunctional");
			    	param.addNewValues().setUnique("1");

	            }
			}
			build = true;
		}

		if (build) {
			String paramFileName = getName(pipelineName);
			Date date = new Date();
		    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		    String s = formatter.format(date);
			
			paramFileName += "_params_" + s + ".xml"; 

			String paramFilePath = saveParameters(buildDir+File.separator + mr.getLabel(),paramFileName,parameters);  
		    xnatPipelineLauncher.setParameterFile(paramFilePath);
			rtn = xnatPipelineLauncher.launch(cmdPrefix);
		}else rtn = true;

		}catch(Exception e) {
			logger.debug(e);
		}
		return rtn;
	}

	private ArrayList<String> getRunLabels(ArrayList<String> boldScans) {
		ArrayList<String> rtn = new ArrayList<String>();
		for (int i = 0; i < boldScans.size(); i++) {
			rtn.add("run"+(i+1));
		}
		return rtn;
	}

	private ArrayList<String> getRunLabels(ArrayList<String> boldScans,ArrayList<String> runLabels,ArrayList<String> functionalScans ) {
		ArrayList<String> rtn = new ArrayList<String>();
		for (int i = 0; i < functionalScans.size(); i++) {
			for (int j=0; j<boldScans.size(); j++) {
				if (boldScans.get(j).equals(functionalScans.get(i))) {
					rtn.add(runLabels.get(j));
					break;
				}
			}
		}
		return rtn;
	}

}
