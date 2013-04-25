/*global define*/
define('ext/ux/CheckColumn', ['extjs'], function(Ext) {

  /*
  This file is part of Ext JS 3.4

  Copyright (c) 2011-2012 Sencha Inc

  Contact:  http://www.sencha.com/contact

  Commercial Usage
  Licensees holding valid commercial licenses may use this file in accordance with the Commercial
  Software License Agreement provided with the Software or, alternatively, in accordance with the
  terms contained in a written agreement between you and Sencha.

  If you are unsure which license is appropriate for your use, please contact the sales department
  at http://www.sencha.com/contact.

  Build date: 2012-07-26 18:41:31 (${git.current.hash})
  */
  Ext.ns('Ext.ux.grid');

  /**
   * @class Ext.ux.grid.CheckColumn
   * @extends Ext.grid.Column
   * <p>A Column subclass which renders a checkbox in each column cell which toggles the truthiness of the associated data field on click.</p>
   * <p><b>Note. As of ExtJS 3.3 this no longer has to be configured as a plugin of the GridPanel.</b></p>
   * <p>Example usage:</p>
   * <pre><code>
  var cm = new Ext.grid.ColumnModel([{
         header: 'Foo',
         ...
      },{
         xtype: 'checkcolumn',
         header: 'Indoor?',
         dataIndex: 'indoor',
         width: 55
      }
  ]);

  // create the grid
  var grid = new Ext.grid.EditorGridPanel({
      ...
      colModel: cm,
      ...
  });
   * </code></pre>
   * In addition to toggling a Boolean value within the record data, this
   * class toggles a css class between <tt>'x-grid3-check-col'</tt> and
   * <tt>'x-grid3-check-col-on'</tt> to alter the background image used for
   * a column.
   */
  Ext.ux.grid.CheckColumn = Ext.extend(Ext.grid.Column, {

      /**
       * @private
       * Process and refire events routed from the GridView's processEvent method.
       */
      processEvent : function(name, e, grid, rowIndex, colIndex){
          if (name == 'mousedown' && !this.readOnly) {
              var record = grid.store.getAt(rowIndex);
              record.set(this.dataIndex, !record.data[this.dataIndex]);
              return false; // Cancel row selection.
          } else {
              return Ext.grid.ActionColumn.superclass.processEvent.apply(this, arguments);
          }
      },

      renderer : function(v, p, record){
          p.css += ' x-grid3-check-col-td';
          return String.format('<div class="x-grid3-check-col{0}">&#160;</div>', v ? '-on' : '');
      },

      // Deprecate use as a plugin. Remove in 4.0
      init: Ext.emptyFn
  });

  // register ptype. Deprecate. Remove in 4.0
  Ext.preg('checkcolumn', Ext.ux.grid.CheckColumn);

  // backwards compat. Remove in 4.0
  Ext.grid.CheckColumn = Ext.ux.grid.CheckColumn;

  // register Column xtype
  Ext.grid.Column.types.checkcolumn = Ext.ux.grid.CheckColumn;
});
