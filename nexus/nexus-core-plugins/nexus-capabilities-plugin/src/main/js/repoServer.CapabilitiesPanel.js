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
/*
 * Capabilities Edit/Create panel layout and controller
 */

var CAPABILITIES_SERVICE_PATH = Sonatype.config.servicePath + '/capabilities';
var CAPABILITY_TYPES_SERVICE_PATH = Sonatype.config.servicePath + '/capabilityTypes';

Sonatype.repoServer.CapabilitiesPanel = function(config) {
  var config = config || {};
  var defaultConfig = {};
  Ext.apply(this, config, defaultConfig);

  var ht = Sonatype.repoServer.resources.help.capabilities;

  this.actions = {
    doRefresh : new Ext.Action({
          text : 'Refresh',
          iconCls : 'st-icon-refresh',
          scope : this,
          handler : this.reloadAll
        }),
    doDelete : new Ext.Action({
          text : 'Delete',
          scope : this,
          handler : this.deleteHandler
        })
  };

  // A record to hold the name and id of a repository
  this.repositoryRecordConstructor = Ext.data.Record.create([{
        name : 'id'
      }, {
        name : 'name',
        sortType : Ext.data.SortTypes.asUCString
      }]);

  // A record to hold the name and id of a repository group
  this.repositoryGroupRecordConstructor = Ext.data.Record.create([{
        name : 'id'
      }, {
        name : 'name',
        sortType : Ext.data.SortTypes.asUCString
      }]);

  this.repositoryOrGroupRecordConstructor = Ext.data.Record.create([{
        name : 'id'
      }, {
        name : 'name',
        sortType : Ext.data.SortTypes.asUCString
      }]);

  // A record to hold details of each capability type
  this.capabilityTypeRecordConstructor = Ext.data.Record.create([{
        name : 'id',
        sortType : Ext.data.SortTypes.asUCString
      }, {
        name : 'name'
      }, {
        name : 'formFields'
      }]);

  // A record that holds the data for each configured capability in the system
  this.capabilityRecordConstructor = Ext.data.Record.create([{
        name : 'resourceURI'
      }, {
        name : 'id'
      }, {
        name : 'description',
        sortType : Ext.data.SortTypes.asUCString
      }, {
        name : 'notes',
        sortType : Ext.data.SortTypes.asUCString
      }, {
        name : 'enabled'
      }, {
        name : 'active'
      }, {
        name : 'typeName'
      }, {
        name : 'typeId'
      }, {
        name : 'stateDescription'
      }]);

  // Datastore that will hold both repos and repogroups
  this.repoOrGroupDataStore = new Ext.data.SimpleStore({
        fields : ['id', 'name'],
        id : 'id'
      });

  // Reader and datastore that queries the server for the list of repositories
  this.repositoryReader = new Ext.data.JsonReader({
        root : 'data',
        id : 'id'
      }, this.repositoryRecordConstructor);
  this.repositoryDataStore = new Ext.data.Store({
        url : Sonatype.config.repos.urls.repositories,
        reader : this.repositoryReader,
        sortInfo : {
          field : 'name',
          direction : 'ASC'
        },
        autoLoad : true,
        listeners : {
          'load' : {
            fn : function() {
              this.repositoryDataStore.each(function(item, i, len) {
                    var newRec = new this.repositoryOrGroupRecordConstructor({
                          id : 'repo_' + item.data.id,
                          name : item.data.name + ' (Repo)'
                        }, 'repo_' + item.id);
                    this.repoOrGroupDataStore.add([newRec]);
                  }, this);
              var allRec = new this.repositoryRecordConstructor({
                    id : 'all_repo',
                    name : 'All Repositories'
                  }, 'all_repo');
              this.repoOrGroupDataStore.insert(0, allRec);
            },
            scope : this
          }
        }
      });

  // Reader and datastore that queries the server for the list of repository
  // groups
  this.repositoryGroupReader = new Ext.data.JsonReader({
        root : 'data',
        id : 'id'
      }, this.repositoryGroupRecordConstructor);
  this.repositoryGroupDataStore = new Ext.data.Store({
        url : Sonatype.config.repos.urls.groups,
        reader : this.repositoryGroupReader,
        sortInfo : {
          field : 'name',
          direction : 'ASC'
        },
        autoLoad : true,
        listeners : {
          'load' : {
            fn : function() {
              this.repositoryGroupDataStore.each(function(item, i, len) {
                    var newRec = new this.repositoryOrGroupRecordConstructor({
                          id : 'group_' + item.data.id,
                          name : item.data.name + ' (Group)'
                        }, 'group_' + item.id);
                    this.repoOrGroupDataStore.add([newRec]);
                  }, this);
            },
            scope : this
          }
        }
      });

  // Reader and datastore that queries the server for the list of capabilities
  // types
  this.capabilityTypeReader = new Ext.data.JsonReader({
        root : 'data',
        id : 'id'
      }, this.capabilityTypeRecordConstructor);
  this.capabilityTypeDataStore = new Ext.data.Store({
        url : CAPABILITY_TYPES_SERVICE_PATH,
        reader : this.capabilityTypeReader,
        sortInfo : {
          field : 'id',
          direction : 'ASC'
        },
        autoLoad : true
      });

  // Reader and datastore that queries the server for the list of currently
  // defined capabilities
  this.capabilitiesReader = new Ext.data.JsonReader({
        root : 'data',
        id : 'resourceURI'
      }, this.capabilityRecordConstructor);
  this.capabilitiesDataStore = new Ext.data.Store({
        url : CAPABILITIES_SERVICE_PATH,
        reader : this.capabilitiesReader,
        sortInfo : {
          field : 'description',
          direction : 'ASC'
        },
        autoLoad : true
      });

  this.COMBO_WIDTH = 300;

  // Build the form
  this.formConfig = {};
  this.formConfig.capability = {
    region : 'center',
    width : '100%',
    height : '100%',
    autoScroll : true,
    border : false,
    frame : true,
    collapsible : false,
    collapsed : false,
    labelWidth : 200,
    layoutConfig : {
      labelSeparator : ''
    },

    items : [{
          xtype : 'hidden',
          name : 'id'
        }, {
          xtype : 'checkbox',
          fieldLabel : 'Enabled',
          labelStyle : 'margin-left: 15px; width: 185px;',
          helpText : 'This flag determines if the capability is currently active. To disable this capability for a period of time, de-select this checkbox.',
          name : 'enabled',
          allowBlank : false,
          checked : true
        }, {
          xtype : 'combo',
          fieldLabel : 'Type',
          labelStyle : 'margin-left: 15px; width: 185px;',
          itemCls : 'required-field',
          helpText : "Type of configured capability",
          name : 'typeId',
          store : this.capabilityTypeDataStore,
          displayField : 'name',
          valueField : 'id',
          editable : false,
          forceSelection : true,
          mode : 'local',
          triggerAction : 'all',
          emptyText : 'Select...',
          selectOnFocus : true,
          allowBlank : false,
          width : this.COMBO_WIDTH
        }, {
          xtype : 'textfield',
          fieldLabel : 'Notes',
          labelStyle : 'margin-left: 15px; width: 185px;',
          itemCls : '',
          helpText : "Optional notes about configured capability",
          name : 'notes',
          width : this.COMBO_WIDTH,
          allowBlank : true
        }, {
          xtype : 'panel',
          id : 'capability-type-config-card-panel',
          header : false,
          layout : 'card',
          region : 'center',
          activeItem : 0,
          bodyStyle : 'padding:15px',
          deferredRender : false,
          autoScroll : false,
          frame : false,
          items : []
        }],
    buttons : [{
          id : 'savebutton',
          text : 'Save',
          disabled : true
        }, {
          id : 'cancelbutton',
          text : 'Cancel'
        }]
  };

  this.sp = Sonatype.lib.Permissions;

  this.capabilitiesGridPanel = new Ext.grid.GridPanel({
        title : 'Capabilities',
        id : 'st-capabilities-grid',

        region : 'north',
        layout : 'fit',
        collapsible : true,
        split : true,
        height : 200,
        minHeight : 150,
        maxHeight : 400,
        frame : false,
        autoScroll : true,
        tbar : [{
              id : 'capability-refresh-btn',
              text : 'Refresh',
              icon : Sonatype.config.resourcePath + '/images/icons/arrow_refresh.png',
              cls : 'x-btn-text-icon',
              scope : this,
              handler : this.reloadAll
            }, {
              id : 'capability-add-btn',
              text : 'Add',
              icon : Sonatype.config.resourcePath + '/images/icons/add.png',
              cls : 'x-btn-text-icon',
              scope : this,
              handler : this.addResourceHandler,
              disabled : !this.sp.checkPermission('nexus:capabilities', this.sp.CREATE)
            }, {
              id : 'capability-delete-btn',
              text : 'Delete',
              icon : Sonatype.config.resourcePath + '/images/icons/delete.png',
              cls : 'x-btn-text-icon',
              scope : this,
              handler : this.deleteHandler,
              disabled : !this.sp.checkPermission('nexus:capabilities', this.sp.DELETE)
            }],

        // grid view options
        ds : this.capabilitiesDataStore,
        sortInfo : {
          field : 'description',
          direction : "ASC"
        },
        loadMask : true,
        deferredRender : false,
        columns : [{
              header : 'Enabled',
              dataIndex : 'enabled',
              width : 50,
              id : 'capabilities-enabled-col'
            }, {
              header : 'Active',
              dataIndex : 'active',
              width : 50,
              renderer:this.renderActive.createDelegate(this),
              id : 'capabilities-active-col'
            }, {
              header : 'Type',
              dataIndex : 'typeName',
              width : 175,
              id : 'capabilities-type-col'
            }, {
              header : 'Description',
              dataIndex : 'description',
              width : 250,
              id : 'capabilities-description-col'
            }, {
              header : 'Notes',
              dataIndex : 'notes',
              width : 175,
              id : 'capabilities-notes-col'
            }],
        autoExpandColumn : 'capabilities-notes-col',
        disableSelection : false,
        viewConfig : {
          emptyText : 'Click "Add" to configure a capability.'
        }
      });
  this.capabilitiesGridPanel.getSelectionModel().on('rowselect', this.rowSelect, this);
  this.capabilitiesGridPanel.on('rowcontextmenu', this.contextClick, this);

  Sonatype.repoServer.CapabilitiesPanel.superclass.constructor.call(this, {
        layout : 'border',
        autoScroll : false,
        width : '100%',
        height : '100%',
        items : [this.capabilitiesGridPanel, {
              xtype : 'panel',
              id : 'capability-config-forms',
              title : 'Capabilities Configuration',
              layout : 'card',
              region : 'center',
              activeItem : 0,
              deferredRender : false,
              autoScroll : false,
              frame : false,
              items : [{
                    xtype : 'panel',
                    layout : 'fit',
                    html : '<div class="little-padding">Select a capability to edit it, or click "Add" to configure a new one.</div>'
                  }]
            }]
      });

  this.formCards = this.findById('capability-config-forms');
};

