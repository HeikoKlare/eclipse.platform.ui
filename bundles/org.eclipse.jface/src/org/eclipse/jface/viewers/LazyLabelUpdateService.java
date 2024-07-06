package org.eclipse.jface.viewers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.swt.widgets.Widget;

/**
 * Used for performing lazy updates of labels that are expensive to calculate
 * via {@link ILazyLabelUpdater}. Uses an {@link ExecutorService} to execute the
 * lazy update operations asynchronously.
 *
 * @since 3.33
 */
public class LazyLabelUpdateService {

	private static LazyLabelUpdateService INSTANCE = new LazyLabelUpdateService();

	private ExecutorService lazyUpdatesExecutorService;

	private Map<Widget, List<ILazyLabelUpdater>> currentlyScheduledUpdaters = new HashMap<>();

	private LazyLabelUpdateService() {
		lazyUpdatesExecutorService = Executors.newCachedThreadPool();
	}

	/**
	 * @return the singleton instance of this update service.
	 */
	public static LazyLabelUpdateService getInstance() {
		return INSTANCE;
	}

	/**
	 * Submits the given update operation for asynchronous execution. All submitted
	 * operations will be executed by a thread pool. There is no guarantee when an
	 * update operation will be scheduled. If the passed updater is {@code null},
	 * nothing will be done.
	 *
	 * @param updater the update operation to be executed asynchronously, may be
	 *                {code null}
	 */
	public void submitUpdate(ILazyLabelUpdater updater) {
		if (updater == null) {
			return;
		}
		Widget widget = updater.getWidget();
		currentlyScheduledUpdaters.computeIfAbsent(widget, __ -> new ArrayList());
		currentlyScheduledUpdaters.get(widget).add(updater);
		lazyUpdatesExecutorService.submit(updater);
	}

	/**
	 * Aborts all update operations submitted for the given {@link Widget} according
	 * to {@link ILazyLabelUpdater#abort()}.
	 *
	 * @param widget the widget for which all submitted update operations shall be
	 *               aborted
	 */
	public void abortUpdates(Widget widget) {
		if (currentlyScheduledUpdaters.containsKey(widget)) {
			currentlyScheduledUpdaters.get(widget).stream().forEach(ILazyLabelUpdater::abort);
			currentlyScheduledUpdaters.remove(widget);
		}
	}

}
