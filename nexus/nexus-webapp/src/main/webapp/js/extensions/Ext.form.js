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
/*
 * Ext.form.js Sonatype specific Ext Form overrides and extensions
 * Ext.form.Field override to provide help text quick tip Sonatype
 * implementations of form Actions Note: Ext namespace is maintained
 */

Ext.override(Ext.form.BasicForm, {
      clearInvalid : function() {
        // same as before, but ignore items without clearInvalid (== non-form-items)
        this.items.each(function(f) {
          if (f.clearInvalid) {
            f.clearInvalid();
          }
        })
      },
      /**
       * Override findField to look for enabled field and return that, otherwise
       * return first found
       */
      findField : function(id) {
        var field = null;
        var fallbackField = null;
        this.items.each(function(f) {
              if (f.isFormField && (f.dataIndex == id || f.id == id || f.getName() == id))
              {
                // Only want to grab the first one found, to match default
                // behaviour
                if (fallbackField == null)
                {
                  fallbackField = f;
                }

                // If the field isn't disabled use it
                if (f.disabled == false)
                {
                  field = f;
                  return false;
                }
              }
            });

        if (field == null)
        {
          if (fallbackField != null)
          {
            field = fallbackField;
          }
          else
          {
            field = this.items.get(id);
          }
        }

        return field || null;
      }
    });

/*
 * Override default form field rendering to include help text quick tip on
 * question mark rendered after field label.
 */