Ext.extend(Sonatype.repoServer.CapabilitiesPanel, Ext.Panel, {
      // Dump the currently stored data and requery for everything
      reloadAll : function() {
        this.capabilitiesDataStore.removeAll();
        this.capabilitiesDataStore.reload();
        this.repoOrGroupDataStore.removeAll();
        this.repositoryDataStore.reload();
        this.repositoryGroupDataStore.reload();
        this.capabilityTypeDataStore.reload();
        this.formCards.items.each(function(item, i, len) {
              if (i > 0)
              {
                this.remove(item, true);
              }
            }, this.formCards);

        this.formCards.getLayout().setActiveItem(0);
      },

      markTreeInvalid : function(tree) {
        var elp = tree.getEl();

        if (!tree.errorEl)
        {
          tree.errorEl = elp.createChild({
                cls : 'x-form-invalid-msg'
              });
          tree.errorEl.setWidth(elp.getWidth(true)); // note removed -20 like
          // on form fields
        }
        tree.invalid = true;
        tree.errorEl.update(tree.invalidText);
        elp.child('.x-panel-body').setStyle({
              'background-color' : '#fee',
              border : '1px solid #dd7870'
            });
        Ext.form.Field.msgFx['normal'].show(tree.errorEl, tree);
      },

      saveHandler : function(formInfoObj) {

        var allValid = false;
        allValid = formInfoObj.formPanel.form.isValid();

        if (allValid)
        {
          var isNew = formInfoObj.isNew;
          var createUri = CAPABILITIES_SERVICE_PATH;
          var updateUri = (formInfoObj.resourceURI) ? formInfoObj.resourceURI : '';
          var form = formInfoObj.formPanel.form;

          form.doAction('sonatypeSubmit', {
            method : (isNew) ? 'POST' : 'PUT',
            url : isNew ? createUri : updateUri,
            waitMsg : isNew ? 'Configuring capability...' : 'Updating capability configuration...',
            fpanel : formInfoObj.formPanel,
            dataModifiers : {
              capability : Sonatype.utils.lowercase,
              properties : this.exportCapabilityPropertiesHelper.createDelegate(this)
            },
            serviceDataObj : {
              id : "",
              enabled : true,
              notes : "",
              typeId : "",
              properties : [{
                    key : "",
                    value : ""
                  }]
            },
            isNew : isNew
              // extra option to send to callback, instead of conditioning on
              // method
            });
        }
      },

      cancelHandler : function(formInfoObj) {
        var formLayout = this.formCards.getLayout();
        var gridSelectModel = this.capabilitiesGridPanel.getSelectionModel();
        var store = this.capabilitiesGridPanel.getStore();

        this.formCards.remove(formInfoObj.formPanel.id, true);

        if (this.formCards.items.length > 1)
        {
          formLayout.setActiveItem(this.formCards.items.length - 1);
          // select the coordinating row in the grid, or none if back to default
          var i = store.indexOfId(formLayout.activeItem.id);
          if (i >= 0)
          {
            gridSelectModel.selectRow(i);
          }
          else
          {
            gridSelectModel.clearSelections();
          }
        }
        else
        {
          formLayout.setActiveItem(0);
          gridSelectModel.clearSelections();
        }

        // delete row from grid if canceling a new repo form
        if (formInfoObj.isNew)
        {
          store.remove(store.getById(formInfoObj.formPanel.id));
        }
      },

      addResourceHandler : function() {

        var id = 'new_capability_' + new Date().getTime();

        var config = Ext.apply({}, this.formConfig.capability, {
              id : id
            });
        config = this.configUniqueIdHelper(id, config);
        Ext.apply(config.items[4].items, FormFieldGenerator(id, 'Settings', 'capabilityProperties_', this.capabilityTypeDataStore, this.repositoryDataStore, this.repositoryGroupDataStore, this.repoOrGroupDataStore, null, this.COMBO_WIDTH));
        var formPanel = new Ext.FormPanel(config);

        formPanel.form.on('actioncomplete', this.actionCompleteHandler, this);
        formPanel.form.on('actionfailed', this.actionFailedHandler, this);
        formPanel.on('afterlayout', this.afterLayoutFormHandler, this, {
              single : true
            });

        var capabilityTypeField = formPanel.find('name', 'typeId')[0];
        capabilityTypeField.on('select', this.capabilityTypeSelectHandler, formPanel);

        var buttonInfoObj = {
          formPanel : formPanel,
          isNew : true
        };

        // save button event handler
        formPanel.buttons[0].on('click', this.saveHandler.createDelegate(this, [buttonInfoObj]));
        // cancel button event handler
        formPanel.buttons[1].on('click', this.cancelHandler.createDelegate(this, [buttonInfoObj]));

        var sp = Sonatype.lib.Permissions;
        if (sp.checkPermission('nexus:tasks', sp.EDIT))
        {
          formPanel.buttons[0].disabled = false;
        }

        // add new form
        this.formCards.add(formPanel);

        // add place holder to grid
        var newRec = new this.capabilityRecordConstructor({
              name : 'New Capability',
              resourceURI : 'new'
            }, id); // use "new_capability_" id instead of resourceURI like the
        // reader does
        this.capabilitiesDataStore.insert(0, [newRec]);
        this.capabilitiesGridPanel.getSelectionModel().selectRow(0);
      },

      afterLayoutFormHandler : function(formPanel, fLayout) {
        // register required field quicktip, but have to wait for elements to
        // show up in DOM
        var temp = function() {
          var els = Ext.select('.required-field .x-form-item-label, .required-field .x-panel-header-text', this.getEl());
          els.each(function(el, els, i) {
                Ext.QuickTips.register({
                      target : el,
                      cls : 'required-field',
                      title : '',
                      text : 'Required Field',
                      enabled : true
                    });
              });
        }.defer(300, formPanel);

      },

      deleteHandler : function() {
        if (this.ctxRecord || this.capabilitiesGridPanel.getSelectionModel().hasSelection())
        {
          var rec = this.ctxRecord ? this.ctxRecord : this.capabilitiesGridPanel.getSelectionModel().getSelected();

          if (rec.data.resourceURI == 'new')
          {
            this.cancelHandler({
                  formPanel : Ext.getCmp(rec.id),
                  isNew : true
                });
          }
          else
          {
            // @note: this handler selects the "No" button as the default
            // @todo: could extend Sonatype.MessageBox to take the button to
            // select as a param
            Sonatype.MessageBox.getDialog().on('show', function() {
                  this.focusEl = this.buttons[2]; // ack! we're offset dependent
                  // here
                  this.focus();
                }, Sonatype.MessageBox.getDialog(), {
                  single : true
                });

            Sonatype.MessageBox.show({
                  animEl : this.capabilitiesGridPanel.getEl(),
                  title : 'Delete Capability configuration?',
                  msg : 'Delete the "' + rec.get('description') + '" capability?',
                  buttons : Sonatype.MessageBox.YESNO,
                  scope : this,
                  icon : Sonatype.MessageBox.QUESTION,
                  fn : function(btnName) {
                    if (btnName == 'yes' || btnName == 'ok')
                    {
                      Ext.Ajax.request({
                            callback : this.deleteCallback,
                            cbPassThru : {
                              resourceId : rec.id
                            },
                            scope : this,
                            method : 'DELETE',
                            url : rec.data.resourceURI
                          });
                    }
                  }
                });
          }
        }
      },

      deleteCallback : function(options, isSuccess, response) {
        if (isSuccess)
        {
          var resourceId = options.cbPassThru.resourceId;
          var formLayout = this.formCards.getLayout();
          var gridSelectModel = this.capabilitiesGridPanel.getSelectionModel();
          var store = this.capabilitiesGridPanel.getStore();

          if (formLayout.activeItem.id == resourceId)
          {
            this.formCards.remove(resourceId, true);
            if (this.formCards.items.length > 0)
            {
              formLayout.setActiveItem(this.formCards.items.length - 1);
              // select the coordinating row in the grid, or none if back to
              // default
              var i = store.indexOfId(formLayout.activeItem.id);
              if (i >= 0)
              {
                gridSelectModel.selectRow(i);
              }
              else
              {
                gridSelectModel.clearSelections();
              }
            }
            else
            {
              formLayout.setActiveItem(0);
              gridSelectModel.clearSelections();
            }
          }
          else
          {
            this.formCards.remove(resourceId, true);
          }

          store.remove(store.getById(resourceId));

          this.capabilitiesDataStore.reload();
        }
        else
        {
          Sonatype.utils.connectionError(response, 'The server did not delete the capability.', null, null, true);
        }
      },

      // (Ext.form.BasicForm, Ext.form.Action)
      actionCompleteHandler : function(form, action) {
        // @todo: handle server error response here!!

        if (action.type == 'sonatypeSubmit')
        {
          var isNew = action.options.isNew;
          var receivedData = action.handleResponse(action.response).data;
          if (isNew)
          {
            // successful create
            var sentData = action.output.data;
            var dataObj = {
              id : receivedData.id,
              description : receivedData.description,
              notes : receivedData.notes,
              enabled : receivedData.enabled,
              active : receivedData.active,
              resourceURI : receivedData.resourceURI,
              typeId : receivedData.typeId,
              typeName : receivedData.typeName,
              stateDescription : receivedData.stateDescription
            };

            var newRec = new this.capabilityRecordConstructor(dataObj, action.options.fpanel.id);

            this.capabilitiesDataStore.remove(this.capabilitiesDataStore.getById(action.options.fpanel.id)); // remove
            // old
            // one
            this.capabilitiesDataStore.addSorted(newRec);
            this.capabilitiesGridPanel.getSelectionModel().selectRecords([newRec], false);

            // set the hidden id field in the form for subsequent updates
            action.options.fpanel.find('name', 'id')[0].setValue(receivedData.id);
            // remove button click listeners
            action.options.fpanel.buttons[0].purgeListeners();
            action.options.fpanel.buttons[1].purgeListeners();

            var buttonInfoObj = {
              formPanel : action.options.fpanel,
              isNew : false,
              resourceURI : dataObj.resourceURI
            };

            // save button event handler
            action.options.fpanel.buttons[0].on('click', this.saveHandler.createDelegate(this, [buttonInfoObj]));

            // cancel button event handler
            action.options.fpanel.buttons[1].on('click', this.cancelHandler.createDelegate(this, [buttonInfoObj]));

            // disable capability type, only avaiable on add
            action.options.fpanel.find('name', 'typeId')[0].disable();
          }
          else
          {
            var sentData = action.output.data;

            var i = this.capabilitiesDataStore.indexOfId(action.options.fpanel.id);
            var rec = this.capabilitiesDataStore.getAt(i);

            this.updateCapabilityRecord(rec, receivedData);

            var sortState = this.capabilitiesDataStore.getSortState();
            this.capabilitiesDataStore.sort(sortState.field, sortState.direction);
          }
          this.capabilitiesDataStore.reload();
        }
      },

      updateCapabilityRecord : function(rec, receivedData) {
        rec.beginEdit();
        rec.set('description', receivedData.description);
        rec.set('notes', receivedData.notes);
        rec.set('typeId', receivedData.typeId);
        rec.set('enabled', receivedData.enabled);
        rec.set('active', receivedData.active);
        rec.set('typeName', receivedData.typeName);
        rec.set('stateDescription', receivedData.stateDescription);
        rec.commit();
        rec.endEdit();
      },

      // (Ext.form.BasicForm, Ext.form.Action)
      actionFailedHandler : function(form, action) {
        if (action.failureType == Ext.form.Action.CLIENT_INVALID)
        {
          Sonatype.MessageBox.alert('Missing or Invalid Fields', 'Please change the missing or invalid fields.').setIcon(Sonatype.MessageBox.WARNING);
        }
        // @note: server validation error are now handled just like client
        // validation errors by marking the field invalid
        // else if(action.failureType == Ext.form.Action.SERVER_INVALID){
        // Sonatype.MessageBox.alert('Invalid Fields', 'The server identified
        // invalid fields.').setIcon(Sonatype.MessageBox.ERROR);
        // }
        else if (action.failureType == Ext.form.Action.CONNECT_FAILURE)
        {
          Sonatype.utils.connectionError(action.response, 'There is an error communicating with the server.')
        }
        else if (action.failureType == Ext.form.Action.LOAD_FAILURE)
        {
          Sonatype.MessageBox.alert('Load Failure', 'The data failed to load from the server.').setIcon(Sonatype.MessageBox.ERROR);
        }

        // @todo: need global alert mechanism for fatal errors.
      },

      formDataLoader : function(formPanel, resourceURI, modFuncs) {
        formPanel.getForm().doAction('sonatypeLoad', {
              url : resourceURI,
              method : 'GET',
              fpanel : formPanel,
              dataModifiers : {
                capability : Sonatype.utils.capitalize,
                properties : this.importCapabilityPropertiesHelper.createDelegate(this)
              },
              scope : this
            });
      },

      rowSelect : function(selectionModel, index, rec) {
        var id = rec.id; // note: rec.id is unique for new resources and equal
        // to resourceURI for existing ones
        var formPanel = this.formCards.findById(id);

        // assumption: new route forms already exist in formCards, so they won't
        // get into this case
        if (!formPanel)
        { // create form and populate current data
          var config = Ext.apply({}, this.formConfig.capability, {
                id : id
              });
          config = this.configUniqueIdHelper(id, config);
          Ext.apply(config.items[4].items, FormFieldGenerator(id, 'Settings', 'capabilityProperties_', this.capabilityTypeDataStore, this.repositoryDataStore, this.repositoryGroupDataStore, this.repoOrGroupDataStore));
          formPanel = new Ext.FormPanel(config);

          formPanel.form.on('actioncomplete', this.actionCompleteHandler, this);
          formPanel.form.on('actionfailed', this.actionFailedHandler, this);
          formPanel.on('afterlayout', this.afterLayoutFormHandler, this, {
                single : true
              });

          // enable capability type panel fields
          var capabilityTypePanel = formPanel.findById(formPanel.id + '_capability-type-config-card-panel');
          capabilityTypePanel.items.each(function(item, i, len) {
                if (item.id == id + '_' + rec.data.typeId)
                {
                  capabilityTypePanel.activeItem = i;
                  if (item.items)
                  {
                    item.items.each(function(item) {
                          item.disabled = false;
                        });
                  }
                }
              });

          formPanel.find('name', 'typeId')[0].disable();

          var buttonInfoObj = {
            formPanel : formPanel,
            isNew : false, // not a new route form, see assumption
            resourceURI : rec.data.resourceURI
          };

          formPanel.buttons[0].on('click', this.saveHandler.createDelegate(this, [buttonInfoObj]));
          formPanel.buttons[1].on('click', this.cancelHandler.createDelegate(this, [buttonInfoObj]));

          var sp = Sonatype.lib.Permissions;
          if (sp.checkPermission('nexus:capabilities', sp.EDIT))
          {
            formPanel.buttons[0].disabled = false;
          }

          this.formDataLoader(formPanel, rec.data.resourceURI);

          this.formCards.add(formPanel);
        }

        // always set active
        this.formCards.getLayout().setActiveItem(formPanel);
        formPanel.doLayout();
      },

      contextClick : function(grid, index, e) {
        this.contextHide();

        if (e.target.nodeName == 'A')
          return; // no menu on links

        this.ctxRow = this.capabilitiesGridPanel.view.getRow(index);
        this.ctxRecord = this.capabilitiesGridPanel.store.getAt(index);
        Ext.fly(this.ctxRow).addClass('x-node-ctx');

        // @todo: would be faster to pre-render the six variations of the menu
        // for whole instance
        var menu = new Ext.menu.Menu({
              id : 'capability-grid-ctx',
              items : [this.actions.doRefresh]
            });

        if (this.sp.checkPermission('nexus:capabilities', this.sp.DELETE))
        {
          menu.add(this.actions.doDelete);
        }

        menu.on('hide', this.contextHide, this);
        e.stopEvent();
        menu.showAt(e.getXY());
      },

      contextHide : function() {
        if (this.ctxRow)
        {
          Ext.fly(this.ctxRow).removeClass('x-node-ctx');
          this.ctxRow = null;
          this.ctxRecord = null;
        }
      },

      capabilityTypeSelectHandler : function(combo, record, index) {
        var capabilityTypePanel = this.findById(this.id + '_capability-type-config-card-panel');
        // First disable all the items currently on screen, so they wont be
        // validated/submitted etc
        capabilityTypePanel.getLayout().activeItem.items.each(function(item) {
              item.disable();
            });
        // Then find the proper card to activate (based upon id of the
        // capabilityType)
        // Then enable the fields in that card
        var formId = this.id;
        capabilityTypePanel.items.each(function(item, i, len) {
              if (item.id == formId + '_' + record.data.id)
              {
                capabilityTypePanel.getLayout().setActiveItem(item);
                item.items.each(function(item) {
                      item.enable();
                    });
              }
            }, capabilityTypePanel);
        capabilityTypePanel.doLayout();
      },

      // creates a unique config object with specific IDs on the two grid item
      configUniqueIdHelper : function(id, config) {
        // @note: there has to be a better way to do this. Depending on offsets
        // is very error prone
        var newConfig = config;

        this.assignItemIds(id, newConfig.items);

        return newConfig;
      },

      assignItemIds : function(id, items) {
        for (var i = 0; i < items.length; i++)
        {
          var item = items[i];
          if (item.id)
          {
            if (!item.originalId)
            {
              item.originalId = item.id;
            }
            item.id = id + '_' + item.originalId;
          }
          if (item.items)
          {
            this.assignItemIds(id, item.items);
          }
        }
      },

      exportCapabilityPropertiesHelper : function(val, fpanel) {
        return FormFieldExporter(fpanel, '_capability-type-config-card-panel', 'capabilityProperties_');
      },

      importCapabilityPropertiesHelper : function(val, srcObj, fpanel) {
        FormFieldImporter(srcObj, fpanel, 'capabilityProperties_');
        return val;
      },

      renderActive:function(val, cell, record) {

        // get data
        var data = record.data;

        // return markup
        return '<div qtip="' + data.stateDescription +'">' + val + '</div>';
      }
    });
