package ca.corefacility.bioinformatics.irida.service.analysis.workspace.galaxy.impl.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ca.corefacility.bioinformatics.irida.exceptions.ExecutionManagerException;
import ca.corefacility.bioinformatics.irida.exceptions.IridaWorkflowAnalysisTypeException;
import ca.corefacility.bioinformatics.irida.exceptions.IridaWorkflowNotFoundException;
import ca.corefacility.bioinformatics.irida.exceptions.UploadException;
import ca.corefacility.bioinformatics.irida.exceptions.WorkflowPreprationException;
import ca.corefacility.bioinformatics.irida.exceptions.galaxy.GalaxyDatasetException;
import ca.corefacility.bioinformatics.irida.model.joins.Join;
import ca.corefacility.bioinformatics.irida.model.project.ReferenceFile;
import ca.corefacility.bioinformatics.irida.model.sample.Sample;
import ca.corefacility.bioinformatics.irida.model.sample.SampleSequenceFileJoin;
import ca.corefacility.bioinformatics.irida.model.sequenceFile.SequenceFile;
import ca.corefacility.bioinformatics.irida.model.upload.galaxy.GalaxyProjectName;
import ca.corefacility.bioinformatics.irida.model.workflow.IridaWorkflow;
import ca.corefacility.bioinformatics.irida.model.workflow.IridaWorkflowTestBuilder;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.Analysis;
import ca.corefacility.bioinformatics.irida.model.workflow.execution.InputFileType;
import ca.corefacility.bioinformatics.irida.model.workflow.execution.galaxy.PreparedWorkflowGalaxy;
import ca.corefacility.bioinformatics.irida.model.workflow.submission.AnalysisSubmission;
import ca.corefacility.bioinformatics.irida.pipeline.upload.Uploader.DataStorage;
import ca.corefacility.bioinformatics.irida.pipeline.upload.galaxy.GalaxyHistoriesService;
import ca.corefacility.bioinformatics.irida.pipeline.upload.galaxy.GalaxyLibraryBuilder;
import ca.corefacility.bioinformatics.irida.pipeline.upload.galaxy.GalaxyWorkflowService;
import ca.corefacility.bioinformatics.irida.repositories.joins.sample.SampleSequenceFileJoinRepository;
import ca.corefacility.bioinformatics.irida.repositories.sequencefile.SequenceFileRepository;
import ca.corefacility.bioinformatics.irida.service.analysis.workspace.galaxy.AnalysisWorkspaceServiceGalaxy;
import ca.corefacility.bioinformatics.irida.service.workflow.IridaWorkflowsService;

import com.github.jmchilton.blend4j.galaxy.beans.Dataset;
import com.github.jmchilton.blend4j.galaxy.beans.History;
import com.github.jmchilton.blend4j.galaxy.beans.Library;
import com.github.jmchilton.blend4j.galaxy.beans.WorkflowDetails;
import com.github.jmchilton.blend4j.galaxy.beans.collection.request.CollectionDescription;
import com.github.jmchilton.blend4j.galaxy.beans.collection.response.CollectionResponse;
import com.google.common.collect.Sets;

/**
 * Tests out preparing a Galaxy Phylogenomics Pipeline workflow for execution.
 * 
 * @author Aaron Petkau <aaron.petkau@phac-aspc.gc.ca>
 *
 */
public class AnalysisWorkspaceServiceGalaxyTest {

	@Mock
	private GalaxyHistoriesService galaxyHistoriesService;
	@Mock
	private GalaxyWorkflowService galaxyWorkflowService;
	@Mock
	private GalaxyLibraryBuilder libraryBuilder;
	@Mock
	private SampleSequenceFileJoinRepository sampleSequenceFileJoinRepository;
	@Mock
	private List<Dataset> sequenceDatasets;
	@Mock
	private Dataset refDataset;

	@Mock
	private SequenceFileRepository sequenceFileRepository;

	@Mock
	private IridaWorkflowsService iridaWorkflowsService;

	private AnalysisWorkspaceServiceGalaxy workflowPreparation;

	private Set<SequenceFile> inputFiles;
	private ReferenceFile referenceFile;
	private Path refFile;
	private AnalysisSubmission submission;
	private CollectionResponse collectionResponse;
	private WorkflowDetails workflowDetails;

	private History workflowHistory;
	private Library workflowLibrary;

	private static final String HISTORY_ID = "10";
	private static final String WORKFLOW_ID = "11";
	private static final String SEQUENCE_FILE_LABEL = "sequence_reads";
	private static final String REFERENCE_FILE_LABEL = "reference";
	private static final String SEQUENCE_FILE_ID = "12";
	private static final String REFERENCE_FILE_ID = "13";

