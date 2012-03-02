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
var MIRROR_URL_REGEXP = /^(?:http|https):\/\//i;

Sonatype.repoServer.AbstractMirrorPanel = function(config) {
  var config = config || {};

  this.mirrorStatusTask = {
    run : function() {
      Ext.Ajax.request({
            url : Sonatype.config.repos.urls.repoMirrorStatus + '/' + this.payload.data.id,
            callback : this.statusCallback,
            scope : this
          });
    },
    interval : 5000, // poll every 5 seconds
    scope : this
  };

  this.mirrorRecordConstructor = Ext.data.Record.create([{
        name : 'id'
      }, {
        name : 'url',
        sortType : Ext.data.SortTypes.asUCString
      }]);

  this.mirrorReader = new Ext.data.JsonReader({
        root : 'data',
        id : 'id'
      }, this.mirrorRecordConstructor);

  this.mirrorDataStore = new Ext.data.Store({
        url : Sonatype.config.repos.urls.repoMirrors + '/' + this.payload.data.id,
        reader : this.mirrorReader,
        sortInfo : {
          field : 'url',
          direction : 'ASC'
        },
        autoLoad : false
      });

  var defaultConfig = {
    uri : Sonatype.config.repos.urls.repoMirrors + '/' + this.payload.data.id,
    referenceData : Sonatype.repoServer.referenceData.repoMirrors,
    dataStores : [this.mirrorDataStore],
    dataModifiers : {
      load : {
        'rootData' : this.loadMirrors.createDelegate(this)
      },
      submit : {
        'rootData' : this.saveMirrors.createDelegate(this)
      }
    },
    listeners : {
      submit : {
        fn : this.submitHandler,
        scope : this
      },
      activate : {
        fn : this.activateHandler,
        scope : this
      },
      deactivate : {
        fn : this.deactivateHandler,
        scope : this
      },
      destroy : {
        fn : this.destroyHandler,
        scope : this
      }
    }
  };

  Ext.apply(this, config, defaultConfig);

  Sonatype.repoServer.AbstractMirrorPanel.superclass.constructor.call(this, {});
};

Ext.extend(Sonatype.repoServer.AbstractMirrorPanel, Sonatype.ext.FormPanel, {
      addNewMirrorUrl : function() {
        var treePanel = this.find('name', 'mirror-url-list')[0];
        var urlField = this.find('name', 'mirrorUrl')[0];
        if (urlField.isValid())
        {
          var url = urlField.getRawValue();

          if (url)
          {
            var nodes = treePanel.root.childNodes;
            for (var i = 0; i < nodes.length; i++)
            {
              if (url == nodes[i].attributes.payload.url)
              {
                urlField.markInvalid('This URL already exists');
                return;
              }
            }

            urlField.clearInvalid();

            this.addUrlNode(treePanel, url, url, Sonatype.config.extPath + '/resources/images/default/tree/leaf.gif');
            urlField.setRawValue('');
            urlField.setValue('');
          }
        }
      },

      addUrlNode : function(treePanel, url, id, icon) {
        var validId;
        var manualUrl;
        if (url == id)
        {
          validId = Ext.id();
          manualUrl = true;
        }
        else
        {
          validId = id;
          manualUrl = false;
        }
        treePanel.root.appendChild(new Ext.tree.TreeNode({
              id : id,
              text : url,
              href : url,
              hrefTarget : '_new',
              payload : {
                id : manualUrl ? '' : id,
                url : url
              },
              allowChildren : false,
              draggable : true,
              leaf : true,
              icon : icon
            }));
      },

      removeMirrorUrl : function() {
        var treePanel = this.find('name', 'mirror-url-list')[0];

        var selectedNode = treePanel.getSelectionModel().getSelectedNode();
        if (selectedNode)
        {
          treePanel.root.removeChild(selectedNode);
        }
      },

      removeAllMirrorUrls : function() {
        var treePanel = this.find('name', 'mirror-url-list')[0];
        var treeRoot = treePanel.root;

        while (treeRoot.lastChild)
        {
          treeRoot.removeChild(treeRoot.lastChild);
        }
      },

      loadMirrors : function(arr, srcObj, fpanel) {
        var treePanel = this.find('name', 'mirror-url-list')[0];

        var mirrorArray = new Array();

        for (var i = 0; i < arr.length; i++)
        {
          var treePanel = this.find('name', 'mirror-url-list')[0];
          var childNodes = treePanel.getRootNode().childNodes;
          var found = false;
          if (childNodes && childNodes.length)
          {
            for (var j = 0; j < childNodes.length; j++)
            {
              if (arr[i].id == childNodes[j].id)
              {
                mirrorArray[i] = {
                  id : arr[i].id,
                  url : arr[i].url,
                  icon : childNodes[j].ui.iconNode.src
                };
                found = true;
                break;
              }
            }
          }
          if (!found)
          {
            mirrorArray[i] = {
              id : arr[i].id,
              url : arr[i].url,
              icon : Sonatype.config.extPath + '/resources/images/default/tree/leaf.gif'
            };
          }
        }

        this.removeAllMirrorUrls();

        for (var i = 0; i < arr.length; i++)
        {
          this.addUrlNode(treePanel, mirrorArray[i].url, mirrorArray[i].id, mirrorArray[i].icon);
        }

        return arr;
      },

      saveMirrors : function(val, fpanel) {
        var treePanel = this.find('name', 'mirror-url-list')[0];

        var outputArr = [];
        var nodes = treePanel.root.childNodes;

        for (var i = 0; i < nodes.length; i++)
        {
          outputArr[i] = nodes[i].attributes.payload;
        }

        return outputArr;
      },

      getActionURL : function() {
        return this.uri;
      },

      getSaveMethod : function() {
        return 'POST';
      },

      statusCallback : function(options, success, response) {
        if (success)
        {
          var statusResp = Ext.decode(response.responseText);
          if (statusResp.data)
          {
            var data = statusResp.data;
            if (data && data.length)
            {
              for (var i = 0; i < data.length; i++)
              {
                var item = data[i];
                var treePanel = this.find('name', 'mirror-url-list')[0];
                var childNodes = treePanel.getRootNode().childNodes;
                if (childNodes && childNodes.length)
                {
                  for (var j = 0; j < childNodes.length; j++)
                  {
                    if (item.id == childNodes[j].id)
                    {
                      childNodes[j].getUI().getIconEl().src = item.status == 'Blacklisted' ? (Sonatype.config.extPath + '/resources/images/default/tree/drop-no.gif') : (Sonatype.config.extPath + '/resources/images/default/tree/drop-yes.gif');
                      break;
                    }
                  }
                }
              }
            }
          }
        }
        else
        {
          Ext.TaskMgr.stop(this.mirrorStatusTask);
          Sonatype.MessageBox.alert('Status retrieval failed');
        }
      },

      submitHandler : function(form, action, receivedData) {
        this.loadMirrors(receivedData, null, this);
      },

      activateHandler : function(panel) {
        this.predefinedMirrorDataStore.load();

        if (panel.payload.data.repoType == 'proxy')
        {
          Ext.TaskMgr.start(this.mirrorStatusTask);
        }
      },

      deactivateHandler : function(panel) {
        if (panel.payload.data.repoType == 'proxy')
        {
          Ext.TaskMgr.stop(this.mirrorStatusTask);
        }
      },

      destroyHandler : function(component) {
        if (component.payload.data.repoType == 'proxy')
        {
          Ext.TaskMgr.stop(this.mirrorStatusTask);
        }
      }
    });