Ext.override(Ext.form.Field, {
      afterRenderOrig : Ext.form.Field.prototype.afterRender,
      afterRender : function() {
        var helpClass = null;
        var wrapDiv = null;
        if (this.getXType() == 'combo' || this.getXType() == 'uxgroupcombo' || this.getXType() == 'datefield' || this.getXType() == 'timefield')
        {
          wrapDiv = this.getEl().up('div.x-form-field-wrap');
          helpClass = 'form-label-helpmark-combo';
        }
        else if (this.getXType() == 'checkbox')
        {
          wrapDiv = this.getEl().up('div.x-form-check-wrap');
          helpClass = 'form-label-helpmark-check';
        }
        else if (this.getXType() == 'textarea')
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
          wrapDiv.createChild({
                tag : 'span',
                cls : 'form-label-after-field',
                html : this.afterText
              });
        }

        if (this.helpText)
        {
          var helpMark = wrapDiv.createChild({
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
        this.afterRenderOrig(arguments);
      }

    });

/**
 * @class Ext.form.Action.sonatypeSubmit
 * @extends Ext.form.Action A custom sonatype form serializer that submits JSON
 *          text to the Sonatype service and processes the returned response.
 *          ToDo: Define the error response format and variations. How to expose
 *          this to callbacks and event handlers? Other data may be placed into
 *          the response for processing the the Ext.form.BasicForm's callback or
 *          event handler methods. The object decoded from this JSON is
 *          available in the result property. Note: no form.errorReader
 *          accepted. JSON error format (when defined) is the only format
 *          accepted. Additional option params: serviceDataObj: reference object
 *          that matches the service's data object for this action fpanel:
 *          FormPanel dataModifiers: optional functions to modify or collect
 *          data values Additional values: after submit the output object is
 *          available on action.output. This is the native object that was
 *          serialized and sent to the server.
 */
Ext.form.Action.sonatypeSubmit = function(form, options) {
  if (options.autoValidation == undefined)
  {
    options.autoValidation = true;
  }
  Ext.form.Action.sonatypeSubmit.superclass.constructor.call(this, form, options);
};

Ext.extend(Ext.form.Action.sonatypeSubmit, Ext.form.Action, {
  /**
   * @cfg {boolean} clientValidation Determines whether a Form's fields are
   *      validated in a final call to
   *      {@link Ext.form.BasicForm#isValid isValid} prior to submission. Pass
   *      <tt>false</tt> in the Form's submit options to prevent this. If not
   *      defined, pre-submission field validation is performed.
   */
  type : 'sonatypeSubmit',

  // private
  run : function() {
    var o = this.options;
    var method = this.getMethod();
    var isPost = method == 'POST';
    if (o.clientValidation === false || this.form.isValid())
    {
      var sJsonOutput = this.serializeForm(this.options.fpanel, this.form);

      Ext.Ajax.request(Ext.apply(this.createCallback(o), {
            jsonData : sJsonOutput,
            url : this.getUrl(!isPost),
            method : method,
            params : isPost ? this.getParams() : null,
            isUpload : this.form.fileUpload
          }));
    }
    else if (o.clientValidation !== false)
    { // client validation failed
      this.failureType = Ext.form.Action.CLIENT_INVALID;
      this.form.afterAction(this, false);
    }
  },

  // override connection failure because server validation errors come back with
  // 400 code
  failure : function(response) {
    this.response = response;
    if (response.status == 400 && this.options.autoValidation)
    { // validation error
      this.success(response);
      return;
    }

    this.failureType = Ext.form.Action.CONNECT_FAILURE;
    this.form.afterAction(this, false);
  },

  // private
  success : function(response) {
    var result = this.processResponse(response);

    // if a 204 response, we arent looking at errors, it should go through ok
    if (result === true || result.data || response.status == 204 ||
        // 1223 is the IE way to deal with 204 http://goo.gl/xzJt1
        response.status == 1223)
    {
      this.form.afterAction(this, true);
      return;
    }

    if (result.errors != null)
    {
      if (this.options.validationModifiers)
      {
        var remainingErrors = [];
        for (var i = 0; i < result.errors.length; i++)
        {
          if (this.options.validationModifiers[result.errors[i].id])
          {
            if (typeof(this.options.validationModifiers[result.errors[i].id]) == 'function')
            {
              (this.options.validationModifiers[result.errors[i].id])(result.errors[i], this.options.fpanel);
            }
            else
            {
              var errorObj = result.errors[i];
              errorObj.id = this.options.validationModifiers[result.errors[i].id];
              remainingErrors[remainingErrors.length] = errorObj;
            }
          }
          else
          {
            remainingErrors[remainingErrors.length] = result.errors[i];
          }
        }

        result.errors = remainingErrors;
      }

      if (result.errors.length == 1 && result.errors[0].id == '*')
      {
        Sonatype.MessageBox.show({
              title : 'Configuration Error',
              msg : result.errors[0].msg,
              buttons : Sonatype.MessageBox.OK,
              icon : Sonatype.MessageBox.ERROR
            });
        return;
      }

      this.form.markInvalid(result.errors);
      this.failureType = Ext.form.Action.SERVER_INVALID;
    }
    this.form.afterAction(this, false);

    // if we came in here on 400 error, and we properly marked field as error,
    // hide dialog
    if (result.errors != null)
    {
      Sonatype.MessageBox.hide();
    }
  },

  // private
  handleResponse : function(response) {
    try
    {
      return Ext.decode(response.responseText); // throws SyntaxError
    }
    catch (e)
    {
      return false;
    }
  },

  // private
  serializeForm : function(fpanel, form) {
    var output = Sonatype.utils.cloneObj(this.options.serviceDataObj);
    // note: srcObj (form.sonatypeLoadedData) is not modified only walked
    var resultOutput = this.serializeFormHelper(fpanel, output, this.options.serviceDataObj, '');
    this.output = {
      "data" : resultOutput ? resultOutput : output
    };
    if (Sonatype.config.isDebug)
    {
      console.info(this.options.method + ' ' + this.options.url + ' ', this.output);
    }
    return Ext.encode(this.output);
  },

  // serializeHelper(object fpanel, object accObj, object srcObj, string
  // sPrepend, [string sVal])
  // Leave off sVal arg to call on root data obj
  // Walks the data object sent from the server originally, and plucks field
  // values from form
  // applying modifier functions if specified. Handles collapsed fleldsets by
  // returning null
  // as object value to server.
  // Invariant: srcObj is not modified! If this changes, call with a cloned copy
  serializeFormHelper : function(fpanel, accObj, srcObj, sPrepend, sVal) {
    var value, nextPrepend;
    if (sVal)
    { // non-root case
      nextPrepend = sPrepend + sVal + '.';
      value = srcObj[sVal]; // @todo: "value" name here is whack because, it's
      // not the field value
    }
    else
    { // root case
      nextPrepend = sPrepend;
      value = srcObj;
    }

    if (this.options.dataModifiers && this.options.dataModifiers['rootData'])
    {
      var fieldValue = null;
      fieldValue = (this.options.dataModifiers['rootData'])(fieldValue, fpanel);
      return fieldValue;
    }
    else if (Ext.type(value) === 'object')
    {
      if (sVal)
      { // only write object serialization for non-root objects
        var fieldSet = Ext.getCmp(fpanel.id + '_' + sPrepend + sVal);
        if (!fieldSet)
        {
          fieldSet = fpanel.find('name', 'fieldset_' + sPrepend + sVal)[0];
        }

        if (fieldSet && fieldSet.collapsed)
        {
          eval('accObj' + '.' + sPrepend + sVal + ' = null;');
          return; // skip recursive calls for children form items
        }
      }

      for (var i in value)
      {
        this.serializeFormHelper(fpanel, accObj, value, nextPrepend, i);
      }
    }
    else
    { // only non-root case should ever get in here
      var flatName = sPrepend + sVal;
      var field = fpanel.form.findField(flatName);
      var fieldValue = null;

      if (field && !Ext.isEmpty(field.getValue(), false))
      { // getValue normalizes undefined to '', but it's still false
        fieldValue = field.getValue();
      }

      // data mod function gets the field value if the field exists. o/w null is
      // passed
      fieldValue = (this.options.dataModifiers && this.options.dataModifiers[flatName]) ? (this.options.dataModifiers[flatName])(fieldValue, fpanel) : fieldValue;
      eval('accObj' + '.' + flatName + ' = fieldValue;');
    }
  }

    // @note: this was going to be used for processing hierarchical error
    // messaging
    // // walks the error response and flattens each field into a flat object by
    // fieldname
    // flattenErrors : function(accObj, srcObj, sPrepend, sVal){
    // var value, nextPrepend;
    // if (sVal){ //non-root case
    // nextPrepend = sPrepend + sVal + '.';
    // value = srcObj[sVal];
    // }
    // else { //root case
    // nextPrepend = sPrepend;
    // value = srcObj;
    // }
    //
    // if (Ext.type(value) === 'object'){
    // for (var i in value){
    // this.flattenErrors(fpanel, accObj, value, nextPrepend, i);
    // }
    // }
    // else { //only non-root case should ever get in here
    // var flatName = sPrepend + sVal;
    // if(!Ext.isEmpty(value, true)){
    // accObj[flatName] = value;
    // }
    // }
    // }
  });

/**
 * @class Ext.form.Action.sonatypeLoad
 * @extends Ext.form.Action A class which handles loading of data from Sonatype
 *          service into the Fields of an Ext.form.BasicForm. Expected repsonse
 *          format { data: { clientName: "Fred. Olsen Lines", portOfLoading:
 *          "FXT", portOfDischarge: "OSL" } } Other data may be placed into the
 *          response for processing the Ext.form.BasicForm Form's callback or
 *          event handler methods. The object decoded from this JSON is
 *          available in the result property. Needed Improvements: create a
 *          standard way to for callbacks to assess server respone's success.
 *          The regular loader gave access to this in the data.success field
 *          that it required in its data format. We could access
 *          this.response.HTTPcode (?). Should we push that down in an acessible
 *          way, so every callback doesn't need to understand our service
 *          response codes. Notes No form.reader may be used here. JSON data
 *          format is assumed this loader Additional options params: fpanel the
 *          FromPanel containing this form dataModifiers (optional)
 */
Ext.form.Action.sonatypeLoad = function(form, options) {
  Ext.form.Action.sonatypeLoad.superclass.constructor.call(this, form, options);
};

Ext.extend(Ext.form.Action.sonatypeLoad, Ext.form.Action, {
      // private
      type : 'sonatypeLoad',

      // private
      run : function() {
        Ext.Ajax.request(Ext.apply(this.createCallback(this.options), {
              method : this.getMethod(),
              url : this.getUrl(false),
              params : this.getParams(),
              suppressStatus : this.options.suppressStatus
            }));
      },

      // private
      // note: service response object "data" value expected here in result.data
      success : function(response) {
        var result = this.processResponse(response);
        if (result === true || !result.data)
        {
          this.failureType = Ext.form.Action.LOAD_FAILURE;
          this.form.afterAction(this, false);
          return;
        }
        this.form.clearInvalid();
        var flatData = this.translateDataToFieldValues(this.options.fpanel, result.data);
        this.form.setValues(flatData);
        this.form.afterAction(this, true);
      },

      // private
      // called from in Ext.form.Action.processResponse
      handleResponse : function(response) {
        return Ext.decode(response.responseText);
      },

      // private
      // takes result.data and returns flattened data to pass to
      // this.form.setValues()
      translateDataToFieldValues : function(fpanel, data) {
        var flat = {};
        this.translateHelper(fpanel, flat, data, '');
        return flat;
      },

      // translateHelper(object accObj, object srcObj, string sPrepend, [string
      // sVal])
      // Leave off sVal arg to call on root data obj
      translateHelper : function(fpanel, accObj, srcObj, sPrepend, sVal) {
        var value, nextPrepend;
        if (sVal)
        { // non-root case
          nextPrepend = sPrepend + sVal + '.';
          value = srcObj[sVal];
        }
        else
        { // root case
          nextPrepend = sPrepend;
          value = srcObj;
        }

        if (this.options.dataModifiers && this.options.dataModifiers['rootData'])
        {
          accObj = this.options.dataModifiers['rootData'](value, srcObj, fpanel);
        }
        else if (Ext.type(value) === 'object')
        {
          var hasNonEmptyChildren = false;
          for (var i in value)
          {
            var thisChildNotEmpty = this.translateHelper(fpanel, accObj, value, nextPrepend, i);
            hasNonEmptyChildren = hasNonEmptyChildren || thisChildNotEmpty;
          }
          if (sVal)
          { // only write object serialization for non-root objects
            if (hasNonEmptyChildren)
            {
              var fieldSet = Ext.getCmp(fpanel.id + '_' + sPrepend + sVal);
              if (!fieldSet)
              {
                fieldSet = fpanel.find('name', 'fieldset_' + sPrepend + sVal)[0];
              }
              if (fieldSet)
              {
                fieldSet.expand();
              }
            }
            accObj['.' + sPrepend + sVal] = hasNonEmptyChildren;
          }
          return hasNonEmptyChildren;
        }
        else
        { // only non-root case should ever get in here
          var flatName = sPrepend + sVal;
          // note: all vaues passed to modifier funcs, even if the value is
          // undefined, null, or empty!
          // Modifier funcs should ALWAYS return a value, even if it's
          // unmodified.
          value = (this.options.dataModifiers && this.options.dataModifiers[flatName]) ? this.options.dataModifiers[flatName](value, srcObj, fpanel) : value;
          if (Ext.isEmpty(value, true))
          {
            return false;
          }
          accObj[flatName] = value;
          return true;
        }
      }
    });

Ext.form.Action.ACTION_TYPES.sonatypeLoad = Ext.form.Action.sonatypeLoad;
Ext.form.Action.ACTION_TYPES.sonatypeSubmit = Ext.form.Action.sonatypeSubmit;

/*
 * Generic object editor (intended to be subclassed). When used with
 * Sonatype.panels.GridViewer, instanced of this editor panel will never be
 * reused. When the form is submitted, all child panels related to this grid
 * record are re-created, so the editor does not have to worry about altering
 * its state after submit (as opposed to having to disable certain fields after
 * the new record is saved, which we had to do previously). Config options:
 * cancelButton: if set to "true", the form will display a "Cancel" button, so
 * the invoker can subscribe to a "cancel" event and do the necessary cleanup
 * (e.g. close the panel). By default, the form will display a "Reset" button
 * instead, which reloads the form when clicked. Note a special dataModifier of
 * 'rootData' will work with entire contents of json response rather than on a
 * field by field basis. Using this modifier will then render other modifiers
 * useless dataModifiers: { // data modifiers on form submit/load load: { attr1:
 * func1, attr2: func2 }, save: { ... } } dataStores: an array of data stores
 * this editor depends on. It will make sure all stores are loaded before the
 * form load request is sent. The stores should be configured with auto load
 * off. listeners: { // custom events offered by the editor panel cancel: { fn:
 * function( panel ) { // do cleanup, remove the form from the container, //
 * delete the temporary grid record, etc. }, scope: this }, load: { fn:
 * function( form, action, receivedData ) { // do extra work for data load if
 * needed }, scope: this }, submit: { fn: function( form, action, receivedData ) { //
 * update the grid record and do other stuff if needed var rec = this.payload;
 * rec.beginEdit(); rec.set( 'attr1', receivedData.attr1 ); rec.set( 'attr2',
 * receivedData.attr2 ); rec.commit(); rec.endEdit(); }, scope: this } }
 * payload: the grid record being edited referenceData: a reference data object
 * that's used as a template on form submit uri: base URL for JSON requests. It
 * will be used for POST requests when creating a new object, or for PUT (with
 * payload.id appended) if the record does not a resourceURI attribute
 */
Sonatype.ext.FormPanel = function(config) {
  var config = config || {};
  var defaultConfig = {
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
    }
  };
  Ext.apply(this, config, defaultConfig);

  this.checkPayload();
  if (this.isNew && this.cancelButton == null)
  {
    this.cancelButton = true;
  }

  Sonatype.ext.FormPanel.superclass.constructor.call(this, {
        buttons : (config.readOnly || this.readOnly) ? [] : [{
              text : 'Save',
              handler : this.saveHandler,
              scope : this
            }, {
              handler : this.cancelButton ? this.cancelHandler : this.resetHandler,
              scope : this,
              text : this.cancelButton ? 'Cancel' : 'Reset'
            }]
      });

  this.on('afterlayout', this.initData, this, {
        single : true
      });
  this.on('afterlayout', this.registerRequiredQuicktips, this, {
        single : true
      });
  this.form.on('actioncomplete', this.actionCompleteHandler, this);
  this.form.on('actionfailed', this.actionFailedHandler, this);

  this.addEvents({
        cancel : true,
        load : true,
        submit : true
      });
};

Ext.extend(Sonatype.ext.FormPanel, Ext.FormPanel, {
      convertDataValue : function(value, store, idProperty, nameProperty) {
        if (value)
        {
          var rec = store.getAt(store.find(idProperty, value));
          if (rec)
          {
            return rec.data[nameProperty];
          }
        }
        return '';
      },
      checkPayload : function() {
        this.isNew = false;
        if (this.payload)
        {
          if (this.payload.id.substring(0, 4) == 'new_')
          {
            this.isNew = true;
          }
        }
      },

      checkStores : function() {
        if (this.dataStores)
        {
          for (var i = 0; i < this.dataStores.length; i++)
          {
            var store = this.dataStores[i];
            if (store.lastOptions == null)
            {
              return false;
            }
          }
        }
        return true;
      },

      dataStoreLoadHandler : function(store, records, options) {
        if (this.checkStores())
        {
          this.loadData();
        }
      },

      registerRequiredQuicktips : function(formPanel, fLayout) {
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

      cancelHandler : function(button, event) {
        this.fireEvent('cancel', this);
      },

      resetHandler : function(button, event) {
        this.loadData();
      },

      initData : function() {
        if (this.dataStores)
        {
          for (var i = 0; i < this.dataStores.length; i++)
          {
            var store = this.dataStores[i];
            store.on('load', this.dataStoreLoadHandler, this);
            if (store.autoLoad != true)
            {
              store.load();
            }
          }
        }
        else
        {
          this.loadData();
        }
      },

      loadData : function() {
        if (this.isNew)
        {
          this.form.reset();
        }
        else
        {
          this.form.doAction('sonatypeLoad', {
                url : this.getActionURL(),
                method : 'GET',
                fpanel : this,
                dataModifiers : this.dataModifiers ? this.dataModifiers.load : {},
                scope : this
              });
        }
      },

      isValid : function() {
        return this.form.isValid();
      },

      saveHandler : function(button, event) {
        if (this.isValid())
        {
          this.form.doAction('sonatypeSubmit', {
            method : this.getSaveMethod(),
            url : this.getActionURL(),
            waitMsg : this.isNew ? 'Creating a new record...' : 'Updating records...',
            fpanel : this,
            validationModifiers : this.validationModifiers,
            dataModifiers : this.dataModifiers ? this.dataModifiers.submit : {},
            serviceDataObj : this.referenceData,
            isNew : this.isNew
              // extra option to send to callback, instead of conditioning on
              // method
            });
        }
      },

      actionFailedHandler : function(form, action) {
        if (action.failureType == Ext.form.Action.CLIENT_INVALID)
        {
          Sonatype.MessageBox.alert('Missing or Invalid Fields', 'Please change the missing or invalid fields.').setIcon(Sonatype.MessageBox.WARNING);
        }
        else if (action.failureType == Ext.form.Action.CONNECT_FAILURE || action.response)
        {
          Sonatype.utils.connectionError(action.response, 'There is an error communicating with the server.');
        }
        else if (action.failureType == Ext.form.Action.LOAD_FAILURE)
        {
          Sonatype.MessageBox.alert('Load Failure', 'The data failed to load from the server.').setIcon(Sonatype.MessageBox.ERROR);
        }
      },

      // (Ext.form.BasicForm, Ext.form.Action)
      actionCompleteHandler : function(form, action) {
        var receivedData = action.handleResponse(action.response).data;
        if (receivedData == null)
        {
          receivedData = {};
        }
        if (action.type == 'sonatypeSubmit')
        {
          this.fireEvent('submit', form, action, receivedData);

          if (this.isNew && this.payload.autoCreateNewRecord)
          {
            var store = this.payload.store;
            store.remove(this.payload);

            if (Ext.isArray(receivedData))
            {
              for (var i = 0; i < receivedData.length; i++)
              {
                var r = receivedData[i];
                var rec = new store.reader.recordType(r, r.resourceURI);
                this.addSorted(store, rec);
              }
            }
            else
            {
              var rec = new store.reader.recordType(receivedData, receivedData.resourceURI);
              rec.autoCreateNewRecord = true;
              this.addSorted(store, rec);
            }
          }
          this.isNew = false;
          this.payload.autoCreateNewRecord = false;
        }
        else if (action.type == 'sonatypeLoad')
        {
          this.fireEvent('load', form, action, receivedData);
        }
      },

      addSorted : function(store, rec) {
        store.addSorted(rec);
      },

      getActionURL : function() {
        return this.isNew ? this.uri : // if new, return the uri
            (this.payload.data && this.payload.data.resourceURI ? // if resouceURI is supplied,
                // return it
                this.payload.data.resourceURI
                : this.uri + '/' + this.payload.id); // otherwise construct a
        // uri
      },

      getSaveMethod : function() {
        return this.isNew ? 'POST' : 'PUT';
      },

      optionalFieldsetExpandHandler : function(panel) {
        panel.items.each(function(item, i, len) {
              if (item.getEl().up('div.required-field', 3))
              {
                item.allowBlank = false;
              }
              else if (item.isXType('fieldset', true))
              {
                this.optionalFieldsetExpandHandler(item);
              }
            }, this);
      },

      optionalFieldsetCollapseHandler : function(panel) {
        panel.items.each(function(item, i, len) {
              if (item.getEl().up('div.required-field', 3))
              {
                item.allowBlank = true;
              }
              else if (item.isXType('fieldset', true))
              {
                this.optionalFieldsetCollapseHandler(item);
              }
            }, this);
      }
    });

/*
 * ! Ext JS Library 3.2.1 Copyright(c) 2006-2010 Ext JS, Inc.
 * licensing@extjs.com http://www.extjs.com/license
 */
/**
 * @class Ext.form.DisplayField
 * @extends Ext.form.Field A display-only text field which is not validated and
 *          not submitted.
 * @constructor Creates a new DisplayField.
 * @param {Object}
 *          config Configuration options
 * @xtype displayfield
 */
Ext.form.DisplayField = Ext.extend(Ext.form.Field, {
  validationEvent : false,
  validateOnBlur : false,
  defaultAutoCreate : {
    tag : "div"
  },
  /**
   * @cfg {String} fieldClass The default CSS class for the field (defaults to
   *      <tt>"x-form-display-field"</tt>)
   */
  fieldClass : "x-form-display-field",
  /**
   * @cfg {Boolean} htmlEncode <tt>false</tt> to skip HTML-encoding the text
   *      when rendering it (defaults to <tt>false</tt>). This might be
   *      useful if you want to include tags in the field's innerHTML rather
   *      than rendering them as string literals per the default logic.
   */
  htmlEncode : false,

  // private
  initEvents : Ext.emptyFn,

  isValid : function() {
    return true;
  },

  validate : function() {
    return true;
  },

  getRawValue : function() {
    var v = this.rendered ? this.el.dom.innerHTML : Ext.value(this.value, '');
    if (v === this.emptyText)
    {
      v = '';
    }
    if (this.htmlEncode)
    {
      v = Ext.util.Format.htmlDecode(v);
    }
    return v;
  },

  getValue : function() {
    return this.getRawValue();
  },

  getName : function() {
    return this.name;
  },

  setRawValue : function(v) {
    if (this.htmlEncode)
    {
      v = Ext.util.Format.htmlEncode(v);
    }
    return this.rendered ? (this.el.dom.innerHTML = (Ext.isEmpty(v) ? '' : v)) : (this.value = v);
  },

  setValue : function(v) {
    this.setRawValue(v);
    return this;
  }
    /**
     * @cfg {String} inputType
     * @hide
     */
    /**
     * @cfg {Boolean} disabled
     * @hide
     */
    /**
     * @cfg {Boolean} readOnly
     * @hide
     */
    /**
     * @cfg {Boolean} validateOnBlur
     * @hide
     */
    /**
     * @cfg {Number} validationDelay
     * @hide
     */
    /**
     * @cfg {String/Boolean} validationEvent
     * @hide
     */
  });

Ext.reg('displayfield', Ext.form.DisplayField);

Ext.form.TimestampDisplayField = Ext.extend(Ext.form.DisplayField, {
      setValue : function(v) {
        // java give the timestamp in miliseconds, extjs consumes it in seconds
        var toSecs = Math.round(v / 1000);
        v = new Date.parseDate(toSecs, 'U').toString();
        this.setRawValue(v);
        return this;
      }
    });

Ext.reg('timestampDisplayField', Ext.form.TimestampDisplayField);

Ext.form.ByteDisplayField = Ext.extend(Ext.form.DisplayField, {
      setValue : function(v) {
        if (v < 1024)
        {
          v = v + ' Bytes';
        }
        else if (v < 1048576)
        {
          v = (v / 1024).toFixed(2) + ' KB';
        }
        else if (v < 1073741824)
        {
          v = (v / 1048576).toFixed(2) + ' MB';
        }
        else
        {
          v = (v / 1073741824).toFixed(2) + ' GB';
        }
        this.setRawValue(v);
        return this;
      }
    });

Ext.reg('byteDisplayField', Ext.form.ByteDisplayField);

Ext.override(Ext.form.TextField, {
  /**
   * @cfg {Boolean} htmlDecode
   * <tt>true</tt> to decode html entities in the value given to
   * Ext.form.ByteDisplayField.setValue and Ext.form.ByteDisplayField.setRawValue
   * before setting the actual value.
   * <p/>
   * This is needed for displaying the 'literal' value in the text field when it was received by the server,
   * for example in the repository name. The REST layer will encode to html entities, which will be correct
   * for html rendering, but text fields without this configuration will display '&quot;test&quot;' instead
   * of the originally sent '"test"'.
   */
  htmlDecode : false,

  /**
   * @cfg {Boolean} htmlConvert
   * <tt>true</tt> to decode html entities in the value given to
   * Ext.form.TextField.set(Raw)Value
   * before setting the actual value, and encode html entities again
   * in the call to Ext.form.TextField.get(Raw)Value.
   * <p/>
   * This is needed for displaying the 'literal' value in the text field when it was received by the server
   * (see htmlDecode configuration doc), and display to the user correctly before round-tripping to the server again
   * (e.g. in a grid field).
   * <p/>
   * when this config is set, the value has to be html-decoded again before sending it to the server, because the REST layer
   * will encode the string again.
   */
  htmlConvert : false,
  
  setRawValueOrig : Ext.form.TextField.prototype.setRawValue,
  setValueOrig : Ext.form.TextField.prototype.setValue,
  getRawValueOrig : Ext.form.TextField.prototype.getRawValue,
  getValueOrig : Ext.form.TextField.prototype.getValue,

  setRawValue : function(value) {
    if ( this.htmlDecode || this.htmlConvert )
    {
      value = Ext.util.Format.htmlDecode(value);
    }
    this.setRawValueOrig(value);
  },
  setValue : function(value) {
    if ( this.htmlDecode || this.htmlConvert )
    {
      value = Ext.util.Format.htmlDecode(value);
    }
    this.setValueOrig(value);
  },
  getRawValue : function() {
    var value = this.getRawValueOrig();
    if ( this.htmlConvert )
    {
      value = Ext.util.Format.htmlEncode(value);
    }
    return value;
  },
  getValue : function() {
    var value = this.getValueOrig();
    if ( this.htmlConvert )
    {
      value = Ext.util.Format.htmlEncode(value);
    }
    return value;
  }
});


