package com.vdopia.ads;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import vlib.Excel2Html;
import vlib.MobileTestClass_Methods;
import vlib.MobileTrackerCalculationLib;
import vlib.StringLib;


public class ResultGeneration implements Runnable 
{
	public String deviceID;
	public String tracker_StartTime;
	public String [][]mobileData;
	public File testResultFile;
	public String publisher;


	//Constructor
	@SuppressWarnings("unused")
	public ResultGeneration(String deviceID, String [][]mobileData, File testResultFile, String publisher)
	{
		try
		{	
			System.out.println();
			System.out.println("Initializing Constructors for device id: "+deviceID);

			this.deviceID = deviceID;
			this.mobileData = mobileData;
			this.testResultFile = testResultFile;
			this.publisher = publisher;

			//Initializing constructor to get config file of webservice 
			MobileTestClass_Methods ws = new MobileTestClass_Methods("webservice");
			MobileTestClass_Methods.InitializeConfiguration();

			tracker_StartTime = MobileTestClass_Methods.GetCurrentDBTime();

			System.out.println("Test Result File: "+testResultFile + ", Tracker Start Time: "+tracker_StartTime + ", Publisher: "+publisher);
			System.out.println();
		}
		catch(Exception e)
		{
			System.out.println("Exception handled by method: ResultGeneration. "+e.getMessage());
		}
	}


	//Get tracker count
	@SuppressWarnings("finally")
	public static String GetTrackerCount(String deviceID, String adFormat, String uniqueRequestParam, String trackerStartTime, int adDuration, String channelID, String publisher) 
	{
		System.out.println("Getting Tracker Count For Device ID: "+deviceID);

		String result = "";
		try
		{
			//Sleeping thread for specific ads
			System.out.println("Sleeping Thread For Ad Format: "+adFormat + " For "+ adDuration +" Sec....");
			System.out.println();
			Thread.sleep(adDuration*1000);

			//If publisher is passed as publisher email
			if(publisher.contains("@"))
			{
				result = MobileTrackerCalculationLib.MobileAdsTrackerValidation(adFormat, uniqueRequestParam, channelID, trackerStartTime);
			}

			//If publisher is passed as campaign id
			else
			{
				//parameter true of the below method is not used in calculating the tracker result in side this method
				result = MobileTestClass_Methods.MobileAds_VdopiaTrackerValidationForUIOperations(adFormat, publisher, channelID, trackerStartTime, true);
			}
		}
		catch(Exception e)
		{
			result = "Exception Occured For Device ID: "+deviceID + "\n" +e.getMessage();
			System.out.println("Exception Occured For Device ID: "+deviceID + "\n" +e.getMessage());
		}
		finally
		{
			return result;
		}

	}


	//Getting result list
	public static List<String> GenerateResult(String deviceID, String [][]mobileData, String tracker_StartTime, String publisher)
	{
		System.out.println("Result List Is Being Generated .......");
		List<String> resultList = new ArrayList<String>();
		String result = "";

		//This is the server processing time, server has to wait for this extra time so that 
		//if there is any user interaction happens then that tracker may also recorded
		int serverWaitSecToRecordUserInteractionForBanner = Integer.parseInt(GenericLib.GetValueFromConfig("serverWaitSecToRecordUserInteractionForBanner"));
		int serverWaitSecToRecordUserInteractionForNonBanner = Integer.parseInt(GenericLib.GetValueFromConfig("serverWaitSecToRecordUserInteractionForNonBanner"));

		System.out.println("Config Values Of User Interaction Time For Non Banner Ads: "+serverWaitSecToRecordUserInteractionForNonBanner + " And Banner Ads: "+serverWaitSecToRecordUserInteractionForBanner);

		for(int i=0; i<mobileData.length; i++)
		{
			try
			{
				//Checking if URL starts with http
				if(mobileData[i][1].matches("^http.*"))
				{
					//Checking if this is VAST Parent Ad
					if(!mobileData[i][1].contains("output=vast"))
					{
						String adformat = mobileData[i][0];
						String uniqueParam = StringLib.GetUniqueParamFromURL(mobileData[i][1]);
						String channelID = mobileData[i][2];
						String adDuration = mobileData[i][3];

						//Getting ad duration based on ad format
						if(adformat.equalsIgnoreCase("video") || adformat.equalsIgnoreCase("vdobanner") ||
								adformat.equalsIgnoreCase("leadervdo") || adformat.equalsIgnoreCase("vastfeed") || adformat.equalsIgnoreCase("inview"))
						{
							if(Integer.parseInt(adDuration)<1)
							{
								adDuration = GenericLib.GetValueFromConfig("nonBannerDelay");
							}
							else
							{
								adDuration = String.valueOf(Integer.parseInt(adDuration));
							}

							//Adding server processing time to ad duration
							adDuration = String.valueOf(Integer.parseInt(adDuration) + serverWaitSecToRecordUserInteractionForNonBanner);
							System.out.println("Ad Duration Calculated For Non Banner Ads = "+adDuration);
						}
						else
						{
							if(Integer.parseInt(adDuration)<1)
							{
								adDuration = GenericLib.GetValueFromConfig("bannerDelay");
							}
							else
							{
								adDuration = String.valueOf(Integer.parseInt(adDuration));
							}

							//Adding server processing time to ad duration 
							adDuration = String.valueOf(Integer.parseInt(adDuration) + serverWaitSecToRecordUserInteractionForBanner);
							System.out.println("Ad Duration Calculated For Banner Ads = "+adDuration);
						}

						//Getting tracker validation result
						result = GetTrackerCount(deviceID, adformat, uniqueParam, tracker_StartTime, Integer.parseInt(adDuration), channelID, publisher);

						System.out.println("Result for device id: "+deviceID);
						System.out.println(result);
						System.out.println("-----------------------------------------------------------------------------------------------");
					}
					else
					{
						result = "SKIP: This is a VAST - Parent Ad, wasn't served. ";
					}
				}
				else
				{
					result = "SKIP: This is not a valid URL.";
				}
			}
			catch(Exception e)
			{
				result = "FAIL: Exception occured at this iteration. ";
				System.out.println("FAIL: Exception occured at this iteration. ");
				System.out.println("Exception handled by method: GenerateResult. "+e.getMessage());
			}
			finally
			{
				resultList.add(result);
			}
		}

		return resultList;
	}


