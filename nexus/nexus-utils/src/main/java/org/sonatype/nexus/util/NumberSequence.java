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

/**
 * A simple interface that gives you a sequence of numbers. That might be simple natural numbers sequence, but anything
 * else too.
 * 
 * @author cstamas
 */
public interface NumberSequence
{
    /**
     * Returns the next number in sequence and advances the sequence.
     * 
     * @return
     */
    long next();

    /**
     * Returns the previous number in sequence and digresses the sequence.
     * 
     * @return
     */
    long prev();

    /**
     * Returns the next number in sequence without advancing the sequence. This method will return always the same
     * number unless method {@code next()} is called.
     * 
     * @return
     */
    long peek();

    /**
     * Resets the sequence.
     */
    void reset();
}
