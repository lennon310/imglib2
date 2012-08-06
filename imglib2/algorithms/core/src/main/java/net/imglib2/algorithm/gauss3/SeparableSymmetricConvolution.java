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

package net.imglib2.algorithm.gauss3;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.imglib2.Dimensions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.list.ListImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/**
 * Convolution with a separable symmetric kernel.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public final class SeparableSymmetricConvolution
{
	/**
	 * Convolve source with a separable symmetric kernel and write the result to
	 * output. In-place operation (source==target) is supported. If T is
	 * {@link DoubleType}, all calculations are done in double precision. For
	 * all other {@link RealType RealTypes} float is used. General
	 * {@link NumericType NumericTypes} are computed in their own precision.
	 *
	 * @param halfkernels
	 *            an array containing half-kernels for every dimension. A
	 *            half-kernel is the upper half (starting at the center pixel)
	 *            of the symmetric convolution kernel for a given dimension.
	 * @param source
	 *            source image, must be sufficiently padded (e.g.
	 *            {@link Views#extendMirrorSingle(RandomAccessibleInterval)}) to
	 *            provide values for the target interval plus a border of half
	 *            the kernel size.
	 * @param target
	 *            target image.
	 * @param numThreads
	 *            how many threads to use for the computation.
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public static < T extends NumericType< T > > void convolve( final double[][] halfkernels, final RandomAccessible< T > source, final RandomAccessibleInterval< T > target, final int numThreads )
	{
		final T type = Util.getTypeFromInterval( target );
		if ( type instanceof RealType )
		{
			if ( type instanceof DoubleType )
				convolveRealTypeDouble( halfkernels, ( RandomAccessible ) source, ( RandomAccessibleInterval ) target, numThreads );
			else
				convolveRealTypeFloat( halfkernels, ( RandomAccessible ) source, ( RandomAccessibleInterval ) target, numThreads );
		}
		else if ( type instanceof NativeType )
		{
			convolveNativeType( halfkernels, ( RandomAccessible ) source, ( RandomAccessibleInterval ) target, numThreads );
		}
		else
		{
			final ConvolverFactory< T, T > convfac = ConvolverNumericType.factory( type );
			convolve( halfkernels, source, target, convfac, convfac, convfac, convfac, new ListImgFactory< T >(), type, numThreads );
		}
	}

	private static < S extends RealType< S >, T extends RealType< T > > void convolveRealTypeFloat( final double[][] halfkernels,
			final RandomAccessible< S > source, final RandomAccessibleInterval< T > target, final int numThreads )
	{
		final FloatType type = new FloatType();
		final ImgFactory< FloatType > imgfac = getImgFactory( target, halfkernels, type );
		if ( canUseBufferedConvolver( target, halfkernels ) )
			convolve( halfkernels, source, target,
					FloatConvolverRealTypeBuffered.< S, FloatType >factory(),
					FloatConvolverRealTypeBuffered.< FloatType, FloatType >factory(),
					FloatConvolverRealTypeBuffered.< FloatType, T >factory(),
					FloatConvolverRealTypeBuffered.< S, T >factory(), imgfac, type, numThreads );
		else
			convolve( halfkernels, source, target,
					FloatConvolverRealType.< S, FloatType >factory(),
					FloatConvolverRealType.< FloatType, FloatType >factory(),
					FloatConvolverRealType.< FloatType, T >factory(),
					FloatConvolverRealType.< S, T >factory(), imgfac, type, numThreads );
	}

	private static < S extends RealType< S >, T extends RealType< T > > void convolveRealTypeDouble( final double[][] halfkernels,
			final RandomAccessible< S > source, final RandomAccessibleInterval< T > target, final int numThreads )
	{
		final DoubleType type = new DoubleType();
		final ImgFactory< DoubleType > imgfac = getImgFactory( target, halfkernels, type );
		if ( canUseBufferedConvolver( target, halfkernels ) )
			convolve( halfkernels, source, target,
					DoubleConvolverRealTypeBuffered.< S, DoubleType >factory(),
					DoubleConvolverRealTypeBuffered.< DoubleType, DoubleType >factory(),
					DoubleConvolverRealTypeBuffered.< DoubleType, T >factory(),
					DoubleConvolverRealTypeBuffered.< S, T >factory(), imgfac, type, numThreads );
		else
			convolve( halfkernels, source, target,
					DoubleConvolverRealType.< S, DoubleType >factory(),
					DoubleConvolverRealType.< DoubleType, DoubleType >factory(),
					DoubleConvolverRealType.< DoubleType, T >factory(),
					DoubleConvolverRealType.< S, T >factory(), imgfac, type, numThreads );
	}

	private static < T extends NumericType< T > & NativeType< T > > void convolveNativeType( final double[][] halfkernels,
			final RandomAccessible< T > source, final RandomAccessibleInterval< T > target, final int numThreads )
	{
		final T type = Util.getTypeFromInterval( target );
		final ConvolverFactory< T, T > convfac;
		if ( canUseBufferedConvolver( target, halfkernels ) )
			convfac = ConvolverNativeTypeBuffered.factory( type );
		else
			convfac = ConvolverNativeType.factory( type );
		final ImgFactory< T > imgfac = getImgFactory( target, halfkernels, type );
		convolve( halfkernels, source, target, convfac, convfac, convfac, convfac, imgfac, type, numThreads );
	}

	public static < S, T > void convolve1d( final double[] halfkernel,
			final RandomAccessible< S > source, final RandomAccessibleInterval< T > target,
			final ConvolverFactory< S, T > convolverFactoryST )
	{
	    final long[] sourceOffset = new long[] { 1 - halfkernel.length };
	    convolveOffset( halfkernel, source, sourceOffset, target, target, 0, convolverFactoryST, 1, 1 );
	}

	/**
	 * Convolve source with a separable symmetric kernel and write the result to
	 * output. In-place operation (source==target) is supported. If T is
	 * {@link DoubleType}, all calculations are done in double precision. For
	 * all other {@link RealType RealTypes} float is used. General
	 * {@link NumericType NumericTypes} are computed in their own precision.
	 *
	 * @param halfkernels
	 *            an array containing half-kernels for every dimension. A
	 *            half-kernel is the upper half (starting at the center pixel)
	 *            of the symmetric convolution kernel for a given dimension.
	 * @param source
	 *            source image, must be sufficiently padded (e.g.
	 *            {@link Views#extendMirrorSingle(RandomAccessibleInterval)}) to
	 *            provide values for the target interval plus a border of half
	 *            the kernel size.
	 * @param target
	 *            target image.
	 * @param convolverFactorySI
	 *            produces line convolvers reading source type and writing
	 *            temporary type.
	 * @param convolverFactoryII
	 *            produces line convolvers reading temporary type and writing
	 *            temporary type.
	 * @param convolverFactoryIT
	 *            produces line convolvers reading temporary type and writing
	 *            target type.
	 * @param convolverFactoryST
	 *            produces line convolvers reading source type and writing
	 *            target type.
	 * @param imgFactory
	 *            factory to create temporary images.
	 * @param type
	 *            instance of the temporary image type.
	 * @param numThreads
	 *            how many threads to use for the computation.
	 */
	public static < S, I, T > void convolve( final double[][] halfkernels,
			final RandomAccessible< S > source, final RandomAccessibleInterval< T > target,
			final ConvolverFactory< S, I > convolverFactorySI,
			final ConvolverFactory< I, I > convolverFactoryII,
			final ConvolverFactory< I, T > convolverFactoryIT,
			final ConvolverFactory< S, T > convolverFactoryST,
			final ImgFactory< I > imgFactory, final I type,
			final int numThreads )
	{
		final int n = source.numDimensions();
		if ( n == 1 )
		{
			convolve1d( halfkernels[ 0 ], source, target, convolverFactoryST );
		}
		else
		{
			final int numTasks = numThreads > 1 ? numThreads * 4 : 1;
		    final long[] sourceOffset = new long[ n ];
		    final long[] targetOffset = new long[ n ];
		    target.min( sourceOffset );
		    for ( int d = 0; d < n; ++d )
		    {
		    	targetOffset[ d ] = -sourceOffset[ d ];
		    	sourceOffset[ d ] += 1 - halfkernels[ d ].length;
		    }

			final long[][] tmpdims = getTempImageDimensions( target, halfkernels );
			Img< I > tmp1 = imgFactory.create( tmpdims[ 0 ], type );
		    if ( n == 2 )
		    {
			    convolveOffset( halfkernels[ 0 ], source, sourceOffset, tmp1, tmp1, 0, convolverFactorySI, numThreads, numTasks );
			    convolveOffset( halfkernels[ 1 ], tmp1, targetOffset, target, target, 1, convolverFactoryIT, numThreads, numTasks );
		    }
		    else
		    {
				Img< I > tmp2 = imgFactory.create( tmpdims[ 1 ], type );
			    final long[] zeroOffset = new long[ n ];
			    convolveOffset( halfkernels[ 0 ], source, sourceOffset, tmp1, new FinalInterval( tmpdims[ 0 ] ), 0, convolverFactorySI, numThreads, numTasks );
				for( int d = 1; d < n - 1; ++d )
				{
				    convolveOffset( halfkernels[ d ], tmp1, zeroOffset, tmp2, new FinalInterval( tmpdims[ d ] ), d, convolverFactoryII, numThreads, numTasks );
				    final Img< I > tmp = tmp2;
				    tmp2 = tmp1;
				    tmp1 = tmp;
				}
			    convolveOffset( halfkernels[ n - 1 ], tmp1, targetOffset, target, target, n - 1, convolverFactoryIT, numThreads, numTasks );
		    }
		}
	}

	/**
	 * 1D convolution in dimension d.
	 */
	static < S, T > void convolveOffset( final double[] halfkernel, final RandomAccessible< S > source, final long[] sourceOffset, final RandomAccessible< T > target, final Interval targetInterval, final int d, final ConvolverFactory< S, T > factory, final int numThreads, final int numTasks )
	{
		final int n = source.numDimensions();
		final int k1 = halfkernel.length - 1;
		long tmp = 1;
		for ( int i = 0; i < n; ++i )
			if ( i != d)
				tmp *= targetInterval.dimension( i );
		final long endIndex = tmp;

		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		final long[] dim = new long[ n ];
		targetInterval.min( min );
		targetInterval.max( max );
		targetInterval.dimensions( dim );
		dim[ d ] = 1;

		final long[] srcmin = new long[ n ];
		final long[] srcmax = new long[ n ];
		for ( int i = 0; i < n; ++i )
		{
			srcmin[ i ] = min[ i ] + sourceOffset[ i ];
			srcmax[ i ] = max[ i ] + sourceOffset[ i ] + 2 * k1;
		}

		final ExecutorService ex = Executors.newFixedThreadPool( numThreads );
		for ( int taskNum = 0; taskNum < numTasks; ++taskNum )
		{
			final long myStartIndex = taskNum * ( ( endIndex + 1 ) / numTasks );
			final long myEndIndex = ( taskNum == numTasks - 1 ) ?
					endIndex :
					( taskNum + 1 ) * ( ( endIndex + 1 ) / numTasks );
			final Runnable r = new Runnable()
			{
				@Override
				public void run()
				{
					final RandomAccess< S > in = source.randomAccess( new FinalInterval( srcmin, srcmax ) );
					final RandomAccess< T > out = target.randomAccess( targetInterval );
					final Runnable convolver = factory.create( halfkernel, in, out, d, targetInterval.dimension( d ) );

					out.setPosition( min );
					in.setPosition( srcmin );

					final long[] moveToStart = new long[ n ];
					IntervalIndexer.indexToPosition( myStartIndex, dim, moveToStart );
					out.move( moveToStart );
					in.move( moveToStart );

					for( long index = myStartIndex; index < myEndIndex; ++index )
					{
						convolver.run();
						out.setPosition( min[ d ], d );
						in.setPosition( srcmin[ d ], d );
						for ( int i = 0; i < n; ++i )
						{
							if ( i != d )
							{
								out.fwd( i );
								if ( out.getLongPosition( i ) > max[ i ] )
								{
									out.setPosition( min[ i ], i );
									in.setPosition( srcmin[ i ], i );
								}
								else
								{
									in.fwd( i );
									break;
								}
							}
						}
					}
				}
			};
			ex.execute( r );
		}
		ex.shutdown();
		try
		{
			ex.awaitTermination( 1000, TimeUnit.DAYS );
		}
		catch ( final InterruptedException e )
		{
			e.printStackTrace();
		}
	}

	static long[][] getTempImageDimensions( final Dimensions targetsize, final double[][] halfkernels )
	{
		final int n = targetsize.numDimensions();
		final long[][] tmpdims = new long[ n ][];
		tmpdims[ n - 1 ] = new long[ n ];
		targetsize.dimensions( tmpdims[ n - 1 ] );
		for( int d = n - 2; d >= 0; --d )
		{
			tmpdims[ d ] = tmpdims[ d + 1 ].clone();
			tmpdims[ d ][ d + 1 ] += 2 * halfkernels[ d + 1 ].length;
		}
		return tmpdims;
	}

	static boolean canUseBufferedConvolver( final Dimensions targetsize, final double[][] halfkernels )
	{
		final int n = targetsize.numDimensions();
		for( int d = 0; d < n; ++d )
			if ( targetsize.dimension( d ) + 4 * halfkernels[ d ].length - 4 > Integer.MAX_VALUE )
				return false;
		return true;
	}

	static boolean canUseArrayImgFactory( final Dimensions targetsize, final double[][] halfkernels )
	{
		final int n = targetsize.numDimensions();
		long size = targetsize.dimension( 0 );
		for( int d = 1; d < n; ++d )
			size *= targetsize.dimension( d ) + 2 * halfkernels[ d ].length;
		return size <= Integer.MAX_VALUE;
	}

	static < T extends NativeType< T > > ImgFactory< T > getImgFactory( final Dimensions targetsize, final double[][] halfkernels, final T type )
	{
		if ( canUseArrayImgFactory( targetsize, halfkernels ) )
			return new ArrayImgFactory< T >();
		else
		{
			final int cellSize = ( int ) Math.pow( Integer.MAX_VALUE / type.getEntitiesPerPixel(), 1.0 / targetsize.numDimensions() );
			return new CellImgFactory< T >( cellSize );
		}
	}
}
