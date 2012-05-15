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
Sonatype.headLinks = Ext.emptyFn;

Ext.apply(Sonatype.headLinks.prototype, {
      /**
       * Update the head links based on the current status of Nexus
       */
      updateLinks : function() {
        var right = Ext.get('head-link-r');

        var loggedIn = Sonatype.user.curr.isLoggedIn;
        if (loggedIn)
        {
          this.updateRightWhenLoggedIn(right);
        }
        else
        {
          this.updateRightWhenLoggedOut(right);
        }
      },

      updateRightWhenLoggedIn : function(linkEl) {
        linkEl.update(Sonatype.user.curr.username);
        linkEl.addClass('head-link-logged-in');
        linkEl.un('click', Sonatype.repoServer.RepoServer.loginHandler, Sonatype.repoServer.RepoServer)
        linkEl.on('click', Sonatype.repoServer.RepoServer.showProfileMenu);
      },
      updateRightWhenLoggedOut : function(linkEl) {
        linkEl.un('click', Sonatype.repoServer.RepoServer.showProfileMenu);
        linkEl.update('Log In');

        this.setClickLink(linkEl);
        linkEl.removeClass('head-link-logged-in');
      },
      setClickLink : function(el) {
        el.removeAllListeners();
        el.on('click', Sonatype.repoServer.RepoServer.loginHandler, Sonatype.repoServer.RepoServer);
      }
    });
