package com.capitalone.dashboard.collector;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.capitalone.dashboard.model.MainSpring;
import com.capitalone.dashboard.model.MainSpringProject;
import com.capitalone.dashboard.repository.MainSpringRepository;
import com.capitalone.dashboard.util.Supplier;
import com.cognizant.mainspring.client.MainSpringWebServiceClient;

@Component
public class DefaultMainSpringClient implements MainSpringClient {
	private static final Log LOG = LogFactory.getLog(DefaultMainSpringClient.class);
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMainSpringClient.class);
	// private final DateFormat QUERY_DATE_FORMAT = new
	// SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
	// private static final String DATE_FORMAT = "dd-MMM-yyyy HH:mm:ss";

	private static final String PRJ_URL_RESOURCES = "ProjectServiceProvider";
	private static final String AUTH_URL_RESOURCES = "TokenServiceProvider";
	private static final String ITEM_URL_RESOURCES = "WSEformItemHandler";

	private final String UserName;
	private final String PassWord;
	private final MainSpringRepository mainSpringRepository;
	private final String projectFilterName;
	private final String projectFilterDate;
	private final String itemFilterDate;
//	 private final String itemFilterName;
	// private static final String URL_RESOURCE_SIZING_MEASURES =
	// "AAD/results?sizing-measures=(10151,68001)&snapshots=-1&applications=";

	private final RestOperations rest;
	// private final HttpEntity<String> httpHeaders;
	// private final CastSettings castSettings;

	@Autowired
	public DefaultMainSpringClient(Supplier<RestOperations> restOperationsSupplier, MainSpringSettings settings,
			MainSpringRepository mainSpringRepository) {
		this.UserName = settings.getUsername();
		this.PassWord = settings.getPassword();
		this.mainSpringRepository = mainSpringRepository;
		this.projectFilterName = settings.getProjectFilterName();
		this.projectFilterDate = settings.getProjectFilterDate();
		this.itemFilterDate = settings.getItemFilterDate();
	//	this.itemFilterName= settings.getItemFilterName();
		LOGGER.info("Rest settings.getUsername(): " + settings.getUsername());
		LOGGER.info("MainSpringCollectorTask Start");
		this.rest = restOperationsSupplier.get();
		// this.castSettings = settings;

	}

	@Override
	public List<MainSpringProject> getProjects(String instanceUrl ) {
		List<MainSpringProject> projects = new ArrayList<>();
		String url = instanceUrl + PRJ_URL_RESOURCES;
		LOGGER.info("Rest url: " + url);
		// "https://sandboxpra.cognizant.com/axis/services/WSEformItemHandler?wsdl")

		if (rest != null) {
			LOGGER.info("instanceUrl: " + instanceUrl);
		}
		MainSpringWebServiceClient mainSpringWSClient = new MainSpringWebServiceClient();

		try {
			String authToken = mainSpringWSClient.DoLogin(url, UserName, PassWord);

			if (authToken.isEmpty())
				return new ArrayList<MainSpringProject>();
			// url,mainSpringAuthToken,"Prj", 52207, "Dft", "Open","1-Aug-2016
			// 00:00:00"
			url = instanceUrl + PRJ_URL_RESOURCES;
			String result;
			result = mainSpringWSClient.getProjectsChangedList(url, authToken, projectFilterName, projectFilterDate);
		//	LOGGER.info("result: " + result);
			if (result.isEmpty())
				return new ArrayList<MainSpringProject>();
			projects = this.perseProjectXML(result, instanceUrl );
/*			projects=new ArrayList<MainSpringProject>();
			MainSpringProject	 mainSpringProject = new MainSpringProject();

				mainSpringProject.setProjectId("52207");
				mainSpringProject.setProjectCode("1000096624");
				mainSpringProject.setProjectName("Orkla AO-HFM");

				mainSpringProject.setInstanceUrl(instanceUrl);
				mainSpringProject.setEnabled(false);
				 
				projects.add(mainSpringProject);*/
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			LOG.error("MalformedURLException: " + url, e);
		//	e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			LOG.error("RemoteException: " + url, e);
		}

		return projects;
	}

	private List<MainSpringProject> perseProjectXML(String xmlStr, String instanceUrl ) {
		List<MainSpringProject> mainSpringProjects = new ArrayList<MainSpringProject>();
		MainSpringProject mainSpringProject = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xmlStr));

			Document doc;

			doc = db.parse(is);
			NodeList nodes = doc.getElementsByTagName("Project");
			for (int i = 0; i < nodes.getLength(); i++) {
				// Element element = (Element) nodes.item(i);

				Node nNode = nodes.item(i);

				// System.out.println("\nCurrent Element :" +
				// nNode.getNodeName());

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;
				    mainSpringProject = new MainSpringProject();

					mainSpringProject
							.setProjectId(eElement.getElementsByTagName("ID").item(0).getFirstChild().getNodeValue());
					mainSpringProject.setProjectCode(
							eElement.getElementsByTagName("CODE").item(0).getFirstChild().getNodeValue());
					mainSpringProject.setProjectName(
							eElement.getElementsByTagName("Name").item(0).getFirstChild().getNodeValue().trim());
				 
					mainSpringProject.setInstanceUrl(instanceUrl);
					mainSpringProject.setEnabled(false);
					// mainSpring.setTotalItems(totalItems);
					mainSpringProjects.add(mainSpringProject);

					mainSpringProject = null;
				}

			}
		} catch (SAXException | IOException e) {
			// TODO Auto-generated catch block
			LOG.error("SAXException Could not parse response : " + xmlStr, e);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			LOG.error("ParserConfigurationException Could not parse response : " + xmlStr, e);
		}

		return mainSpringProjects;

	}
	 @SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts") // agreed PMD, fixme
	private List<MainSpring> perseItemDetailsXML(String xmlStr, String instanceUrl, String projectName, int projectId,
			ObjectId collectorItemId, String itemCode, String itemName,String statusFieldName ) {
		List<MainSpring> mainSpringData = new ArrayList<MainSpring>();
		MainSpring mainSpring = null;
		try {
			 DocumentBuilderFactory dbf =
			            DocumentBuilderFactory.newInstance();
			        DocumentBuilder db = dbf.newDocumentBuilder();
			        InputSource is = new InputSource();
			        is.setCharacterStream(new StringReader(xmlStr));

			        Document doc;
					
						doc = db.parse(is);
						 NodeList nodes = doc.getElementsByTagName("Item");
						  for (int i = 0; i < nodes.getLength(); i++) {
					        //   Element element = (Element) nodes.item(i);

					           Node nNode = nodes.item(i);

					   	//	System.out.println("\nCurrent Element :" + nNode.getNodeName());

					   		if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					   			Element eElement = (Element) nNode;
					   		//	System.out.println("ID : " + eElement.getElementsByTagName("ID").item(0).getFirstChild().getNodeValue());
						   		String itemId= eElement.getElementsByTagName("ID").item(0).getFirstChild().getNodeValue();
						   	
					   		 NodeList nodeLabelInfos = eElement.getElementsByTagName("LabelInfo");
					   		 Node nodeLabelInfo = nodeLabelInfos.item(0);
					   			Element eElementLabelInfo = (Element) nodeLabelInfo;
					   		 NodeList nodeLabels = eElementLabelInfo.getElementsByTagName("Label");
					   		 NodeList nodeValues = eElementLabelInfo.getElementsByTagName("Value");
					   		mainSpring = new MainSpring();
							mainSpring.setArtifactCode(itemCode);
							mainSpring.setArtifactName(itemName);
							mainSpring.setProjectID(String.valueOf(projectId));
							mainSpring.setProjectName(projectName);
							mainSpring.setUrl(instanceUrl);
							mainSpring.setItemId(itemId);
							mainSpring.setCollectorItemId(collectorItemId);
					   		 for (int lblInfoId = 0; lblInfoId < nodeLabels.getLength(); lblInfoId++) {
					   			 Node nodeLabel = nodeLabels.item(lblInfoId);
					   			 Node nodenodeValue = nodeValues.item(lblInfoId);
					   						   		
					   			 String fieldName = nodeLabel.getFirstChild().getNodeValue();
					   			 String fieldValue = nodenodeValue.getFirstChild().getNodeValue();
					   		
						   		if("Name".equals(fieldName))
						   			mainSpring.setItemName(fieldValue);
						   		 
						   	 if("ID".equals(fieldName))
						   			mainSpring.setItemCode(fieldValue);
						   	 if(statusFieldName.equals(fieldName))
						   			 mainSpring.setItemStatus(fieldValue);
						   		// else
						   		//	continue;
						
								}
					   		 MainSpring temp=isNewItem(String.valueOf(projectId), mainSpring);
					   		if (temp==null) {
								mainSpringData.add(mainSpring);
					   		 }else{
					   			mainSpring.setId(temp.getId());
					   			mainSpring.setCollectorItemId(temp.getCollectorItemId());
					   			mainSpringData.add(mainSpring);
					   		 }
					   	 
					   		
					   		}

					           
					        }
					} catch (SAXException | IOException e) {
						// TODO Auto-generated catch block
					//	e.printStackTrace();
					} catch (ParserConfigurationException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
		return mainSpringData;
	}
	 @SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts") // agreed PMD, fixme
	private List<MainSpring> perseItemXML(String xmlStr, String url, String instanceUrl, String projectName, int projectId,
			ObjectId collectorItemId, String itemCode, String itemName) {
		List<MainSpring> mainSpringData = new ArrayList<MainSpring>();
		//MainSpring mainSpring = null;
		String statusFieldName="State";
		if("dft".equalsIgnoreCase(itemCode)){
			statusFieldName = "Status";
		}
		String[] labels = {"Name","ID",statusFieldName};
		 LOG.info("itemCode : " + itemCode+ " statusFieldName:" +statusFieldName);
		List<Integer> itemIds = new ArrayList<Integer>();
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xmlStr));

			Document doc;

			doc = db.parse(is);
			NodeList nodes = doc.getElementsByTagName("Item");
			for (int i = 0; i < nodes.getLength(); i++) {
				// Element element = (Element) nodes.item(i);

				Node nNode = nodes.item(i);

				// System.out.println("\nCurrent Element :" +
				// nNode.getNodeName());

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;
					 
					itemIds.add(Integer.parseInt(eElement.getElementsByTagName("ID").item(0).getFirstChild().getNodeValue()));
			 
				}

			}
			if(!itemIds.isEmpty())
			{
				MainSpringWebServiceClient mainSpringWSClient = new MainSpringWebServiceClient();
				String authToken = mainSpringWSClient.DoLogin(url, UserName, PassWord);

				if (authToken.isEmpty())
					return new ArrayList<MainSpring>();
				// url,mainSpringAuthToken,"Prj", 52207, "Dft", "Open","1-Aug-2016
				// 00:00:00"
				int[] ret = new int[itemIds.size()];
			    for (int i=0; i < ret.length; i++)
			    {
			        ret[i] = itemIds.get(i).intValue();
			    }
				String result = mainSpringWSClient.getEFormItemDetails(url,authToken,itemCode,ret,labels );
					 
			 	LOGGER.info("result item detail xml: " + result);
				if (result.isEmpty())
					return new ArrayList<MainSpring>();
				mainSpringData	= perseItemDetailsXML(result,  instanceUrl,  projectName,  projectId,
						 collectorItemId,  itemCode,  itemName,statusFieldName );
			 
			}
		} catch (SAXException | IOException e) {
			// TODO Auto-generated catch block
			LOG.error("SAXException Could not parse response : " + xmlStr, e);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			LOG.error("ParserConfigurationException Could not parse response : " + xmlStr, e);
		}

		return mainSpringData;

	}

	@Override
	public List<MainSpring> currentMainSpringAnalysis(MainSpringProject project) {
		String url = project.getInstanceUrl() + AUTH_URL_RESOURCES;
		// Object grade = "0.00";
		// DecimalFormat df2 = new DecimalFormat(".##");
		// String formattedValue="0";
		// String shortName ="";
		List<MainSpring> mainspringData = new ArrayList<MainSpring>();
		LOGGER.info("currentMainSpringQuality url2: " + url);
		LOGGER.info("currentMainSpringQuality ID: " + project.getProjectId() + " NAME: " + project.getProjectName());
		try {
			// JSONArray jsonArray = parseAsArray(url);
			MainSpringWebServiceClient mainSpringWSClient = new MainSpringWebServiceClient();
			String authToken = mainSpringWSClient.DoLogin(url, UserName, PassWord);

			if (authToken.isEmpty())
				return new ArrayList<MainSpring>();
			// url,mainSpringAuthToken,"Prj", 52207, "Dft", "Open","1-Aug-2016
			// 00:00:00"
			url = project.getInstanceUrl() + ITEM_URL_RESOURCES;
			String result = mainSpringWSClient.getEFormItemListWithFilter(url, authToken, "prj",
					Integer.parseInt(project.getProjectId()), "dft", "OpsHubAllIssues", itemFilterDate);
		//	LOGGER.info("result: " + result);
			if (!result.isEmpty())
			{
				mainspringData = this.perseItemXML(result,url, project.getInstanceUrl(), project.getProjectName(),
						Integer.parseInt(project.getProjectId()), project.getId(), "dft", "Defect");

			}
		 	authToken = mainSpringWSClient.DoLogin(url, UserName, PassWord);
			result = mainSpringWSClient.getEFormItemListWithFilter(url, authToken, "prj",
					Integer.parseInt(project.getProjectId()), "AUSRST", "OpsHubAllIssues", itemFilterDate);
		//	LOGGER.info("result: " + result);
			if (!result.isEmpty())
			{
			List<MainSpring>	mainspringDataUS = this.perseItemXML(result,url, project.getInstanceUrl(), project.getProjectName(),
						Integer.parseInt(project.getProjectId()), project.getId(), "AUSRST", "User Story");
			
			if(!mainspringDataUS.isEmpty())
			{
				if(mainspringData ==null)mainspringData = new ArrayList<MainSpring>();
				mainspringData.addAll(mainspringDataUS);
			}
			} 
			 
			// mainSpring.setTimestamp((long) dateJson.get(TIME));
			/// codeQuality.setVersion(str(prjData, VERSION));

			//////////////////////////////////////////////////////////////////
			return mainspringData;

		} catch (RestClientException rce) {
			LOG.error(rce);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			LOG.error("NumberFormatException: ", e);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			LOG.error("MalformedURLException: " + url, e);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			LOG.error("RemoteException: " + url, e);
		}

		return null;
	}

	private MainSpring isNewItem(String projectId, MainSpring mainSpring) {
		MainSpring temp = mainSpringRepository.checkItemExists(projectId, mainSpring.getArtifactCode(),
				mainSpring.getItemId()) ;
		return temp;
	}

	@SuppressWarnings("unused")
	private Integer integer(JSONObject json, String key) {
		Object obj = json.get(key);
		return obj == null ? null : (Integer) obj;
	}

	@SuppressWarnings("unused")
	private BigDecimal decimal(JSONObject json, String key) {
		Object obj = json.get(key);
		return obj == null ? null : new BigDecimal(obj.toString());
	}

	@SuppressWarnings("unused")
	private Boolean bool(JSONObject json, String key) {
		Object obj = json.get(key);
		return obj == null ? null : Boolean.valueOf(obj.toString());
	}

}
