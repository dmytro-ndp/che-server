/*
 * Copyright (c) 2012-2023 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.factory.server.bitbucket;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import org.eclipse.che.api.factory.server.bitbucket.server.BitbucketServerApiClient;
import org.eclipse.che.api.factory.server.bitbucket.server.NoopBitbucketServerApiClient;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.inject.ConfigurationException;
import org.eclipse.che.security.oauth.OAuthAPI;
import org.eclipse.che.security.oauth1.BitbucketServerOAuthAuthenticator;
import org.eclipse.che.security.oauth1.OAuthAuthenticator;
import org.mockito.Mock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class BitbucketServerApiClientProviderTest {
  BitbucketServerOAuthAuthenticator oAuthAuthenticator;
  @Mock OAuthAPI oAuthAPI;

  @BeforeClass
  public void setUp() {
    oAuthAuthenticator =
        new BitbucketServerOAuthAuthenticator(
            "df", "private", " https://bitbucket2.server.com", " https://che.server.com");
  }

  @Test
  public void shouldBeAbleToCreateBitbucketServerApi() {
    // given
    BitbucketServerApiProvider bitbucketServerApiProvider =
        new BitbucketServerApiProvider(
            "https://bitbucket.server.com, https://bitbucket2.server.com",
            "https://bitbucket.server.com",
            "https://bitbucket.server.com",
            oAuthAPI,
            ImmutableSet.of(oAuthAuthenticator));
    // when
    BitbucketServerApiClient actual = bitbucketServerApiProvider.get();
    // then
    assertNotNull(actual);
    assertTrue(HttpBitbucketServerApiClient.class.isAssignableFrom(actual.getClass()));
  }

  @Test
  public void shouldNormalizeURLsBeforeCreateBitbucketServerApi() {
    // given
    BitbucketServerApiProvider bitbucketServerApiProvider =
        new BitbucketServerApiProvider(
            "https://bitbucket.server.com, https://bitbucket2.server.com",
            "https://bitbucket.server.com",
            "https://bitbucket.server.com",
            oAuthAPI,
            ImmutableSet.of(oAuthAuthenticator));
    // when
    BitbucketServerApiClient actual = bitbucketServerApiProvider.get();
    // then
    assertNotNull(actual);
    // internal representation always w/out slashes
    assertTrue(actual.isConnected("https://bitbucket.server.com/"));
  }

  @Test(dataProvider = "noopConfig")
  public void shouldProvideNoopOAuthAuthenticatorIfSomeConfigurationIsNotSet(
      @Nullable String bitbucketEndpoints, Set<OAuthAuthenticator> authenticators)
      throws IOException {
    // given
    BitbucketServerApiProvider bitbucketServerApiProvider =
        new BitbucketServerApiProvider(
            bitbucketEndpoints, "https://bitbucket.org", "", oAuthAPI, authenticators);
    // when
    BitbucketServerApiClient actual = bitbucketServerApiProvider.get();
    // then
    assertNotNull(actual);
    assertTrue(NoopBitbucketServerApiClient.class.isAssignableFrom(actual.getClass()));
  }

  @Test(dataProvider = "httpOnlyConfig")
  public void shouldProvideHttpAuthenticatorIfOauthConfigurationIsNotSet(
      @Nullable String bitbucketEndpoints, Set<OAuthAuthenticator> authenticators)
      throws IOException {
    // given
    BitbucketServerApiProvider bitbucketServerApiProvider =
        new BitbucketServerApiProvider(
            bitbucketEndpoints, "https://bitbucket.org", "", oAuthAPI, authenticators);
    // when
    BitbucketServerApiClient actual = bitbucketServerApiProvider.get();
    // then
    assertNotNull(actual);
    assertTrue(HttpBitbucketServerApiClient.class.isAssignableFrom(actual.getClass()));
  }

  @Test(
      expectedExceptions = ConfigurationException.class,
      expectedExceptionsMessageRegExp =
          "`che.integration.bitbucket.server_endpoints` bitbucket configuration is missing. It should contain values from 'che.oauth.bitbucket.endpoint'")
  public void shouldFailToBuildIfEndpointsAreMisconfigured() {
    // given
    // when
    BitbucketServerApiProvider bitbucketServerApiProvider =
        new BitbucketServerApiProvider(
            "",
            "https://bitbucket.server.com",
            "https://bitbucket.server.com",
            oAuthAPI,
            ImmutableSet.of(oAuthAuthenticator));
  }

  @Test(
      expectedExceptions = ConfigurationException.class,
      expectedExceptionsMessageRegExp =
          "'che.oauth.bitbucket.endpoint' is set but BitbucketServerOAuthAuthenticator is not deployed correctly")
  public void shouldFailToBuildIfEndpointsAreMisconfigured2() {
    // given
    // when
    BitbucketServerApiProvider bitbucketServerApiProvider =
        new BitbucketServerApiProvider(
            "https://bitbucket.server.com, https://bitbucket2.server.com",
            "https://bitbucket.server.com",
            "https://bitbucket.server.com",
            oAuthAPI,
            Collections.emptySet());
  }

  @Test(
      expectedExceptions = ConfigurationException.class,
      expectedExceptionsMessageRegExp =
          "`che.integration.bitbucket.server_endpoints` must contain `https://bitbucket.server.com` value")
  public void shouldFailToBuildIfEndpointsAreMisconfigured3() {
    // given
    // when
    BitbucketServerApiProvider bitbucketServerApiProvider =
        new BitbucketServerApiProvider(
            "https://bitbucket3.server.com, https://bitbucket2.server.com",
            "https://bitbucket.server.com",
            "https://bitbucket.server.com",
            oAuthAPI,
            ImmutableSet.of(oAuthAuthenticator));
  }

  @DataProvider(name = "noopConfig")
  public Object[][] noopConfig() {
    return new Object[][] {
      {null, null},
      {"", null}
    };
  }

  @DataProvider(name = "httpOnlyConfig")
  public Object[][] httpOnlyConfig() {
    return new Object[][] {
      {"https://bitbucket.server.com", null},
      {"https://bitbucket.server.com, https://bitbucket2.server.com", null},
      {
        "https://bitbucket.server.com, https://bitbucket2.server.com",
        ImmutableSet.of(oAuthAuthenticator)
      }
    };
  }
}
