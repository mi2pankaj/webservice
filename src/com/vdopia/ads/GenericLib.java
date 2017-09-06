package com.vdopia.ads;

import java.io.File;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import com.mysql.jdbc.Connection;

import vlib.MobileTestClass_Methods;
import vlib.StringLib;

public class GenericLib 
{

	@SuppressWarnings({ "finally", "unused" })
	public static String GetValueFromConfig(String key)
	{
		String value ="";
		try
		{
			MobileTestClass_Methods ws = new MobileTestClass_Methods("webservice");
			MobileTestClass_Methods.InitializeConfiguration();

			value = MobileTestClass_Methods.propertyConfigFile.getProperty(key).toString();
		}
		catch(Exception e)
		{
			System.out.println("Exception occured while getting value of: "+key + " from config.");
		}
		finally
		{
			return value;
		}
	}


	@SuppressWarnings({ "finally", "unused" })
	public static String[][] GetSQLRecords(String publisherEmailorCampaignId)
	{
		String [][] recordOutput = null;

		try
		{			
			MobileTestClass_Methods ws = new MobileTestClass_Methods("webservice");
			MobileTestClass_Methods.InitializeConfiguration();

			Connection dbCon =  MobileTestClass_Methods.CreateSQLConnection(); 
			recordOutput = MobileTestClass_Methods.GetInputDataFromMySQL(dbCon, "iphone", publisherEmailorCampaignId);
		}
		catch(Exception e )
		{
			recordOutput = null;
		}
		finally
		{
			return recordOutput;
		}

	}


	//************ This Method Will Split The File Name From The Given HTTP URL ***********
	public static String SplitFileNameFromDirLocation(String url)
	{	
		String fileName = "";

		if (System.getProperty("os.name").toLowerCase().matches("^mac.*"))
		{
			fileName = url.substring(url.lastIndexOf("/") + 1, url.length());
		}
		else
		{
			fileName = url.substring(url.lastIndexOf("\\") + 1, url.length());
		}

		return fileName;
	}


	//************ This Method Will Create an excel sheet containing text That No Record Found ***********
	@SuppressWarnings("finally")
	public static boolean CreateNoRecordExcelSheet(String fileNameWithLocation, String inputParam, boolean exceptionFlag)
	{
		boolean flag = false;
		try
		{
			System.out.println("Writing excel sheet containing result for NO RECORD FOUND at location: "+fileNameWithLocation);

			//Getting directory location
			String directory = StringLib.SplitDirectoryFromFileLocation(fileNameWithLocation);
			File dirFile = new File(directory);
			
			//Check if directory exits, if not create it.
			if(!dirFile.exists())
			{
				System.out.println("File Location: "+dirFile.toString() + " doesn't exist, creating this directory now... " );
				if(!dirFile.mkdirs())
				{
					System.out.println("There were some problem while creating directory: "+dirFile);
				}
			}
			
			WritableWorkbook book = Workbook.createWorkbook(new File(fileNameWithLocation));
			WritableSheet sheet = book.createSheet("Test_Results", 0);

			Label desc = new Label(0, 0, "Test_Description");
			sheet.addCell(desc);

			Label desc1 = new Label(0, 1, "TEST RUN FOR INPUT: "+inputParam);
			sheet.addCell(desc1);

			Label res = new Label(1, 0, "Test_Results");
			sheet.addCell(res);

			if(exceptionFlag)
			{
				Label res1 = new Label(1, 1, "SKIP: EXCEPTION OCCURED WHILE FETCHING DATA FOR INPUT: "+inputParam);
				sheet.addCell(res1);
			}
			else
			{
				Label res1 = new Label(1, 1, "SKIP: NO RECORD FOUND FOR THIS INPUT: "+inputParam);
				sheet.addCell(res1);
			}

			book.write();
			book.close();

			flag = true;
		}
		catch(Exception e)
		{
			flag = false;
			System.out.println("Exception occured while writing a dummy excel result file containing no record results. "); 
			System.out.println(e.getMessage());
		}
		finally
		{
			return flag;
		}

	}


}
