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
/*global define*/
define('Nexus/util/Format',['extjs', 'nexus'], function(Ext, Nexus) {
  Ext.namespace('Nexus.util');
/**
 * @static
 */
Nexus.util.Format = {
  /**
   * Decode the given string as HTML. Strips all HTML tags before rendering as XSS protection.
   *
   * @param input The input string. May contain HTML entities (e.g. &#26412;&amp;)
   * @return {string} The string as rendered in a browser, with all tags stripped.
   */
  htmlDecode : function(input) {
    if (input === null || input === '') {
      return input;
    }
    var el = document.createElement('div');
    el.innerHTML = Ext.util.Format.stripTags(input);
    return el.childNodes[0].nodeValue;
  }
};

  return Nexus.util.Format;
});

