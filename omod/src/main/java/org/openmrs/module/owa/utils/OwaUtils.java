/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0 + Health disclaimer. If a copy of the MPL was not distributed with
 * this file, You can obtain one at http://license.openmrs.org
 */
package org.openmrs.module.owa.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import org.openmrs.module.Module;
import org.openmrs.module.ModuleUtil;
import org.openmrs.module.ModuleFactory;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.module.owa.App;
import org.openmrs.module.owa.AppRequirements;
import org.openmrs.module.owa.AppRequiredModule;

import java.lang.StringBuilder;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;

/**
 * This class contains utility methods that support various actions that affect OWAs for example OWA
 * installation
 */
public class OwaUtils {
	
	private static final Log log = LogFactory.getLog(OwaUtils.class);
	
	/**
	 * Gets the file name from the OWA installation url
	 * 
	 * @param installUrl URL to where the OWA zip can be downloaded
	 * @return the file name of the owa
	 */
	public static String getFileName(String installUrl) {
		String passedFileName = null;
		if (installUrl.contains("file_path=")) {
			passedFileName = StringUtils.substringBetween(installUrl, "file_path=", ".zip");
		} else {
			passedFileName = FilenameUtils.getName(installUrl);
		}
		return removeVersionNumber(passedFileName);
	}
	
	/**
	 * Removes the version number from the file name of the OWA
	 * 
	 * @param passedFileName File name of owa that may contain owa version number
	 * @return the file name of the owa with the version number removed
	 */
	public static String removeVersionNumber(String passedFileName) {
		String[] tokens = passedFileName.split("((-|[_])+[0-9])|[\\s]");
		String fileName = tokens[0];
		if (fileName != null && !fileName.contains(".zip")) {
			fileName += ".zip";
		}
		return fileName;
	}
	
	/**
	 * Returns String message of missing requirements and empty String if all the requirements are
	 * installed or if no special requirements are needed.
	 * 
	 * @param file uploaded file of owa that contains Manifest.webapp entry
	 * @return message about missing requirements
	 */
	public static String extractMissingRequirementsMessage(File file) throws IOException {
		
		StringBuilder errorMessage = new StringBuilder("");
		
		App app = getAppDefinition(file);
		AppRequirements appRequirements = null;
		
		if (null != app.getActivities().getOpenmrs().getRequirements()) {
			appRequirements = app.getActivities().getOpenmrs().getRequirements();
			
			if (null != appRequirements.getCoreVersion()) {
				if (!ModuleUtil.matchRequiredVersions(OpenmrsConstants.OPENMRS_VERSION_SHORT,
				    appRequirements.getCoreVersion())) {
					
					errorMessage.append("OpenMRS-core version: ").append(appRequirements.getCoreVersion());
				}
			}
			
			if (null != appRequirements.getRequiredModules()) {
				for (AppRequiredModule requiredModule : appRequirements.getRequiredModules()) {
					boolean moduleStarted = false;
					String reqVersion = requiredModule.getVersion();
					for (Module module : ModuleFactory.getStartedModules()) {
						if (module.getPackageName().equals(requiredModule.getName())) {
							
							if (reqVersion != null && ModuleUtil.matchRequiredVersions(module.getVersion(), reqVersion)) {
								moduleStarted = true;
							}
							break;
						}
					}
					if (!moduleStarted) {
						errorMessage
						        .append(", ")
						        .append(
						            requiredModule.getName().replace("org.openmrs.module.", "").replace("org.openmrs.", ""))
						        .append(" version: ").append(reqVersion);
					}
				}
			}
		}
		return errorMessage.toString();
	}
	
	/**
	 * Returns App app definition from file
	 * 
	 * @param file zip file of owa that contains Manifest.webapp entry
	 * @return App extracted from the manifest.webapp file
	 */
	
	private static App getAppDefinition(File file) throws IOException {

		App app = null;    		
		ObjectMapper mapper = new ObjectMapper();     
		mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);		
		
		try (ZipFile zipFile = new ZipFile(file)) {
			ZipArchiveEntry entry = zipFile.getEntry("manifest.webapp");
			
			try (InputStream inputStream = zipFile.getInputStream(entry)) { 
				String manifest = org.apache.commons.io.IOUtils.toString(inputStream);
				app = mapper.readValue(manifest, App.class);
				
			}		
		}
		return app;
	}
}
