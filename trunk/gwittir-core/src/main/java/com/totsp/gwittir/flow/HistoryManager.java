/*
 * HistoryManager.java
 *
 * Created on June 27, 2007, 11:43 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.totsp.gwittir.flow;

import com.google.gwt.user.client.ui.Panel;
import com.totsp.gwittir.ui.BoundWidget;

/**
 *
 * @author rcooper
 */
public interface HistoryManager {
    
    public void transition( String name, BoundWidget old, BoundWidget current );
    
    public void apply( String historyToken );
    
    public String getParameter( String key );
    
    public void setParameter(String key, String value);
   
}
