package org.webseer.repository;

import java.io.File;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.sonatype.aether.AbstractRepositoryListener;
import org.sonatype.aether.RepositoryEvent;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.transfer.AbstractTransferListener;
import org.sonatype.aether.transfer.TransferEvent;
import org.sonatype.aether.transfer.TransferResource;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;
import org.sonatype.aether.util.filter.DependencyFilterUtils;
import org.webseer.model.meta.Library;

public class Repository {

	private static final Repository DEFAULT_INSTANCE = new Repository(new File("target/local-repo"));

	public static final Repository getDefaultInstance() {
		return DEFAULT_INSTANCE;
	}

	private LocalRepository localRepo;
	private RemoteRepository centralRepo;

	public Repository(File localCache) {
		this.localRepo = new LocalRepository(localCache);

		this.centralRepo = new RemoteRepository("central", "default", "http://localhost:2323/archiva/repository/internal/");
	}

	public File getArtifact(Library library) throws RepositoryException {
		return getArtifact(library.getGroup(), library.getName(), library.getVersion());
	}

	public File getArtifact(String groupId, String artifactId, String version) throws RepositoryException {
		return getArtifact(groupId + ":" + artifactId + ":" + version);
	}

	public File getArtifact(String artifactId) throws RepositoryException {
		Artifact artifact = new DefaultArtifact(artifactId);

		RepositorySystem system = newRepositorySystem();
		DefaultRepositorySystemSession session = newRepositorySystemSession(system);
		
		ArtifactRequest artifactRequest = new ArtifactRequest(artifact, Arrays.asList(centralRepo), null);
		
		ArtifactResult result = system.resolveArtifact(session, artifactRequest);

		if (!result.isResolved()) {
			return null; 
		}
		
		return result.getArtifact().getFile();
	}
	
	public List<File> resolveArtifact(Library library) throws RepositoryException {
		return resolveArtifact(library.getGroup(), library.getName(), library.getVersion());
	}

	/**
	 * Resolves the artifact address into a set of local Files.
	 */
	public List<File> resolveArtifact(String groupId, String artifactId, String version) throws RepositoryException {
		Artifact artifact = new DefaultArtifact(groupId + ":" + artifactId + ":" + version);

		DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);

		RepositorySystem system = newRepositorySystem();
		DefaultRepositorySystemSession session = newRepositorySystemSession(system);

		CollectRequest collectRequest = new CollectRequest();
		collectRequest.setRoot(new Dependency(artifact, JavaScopes.COMPILE));
		collectRequest.addRepository(centralRepo);

		DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, classpathFlter);

		List<ArtifactResult> artifactResults = system.resolveDependencies(session, dependencyRequest)
				.getArtifactResults();

		List<File> artifactFiles = new ArrayList<File>();
		for (ArtifactResult artifactResult : artifactResults) {
			artifactFiles.add(artifactResult.getArtifact().getFile());
		}

		return artifactFiles;
	}

	public static RepositorySystem newRepositorySystem() {
		return ManualRepositorySystemFactory.newRepositorySystem();
	}

	public DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
		MavenRepositorySystemSession session = new MavenRepositorySystemSession();

		session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo));

		session.setTransferListener(new ConsoleTransferListener());
		session.setRepositoryListener(new ConsoleRepositoryListener());

		// uncomment to generate dirty trees
		// session.setDependencyGraphTransformer( null );

		return session;
	}

	private static class ManualWagonProvider implements WagonProvider {

		public Wagon lookup(String roleHint) throws Exception {
			if ("http".equals(roleHint)) {
				return new LightweightHttpWagon();
			}
			return null;
		}

		public void release(Wagon wagon) {

		}

	}

	private static class ManualRepositorySystemFactory {

		public static RepositorySystem newRepositorySystem() {
			/*
			 * Aether's components implement org.eclipse.aether.spi.locator.Service to ease manual wiring and using the
			 * prepopulated DefaultServiceLocator, we only need to register the repository connector factories.
			 */
			DefaultServiceLocator locator = new DefaultServiceLocator();
			locator.addService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
			locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
			locator.setServices(WagonProvider.class, new ManualWagonProvider());

			return locator.getService(RepositorySystem.class);
		}

	}

	private static class ConsoleRepositoryListener extends AbstractRepositoryListener {

		private PrintStream out;

		public ConsoleRepositoryListener() {
			this(null);
		}

		public ConsoleRepositoryListener(PrintStream out) {
			this.out = (out != null) ? out : System.out;
		}

		public void artifactDeployed(RepositoryEvent event) {
			out.println("Deployed " + event.getArtifact() + " to " + event.getRepository());
		}

		public void artifactDeploying(RepositoryEvent event) {
			out.println("Deploying " + event.getArtifact() + " to " + event.getRepository());
		}

		public void artifactDescriptorInvalid(RepositoryEvent event) {
			out.println("Invalid artifact descriptor for " + event.getArtifact() + ": "
					+ event.getException().getMessage());
		}

		public void artifactDescriptorMissing(RepositoryEvent event) {
			out.println("Missing artifact descriptor for " + event.getArtifact());
		}

		public void artifactInstalled(RepositoryEvent event) {
			out.println("Installed " + event.getArtifact() + " to " + event.getFile());
		}

		public void artifactInstalling(RepositoryEvent event) {
			out.println("Installing " + event.getArtifact() + " to " + event.getFile());
		}

		public void artifactResolved(RepositoryEvent event) {
			out.println("Resolved artifact " + event.getArtifact() + " from " + event.getRepository());
		}

		public void artifactDownloading(RepositoryEvent event) {
			out.println("Downloading artifact " + event.getArtifact() + " from " + event.getRepository());
		}

		public void artifactDownloaded(RepositoryEvent event) {
			out.println("Downloaded artifact " + event.getArtifact() + " from " + event.getRepository());
		}

		public void artifactResolving(RepositoryEvent event) {
			out.println("Resolving artifact " + event.getArtifact());
		}

		public void metadataDeployed(RepositoryEvent event) {
			out.println("Deployed " + event.getMetadata() + " to " + event.getRepository());
		}

		public void metadataDeploying(RepositoryEvent event) {
			out.println("Deploying " + event.getMetadata() + " to " + event.getRepository());
		}

		public void metadataInstalled(RepositoryEvent event) {
			out.println("Installed " + event.getMetadata() + " to " + event.getFile());
		}

		public void metadataInstalling(RepositoryEvent event) {
			out.println("Installing " + event.getMetadata() + " to " + event.getFile());
		}

		public void metadataInvalid(RepositoryEvent event) {
			out.println("Invalid metadata " + event.getMetadata());
		}

		public void metadataResolved(RepositoryEvent event) {
			out.println("Resolved metadata " + event.getMetadata() + " from " + event.getRepository());
		}

		public void metadataResolving(RepositoryEvent event) {
			out.println("Resolving metadata " + event.getMetadata() + " from " + event.getRepository());
		}

	}

	private static class ConsoleTransferListener extends AbstractTransferListener {

		private PrintStream out;

		private Map<TransferResource, Long> downloads = new ConcurrentHashMap<TransferResource, Long>();

		private int lastLength;

		public ConsoleTransferListener() {
			this(null);
		}

		public ConsoleTransferListener(PrintStream out) {
			this.out = (out != null) ? out : System.out;
		}

		@Override
		public void transferInitiated(TransferEvent event) {
			String message = event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading";

			out.println(message + ": " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
		}

		@Override
		public void transferProgressed(TransferEvent event) {
			TransferResource resource = event.getResource();
			downloads.put(resource, Long.valueOf(event.getTransferredBytes()));

			StringBuilder buffer = new StringBuilder(64);

			for (Map.Entry<TransferResource, Long> entry : downloads.entrySet()) {
				long total = entry.getKey().getContentLength();
				long complete = entry.getValue().longValue();

				buffer.append(getStatus(complete, total)).append("  ");
			}

			int pad = lastLength - buffer.length();
			lastLength = buffer.length();
			pad(buffer, pad);
			buffer.append('\r');

			out.print(buffer);
		}

		private String getStatus(long complete, long total) {
			if (total >= 1024) {
				return toKB(complete) + "/" + toKB(total) + " KB ";
			} else if (total >= 0) {
				return complete + "/" + total + " B ";
			} else if (complete >= 1024) {
				return toKB(complete) + " KB ";
			} else {
				return complete + " B ";
			}
		}

		private void pad(StringBuilder buffer, int spaces) {
			String block = "                                        ";
			while (spaces > 0) {
				int n = Math.min(spaces, block.length());
				buffer.append(block, 0, n);
				spaces -= n;
			}
		}

		@Override
		public void transferSucceeded(TransferEvent event) {
			transferCompleted(event);

			TransferResource resource = event.getResource();
			long contentLength = event.getTransferredBytes();
			if (contentLength >= 0) {
				String type = (event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploaded" : "Downloaded");
				String len = contentLength >= 1024 ? toKB(contentLength) + " KB" : contentLength + " B";

				String throughput = "";
				long duration = System.currentTimeMillis() - resource.getTransferStartTime();
				if (duration > 0) {
					DecimalFormat format = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH));
					double kbPerSec = (contentLength / 1024.0) / (duration / 1000.0);
					throughput = " at " + format.format(kbPerSec) + " KB/sec";
				}

				out.println(type + ": " + resource.getRepositoryUrl() + resource.getResourceName() + " (" + len
						+ throughput + ")");
			}
		}

		@Override
		public void transferFailed(TransferEvent event) {
			transferCompleted(event);

			event.getException().printStackTrace(out);
		}

		private void transferCompleted(TransferEvent event) {
			downloads.remove(event.getResource());

			StringBuilder buffer = new StringBuilder(64);
			pad(buffer, lastLength);
			buffer.append('\r');
			out.print(buffer);
		}

		public void transferCorrupted(TransferEvent event) {
			event.getException().printStackTrace(out);
		}

		protected long toKB(long bytes) {
			return (bytes + 1023) / 1024;
		}

	}
}
