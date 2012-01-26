/**
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2012 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.util;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;

public class NumberSequenceTest
{
    @Test
    public void testConstantSequence()
    {
        long startValue = 10;

        ConstantNumberSequence cs = new ConstantNumberSequence( startValue );

        for ( int i = 0; i < 20; i++ )
        {
            Assert.assertEquals( startValue, cs.next() );
        }

        cs.reset();

        for ( int i = 0; i < 20; i++ )
        {
            Assert.assertEquals( startValue, cs.next() );
        }
    }

    @Test
    public void testFibonacciSequence()
    {
        int[] fibonacciNumbers = new int[] { 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233 };

        FibonacciNumberSequence fs = new FibonacciNumberSequence();

        for ( int f : fibonacciNumbers )
        {
            Assert.assertEquals( f, fs.next() );
        }

        fs.reset();

        for ( int f : fibonacciNumbers )
        {
            Assert.assertEquals( f, fs.next() );
        }
    }

    @Test
    public void testFoxiedFibonacciSequence()
    {
        int[] fibonacciNumbers = new int[] { 10, 10, 20, 30, 50, 80, 130, 210, 340, 550, 890, 1440, 2330 };

        FibonacciNumberSequence fs = new FibonacciNumberSequence( 10 );

        for ( int f : fibonacciNumbers )
        {
            Assert.assertEquals( f, fs.next() );
        }

        fs.reset();

        for ( int f : fibonacciNumbers )
        {
            Assert.assertEquals( f, fs.next() );
        }
    }

    @Test
    public void testFibonacciSequenceBackAndForth()
    {
        int[] fibonacciNumbers = new int[] { 10, 10, 20, 30, 50, 80, 130, 210, 340, 550, 890, 1440, 2330 };

        FibonacciNumberSequence fs = new FibonacciNumberSequence( 10 );

        for ( int f : fibonacciNumbers )
        {
            Assert.assertEquals( f, fs.next() );
        }

        fs.reset();

        for ( int f : fibonacciNumbers )
        {
            Assert.assertEquals( f, fs.next() );
        }

        ArrayUtils.reverse( fibonacciNumbers );

        for ( int f : fibonacciNumbers )
        {
            Assert.assertEquals( f, fs.prev() );
        }
    }
}
