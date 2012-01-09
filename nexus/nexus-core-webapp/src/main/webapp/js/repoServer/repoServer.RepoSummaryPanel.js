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
Sonatype.repoServer.AbstractRepositorySummaryPanel = function(config) {
  var config = config || {};
  var defaultConfig = {};

  Ext.apply(this, config, defaultConfig);

  var abstractItems = [{
        xtype : 'textfield',
        fieldLabel : 'Repository Information',
        hidden : true
      }, {
        xtype : 'textarea',
        name : 'informationField',
        anchor : Sonatype.view.FIELD_OFFSET,
        readOnly : true,
        hideLabel : true,
        height : 150
      }];

  Sonatype.repoServer.AbstractRepositorySummaryPanel.superclass.constructor.call(this, {
        uri : this.payload.data.resourceURI + '/meta',
        readOnly : true,
        dataModifiers : {
          load : {
            'rootData' : this.populateFields.createDelegate(this)
          },
          save : {}
        },
        items : abstractItems.concat(this.items)
      });
};

Ext.extend(Sonatype.repoServer.AbstractRepositorySummaryPanel, Sonatype.ext.FormPanel, {
      getActionURL : function() {
        return this.uri;
      },
      populateFields : function(arr, srcObj, fpanel) {
        this.populateInformationField('Repository ID: ' + srcObj.id + '\n');
        this.populateInformationField('Repository Name: ' + this.payload.data.name + '\n');
        this.populateInformationField('Repository Type: ' + this.payload.data.repoType + '\n');
        this.populateInformationField('Repository Policy: ' + this.payload.data.repoPolicy + '\n');
        this.populateInformationField('Repository Format: ' + this.payload.data.format + '\n');
        this.populateInformationField('Contained in groups: ' + '\n' + this.combineGroups(srcObj.groups) + '\n');
      },
      populateInformationField : function(text) {
        var infoText = this.find('name', 'informationField')[0].getValue();
        infoText += text;
        this.find('name', 'informationField')[0].setRawValue(infoText);
      },
      combineGroups : function(groups) {
        var combinedGroups = '';
        if (groups != undefined && groups.length > 0)
        {
          for (var i = 0; i < groups.length; i++)
          {
            var group = this.groupStore.getAt(this.groupStore.findBy(function(rec, recid) {
                  return rec.data.id == groups[i];
                }, this));

            if (group)
            {
              if (combinedGroups.length > 0)
              {
                combinedGroups += '\n';
              }

              combinedGroups += '   ' + group.data.name;
            }
          }
        }

        return combinedGroups;
      }
    });

Sonatype.repoServer.HostedRepositorySummaryPanel = function(config) {
  var config = config || {};
  var defaultConfig = {};
  Ext.apply(this, config, defaultConfig);

  Sonatype.repoServer.HostedRepositorySummaryPanel.superclass.constructor.call(this, {
        items : [{
              xtype : 'textfield',
              fieldLabel : 'Distribution Management',
              hidden : true
            }, {
              xtype : 'textarea',
              name : 'distMgmtField',
              anchor : Sonatype.view.FIELD_OFFSET,
              readOnly : true,
              hideLabel : true,
              height : 100
            }]
      });
};

Ext.extend(Sonatype.repoServer.HostedRepositorySummaryPanel, Sonatype.repoServer.AbstractRepositorySummaryPanel, {
      populateFields : function(arr, srcObj, fpanel) {
        Sonatype.repoServer.HostedRepositorySummaryPanel.superclass.populateFields.call(this, arr, srcObj, fpanel);

        this.populateDistributionManagementField(this.payload.data.id, this.payload.data.repoPolicy, this.payload.data.contentResourceURI);
      },
      populateDistributionManagementField : function(id, policy, uri) {
        var distMgmtString = '<distributionManagement>\n  <${repositoryType}>\n    <id>${repositoryId}</id>\n    <url>${repositoryUrl}</url>\n  </${repositoryType}>\n</distributionManagement>';

        distMgmtString = distMgmtString.replaceAll('${repositoryType}', policy == 'Release' ? 'repository' : 'snapshotRepository');
        distMgmtString = distMgmtString.replaceAll('${repositoryId}', id);
        distMgmtString = distMgmtString.replaceAll('${repositoryUrl}', uri);

        this.find('name', 'distMgmtField')[0].setRawValue(distMgmtString);
      }
    });

Sonatype.repoServer.ProxyRepositorySummaryPanel = function(config) {
  var config = config || {};
  var defaultConfig = {};
  Ext.apply(this, config, defaultConfig);

  Sonatype.repoServer.ProxyRepositorySummaryPanel.superclass.constructor.call(this, {
        items : []
      });
};

Ext.extend(Sonatype.repoServer.ProxyRepositorySummaryPanel, Sonatype.repoServer.AbstractRepositorySummaryPanel, {
      populateFields : function(arr, srcObj, fpanel) {
        Sonatype.repoServer.ProxyRepositorySummaryPanel.superclass.populateFields.call(this, arr, srcObj, fpanel);

        var remoteUri = this.payload.data.remoteUri;

        if (remoteUri == undefined)
        {
          remoteUri = this.payload.data.remoteStorage.remoteStorageUrl;
        }

        this.populateInformationField('Remote URL: ' + remoteUri + '\n');
      }
    });

Sonatype.repoServer.VirtualRepositorySummaryPanel = function(config) {
  var config = config || {};
  var defaultConfig = {};
  Ext.apply(this, config, defaultConfig);

  Sonatype.repoServer.VirtualRepositorySummaryPanel.superclass.constructor.call(this, {
        items : []
      });
};

Ext.extend(Sonatype.repoServer.VirtualRepositorySummaryPanel, Sonatype.repoServer.AbstractRepositorySummaryPanel, {});

Sonatype.Events.addListener('repositoryViewInit', function(cardPanel, rec, gridPanel) {
      var sp = Sonatype.lib.Permissions;

      var repoPanels = {
        hosted : Sonatype.repoServer.HostedRepositorySummaryPanel,
        proxy : Sonatype.repoServer.ProxyRepositorySummaryPanel,
        virtual : Sonatype.repoServer.VirtualRepositorySummaryPanel
      };

      var panel = repoPanels[rec.data.repoType];

      if (panel && rec.data.resourceURI && sp.checkPermission('nexus:repometa', sp.READ))
      {
        cardPanel.add(new panel({
              tabTitle : 'Summary',
              name : 'summary',
              payload : rec,
              groupStore : gridPanel.groupStore
            }));
      }
    });