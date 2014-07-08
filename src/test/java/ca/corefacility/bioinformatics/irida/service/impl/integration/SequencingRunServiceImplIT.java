package ca.corefacility.bioinformatics.irida.service.impl.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExcecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import ca.corefacility.bioinformatics.irida.config.IridaApiServicesConfig;
import ca.corefacility.bioinformatics.irida.config.data.IridaApiTestDataSourceConfig;
import ca.corefacility.bioinformatics.irida.config.processing.IridaApiTestMultithreadingConfig;
import ca.corefacility.bioinformatics.irida.model.SequenceFile;
import ca.corefacility.bioinformatics.irida.model.SequencingRunEntity;
import ca.corefacility.bioinformatics.irida.model.run.MiseqRun;
import ca.corefacility.bioinformatics.irida.model.run.SequencingRun;
import ca.corefacility.bioinformatics.irida.model.sample.Sample;
import ca.corefacility.bioinformatics.irida.service.SequenceFileService;
import ca.corefacility.bioinformatics.irida.service.SequencingRunService;
import ca.corefacility.bioinformatics.irida.service.sample.SampleService;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

/**
 * Test for SequencingRunServiceImplIT.
 * NOTE: This class uses a separate table reset file at 
 * /ca/corefacility/bioinformatics/irida/service/impl/SequencingRunServiceTableReset.xml
 * 
 * @author Thomas Matthews <thomas.matthews@phac-aspc.gc.ca>
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = { IridaApiServicesConfig.class,
		IridaApiTestDataSourceConfig.class, IridaApiTestMultithreadingConfig.class })
@ActiveProfiles("test")
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, DbUnitTestExecutionListener.class, WithSecurityContextTestExcecutionListener.class })
@DatabaseSetup("/ca/corefacility/bioinformatics/irida/service/impl/SequencingRunServiceImplIT.xml")
@DatabaseTearDown({"/ca/corefacility/bioinformatics/irida/service/impl/SequencingRunServiceTableReset.xml"})
public class SequencingRunServiceImplIT {
	@Autowired
	private SequencingRunService miseqRunService;
	@Autowired
	private SequenceFileService sequenceFileService;
	@Autowired
	private SampleService sampleService;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@WithMockUser(username="fbristow", password="password1", roles="SEQUENCER")
	public void testAddSequenceFileToMiseqRunAsSequencer() {
		testAddSequenceFileToMiseqRun();
	}

	@Test
	@WithMockUser(username="fbristow", password="password1", roles="ADMIN")
	public void testAddSequenceFileToMiseqRunAsAdmin() {
		testAddSequenceFileToMiseqRun();
	}

	@Test(expected = AccessDeniedException.class)
	@WithMockUser(username="fbristow", password="password1", roles="USER")
	public void testAddSequenceFileToMiseqRunAsUser() {
		testAddSequenceFileToMiseqRun();
	}

	@Test(expected = AccessDeniedException.class)
	@WithMockUser(username="fbristow", password="password1", roles="MANAGER")
	public void testAddSequenceFileToMiseqRunAsManager() {
		testAddSequenceFileToMiseqRun();
	}

	private void testAddSequenceFileToMiseqRun() {
		SequenceFile sf = sequenceFileService.read(1l);
		SequencingRun miseqRun = miseqRunService.read(1l);
		miseqRunService.addSequenceFileToSequencingRun(miseqRun, sf);
		SequencingRun saved = miseqRunService.read(1l);
		Set<SequenceFile> sequenceFilesForMiseqRun = sequenceFileService.getSequenceFilesForSequencingRun(saved);
		assertTrue("Saved miseq run should have seqence file", sequenceFilesForMiseqRun.contains(sf));
	}

	@Test
	@WithMockUser(username="fbristow", password="password1", roles="ADMIN")
	public void testGetMiseqRunForSequenceFile() {
		SequenceFile sf = sequenceFileService.read(2l);

		try {
			SequencingRun j = miseqRunService.getSequencingRunForSequenceFile(sf);
			assertEquals("Join had wrong miseq run.", Long.valueOf(2l), j.getId());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Test failed for unknown reason.");
		}
	}

	@Test
	@WithMockUser(username="fbristow", password="password1", roles="SEQUENCER")
	public void testCreateMiseqRunAsSequencer() {
		MiseqRun mr = new MiseqRun();
		mr.setWorkflow("Workflow name.");
		SequencingRun returned = miseqRunService.create(mr);
		assertNotNull("Created run was not assigned an ID.", returned.getId());
	}

	@Test
	@WithMockUser(username="fbristow", password="password1", roles="SEQUENCER")
	public void testReadMiseqRunAsSequencer() {
		SequencingRun mr = miseqRunService.read(1L);
		assertNotNull("Created run was not assigned an ID.", mr.getId());
	}

	@Test
	@WithMockUser(username="fbristow", password="password1", roles="ADMIN")
	public void testCreateMiseqRunAsAdmin() {
		MiseqRun r = new MiseqRun();
		r.setWorkflow("some workflow");
		miseqRunService.create(r);
	}
	
	@Test
	@WithMockUser(username="fbristow", password="password1", roles="ADMIN")
	public void testDeleteCascadeToSequenceFile(){
		assertTrue("Sequence file should exist before",sequenceFileService.exists(2L));
		miseqRunService.delete(2L);
		assertFalse("Sequence file should be deleted on cascade",sequenceFileService.exists(2L));
	}
	
	@Test
	@WithMockUser(username="fbristow", password="password1", roles="ADMIN")
	public void testDeleteCascadeToSample(){
		assertTrue("Sequence file should exist before",sequenceFileService.exists(1L));
		miseqRunService.delete(3L);
		assertFalse("Sequence file should be deleted on cascade",sequenceFileService.exists(1L));
		assertFalse("Sequence file should be deleted on cascade",sequenceFileService.exists(3L));
		assertFalse("Sequence file should be deleted on cascade",sequenceFileService.exists(4L));
		assertFalse("Sample should be deleted on cascade", sampleService.exists(2L));
		assertTrue("This sample should not be removed", sampleService.exists(1L));
	}
	
	@Test
	@WithMockUser(username="fbristow", password="password1", roles="ADMIN")
	public void testListAllSequencingRuns(){
		Iterable<SequencingRun> findAll = miseqRunService.findAll();
		assertNotNull(findAll);
		boolean foundMiseq = false;
		boolean foundTestEntity = false;
		for(SequencingRun run : findAll){
			assertNotNull(run);
			if(run instanceof MiseqRun){
				foundMiseq = true;
			}
			else if(run instanceof SequencingRunEntity){
				foundTestEntity = true;
			}
		}
		
		assertTrue(foundMiseq);
		assertTrue(foundTestEntity);
	}
	
	/**
	 * This test simulates a bug that happens from the REST API when uploading sequence files to samples, 
	 * where a new sequence file is created, then detached from a transaction.
	 * 
	 * @throws IOException
	 */
	@Test
	@WithMockUser(username="fbristow", password="password1", roles="ADMIN")
	public void testAddDetachedRunToSequenceFile() throws IOException{
		final String SEQUENCE = "ACGTACGTN";
		final byte[] FASTQ_FILE_CONTENTS = ("@testread\n" + SEQUENCE + "\n+\n?????????\n@testread2\n"
				+ SEQUENCE + "\n+\n?????????").getBytes();
		Path p = Files.createTempFile(null,  null);
		Files.write(p, FASTQ_FILE_CONTENTS);
		
		SequenceFile sf = new SequenceFile();
		sf.setFile(p);
		Sample sample = sampleService.read(1L);
		SequencingRun run = miseqRunService.read(2L);
		
		sequenceFileService.createSequenceFileInSample(sf, sample);
		
		miseqRunService.addSequenceFileToSequencingRun(run, sf);
		
	}
}