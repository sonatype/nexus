/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.proxy.repository;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.application.AuthenticationInfoConverter;
import org.sonatype.nexus.configuration.application.GlobalRemoteConnectionSettings;
import org.sonatype.nexus.configuration.model.CRemoteStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.proxy.storage.remote.RemoteProviderHintFactory;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

/**
 *
 */
public class AbstractProxyRepositoryConfiguratorTest
    extends TestSupport
{

    private AbstractProxyRepositoryConfigurator underTest;

    @Mock
    private RemoteProviderHintFactory providerHints;

    @Mock
    private GlobalRemoteConnectionSettings connSettings;

    @Mock
    private AuthenticationInfoConverter authInfoConverter;

    @Mock
    private CRepositoryCoreConfiguration coreConfiguration;

    @Mock
    private ProxyRepository repository;

    @Mock
    private ApplicationConfiguration configuration;

    @Mock
    private CRepository repoConfiguration;

    @Mock
    private CRemoteStorage storageConfiguration;

    @Mock
    private RemoteRepositoryStorage storage;

    @Mock
    private RemoteStorageContext storageContext;

    @Before
    public void setup()
    {
        underTest = new AbstractProxyRepositoryConfigurator( authInfoConverter, connSettings, providerHints ) {};

        when( providerHints.getDefaultHttpRoleHint() ).thenReturn( "defaultHint" );
        when( coreConfiguration.getConfiguration( true ) ).thenReturn( repoConfiguration );
        when( repoConfiguration.getRemoteStorage() ).thenReturn( storageConfiguration );
        when( repository.getRemoteStorage() ).thenReturn( storage );
        when( repository.getRemoteStorageContext() ).thenReturn( storageContext );
    }

    @Test
    public void doNotSaveDefaultProvider()
    {
        when( storage.getProviderId() ).thenReturn( "defaultHint" );

        underTest.doPrepareForSave( repository, configuration, coreConfiguration );

        verify( storageConfiguration ).setProvider( null );
    }

    @Test
    public void retainNonDefaultProvider()
    {
        when( storage.getProviderId() ).thenReturn( "differentHint" );

        underTest.doPrepareForSave( repository, configuration, coreConfiguration );

        verify( storageConfiguration, Mockito.never() ).setProvider( anyString() );
    }

}
