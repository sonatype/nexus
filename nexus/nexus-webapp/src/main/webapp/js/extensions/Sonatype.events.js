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
Sonatype.utils.Observable = function() {
  this.addEvents({
        /*
         * Fired when the main Nexus navigation panel is being built.
         * Subscribers can use this event to add items to the navigation panel.
         * A Sonatype.navigation.NavigationPanel instance is passed as a
         * parameter. The subscriber can use the "add" method on it to append
         * new sections or individual items to existing sections (using a
         * "sectionId" config property). init: function() {
         * Sonatype.Events.addListener( 'nexusNavigationInit', this.naviHandler,
         * this ); }, naviHandler: function( navigationPanel ) { // add a new
         * section with some items navigationPanel.add( { title: 'My Section',
         * id: 'my-nexus-section', items: [ { title: 'Open New Tab', tabId:
         * 'my-unique-tab-id', tabCode: My.package.ClassName, // JavaScript
         * class implementing Ext.Panel, tabTitle: 'My Tab' // optional tab
         * title (if different from the link title) }, { title: 'Open Another
         * Tab', tabId: 'my-second-tab-id', tabCode: My.package.AnotherClass,
         * enabled: this.isSecondTabEnabled() // condition to show the link or
         * not }, { title: 'Pop-up Dialog', handler: this.popupHandler, // click
         * handler scope: this // handler execution scope } ] } ); // add a link
         * to an existing section navigationPanel.add( { sectionId:
         * 'st-nexus-docs', title: 'Download Nexus', href:
         * 'http://nexus.sonatype.org/using/download.html' } ); }, See
         * Sonatype.repoServer.RepoServer.addNexusNavigationItems() for more
         * examples.
         */
        'nexusNavigationInit' : true,

        /*
         * Fired when a repository context action menu is initialized.
         * Subscribers can add action items to the menu. A menu object and
         * repository record are passed as parameters. If clicked, the action
         * handler receives a repository record as parameter. init: function() {
         * Sonatype.Events.addListener( 'repositoryMenuInit', this.onRepoMenu,
         * this ); }, onRepoMenu: function( menu, repoRecord ) { if (
         * repoRecord.get( 'repoType' ) == 'proxy' ) { menu.add(
         * this.actions.myProxyAction ); } },
         */
        'repositoryMenuInit' : true,

        /*
         * Fired when an repository item (e.g. artifact or folder) context
         * action menu is initialized. Subscribers can add action items to the
         * menu. A menu object, a repository and item records are passed as
         * parameters. If clicked, the action handler receives an item record as
         * parameter. init: function() { Sonatype.Events.addListener(
         * 'repositoryContentMenuInit', this.onArtifactMenu, this ); },
         * onRepoMenu: function( menu, repoRecord, contentRecord ) { if (
         * repoRecord.get( 'repoType' ) == 'proxy' ) { menu.add(
         * this.actions.myProxyContentAction ); } },
         */
        'repositoryContentMenuInit' : true,

        /*
         * Fired when a user action menu is initialized (most likely an admin
         * function). Subscribers can add action items to the menu. A menu
         * object and a user record are passed as parameters. If clicked, the
         * action handler receives an user record as parameter. init: function() {
         * Sonatype.Events.addListener( 'userMenuInit', this.onUserMenu, this ); },
         * onUserMenu: function( menu, userRecord ) { if ( userRecord.get(
         * 'userId' ) != 'anonymous' ) { menu.add( this.actions.myUserAction ); } },
         */
        'userMenuInit' : true,

        /*
         * Fired when a privilege formPanel initializes Subscribers can mangle
         * the PrivilegeEditor as they see fit init: function() {
         * Sonatype.Events.addListener( 'privilegeEditorInit',
         * this.onPrivilegeEditorInit, this ); }, onPrivilegeEditorInit:
         * function( editor ) { editor.mangle(); },
         */
        'privilegeEditorInit' : true,

        /*
         * Fired when a privilege panel initializes Subscribers can mangle the
         * PrivilegePanel as they see fit init: function() {
         * Sonatype.Events.addListener( 'privilegePanelInit',
         * this.onPrivilegePanelInit, this ); }, onPrivilegePanelInit: function(
         * panel ) { panel.mangle(); },
         */
        'privilegePanelInit' : true
      });
};
Ext.extend(Sonatype.utils.Observable, Ext.util.Observable);

Sonatype.Events = new Sonatype.utils.Observable();
