package ca.corefacility.bioinformatics.irida.pipeline.workflow.impl;

import java.io.File;
import java.util.List;

import ca.corefacility.bioinformatics.irida.pipeline.workflow.WorkflowSubmissionException;

import com.github.jmchilton.blend4j.galaxy.GalaxyInstance;
import com.github.jmchilton.blend4j.galaxy.GalaxyInstanceFactory;
import com.github.jmchilton.blend4j.galaxy.LibrariesClient;
import com.github.jmchilton.blend4j.galaxy.WorkflowsClient;
import com.github.jmchilton.blend4j.galaxy.beans.FileLibraryUpload;
import com.github.jmchilton.blend4j.galaxy.beans.Library;
import com.github.jmchilton.blend4j.galaxy.beans.LibraryContent;
import com.github.jmchilton.blend4j.galaxy.beans.LibraryFolder;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;

public class WorkflowRESTAPIGalaxy
{
	private GalaxyInstance galaxyInstance;
	
	public WorkflowRESTAPIGalaxy(String galaxyURL, String apiKey)
	{
		if (galaxyURL == null)
		{
			throw new IllegalArgumentException("galaxyURL is null");
		}
		else if (apiKey == null)
		{
			throw new IllegalArgumentException("apiKey is null");			
		}
		
		galaxyInstance = GalaxyInstanceFactory.get(galaxyURL, apiKey);
		
		if (galaxyInstance == null)
		{
			throw new RuntimeException("Could not create GalaxyInstance with URL=" + 
						galaxyURL + ", apiKey=" + apiKey);
		}
	}
	
	public WorkflowRESTAPIGalaxy(GalaxyInstance galaxyInstance)
	{
		if (galaxyInstance == null)
		{
			throw new IllegalArgumentException("galaxyInstance is null");
		}
		
		this.galaxyInstance = galaxyInstance;
	}
	
	/**
	 * Imports the passed workflow into Galaxy.
	 * @param workflowGalaxy  The Galaxy Workflow to import.
	 * @return  The ID of the Galaxy Workflow, or null if not successfully imported.
	 * @throws WorkflowSubmissionException  If an error occurred while importing the workflow.
	 */
	public String importWorkflow(ExecutableWorkflowGalaxy workflowGalaxy) throws WorkflowSubmissionException
	{
		WorkflowsClient workflowsClient = galaxyInstance.getWorkflowsClient();
		com.github.jmchilton.blend4j.galaxy.beans.Workflow galaxyWorkflow = null;
		
		try
		{
			galaxyWorkflow = workflowsClient.importWorkflow(workflowGalaxy.getJson());
		}
		catch (ClientHandlerException e)
		{
			throw new WorkflowSubmissionException(e);
		}
		
		if (galaxyWorkflow == null)
		{
			throw new WorkflowSubmissionException("uploaded Galaxy workflow is null");
		}
		
		return galaxyWorkflow.getId();
	}
	
	/**
	 * Imports the files used within the given workflow to Galaxy.
	 * @param workflowGalaxy  The workflow containing the files to import.
	 * @return  An ID of the data library containing the files.
	 */
	public String importWorkflowFiles(ExecutableWorkflowGalaxy workflowGalaxy)
		throws WorkflowSubmissionException
	{
		return null;
	}
	
	/**
	 * Builds a data library in Galaxy with the name and owner.
	 * @param dataFiles  The data files to upload to this library.
	 * @param libraryName  The name of the library to create.
	 * @return  A unique ID for the created library, or null if no library was created.
	 */
	public String buildGalaxyLibrary(String libraryName)
	{
		if (libraryName == null)
		{
			throw new IllegalArgumentException("libraryName is null");
		}
		
		String libraryID = null;
		
		LibrariesClient librariesClient = galaxyInstance.getLibrariesClient();
		Library library = new Library(libraryName);
		Library persistedLibrary = librariesClient.createLibrary(library);
		
		if (persistedLibrary != null)
		{
			libraryID = persistedLibrary.getId();
		}
		
		return libraryID;
	}
	
