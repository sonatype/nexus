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
/*global NX, Nexus, Ext, Sonatype*/

NX.define('Sonatype.repoServer.LogsViewPanel', {
  extend : 'Ext.form.FormPanel',
  requirejs : ['Sonatype/all'],
  constructor : function(cfg) {
    Ext.apply(this, cfg || {});

    this.currentLogUrl = null;
    this.currentContentType = null;
    this.totalSize = 0;
    this.currentSize = 0;
    this.currentOffset = 0;
    this.doLoadTail = false;
    this.tailed = true;
    this.tailEnabled = true;

    this.logListLoaded = false;
    this.logToShowWhenLoaded = null;

    this.listeners = {
      'beforerender' : {
        fn : function() {
          this.updateLogFileList();
          this.updateConfigFileList();
        },
        scope : this
      },
      'beforedestroy' : {
        fn : this.stopTailUpdateTask,
        scope : this
      }
    };

    this.tailUpdateTask = {
      run : function() {
        this.loadTail();
      },
      interval : 10000,
      scope : this,
      started : false
    };

    this.tailUpdateButton = new Ext.SplitButton({
      text : 'Update log tail manually',
      icon : Sonatype.config.extPath + '/resources/images/default/grid/refresh.gif',
      cls : 'x-btn-text-icon',
      value : '0',
      handler : this.loadTailButtonHandler,
      disabled : true,
      scope : this,
      menu : {
        items : [
          {
            text : 'Update log tail manually',
            value : '0',
            scope : this,
            checked : true,
            group : 'tail-update',
            handler : this.loadTailButtonHandler
          },
          {
            text : 'Update every 20 seconds',
            value : '20',
            scope : this,
            checked : false,
            group : 'tail-update',
            handler : this.loadTailButtonHandler
          },
          {
            text : 'Update every minute',
            value : '60',
            scope : this,
            checked : false,
            group : 'tail-update',
            handler : this.loadTailButtonHandler
          },
          {
            text : 'Update every 2 minutes',
            value : '120',
            scope : this,
            checked : false,
            group : 'tail-update',
            handler : this.loadTailButtonHandler
          },
          {
            text : 'Update every 5 minutes',
            value : '300',
            scope : this,
            checked : false,
            group : 'tail-update',
            handler : this.loadTailButtonHandler
          }
        ]
      }

    });

    this.fetchMoreButton = new Ext.SplitButton({
      text : 'Fetch Next 100Kb',
      icon : Sonatype.config.resourcePath + '/static/images/icons/search.gif',
      cls : 'x-btn-text-icon',
      value : '100',
      handler : this.fetchMore,
      disabled : true,
      hidden : true,
      scope : this,
      menu : {
        items : [
          {
            text : 'Fetch Next 100Kb',
            value : '100',
            scope : this,
            checked : true,
            group : 'fetch-more',
            handler : this.fetchMore
          },
          {
            text : 'Fetch Next 200Kb',
            value : '200',
            scope : this,
            checked : false,
            group : 'fetch-more',
            handler : this.fetchMore
          },
          {
            text : 'Fetch Next 500Kb',
            value : '500',
            scope : this,
            checked : false,
            group : 'fetch-more',
            handler : this.fetchMore
          }
        ]
      }
    });

    this.fetchMoreBar = new Ext.Toolbar({
      ctCls : 'search-all-tbar',
      items : ['Displaying 0 of 0Kb', ' ', this.tailUpdateButton, this.fetchMoreButton]
    });

    Sonatype.repoServer.LogsViewPanel.superclass.constructor.call(this, {
      autoScroll : false,
      border : false,
      frame : false,
      collapsible : false,
      collapsed : false,
      tbar : [
        {
          text : "Reload",
          tooltip : {
            text : 'Reloads the current document'
          },
          iconCls : 'st-icon-refresh',
          cls : 'x-btn-text-icon',
          handler : this.reloadAllFiles,
          scope : this
        },
        {
          text : 'Download',
          icon : Sonatype.config.resourcePath + '/static/images/icons/page_white_put.png',
          cls : 'x-btn-text-icon',
          scope : this,
          handler : function() {
            if (this.currentLogUrl) {
              Sonatype.utils.openWindow(this.currentLogUrl);
            }
          }
        },
        {
          id : 'log-btn',
          text : 'Select a document...',
          icon : Sonatype.config.resourcePath + '/static/images/icons/page_white_stack.png',
          cls : 'x-btn-text-icon',
          tooltip : {
            text : 'Select the file to display'
          },
          // handler: this.movePreview.createDelegate(this, []),
          menu : {
            width : 200,
            items : [

            ]
          }
        },
        {
          xtype : 'tbspacer'
        },
        ' ',
        {
          xtype : 'checkbox',
          checked : true,
          boxLabel : 'Tail',
          listeners : {
            'check' : {
              fn : function(checkbox, checked) {
                this.tailUpdateButton.setVisible(checked);
                this.fetchMoreButton.setVisible(!checked);
                this.tailEnabled = checked;
                this.fixUpdateTask();
              },
              scope : this
            }
          }
        }
      ],
      bbar : this.fetchMoreBar,
      items : [
        {
          xtype : 'textarea',
          id : 'log-text',
          readOnly : true,
          hideLabel : true,
          anchor : '100% 100%',
          emptyText : 'Select a document to view',
          style : 'font-family: monospace'
        }
      ]
    });

    this.logTextArea = this.findById('log-text');
  },

  renderLogList : function(options, success, response) {
    if (!success) {
      Sonatype.MessageBox.alert('Failed to get file list from server.');
    }
    else {
      var
            i, name, size, text, uri, existingItem,
            resp = Ext.decode(response.responseText),
            myMenu = Ext.getCmp('log-btn').menu,
            list = resp.data,
            sameUri = function(o) {
              return o.logUri === uri;
            };

      this.sortFileListByName(list);

      for (i = 0; i < list.length; i += 1) {
        name = list[i].name;
        size = list[i].size;
        text = name + ' (' + this.printKb(size) + ')';
        uri = list[i].resourceURI;

        existingItem = myMenu.items.find(sameUri);

        if (existingItem) {
          existingItem.setText(text);
          existingItem.value = size;
          if (this.currentLogUrl === uri) {
            this.totalSize = size;
            this.getTopToolbar().items.get(2).setText(text);
            this.updateTotals();
          }
        }
        else {
          myMenu.addMenuItem({
            id : name,
            logUri : uri,
            text : text,
            value : size,
            checked : false,
            group : 'rp-group',
            checkHandler : this.showItem.createDelegate(this, [name, 'text/plain'], 0),
            scope : this
          });
        }
      }

      if (this.doUpdateTail) {
        this.doUpdateTail = false;
        this.getLogFile();
      }

      // we might be waiting to show a log file
      this.logListLoaded = true;
      if (this.logToShowWhenLoaded) {
        // show the log
        this.showLog(this.logToShowWhenLoaded);
        this.logToShowWhenLoaded = null;
      }
    }
  },

  renderConfigList : function(options, success, response) {
    if (!success) {
      Sonatype.MessageBox.alert('Failed to get config file list from server.');
    }
    else {
      var
            i, name, uri, existingItem,
            resp = Ext.decode(response.responseText),
            myMenu = Ext.getCmp('log-btn').menu,
            list = resp.data,
            sameUri = function(o) {
              return o.logUri === uri;
            };

      this.sortFileListByName(list);

      if (list.length > 0) {
        myMenu.add('-');
      }

      for (i = 0; i < list.length; i += 1) {
        name = list[i].name;
        uri = list[i].resourceURI;

        existingItem = myMenu.items.find(sameUri);

        if (!existingItem) {
          myMenu.addMenuItem({
            id : name,
            logUri : uri,
            text : name,
            value : 0,
            checked : false,
            group : 'rp-group',
            checkHandler : this.showItem.createDelegate(this, [name, 'application/xml'], 0),
            scope : this
          });
        }
      }
    }
  },

  showItem : function(shortLogName, contentType, mItem, pressed) {
    if (!pressed) {
      return;
    }
    this.logTextArea.setRawValue('');
    this.currentSize = 0;
    this.currentOffset = 0;
    this.totalSize = mItem.value;
    this.currentContentType = contentType;
    this.getTopToolbar().items.get(2).setText(mItem.text);
    this.currentLogUrl = mItem.logUri;
    this.tailed = this.tailEnabled;

    if (this.tailed) {
      this.doUpdateTail = true;
      this.updateLogFileList();
    }
    else {
      this.getLogFile();
    }
  },

  showLog : function(logName) {
    // check to make sure the menubar is rendered
    if (this.logListLoaded) {
      var ii, logsAndConfMenu = Ext.getCmp('log-btn').menu, tmpMenuItem;
      for (ii = 0; ii < logsAndConfMenu.items.length; ii += 1) {
        tmpMenuItem = logsAndConfMenu.items.itemAt(ii);
        if (tmpMenuItem && tmpMenuItem.id === logName) {
          this.showItem(tmpMenuItem.logUri, 'text/plain', tmpMenuItem, true);
          break;
        }
      }

    }
    else {
      // load it when when the list is done rendering
      this.logToShowWhenLoaded = logName;
    }

  },

  // reload log and config file list
  // reload the log file specified by this.currentLogUrl
  reloadAllFiles : function() {
    this.updateLogFileList();
    this.updateConfigFileList();

    this.currentSize = 0;
    this.currentOffset = 0;
    this.tailed = this.tailEnabled;
    this.logTextArea.setRawValue('');
    this.getLogFile();
  },

  // gets the log file specified by this.currentLogUrl
  getLogFile : function() {
    // Don't bother refreshing if no files are currently shown
    if (this.currentLogUrl) {
      var toFetch = 0;
      if (!this.tailEnabled) {
        toFetch = Number(this.fetchMoreButton.value) * 1024;
      }
      if (toFetch === 0) {
        if (this.tailed && this.currentOffset === 0 && this.totalSize > 102400) { // 100Kb
          this.currentOffset = this.totalSize - 102400;
        }
        toFetch = this.totalSize - this.currentOffset;
      }
      Ext.Ajax.request({
        callback : this.renderLog,
        scope : this,
        method : 'GET',
        params : {
          from : this.currentOffset,
          count : toFetch
        },
        headers : {
          'accept' : this.currentContentType || 'text/plain'
        },
        url : this.currentLogUrl
      });
    }
  },

  renderLog : function(options, success, response) {
    if (success) {
      var
            text = response.responseText,
            newValue = this.currentSize === 0 ? text : this.logTextArea.getRawValue() + text,
            logDom = this.logTextArea.getEl().dom,
            scrollTop = logDom.scrollTop,
            scrollHeight = logDom.scrollHeight,
            clientHeight = logDom.clientHeight;
      this.logTextArea.setRawValue(newValue);
      if (this.tailEnabled && (scrollTop === 0 || scrollTop + clientHeight >= scrollHeight - 20)) {
        scrollTop = logDom.scrollHeight - clientHeight;
      }
      logDom.scrollTop = scrollTop;

      this.currentSize += response.responseText.length;
      this.currentOffset += response.responseText.length;
      this.updateTotals();
      this.updateLogFileList();
    }
    else {
      Sonatype.utils.connectionError(response, 'The file failed to load from the server.');
    }
  },

  fetchMore : function(button, event) {
    if (button.value !== this.fetchMoreButton.value) {
      this.fetchMoreButton.value = button.value;
      this.fetchMoreButton.setText(button.text);
    }
    this.getLogFile();
  },

  updateTotals : function() {
    if (this.currentOffset > this.totalSize) {
      this.totalSize = this.currentOffset;
    }

    this.fetchMoreBar.items.items[0].destroy();
    this.fetchMoreBar.items.removeAt(0);
    this.fetchMoreBar.insertButton(0,
          new Ext.Toolbar.TextItem('Displaying ' + (this.tailed ? 'last ' : '') + this.printKb(this.currentSize)
                + ' of ' + this.printKb(this.totalSize)));

    this.tailUpdateButton.setDisabled(this.currentLogUrl === Sonatype.config.repos.urls.configCurrent);
    this.fetchMoreButton.setDisabled(this.currentOffset >= this.totalSize);
  },

  printKb : function(n) {
    var kb = 'b';
    if (n > 1024) {
      n = (n / 1024).toFixed(0);
      kb = 'Kb';
    }
    if (n > 1024) {
      n = (n / 1024).toFixed(1);
      kb = 'Mb';
    }
    return n + ' ' + kb;
  },

  updateLogFileList : function() {
    Ext.Ajax.request({
      callback : this.renderLogList,
      scope : this,
      method : 'GET',
      url : Sonatype.config.repos.urls.logs
    });
  },

  updateConfigFileList : function() {
    Ext.Ajax.request({
      callback : this.renderConfigList,
      scope : this,
      method : 'GET',
      url : Sonatype.config.repos.urls.configs
    });
    return true;
  },

  loadTailButtonHandler : function(button, event) {
    if (button.value !== this.tailUpdateButton.value) {
      this.tailUpdateButton.value = button.value;
      this.tailUpdateButton.setText(button.text);
      this.fixUpdateTask();
    }
    else if (button.value === 0) {
      this.loadTail();
    }
  },

  loadTail : function() {
    this.doUpdateTail = true;
    if (!this.tailed) {
      // if the file had been partially read in non-tail mode, reset the
      // counter
      this.tailed = true;
      this.currentOffset = 0;
      this.currentSize = 0;
    }
    this.updateLogFileList();
  },

  fixUpdateTask : function() {
    this.doUpdateTail = false;
    this.stopTailUpdateTask();

    if (this.tailEnabled) {
      var n = this.tailUpdateButton.value;
      if (n > 0) {
        this.tailUpdateTask.interval = n * 1000;
        this.tailUpdateTask.started = true;
        Ext.TaskMgr.start(this.tailUpdateTask);
      }
    }
  },

  stopTailUpdateTask : function() {
    if (this.tailUpdateTask.started) {
      Ext.TaskMgr.stop(this.tailUpdateTask);
      this.tailUpdateTask.started = false;
    }
  },

  sortFileListByName : function(list) {
    list.sort(function(a, b) {
      var
            valueA = a.name.toLowerCase(),
            valueB = b.name.toLowerCase();

      if (valueA < valueB) {
        return -1;
      }

      if (valueA > valueB) {
        return 1;
      }

      return 0;
    });
  }
});

