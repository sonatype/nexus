/*
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions
 *
 * This program is free software: you can redistribute it and/or modify it only under the terms of the GNU Affero General
 * Public License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License Version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License Version 3 along with this program.  If not, see
 * http://www.gnu.org/licenses.
 *
 * Sonatype Nexus (TM) Open Source Version is available from Sonatype, Inc. Sonatype and Sonatype Nexus are trademarks of
 * Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation. M2Eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */
Sonatype.repoServer.LogEditPanel = function(config) {
  var config = config || {};
  var defaultConfig = {};
  Ext.apply(this, config, defaultConfig);

  // help text alias
  var ht = Sonatype.repoServer.resources.help.log;

  var formId = Ext.id();

  var rootLoggerLevelStore = new Ext.data.SimpleStore({
        fields : ['value', 'display'],
        data : [['DEBUG', 'DEBUG'], ['INFO', 'INFO'], ['ERROR', 'ERROR']]

      });

  this.formPanel = new Ext.FormPanel({
        region : 'center',
        id : formId,
        trackResetOnLoad : true,
        autoScroll : true,
        border : false,
        frame : true,
        collapsible : false,
        collapsed : false,
        labelWidth : 175,
        layoutConfig : {
          labelSeparator : ''
        },

        items : [{
              xtype : 'fieldset',
              checkboxToggle : false,
              collapsible : true,
              collapsed : false,
              id : formId + '_' + 'logConfig',
              title : 'Configuration',
              anchor : Sonatype.view.FIELDSET_OFFSET,
              autoHeight : true,
              layoutConfig : {
                labelSeparator : ''
              },
              items : [{
                    xtype : 'combo',
                    fieldLabel : 'Root Logger Level',
                    itemCls : 'required-field',
                    allowBlank : false,
                    width : 100,
                    helpText : ht.rootLoggerLevel,
                    name : 'rootLoggerLevel',
                    store : rootLoggerLevelStore,
                    valueField : 'value',
                    displayField : 'display',
                    mode : 'local',
                    triggerAction : 'all',
                    forceSelection : true,
                    selectOnFocus : true
                  }, {
                    xtype : 'textfield',
                    fieldLabel : 'Root Logger Appenders',
                    itemCls : 'required-field',
                    allowBlank : false,
                    helpText : ht.rootLoggerAppenders,
                    name : 'rootLoggerAppenders',
                    anchor : Sonatype.view.FIELD_OFFSET,
                    disabled : true
                  }, {
                    xtype : 'textfield',
                    fieldLabel : 'File Appender Pattern',
                    itemCls : 'required-field',
                    allowBlank : false,
                    helpText : ht.fileAppenderPattern,
                    name : 'fileAppenderPattern',
                    anchor : Sonatype.view.FIELD_OFFSET
                  }, {
                    xtype : 'textfield',
                    fieldLabel : 'File Appender Location',
                    itemCls : 'required-field',
                    allowBlank : false,
                    name : 'fileAppenderLocation',
                    helpText : ht.fileAppenderLocation,
                    anchor : Sonatype.view.FIELD_OFFSET,
                    disabled : true
                  }]
            }],

        buttons : [{
              id : 'savebutton',
              text : 'Save',
              handler : this.saveBtnHandler,
              disabled : true,
              scope : this
            }, {
              id : 'cancelbutton',
              text : 'Cancel',
              handler : this.cancelBtnHandler,
              scope : this
            }]

      });

  Sonatype.repoServer.LogEditPanel.superclass.constructor.call(this, {
        autoScroll : false,
        layout : 'border',
        items : [this.formPanel]
      });

  this.formPanel.on('beforerender', this.beforeRenderHandler, this.formPanel);
  this.formPanel.on('afterlayout', this.afterLayoutHandler, this, {
        single : true
      });
};

Ext.extend(Sonatype.repoServer.LogEditPanel, Ext.Panel, {

      loadLogConfig : function() {
        var fpanel = this.formPanel;
        this.formPanel.getForm().doAction('sonatypeLoad', {
              url : Sonatype.config.repos.urls.logConfig,
              method : 'GET',
              fpanel : fpanel
            });
      },

      beforeRenderHandler : function() {
        var sp = Sonatype.lib.Permissions;
        if (sp.checkPermission('nexus:logconfig', sp.EDIT))
        {
          this.buttons[0].disabled = false;
        }
      },

      afterLayoutHandler : function() {
        this.loadLogConfig();
      },

      cancelBtnHandler : function() {
        Sonatype.view.mainTabPanel.remove(this.id, true);
      },

      saveBtnHandler : function() {
        var allValid = this.formPanel.getForm().isValid();
        if (allValid)
        {
          this.save();
        }
      },

      save : function() {
        var form = this.formPanel.getForm();
        form.doAction('sonatypeSubmit', {
              url : Sonatype.config.repos.urls.logConfig,
              method : 'PUT',
              waitMsg : 'Updating log configuration...',
              fpanel : this.formPanel,
              serviceDataObj : Sonatype.repoServer.referenceData.logConfig
            });
      }

    });