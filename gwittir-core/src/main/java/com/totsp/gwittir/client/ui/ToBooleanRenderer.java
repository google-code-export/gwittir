package com.totsp.gwittir.client.ui;
/*
 * ToBooleanRenderer.java
 *
 * Created on July 27, 2007, 7:56 PM
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

/**
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet" Cooper</a>
 */
public class ToBooleanRenderer implements Renderer<Object,Boolean> {
    
    public static final ToBooleanRenderer INSTANCE = new ToBooleanRenderer();
    
    /** Creates a new instance of ToBooleanRenderer */
    private ToBooleanRenderer() {
        super();
    }

    public Boolean render(Object o) {
        if( o == null ){
            return null;
        }
        if( o instanceof Boolean ){
            return (Boolean) o;
        }
        return Boolean.valueOf( o.toString() );
    }
    
}
