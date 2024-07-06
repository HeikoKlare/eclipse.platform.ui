package org.eclipse.jface.viewers;

import org.eclipse.swt.widgets.Widget;

/**
 * A label update operation that can be executed asynchronously. It is
 * especially useful for expensive label calculation that may otherwise block
 * the UI. The update operation can be executed via the
 * {@link LazyLabelUpdateService}. It provides {@link #abort()} functionality to
 * make the calculation cancellable for the case it is not needed anymore, e.g.,
 * because the using control has been filled with other contents or has been
 * disposed.
 *
 * @since 3.36
 */
public interface ILazyLabelUpdater extends Runnable {

	/**
	 * Executes the label update operation.
	 */
	@Override
	public void run();

	/**
	 * @return the widget whose label is to be updated by this update operation
	 */
	public Widget getWidget();

	/**
	 * Aborts the label update operation. In consequence, no changes will be
	 * performed to the labeled control. In case the label calculation has not been
	 * started, starting it will directly abort. If the calculation already runs, it
	 * is up to the calculation whether it can abort in between.
	 */
	public void abort();

}
