package uk.ac.cam.cares.jps.base.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cares.jps.base.exception.PythonException;
import uk.ac.cam.cares.jps.base.config.AgentLocator;

public class PythonHelper {
	
	private static Logger logger = LoggerFactory.getLogger(PythonHelper.class);

	/**
	 * @param pythonScriptName
	 *            (including package name followed by script name and .py extension,
	 *            e.g. caresjpsadmsinputs/ADMSGeoJsonGetter.py)
	 * @param parameter input parameter for calling python
	 * @param thisObject <code>this</code> must be used as input value 
	 * @return
	 * @throws IOException
	 */
	public static String callPython(String pythonScriptName, String parameter, Object thisObject) throws IOException {
		String path = AgentLocator.getNewPathToPythonScript(pythonScriptName, thisObject);
		
		logger.info(path, parameter);
		
		String[] cmd = { "python", path, parameter };

		Process p = Runtime.getRuntime().exec(cmd);

		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String returnValue = stdInput.readLine();
		
		int index = returnValue.lastIndexOf("{exception:");
		if (index >= 0) {
			int lastIndex = returnValue.length() - 1;
			String message = returnValue.substring(index+11, lastIndex);
			throw (new PythonException(message));
		} else {
			return returnValue;
		}
	}
	
	public static String callPython(String pythonScriptName, String parameter1, String parameter2, Object thisObject) throws IOException {
		String pathPythonScript = AgentLocator.getNewPathToPythonScript(pythonScriptName, thisObject);		
		
		logger.info(pathPythonScript, parameter1, parameter2);
		
		String[] cmd = { "python", pathPythonScript, parameter1, parameter2 };
		
		Process p = Runtime.getRuntime().exec(cmd);
		
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
		return stdInput.readLine();
	}
}