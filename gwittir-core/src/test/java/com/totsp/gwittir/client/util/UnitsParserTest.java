/*
 * UnitsParserTest.java
 * JUnit based test
 *
 * Created on August 3, 2007, 3:44 PM
 */

package com.totsp.gwittir.client.util;

import com.totsp.gwittir.client.util.UnitsParser.UnitValue;
import junit.framework.*;

/**
 *
 * @author cooper
 */
public class UnitsParserTest extends TestCase {
    
    public UnitsParserTest(String testName) {
        super(testName);
    }

    public static class UnitValueTest extends TestCase {

        public UnitValueTest(String testName) {
            super(testName);
        }
    }


    /**
     * Test of parse method, of class com.totsp.gwittir.client.util.UnitsParser.
     */
    public void testParse() {
        UnitValue v = UnitsParser.parse( "100px" );
        this.assertEquals( 100, v.value );
        this.assertEquals( "px", v.units );
    }
    
}
