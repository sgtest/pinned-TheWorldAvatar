package uk.ac.cam.cares.jps.powerplant;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cares.jps.base.discovery.AgentCaller;


/**
 * Servlet implementation class PowerPlantWrapperAgent
 */
@WebServlet("/powerplant/calculateemission")
public class PowerPlantAgent extends HttpServlet {

	private static final long serialVersionUID = 2796334308068192311L;
	private Logger logger = LoggerFactory.getLogger(PowerPlantAgent.class);
	
	OntModel jenaOwlModel = null;
	private DatatypeProperty numval = null;
	private OntClass scalarvalueclass = null;
	public static String baseURL = null;
	ArrayList<String> cpirilist = new ArrayList<String>();
	
    HashMap<String, String> hmap = new HashMap<String, String>();


	
	
	public void savefile(OntModel jenaOwlModel, String filePath2) throws URISyntaxException, FileNotFoundException {

		FileOutputStream out = new FileOutputStream(filePath2);

		Collection errors = new ArrayList();
		jenaOwlModel.write(out, "RDF/XML-ABBREV");

		System.out.println("File saved with " + errors.size() + " errors.");
	}
	
	public void initOWLClasses(OntModel jenaOwlModel) {
		numval = jenaOwlModel.getDatatypeProperty("http://www.theworldavatar.com/ontology/ontocape/upper_level/system.owl#numericalValue");
		scalarvalueclass = jenaOwlModel.getOntClass("http://www.theworldavatar.com/ontology/ontocape/upper_level/system.owl#ScalarValue");	
		
	}
	

	
	
	
	public void doConversion(OntModel jenaOwlModel,String iri,String jsonresultstring) throws JSONException, IOException
	{
	    /*Adding elements to HashMap*/
	    hmap.put("CO", "ChemSpecies_Carbon__monoxide");
	    hmap.put("CO2", "ChemSpecies_Carbon__dioxide");
	    hmap.put("NO2", "ChemSpecies_Nitrogen__dioxide");
	    hmap.put("HC", "PseudoComponent_Unburned_Hydrocarbon");
	    hmap.put("NOx", "PseudoComponent_Nitrogen__oxides");
	    
	    

	    JSONObject jsonObject=new JSONObject(jsonresultstring);
		
		//JSONObject jsonObject = parseJSONFile(outputfiledir); (used after the format of json file is fixed )
		Double molecularvalue = jsonObject.getJSONObject("mixture").getJSONObject("molmass").getDouble("value")*1000;
		Double Cpvalue = jsonObject.getJSONObject("mixture").getJSONObject("cp").getDouble("value");
		Double temperaturevalue = jsonObject.getJSONObject("mixture").getJSONObject("temperature").getDouble("value")-273.15;
		Double massfluxvalue = jsonObject.getJSONObject("mixture").getJSONObject("massflux").getDouble("value");
		Double densityvalue = jsonObject.getJSONObject("mixture").getJSONObject("density").getDouble("value");
		
		int valueoftotalpollutant = jsonObject.getJSONArray("pollutants").length();
		
		Individual valueofmassflowrate = jenaOwlModel.getIndividual(iri.split("#")[0] +"#V_massF_WasteStream-001");
		valueofmassflowrate.setPropertyValue(numval,jenaOwlModel.createTypedLiteral(massfluxvalue));
		
		Individual valueofdensityrate = jenaOwlModel.getIndividual(iri.split("#")[0] +"#V_Density_MaterialInWasteStream-001");
		valueofdensityrate.setPropertyValue(numval,jenaOwlModel.createTypedLiteral(densityvalue));
		
		Individual valueoftemperature = jenaOwlModel.getIndividual(iri.split("#")[0] +"#V_Temperature_MaterialInWasteStream-001");
		valueoftemperature.setPropertyValue(numval,jenaOwlModel.createTypedLiteral(temperaturevalue));
		
		Individual valueofcombinedmolecularmass = jenaOwlModel.getIndividual(iri.split("#")[0] +"#V_ChemSpecies_Combined_MolecularMass");
		valueofcombinedmolecularmass.setPropertyValue(numval,jenaOwlModel.createTypedLiteral(molecularvalue));
		
		Individual valueofcombinedheatcapacity = jenaOwlModel.getIndividual(iri.split("#")[0] +"#V_Cp_MaterialInWasteStream-001");
		valueofcombinedheatcapacity.setPropertyValue(numval,jenaOwlModel.createTypedLiteral(Cpvalue));
		
		for (int b = 0; b < valueoftotalpollutant; b++) {
			String parametername = jsonObject.getJSONArray("pollutants").getJSONObject(b).getString("name");
			Double parametervalue = jsonObject.getJSONArray("pollutants").getJSONObject(b).getDouble("value")*1000;

			Individual valueofspeciesemissionrate = jenaOwlModel.getIndividual(iri.split("#")[0] + "#V_" + hmap.get(parametername) + "_EmissionRate");
			valueofspeciesemissionrate.setPropertyValue(numval, jenaOwlModel.createTypedLiteral(parametervalue));
		}
	}
		
	
    //temporarily unused
	public static JSONObject parseJSONFile(String filename) throws JSONException, IOException {
        String content = new String(Files.readAllBytes(Paths.get(filename)));
        JSONObject content2 = new JSONObject(content.trim());
        return content2;
    }
	
