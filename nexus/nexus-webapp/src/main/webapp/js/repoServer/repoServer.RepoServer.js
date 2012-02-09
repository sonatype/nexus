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
(function() {

  // Repository main Controller(conglomerate) Singleton
  Sonatype.repoServer.RepoServer = function() {
    var cfg = Sonatype.config.repos;
    var sp = Sonatype.lib.Permissions;

    // ************************************
    return {
      pomDepTmpl : new Ext.XTemplate('<dependency><groupId>{groupId}</groupId><artifactId>{artifactId}</artifactId><version>{version}</version></dependency>'),

      loginFormConfig : {
        labelAlign : 'right',
        labelWidth : 60,
        frame : true,

        defaultType : 'textfield',
        monitorValid : true,

        items : [{
              id : 'usernamefield',
              fieldLabel : 'Username',
              name : 'username',
              tabIndex : 1,
              width : 200,
              allowBlank : false
            }, {
              id : 'passwordfield',
              fieldLabel : 'Password',
              name : 'password',
              tabIndex : 2,
              inputType : 'password',
              width : 200,
              allowBlank : false
            }]
        // buttons added later to provide scope to handler

      },

      buildRecoveryText : function() {
        var htmlString = null;

        if (sp.checkPermission('security:usersforgotid', sp.CREATE))
        {
          htmlString = 'Forgot your <a id="recover-username" href="#">username</a>'
        }
        if (sp.checkPermission('security:usersforgotpw', sp.CREATE))
        {
          if (htmlString != null)
          {
            htmlString += ' or ';
          }
          else
          {
            htmlString = 'Forgot your ';
          }
          htmlString += '<a id="recover-password" href="#">password</a>';
        }
        if (htmlString != null)
        {
          htmlString += '?';
        }

        return htmlString;
      },

      statusComplete : function(statusResponse) {
        this.resetMainTabPanel();

        this.createSubComponents(); // update left panel

        var htmlString = this.buildRecoveryText();

        var recoveryPanel = this.loginForm.findById('recovery-panel');
        if (recoveryPanel)
        {
          this.loginForm.remove(recoveryPanel);
          recoveryPanel.destroy();
        }
        this.loginForm.add({
              xtype : 'panel',
              id : 'recovery-panel',
              style : 'padding-left: 70px',
              html : htmlString
            });
      },

      // Each Sonatype server will need one of these
      initServerTab : function() {

        Sonatype.Events.addListener('nexusNavigationInit', this.addNexusNavigationItems, this);

        Sonatype.Events.addListener('nexusStatus', this.nexusStatusEvent, this);

        // Left Panel
        this.nexusPanel = new Sonatype.navigation.NavigationPanel({
              id : 'st-nexus-tab',
              title : 'Nexus'
            });

        this.createSubComponents();

        Sonatype.view.serverTabPanel.add(this.nexusPanel);

        var htmlString = this.buildRecoveryText();

        if (htmlString != null)
        {
          this.loginFormConfig.items[2] = {
            xtype : 'panel',
            id : 'recovery-panel',
            style : 'padding-left: 70px',
            html : htmlString
          };
        }

        this.loginFormConfig.buttons = [{
              id : 'loginbutton',
              text : 'Log In',
              tabIndex : 3,
              formBind : true,
              scope : this,
              handler : function() {
                var usernameField = this.loginForm.find('name', 'username')[0];
                var passwordField = this.loginForm.find('name', 'password')[0];

                if (usernameField.isValid() && passwordField.isValid())
                {
                  Sonatype.utils.doLogin(this.loginWindow, usernameField.getValue(), passwordField.getValue());
                }
              }
            }];

        this.loginFormConfig.keys = {
          key : Ext.EventObject.ENTER,
          fn : this.loginFormConfig.buttons[0].handler,
          scope : this
        };

        this.loginForm = new Ext.form.FormPanel(this.loginFormConfig);
        this.loginWindow = new Ext.Window({
              id : 'login-window',
              title : 'Nexus Log In',
              animateTarget : 'head-link-r',
              closable : true,
              closeAction : 'hide',
              autoWidth : false,
              width : 300,
              autoHeight : true,
              modal : true,
              constrain : true,
              resizable : false,
              draggable : false,
              items : [this.loginForm]
            });

        this.loginWindow.on('show', function() {
              var panel = this.loginWindow.findById('recovery-panel');
              if (panel && !panel.clickListenerAdded)
              {
                // these listeners only work if added after the window is
                // created
                panel.body.on('click', Ext.emptyFn, null, {
                      delegate : 'a',
                      preventDefault : true
                    });
                panel.body.on('mousedown', this.recoverLogin, this, {
                      delegate : 'a'
                    });
                panel.clickListenerAdded = true;
              }

              var field = this.loginForm.find('name', 'username')[0];
              if (field.getRawValue())
              {
                field = this.loginForm.find('name', 'password')[0]
              }
              field.focus(true, 100);
            }, this);

        this.loginWindow.on('close', function() {
              this.loginForm.getForm().reset();
            }, this);

        this.loginWindow.on('hide', function() {
              this.loginForm.getForm().reset();
              Sonatype.view.afterLoginToken = null;
            }, this);
      },

      // Add/Replace Nexus left hand components
      createSubComponents : function() {
        var wasRendered = this.nexusPanel.rendered;

        if (wasRendered)
        {
          this.nexusPanel.getEl().mask('Updating...', 'loading-indicator');
          this.nexusPanel.items.each(function(item, i, len) {
                this.remove(item, true);
              }, this.nexusPanel);
        }

        Sonatype.Events.fireEvent('nexusNavigationInit', this.nexusPanel);

        // fire second event so plugins can contribute navigation items, and set the order
        Sonatype.Events.fireEvent('nexusNavigationPostInit', this.nexusPanel);

        if (wasRendered)
        {
          this.nexusPanel.doLayout();
          this.nexusPanel.getEl().unmask();
          // this.nexusPanel.enable();
        }

        Sonatype.Events.fireEvent('nexusNavigationInitComplete', this.nexusPanel);

      },

      nexusStatusEvent : function() {

        // check the user status, if it is not set, then reset the panels
        if (!Sonatype.user.curr.repoServer)
        {
          this.resetMainTabPanel();
        }
      },

      addNexusNavigationItems : function(nexusPanel) {

        // Views Group **************************************************
        nexusPanel.add({
              title : 'Views/Repositories',
              id : 'st-nexus-views',
              items : [{
                    enabled : sp.checkPermission('nexus:repostatus', sp.READ),
                    title : 'Repositories',
                    tabId : 'view-repositories',
                    tabCode : Sonatype.repoServer.RepositoryPanel,
                    tabTitle : 'Repositories'
                  }, {
                    enabled : sp.checkPermission('nexus:targets', sp.READ) && (sp.checkPermission('nexus:targets', sp.CREATE) || sp.checkPermission('nexus:targets', sp.DELETE) || sp.checkPermission('nexus:targets', sp.EDIT)),
                    title : 'Repository Targets',
                    tabId : 'targets-config',
                    tabCode : Sonatype.repoServer.RepoTargetEditPanel
                  }, {
                    enabled : sp.checkPermission('nexus:routes', sp.READ) && (sp.checkPermission('nexus:routes', sp.CREATE) || sp.checkPermission('nexus:routes', sp.DELETE) || sp.checkPermission('nexus:routes', sp.EDIT)),
                    title : 'Routing',
                    tabId : 'routes-config',
                    tabCode : Sonatype.repoServer.RoutesEditPanel
                  }, {
                    enabled : sp.checkPermission('nexus:feeds', sp.READ),
                    title : 'System Feeds',
                    tabId : 'feed-view-system-changes',
                    tabCode : Sonatype.repoServer.FeedViewPanel
                  }]
            });

        // Config Group **************************************************
        nexusPanel.add({
              title : 'Enterprise',
              id : 'st-nexus-enterprise'
            });

        // Security Group **************************************************
        nexusPanel.add({
              title : 'Security',
              id : 'st-nexus-security',
              collapsed : true,
              items : [{
                    enabled : Sonatype.user.curr.isLoggedIn && Sonatype.user.curr.loggedInUserSource == 'default' && sp.checkPermission('security:userschangepw', sp.CREATE),
                    title : 'Change Password',
                    handler : Sonatype.utils.changePassword,
                    tabId : 'change-password'
                  }, {
                    enabled : sp.checkPermission('security:users', sp.READ) && (sp.checkPermission('security:users', sp.CREATE) || sp.checkPermission('security:users', sp.DELETE) || sp.checkPermission('security:users', sp.EDIT)),
                    title : 'Users',
                    tabId : 'security-users',
                    tabCode : Sonatype.repoServer.UserEditPanel
                  }, {
                    enabled : sp.checkPermission('security:roles', sp.READ) && (sp.checkPermission('security:roles', sp.CREATE) || sp.checkPermission('security:roles', sp.DELETE) || sp.checkPermission('security:roles', sp.EDIT)),
                    title : 'Roles',
                    tabId : 'security-roles',
                    tabCode : Sonatype.repoServer.RoleEditPanel
                  }, {
                    enabled : sp.checkPermission('security:privileges', sp.READ) && (sp.checkPermission('security:privileges', sp.CREATE) || sp.checkPermission('security:privileges', sp.DELETE) || sp.checkPermission('security:privileges', sp.EDIT)),
                    title : 'Privileges',
                    tabId : 'security-privileges',
                    tabCode : Sonatype.repoServer.PrivilegeEditPanel
                  }]
            });

        // Config Group **************************************************
        nexusPanel.add({
              title : 'Administration',
              id : 'st-nexus-config',
              collapsed : true,
              items : [{
                    enabled : sp.checkPermission('nexus:settings', sp.READ) && (sp.checkPermission('nexus:settings', sp.CREATE) || sp.checkPermission('nexus:settings', sp.DELETE) || sp.checkPermission('nexus:settings', sp.EDIT)),
                    title : 'Server',
                    tabId : 'nexus-config',
                    tabCode : Sonatype.repoServer.ServerEditPanel,
                    tabTitle : 'Nexus'
                  }, {
                    enabled : sp.checkPermission('nexus:tasks', sp.READ) && (sp.checkPermission('nexus:tasks', sp.CREATE) || sp.checkPermission('nexus:tasks', sp.DELETE) || sp.checkPermission('nexus:tasks', sp.EDIT)),
                    title : 'Scheduled Tasks',
                    tabId : 'schedules-config',
                    tabCode : Sonatype.repoServer.SchedulesEditPanel
                  }, {
                    enabled : sp.checkPermission('nexus:logs', sp.READ) || sp.checkPermission('nexus:configuration', sp.READ),
                    title : 'System Files',
                    tabId : 'view-logs',
                    tabCode : Sonatype.repoServer.LogsViewPanel,
                    tabTitle : 'System Files'
                  }, {
                    enabled : sp.checkPermission('nexus:logconfig', sp.READ) && (sp.checkPermission('nexus:logconfig', sp.CREATE) || sp.checkPermission('nexus:logconfig', sp.DELETE) || sp.checkPermission('nexus:logconfig', sp.EDIT)),
                    title : 'Log Configuration',
                    tabId : 'log-config',
                    tabCode : Sonatype.repoServer.LogEditPanel
                  }]
            });

        nexusPanel.add({
              title : 'Help',
              id : 'st-nexus-docs',
              collapsible : true,
              collapsed : true,
              items : [{
                    title : 'About Nexus',
                    tabId : 'AboutNexus',
                    tabCode : Sonatype.repoServer.HelpAboutPanel
                  }, {
                    title : 'Browse Issue Tracker',
                    href : 'http://links.sonatype.com/products/nexus/oss/issues'
                  }, {
                    title : 'Documentation',
                    tabId : 'Documentation',
                    tabCode : Sonatype.repoServer.Documentation
                  }, {
                    enabled : sp.checkPermission('nexus:settings', sp.READ),
                    title : 'Report Problem',
                    tabId : 'error-report',
                    handler : Sonatype.utils.generateErrorReportHandler
                  }]
            });
      },

      loginHandler : function() {
        if (Sonatype.user.curr.isLoggedIn)
        {
          // do logout
          Ext.Ajax.request({
                scope : this,
                method : 'GET',
                url : Sonatype.config.repos.urls.logout,
                callback : function(options, success, response) {
                  Sonatype.utils.authToken = null;
                  Sonatype.view.justLoggedOut = true;
                  Sonatype.utils.loadNexusStatus();
                  window.location = 'index.html#welcome';
                }
              });

        }
        else
        {
          this.loginForm.getForm().clearInvalid();
          var cp = Sonatype.state.CookieProvider;
          var username = cp.get('username', null);
          if (username)
          {
            this.loginForm.find('name', 'username')[0].setValue(username);
          }
          this.loginWindow.show();
        }
      },

      resetMainTabPanel : function() {
        Sonatype.view.mainTabPanel.items.each(function(item, i, len) {
              this.remove(item, true);
            }, Sonatype.view.mainTabPanel);
        Sonatype.view.mainTabPanel.activeTab = null;
        Sonatype.view.supportedNexusTabs = {};

        var welcomePanelConfig = {
          layout : 'auto',
          width : 500,
          items : []
        };
        var welcomeTabConfig = {
          title : 'Welcome',
          id : 'welcome',
          items : [{
                layout : 'column',
                border : false,
                defaults : {
                  border : false,
                  style : 'padding-top: 30px;'
                },
                items : [{
                      columnWidth : .5,
                      html : '&nbsp;'
                    }, welcomePanelConfig, {
                      columnWidth : .5,
                      html : '&nbsp;'
                    }]
              }],
          listeners : {
            render : {
              fn : function() {
                Sonatype.Events.fireEvent('welcomeTabRender');
              },
              single : true,
              delay : 300
            }
          }
        };

        var welcomeMsg = '<p style="text-align:center;"><a href="http://nexus.sonatype.org" target="new">' + '<img src="images/nexus200x50.png" border="0" alt="Welcome to the Sonatype Nexus Maven Repository Manager"></a>' + '</p>';

        var statusEnabled = sp.checkPermission('nexus:status', sp.READ);
        if (!statusEnabled)
        {
          welcomeMsg += '</br>';
          welcomeMsg += '<p style="color:red">Warning: Could not retrieve Nexus status, anonymous access might be disabled.</p>';
        }

        welcomePanelConfig.items.push({
              border : false,
              html : '<div class="little-padding">' + welcomeMsg + '</div>'
            });

        var itemCount = welcomePanelConfig.items.length;

        Sonatype.Events.fireEvent('welcomePanelInit', this, welcomePanelConfig);

        // If nothing was added, then add the default blurb, if perm'd of course
        if (welcomePanelConfig.items.length <= itemCount && sp.checkPermission('nexus:repostatus', sp.READ))
        {
          welcomePanelConfig.items.push({
                layout : 'form',
                border : false,
                frame : false,
                labelWidth : 10,
                items : [{
                      border : false,
                      html : '<div class="little-padding">' + '<br/><p>You may browse the repositories using the options on the left.</p>' + '</div>'
                    }]
              });
        }

        Sonatype.view.welcomeTab = new Ext.Panel(welcomeTabConfig);
        Sonatype.view.mainTabPanel.add(Sonatype.view.welcomeTab);
        Sonatype.view.mainTabPanel.setActiveTab(Sonatype.view.welcomeTab);
      },

      recoverLogin : function(e, target) {
        e.stopEvent();
        if (this.loginWindow.isVisible())
        {
          this.loginWindow.hide();
        }

        var action = target.id;
        if (action == 'recover-username')
        {
          Sonatype.utils.recoverUsername();
        }
        else if (action == 'recover-password')
        {
          Sonatype.utils.recoverPassword();
        }
      }

    };
  }();

})();
