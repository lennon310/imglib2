/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2012 Stephan Preibisch, Stephan Saalfeld, Tobias
 * Pietzsch, Albert Cardona, Barry DeZonia, Curtis Rueden, Lee Kamentsky, Larry
 * Lindsey, Johannes Schindelin, Christian Dietz, Grant Harris, Jean-Yves
 * Tinevez, Steffen Jaensch, Mark Longair, Nick Perry, and Jan Funke.
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

package net.imglib2.view;

import java.util.Iterator;

import net.imglib2.AbstractInterval;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.IterableRealInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.iteration.IterableTransformBuilder;

/**
 * IntervalView is a view that puts {@link Interval} boundaries on its source
 * {@link RandomAccessible}. IntervalView uses {@link TransformBuilder} to
 * create efficient {@link RandomAccess accessors}. Usually an IntervalView is
 * created through the {@link Views#interval(RandomAccessible, Interval)} method
 * instead.
 */
public class IntervalView< T > extends AbstractInterval implements RandomAccessibleInterval< T >, IterableInterval< T >
{
	protected final RandomAccessible< T > source;

	protected RandomAccessible< T > fullViewRandomAccessible;

	protected IterableInterval< T > fullViewIterableInterval;

	/**
	 * Create a view that defines an interval on a source. It is the callers
	 * responsibility to ensure that the source is defined in the specified
	 * interval.
	 *
	 * @see Views#interval(RandomAccessible, Interval)
	 */
	public IntervalView( final RandomAccessible< T > source, final Interval interval )
	{
		super( interval );
		assert( source.numDimensions() == interval.numDimensions() );

		this.source = source;
		this.fullViewRandomAccessible = null;
	}

	/**
	 * Create a view that defines an interval on a source. It is the callers
	 * responsibility to ensure that the source is defined in the specified
	 * interval.
	 *
	 * @see Views#interval(RandomAccessible, Interval)
	 *
	 * @param min
	 *            minimum coordinate of the interval.
	 * @param max
	 *            maximum coordinate of the interval.
	 */
	public IntervalView( final RandomAccessible< T > source, final long[] min, final long[] max )
	{
		super( min, max );
		assert( source.numDimensions() == min.length );

		this.source = source;
		this.fullViewRandomAccessible = null;
	}

	public RandomAccessible< T > getSource()
	{
		return source;
	}

	@Override
	public RandomAccess< T > randomAccess( final Interval interval )
	{
		return TransformBuilder.getEfficientRandomAccessible( interval, this ).randomAccess();
	}

	@Override
	public RandomAccess< T > randomAccess()
	{
		if ( fullViewRandomAccessible == null )
			fullViewRandomAccessible = TransformBuilder.getEfficientRandomAccessible( this, this );
		return fullViewRandomAccessible.randomAccess();
	}

	protected IterableInterval< T > getFullViewIterableInterval()
	{
		if ( fullViewIterableInterval == null )
			fullViewIterableInterval = IterableTransformBuilder.getEfficientIterableInterval( this, this );
		return fullViewIterableInterval;
	}

	@Override
	public long size()
	{
		return getFullViewIterableInterval().size();
	}

	@Override
	public T firstElement()
	{
		return getFullViewIterableInterval().firstElement();
	}

	@Override
	public Object iterationOrder()
	{
		return getFullViewIterableInterval().iterationOrder();
	}

	@Override
	public boolean equalIterationOrder( final IterableRealInterval< ? > f )
	{
		return iterationOrder().equals( f.iterationOrder() );
	}

	@Override
	public Iterator< T > iterator()
	{
		return getFullViewIterableInterval().iterator();
	}

	@Override
	public Cursor< T > cursor()
	{
		return getFullViewIterableInterval().cursor();
	}

	@Override
	public Cursor< T > localizingCursor()
	{
		return getFullViewIterableInterval().localizingCursor();
	}
}