/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Robin Stocker - Bug 236006 - [Viewers] Add tooltip support for DelegatingStyledCellLabelProvider
 *******************************************************************************/
package org.eclipse.jface.viewers;

import java.util.Arrays;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;

/**
 * A {@link DelegatingStyledCellLabelProvider} is a
 * {@link StyledCellLabelProvider} that delegates requests for the styled string
 * and the image to a
 * {@link DelegatingStyledCellLabelProvider.IStyledLabelProvider}.
 *
 * <p>
 * Existing label providers can be enhanced by implementing
 * {@link DelegatingStyledCellLabelProvider.IStyledLabelProvider} so they can be
 * used in viewers with styled labels.
 * </p>
 *
 * <p>
 * The {@link DelegatingStyledCellLabelProvider.IStyledLabelProvider} can
 * optionally implement {@link IColorProvider} and {@link IFontProvider} to
 * provide foreground and background color and a default font.
 * </p>
 *
 * <p>
 * Since 3.10, {@link DelegatingStyledCellLabelProvider.IStyledLabelProvider}
 * can optionally implement {@link IToolTipProvider} to provide tooltip
 * support.
 * </p>
 *
 * @since 3.4
 */
public class DelegatingStyledCellLabelProvider extends StyledCellLabelProvider {

	/**
	 * Interface marking a label provider that provides styled text labels and
	 * images.
	 * <p>
	 * The {@link DelegatingStyledCellLabelProvider.IStyledLabelProvider} can
	 * optionally implement {@link IColorProvider} and {@link IFontProvider} to
	 * provide foreground and background color and a default font.
	 * </p>
	 */
	public static interface IStyledLabelProvider extends IBaseLabelProvider {

		/**
		 * Returns the styled text label for the given element
		 *
		 * @param element
		 *            the element to evaluate the styled string for
		 *
		 * @return the styled string.
		 */
		public StyledString getStyledText(Object element);

		/**
		 * Returns the image for the label of the given element. The image is
		 * owned by the label provider and must not be disposed directly.
		 * Instead, dispose the label provider when no longer needed.
		 *
		 * @param element
		 *            the element for which to provide the label image
		 * @return the image used to label the element, or <code>null</code>
		 *         if there is no image for the given object
		 */
		public Image getImage(Object element);

		/**
		 * Returns a preview image for the label of the given element. The image is
		 * owned by the label provider and must not be disposed directly. Instead,
		 * dispose the label provider when no longer needed. In case this label provider
		 * cannot provide a fast-calculated image preview, the result will be the same
		 * as {@link #getImage(Object)}. Otherwise, the preview image may be used for a
		 * fast UI update that can be updated with the expensively calculated one of
		 * {@link #getImage(Object)}. Note that there is no guarantee that the
		 * calculation of the preview image will happen fast.
		 *
		 * @param element the element for which to provide the label image
		 * @return the preview image used to label the element, or <code>null</code> if
		 *         there is no image for the given object
		 * @since 3.36
		 */
		public default Image getPreviewImage(Object element) {
			return getImage(element);
		}
	}

	private IStyledLabelProvider styledLabelProvider;

	/**
	 * Creates a {@link DelegatingStyledCellLabelProvider} that delegates the
	 * requests for the styled labels and the images to a
	 * {@link IStyledLabelProvider}.
	 *
	 * @param labelProvider
	 *            the label provider that provides the styled labels and the
	 *            images
	 */
	public DelegatingStyledCellLabelProvider(IStyledLabelProvider labelProvider) {
		if (labelProvider == null)
			throw new IllegalArgumentException(
					"Label provider must not be null"); //$NON-NLS-1$

		this.styledLabelProvider = labelProvider;
	}

	@Override
	public void update(ViewerCell cell) {
		update(cell, false);
		// no super call required. changes on item will trigger the refresh.

	}

	/**
	 * @since 3.36
	 */
	@Override
	public ILazyLabelUpdater fastUpdate(ViewerCell cell) {
		update(cell, true);
		return new ImageUpdater(cell);
		// no super call required. changes on item will trigger the refresh.
	}

