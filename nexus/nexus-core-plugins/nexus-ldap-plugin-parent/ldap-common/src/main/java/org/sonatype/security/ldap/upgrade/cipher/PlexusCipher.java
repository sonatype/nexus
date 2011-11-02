/**
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
package org.sonatype.security.ldap.upgrade.cipher;

/**
 * 
 * @author Oleg Gusakov
 * 
 */
public interface PlexusCipher
{
  public static String     ROLE                              = PlexusCipher.class
                                                                 .getName();

  public static final char ENCRYPTED_STRING_DECORATION_START = '{';
  public static final char ENCRYPTED_STRING_DECORATION_STOP  = '}';

  /**
   * encrypt given string with the given passPhrase and encode it into base64
   * 
   * @param str
   * @param passPhrase
   * @return
   * @throws PlexusCipherException
   */
  String encrypt(
      String str,
      String passPhrase )
      throws PlexusCipherException;

  /**
   * encrypt given string with the given passPhrase, encode it into base64 and
   * return result, wrapped into { } decorations
   * 
   * @param str
   * @param passPhrase
   * @return
   * @throws PlexusCipherException
   */
  String encryptAndDecorate(
      String str,
      String passPhrase )
      throws PlexusCipherException;

  /**
   * decrypt given base64 encrypted string
   * 
   * @param str
   * @param passPhrase
   * @return
   * @throws PlexusCipherException
   */
  String decrypt(
      String str,
      String passPhrase )
      throws PlexusCipherException;

  /**
   * decrypt given base64 encoded encrypted string. If string is decorated,
   * decrypt base64 encoded string inside decorations
   * 
   * @param str
   * @param passPhrase
   * @return
   * @throws PlexusCipherException
   */
  String decryptDecorated(
      String str,
      String passPhrase )
      throws PlexusCipherException;

  /**
   * check if given string is decorated
   * 
   * @param str
   * @return
   */
  public boolean isEncryptedString(
      String str );

  /**
   * return string inside decorations
   * 
   * @param str
   * @return
   * @throws PlexusCipherException
   */
  public String unDecorate(
      String str )
      throws PlexusCipherException;

  /**
   * decorated given string with { and }
   * 
   * @param str
   * @return
   */
  public String decorate(
      String str );

}
