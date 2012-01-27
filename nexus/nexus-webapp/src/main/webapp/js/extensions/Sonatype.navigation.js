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
Ext.namespace('Sonatype.navigation');

Sonatype.navigation.NavigationPanel = function(config) {
  var config = config || {};
  var defaultConfig = {};
  Ext.apply(this, config, defaultConfig);

  this.delayedItems = {};

  Sonatype.navigation.NavigationPanel.superclass.constructor.call(this, {
        cls : 'st-server-panel',
        autoScroll : true,
        border : false,
        items : []
      });
};

Ext.extend(Sonatype.navigation.NavigationPanel, Ext.Panel, {
      insert : function(sectionIndex, container) {
        if (sectionIndex == null || sectionIndex == undefined)
          sectionIndex = 0;
        if (container == null)
          return;

        // check if this is an attempt to add a navigation item to an existing
        // section
        if (container.sectionId)
        {
          var panel = this.findById(container.sectionId);
          if (panel)
          {
            return panel.insert(sectionIndex, container);
          }
          else
          {
            if (this.delayedItems[container.sectionId] == null)
            {
              this.delayedItems[container.sectionId] = [];
            }
            this.delayedItems[container.sectionId].push(container);
            return null;
          }
        }

        var panel = new Sonatype.navigation.Section(container);
        panel = Sonatype.navigation.NavigationPanel.superclass.insert.call(this, sectionIndex, panel);
        if (panel.id && this.delayedItems[panel.id])
        {
          panel.add(this.delayedItems[panel.id]);
          this.delayedItems[panel.id] = null;
        }
        return panel;
      },
      add : function(c) {
        var arr = null;
        var a = arguments;
        if (a.length > 1)
        {
          arr = a;
        }
        else if (Ext.isArray(c))
        {
          arr = c;
        }
        if (arr != null)
        {
          for (var i = 0; i < arr.length; i++)
          {
            this.add(arr[i]);
          }
          return;
        }

        if (c == null)
          return;

        // check if this is an attempt to add a navigation item to an existing
        // section
        if (c.sectionId)
        {
          var panel = this.findById(c.sectionId);
          if (panel)
          {
            panel.add(c);
            panel.sort();
          }
          else
          {
            if (this.delayedItems[c.sectionId] == null)
            {
              this.delayedItems[c.sectionId] = [];
            }
            this.delayedItems[c.sectionId].push(c);
            return null;
          }
        }

        var panel = new Sonatype.navigation.Section(c);
        panel = Sonatype.navigation.NavigationPanel.superclass.add.call(this, panel);
        if (panel.id && this.delayedItems[panel.id])
        {
          panel.add(this.delayedItems[panel.id]);
          this.delayedItems[panel.id] = null;
          panel.sort();
        }
        return panel;
      }
    });

Sonatype.navigation.Section = function(config) {
  var config = config || {};
  var defaultConfig = {
    collapsible : true,
    titleCollapse: true,
    collapsed : false
  };

  if (config.items)
  {
    config.items = this.transformItem(config.items);
  }
  if (!config.items || config.items.length == 0)
  {
    config.hidden = true;
  }

  Ext.apply(this, config, defaultConfig);

  Sonatype.navigation.Section.superclass.constructor.call(this, {
        cls : 'st-server-sub-container',
        layout : 'fit',
        frame : true,
        autoHeight : true,
        listeners : {
          collapse : this.collapseExpandHandler,
          expand : this.collapseExpandHandler,
          scope : this
        }
      });
};

Ext.extend(Sonatype.navigation.Section, Ext.Panel, {
      collapseExpandHandler : function() {
        if (this.layout && this.layout.layout)
        {
          this.ownerCt.doLayout();
        }
      },

      transformItem : function(c) {
        if (!c)
          return null;

        if (Ext.isArray(c))
        {
          var c2 = [];
          for (var i = 0; i < c.length; i++)
          {
            var item = this.transformItem(c[i]);
            if (item)
            {
              c2.push(item);
            }
          }
          return c2;
        }

        if (!c.xtype)
        {
          if (c.href)
          {
            // regular external link
            return {
              sortable_title : c.title,
              autoHeight : true,
              html : '<ul class="group-links"><li><a href="' + c.href + '" target="' + c.href + '">' + c.title + '</a></li></ul>'
            }
          }
          else if (c.tabCode || c.handler)
          {

            if (Sonatype.view.supportedNexusTabs)
            {
              Sonatype.view.supportedNexusTabs[c.tabId] = true;
            }
            // panel open action
            return c.enabled == false ? null : {
              sortable_title : c.title,
              autoHeight : true,
              id : 'navigation-' + c.tabId,
              initialConfigNavigation : c,
              listeners : {
                render : {
                  fn : function(panel) {
                    panel.body.on('click', Ext.emptyFn, null, {
                          delegate : 'a',
                          preventDefault : true
                        });
                    panel.body.on('mousedown', function(e, target) {
                          e.stopEvent();
                          if (c.handler)
                          {
                            c.handler();
                          }
                          else
                          {
                            Sonatype.view.mainTabPanel.addOrShowTab(c.tabId, c.tabCode, {
                                  title : c.tabTitle ? c.tabTitle : c.title
                                });
                          }
                        }, c.scope, {
                          delegate : 'a'
                        });
                  },
                  scope : this
                }
              },
              html : '<ul class="group-links"><li><a href="#">' + c.title + '</a></li></ul>'
            }
          }
        }
        return c;
      },

      add : function(c) {
        var arr = null;
        var a = arguments;
        if (a.length > 1)
        {
          arr = a;
        }
        else if (Ext.isArray(c))
        {
          arr = c;
        }
        if (arr != null)
        {
          for (var i = 0; i < arr.length; i++)
          {
            this.add(arr[i]);
          }
          return;
        }

        var c = this.transformItem(c);
        if (c == null)
          return;

        if (this.hidden)
        {
          this.show();
        }
        return Sonatype.navigation.Section.superclass.add.call(this, c);
      },

      sort : function(asOrder) {
        if(!this.items)
        {
            return;
        }
          
        _fSorter = function(obj1, obj2) {
           var fieldName = "sortable_title";
           return Sonatype.utils.sortFn(obj1[fieldName], obj2[fieldName])
        };
        this.items.sort(asOrder || 'ASC', _fSorter);
      }
    });

Ext.namespace('Sonatype.menu');

Sonatype.menu.Menu = function(config) {
  var config = config || {};
  var defaultConfig = {};
  Ext.apply(this, config, defaultConfig);

  Sonatype.menu.Menu.superclass.constructor.call(this);
};

Ext.extend(Sonatype.menu.Menu, Ext.menu.Menu, {
      add : function(c) {
        if (c == null)
          return null;

        var arr = null;
        var a = arguments;
        if (a.length > 1)
        {
          arr = a;
        }
        else if (Ext.isArray(c))
        {
          arr = c;
        }
        if (arr != null)
        {
          for (var i = 0; i < arr.length; i++)
          {
            this.add(arr[i]);
          }
          return;
        }

        var item = Sonatype.menu.Menu.superclass.add.call(this, c);
        var param = c.payload ? c.payload : this.payload;
        if (c.handler && param)
        {
          // create a delegate to pass the payload object to the handler
          item.setHandler(c.handler.createDelegate(c.scope ? c.scope : this.scope, [param], 0));
        }
        return item;
      }
    });
