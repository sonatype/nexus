/*
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

/*global Ext, Nexus, Sonatype, console*/

Ext.namespace('Nexus');

/**
 * Nexus logging utility.
 *
 * @static
 */
Nexus.Log = (function() {
  var log = function(level, msg) {
    if (console) {
      console.log(level + ' ' + msg);
    }
  };

  return {
    /**
     * @static
     * @param msg {String} The message to log.
     */
    debug : function(msg) {
      // Sonatype.config may be undefined if Nexus.log is called on setup of ext js extensions
      // it should always be there on runtime for UI components, but better safe than sorry
      if ( Sonatype.config && Sonatype.config.isDebug )
      {
        log('DEBUG', msg);
      }
    },
    /**
     * @static
     * @param msg {String} The message to log.
     */
    info : function(msg) {
      log('INFO', msg);
    },
    /**
     * @static
     * @param msg {String} The message to log.
     */
    warn : function(msg) {
      log('WARN', msg);
    },
    /**
     * @static
     * @param msg {String} The message to log.
     */
    error : function(msg) {
      log('WARN', msg);
    }
  };
}());

/**
 * @static
 * @see Nexus.Log.debug
 */
Nexus.log = Nexus.Log.debug;
