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
Sonatype.repoServer.HelpAboutPanel = function(config) {
  var config = config || {};
  var defaultConfig = {};
  Ext.apply(this, config, defaultConfig);
  
  var helpItems = [{
      //add this one by default, so existing usage will be preserved
      text: this.getHelpText()
  }];
  
  Sonatype.Events.fireEvent('aboutPanelContributions',helpItems);
  
  var helpText = '';
  
  for ( var i = 0 ; i < helpItems.length ; i++ ) {
      helpText += helpItems[i].text;
  }

  Sonatype.repoServer.HelpAboutPanel.superclass.constructor.call(this, {
        layout : 'border',
        autoScroll : false,
        width : '100%',
        height : '100%',
        items : [{
              xtype : 'panel',
              region : 'center',
              layout : 'fit',
              html : helpText
            }]
      });
};

Ext.extend(Sonatype.repoServer.HelpAboutPanel, Ext.Panel, {
  getHelpText : function() {
    return '<div class="little-padding">'
        + 'Sonatype Nexus&trade; ' + Sonatype.utils.edition + ' Version'
        + '<br/>Copyright &copy; 2008-2012 Sonatype, Inc.'
		+ '<br/>All rights reserved. Includes the third-party code listed at <a href="' + Sonatype.utils.attributionsURL + '" target="_new">' + Sonatype.utils.attributionsURL + '</a>.'
		+ '<br/>'
		+ '<br/>This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,'
		+ '<br/>which accompanies this distribution and is available at <a href="http://www.eclipse.org/legal/epl-v10.html" target="_new">http://www.eclipse.org/legal/epl-v10.html</a>.'
		+ '<br/>'
		+ '<br/>Sonatype Nexus&trade; Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks'
		+ '<br/>of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the'
		+ '<br/>Eclipse Foundation. All other trademarks are the property of their respective owners.'
  		+ '</div>';
  }
});
