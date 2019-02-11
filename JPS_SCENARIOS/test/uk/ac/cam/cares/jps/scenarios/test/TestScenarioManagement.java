package uk.ac.cam.cares.jps.scenarios.test;

import java.util.List;

import junit.framework.TestCase;
import uk.ac.cam.cares.jps.scenario.ScenarioManagementAgent;

public class TestScenarioManagement extends TestCase {
	
	public void testCreateScenarioDescription() {
		
		String scenarioName = "test1234567";
		String agent = "http://www.theworldavatar.com/kb/agents/Service__OpenWeatherMap.owl";
		
		new ScenarioManagementAgent().createScenarioDescription(scenarioName, agent);
		
		
	}
	
	public void testTmp() {
		List<String> list = new ScenarioManagementAgent().getScenarioIRIs();
		for (String current : list) {
			//system.out.println(current);
		}
	}
	
	public void testTmp2() {
		
		//String json = new ScenarioManagementAgent().listScenariosAndAgentsAsJson();
		////system.out.println(json);
		
		new ScenarioManagementAgent().writeListToFile();
	}

//	public void testTmp3() {
//		
//		//String json = new ScenarioManagementAgent().listScenariosAndAgentsAsJson();
//		////system.out.println(json);
//		String dir = "C:/Users/Andreas/my/JPSWorkspace/JParkSimulator-git/JPS_COMPOSITION/testres/admsservicesWithWasteProduct";
//		KeyValueServer.set(ServiceDiscovery.KEY_DIR_KB_AGENTS, dir);
//		//system.out.println("dir=" + KeyValueServer.get(ServiceDiscovery.KEY_DIR_KB_AGENTS));
//		
//		ArrayList<Service> services = ServiceDiscovery.getInstance().getServices();
//		for (Service current : services) {
//			//system.out.println(current.getOperations().get(0).getHttpUrl());
//		}
//	}
	
	public void testTmp4() {
		
		//String result = new ScenarioManagementAgent().listAgentsAsJson();
		//String result = new ScenarioManagementAgent().listScenariosAsJson();
		////system.out.println(result);
	}
}