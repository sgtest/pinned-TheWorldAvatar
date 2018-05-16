package uk.ac.cam.cares.jps.discovery.api;

import java.io.IOException;
import java.util.List;

public interface IAgentCommunication {

	List<TypeIRI> searchAgents(AgentRequest agentRequest);
	
	AgentResponse callAgent(AgentRequest agentRequest);
	
	void registerAgent(Agent agent) throws IOException;
	
	void deregisterAgent(TypeIRI agentAddress) throws IOException;
	
	/**
	 * This method returns the names of all agents registered in JPS.
	 * Only use this method for test purposes!
	 *  
	 * @return
	 */
	List<TypeIRI> getAllAgentNames();
}