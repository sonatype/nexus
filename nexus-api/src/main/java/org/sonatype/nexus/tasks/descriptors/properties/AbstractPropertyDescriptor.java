/**
 * Sonatype Nexus™ [Open Source Version].
 * Copyright © 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at ${thirdpartyurl}.
 *
 * This program is licensed to you under Version 3 only of the GNU General
 * Public License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * Version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * Version 3 along with this program. If not, see http://www.gnu.org/licenses/.
 */
package org.sonatype.nexus.tasks.descriptors.properties;

public abstract class AbstractPropertyDescriptor
    implements ScheduledTaskPropertyDescriptor
{
    private String helpText;
    private boolean required;
    
    public String getHelpText()
    {
        return helpText;
    }

    public boolean isRequired()
    {
        return required;
    }

    public void setHelpText( String helpText )
    {
        this.helpText = helpText;
    }

    public void setRequired( boolean required )
    {
        this.required = required;
    }
}
