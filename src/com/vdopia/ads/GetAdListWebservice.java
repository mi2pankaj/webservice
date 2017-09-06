package com.vdopia.ads;


import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.commons.io.FileUtils;

import vlib.Excel2Html;
import vlib.FileLib;
import vlib.MobileTestClass_Methods;
import vlib.XlsLib;


@Path("/adlist")
public class GetAdListWebservice 
{
	//static String home = "/Users/user/Documents/ProjectAdServingWebservice/VdopiaAdserving";
	static String webserviceHome = GenericLib.GetValueFromConfig("webserviceHome");

	@Path("{deviceID}/{publisherEmail}")
	@GET
	@Produces("application/xml")

	@SuppressWarnings({ "finally" })
	public String GetAdURLs(@PathParam("deviceID") String deviceID, @PathParam("publisherEmail") String publisherEmail)
	{
		System.out.println();
		System.out.println("******* Received Test Request From Device ID: "+deviceID + " For Publisher Email: "+publisherEmail +" At Time: "+MobileTestClass_Methods.DateTimeStamp("yyMMdd_hhmmss")+" *********");	
		System.out.println();

		String testDataFile="";
		String result="";

		//Getting the location of dummy result file --> Apache Test Results Directory 
		String apacheHome = GenericLib.GetValueFromConfig("apacheHome");
		String dummyResultExcel_Apache = apacheHome.concat("/webapps/TestResults/").concat(deviceID).concat("/dummyresultfile_"+MobileTestClass_Methods.DateTimeStamp("yyMMdd_hhmmss")+".xls");

		//Check if dummy excel file exists if not then create it.
		if(FileLib.CreateNewFile(dummyResultExcel_Apache))
		{
			System.out.println("Found the result directory location for creating dummy excel file in apache: "+dummyResultExcel_Apache);
		}
		else
		{
			System.out.println("There are some problems while finding the result directory location for creating dummy excel file in apache: "+dummyResultExcel_Apache);
		}
		
		//Getting the location of dummy result file --> Webservice Test Results Directory 		
		String dummyResultExcel_WebService = webserviceHome.concat("/Test_Results/").concat(deviceID).concat("/dummyresultfile_"+MobileTestClass_Methods.DateTimeStamp("yyMMdd_hhmmss")+".xls");
		System.out.println("Found the result directory location for creating dummy excel file in webservice: "+dummyResultExcel_WebService);

		try
		{						
			testDataFile = webserviceHome.concat("/TestData/"+deviceID+"_TestData.xls");			

			String [][] recordOutput = GenericLib.GetSQLRecords(publisherEmail);

			if(recordOutput !=null && (!(recordOutput.length <2))) 
			{	
				//Write SQL Records in excel sheet
				FileLib.WritingMySQLRecordsInExcelSheet(testDataFile, recordOutput);

				//Write test urls
				if(publisherEmail.contains("@"))
				{
					//write test url - channel test pages
					FileLib.WritingTestURLInExcelSheet(testDataFile, deviceID, "channel");
				}
				else
				{
					//write test url - campaign test pages
					FileLib.WritingTestURLInExcelSheet(testDataFile, deviceID, "campaign");
				}

				//Writing Description in excel sheet:
				System.out.println("Writing Test Descripton in Test Data File For Device ID: "+deviceID + " For Publisher: "+publisherEmail);
				XlsLib.WriteTestDescription(testDataFile);

				//Copy Test Data File In Test Result Folder
				String timestamp = MobileTestClass_Methods.DateTimeStamp("yyMMdd_hhmmss");
				File testResultFile = new File(webserviceHome.concat("/Test_Results/"+deviceID+"/"+deviceID+"_Results_"+timestamp+".xls"));
				FileUtils.copyFile(new File(testDataFile), testResultFile);

				String [][] arr = FileLib.FetchDataFromExcelSheet(testDataFile, "Test_URLs", "Ads_Duration", "Ad_Format");

				//Getting Default Ad Duration From Config File
				int nonVideoAdDuration = Integer.parseInt(GenericLib.GetValueFromConfig("bannerDelay"));
				int defaultVideoAdDuration = Integer.parseInt(GenericLib.GetValueFromConfig("nonBannerDelay"));;

				//This is the server processing time, app has to wait for this extra time other than ad duration to sync with server. 
				int appWaitSecToSyncWithServer = Integer.parseInt(GenericLib.GetValueFromConfig("appWaitSecToSyncWithServer"));

				//Forming xml string for returning result
				for(int i=0; i<arr.length; i++)
				{
					result = result + "<addetails>";

					result = result + "<testurl>"+ arr[i][0] + "</testurl>" + "\n";

					String adFormat = arr[i][2].toString();

					//Adding Ad Duration For Each Ad Type
					if(adFormat.equalsIgnoreCase("video") || adFormat.equalsIgnoreCase("vdobanner") || 
							adFormat.equalsIgnoreCase("leadervdo")|| adFormat.equalsIgnoreCase("vastfeed") || adFormat.equalsIgnoreCase("inview"))
					{
						if(arr[i][1].equalsIgnoreCase("0"))
						{
							result = result + "<adduration>"+ String.valueOf(defaultVideoAdDuration + appWaitSecToSyncWithServer) + "</adduration>" + "\n";
						}
						else
						{
							result = result + "<adduration>"+ String.valueOf(Integer.parseInt(arr[i][1]) + appWaitSecToSyncWithServer) + "</adduration>" + "\n";
						}
					}
					else
					{
						result = result + "<adduration>"+ String.valueOf(nonVideoAdDuration + appWaitSecToSyncWithServer) + "</adduration>" + "\n";
					}

					result = result + "</addetails>";
				}

				System.out.println(" AdList Request XML: ");
				System.out.println(result);

				//fetch required data from excel sheet to initialize constructor -- runner
				String [][]mobileData = FileLib.FetchDataFromExcelSheet(testDataFile, "Ad_Format", "Test_URLs", "Channel_ID", "Ads_Duration"); 

				ExecutorService executor = Executors.newFixedThreadPool(2);
				Runnable runner = new ResultGeneration(deviceID, mobileData, testResultFile, publisherEmail);

				//Submit run() to executor
				executor.submit(runner);

				//executor.shutdown();
			}
			else
			{	
				GetResultsWebservice rs = new GetResultsWebservice();

				String text = "PUBLISHER";

				if(!publisherEmail.contains("@"))
				{
					text = "CAMPAIGN ID";
				}

				String dummyURL = rs.WriteDummyResultHTMLFile("NO RECORDS FOUND FOR THIS "+ text +": "+publisherEmail);

				result = "<addetails>" +  "<testurl>"+ dummyURL + "</testurl>" + "<adduration>" + "10" + "</adduration>" + "</addetails>";
				System.out.println("No record found for "+text+": "+publisherEmail + " for device id: "+deviceID);

				//Creating a dummy result excel file 
				GenericLib.CreateNoRecordExcelSheet(dummyResultExcel_WebService, publisherEmail, false);
				GenericLib.CreateNoRecordExcelSheet(dummyResultExcel_Apache, publisherEmail, false);

				//converting dummy result excel file into html in apache, to be retrieved later on
				String htmlResults = dummyResultExcel_Apache.toString().replace("xls", "html");
				String timestamp = MobileTestClass_Methods.DateTimeStamp("dd-MMM-yyyy hh:mm:ss");
				Excel2Html.GenerateWebServiceHTMLResult(dummyResultExcel_Apache.toString(), htmlResults, timestamp);

				System.out.println("Dummay excel result file is created mentioning no records found in results.");
			}
		}
		catch(Exception e)
		{
			GetResultsWebservice rs = new GetResultsWebservice();
			String dummyURL = rs.WriteDummyResultHTMLFile("Exception occured while getting Ad List for device id: "+deviceID + " for publisher: "+publisherEmail);

			result = "<addetails>" +  "<testurl>"+ dummyURL + "</testurl>" + "<adduration>" + "10" + "</adduration>" + "</addetails>";
			System.out.println("Exception occured while getting Ad List for device id: "+deviceID);

			//Creating a dummy result excel file 
			GenericLib.CreateNoRecordExcelSheet(dummyResultExcel_WebService, publisherEmail, true);
			GenericLib.CreateNoRecordExcelSheet(dummyResultExcel_Apache, publisherEmail, true);

			//converting dummy result excel file into html in apache directory, to be retrieved later on
			String htmlResults = dummyResultExcel_Apache.toString().replace("xls", "html");
			String timestamp = MobileTestClass_Methods.DateTimeStamp("dd-MMM-yyyy hh:mm:ss");
			Excel2Html.GenerateWebServiceHTMLResult(dummyResultExcel_Apache.toString(), htmlResults, timestamp);

			System.out.println("Dummay excel result file is created mentioning exception in results.");
		}
		finally
		{			
			return "<ResultNode>" + "<result>" + result + "</result>" + "</ResultNode>";
		}
	}


}
