package org.eclipse.ui.tests.harness.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * @since 1.11
 *
 */
public class ShellLeakTestExtension implements AfterEachCallback {

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		List<String> leakedModalShellTitles = new ArrayList<>();
		Shell[] shells = PlatformUI.getWorkbench().getDisplay().getShells();
		for (Shell shell : shells) {
			if (!shell.isDisposed() && shell.isVisible()
					&& (shell.getStyle() & (SWT.APPLICATION_MODAL | SWT.PRIMARY_MODAL | SWT.SYSTEM_MODAL)) != 0) {
				leakedModalShellTitles.add(shell.getText());
				shell.close();
			}
		}
		assertEquals(0, leakedModalShellTitles.size(),
				"Test leaked modal shell: [" + String.join(", ", leakedModalShellTitles) + "]");
	}

}