Sonatype.repoServer.ProxyMirrorEditor = function(config) {
  var config = config || {};
  Ext.apply(this, config, {});
  var ht = Sonatype.repoServer.resources.help.repoMirrors;

  this.mirrorRecordConstructor = Ext.data.Record.create([{
        name : 'id'
      }, {
        name : 'url',
        sortType : Ext.data.SortTypes.asUCString
      }]);

  this.mirrorReader = new Ext.data.JsonReader({
        root : 'data',
        id : 'id'
      }, this.mirrorRecordConstructor);

  this.predefinedMirrorDataStore = new Ext.data.Store({
        url : Sonatype.config.repos.urls.repoPredefinedMirrors + '/' + this.payload.data.id,
        reader : this.mirrorReader,
        sortInfo : {
          field : 'url',
          direction : 'ASC'
        }
      });

  Sonatype.repoServer.ProxyMirrorEditor.superclass.constructor.call(this, {
        items : [{
              xtype : 'panel',
              style : 'padding-top: 20px',
              layout : 'column',
              items : [{
                    xtype : 'panel',
                    layout : 'form',
                    labelWidth : 150,
                    width : 430,
                    items : [{
                          xtype : 'combo',
                          fieldLabel : 'Mirror URL',
                          helpText : ht.mirrorUrl,
                          name : 'mirrorUrl',
                          width : 238,
                          listWidth : 238,
                          store : this.predefinedMirrorDataStore,
                          displayField : 'url',
                          valueField : 'id',
                          editable : true,
                          forceSelection : false,
                          mode : 'local',
                          triggerAction : 'all',
                          emptyText : 'Enter or Select URL...',
                          selectOnFocus : true,
                          allowBlank : true,
                          validator : function(v) {
                            if (v.match(MIRROR_URL_REGEXP))
                            {
                              return true;
                            }
                            else
                            {
                              return 'Protocol must be http:// or https://';
                            }
                          }
                        }]
                  }, {
                    xtype : 'panel',
                    width : 120,
                    items : [{
                          xtype : 'button',
                          text : 'Add',
                          style : 'padding-left: 7px',
                          minWidth : 100,
                          id : 'button-add',
                          handler : this.addNewMirrorUrl,
                          scope : this
                        }]
                  }]
            }, {
              xtype : 'panel',
              layout : 'column',
              autoHeight : true,
              style : 'padding-left: 155px',
              items : [{
                    xtype : 'treepanel',
                    name : 'mirror-url-list',
                    title : 'Mirror URLs',
                    border : true,
                    bodyBorder : true,
                    bodyStyle : 'background-color:#FFFFFF; border: 1px solid #B5B8C8',
                    style : 'padding: 0 20px 0 0',
                    width : 275,
                    height : 300,
                    animate : true,
                    lines : false,
                    autoScroll : true,
                    containerScroll : true,
                    rootVisible : false,
                    ddScroll : true,
                    enableDD : true,
                    root : new Ext.tree.TreeNode({
                          text : 'root',
                          draggable : false
                        })
                  }, {
                    xtype : 'panel',
                    width : 120,
                    items : [{
                          xtype : 'button',
                          text : 'Remove',
                          style : 'padding-left: 6px',
                          minWidth : 100,
                          id : 'button-remove',
                          handler : this.removeMirrorUrl,
                          scope : this
                        }, {
                          xtype : 'button',
                          text : 'Remove All',
                          style : 'padding-left: 6px; margin-top: 5px',
                          minWidth : 100,
                          id : 'button-remove-all',
                          handler : this.removeAllMirrorUrls,
                          scope : this
                        }]
                  }]
            }]
      });
};

