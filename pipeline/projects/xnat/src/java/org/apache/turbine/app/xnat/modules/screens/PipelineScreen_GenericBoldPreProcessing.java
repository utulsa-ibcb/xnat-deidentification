/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.apache.turbine.app.cnda_xnat.modules.screens;

import java.io.File;
import java.util.Enumeration;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.pipeline.launchers.GenericBoldPreProcessingLauncher;
import org.nrg.pipeline.launchers.StdBuildLauncher;
import org.nrg.pipeline.utils.PipelineUtils;
import org.nrg.xdat.om.ArcPipelineparameterdata;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.modules.screens.DefaultPipelineScreen;

public class PipelineScreen_GenericBoldPreProcessing extends DefaultPipelineScreen  {

	String mprageScanType;
	String tseScanType;
	String t1wScanType;
	String pdt2ScanType;
	String boldScanType;

	static Logger logger = Logger.getLogger(PipelineScreen_GenericBoldPreProcessing.class);
 
	
	 public void finalProcessing(RunData data, Context context){
		 try {
			XnatMrsessiondata mr = (XnatMrsessiondata) om;
	    	context.put("mr", mr); 
	    
		    ArcPipelineparameterdata  mprageParam = 	getProjectPipelineSetting(GenericBoldPreProcessingLauncher.MPRAGE_PARAM);
		    if (mprageParam != null) {
		       mprageScanType = mprageParam.getCsvvalues();
		    }
		    ArcPipelineparameterdata tseParam = 	 getProjectPipelineSetting(GenericBoldPreProcessingLauncher.TSE_PARAM);
		    if (tseParam != null)
		    	tseScanType = tseParam.getCsvvalues();
		    ArcPipelineparameterdata t1wParam = 	 getProjectPipelineSetting(GenericBoldPreProcessingLauncher.T1W_PARAM);
		    if (t1wParam != null)
		    	t1wScanType = t1wParam.getCsvvalues();
		    ArcPipelineparameterdata pdt2Param = 	 getProjectPipelineSetting(GenericBoldPreProcessingLauncher.PDT2_PARAM);
		    if (pdt2Param != null)
		    	pdt2ScanType = pdt2Param.getCsvvalues();
		    
		    ArcPipelineparameterdata epiParam = 	 getProjectPipelineSetting(GenericBoldPreProcessingLauncher.EPI_PARAM);
		    if (epiParam != null)
		    	boldScanType = 	 epiParam.getCsvvalues();
		    	
	    	String selfStatus = WrkWorkflowdata.GetLatestWorkFlowStatusByPipeline(mr.getId(), XnatMrsessiondata.SCHEMA_ELEMENT_NAME, GenericBoldPreProcessingLauncher.LOCATION+File.separator +GenericBoldPreProcessingLauncher.NAME, mr.getProject(), TurbineUtils.getUser(data));
	    	if (selfStatus.equalsIgnoreCase(WrkWorkflowdata.COMPLETE)) {
	    		data.setMessage("This pipeline has already completed. Relaunching the pipeline may result in loss of processed files");
	    	}
	        	String pathToTarget = getTarget(); 
	        	context.put("target",pathToTarget);
	        	context.put("target_name",getTargetName(pathToTarget));
	        	context.put("projectSettings", projectParameters);
	        	LinkedHashMap<String,String> buildableScanTypes = new LinkedHashMap<String,String>();
	        	if (mprageScanType != null) 
	        		buildableScanTypes.put(GenericBoldPreProcessingLauncher.MPRAGE, mprageScanType);
	        	if (tseScanType != null) 
	        		buildableScanTypes.put(GenericBoldPreProcessingLauncher.TSE,tseScanType);
	        	if (t1wScanType != null) 
	        		buildableScanTypes.put(GenericBoldPreProcessingLauncher.T1W,t1wScanType);
	        	if (pdt2ScanType != null) 
	        		buildableScanTypes.put(GenericBoldPreProcessingLauncher.PDT2,pdt2ScanType);

	        	if (boldScanType != null)  {
	        		buildableScanTypes.put(StdBuildLauncher.EPI, boldScanType);
	        	}

	        	context.put("buildableScanTypes", buildableScanTypes);

		  		 String cross_day_register = "0";
		  		ArcPipelineparameterdata parameter  = 	 getProjectPipelineSetting(PipelineUtils.CROSS_DAY_REGISTER);
		         if (parameter.getCsvvalues().equals("1")) {
		        	  context.put("cross_day_list", XnatMrsessiondata.GetAllPreviousImagingSessions(mr, mr.getProject(), TurbineUtils.getUser(data)));
		          }
		 }catch(Exception e) {
			 logger.error("Possibly the project wide pipeline has not been set", e);
			 e.printStackTrace();
		 }
	 }
	 
	 public void preProcessing(RunData data, Context context)   {
	     super.preProcessing(data, context);
  	 }
	 
		protected String getTarget() {
			String rtn = null;
			Enumeration<String>  keysEnum = projectParameters.keys();
			while (keysEnum.hasMoreElements()) {
				String key = keysEnum.nextElement(); 
				if (key.equalsIgnoreCase("target" )) {	
					ArcPipelineparameterdata param = projectParameters.get(key);
					rtn = param.getCsvvalues();
					break;
				}
			}
			return rtn;
		}
		
		protected String getTargetName(String atlasPath) {
			String rtn = null; if (atlasPath == null) return rtn;
			rtn = atlasPath;
			int indexOfLastSlash = atlasPath.lastIndexOf(File.separator);
			if (indexOfLastSlash != -1) {
				String targetName = atlasPath.substring(indexOfLastSlash + 1);
				int indexOfDot = targetName.indexOf(".");
				if (indexOfDot != -1)
				   rtn = targetName.substring(0,indexOfDot);
				else
					rtn = targetName;
			}
			return rtn;
		}
	

		
	
		

}
