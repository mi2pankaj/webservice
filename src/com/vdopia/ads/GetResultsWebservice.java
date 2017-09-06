package com.vdopia.ads;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import vlib.FileLib;
import vlib.MobileTestClass_Methods;


@Path("/getresults")

public class GetResultsWebservice 
{	

	@Path("{deviceID}")
	@GET
	@Produces("application/xml")

	@SuppressWarnings("finally")
	public String GetTestResult(@PathParam("deviceID") String deviceID)
	{
		System.out.println();
		System.out.println("******* Received Test Result Request From Device ID: "+deviceID + " At Time: "+MobileTestClass_Methods.DateTimeStamp("yyMMdd_hhmmss")+" *********");	
		System.out.println();

		String resultURL = "";
		String ipPort = GenericLib.GetValueFromConfig("ipPort");

		String apacheHome = GenericLib.GetValueFromConfig("apacheHome");

		try
		{
			System.out.println("Searching the latest html test result file for device id: "+deviceID +" in apache public directory....");

			String apacheResultDirectory = apacheHome.concat("/webapps/TestResults/").concat(deviceID);
			
			//Getting result file from web service test results folder
			String expectedHTMLResultFile = getResultFileNameFromWebServiceProjectDir(deviceID);

			if(expectedHTMLResultFile != null)
			{
				expectedHTMLResultFile = expectedHTMLResultFile + ".html";

				//Searching result file (after getting from webservice folder) in apache test results directory
				if(WaitForResultFile_ApacheDir(apacheResultDirectory, expectedHTMLResultFile, 15, deviceID))
				{
					resultURL = "http://"+ ipPort +"/TestResults/"+deviceID+"/"+expectedHTMLResultFile;
					System.out.println("Device ID: " +deviceID + " Has Received Result File URL: "+ resultURL);
				}
				else
				{
					String dummyResult = "http://"+ ipPort +"/TestResults/"+deviceID+"/"+expectedHTMLResultFile;

					dummyResult = "Check Results Later At: " + dummyResult;
					resultURL = WriteDummyResultHTMLFile(dummyResult);

					System.out.println("Result is not generated yet, Please check after some time.");
					System.out.println("Dummy Result: ");
					System.out.println(dummyResult);
				}
			}
			else
			{
				String dummyResult = "There are some processing errors, please check the entered value. ";
				resultURL = WriteDummyResultHTMLFile(dummyResult);

				System.out.println("Result is not generated yet, Please check after some time.");
				System.out.println("Dummy Result: ");
				System.out.println(dummyResult);
			}
		}
		catch(Exception e)
		{
			String dummyResult = "There are some processing errors while generating results. ";
			resultURL = WriteDummyResultHTMLFile(dummyResult);

			System.out.println("Exception occured while searching the result file for device id: "+deviceID);
			System.out.println("Dummy Result: ");
			System.out.println(dummyResult);
		}
		finally
		{
			System.out.println("Result URL for device id: "+deviceID +" is being returned as: "+resultURL );
			return "<ResultNode>" + "<result>" + "<url>" + resultURL + "</url>" + "</result>" + "</ResultNode>";
		}

	}


	@SuppressWarnings("finally")
	public String WriteDummyResultHTMLFile(String text)
	{
		String apacheHome = GenericLib.GetValueFromConfig("apacheHome");

		try
		{
			String strText = "<html><body><h4 align=\"center\"><a>"+ text +"</a></h4></body></html>";
			FileLib.WriteTextInFile(apacheHome.concat("/webapps/TestResults/dummypage/dummypage.html"), strText);
		}
		catch(Exception e)
		{
			System.out.println("Exception handled while writing dummy result page in apache/webapps/testresults/dummypage.html. "+e.getMessage());
		}
		finally
		{
			String ipPort = GenericLib.GetValueFromConfig("ipPort");

			System.out.println("Dummy Result Page: " + "http://"+ ipPort +"/TestResults/dummypage/dummypage.html");
			return "http://"+ ipPort +"/TestResults/dummypage/dummypage.html";
		}
	}


	//Getting file name from webservice directory / test results
	@SuppressWarnings("finally")
	public String getResultFileNameFromWebServiceProjectDir(String deviceID)
	{
		String resultFile = "";
		String webserviceHome = GenericLib.GetValueFromConfig("webserviceHome");

		try
		{
			//Get the last modified result file from test results directory of webservice folder
			String resultDir = webserviceHome.concat("/Test_Results/"+deviceID);
			resultFile = FileLib.GetLastModifiedFile(resultDir, "xls");

			if(resultFile.equalsIgnoreCase("NO_FILE"))
			{
				resultFile = null;
				System.out.println("Test Result XLS is not created yet at dir: "+resultDir + " for device id: " +deviceID);
			}
			else
			{
				resultFile = GenericLib.SplitFileNameFromDirLocation(resultFile);
				resultFile = resultFile.substring(0, resultFile.indexOf("."));
			}
		}
		catch(Exception e)
		{
			resultFile = null;
			System.out.println("Exception handled by method: GetResultFile. "+e.getMessage());
		}
		finally
		{
			System.out.println("Test Result File: "+resultFile + " will be used to find the final result file for device id: "+deviceID);
			return resultFile;
		}
	}


	@SuppressWarnings("finally")
	public boolean WaitForResultFile_ApacheDir(String apacheResultDirectory, String expectedHTMLResultFile, int waitDuration, String deviceID)
	{
		boolean flag = false;
		try
		{
			for(int i=0; i<waitDuration; i++)
			{
				if(FileLib.CheckFileInDirectory(apacheResultDirectory, expectedHTMLResultFile))
				{
					flag = true;
					System.out.println("Result File Found For Device ID: " +deviceID);
					break;
				}
				else
				{
					System.out.println("Searching the result file: "+expectedHTMLResultFile +" for device id: " +deviceID + " .......... ");
					Thread.sleep(10000);
				}
			}
		}
		catch(Exception e)
		{	
			flag = false;
			System.out.println("Exception occured while searching the result file for device id: "+deviceID + "\n" +e.getMessage());
		}
		finally
		{
			return flag;
		}
	}

}