	//This is only for testing
	public static List<String> CreateList()
	{
		List<String> arr = new ArrayList<String>();

		for(int i=1; i<=20 ;i++)
		{
			arr.add(String.valueOf(i));
		}

		return arr;
	}


	//This method is executed by executor thread to do ad serving tracker validation 
	@Override
	public void run() 
	{
		try
		{
			System.out.println("******** Webservice Result Generation Started At: " +MobileTestClass_Methods.DateTimeStamp("yyMMdd_hhmmss") + " ************");

			//get the result list after validating trackers ... passing all constructor values to this method
			List<String> resultList = GenerateResult(deviceID, mobileData, tracker_StartTime, publisher);

			System.out.println("******* Test Results are being written for device id: "+deviceID + " **********");

			MobileTestClass_Methods.WritingTestResultsInExcelSheet(testResultFile, resultList);

			//This is only for testing
			//List<String> resultList = CreateList();

			//Getting required params to be used while generating result html file from xls file
			System.out.println("Converting Results File: "+testResultFile.toString() +" in to HTML File. ");

			String htmlResults = testResultFile.toString().replace("xls", "html");

			System.out.println("HTML Result File: "+htmlResults);

			String timestamp = MobileTestClass_Methods.DateTimeStamp("dd-MMM-yyyy hh:mm:ss");

			String severNameToDisplay = GenericLib.GetValueFromConfig("serverNameToDisplayInResults");

			if(publisher.contains("@"))
			{
				timestamp = timestamp + "<br> Executed at: "+ severNameToDisplay +" <br> For Publisher: "+publisher;
			}
			else
			{
				timestamp = timestamp + "<br> Executed at: "+ severNameToDisplay +" <br> For Campaign ID: "+publisher;
			}

			//Converting excel to html format
			Excel2Html.GenerateWebServiceHTMLResult(testResultFile.toString(), htmlResults, timestamp);

			//Copy html result file from webservice results folder to apache results directory
			System.out.println("HTML Result file is being copied to Apache Public Folder... ");
			String htmlFileName = GenericLib.SplitFileNameFromDirLocation(htmlResults);

			String apacheHome = GenericLib.GetValueFromConfig("apacheHome");
			System.out.println("Apache Home: "+apacheHome);
			FileUtils.copyFile(new File(htmlResults), new File(apacheHome.concat("/webapps/TestResults/"+deviceID+"/"+htmlFileName)));
		}
		catch(Exception e)
		{
			System.out.println("Exception handled by method: run() for device id: "+deviceID + "\n" + e.getMessage());
		}
		finally
		{
			System.out.println("******** Webservice Result Generation Ended At: " +MobileTestClass_Methods.DateTimeStamp("yyMMdd_hhmmss") + " For Device ID: "+ deviceID + " *******");
		}
	}


}