	private boolean uploadSample(GalaxySample sample, LibrariesClient librariesClient, LibraryContent rootFolder,
			Library library, String errorSuffix) throws LibraryUploadException
	{		
		LibraryFolder sampleFolder = new LibraryFolder();
		sampleFolder.setName(sample.getSampleName());
		sampleFolder.setFolderId(rootFolder.getId());
		
		LibraryFolder persistedSampleFolder = librariesClient.createFolder(library.getId(), sampleFolder);
		
		boolean success = true;
		
		if (persistedSampleFolder != null)
		{
			for (File file : sample.getSampleFiles())
			{
				FileLibraryUpload upload = new FileLibraryUpload();
				upload.setFolderId(persistedSampleFolder.getId());
				
				upload.setFile(file);
				upload.setName(file.getName());
				
				ClientResponse uploadResponse = librariesClient.uploadFile(library.getId(), upload);
				
				success &= ClientResponse.Status.OK.equals(uploadResponse.getClientResponseStatus());
			}
			
			return success;
		}
		else
		{
			throw new LibraryUploadException("Could not build folder for sample " + sample.getSampleName() +
					" within library " + library.getName() + ":" + library.getId() + errorSuffix);
		}
	}
	
	/**
	 * Uploads the given list of samples to the passed Galaxy library.
	 * @param samples  The set of samples to upload.
	 * @param libraryName  The name of the library to upload to.
	 * @return  True if successful, false otherwise.
	 * @throws LibraryUploadException If an error occurred.
	 */
	public boolean uploadSamples(List<GalaxySample> samples, String libraryName)
			throws LibraryUploadException
	{
		if (libraryName == null)
		{
			throw new IllegalArgumentException("libraryName is null");
		}
		
		if (samples == null)
		{
			throw new IllegalArgumentException("samples is null");
		}
		
		boolean success = false;
		
		try
		{
			String libraryId = buildGalaxyLibrary(libraryName);
			if (libraryId != null)
			{		
				success = uploadFilesToLibrary(samples, libraryId);
			}
			else
			{
				throw new LibraryUploadException("Could not create library with name " + libraryName
						+ " in instance of galaxy with url=" + galaxyInstance.getGalaxyUrl());
			}
		}
		catch (LibraryUploadException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new LibraryUploadException(e);
		}
		
		return success;
	}
	
	/**
	 * Uploads the passed set of files to a Galaxy library.
	 * @param samples  The samples to upload to Galaxy.
	 * @param libraryID  A unique ID for the library, generated from buildGalaxyLibrary(String)
	 * @return  True if the files have been uploaded, false otherwise.
	 * @throws LibraryUploadException 
	 */
	public boolean uploadFilesToLibrary(List<GalaxySample> samples, String libraryID) throws LibraryUploadException
	{
		if (samples == null)
		{
			throw new IllegalArgumentException("samples are null");
		}
		else if (libraryID == null)
		{
			throw new IllegalArgumentException("libraryID is null");
		}
		
		boolean success = true;
		
		if (samples.size() > 0)
		{
			String errorSuffix = " in instance of galaxy with url=" + galaxyInstance.getGalaxyUrl();
			
			Library library = null;
			LibrariesClient librariesClient = galaxyInstance.getLibrariesClient();
			List<Library> libraries = librariesClient.getLibraries();
			for (Library curr : libraries)
			{
				if (libraryID.equals(curr.getId()))
				{
					library = curr;
				}
			}
			
			if (library != null)
			{
				LibraryContent rootFolder = librariesClient.getRootFolder(library.getId());
				
				if (rootFolder != null)
				{
					for (GalaxySample sample : samples)
					{
						if (sample != null)
						{
							success &= uploadSample(sample, librariesClient, rootFolder, library, errorSuffix);
						}
						else
						{
							throw new LibraryUploadException("Cannot upload a null sample" + errorSuffix);
						}
					}
				}
				else
				{
					throw new LibraryUploadException("Could not get root folder from library with id=" + libraryID + errorSuffix);
				}
			}
			else
			{
				throw new LibraryUploadException("Could not find library with id=" + libraryID + errorSuffix);
			}
		}
		
		return success;
	}
}