Ext.extend(Sonatype.repoServer.ProxyMirrorEditor, Sonatype.repoServer.AbstractMirrorPanel, {});

Sonatype.repoServer.HostedMirrorEditor = function(config) {
  var config = config || {};
  Ext.apply(this, config, {});
  var ht = Sonatype.repoServer.resources.help.repoMirrors;

  Sonatype.repoServer.HostedMirrorEditor.superclass.constructor.call(this, {
        items : [{
              xtype : 'panel',
              style : 'padding-top: 20px',
              layout : 'column',
              items : [{
                    xtype : 'panel',
                    layout : 'form',
                    labelWidth : 150,
                    width : 430,
                    items : [{
                          xtype : 'textfield',
                          fieldLabel : 'Mirror URL',
                          helpText : ht.mirrorUrl,
                          name : 'mirrorUrl',
                          width : 255,
                          emptyText : 'Enter URL...',
                          selectOnFocus : true,
                          validator : function(v) {
                            if (v.match(MIRROR_URL_REGEXP))
                            {
                              return true;
                            }
                            else
                            {
                              return 'Protocol must be http:// or https://';
                            }
                          }
                        }]
                  }, {
                    xtype : 'panel',
                    width : 120,
                    items : [{
                          xtype : 'button',
                          text : 'Add',
                          style : 'padding-left: 7px',
                          minWidth : 100,
                          id : 'button-add',
                          handler : this.addNewMirrorUrl,
                          scope : this
                        }]
                  }]
            }, {
              xtype : 'panel',
              layout : 'column',
              autoHeight : true,
              style : 'padding-left: 155px',
              items : [{
                    xtype : 'treepanel',
                    name : 'mirror-url-list',
                    title : 'Mirror URLs',
                    border : true,
                    bodyBorder : true,
                    bodyStyle : 'background-color:#FFFFFF; border: 1px solid #B5B8C8',
                    style : 'padding: 0 20px 0 0',
                    width : 275,
                    height : 300,
                    animate : true,
                    lines : false,
                    autoScroll : true,
                    containerScroll : true,
                    rootVisible : false,
                    enableDD : false,
                    root : new Ext.tree.TreeNode({
                          text : 'root'
                        })
                  }, {
                    xtype : 'panel',
                    width : 120,
                    items : [{
                          xtype : 'button',
                          text : 'Remove',
                          style : 'padding-left: 6px',
                          minWidth : 100,
                          id : 'button-remove',
                          handler : this.removeMirrorUrl,
                          scope : this
                        }, {
                          xtype : 'button',
                          text : 'Remove All',
                          style : 'padding-left: 6px; margin-top: 5px',
                          minWidth : 100,
                          id : 'button-remove-all',
                          handler : this.removeAllMirrorUrls,
                          scope : this
                        }]
                  }]
            }]
      });
};

Ext.extend(Sonatype.repoServer.HostedMirrorEditor, Sonatype.repoServer.AbstractMirrorPanel, {});

Sonatype.Events.addListener('repositoryViewInit', function(cardPanel, rec) {
      var sp = Sonatype.lib.Permissions;
      if (rec.data.resourceURI && sp.checkPermission('nexus:repositorymirrors', sp.READ) && rec.data.userManaged == true && (rec.data.repoType == 'proxy' || rec.data.repoType == 'hosted'))
      {
        if (rec.data.repoType == 'proxy')
        {
          cardPanel.add(new Sonatype.repoServer.ProxyMirrorEditor({
                payload : rec,
                tabTitle : 'Mirrors',
                name : 'mirrors'
              }));
        }
        else if (rec.data.repoType == 'hosted')
        {
          cardPanel.add(new Sonatype.repoServer.HostedMirrorEditor({
                payload : rec,
                tabTitle : 'Mirrors',
                name : 'mirrors'
              }));
        }
      }
    });
