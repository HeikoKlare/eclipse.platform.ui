package org.eclipse.ui.tests.harness.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

/**
 * @since 3.3
 */
public class CloseWindowsExtension implements BeforeEachCallback, AfterEachCallback {

	private static final String WINDOW_LISTENER_STORE_KEY = "CloseTestWindowsExtension_WindowListener";

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		TestWindowListener windowListener = addWindowListener();
		context.getStore(Namespace.GLOBAL).put(WINDOW_LISTENER_STORE_KEY, windowListener);
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		TestWindowListener windowListener = (TestWindowListener) context.getStore(Namespace.GLOBAL)
				.get(WINDOW_LISTENER_STORE_KEY);
		removeWindowListener(windowListener);
		UITestCase.processEvents();
		closeAllTestWindows(windowListener);
		UITestCase.processEvents();
	}

	private TestWindowListener addWindowListener() {
		TestWindowListener windowListener = new TestWindowListener();
		PlatformUI.getWorkbench().addWindowListener(windowListener);
		return windowListener;
	}

	private void removeWindowListener(TestWindowListener windowListener) {
		if (windowListener != null) {
			PlatformUI.getWorkbench().removeWindowListener(windowListener);
		}
	}

	public void closeAllTestWindows(TestWindowListener windowListener) {
		List<IWorkbenchWindow> testWindowsCopy = new ArrayList<>(windowListener.openWindows);
		for (IWorkbenchWindow testWindow : testWindowsCopy) {
			testWindow.close();
		}
	}

	class TestWindowListener implements IWindowListener {
		private final Set<IWorkbenchWindow> openWindows = new HashSet<>();

		@Override
		public void windowActivated(IWorkbenchWindow window) {
			// do nothing
		}

		@Override
		public void windowDeactivated(IWorkbenchWindow window) {
			// do nothing
		}

		@Override
		public void windowClosed(IWorkbenchWindow window) {
			openWindows.remove(window);
		}

		@Override
		public void windowOpened(IWorkbenchWindow window) {
			openWindows.add(window);
		}
	}
}