	private void update(ViewerCell cell, boolean fast) {
		Object element = cell.getElement();

		StyledString styledString = getStyledText(element);
		String newText= styledString.toString();

		StyleRange[] oldStyleRanges= cell.getStyleRanges();
		StyleRange[] newStyleRanges= isOwnerDrawEnabled() ? styledString.getStyleRanges() : null;

		if (!Arrays.equals(oldStyleRanges, newStyleRanges)) {
			cell.setStyleRanges(newStyleRanges);
			if (cell.getText().equals(newText)) {
				// make sure there will be a refresh from a change
				cell.setText(""); //$NON-NLS-1$
			}
		}

		cell.setText(newText);
		if (fast) {
			cell.setImage(getPreviewImage(element));
		} else {
			cell.setImage(getImage(element));
		}
		cell.setFont(getFont(element));
		cell.setForeground(getForeground(element));
		cell.setBackground(getBackground(element));
	}

	private class ImageUpdater implements ILazyLabelUpdater {
		private ViewerCell cell;

		private boolean aborted;

		private ImageUpdater(ViewerCell cell) {
			this.cell = cell.getViewerRow().getCell(cell.getColumnIndex());
		}

		@Override
		public Widget getWidget() {
			return cell.getViewerRow().getItem();
		}

		@Override
		public void run() {
			if (aborted) {
				return;
			}
			Image image = getImage(cell.getElement());
			if (aborted || image == null) {
				return;
			}
			Display.getDefault().asyncExec(() -> {
				synchronized (this) {
					if (!aborted && !getWidget().isDisposed()) {
						cell.setImage(image);
					}
				}
			});
		}

		@Override
		public synchronized void abort() {
			aborted = true;
		}
	}

	/**
	 * Provides a foreground color for the given element.
	 *
	 * @param element
	 *            the element
	 * @return the foreground color for the element, or <code>null</code> to
	 *         use the default foreground color
	 */
	public Color getForeground(Object element) {
		if (this.styledLabelProvider instanceof IColorProvider) {
			return ((IColorProvider) this.styledLabelProvider)
					.getForeground(element);
		}
		return null;
	}

	/**
	 * Provides a background color for the given element.
	 *
	 * @param element
	 *            the element
	 * @return the background color for the element, or <code>null</code> to
	 *         use the default background color
	 */
	public Color getBackground(Object element) {
		if (this.styledLabelProvider instanceof IColorProvider) {
			return ((IColorProvider) this.styledLabelProvider)
					.getBackground(element);
		}
		return null;
	}

	/**
	 * Provides a font for the given element.
	 *
	 * @param element
	 *            the element
	 * @return the font for the element, or <code>null</code> to use the
	 *         default font
	 */
	public Font getFont(Object element) {
		if (this.styledLabelProvider instanceof IFontProvider) {
			return ((IFontProvider) this.styledLabelProvider).getFont(element);
		}
		return null;
	}

	@Override
	public String getToolTipText(Object element) {
		if (styledLabelProvider instanceof IToolTipProvider) {
			return ((IToolTipProvider) this.styledLabelProvider).getToolTipText(element);
		}
		return super.getToolTipText(element);
	}

	/**
	 * Returns the image for the label of the given element. The image is owned
	 * by the label provider and must not be disposed directly. Instead, dispose
	 * the label provider when no longer needed.
	 *
	 * @param element
	 *            the element for which to provide the label image
	 * @return the image used to label the element, or <code>null</code> if
	 *         there is no image for the given object
	 */
	public Image getImage(Object element) {
		return this.styledLabelProvider.getImage(element);
	}

	private Image getPreviewImage(Object element) {
		return this.styledLabelProvider.getPreviewImage(element);
	}

	/**
	 * Returns the styled text for the label of the given element.
	 *
	 * @param element
	 *            the element for which to provide the styled label text
	 * @return the styled text string used to label the element
	 */
	protected StyledString getStyledText(Object element) {
		return this.styledLabelProvider.getStyledText(element);
	}

	/**
	 * Returns the styled string provider.
	 *
	 * @return the wrapped label provider
	 */
	public IStyledLabelProvider getStyledStringProvider() {
		return this.styledLabelProvider;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		super.addListener(listener);
		this.styledLabelProvider.addListener(listener);
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		super.removeListener(listener);
		this.styledLabelProvider.removeListener(listener);
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return this.styledLabelProvider.isLabelProperty(element, property);
	}

	@Override
	public void dispose() {
		super.dispose();
		this.styledLabelProvider.dispose();
	}

}
