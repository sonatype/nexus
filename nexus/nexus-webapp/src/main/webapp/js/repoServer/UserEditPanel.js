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
define('repoServer/UserEditPanel',['sonatype/all'], function(){
/*
 * User Edit/Create panel layout and controller
 */

var WHITE_SPACE_REGEX = /^\S/;

Sonatype.repoServer.UserEditPanel = function(config) {
  var config = config || {};
  var defaultConfig = {
    title : 'Users'
  };
  Ext.apply(this, config, defaultConfig);

  this.sp = Sonatype.lib.Permissions;

  this.actions = {
    resetPasswordAction : {
      text : 'Reset Password',
      scope : this,
      handler : this.resetPasswordHandler
    },
    changePasswordAction : {
      text : 'Set Password',
      scope : this,
      handler : this.changePasswordHandler
    }
  };

  this.displaySelector = new Ext.Button({
        text : 'All Configured Users',
        icon : Sonatype.config.resourcePath + '/images/icons/page_white_stack.png',
        cls : 'x-btn-text-icon',
        value : 'allConfigured',
        menu : {
          id : 'user-realm-selector-menu',
          items : [{
                text : 'All Users',
                checked : false,
                handler : this.showUsers,
                value : 'all',
                group : 'user-realm-selector',
                scope : this
              }, {
                text : 'All Authorized Users',
                checked : false,
                handler : this.showUsers,
                value : 'effectiveUsers',
                group : 'user-realm-selector',
                scope : this
              }]
        }
      });

  this.sourceStore = new Ext.data.JsonStore({
        root : 'data',
        id : 'roleHint',
        autoLoad : true,
        url : Sonatype.config.repos.urls.userLocators,
        sortInfo : {
          field : 'description',
          direction : 'ASC'
        },
        fields : [{
              name : 'roleHint'
            }, {
              name : 'description',
              sortType : Ext.data.SortTypes.asUCString
            }],
        listeners : {
          load : {
            fn : this.loadSources,
            scope : this
          }
        }
      });

  this.searchField = new Ext.app.SearchField({
        searchPanel : this,
        disabled : true,
        width : 250,
        emptyText : 'Select a filter to enable username search',

        onTrigger2Click : function() {
          var v = this.getRawValue();
          this.searchPanel.startSearch(this.searchPanel);
        }
      });

  Sonatype.Events.on('userAddMenuInit', this.onAddMenuInit, this);
  Sonatype.Events.on('userMenuInit', this.onUserMenuInit, this);

  Sonatype.repoServer.UserEditPanel.superclass.constructor.call(this, {
        addMenuInitEvent : 'userAddMenuInit',
        deleteButton : this.sp.checkPermission('security:users', this.sp.DELETE),
        rowClickEvent : 'userViewInit',
        rowContextClickEvent : 'userMenuInit',
        url : Sonatype.config.repos.urls.plexusUsersAllConfigured,
        dataAutoLoad : true,
        dataId : 'userId',
        dataBookmark : 'userId',
        dataSortInfo : {
          field : 'firstName',
          direction : 'asc'
        },
        columns : [{
              name : 'resourceURI',
              mapping : 'userId',
              convert : function(value, parent) {
                return parent.source == 'default' ? (Sonatype.config.repos.urls.users + '/' + value) : (Sonatype.config.repos.urls.plexusUser + '/' + parent.source + '/' + value);
              }
            }, {
              name : 'userId',
              sortType : Ext.data.SortTypes.asUCString,
              header : 'User ID',
              width : 100
            }, {
              name : 'source',
              header : 'Realm',
              width : 50
            }, {
              name : 'firstName',
              sortType : Ext.data.SortTypes.asUCString,
              header : 'First Name',
              width : 175
            }, {
              name : 'lastName',
              sortType : Ext.data.SortTypes.asUCString,
              header : 'Last Name',
              width : 175
            }, {
              name : 'email',
              header : 'Email',
              width : 175
            }, {
              name : 'roles'
            }, {
              name : 'displayRoles',
              mapping : 'roles',
              convert : this.combineRoles.createDelegate(this),
              header : 'Roles',
              autoExpand : true
            }],
        listeners : {
          beforedestroy : {
            fn : function() {
              Sonatype.Events.un('userAddMenuInit', this.onAddMenuInit, this);
              Sonatype.Events.un('userMenuInit', this.onUserMenuInit, this);
            },
            scope : this
          }
        },
        tbar : [' ', this.displaySelector, this.searchField]
      });

  this.titleColumn = 'userId';

};

Ext.extend(Sonatype.repoServer.UserEditPanel, Sonatype.panels.GridViewer, {
      combineRoles : function(val, parent) {
        var s = '';
        if (val)
        {
          for (var i = 0; i < val.length; i++)
          {
            var roleName = val[i].name;
            if (s)
            {
              s += ', ';
            }
            s += roleName;
          }
        }

        return s;
      },

      changePasswordHandler : function(rec) {
        var userId = rec.get('userId');

        var w = new Ext.Window({
              id : 'set-password-window',
              title : 'Set Password',
              closable : true,
              autoWidth : false,
              width : 350,
              autoHeight : true,
              modal : true,
              constrain : true,
              resizable : false,
              draggable : false,
              items : [{
                    xtype : 'form',
                    labelAlign : 'right',
                    labelWidth : 110,
                    frame : true,
                    defaultType : 'textfield',
                    monitorValid : true,
                    items : [{
                          xtype : 'panel',
                          style : 'padding-left: 70px; padding-bottom: 10px',
                          html : 'Enter a new password for user ' + userId
                        }, {
                          fieldLabel : 'New Password',
                          inputType : 'password',
                          name : 'newPassword',
                          width : 200,
                          allowBlank : false
                        }, {
                          fieldLabel : 'Confirm Password',
                          inputType : 'password',
                          name : 'confirmPassword',
                          width : 200,
                          allowBlank : false,
                          validator : function(s) {
                            var firstField = this.ownerCt.find('name', 'newPassword')[0];
                            if (firstField && firstField.getRawValue() != s)
                            {
                              return "Passwords don't match";
                            }
                            return true;
                          }
                        }],
                    buttons : [{
                          text : 'Set Password',
                          formBind : true,
                          scope : this,
                          handler : function() {
                            var newPassword = w.find('name', 'newPassword')[0].getValue();

                            Ext.Ajax.request({
                                  scope : this,
                                  method : 'POST',
                                  cbPassThru : {
                                    userId : userId,
                                    newPassword : newPassword
                                  },
                                  jsonData : {
                                    data : {
                                      userId : userId,
                                      newPassword : newPassword
                                    }
                                  },
                                  url : Sonatype.config.repos.urls.usersSetPassword,
                                  success : function(response, options) {
                                    w.close();
                                    Sonatype.MessageBox.show({
                                          id : 'password-changed-messagebox',
                                          title : 'Password Changed',
                                          msg : 'Password change request completed successfully.',
                                          buttons : Sonatype.MessageBox.OK,
                                          icon : Sonatype.MessageBox.INFO,
                                          animEl : 'mb3'
                                        });
                                  },
                                  failure : function(response, options) {
                                    Sonatype.utils.connectionError(response, 'There is a problem changing the password.')
                                  }
                                });
                          }
                        }, {
                          text : 'Cancel',
                          formBind : false,
                          scope : this,
                          handler : function() {
                            w.close();
                          }
                        }]
                  }],
              listeners : {
                show : function(c) {
                  var field = c.find('name', 'newPassword')[0].focus(false, 100);
                }
              }
            });

        w.show();
      },

      refreshHandler : function(button, e) {
        this.clearCards();
        if (this.lastUrl)
        {
          this.searchByUrl();
        }
        else
        {
          this.dataStore.reload();
        }
      },

      resetPasswordHandler : function(rec) {
        if (rec.data.resourceURI != 'new')
        {
          Sonatype.utils.defaultToNo();

          Sonatype.MessageBox.show({
                animEl : this.gridPanel.getEl(),
                title : 'Reset user password?',
                msg : 'Reset the ' + rec.get('userId') + ' user password?',
                buttons : Sonatype.MessageBox.YESNO,
                scope : this,
                icon : Sonatype.MessageBox.QUESTION,
                fn : function(btnName) {
                  if (btnName == 'yes' || btnName == 'ok')
                  {
                    Ext.Ajax.request({
                          callback : this.resetPasswordCallback,
                          cbPassThru : {
                            resourceId : rec.id
                          },
                          scope : this,
                          method : 'DELETE',
                          url : Sonatype.config.repos.urls.usersReset + '/' + rec.data.userId
                        });
                  }
                }
              });
        }
      },

      resetPasswordCallback : function(options, isSuccess, response) {
        if (isSuccess)
        {
          Sonatype.MessageBox.alert('Password Reset', 'The password has been reset.');
        }
        else
        {
          Sonatype.utils.connectionError(response, 'The server did not reset the password.');
        }
      },

      searchByUrl : function() {
        this.clearAll();
        if (this.warningLabel)
        {
          this.warningLabel.destroy();
          this.warningLabel = null;
        }
        this.gridPanel.loadMask.show();
        Ext.Ajax.request({
              scope : this,
              url : this.lastUrl,
              callback : function(options, success, response) {
                this.gridPanel.loadMask.hide();
                if (success)
                {
                  var r = Ext.decode(response.responseText);
                  if (r.data)
                  {
                    this.dataStore.loadData(r);
                  }
                }
              }
            });
      },

      showUsers : function(button, e) {
        this.displaySelector.setText(button.text);
        this.displaySelector.value = button.value;
        this.clearAll();

        if (button.value == 'allConfigured')
        {
          this.searchField.emptyText = 'Select a filter to enable username search';
          this.searchField.setValue('');
          this.searchField.disable();
          this.calculateSearchUrl(this);
          this.searchByUrl();
        }
        else
        {
          this.searchField.emptyText = 'Enter a username or leave blank to display all';
          this.searchField.setValue('');
          this.searchField.enable();
          this.calculateSearchUrl(this);
          if (!this.warningLabel)
          {
            this.warningLabel = this.getTopToolbar().addText('<span class="x-toolbar-warning">Click "Search" to refresh the view</span>');
          }
        }
      },

      startSearch : function(panel) {
        panel.calculateSearchUrl(panel);
        panel.searchByUrl();
      },

      calculateSearchUrl : function(panel) {
        var url = Sonatype.config.repos.urls.searchUsers;
        var prefix = '/' + panel.displaySelector.value;
        var suffix = '';

        // this is somewhat ugly, since effectiveUsers is a flag, not a locator
        if (panel.displaySelector.value == 'effectiveUsers')
        {
          prefix = '/all';
          suffix = '?effectiveUsers=true';
        }

        var v = '';
        if (panel.searchField.disabled)
        {
          url = Sonatype.config.repos.urls.plexusUsers;
        }
        else
        {
          v = panel.searchField.getValue();
        }

        if (v.length > 0)
        {
          panel.lastUrl = url + prefix + '/' + v + suffix;
        }
        else
        {
          panel.lastUrl = url + prefix + suffix;
        }
      },

      stopSearch : function(panel) {
        panel.searchField.setValue(null);
        panel.searchField.triggers[0].hide();
      },

      onAddMenuInit : function(menu) {
        menu.add('-');
        if (this.sp.checkPermission('security:users', this.sp.CREATE))
        {
          menu.add({
                text : 'Nexus User',
                autoCreateNewRecord : true,
                handler : function(container, rec, item, e) {
                  rec.beginEdit();
                  rec.set('source', 'default');
                  rec.commit();
                  rec.endEdit();
                },
                scope : this
              });
        }
        menu.add({
              text : 'External User Role Mapping',
              handler : this.mapRolesHandler,
              scope : this
            });
      },

      onUserMenuInit : function(menu, userRecord) {
        if (userRecord.data.source == 'default' // &&
        // userRecord.data.userManaged
        // == true
        )
        {

          if (userRecord.data.resourceURI.substring(0, 4) != 'new_')
          {
            if (this.sp.checkPermission('security:usersreset', this.sp.DELETE))
            {
              menu.add(this.actions.resetPasswordAction);
            }

            if (this.sp.checkPermission('security:users', this.sp.EDIT))
            {
              menu.add(this.actions.changePasswordAction);
            }
          }
        }
      },

      mapRolesHandler : function(button, e) {
        this.createChildPanel({
              id : 'new_mapping',
              hostPanel : this,
              data : {
                name : 'User Role Mapping'
              }
            }, true);
      },

      deleteRecord : function(rec) {
        if (rec.data.source == 'default')
        {
          return Sonatype.repoServer.UserEditPanel.superclass.deleteRecord.call(this, rec);
        }
        else
        {
          Ext.Ajax.request({
                callback : function(options, success, response) {
                  if (success)
                  {
                    this.dataStore.remove(rec);
                  }
                  else
                  {
                    Sonatype.utils.connectionError(response, 'Delete Failed!');
                  }
                },
                scope : this,
                suppressStatus : 404,
                method : 'DELETE',
                url : Sonatype.config.repos.urls.userToRoles + '/' + rec.data.source + '/' + rec.data.userId
              });
        }
      },

      deleteActionHandler : function(button, e) {
        if (this.gridPanel.getSelectionModel().hasSelection())
        {
          var rec = this.gridPanel.getSelectionModel().getSelected();
          if (rec.data.source == 'default')
          {
            return Sonatype.repoServer.UserEditPanel.superclass.deleteActionHandler.call(this, button, e);
          }
          else
          {
            var roles = rec.data.roles;
            if (roles)
              for (var i = 0; i < roles.length; i++)
              {
                if (roles[i].source == 'default')
                {
                  Sonatype.utils.defaultToNo();

                  Sonatype.MessageBox.show({
                        animEl : this.gridPanel.getEl(),
                        title : 'Delete',
                        msg : 'Delete Nexus role mapping for ' + rec.data[this.titleColumn] + '?',
                        buttons : Sonatype.MessageBox.YESNO,
                        scope : this,
                        icon : Sonatype.MessageBox.QUESTION,
                        fn : function(btnName) {
                          if (btnName == 'yes' || btnName == 'ok')
                          {
                            this.deleteRecord(rec);
                          }
                        }
                      });
                  return;
                }
              }
            Sonatype.MessageBox.show({
                  animEl : this.gridPanel.getEl(),
                  title : 'Delete',
                  msg : 'This user does not have any Nexus roles mapped.',
                  buttons : Sonatype.MessageBox.OK,
                  scope : this,
                  icon : Sonatype.MessageBox.WARNING
                });
          }
        }
      },

      loadSources : function(store, records, options) {
        var menu = Ext.menu.MenuMgr.get('user-realm-selector-menu');

        for (var i = 0; i < records.length; i++)
        {
          var rec = records[i];
          var v = rec.data.roleHint;
          if (v != 'mappedExternal')
          {
            menu.addMenuItem({
                  text : v == 'default' ? 'Default Realm Users' : rec.data.description,
                  value : v,
                  checked : v == 'allConfigured',
                  handler : this.showUsers,
                  group : 'user-realm-selector',
                  scope : this
                });
          }
        }
      }
    });

Sonatype.repoServer.DefaultUserEditor = function(config) {
  var config = config || {};
  var defaultConfig = {
    uri : Sonatype.config.repos.urls.users,
    labelWidth : 100,
    dataModifiers : {
      load : {
        roles : function(arr, srcObj, fpanel) {
          fpanel.find('name', 'roleManager')[0].setSelectedRoleIds(arr, true);
          return arr;
        }
      },
      submit : {
        roles : function(value, fpanel) {
          return fpanel.find('name', 'roleManager')[0].getSelectedRoleIds();
        },
        email : function(value, fpanel) {
          return Ext.util.Format.trim(value);
        }
      }
    }
  };
  Ext.apply(this, config, defaultConfig);

  // List of user statuses
  this.statusStore = new Ext.data.SimpleStore({
        fields : ['value', 'display'],
        data : [['active', 'Active'], ['disabled', 'Disabled']]
      });

  var ht = Sonatype.repoServer.resources.help.users;

  this.COMBO_WIDTH = 300;

  this.checkPayload();

  var items = [{
        xtype : 'textfield',
        fieldLabel : 'User ID',
        itemCls : 'required-field',
        labelStyle : 'margin-left: 15px; width: 185px;',
        helpText : ht.userId,
        name : 'userId',
        disabled : !this.isNew,
        allowBlank : false,
        width : this.COMBO_WIDTH,
        validator : Sonatype.utils.validateId
      }, {
        xtype : 'textfield',
        fieldLabel : 'First Name',
        labelStyle : 'margin-left: 15px; width: 185px;',
        helpText : ht.firstName,
        name : 'firstName',
        allowBlank : true,
        htmlDecode : true,
        width : this.COMBO_WIDTH,
        validator : function(v) {
          if (v && v.length != 0 && v.match(WHITE_SPACE_REGEX))
          {
            return true;
          }
          else
          {
            return 'First Name cannot start with whitespace.';
          }
        }
      }, {
        xtype : 'textfield',
        fieldLabel : 'Last Name',
        labelStyle : 'margin-left: 15px; width: 185px;',
        helpText : ht.lastName,
        name : 'lastName',
        allowBlank : true,
        htmlDecode : true,
        width : this.COMBO_WIDTH,
        validator : function(v) {
          if (v && v.length != 0 && v.match(WHITE_SPACE_REGEX))
          {
            return true;
          }
          else
          {
            return 'Last Name cannot start with whitespace.';
          }
        }
      }, {
        xtype : 'textfield',
        fieldLabel : 'Email',
        itemCls : 'required-field',
        labelStyle : 'margin-left: 15px; width: 185px;',
        helpText : ht.email,
        name : 'email',
        allowBlank : false,
        width : this.COMBO_WIDTH
      }, {
        xtype : 'combo',
        fieldLabel : 'Status',
        labelStyle : 'margin-left: 15px; width: 185px;',
        itemCls : 'required-field',
        helpText : ht.status,
        name : 'status',
        store : this.statusStore,
        displayField : 'display',
        valueField : 'value',
        editable : false,
        forceSelection : true,
        mode : 'local',
        triggerAction : 'all',
        emptyText : 'Select...',
        selectOnFocus : true,
        allowBlank : false,
        width : this.COMBO_WIDTH
      }];

  if (this.isNew)
  {
    items.push({
          xtype : 'textfield',
          fieldLabel : 'New Password (optional)',
          inputType : 'password',
          labelStyle : 'margin-left: 15px; width: 185px;',
          helpText : ht.password,
          name : 'password',
          allowBlank : true,
          width : this.COMBO_WIDTH
        });
    items.push({
          xtype : 'textfield',
          fieldLabel : 'Confirm Password',
          inputType : 'password',
          labelStyle : 'margin-left: 15px; width: 185px;',
          helpText : ht.reenterPassword,
          name : 'confirmPassword',
          allowBlank : true,
          width : this.COMBO_WIDTH,
          validator : function(s) {
            var firstField = this.ownerCt.find('name', 'password')[0];
            if (firstField && firstField.getRawValue() != s)
            {
              return "Passwords don't match";
            }
            return true;
          }
        });
  }

  items.push({
        id : 'useredit-rolemanager',
        xtype : 'rolemanager',
        name : 'roleManager',
        height : 200,
        width : 490,
        usePrivileges : false,
        style : 'margin-left: 15px;margin-top: 10px;border: 1px solid #B5B8C8;'
      });

  Sonatype.repoServer.DefaultUserEditor.superclass.constructor.call(this, {
        items : items,
        listeners : {
          submit : {
            fn : this.submitHandler,
            scope : this
          }
        }
      });
};

Ext.extend(Sonatype.repoServer.DefaultUserEditor, Sonatype.ext.FormPanel, {
      combineRoles : function(val) {
        var s = '';
        if (val)
        {
          for (var i = 0; i < val.length; i++)
          {
            if (s)
            {
              s += ', ';
            }
            s += this.find('name', 'roleManager')[0].getRoleNameFromId(val[i]);
          }
        }

        return s;
      },

      isValid : function() {
        return this.form.isValid() && this.find('name', 'roleManager')[0].validate();
      },

      saveHandler : function(button, event) {
        var password = this.form.getValues().password;
        this.referenceData = (this.isNew && password) ? Sonatype.repoServer.referenceData.userNew : Sonatype.repoServer.referenceData.users;

        return Sonatype.repoServer.DefaultUserEditor.superclass.saveHandler.call(this, button, event);
      },

      submitHandler : function(form, action, receivedData) {
        if (this.isNew)
        {
          receivedData.source = 'default';
          receivedData.displayRoles = this.combineRoles(receivedData.roles);
          return;
        }

        var rec = this.payload;
        rec.beginEdit();
        rec.set('firstName', receivedData.firstName);
        rec.set('lastName', receivedData.lastName);
        rec.set('email', receivedData.email);
        rec.set('displayRoles', this.combineRoles(receivedData.roles));
        rec.commit();
        rec.endEdit();
      },
      validationModifiers : { 'roles' :
        function(error,panel) {
          Ext.getCmp('useredit-rolemanager').markInvalid(error.msg); 
        }
      }
    });

Sonatype.repoServer.UserMappingEditor = function(config) {
  var config = config || {};
  var defaultConfig = {
    uri : Sonatype.config.repos.urls.plexusUser,
    dataModifiers : {
      load : {
        roles : function(arr, srcObj, fpanel) {
          var roleManager = fpanel.find('name', 'roleManager')[0];
          if (this.lastLoadedId)
          {
            roleManager.setUserId(this.lastLoadedId);
          }
          else if (fpanel.payload && fpanel.payload.get)
          {
            roleManager.setUserId(fpanel.payload.get('userId'));
          }
          roleManager.setSelectedRoleIds(arr, true);
          return arr;
        }
      },
      submit : {
        roles : function(value, fpanel) {
          return fpanel.find('name', 'roleManager')[0].getSelectedRoleIds();
        }
      }
    },
    referenceData : {
      userId : '',
      source : '',
      roles : []
    }
  };
  Ext.apply(this, config, defaultConfig);

  var ht = Sonatype.repoServer.resources.help.users;

  this.COMBO_WIDTH = 300;

  var useridField = this.payload.id == 'new_mapping' ? {
    xtype : 'trigger',
    triggerClass : 'x-form-search-trigger',
    fieldLabel : 'Enter a User ID',
    itemCls : 'required-field',
    labelStyle : 'margin-left: 15px; width: 185px;',
    name : 'userId',
    allowBlank : false,
    width : this.COMBO_WIDTH,
    listeners : {
      specialkey : {
        fn : function(f, e) {
          if (e.getKey() == e.ENTER)
          {
            this.loadUserId.createDelegate(this);
          }
        }
      }
    },
    onTriggerClick : this.loadUserId.createDelegate(this),
    listeners : {
      change : {
        fn : function(control, newValue, oldValue) {
          if (newValue != this.lastLoadedId)
          {
            this.loadUserId();
          }
        },
        scope : this
      }
    }
  } : {
    xtype : 'textfield',
    fieldLabel : 'User ID',
    itemCls : 'required-field',
    labelStyle : 'margin-left: 15px; width: 185px;',
    name : 'userId',
    disabled : true,
    allowBlank : false,
    width : this.COMBO_WIDTH,
    userFound : true
  };

  Sonatype.repoServer.UserMappingEditor.superclass.constructor.call(this, {
        items : [{
              xtype : 'panel',
              layout : 'form',
              width : 600,
              items : [useridField, {
                    xtype : 'textfield',
                    fieldLabel : 'Realm',
                    itemCls : 'required-field',
                    labelStyle : 'margin-left: 15px; width: 185px;',
                    name : 'source',
                    disabled : true,
                    allowBlank : false,
                    width : this.COMBO_WIDTH
                  }, {
                    xtype : 'textfield',
                    fieldLabel : 'First Name',
                    itemCls : 'required-field',
                    labelStyle : 'margin-left: 15px; width: 185px;',
                    name : 'firstName',
                    disabled : true,
                    allowBlank : false,
                    width : this.COMBO_WIDTH
                  }, {
                    xtype : 'textfield',
                    fieldLabel : 'Last Name',
                    itemCls : 'required-field',
                    labelStyle : 'margin-left: 15px; width: 185px;',
                    name : 'lastName',
                    disabled : true,
                    allowBlank : false,
                    width : this.COMBO_WIDTH
                  }, {
                    xtype : 'textfield',
                    fieldLabel : 'Email',
                    itemCls : 'required-field',
                    labelStyle : 'margin-left: 15px; width: 185px;',
                    name : 'email',
                    disabled : true,
                    allowBlank : false,
                    width : this.COMBO_WIDTH
                  }, {
                    id : "usermapping-rolemanager",
                    xtype : 'rolemanager',
                    name : 'roleManager',
                    height : 200,
                    width : 505,
                    usePrivileges : false
                  }]
            }],
        listeners : {
          load : this.loadHandler,
          submit : this.submitHandler,
          scope : this
        }
      });
};

Ext.extend(Sonatype.repoServer.UserMappingEditor, Sonatype.ext.FormPanel, {
      saveHandler : function(button, event) {
        if (this.isValid())
        {
          var method = 'PUT';
          var roleManager = this.find('name', 'roleManager')[0];
          var roles = roleManager.getSelectedRoleIds();
          if (roles.length == 0)
          {
            if (roleManager.noRolesOnStart)
            {
              // if there weren't any nexus roles on load, and we're not saving
              // any - do nothing
              return;
            }
            else
            {
              method = 'DELETE';
            }
          }

          var url = Sonatype.config.repos.urls.userToRoles + '/' + this.form.findField('source').getValue() + '/' + this.form.findField('userId').getValue();

          this.form.doAction('sonatypeSubmit', {
            method : method,
            url : url,
            waitMsg : 'Updating records...',
            fpanel : this,
            dataModifiers : this.dataModifiers.submit,
            serviceDataObj : this.referenceData,
            validationModifiers : this.validationModifiers,
            isNew : this.isNew
              // extra option to send to callback, instead of conditioning on
              // method
            });
        }
      },

      // update roles if the user record with the same id is displayed in the
      // grid
      // (auto-update doesn't work since the mapping resource does not return
      // anything)
      submitHandler : function(form, action, receivedData) {
        var store;
        if (this.payload.id == 'new_mapping' && this.payload.hostPanel)
        {
          store = this.payload.hostPanel.dataStore;
        }
        else if (this.payload.store)
        {
          store = this.payload.store;
        }

        if (store)
        {
          var s = '';
          var roles = [];
          var sentRoles = action.output.data.roles;
          for (var i = 0; i < sentRoles.length; i++)
          {
            var roleName = sentRoles[i];
            var roleManager = this.find('name', 'roleManager')[0];
            var newRole = {
              id : sentRoles[i],
              name : roleManager.getRoleNameFromId(sentRoles[i]),
              source : 'default'
            }
            roles.push(newRole);

            if (s)
            {
              s += ', ';
            }
            s += newRole.name;
          }

          var rec = store.getById(action.output.data.userId);
          if (rec)
          {
            rec.beginEdit();
            rec.set('roles', roles);
            rec.set('displayRoles', s);
            rec.commit();
            rec.endEdit();
          }
          else if (this.payload.hostPanel && this.loadedUserData)
          {
            var resourceURI = Sonatype.config.host + Sonatype.config.repos.urls.plexusUser + '/' + this.loadedUserData.userId;
            var rec = new store.reader.recordType({
                  //                  name : this.loadedUserData.name,
                  email : this.loadedUserData.email,
                  source : this.loadedUserData.source,
                  userId : this.loadedUserData.userId,
                  resourceURI : resourceURI,
                  roles : roles,
                  displayRoles : s
                }, resourceURI);
            rec.autoCreateNewRecord = true;
            store.addSorted(rec);
          }
        }
      },

      isValid : function() {
        return this.form.findField('userId').userFound && this.form.findField('source').getValue() && this.find('name', 'roleManager')[0].validate();
      },

      loadHandler : function(form, action, receivedData) {
        this.loadedUserData = receivedData;
      },

      loadUserId : function() {
        var testField = this.form.findField('userId');
        testField.clearInvalid();
        testField.userFound = true;
        this.lastLoadedId = testField.getValue();
        var roleManager = this.find('name', 'roleManager')[0];
        roleManager.setUserId(testField.getValue());

        this.form.doAction('sonatypeLoad', {
              url : this.uri + '/' + testField.getValue(),
              method : 'GET',
              fpanel : this,
              testField : testField,
              suppressStatus : 404,
              dataModifiers : this.dataModifiers.load,
              scope : this
            });
      },

      actionFailedHandler : function(form, action) {
        if (action.response.status == 404)
        {
          if (action.options.testField)
          {
            action.options.testField.markInvalid('User record not found.');
            action.options.testField.userFound = false;
          }
          else
          {
            var s;
            if (this.payload && this.payload.data)
            {
              s = this.payload.data.source;
            }
            Sonatype.MessageBox.show({
                  title : 'Error',
                  msg : 'Unable to retrieve user details.' + '<br/><br/>' + 'Please make sure the ' + (s ? s : 'selected') + ' realm is enabled<br/>on the server administration panel.',
                  buttons : Sonatype.MessageBox.OK,
                  icon : Sonatype.MessageBox.ERROR,
                  animEl : 'mb3'
                });
          }
        }
        else
        {
          return Sonatype.repoServer.UserMappingEditor.superclass.actionFailedHandler.call(this, form, action);
        }
      },
      validationModifiers : { 'roles' :
        function(error,panel) {
          Ext.getCmp('usermapping-rolemanager').markInvalid(error.msg); 
        }
      }
    });

Sonatype.Events.addListener('userViewInit', function(cardPanel, rec) {
      var config = {
        payload : rec,
        tabTitle : 'Config'
      }
      cardPanel.add(rec.data.source == 'default' ? new Sonatype.repoServer.DefaultUserEditor(config) : new Sonatype.repoServer.UserMappingEditor(config));
    });

(function(){
  var views = [];
  var viewsCollected = false;

  // NXCM-4099 plugin-contributed tabs
  Sonatype.Events.addListener('userViewInit', function(cardPanel, rec) {
    if (!viewsCollected) {
      Sonatype.Events.fireEvent('userAdminViewInit', views);
      viewsCollected = true;
    }

    for( var i = 0; i < views.length; i++ ) {
      var view = views[i];

      // don't add views for 'new user':
      // if you add a Nexus user, you get a record without username
      // if you add a role mapping, you get a js object (without even #get(string)
      var username = undefined;

      if ( rec.get ) {
        username = rec.get("userId");
      }

      if (username === undefined) {
        return
      }

      var content = new view.item({username:username});
      content.tabTitle = view.name;
      if ( !content.shouldShow || content.shouldShow() ) {
        cardPanel.add(content);
      }
    }
  });
})();

});

