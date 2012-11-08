/**
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
package org.sonatype.nexus.util;

import java.util.LinkedHashMap;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * A simple and generic "tree like" structure for use cases where applicable. It assumes root node has {@code null}
 * parent, but you can easily override the {@link #isRoot()} method if needed. Implementation uses {@link LinkedHashMap}
 * to store keyed children, hence it should be fast, and preserve addition ordering, but not so conservative on memory
 * in case of huge trees.
 * 
 * @author cstamas
 * @param <P>
 * @since 2.0
 */
public class Node<P>
{
    private final Node<P> parent;

    private final String label;

    private final P payload;

    private final LinkedHashMap<String, Node<P>> children;

    /**
     * Contructs a node instance.
     * 
     * @param parent the parent of the created node.
     * @param label the label of the created node.
     * @param payload the payload of the created node.
     */
    public Node( final Node<P> parent, final String label, final P payload )
    {
        this.parent = parent;
        this.label = Preconditions.checkNotNull( label );
        this.payload = payload;
        this.children = new LinkedHashMap<String, Node<P>>();
    }

    // ==

    /**
     * Returns the parent node of this node, or {@code null} if this is "root" node.
     * 
     * @return
     */
    public Node<P> getParent()
    {
        return parent;
    }

    /**
     * Returns {@code true} if this node is root node.
     * 
     * @return
     */
    public boolean isRoot()
    {
        return getParent() == null;
    }

    /**
     * Returns the depth from the "root" node. Root node has depth 0.
     * 
     * @return
     */
    public int getDepth()
    {
        Node<P> currentNode = this;
        int result = 0;
        while ( !currentNode.isRoot() )
        {
            currentNode = currentNode.getParent();
            result++;
        }
        return result;
    }

    /**
     * Returns the "label" of this node, never {@code null}.
     * 
     * @return
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * Returns the "payload" associated with this node.
     * 
     * @return
     */
    public P getPayload()
    {
        return payload;
    }

    /**
     * Creates a child node of this node, and returns it, never returns {@code null}.
     * 
     * @param label
     * @param payload
     * @return
     */
    public Node<P> addChild( final String label, final P payload )
    {
        final Node<P> node = new Node<P>( this, label, payload );
        this.children.put( node.getLabel(), node );
        return node;
    }

    /**
     * Removes the child from this node's children.
     * 
     * @param child
     */
    public void removeChild( final Node<P> child )
    {
        children.remove( child.getLabel() );
    }

    /**
     * Returns the child of this node that has "labal" equals to the passed in, or {@code null} if no child present with
     * such label.
     * 
     * @param label
     * @return
     */
    public Node<P> getChildByLabel( final String label )
    {
        return children.get( label );
    }

    /**
     * Returns an immutable snapshot of this node's children as list of nodes.
     * 
     * @return
     */
    public List<Node<P>> getChildren()
    {
        return new ImmutableList.Builder<Node<P>>().addAll( children.values() ).build();
    }

}