	private Map<Path, String> datasetsMap;

	private SequenceFile sFileA;
	private SequenceFile sFileB;
	private SequenceFile sFileC;

	private Dataset datasetA;
	private Dataset datasetB;
	private Dataset datasetC;
	
	private Dataset output1Dataset;
	private Dataset output2Dataset;
	private String output1Filename = "output1.txt";
	private String output2Filename = "output2.txt";

	private Join<Sample, SequenceFile> sampleAJoin;
	private Join<Sample, SequenceFile> sampleBJoin;
	private Join<Sample, SequenceFile> sampleCJoin;

	private Join<Sample, SequenceFile> sampleAJoinWithB;

	private UUID workflowId = IridaWorkflowTestBuilder.DEFAULT_ID;
	private IridaWorkflow iridaWorkflow = IridaWorkflowTestBuilder.buildTestWorkflow();

	/**
	 * Sets up variables for testing.
	 * 
	 * @throws IOException
	 * @throws GalaxyDatasetException
	 * @throws UploadException
	 */
	@Before
	public void setup() throws IOException, UploadException, GalaxyDatasetException {
		MockitoAnnotations.initMocks(this);

		sFileA = new SequenceFile(createTempFile("fileA", "fastq"));
		sFileB = new SequenceFile(createTempFile("fileB", "fastq"));
		sFileC = new SequenceFile(createTempFile("fileC", "fastq"));

		Sample sampleA = new Sample();
		sampleA.setSampleName("SampleA");

		Sample sampleB = new Sample();
		sampleB.setSampleName("SampleB");

		Sample sampleC = new Sample();
		sampleC.setSampleName("SampleC");

		sampleAJoin = new SampleSequenceFileJoin(sampleA, sFileA);
		sampleBJoin = new SampleSequenceFileJoin(sampleB, sFileB);
		sampleCJoin = new SampleSequenceFileJoin(sampleC, sFileC);

		datasetsMap = new HashMap<>();
		datasetsMap.put(sFileA.getFile(), "1");
		datasetsMap.put(sFileB.getFile(), "2");
		datasetsMap.put(sFileC.getFile(), "3");

		sampleAJoinWithB = new SampleSequenceFileJoin(sampleA, sFileB);

		datasetA = new Dataset();
		datasetA.setId("1");
		datasetB = new Dataset();
		datasetB.setId("2");
		datasetC = new Dataset();
		datasetC.setId("3");

		refFile = createTempFile("reference", "fasta");
		referenceFile = new ReferenceFile(refFile);

		inputFiles = new HashSet<>();
		inputFiles.addAll(Arrays.asList(sFileA, sFileB, sFileC));

		submission = new AnalysisSubmission("my analysis", inputFiles, referenceFile, workflowId);

		workflowHistory = new History();
		workflowHistory.setId(HISTORY_ID);

		workflowLibrary = new Library();
		workflowLibrary.setId("1");

		collectionResponse = new CollectionResponse();
		collectionResponse.setId("1");

		workflowDetails = new WorkflowDetails();
		workflowDetails.setId(WORKFLOW_ID);

		workflowPreparation = new AnalysisWorkspaceServiceGalaxy(galaxyHistoriesService,
				galaxyWorkflowService, sampleSequenceFileJoinRepository, sequenceFileRepository, libraryBuilder,
				iridaWorkflowsService);
		
		output1Dataset = new Dataset();
		output1Dataset.setId("1");
		output1Dataset.setName("output1.txt");

		output2Dataset = new Dataset();
		output2Dataset.setId("2");
		output2Dataset.setName("output2.txt");
	}

	private Path createTempFile(String prefix, String suffix) throws IOException {
		File file = File.createTempFile(prefix, suffix);
		file.deleteOnExit();

		return file.toPath();
	}

	/**
	 * Tests out successfully to preparing an analysis workspace
	 * 
	 * @throws ExecutionManagerException
	 */
	@Test
	public void testPrepareAnalysisWorkspaceSuccess() throws ExecutionManagerException {
		when(galaxyHistoriesService.newHistoryForWorkflow()).thenReturn(workflowHistory);
		assertEquals(HISTORY_ID, workflowPreparation.prepareAnalysisWorkspace(submission));
	}

	/**
	 * Tests out failing to preparing an analysis workspace
	 * 
	 * @throws ExecutionManagerException
	 */
	@Test(expected = RuntimeException.class)
	public void testPrepareAnalysisWorkspaceFail() throws ExecutionManagerException {
		when(galaxyHistoriesService.newHistoryForWorkflow()).thenThrow(new RuntimeException());
		assertEquals(HISTORY_ID, workflowPreparation.prepareAnalysisWorkspace(submission));
	}

