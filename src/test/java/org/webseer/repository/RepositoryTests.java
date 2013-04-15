package org.webseer.repository;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.common.io.Files;

@RunWith(JUnit4.class)
public class RepositoryTests {

	private File tempDir;
	private Repository repository;

	@Before
	public void setUp() {
		tempDir = Files.createTempDir();

		repository = new Repository(tempDir);
	}

	@After
	public void tearDown() throws IOException {
		FileUtils.deleteDirectory(tempDir);
	}

	@Test
	public void testGetCommonsIO() throws Exception {
		File file = repository.getArtifact("commons-io", "commons-io", "1.0");

		Assert.assertEquals(new File(tempDir, "commons-io/commons-io/1.0/commons-io-1.0.jar"), file);
	}

	@Test
	public void testResolveCommonsIO() throws Exception {
		List<File> files = repository.resolveArtifact("commons-io", "commons-io", "1.0");

		Assert.assertEquals(Arrays.asList(new File[] { new File(tempDir, "commons-io/commons-io/1.0/commons-io-1.0.jar") }), files);
	}

}
