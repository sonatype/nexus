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

  Sonatype.utils = {
    passwordPlaceholder : '|$|N|E|X|U|S|$|',
    edition : '',
    editionShort : '',
    version : '',
    attributionsURL : '',
    purchaseURL : '',
    userLicenseURL : '',
    authToken : '',
    lowercase : function(str) {
      if (Ext.isEmpty(str))
      {
        return str;
      }
      str = str.toString();
      return str.toLowerCase();
    },
    lowercaseFirstChar : function(str) {
      if (Ext.isEmpty(str))
      {
        return str;
      }
      str = str.toString();
      return str.charAt(0).toLowerCase() + str.slice(1);
    },
    upperFirstCharLowerRest : function(str) {
      if (Ext.isEmpty(str))
      {
        return str;
      }
      str = str.toString();
      return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
    },
    uppercase : function(str) {
      if (Ext.isEmpty(str))
      {
        return str;
      }
      str = str.toString();
      return str.toUpperCase();
    },
    capitalize : function(str) {
      if (Ext.isEmpty(str))
      {
        return str;
      }
      str = str.toString();
      return str.charAt(0).toUpperCase() + str.slice(1);
    },
    returnEmptyStr : function() {
      return '';
    },
    returnValidStr : function(str) {
      if (str != null)
      {
        return str;
      }
      else
      {
        return Sonatype.utils.returnEmptyStr();
      }
    },

    validateNoSpaces : function(value) {
      if (value && value.indexOf(' ') != -1)
      {
        return 'Spaces are not allowed in ID';
      }

      return true;
    },

    validateId : function(value) {
      var idPattern = /^[a-zA-Z0-9_\-\.]+$/;
      if (idPattern.test(value))
      {
        return true;
      }
      else
      {
        return 'Only letters, digits, underscores(_), hyphens(-), and dots(.) are allowed in ID';
      }
    },

    sortFn : function(r1, r2) {
        var v1 = Sonatype.utils.lowercase(r1), v2 = Sonatype.utils.lowercase(r2);
        return v1 > v2 ? 1 : (v1 < v2 ? -1 : 0);
    },

    convert : {
      stringContextToBool : function(str) {
        return (str.toLowerCase() === 'true');
      },
      passwordToString : function(str) {
        if (Sonatype.utils.passwordPlaceholder === str)
        {
          return null;
        }
        else if (str)
        {
          return str;
        }
        else
        {
          return Sonatype.utils.returnEmptyStr();
        }
      }
    },
    // deep copy of an object. All references independent from object passed in.
    cloneObj : function(o) {
      if (typeof(o) != 'object' || o === null)
      {
        return o;
      }

      var newObj = {};

      for (var i in o)
      {
        newObj[i] = Sonatype.utils.cloneObj(o[i]);
      }

      return newObj;
    },

    // (Array : arr, string : child, [string seperator])
    // array to join, name of element of contained object, seperator (defaults
    // to ", ")
    joinArrayObject : function(arr, child, seperator) {
      var sOut = '';
      var sep = (seperator) ? seperator : ', ';

      for (var i = 0; i < arr.length; i++)
      {
        if ((arr[i])[child])
        {
          sOut += (arr[i])[child] + sep;
        }
      }

      if (sOut != "")
      {
        return sOut.substring(0, sOut.length - sep.length);
      }
      else
      {
        return sOut;
      }
    },

    updateGlobalTimeout : function() {
      Ext.Ajax.request({
            method : 'GET',
            scope : this,
            url : Sonatype.config.repos.urls.restApiSettings,
            callback : function(options, success, response) {
              if (success)
              {
                var dec = Ext.decode(response.responseText);
                Ext.Ajax.timeout = dec.data.uiTimeout * 1000;
              }
              else
              {
                Ext.Ajax.timeout = 60000;
                Sonatype.utils.connectionError(response, 'Error retrieving rest timeout');
              }
            }
          });
    },
    connectionError : function(response, message, offerRestart, options, showResponseText) {
      var serverMessage = '';
      var r = response.responseText;
      if (r)
      {
        if (options && options.decodeErrorResponse && r.toLowerCase().indexOf('"errors"') > -1)
        {
          var errorResponse = Ext.decode(response.responseText);

          for (var i = 0; i < errorResponse.errors.length; i++)
          {
            serverMessage += '<br /><br />';
            serverMessage += Ext.decode(response.responseText).errors[i].msg;
          }
        }
        else
        {
          var n1 = r.toLowerCase().indexOf('<h3>') + 4;
          var n2 = r.toLowerCase().indexOf('</h3>');
          if (n2 > n1)
          {
            serverMessage += '<br /><br />'
            serverMessage += r.substring(n1, n2);
          }
        }
      }

      if (Sonatype.repoServer.RepoServer.loginWindow.isVisible() && (response.status == 403 || response.status == 401))
      {
        if (Sonatype.repoServer.RepoServer.loginWindow.isVisible())
        {
          var nexusReason = response.getResponseHeader['X-Nexus-Reason'];
          if (nexusReason && nexusReason.substring(0, 7) == 'expired')
          {
            Sonatype.repoServer.RepoServer.loginWindow.hide();
            Sonatype.utils.changePassword(Sonatype.repoServer.RepoServer.loginForm.find('name', 'username')[0].getValue());
          }
          else
          {
            Sonatype.MessageBox.show({
                  title : 'Login Error',
                  msg : 'Incorrect username, password or no permission to use the Nexus User Interface.<br />Try again.' + serverMessage,
                  buttons : Sonatype.MessageBox.OK,
                  icon : Sonatype.MessageBox.ERROR,
                  animEl : 'mb3',
                  fn : this.focusPassword
                });
          }
        }
      }
      else
      {
        if (message == null)
        {
          message = '';
        }
        if (serverMessage)
        {
          message += serverMessage;
        }

        var responseStatus = response.status;
        var responseStatusText = response.statusText;

        // the prompt is in the following format

        // <message> (optional)
        //
        // <response text> (if 400)
        //
        // <detail> (include status code if valid)
        //
        // <retry message> (optional)

        Sonatype.MessageBox.show({
              title : "Error",
              msg : ((message && (responseStatus != -1) ? message : '')
                  + (responseStatus == 400 && showResponseText ?
                      ('<br /><br />' + response.responseText) :
                      (options && options.hideErrorStatus ? '' :
                              ( (responseStatus && responseStatus != -1 ) ? // hide the "-1 status"
                                      '<br /><br />Nexus returned an error: ERROR ' + responseStatus + ': ' + responseStatusText :
                                      '<br /><br />There was an error communicating with the server: request timed out.')
                              )
                      )
                  + (offerRestart ?
                      '<br /><br />Click OK to reload the console or CANCEL if you wish to retry the same action in a little while.' :
                      '')
              ),
              buttons : offerRestart ? Sonatype.MessageBox.OKCANCEL : Sonatype.MessageBox.OK,
              icon : Sonatype.MessageBox.ERROR,
              animEl : 'mb3',
              fn : function(button) {
                if (offerRestart && button == "ok")
                {
                  window.location.reload();
                }
              }
            });
      }
    },
    /**
     * Call this after the error signing in dialog appears. Otherwise focus
     * doesn't get put in the password field correctly.
     */
    focusPassword : function() {
      if (Sonatype.repoServer.RepoServer.loginWindow.isVisible())
      {
        Sonatype.repoServer.RepoServer.loginForm.find('name', 'password')[0].focus(true);
      }
    },
    /**
     * Base64 encode / decode http://www.webtoolkit.info/
     */
    base64 : function() {
      // private property
      var _keyStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

      // private method for UTF-8 encoding
      var _utf8_encode = function(string) {
        string = string.replace(/\r\n/g, "\n");
        var utftext = "";

        for (var n = 0; n < string.length; n++)
        {
          var c = string.charCodeAt(n);

          if (c < 128)
          {
            utftext += String.fromCharCode(c);
          }
          else if ((c > 127) && (c < 2048))
          {
            utftext += String.fromCharCode((c >> 6) | 192);
            utftext += String.fromCharCode((c & 63) | 128);
          }
          else
          {
            utftext += String.fromCharCode((c >> 12) | 224);
            utftext += String.fromCharCode(((c >> 6) & 63) | 128);
            utftext += String.fromCharCode((c & 63) | 128);
          }
        }

        return utftext;
      };

      // private method for UTF-8 decoding
      var _utf8_decode = function(utftext) {
        var string = "";
        var i = 0;
        var c = c1 = c2 = 0;

        while (i < utftext.length)
        {
          c = utftext.charCodeAt(i);

          if (c < 128)
          {
            string += String.fromCharCode(c);
            i++;
          }
          else if ((c > 191) && (c < 224))
          {
            c2 = utftext.charCodeAt(i + 1);
            string += String.fromCharCode(((c & 31) << 6) | (c2 & 63));
            i += 2;
          }
          else
          {
            c2 = utftext.charCodeAt(i + 1);
            c3 = utftext.charCodeAt(i + 2);
            string += String.fromCharCode(((c & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63));
            i += 3;
          }
        }

        return string;
      };

      return {
        // public method for encoding
        encode : function(input) {
          var output = "";
          var chr1, chr2, chr3, enc1, enc2, enc3, enc4;
          var i = 0;

          input = _utf8_encode(input);

          while (i < input.length)
          {
            chr1 = input.charCodeAt(i++);
            chr2 = input.charCodeAt(i++);
            chr3 = input.charCodeAt(i++);

            enc1 = chr1 >> 2;
            enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
            enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
            enc4 = chr3 & 63;

            if (isNaN(chr2))
            {
              enc3 = enc4 = 64;
            }
            else if (isNaN(chr3))
            {
              enc4 = 64;
            }

            output = output + _keyStr.charAt(enc1) + _keyStr.charAt(enc2) + _keyStr.charAt(enc3) + _keyStr.charAt(enc4);
          }

          return output;
        },

        // public method for decoding
        decode : function(input) {
          var output = "";
          var chr1, chr2, chr3;
          var enc1, enc2, enc3, enc4;
          var i = 0;

          input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");

          while (i < input.length)
          {
            enc1 = _keyStr.indexOf(input.charAt(i++));
            enc2 = _keyStr.indexOf(input.charAt(i++));
            enc3 = _keyStr.indexOf(input.charAt(i++));
            enc4 = _keyStr.indexOf(input.charAt(i++));

            chr1 = (enc1 << 2) | (enc2 >> 4);
            chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            chr3 = ((enc3 & 3) << 6) | enc4;

            output = output + String.fromCharCode(chr1);

            if (enc3 != 64)
            {
              output = output + String.fromCharCode(chr2);
            }
            if (enc4 != 64)
            {
              output = output + String.fromCharCode(chr3);
            }
          }

          output = _utf8_decode(output);

          return output;
        }
      }
    }(),

    defaultToNo : function() {
      // @note: this handler selects the "No" button as the default
      // @todo: could extend Sonatype.MessageBox to take the button to select as
      // a param
      Sonatype.MessageBox.getDialog().on('show', function() {
            this.focusEl = this.buttons[2]; // ack! we're offset dependent here
            this.focus();
          }, Sonatype.MessageBox.getDialog(), {
            single : true
          });
    },

    getCookie : function(cookieName) {
      var c = document.cookie + ";";
      var re = /\s?(.*?)=(.*?);/g;
      var matches;
      while ((matches = re.exec(c)) != null)
      {
        if (matches[1] == cookieName)
        {
          return matches[2];
        }
      }
      return null;
    },

    setCookie : function(cookieName, value) {
      document.cookie = cookieName + "=" + value + "; path=" + Sonatype.config.resourcePath
    },

    clearCookie : function(cookieName) {
      document.cookie = cookieName + "=null; expires=Thu, 01-Jan-70 00:00:01 GMT" + "; path=" + Sonatype.config.resourcePath
    },

    recoverUsername : function() {
      var w = new Ext.Window({
            title : 'Username Recovery',
            closable : true,
            autoWidth : false,
            width : 300,
            autoHeight : true,
            modal : true,
            constrain : true,
            resizable : false,
            draggable : false,
            items : [{
                  xtype : 'form',
                  labelAlign : 'right',
                  labelWidth : 60,
                  frame : true,
                  defaultType : 'textfield',
                  monitorValid : true,
                  items : [{
                        xtype : 'panel',
                        style : 'padding-left: 70px; padding-bottom: 10px',
                        html : 'Please enter the e-mail address you used to register your account and we will send you your username.'
                      }, {
                        fieldLabel : 'E-mail',
                        name : 'email',
                        width : 200,
                        allowBlank : false
                      }],
                  buttons : [{
                        text : 'E-mail Username',
                        formBind : true,
                        scope : this,
                        handler : function() {
                          var email = w.find('name', 'email')[0].getValue();

                          Ext.Ajax.request({
                                scope : this,
                                method : 'POST',
                                jsonData : {
                                  data : {
                                    email : email
                                  }
                                },
                                url : Sonatype.config.repos.urls.usersForgotId + '/' + email,
                                success : function(response, options) {
                                  w.close();
                                  Sonatype.MessageBox.show({
                                        title : 'Username Recovery',
                                        msg : 'Username request completed successfully.' + '<br /><br />' + 'Check your mailbox, the username reminder should arrive in a few minutes.',
                                        buttons : Sonatype.MessageBox.OK,
                                        icon : Sonatype.MessageBox.INFO,
                                        animEl : 'mb3'
                                      });
                                },
                                failure : function(response, options) {
                                  var errorOptions = {
                                    hideErrorStatus : true
                                  };
                                  Sonatype.utils.connectionError(response, 'There is a problem retrieving your username.', false, errorOptions)
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
                }]
          });

      w.show();
    },

    recoverPassword : function() {
      var w = new Ext.Window({
            title : 'Password Recovery',
            closable : true,
            autoWidth : false,
            width : 300,
            autoHeight : true,
            modal : true,
            constrain : true,
            resizable : false,
            draggable : false,
            items : [{
                  xtype : 'form',
                  labelAlign : 'right',
                  labelWidth : 60,
                  frame : true,
                  defaultType : 'textfield',
                  monitorValid : true,
                  items : [{
                        xtype : 'panel',
                        style : 'padding-left: 70px; padding-bottom: 10px',
                        html : 'Please enter your username and e-mail address below. We will send you a new password.'
                      }, {
                        fieldLabel : 'Username',
                        name : 'username',
                        width : 200,
                        allowBlank : false
                      }, {
                        fieldLabel : 'E-mail',
                        name : 'email',
                        width : 200,
                        allowBlank : false
                      }],
                  buttons : [{
                        text : 'Reset Password',
                        formBind : true,
                        scope : this,
                        handler : function() {
                          var username = w.find('name', 'username')[0].getValue();
                          var email = w.find('name', 'email')[0].getValue();

                          Ext.Ajax.request({
                                scope : this,
                                method : 'POST',
                                jsonData : {
                                  data : {
                                    userId : username,
                                    email : email
                                  }
                                },
                                url : Sonatype.config.repos.urls.usersForgotPassword,
                                success : function(response, options) {
                                  w.close();
                                  Sonatype.MessageBox.show({
                                        title : 'Reset Password',
                                        msg : 'Password request completed successfully.' + '<br /><br />' + 'Check your mailbox, your new password should arrive in a few minutes.',
                                        buttons : Sonatype.MessageBox.OK,
                                        icon : Sonatype.MessageBox.INFO,
                                        animEl : 'mb3'
                                      });
                                },
                                failure : function(response, options) {
                                  var errorOptions = {
                                    hideErrorStatus : true
                                  };
                                  Sonatype.utils.connectionError(response, 'There is a problem resetting your password.', false, errorOptions)
                                }
                              });
                        }
                      }, {
                        text : 'Cancel',
                        formBind : false,
                        scope : this,
                        handler : function() {
                          w.hide();
                          w.close();
                        }
                      }]
                }]
          });

      w.show();
    },

    changePassword : function(expiredUsername) {
      var w = new Ext.Window({
            id : 'change-password-window',
            title : 'Change Password',
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
                        html : (expiredUsername ? 'Your password has expired, you need to reset it. ' : '') + 'Please enter your current password and then the new password twice to confirm.'
                      }, {
                        fieldLabel : 'Current Password',
                        inputType : 'password',
                        name : 'currentPassword',
                        width : 200,
                        allowBlank : false,
                        validateOnBlur : false
                      }, {
                        fieldLabel : 'New Password',
                        inputType : 'password',
                        name : 'newPassword',
                        width : 200,
                        allowBlank : false,
                        validateOnBlur : false
                      }, {
                        fieldLabel : 'Confirm Password',
                        inputType : 'password',
                        name : 'confirmPassword',
                        width : 200,
                        allowBlank : false,
                        validateOnBlur : false,
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
                        id : 'change-password-button',
                        text : 'Change Password',
                        formBind : true,
                        scope : this,
                        handler : function() {
                          var currentPassword = w.find('name', 'currentPassword')[0].getValue();
                          var newPassword = w.find('name', 'newPassword')[0].getValue();

                          Ext.Ajax.request({
                                scope : this,
                                method : 'POST',
                                cbPassThru : {
                                  newPassword : newPassword
                                },
                                jsonData : {
                                  data : {
                                    userId : expiredUsername ? expiredUsername : Sonatype.user.curr.username,
                                    oldPassword : currentPassword,
                                    newPassword : newPassword
                                  }
                                },
                                url : Sonatype.config.repos.urls.usersChangePassword,
                                success : function(response, options) {
                                  if (expiredUsername)
                                  {
                                    Sonatype.utils.doLogin(w, expiredUsername, newPassword);
                                    w.close();
                                  }
                                  else
                                  {
                                    w.close();
                                    Sonatype.utils.updateAuthToken(Sonatype.user.curr.username, options.cbPassThru.newPassword);
                                    Sonatype.MessageBox.show({
                                          title : 'Password Changed',
                                          msg : 'Password change request completed successfully.',
                                          buttons : Sonatype.MessageBox.OK,
                                          icon : Sonatype.MessageBox.INFO,
                                          animEl : 'mb3'
                                        });
                                  }
                                },
                                failure : function(response, options) {
                                  Sonatype.utils.connectionError(response, 'There is a problem changing your password.')
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
                }]
          });

      w.show();
    },

    updateAuthToken : function(username, password) {
      Sonatype.utils.authToken = Sonatype.utils.base64.encode(username + ':' + password);
    },

    doLogin : function(activeWindow, username, password) {
      if (activeWindow)
      {
        activeWindow.getEl().mask("Logging you in...");
      }

      Sonatype.utils.updateAuthToken(username, password);
      Ext.Ajax.request({
            method : 'GET',
            cbPassThru : {
              username : username
            },
            headers : {
              'Authorization' : 'Basic ' + Sonatype.utils.authToken
            }, // @todo: send HTTP basic auth data
            url : Sonatype.config.repos.urls.login,
            success : function(response, options) {
              if (activeWindow)
              {
                activeWindow.getEl().unmask();
              }

              if (Sonatype.repoServer.RepoServer.loginWindow.isVisible())
              {
                Sonatype.view.loginSuccessfulToken = Sonatype.view.afterLoginToken;
                Sonatype.repoServer.RepoServer.loginWindow.hide();
                Sonatype.repoServer.RepoServer.loginForm.getForm().reset();
              }

              var respObj = Ext.decode(response.responseText);
              Sonatype.utils.loadNexusStatus(respObj.data.clientPermissions.loggedInUserSource);
            },
            failure : function(response, options) {
              Sonatype.utils.clearCookie('JSESSIONID');
              Sonatype.utils.authToken = null;
              if (activeWindow)
              {
                activeWindow.getEl().unmask();
              }

              if (Sonatype.repoServer.RepoServer.loginWindow.isVisible())
              {
                Sonatype.repoServer.RepoServer.loginForm.find('name', 'password')[0].focus(true);
              }
              
              Ext.Ajax.request({
                  scope : this,
                  method : 'GET',
                  url : Sonatype.config.repos.urls.logout,
                  callback : function(options, success, response) {
                	Sonatype.utils.clearCookie('JSESSIONID');
                    Sonatype.utils.authToken = null;
                    Sonatype.view.justLoggedOut = true;
                  }
                });
              
            }

          });
    },

    parseFormattedAppName : function(formattedAppName) {
      return formattedAppName;
    },

    loadNexusStatus : function(loggedInUserSource, versionOnly) {
      if (!versionOnly)
      {
        Sonatype.user.curr = Sonatype.utils.cloneObj(Sonatype.user.anon);
      }

      Ext.Ajax.request({
            method : 'GET',
            options : {
              ignore401 : true
            },
            url : Sonatype.config.repos.urls.status,
            callback : function(options, success, response) {
              var baseUrlMismatch = false;

              Sonatype.view.historyDisabled = true;

              if (success)
              {
                var respObj = Ext.decode(response.responseText);

                Sonatype.utils.edition = respObj.data.editionLong;
                Sonatype.utils.editionShort = respObj.data.editionShort;
                Sonatype.utils.version = respObj.data.version;
                Sonatype.utils.attributionsURL = respObj.data.attributionsURL;
                Sonatype.utils.purchaseURL = respObj.data.purchaseURL;
                Sonatype.utils.userLicenseURL = respObj.data.userLicenseURL;

                Sonatype.utils.formattedAppName = Sonatype.utils.parseFormattedAppName(respObj.data.formattedAppName);

                Ext.get('logo').update('<span>' + Sonatype.utils.formattedAppName + '</span>');

                Sonatype.view.headerPanel.doLayout();

                Sonatype.user.curr.repoServer = respObj.data.clientPermissions.permissions;
                Sonatype.user.curr.isLoggedIn = respObj.data.clientPermissions.loggedIn;
                Sonatype.user.curr.username = respObj.data.clientPermissions.loggedInUsername;
                Sonatype.user.curr.loggedInUserSource = respObj.data.clientPermissions.loggedInUserSource;

                var baseUrl = respObj.data.baseUrl;
                baseUrlMismatch = (baseUrl.toLowerCase() != window.location.href.substring(0, baseUrl.length).toLowerCase());
              }
              else
              {
                Sonatype.utils.edition = '';

                Sonatype.user.curr.repoServer = null;
                Sonatype.user.curr.isLoggedIn = null;
                Sonatype.user.curr.username = null;
                Sonatype.user.curr.loggedInUserSource = null;
              }

              var availSvrs = Sonatype.config.installedServers;
              for (var srv in availSvrs)
              {
                if (availSvrs[srv] && typeof(Sonatype[srv]) != 'undefined')
                {
                  Sonatype[srv][Sonatype.utils.capitalize(srv)].statusComplete(respObj);
                }
              }

              Sonatype.Events.fireEvent('initHeadLinks');

              Sonatype.view.serverTabPanel.doLayout();

              if (baseUrlMismatch && Sonatype.lib.Permissions.checkPermission('nexus:settings', Sonatype.lib.Permissions.READ))
              {
                Sonatype.utils.postWelcomePageAlert('<b>WARNING:</b> ' + 'Base URL setting of <a href="' + baseUrl + '">' + baseUrl + '</a> ' + 'does not match your actual URL! ' + 'If you\'re running Apache mod_proxy, here\'s '
                    + '<a href="http://links.sonatype.com/products/nexus/oss/docs-mod-proxy">' + 'more information</a> on configuring Nexus with it.' );
              }

              Sonatype.Events.fireEvent('nexusStatus');

              Sonatype.view.historyDisabled = false;
              var token = Ext.History.getToken();
              if (Sonatype.view.loginSuccessfulToken)
              {
                token = Sonatype.view.loginSuccessfulToken;
                Sonatype.view.loginSuccessfulToken = null;
              }
              Sonatype.utils.onHistoryChange(token);
              Sonatype.utils.updateHistory();
              Sonatype.view.justLoggedOut = false;
            }
          });
    },

    postWelcomePageAlert : function(msg) {
      // we want to insert the toolbar first, rather than add to the page in
      // random spot
      Sonatype.view.welcomeTab.insert(0, {
            xtype : 'panel',
            html : '<div class="x-toolbar-warning-box"><div class="x-form-invalid-msg" style="width: 95%;">' + msg + '</div></div>'
          });
      Sonatype.view.welcomeTab.doLayout();
    },

    onHistoryChange : function(token) {
      if (!token && window.location.hash)
      {
        token = window.location.hash.substring(1);
      }

      if (token && Sonatype.user.curr.repoServer && Sonatype.user.curr.repoServer.length)
      {
        Sonatype.view.historyDisabled = true;

        var toks = token.split(Sonatype.view.HISTORY_DELIMITER);

        var tabId = toks[0];
        var tabPanel = Sonatype.view.mainTabPanel.getComponent(tabId);
        if (tabPanel)
        {
          Sonatype.view.mainTabPanel.setActiveTab(tabId);
        }
        else
        {
          var navigationPanel = Ext.getCmp('navigation-' + tabId);
          if (navigationPanel)
          {
            var c = navigationPanel.initialConfigNavigation;
            if (c)
            {
              tabPanel = Sonatype.view.mainTabPanel.addOrShowTab(c.tabId, c.tabCode, {
                    title : c.tabTitle ? c.tabTitle : c.title
                  });
            }
          }
          else if (Sonatype.view.supportedNexusTabs[tabId] && !Sonatype.view.justLoggedOut && !Sonatype.user.curr.isLoggedIn)
          {
            Sonatype.view.historyDisabled = false;
            Sonatype.view.afterLoginToken = token;
            Sonatype.repoServer.RepoServer.loginHandler();
            return;
          }
        }
        Sonatype.view.historyDisabled = false;

        if (tabPanel && tabPanel.applyBookmark && toks.length > 1)
        {
          tabPanel.applyBookmark(toks[1]);
        }
      }
    },

    updateHistory : function(tab) {
      var bookmark = Sonatype.utils.getBookmark(tab);
      if (!bookmark)
      {
        return;
      }
      var oldBookmark = Ext.History.getToken();
      if (bookmark != oldBookmark)
      {
        Ext.History.add(bookmark);
      }
    },

    replaceHistory : function(tab) {
      var bookmark = Sonatype.utils.getBookmark(tab);
      if (!bookmark)
      {
        return;
      }
      location.replace(window.location.href.replace(/\#.*$/, '') + '#' + bookmark);
    },

    getBookmark : function(tab) {
      if (Sonatype.view.historyDisabled)
        return;

      var bookmark = null;

      if (tab)
      {
        if (tab.ownerCt != Sonatype.view.mainTabPanel || tab != Sonatype.view.mainTabPanel.getActiveTab())
          return;

        bookmark = tab.id;
      }
      else
      {
        // this is to handle very special where we are passing a bookmark on
        // redirect, in IE this is not handled very well at all, so we trick it
        var oldBookmark = Ext.History.getToken();
        if (oldBookmark && oldBookmark.indexOf('force') == 0)
        {
          bookmark = oldBookmark.substring('force'.length);
        }
        else
        {
          tab = Sonatype.view.mainTabPanel.getActiveTab();
          bookmark = tab.id;
        }
      }

      if (tab && tab.getBookmark)
      {
        var b2 = tab.getBookmark();
        if (b2)
        {
          bookmark += Sonatype.view.HISTORY_DELIMITER + b2;
        }
      }

      return bookmark;
    },

    openWindow : function(url) {
      window.open(url);
    },

    appendAuth : function(url) {
      if (url.indexOf('?') >= 0)
      {
        return url + '&authorization=Basic ' + Sonatype.utils.authToken;
      }
      else
      {
        return url + '?authorization=Basic ' + Sonatype.utils.authToken;
      }

    },

    generateErrorReportHandler : function(username) {
      Ext.Ajax.request({
            method : 'GET',
            scope : this,
            url : Sonatype.config.repos.urls.globalSettingsState,
            callback : function(options, success, response) {
              if (success)
              {
                var dec = Ext.decode(response.responseText);
                var needCredentials = dec.data.errorReportingSettings == null || dec.data.errorReportingSettings.jiraUsername == null || dec.data.errorReportingSettings.jiraUsername == '';
                Sonatype.utils.showProbleReport(needCredentials);
              }
              else
              {
                Sonatype.utils.connectionError(response, 'Error generating Problem Report');
              }
            }
          });
    },

    showProbleReport : function(requestCredentials) {
      var items = [];

      var credentials = [];
      items.push({
            xtype : 'fieldset',
            checkboxToggle : !requestCredentials,
            collapsed : !requestCredentials,
            collapsible : !requestCredentials,
            name : 'errorReportingSettings',
            title : requestCredentials ? 'Enter JIRA Credentials' : 'Override Default JIRA Credentials (optional)',
            anchor : Sonatype.view.FIELDSET_OFFSET,
            autoHeight : true,
            layoutConfig : {
              labelSeparator : ''
            },
            items : credentials
          });

      credentials.push({
            xtype : 'panel',
            style : 'padding-bottom: 10px',
            html : 'In order to submit a Problem Report you must have JIRA account. Click here to <a href="http://links.sonatype.com/products/nexus/oss/issues-signup" target="_blank">Sign Up</a>.'
          });
      credentials.push({
            xtype : 'textfield',
            itemCls : 'required-field',
            fieldLabel : 'JIRA Username',
            helpText : Sonatype.repoServer.resources.help.server.jiraUsername,
            name : 'jiraUsername',
            allowBlank : !requestCredentials
          });
      credentials.push({
            xtype : 'textfield',
            itemCls : 'required-field',
            fieldLabel : 'JIRA Password',
            helpText : Sonatype.repoServer.resources.help.server.jiraPassword,
            inputType : 'password',
            name : 'jiraPassword',
            allowBlank : !requestCredentials
          });

      var sp = Sonatype.lib.Permissions;
      // we can't ofer to save if APR is not enabled
      if (sp.checkPermission('nexus:settings', sp.EDIT) && !requestCredentials)
      {
        credentials.push({
              xtype : 'checkbox',
              fieldLabel : 'Save as default',
              helpText : 'If selected will save currect Problem Report settings as default settings.',
              name : 'saveErrorReportingSettings'
            });
      }

      items.push({
            xtype : 'panel',
            style : 'padding-left: 10px; padding-bottom: 10px',
            html : 'Enter a short title for the Problem Report, along with a more detailed description.'
          });

      items.push({
            fieldLabel : 'Enter a short title',
            itemCls : 'required-field',
            name : 'title',
            width : 250,
            allowBlank : false
          });
      items.push({
            xtype : 'textarea',
            fieldLabel : 'Description',
            name : 'description',
            width : 250,
            height : 150
          });

      items.push({
        xtype : 'panel',
        style : 'padding-left: 10px; padding-bottom: 10px',
        html : 'Copies of your logs and configuration will be attached to this report to allow Sonatype to more quickly solve the problem. All passwords and emails are removed and the bundle is encrypted locally with Sonatype\'s Public key before transmitting it to Sonatype. Your data will be handled with strict confidence and will not be disclosed to any third parties.'
      });

      var w = new Ext.Window({
            title : 'Generate Nexus Problem Report',
            closable : true,
            autoWidth : false,
            width : 425,
            autoHeight : true,
            modal : true,
            constrain : true,
            resizable : false,
            draggable : false,
            items : [{
                  xtype : 'form',
                  labelWidth : 125,
                  frame : true,
                  defaultType : 'textfield',
                  monitorValid : true,
                  items : items,
                  buttons : [{
                        text : 'Submit',
                        formBind : true,
                        scope : this,
                        handler : function() {
                          var title = w.find('name', 'title')[0].getValue();
                          var description = w.find('name', 'description')[0].getValue();
                          if (requestCredentials || w.find('name', 'errorReportingSettings')[0])
                          {
                            var jiraUser = w.find('name', 'jiraUsername')[0].getValue();
                            var jiraPass = w.find('name', 'jiraPassword')[0].getValue();
                            var saveCheck = w.find('name', 'saveErrorReportingSettings')[0];
                            var saveErrorReportingSettings;
                            if (saveCheck)
                            {
                              saveErrorReportingSettings = w.find('name', 'saveErrorReportingSettings')[0].getValue();
                            }
                            else
                            {
                              saveErrorReportingSettings = false
                            }
                            Sonatype.utils.createProblemReport(w, title, description, jiraUser, jiraPass, saveErrorReportingSettings);
                          }
                          else
                          {
                            Sonatype.utils.createProblemReport(w, title, description);
                          }
                        }
                      }, {
                        text : 'Cancel',
                        formBind : false,
                        scope : this,
                        handler : function() {
                          w.close();
                        }
                      }]
                }]
          });

      w.show();
    },

    createProblemReport : function(window, title, description, username, password, saveErrorReportingSettings) {
      Sonatype.MessageBox.wait('Generating Report ...');
      Ext.Ajax.request({
            method : 'PUT',
            url : Sonatype.config.servicePath + '/error_reporting',
            // 10 minute timeout, upload may be large...
            timeout : 600000,
            jsonData : {
              data : {
                title : title,
                description : description,
                saveErrorReportingSettings : saveErrorReportingSettings,
                errorReportingSettings : {
                  jiraUsername : username,
                  jiraPassword : password
                }
              }
            },
            cbPassThru : {
              window : window
            },
            callback : function(options, success, response) {
              var json = Ext.decode(response.responseText);

              if (success)
              {
                options.cbPassThru.window.close();
                Sonatype.MessageBox.show({
                      title : 'Problem Report Generated',
                      msg : 'Your Problem Report was generated <a href=' + json.data.jiraUrl + ' target="_new">here</a>.',
                      buttons : Sonatype.MessageBox.OK,
                      icon : Sonatype.MessageBox.INFO
                    });
              }
              else
              {
                Sonatype.utils.connectionError(response, 'Error generating Problem Report');
              }
            }
          });
    }

  };

})();

Ext.apply(Ext.form.VTypes, {
      password : function(val, field) {
        if (field.initialPasswordField != null && field.initialPasswordField != 'undefined')
        {
          var pwd = field.ownerCt.find('name', field.initialPasswordField)[0].getValue();
          return (val == pwd);
        }
        return true;
      },

      passwordText : 'Passwords do not match.'
    });
