package uk.ac.cam.cares.jps.base.scenario;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.ThreadContext;
import org.json.JSONObject;

import uk.ac.cam.cares.jps.base.config.JPSConstants;
import uk.ac.cam.cares.jps.base.discovery.AgentCaller;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;

/**
 * All JPS agents that want to make use of scenario have to inherit from this servlet class.
 * 
 * @author Andreas
 *
 */
public abstract class JPSHttpServlet extends HttpServlet {
	
	private static final long serialVersionUID = 3507827296966247195L;
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		try {
			enableScenario(request);
			doGetJPS(request, response);
		} catch (Exception e) {
			throw new JPSRuntimeException(e.getMessage(), e);
		} finally {
			disableScenario();
		}
	}
	
	abstract protected void doGetJPS(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;
	
	/**
	 * Adds the scenariourl to ThreadContext such that other classes from the JPS_BASE can access it
	 * 
	 * @param request
	 */
	public static void enableScenario(HttpServletRequest request) {
		JSONObject jo = AgentCaller.readJsonParameter(request);
		if (!jo.isNull(JPSConstants.SCENARIO_URL)) {
			String scenarioURL = jo.getString(JPSConstants.SCENARIO_URL);
			ThreadContext.put(JPSConstants.SCENARIO_URL, scenarioURL);
		}
	}
	
	/**
	 * Removes the scenariourl. This is important for the case that Tomcat (or any other server) might reuse the threads 
	 * which would have the consequence that other agents run wrongly in the same scenario. For this reason, this method
	 * must be called whenever enable was called - even in case of an exception during any get, put etc. method of a servlet. 
	 */
	public static void disableScenario() {
		ThreadContext.remove(JPSConstants.SCENARIO_URL);
	}
}