	/**
	 * Tests out successfully to preparing an analysis
	 * 
	 * @throws ExecutionManagerException
	 * @throws IridaWorkflowNotFoundException
	 */
	@Test
	public void testPrepareAnalysisFilesSuccess() throws ExecutionManagerException, IridaWorkflowNotFoundException {
		submission.setRemoteAnalysisId(HISTORY_ID);

		when(iridaWorkflowsService.getIridaWorkflow(workflowId)).thenReturn(iridaWorkflow);

		when(galaxyHistoriesService.findById(HISTORY_ID)).thenReturn(workflowHistory);
		when(libraryBuilder.buildEmptyLibrary(any(GalaxyProjectName.class))).thenReturn(workflowLibrary);

		when(
				galaxyHistoriesService.filesToLibraryToHistory(datasetsMap.keySet(), InputFileType.FASTQ_SANGER,
						workflowHistory, workflowLibrary, DataStorage.LOCAL)).thenReturn(datasetsMap);

		when(sampleSequenceFileJoinRepository.getSampleForSequenceFile(sFileA)).thenReturn(sampleAJoin);
		when(sampleSequenceFileJoinRepository.getSampleForSequenceFile(sFileB)).thenReturn(sampleBJoin);
		when(sampleSequenceFileJoinRepository.getSampleForSequenceFile(sFileC)).thenReturn(sampleCJoin);

		when(galaxyHistoriesService.fileToHistory(refFile, InputFileType.FASTA, workflowHistory))
				.thenReturn(refDataset);
		when(galaxyHistoriesService.constructCollection(any(CollectionDescription.class), eq(workflowHistory)))
				.thenReturn(collectionResponse);
		when(galaxyWorkflowService.getWorkflowDetails(WORKFLOW_ID)).thenReturn(workflowDetails);
		when(galaxyWorkflowService.getWorkflowInputId(workflowDetails, SEQUENCE_FILE_LABEL)).thenReturn(
				SEQUENCE_FILE_ID);
		when(galaxyWorkflowService.getWorkflowInputId(workflowDetails, REFERENCE_FILE_LABEL)).thenReturn(
				REFERENCE_FILE_ID);

		submission.setRemoteWorkflowId(WORKFLOW_ID);
		PreparedWorkflowGalaxy preparedWorkflow = workflowPreparation.prepareAnalysisFiles(submission);
		assertEquals("preparedWorflow history id not equal to " + HISTORY_ID, HISTORY_ID,
				preparedWorkflow.getRemoteAnalysisId());
		assertNotNull("workflowInputs in preparedWorkflow is null", preparedWorkflow.getWorkflowInputs());
		assertTrue(preparedWorkflow.getWorkflowInputs().getInputsObject().getInputs().containsKey(REFERENCE_FILE_ID));
		assertTrue(preparedWorkflow.getWorkflowInputs().getInputsObject().getInputs().containsKey(SEQUENCE_FILE_ID));
	}

	/**
	 * Tests out failing to prepare an analysis.
	 * 
	 * @throws ExecutionManagerException
	 * @throws IridaWorkflowNotFoundException
	 */
	@Test(expected = UploadException.class)
	public void testPrepareAnalysisFilesFail() throws ExecutionManagerException, IridaWorkflowNotFoundException {
		submission.setRemoteAnalysisId(HISTORY_ID);
		submission.setRemoteWorkflowId(WORKFLOW_ID);

		when(iridaWorkflowsService.getIridaWorkflow(workflowId)).thenReturn(iridaWorkflow);
		when(galaxyHistoriesService.findById(HISTORY_ID)).thenReturn(workflowHistory);
		when(libraryBuilder.buildEmptyLibrary(any(GalaxyProjectName.class))).thenReturn(workflowLibrary);

		when(
				galaxyHistoriesService.filesToLibraryToHistory(datasetsMap.keySet(), InputFileType.FASTQ_SANGER,
						workflowHistory, workflowLibrary, DataStorage.LOCAL)).thenThrow(new UploadException());

		when(sampleSequenceFileJoinRepository.getSampleForSequenceFile(sFileA)).thenReturn(sampleAJoin);
		when(sampleSequenceFileJoinRepository.getSampleForSequenceFile(sFileB)).thenReturn(sampleBJoin);
		when(sampleSequenceFileJoinRepository.getSampleForSequenceFile(sFileC)).thenReturn(sampleCJoin);

		workflowPreparation.prepareAnalysisFiles(submission);
	}