	public void startConversion(String iriOfPlant,String jsonresultstring) throws Exception {
				
		jenaOwlModel = ModelFactory.createOntologyModel();	
		jenaOwlModel.read(iriOfPlant, null);


				initOWLClasses(jenaOwlModel);

				doConversion(jenaOwlModel,iriOfPlant,jsonresultstring);

				
				String filePath2= iriOfPlant.replaceAll("http://www.theworldavatar.com/kb", "C:/TOMCAT/webapps/ROOT/kb").split("#")[0]; //update the file locally
				System.out.println("filepath2= "+filePath2);
				
				/** save the updated model file */
				savefile(jenaOwlModel, filePath2);

	}
	
	
	public static synchronized ResultSet query(String sparql, OntModel model) {
		Query query = QueryFactory.create(sparql);
		QueryExecution queryExec = QueryExecutionFactory.create(query, model);
		ResultSet rs = queryExec.execSelect();   
		//reset the cursor, so that the ResultSet can be repeatedly used
		ResultSetRewindable results = ResultSetFactory.copyResults(rs);    
		//ResultSetFormatter.out(System.out, results, query); ?? don't know why this is needed to be commented
		return results;
	}
	

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    
	    
		JSONObject joforrec = AgentCaller.readJsonParameter(request);
		String iri = null;
		String iri2 = null;
		try {
			iri = joforrec.getString("reactionmechanism");
			iri2 = joforrec.getString("plant");

		} catch (JSONException e1) {
			logger.error(e1.getMessage(), e1);
			e1.printStackTrace();
		} 
		System.out.println("data got= "+iri+" and "+iri2);
		
		
	    String resultjson = AgentCaller.executeGet("JPS/SRMAgent", "query", joforrec.toString());
	    
	    String jsonsrmresult = null;
	    try {
			jsonsrmresult = new JSONObject(resultjson).getString("file");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		
		try {
	
			//update the emission and other information into the plant owl file
			startConversion(iri2,jsonsrmresult); //convert to update value
			
			//try to query the owl file to get the waste stream inside it 
			String wastestreamInfo = "PREFIX cp:<http://www.theworldavatar.com/ontology/ontocape/chemical_process_system/CPS_realization/plant.owl#> " 
					+ "PREFIX j2:<http://www.theworldavatar.com/ontology/ontocape/upper_level/system.owl#> "
					+ "PREFIX j3:<http://www.theworldavatar.com/ontology/ontocape/upper_level/technical_system.owl#> "
					+ "PREFIX j4:<http://www.theworldavatar.com/ontology/meta_model/topology/topology.owl#> "
					+ "SELECT ?wastestream "
					+ "WHERE {?entity  a  cp:Plant  ." 
					+ "?entity   j2:hasSubsystem ?chimney ."
					+ "?chimney  j3:realizes  ?releaseprocess  ."
					+ "?releaseprocess   j4:hasOutput ?wastestream ."
					+ "}";
					

			ResultSet rs_plant = PowerPlantAgent.query(wastestreamInfo,jenaOwlModel); 
					
			for (; rs_plant.hasNext();) {			
				QuerySolution qs_p = rs_plant.nextSolution();

				Resource cpiri = qs_p.getResource("wastestream");
				String valueiri = cpiri.toString();
				logger.info("query result= "+valueiri);
				cpirilist.add(valueiri);
			}
			
			
			JSONObject jo=null;
			if(cpirilist.size()==1)
			{			
				 jo = new JSONObject().put("waste", cpirilist.get(0));
				
			}	
			logger.info("message to sent= "+jo.toString());
			response.getWriter().write(jo.toString());
			cpirilist.clear();
			
		} catch (Exception e1) {
			logger.error(e1.getMessage(),e1);
		}
		
		
	}
	
	
}