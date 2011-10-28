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
(function() {

  // ********* Set ExtJS options
  // *************************************************

  Ext.Ajax.defaultHeaders = {
    'accept' : 'application/json'
  };

  // Set default HTTP headers //@todo: move this to some other common init
  // section
  Ext.lib.Ajax.defaultPostHeader = 'application/json; charset=utf-8';

  // set Sonatype defaults for Ext widgets
  Ext.form.Field.prototype.msgTarget = 'under';

  Sonatype.MessageBox.minWidth = 200;

  Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

  Sonatype.config = function() {
    var host = window.location.protocol + '//' + window.location.host;
    var contextPath = window.location.pathname;
    contextPath = contextPath.substr(0, contextPath.lastIndexOf('/'));
    var servicePathSnippet = '/service/local';
    var servicePath = contextPath + servicePathSnippet;
    var resourcePath = contextPath;
    var browsePathSnippet = '/content';
    var contentPath = contextPath + browsePathSnippet;
    var repoBrowsePathSnippet = browsePathSnippet + '/repositories';
    var groupBrowsePathSnippet = browsePathSnippet + '/groups';
    var repoServicePathSnippet = servicePathSnippet + '/repositories';
    var groupServicePathSnippet = servicePathSnippet + '/repo_groups';

    return {
      isDebug : false, // set to true to enable Firebug console output
                        // (getfirebug.com)
      host : host,
      servicePath : servicePath,
      resourcePath : resourcePath,
      extPath : resourcePath + '/ext-2.3',
      contentPath : contentPath,
      cssPath : '/styles',
      jsPath : '/js',
      browsePathSnippet : browsePathSnippet,

      installedServers : {
        repoServer : true
      },

      repos : {
        snippets : {
          repoBrowsePathSnippet : repoBrowsePathSnippet,
          groupBrowsePathSnippet : groupBrowsePathSnippet,
          repoServicePathSnippet : repoServicePathSnippet,
          groupServicePathSnippet : groupServicePathSnippet
        },
        urls : {
          login : servicePath + '/authentication/login',
          logout : servicePath + '/authentication/logout',
          globalSettings : servicePath + '/global_settings',
          globalSettingsState : servicePath + '/global_settings/current',
          restApiSettings: servicePath + '/rest_api_settings',
          repositories : servicePath + '/repositories',
          allRepositories : servicePath + '/all_repositories',
          repositoryStatuses : servicePath + '/repository_statuses',
          repoTemplates : servicePath + '/templates/repositories',
          repoTemplate : {
            virtual : servicePath + '/templates/repositories/default_virtual',
            hosted : servicePath + '/templates/repositories/default_hosted_release', // default
            hosted_release : servicePath + '/templates/repositories/default_hosted_release',
            hosted_snapshot : servicePath + '/templates/repositories/default_hosted_snapshot',
            proxy : servicePath + '/templates/repositories/default_proxy_release', // default
            proxy_release : servicePath + '/templates/repositories/default_proxy_release',
            proxy_snapshot : servicePath + '/templates/repositories/default_proxy_snapshot'
          },

          metadata : servicePath + '/metadata',
          cache : servicePath + '/data_cache',
          groups : servicePath + '/repo_groups',
          routes : servicePath + '/repo_routes',
          configs : servicePath + '/configs',
          configCurrent : servicePath + '/configs/current',
          logs : servicePath + '/logs',
          logConfig : servicePath + '/log/config',
          feeds : servicePath + '/feeds',
          recentlyChangedArtifactsRss : servicePath + '/feeds/recentChanges',
          recentlyCachedArtifactsRss : servicePath + '/feeds/recentlyCached',
          recentlyDeployedArtifactsRss : servicePath + '/feeds/recentlyDeployed',
          systemChangesRss : servicePath + '/feeds/systemChanges',
          status : servicePath + '/status',
          schedules : servicePath + '/schedules',
          scheduleRun : servicePath + '/schedule_run',
          scheduleTypes : servicePath + '/schedule_types',
          upload : servicePath + '/artifact/maven/content',
          redirect : servicePath + '/artifact/maven/redirect',
          trash : servicePath + '/wastebasket',
          plexusUsersAllConfigured : servicePath + '/plexus_users/allConfigured',
          plexusUsersDefault : servicePath + '/plexus_users/default',
          plexusUsers : servicePath + '/plexus_users',
          userLocators : servicePath + '/components/userLocators',
          searchUsers : servicePath + '/user_search',
          plexusUser : servicePath + '/plexus_user',
          userToRoles : servicePath + '/user_to_roles',
          users : servicePath + '/users',
          usersReset : servicePath + '/users_reset',
          usersForgotId : servicePath + '/users_forgotid',
          usersForgotPassword : servicePath + '/users_forgotpw',
          usersChangePassword : servicePath + '/users_changepw',
          usersSetPassword : servicePath + '/users_setpw',
          roles : servicePath + '/roles',
          plexusRoles : servicePath + '/plexus_roles',
          plexusRolesAll : servicePath + '/plexus_roles/all',
          externalRolesAll : servicePath + '/external_role_map/all',
          privileges : servicePath + '/privileges',
          repoTargets : servicePath + '/repo_targets',
          repoContentClasses : servicePath + '/components/repo_content_classes',
          realmComponents : servicePath + '/components/realm_types',
          repoTypes : servicePath + '/components/repo_types',
          repoMirrors : servicePath + '/repository_mirrors',
          repoPredefinedMirrors : servicePath + '/repository_predefined_mirrors',
          repoMirrorStatus : servicePath + '/repository_mirrors_status',
          privilegeTypes : servicePath + '/privilege_types',
          smtpSettingsState : servicePath + '/check_smtp_settings'
        }
      },

      content : {
        groups : contentPath + '/groups',
        repositories : contentPath + '/repositories'
      }
    }
  }();

  // Default anonymous user permissions; 3-bit permissions: delete | edit | read
  Sonatype.user.anon = {
    username : '',
    isLoggedIn : false,
    authToken : null,
    repoServer : {}
  };

  Sonatype.user.curr = Sonatype.utils.cloneObj(Sonatype.user.anon);
  // Sonatype.user.curr = {
  // repoServer : {
  // viewSearch : 1,
  // viewUpdatedArtifacts : 1,
  // viewCachedArtifacts : 1,
  // viewDeployedArtifacts : 1,
  // viewSystemChanges : 1,
  // maintRepos : 3,
  // maintLogs : 1,
  // maintConfig : 1,
  // configServer : 3,
  // configGroups : 7,
  // configRules : 7,
  // configRepos : 7
  // }
  // };

})();
