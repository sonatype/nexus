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
package org.sonatype.nexus.proxy.attributes.upgrade;

/**
 * Component responsible for "upgrading" of the legacy attributes to new attributes on upgraded systems.
 * 
 * @author cstamas
 * @since 2.0
 */
public interface AttributeUpgrader
{
    /**
     * Returns true if folder holding legacy attributes is present.
     * 
     * @return
     */
    boolean isLegacyAttributesDirectoryPresent();

    /**
     * Returns true if {@link #isLegacyAttributesDirectoryPresent()} returns true, and those folders are not marked as
     * "done".
     * 
     * @return
     */
    boolean isUpgradeNeeded();

    /**
     * Returns true if the UpgraderThread is started, and is alive.
     * 
     * @return
     */
    boolean isUpgradeRunning();

    /**
     * Returns true if {@link #isUpgradeRunning()} returns false, and {@link #isLegacyAttributesDirectoryPresent()}
     * returns true, and those folders are marked as "done".
     * 
     * @return
     */
    boolean isUpgradeFinished();

    /**
     * Returns the global average TPS of currently executing upgrade (if {@link #isUpgradeRunning()} returns true).
     * Otherwise, it returns -1.
     * 
     * @return
     */
    int getGlobalAverageTps();

    /**
     * Returns the global maximum TPS achieved by currently executing upgrade (if {@link #isUpgradeRunning()} returns
     * true). Otherwise, it returns -1.
     * 
     * @return
     */
    int getGlobalMaximumTps();

    /**
     * Returns the last slice's TPS of currently executing upgrade (if {@link #isUpgradeRunning()} returns true).
     * Otherwise, it returns -1.
     * 
     * @return
     */
    int getLastSliceTps();

    /**
     * Returns the last slice's sleep time of currently executing upgrade (if {@link #isUpgradeRunning()} returns true).
     * Otherwise, it returns -1.
     * 
     * @return
     */
    long getCurrentSleepTime();

    /**
     * Returns the minimum sleep time that will be used if throttling is in effect.
     * 
     * @return
     */
    long getMinimumSleepTime();

    /**
     * Sets the minimum sleep time.
     * 
     * @param sleepTime
     */
    void setMinimumSleepTime( long sleepTime );

    /**
     * Returns the max TPS of currently executing upgrade (if {@link #isUpgradeRunning()} returns true). Otherwise, it
     * returns the value of parameter (ie. "this would be it if it would running").
     * 
     * @return
     */
    int getLimiterTps();

    /**
     * Sets the max UPS of currently executing upgrade (if {@link #isUpgradeRunning()} returns true). Otherwise, it is
     * setting the parameter (ie. "this would be it if it would running").
     * 
     * @param limiterTps
     */
    void setLimiterTps( final int limiterTps );

    /**
     * Starts the attributes upgrade if needed (will perform the needed checks and might not start it if decided as not
     * needed). Calls into {@link #upgradeAttributes(false)}.
     */
    void upgradeAttributes();

    /**
     * Starts the attributes upgrade if needed, but overrides user set preferences (will perform the needed checks and
     * might not start it if decided as not needed).
     */
    void upgradeAttributes( boolean force );
}
