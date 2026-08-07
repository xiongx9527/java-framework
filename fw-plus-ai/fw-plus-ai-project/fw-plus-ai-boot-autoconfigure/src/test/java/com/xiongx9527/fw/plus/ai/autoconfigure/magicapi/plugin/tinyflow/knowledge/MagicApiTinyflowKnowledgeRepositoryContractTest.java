package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowChunkMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowDifyConnectionMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowDocumentMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowIngestionTaskMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowKnowledgeKeyMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage.TinyflowKnowledgeMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.AbstractTinyflowKnowledgeRepositoryContractTest;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import org.junit.After;
import org.junit.Before;
import org.springframework.context.ApplicationEventPublisher;
import org.ssssssss.magicapi.core.resource.FileResource;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.core.service.impl.DefaultMagicResourceService;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletResponse;
import org.ssssssss.magicapi.core.servlet.MagicRequestContextHolder;
import org.ssssssss.magicapi.utils.WebUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class MagicApiTinyflowKnowledgeRepositoryContractTest
		extends AbstractTinyflowKnowledgeRepositoryContractTest {

	private MagicRequestContextHolder previousContextHolder;

	@Before
	public void installMagicRequestContextHolder() {
		previousContextHolder = WebUtils.magicRequestContextHolder;
		WebUtils.magicRequestContextHolder = new MagicRequestContextHolder() {
			@Override
			public org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest getRequest() {
				return null;
			}

			@Override
			public MagicHttpServletResponse getResponse() {
				return null;
			}
		};
	}

	@After
	public void restoreMagicRequestContextHolder() {
		WebUtils.magicRequestContextHolder = previousContextHolder;
	}

	@Override
	protected TinyflowKnowledgeRepository createRepository() {
		try {
			File root = Files.createTempDirectory("tinyflow-magic-repository-contract").toFile();
			List<MagicResourceStorage<? extends org.ssssssss.magicapi.core.model.MagicEntity>> storages = Arrays
					.<MagicResourceStorage<? extends org.ssssssss.magicapi.core.model.MagicEntity>>asList(
							new TinyflowKnowledgeMagicResourceStorage(), new TinyflowDocumentMagicResourceStorage(),
							new TinyflowChunkMagicResourceStorage(), new TinyflowIngestionTaskMagicResourceStorage(),
							new TinyflowKnowledgeKeyMagicResourceStorage(), new TinyflowDifyConnectionMagicResourceStorage());
			DefaultMagicResourceService service = new DefaultMagicResourceService(
					new FileResource(root, false, root.getAbsolutePath()), storages, new ApplicationEventPublisher() {
						@Override public void publishEvent(Object event) { }
					});
			service.refresh();
			return new MagicApiTinyflowKnowledgeRepository(service);
		} catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
	}

	@Override
	protected void failNextCompletionAt(TinyflowKnowledgeRepository target, String step) {
		repository(target).failNextCompletionAt(step);
	}

	@Override
	protected void failNextCompensationStartAt(TinyflowKnowledgeRepository target, String step) {
		repository(target).failNextCompensationStartAt(step);
	}

	@Override
	protected RepositoryConcurrencyProbe concurrencyProbe(TinyflowKnowledgeRepository target) {
		MagicApiTinyflowKnowledgeRepository repository = repository(target);
		return new RepositoryConcurrencyProbe() {
			@Override public void pauseNextCompletionAfter(String step) { repository.pauseNextCompletionAfter(step); }
			@Override public void awaitCompletionPaused() throws InterruptedException { repository.awaitCompletionPaused(); }
			@Override public void releaseCompletion() { repository.releaseCompletion(); }
			@Override public void expectReaderEntry() { repository.expectReaderEntry(); }
			@Override public void awaitReaderEntered() throws InterruptedException { repository.awaitReaderEntered(); }
			@Override public void awaitReaderBlocked() { repository.awaitReaderBlocked(); }
			@Override public void expectWriterEntry() { repository.expectWriterEntry(); }
			@Override public void awaitWriterEntered() throws InterruptedException { repository.awaitWriterEntered(); }
			@Override public void awaitWriterBlocked() { repository.awaitWriterBlocked(); }
		};
	}

	private MagicApiTinyflowKnowledgeRepository repository(TinyflowKnowledgeRepository target) {
		return (MagicApiTinyflowKnowledgeRepository) target;
	}
}