	/**
	 * Tests out failing to prepare an analysis due to two different sequence
	 * files with the same sample.
	 * 
	 * @throws ExecutionManagerException
	 * @throws IridaWorkflowNotFoundException
	 */
	@Test(expected = WorkflowPreprationException.class)
	public void testPrepareAnalysisWorkspaceFilesDuplicateSamples() throws ExecutionManagerException,
			IridaWorkflowNotFoundException {
		submission.setRemoteAnalysisId(HISTORY_ID);
		submission.setRemoteWorkflowId(WORKFLOW_ID);
		when(iridaWorkflowsService.getIridaWorkflow(workflowId)).thenReturn(iridaWorkflow);

		when(galaxyHistoriesService.findById(HISTORY_ID)).thenReturn(workflowHistory);
		when(libraryBuilder.buildEmptyLibrary(any(GalaxyProjectName.class))).thenReturn(workflowLibrary);

		when(
				galaxyHistoriesService.filesToLibraryToHistory(datasetsMap.keySet(), InputFileType.FASTQ_SANGER,
						workflowHistory, workflowLibrary, DataStorage.LOCAL)).thenReturn(datasetsMap);

		when(sampleSequenceFileJoinRepository.getSampleForSequenceFile(sFileA)).thenReturn(sampleAJoin);
		when(sampleSequenceFileJoinRepository.getSampleForSequenceFile(sFileB)).thenReturn(sampleAJoinWithB);
		when(sampleSequenceFileJoinRepository.getSampleForSequenceFile(sFileC)).thenReturn(sampleCJoin);

		workflowPreparation.prepareAnalysisFiles(submission);
	}
	
	/**
	 * Tests successfully getting analysis results from Galaxy.
	 * 
	 * @throws IridaWorkflowNotFoundException
	 * @throws IOException
	 * @throws ExecutionManagerException
	 * @throws IridaWorkflowAnalysisTypeException
	 */
	@Test
	public void testGetAnalysisResultsSuccess() throws IridaWorkflowNotFoundException,
			IridaWorkflowAnalysisTypeException, ExecutionManagerException, IOException {
		submission = new AnalysisSubmission("my analysis", Sets.newHashSet(), referenceFile, workflowId);
		submission.setRemoteWorkflowId(WORKFLOW_ID);
		submission.setRemoteAnalysisId(HISTORY_ID);

		when(iridaWorkflowsService.getIridaWorkflow(workflowId)).thenReturn(iridaWorkflow);
		when(galaxyHistoriesService.getDatasetForFileInHistory(output1Filename, HISTORY_ID)).thenReturn(output1Dataset);
		when(galaxyHistoriesService.getDatasetForFileInHistory(output2Filename, HISTORY_ID)).thenReturn(output2Dataset);

		Analysis analysis = workflowPreparation.getAnalysisResults(submission);

		assertNotNull(analysis);
		assertEquals(2, analysis.getAnalysisOutputFiles().size());
		assertEquals(Paths.get("output1.txt"), analysis.getAnalysisOutputFile("output1").getFile().getFileName());
		assertEquals(Paths.get("output2.txt"), analysis.getAnalysisOutputFile("output2").getFile().getFileName());

		verify(galaxyHistoriesService).getDatasetForFileInHistory("output1.txt", HISTORY_ID);
		verify(galaxyHistoriesService).getDatasetForFileInHistory("output2.txt", HISTORY_ID);
	}
	
	/**
	 * Tests failure to get analysis results from Galaxy due to failure to get a dataset
	 * 
	 * @throws IridaWorkflowNotFoundException
	 * @throws IOException
	 * @throws ExecutionManagerException
	 * @throws IridaWorkflowAnalysisTypeException
	 */
	@Test(expected=GalaxyDatasetException.class)
	public void testGetAnalysisResultsFail() throws IridaWorkflowNotFoundException,
			IridaWorkflowAnalysisTypeException, ExecutionManagerException, IOException {
		submission = new AnalysisSubmission("my analysis", Sets.newHashSet(), referenceFile, workflowId);
		submission.setRemoteWorkflowId(WORKFLOW_ID);
		submission.setRemoteAnalysisId(HISTORY_ID);

		when(iridaWorkflowsService.getIridaWorkflow(workflowId)).thenReturn(iridaWorkflow);
		when(galaxyHistoriesService.getDatasetForFileInHistory(output1Filename, HISTORY_ID)).thenThrow(new GalaxyDatasetException());

		workflowPreparation.getAnalysisResults(submission);
	}
}