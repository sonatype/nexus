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
/*global define*/
define('ext/form/field',['extjs','sonatype'], function(Ext, Sonatype) {
Ext.override(Ext.form.Field, {
  adjustWidth : function(tag, w) {
    tag = tag.toLowerCase();
    // Sonatype: modified input text sizing for Safari3 in strict mode bug.
    if (typeof w === 'number')
    {
      if (Ext.isIE && (tag === 'input' || tag === 'textarea'))
      {
        if (tag === 'input' && !Ext.isStrict)
        {
          return this.inEditor ? w : w - 3;
        }
        if (tag === 'input' && Ext.isStrict)
        {
          return w - (Ext.isIE6 ? 4 : 1);
        }
        if (tag === 'textarea' && Ext.isStrict)
        {
          return w - 2;
        }
      }
      else if (Ext.isOpera && Ext.isStrict)
      {
        if (tag === 'input')
        {
          return w + 2;
        }
        if (tag === 'textarea')
        {
          return w - 2;
        }
      }
      else if (Ext.isSafari3)
      {
        // Sonatype: assumes we are serving xhtml transitional doctype
        if (tag === 'input')
        {
          return w - 8;
        }
      }
    }
    return w;
  },
  onEnable : function() {
    this.getActionEl().removeClass(this.disabledClass);
    if (this.actionMode === 'container')
    {
      // in some cases the action mode seems to change after rendering,
      // so we may need to clean up the disabled class from this.el
      this.el.removeClass(this.disabledClass);

      //also check for wrap
      if ( this.wrap ) {
        this.wrap.removeClass(this.disabledClass);
      }
    }
    this.el.dom.readOnly = false;
  },
  onDisable : function() {
    this.getActionEl().addClass(this.disabledClass);
    this.el.dom.readOnly = true;
  },
  /*
   * Override default form field rendering to include help text quick tip on
   * question mark rendered after field label.
   */
  afterRenderOrig : Ext.form.Field.prototype.afterRender,
  afterRender : function() {
    var helpClass = null, wrapDiv = null, helpMark = null;
    if (this.getXType() === 'combo' || this.getXType() === 'uxgroupcombo' || this.getXType() === 'datefield' || this.getXType() === 'timefield')
    {
      wrapDiv = this.getEl().up('div.x-form-field-wrap');
      helpClass = 'form-label-helpmark-combo';
    }
    else if (this.getXType() === 'checkbox')
    {
      wrapDiv = this.getEl().up('div.x-form-check-wrap');
      helpClass = 'form-label-helpmark-check';
    }
    else if (this.getXType() === 'textarea')
    {
      wrapDiv = this.getEl().up('div.x-form-element');
      helpClass = 'form-label-helpmark-textarea';
    }
    else
    {
      wrapDiv = this.getEl().up('div.x-form-element');
      helpClass = 'form-label-helpmark';
    }

    // @todo: afterText doesn't work with combo boxes!
    if (this.afterText)
    {
      if ( !wrapDiv ) {
        wrapDiv = this.getEl().wrap({cls:'x-form-element'});
      }

      wrapDiv.createChild({
        tag : 'span',
        cls : 'form-label-after-field',
        html : this.afterText
      });
    }

    if (this.helpText)
    {
      if ( !wrapDiv ) {
        wrapDiv = this.getEl().wrap({cls:'x-form-element'});
      }

      helpMark = wrapDiv.createChild({
        tag : 'img',
        src : Sonatype.config.resourcePath + '/images/icons/help.png',
        width : 16,
        height : 16,
        cls : helpClass
      });

      Ext.QuickTips.register({
        target : helpMark,
        title : '',
        text : this.helpText,
        enabled : true
      });
    }

    // original method
    this.afterRenderOrig.apply(this, arguments);
  }

});
});
