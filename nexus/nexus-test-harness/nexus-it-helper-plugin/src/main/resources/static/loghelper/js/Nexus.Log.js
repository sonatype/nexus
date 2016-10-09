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

/*global Ext, Sonatype, Nexus*/

(function() {
  var
        log = function(level, msg) {
          Ext.Ajax.request({
            url : Sonatype.config.servicePath + "/loghelper?loggerName=Nexus.ui&level=" + level + "&message=" + msg,
            suppressStatus : true
          });
        },
        curry = function(fn, arg) {
          var array = [arg];
          return function()
          {
            // arguments is not a real array, concat et. al. won't work
            Ext.each(arguments, function(item) {
              array.push(item);
            });

            fn.apply(this, array);
          };
        };

  Nexus.Log.debug = Nexus.Log.debug.createSequence(curry(log, 'DEBUG'));
  Nexus.Log.info = Nexus.Log.info.createSequence(curry(log, "INFO"));
  Nexus.Log.warn = Nexus.Log.warn.createSequence(curry(log, "WARN"));
  Nexus.Log.error = Nexus.Log.error.createSequence(curry(log, "ERROR"));
  Nexus.log = Nexus.log.createSequence(curry(log, "DEBUG"));

}());