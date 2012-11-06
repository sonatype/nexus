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
/*
 * View Nexus serer XML configuration file
 */

define('repoServer/ConfigViewPanel',['sonatype/all'], function(){
Sonatype.repoServer.ConfigViewPanel = function(config) {
  var config = config || {};
  var defaultConfig = {};
  Ext.apply(this, config, defaultConfig);

  this.listeners = {
    // note: this isn't pre-render dependent, we just need an early event to
    // start this off
    'beforerender' : this.getConfigFile,
    scope : this
  };

  Sonatype.repoServer.ConfigViewPanel.superclass.constructor.call(this, {
        autoScroll : false,
        border : false,
        frame : false,
        collapsible : false,
        collapsed : false,
        tbar : [{
              text : 'Refresh',
              icon : Sonatype.config.resourcePath + '/images/icons/arrow_refresh.png',
              cls : 'x-btn-text-icon',
              tooltip : {
                text : 'Reloads the config file'
              },
              scope : this,
              handler : this.getConfigFile
            }, {
              text : 'Download Config',
              icon : Sonatype.config.resourcePath + '/images/icons/page_white_put.png',
              cls : 'x-btn-text-icon',
              scope : this,
              handler : function() {
                Sonatype.utils.openWindow(Sonatype.config.repos.urls.configCurrent);
              }
            }],
        items : [{
              xtype : 'textarea',
              id : 'config-text',
              readOnly : true,
              hideLabel : true,
              anchor : '100% 100%'
            }]
      });

  this.configTextArea = this.findById('config-text');
};

Ext.extend(Sonatype.repoServer.ConfigViewPanel, Ext.form.FormPanel, {
      getConfigFile : function() {
        Ext.Ajax.request({
              callback : this.renderResponse,
              scope : this,
              method : 'GET',
              headers : {
                'accept' : 'application/xml'
              },
              url : Sonatype.config.repos.urls.configCurrent
            });
      },

      renderResponse : function(options, success, response) {
        if (success)
        {
          this.configTextArea.setRawValue(response.responseText);
        }
        else
        {
          Sonatype.MessageBox.alert('The data failed to load from the server.');
        }
      }

    });
});

