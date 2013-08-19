/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */
package net.imglib2.ui;

import java.awt.image.BufferedImage;

/**
 * Render source data into a {@link BufferedImage} and provide this to a
 * {@link RenderTarget}. Handle repaint requests by sending them to a
 * {@link PainterThread}.
 *
 * @param <A>
 *            transform type
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public abstract class Renderer< A >
{
	final protected AffineTransformType< A > transformType;

	/**
	 * Receiver for the {@link BufferedImage BufferedImages} that we render.
	 */
	final protected RenderTarget display;

	/**
	 * Thread that triggers repainting of the display.
	 * Requests for repainting are send there.
	 */
	final protected PainterThread painterThread;

	/**
	 *
	 * @param transformType
	 * @param display
	 *            Receiver for the {@link BufferedImage BufferedImages} that we render.
	 * @param painterThread
	 *            Thread that triggers repainting of the display. Requests for
	 *            repainting are send there.
	 */
	public Renderer( final AffineTransformType< A > transformType, final RenderTarget display, final PainterThread painterThread )
	{
		this.display = display;
		this.painterThread = painterThread;
		this.transformType = transformType;
	}

	/**
	 * Request a repaint of the display from the painter thread. The painter
	 * thread will trigger a {@link #paint()} as soon as possible (that is,
	 * immediately or after the currently running {@link #paint()} has
	 * completed).
	 * <p>
	 * <em>All repaint request should be directed through here,
	 * usually not to {@link PainterThread#requestRepaint()} directly</em>. The
	 * reason for this is, that derived classes (i.e.,
	 * {@link MultiResolutionRenderer}) may choose to cancel the on-going
	 * rendering operation when a new repaint request comes in.
	 */
	public void requestRepaint()
	{
		painterThread.requestRepaint();
	}

	/**
	 * Render the given source to our {@link RenderTarget}.
	 * <p>
	 * To do this, transform the source according to the given viewer transform,
	 * render it to a {@link BufferedImage}, and
	 * {@link RenderTarget#setBufferedImage(BufferedImage) hand} that
	 * {@link BufferedImage} to the {@link RenderTarget}.
	 * <p>
	 * Note that the total transformation to apply to the source is a
	 * composition of the {@link RenderSource#getSourceTransform() source
	 * transform} (source to global coordinates) and the viewer transform
	 * (global to screen).
	 *
	 * @param source
	 *            the source data to render.
	 * @param viewerTransform
	 *            transforms global to screen coordinates.
	 * @return whether rendering was successful.
	 */
	public abstract boolean paint( final RenderSource< ?, A > source, final A viewerTransform );
}
